package top.csituka.magicaland.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** 实际 NetworkHandler 的事件与报文路径；桩仅替代 Fabric 和游戏传输环境。 */
public final class NetworkHandlerLifecycleTest {
    private static int checks;
    private static final UUID OWNER = new UUID(1, 1), VIEWER = new UUID(2, 2);

    public static void main(String[] args) throws Exception {
        NetworkHandler.registerServer();
        snapshotWithoutBroadcast();
        pendingAnimationAndLifecycle();
        staleConnection();
        System.out.println("PASS NetworkHandlerLifecycleTest: " + checks + " actual handler/event checks");
    }

    private static void snapshotWithoutBroadcast() {
        var server = new MinecraftServer();
        reset(server);
        var owner = join(server, OWNER);
        receive(server, owner, "{\"type\":\"model_update\",\"data\":\"{\\\"bodyColor\\\":123}\"}");
        receive(server, owner, "{\"type\":\"animation_update\",\"controller\":\"controller\",\"animation\":\"idle\"}");
        var viewer = join(server, VIEWER);
        require(!NetworkHandler.playerModels.containsKey(VIEWER), "viewer never sends own model");
        var messages = messages(viewer);
        require(messages.size() == 3, "read-only viewer receives handshake, appearance and animation");
        require(type(messages.get(0)).equals("handshake"), "handshake arrives before snapshot");
        require(type(messages.get(1)).equals("model_update") && ownerId(messages.get(1)), "existing model follows handshake");
        require(type(messages.get(2)).equals("animation_update") && ownerId(messages.get(2)), "animation follows its model");
        require(!messages.get(1).has("transform"), "snapshot does not replay transformation effects");
        viewer.sent.clear();
        receive(server, viewer, "{\"type\":\"model_update\",\"data\":\"{\\\"bodyColor\\\":456}\"}");
        require(viewer.sent.isEmpty(), "own model publication does not resend everyone's snapshot");
        var vanilla = new ServerPlayerEntity(new UUID(3, 3));
        vanilla.supportsChannel = false;
        server.getPlayerManager().online.add(vanilla);
        ServerPlayConnectionEvents.JOIN.listener.onJoin(new ServerPlayConnectionEvents.Handler(vanilla), null, server);
        require(vanilla.sent.isEmpty(), "vanilla client receives no unsupported packets");
    }

    private static void pendingAnimationAndLifecycle() throws Exception {
        var server = new MinecraftServer();
        reset(server);
        var owner = join(server, OWNER);
        var viewer = join(server, VIEWER);
        receive(server, owner, "{\"type\":\"model_update\",\"data\":\"{}\"}");
        animation(server, owner, "controller", "run");
        viewer.sent.clear();
        freezeBudget(OWNER);
        animation(server, owner, "controller", "walk");
        animation(server, owner, "blink_controller", "blink_parallel");
        animation(server, owner, "controller", "idle");
        require(viewer.sent.isEmpty(), "jittered packets wait instead of bypassing rate limit");
        releaseBudget(OWNER);
        ServerTickEvents.END_SERVER_TICK.listener.accept(server);
        require(NetworkHandler.playerAnimations.get(OWNER).get("controller").equals("idle"), "tick delivers final idle without another packet");
        require(messages(viewer).stream().noneMatch(message -> "walk".equals(string(message, "animation"))), "superseded walk not replayed");
        releaseBudget(OWNER);
        ServerTickEvents.END_SERVER_TICK.listener.accept(server);
        require(NetworkHandler.playerAnimations.get(OWNER).get("blink_controller").equals("blink_parallel"), "other controller not lost");
        freezeBudget(OWNER);
        animation(server, owner, "controller", "run");
        receive(server, owner, "{\"type\":\"model_remove\"}");
        viewer.sent.clear();
        ServerTickEvents.END_SERVER_TICK.listener.accept(server);
        require(!NetworkHandler.playerAnimations.containsKey(OWNER) && viewer.sent.isEmpty(), "withdrawal cannot replay queued animation");
        receive(server, owner, "{\"type\":\"model_update\",\"data\":\"{}\"}");
        animation(server, owner, "controller", "idle");
        require(NetworkHandler.playerAnimations.get(OWNER).get("controller").equals("idle"), "republish clears stale cooldown");
        freezeBudget(OWNER);
        animation(server, owner, "controller", "run");
        server.getPlayerManager().online.remove(owner);
        ServerPlayConnectionEvents.DISCONNECT.listener.onDisconnect(new ServerPlayConnectionEvents.Handler(owner), server);
        viewer.sent.clear();
        ServerTickEvents.END_SERVER_TICK.listener.accept(server);
        require(viewer.sent.isEmpty() && queue().pendingPlayers().isEmpty(), "disconnect removes pending work");
        reset(server);
        require(NetworkHandler.playerModels.isEmpty() && NetworkHandler.playerAnimations.isEmpty()
                && queue().pendingPlayers().isEmpty(), "stop clears synchronized and pending state");
    }

    private static void staleConnection() {
        var server = new MinecraftServer();
        reset(server);
        var stale = join(server, OWNER);
        server.getPlayerManager().online.remove(stale);
        join(server, OWNER);
        receive(server, stale, "{\"type\":\"model_update\",\"data\":\"{}\"}");
        require(NetworkHandler.playerModels.isEmpty(), "old session cannot republish after reconnect");
    }

    private static ServerPlayerEntity join(MinecraftServer server, UUID uuid) {
        var player = new ServerPlayerEntity(uuid);
        server.getPlayerManager().online.add(player);
        ServerPlayConnectionEvents.JOIN.listener.onJoin(new ServerPlayConnectionEvents.Handler(player), null, server);
        return player;
    }

    private static void receive(MinecraftServer server, ServerPlayerEntity player, String json) {
        ServerPlayNetworking.receiver.receive(server, player, null,
                PacketByteBufs.create().writeString(json, NetworkHandler.MAX_MESSAGE_LENGTH), null);
    }

    private static void animation(MinecraftServer server, ServerPlayerEntity player, String controller, String value) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "animation_update");
        message.addProperty("controller", controller);
        message.addProperty("animation", value);
        receive(server, player, message.toString());
    }

    private static List<JsonObject> messages(ServerPlayerEntity player) {
        return player.sent.stream().map(value -> JsonParser.parseString(value).getAsJsonObject()).toList();
    }

    private static String type(JsonObject message) { return string(message, "type"); }
    private static String string(JsonObject message, String name) { return message.has(name) ? message.get(name).getAsString() : ""; }
    private static boolean ownerId(JsonObject message) { return OWNER.toString().equals(string(message, "uuid")); }
    private static void reset(MinecraftServer server) { ServerLifecycleEvents.SERVER_STOPPED.listener.accept(server); }

    private static LatestAnimationUpdates queue() throws Exception {
        Field field = NetworkHandler.class.getDeclaredField("animationUpdates");
        field.setAccessible(true);
        return (LatestAnimationUpdates) field.get(null);
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, Long> budget() throws Exception {
        Field field = LatestAnimationUpdates.class.getDeclaredField("lastApplied");
        field.setAccessible(true);
        return (Map<UUID, Long>) field.get(queue());
    }

    private static void freezeBudget(UUID uuid) throws Exception { budget().put(uuid, System.nanoTime() + 10_000_000_000L); }
    private static void releaseBudget(UUID uuid) throws Exception { budget().put(uuid, System.nanoTime() - 100_000_000L); }
    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
