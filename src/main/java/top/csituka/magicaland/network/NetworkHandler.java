package top.csituka.magicaland.network;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import top.csituka.magicaland.gaze.ServerGaze;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkHandler {

    public static final Identifier CHANNEL = new Identifier("magicaland", "sync");
    private static final Gson GSON = new Gson();
    public static final int MAX_MESSAGE_LENGTH = 32767;
    public static final int MAX_MODEL_DATA_LENGTH = 16384;
    public static final int MAX_ANIMATION_LENGTH = 64;

    private static final long MODEL_UPDATE_INTERVAL_NANOS = 100_000_000L;
    private static final long ANIMATION_UPDATE_INTERVAL_NANOS = 50_000_000L;
    private static final Map<UUID, Long> lastModelUpdates = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastAnimationUpdates = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> ALLOWED_ANIMATIONS = Map.of(
            "controller", Set.of("fly", "elytra_fly", "swim", "swim_hold", "sneak", "sneaking", "run",
                    "backward_walk", "walk", "idle", "attacked", "jump1", "sleep", "boat", "ride",
                    "ride_pig", "sit", "fall_transfer", "land", "larger_land"),
            "blink_controller", Set.of("blink_parallel"),
            "ear_controller", Set.of("ear_parallel"),
            "tail_controller", Set.of("tail_parallel"));

    public static final Map<UUID, String> playerModels = new ConcurrentHashMap<>();
    public static final Map<UUID, Map<String, String>> playerAnimations = new ConcurrentHashMap<>();
    public static volatile boolean serverHasMod = false;

    public static void registerServer() {
        ServerGaze.register();

        ServerPlayNetworking.registerGlobalReceiver(CHANNEL,
                (server, player, handler, buf, responseSender) -> {
                    String json;
                    try {
                        json = buf.readString(MAX_MESSAGE_LENGTH);
                    } catch (RuntimeException ignored) {
                        return;
                    }
                    server.execute(() -> handleMessage(server, player, json));
                });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            JsonObject handshake = new JsonObject();
            handshake.addProperty("type", "handshake");
            handshake.addProperty("gaze_version", 1);
            send(handler.getPlayer(), GSON.toJson(handshake));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.getPlayer().getUuid();
            playerModels.remove(uuid);
            playerAnimations.remove(uuid);
            lastModelUpdates.remove(uuid);
            lastAnimationUpdates.remove(uuid);

            JsonObject remove = new JsonObject();
            remove.addProperty("type", "player_remove");
            remove.addProperty("uuid", uuid.toString());
            String removeJson = GSON.toJson(remove);

            for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
                send(other, removeJson);
            }
        });
    }

    public static boolean isAllowedAnimation(String controller, String animation) {
        if (controller == null || animation == null || animation.length() > MAX_ANIMATION_LENGTH) {
            return false;
        }
        Set<String> allowedAnimations = ALLOWED_ANIMATIONS.get(controller);
        return allowedAnimations != null && (animation.isEmpty() || allowedAnimations.contains(animation));
    }

    private static void handleMessage(MinecraftServer server, ServerPlayerEntity player, String json) {
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                return;
            }

            JsonObject msg = parsed.getAsJsonObject();
            String type = readString(msg, "type", 32);
            if (type == null) {
                return;
            }

            UUID uuid = player.getUuid();
            if ("model_update".equals(type)) {
                String modelData = readString(msg, "data", MAX_MODEL_DATA_LENGTH);
                if (modelData == null || !isModelData(modelData)
                        || isRateLimited(lastModelUpdates, uuid, MODEL_UPDATE_INTERVAL_NANOS)) {
                    return;
                }

                String previous = playerModels.put(uuid, modelData);
                if (modelData.equals(previous)) {
                    return;
                }

                JsonObject broadcast = new JsonObject();
                broadcast.addProperty("type", "model_update");
                broadcast.addProperty("uuid", uuid.toString());
                broadcast.addProperty("data", modelData);
                String broadcastJson = GSON.toJson(broadcast);

                for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
                    if (!other.getUuid().equals(uuid)) {
                        send(other, broadcastJson);
                    }
                }

                for (Map.Entry<UUID, String> entry : playerModels.entrySet()) {
                    if (!entry.getKey().equals(uuid)) {
                        JsonObject existing = new JsonObject();
                        existing.addProperty("type", "model_update");
                        existing.addProperty("uuid", entry.getKey().toString());
                        existing.addProperty("data", entry.getValue());
                        send(player, GSON.toJson(existing));
                    }
                }

                sendAnimationStates(player, uuid);
            } else if ("model_remove".equals(type)) {
                boolean removedModel = playerModels.remove(uuid) != null;
                boolean removedAnimations = playerAnimations.remove(uuid) != null;
                lastModelUpdates.remove(uuid);
                lastAnimationUpdates.remove(uuid);
                if (!removedModel && !removedAnimations) {
                    return;
                }

                JsonObject remove = new JsonObject();
                remove.addProperty("type", "player_remove");
                remove.addProperty("uuid", uuid.toString());
                String removeJson = GSON.toJson(remove);
                for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
                    if (!other.getUuid().equals(uuid)) {
                        send(other, removeJson);
                    }
                }
            } else if ("animation_update".equals(type)) {
                String controller = readString(msg, "controller", 32);
                String animation = msg.has("animation") ? readString(msg, "animation", MAX_ANIMATION_LENGTH) : "";
                if (!playerModels.containsKey(uuid) || !isAllowedAnimation(controller, animation)
                        || isRateLimited(lastAnimationUpdates, uuid, ANIMATION_UPDATE_INTERVAL_NANOS)) {
                    return;
                }

                Map<String, String> animations = playerAnimations.computeIfAbsent(uuid,
                        ignored -> new ConcurrentHashMap<>());
                String previous = animations.put(controller, animation);
                if (animation.equals(previous)) {
                    return;
                }

                for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
                    if (!other.getUuid().equals(uuid)) {
                        sendAnimationState(other, uuid, controller, animation);
                    }
                }
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static String readString(JsonObject object, String name, int maxLength) {
        if (!object.has(name) || !object.get(name).isJsonPrimitive()
                || !object.getAsJsonPrimitive(name).isString()) {
            return null;
        }
        String value = object.get(name).getAsString();
        return value.length() <= maxLength ? value : null;
    }

    private static boolean isModelData(String modelData) {
        try {
            return JsonParser.parseString(modelData).isJsonObject();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isRateLimited(Map<UUID, Long> lastUpdates, UUID uuid, long intervalNanos) {
        long now = System.nanoTime();
        Long previous = lastUpdates.get(uuid);
        if (previous != null && now - previous < intervalNanos) {
            return true;
        }
        lastUpdates.put(uuid, now);
        return false;
    }

    private static void sendAnimationStates(ServerPlayerEntity player, UUID excludedUuid) {
        for (Map.Entry<UUID, Map<String, String>> entry : playerAnimations.entrySet()) {
            if (entry.getKey().equals(excludedUuid)) {
                continue;
            }
            for (Map.Entry<String, String> animation : entry.getValue().entrySet()) {
                sendAnimationState(player, entry.getKey(), animation.getKey(), animation.getValue());
            }
        }
    }

    private static void sendAnimationState(ServerPlayerEntity player, UUID uuid, String controller, String animation) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "animation_update");
        message.addProperty("uuid", uuid.toString());
        message.addProperty("controller", controller);
        message.addProperty("animation", animation);
        send(player, GSON.toJson(message));
    }

    private static void send(ServerPlayerEntity player, String json) {
        if (!ServerPlayNetworking.canSend(player, CHANNEL)) {
            return;
        }
        try {
            ServerPlayNetworking.send(player, CHANNEL,
                    PacketByteBufs.create().writeString(json, MAX_MESSAGE_LENGTH));
        } catch (RuntimeException ignored) {
        }
    }
}
