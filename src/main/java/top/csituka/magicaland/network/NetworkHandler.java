package top.csituka.magicaland.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkHandler {

    public static final Identifier CHANNEL = new Identifier("magicaland", "sync");
    private static final Gson GSON = new Gson();

    public static final Map<UUID, String> playerModels = new ConcurrentHashMap<>();
    public static final Map<UUID, Map<String, String>> playerAnimations = new ConcurrentHashMap<>();
    public static volatile boolean serverHasMod = false;

    public static void registerServer() {

        ServerPlayNetworking.registerGlobalReceiver(CHANNEL,
                (server, player, handler, buf, responseSender) -> {
                    String json = buf.readString();
                    server.execute(() -> {
                        JsonObject msg = JsonParser.parseString(json).getAsJsonObject();
                        String type = msg.get("type").getAsString();

                        if ("model_update".equals(type)) {
                            String modelData = msg.get("data").getAsString();
                            UUID uuid = player.getUuid();
                            playerModels.put(uuid, modelData);

                            JsonObject broadcast = new JsonObject();
                            broadcast.addProperty("type", "model_update");
                            broadcast.addProperty("uuid", uuid.toString());
                            broadcast.addProperty("data", modelData);
                            String broadcastJson = GSON.toJson(broadcast);

                            for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
                                if (!other.getUuid().equals(uuid)) {
                                    ServerPlayNetworking.send(other, CHANNEL,
                                            PacketByteBufs.create().writeString(broadcastJson));
                                }
                            }

                            for (Map.Entry<UUID, String> entry : playerModels.entrySet()) {
                                if (!entry.getKey().equals(uuid)) {
                                    JsonObject existing = new JsonObject();
                                    existing.addProperty("type", "model_update");
                                    existing.addProperty("uuid", entry.getKey().toString());
                                    existing.addProperty("data", entry.getValue());
                                    ServerPlayNetworking.send(player, CHANNEL,
                                            PacketByteBufs.create().writeString(GSON.toJson(existing)));
                                }
                            }

                            sendAnimationStates(player, uuid);
                        } else if ("animation_update".equals(type)) {
                            String controller = msg.get("controller").getAsString();
                            String animation = msg.has("animation") ? msg.get("animation").getAsString() : "";
                            if (controller.isEmpty() || controller.length() > 32 || animation.length() > 64) {
                                return;
                            }

                            UUID uuid = player.getUuid();
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
                    });
                });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            JsonObject handshake = new JsonObject();
            handshake.addProperty("type", "handshake");
            ServerPlayNetworking.send(handler.getPlayer(), CHANNEL,
                    PacketByteBufs.create().writeString(GSON.toJson(handshake)));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.getPlayer().getUuid();
            playerModels.remove(uuid);
            playerAnimations.remove(uuid);

            JsonObject remove = new JsonObject();
            remove.addProperty("type", "player_remove");
            remove.addProperty("uuid", uuid.toString());
            String removeJson = GSON.toJson(remove);

            for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(other, CHANNEL,
                        PacketByteBufs.create().writeString(removeJson));
            }
        });
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
        ServerPlayNetworking.send(player, CHANNEL,
                PacketByteBufs.create().writeString(GSON.toJson(message)));
    }
}
