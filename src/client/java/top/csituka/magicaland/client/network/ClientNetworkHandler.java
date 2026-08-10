package top.csituka.magicaland.client.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.model.GeckoPlayerAnimatable;
import top.csituka.magicaland.network.NetworkHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientNetworkHandler {

    private static final Gson GSON = new Gson();

    public static final Map<UUID, ModelConfig> remoteModels = new ConcurrentHashMap<>();
    public static final Map<UUID, Map<String, String>> remoteAnimations = new ConcurrentHashMap<>();
    private static final Map<String, String> localAnimations = new ConcurrentHashMap<>();
    private static final GeckoPlayerAnimatable localAnimationTracker = new GeckoPlayerAnimatable();

    private static int ticksSinceJoin = -1;
    private static final int HANDSHAKE_TIMEOUT_TICKS = 60;

    public static void register() {

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ticksSinceJoin >= 0) {
                ticksSinceJoin++;
                if (ticksSinceJoin > HANDSHAKE_TIMEOUT_TICKS) {
                    ticksSinceJoin = -1;
                }
            }
            if (NetworkHandler.serverHasMod && client.player != null) {
                localAnimationTracker.syncLocalAnimationState(client.player);
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            NetworkHandler.serverHasMod = false;
            ticksSinceJoin = 0;
            remoteModels.clear();
            remoteAnimations.clear();
            localAnimations.clear();
            sendModelToServer();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            NetworkHandler.serverHasMod = false;
            ticksSinceJoin = -1;
            remoteModels.clear();
            remoteAnimations.clear();
            localAnimations.clear();
            localAnimationTracker.setPlayer(null);
        });

        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.CHANNEL,
                (client, handler, buf, responseSender) -> {
                    String json = buf.readString();
                    JsonObject msg = JsonParser.parseString(json).getAsJsonObject();
                    String type = msg.get("type").getAsString();

                    client.execute(() -> {
                        switch (type) {
                            case "handshake" -> {
                                NetworkHandler.serverHasMod = true;
                                ticksSinceJoin = -1;
                                sendModelToServer();
                            }
                            case "model_update" -> {
                                UUID uuid = UUID.fromString(msg.get("uuid").getAsString());
                                String modelData = msg.get("data").getAsString();
                                ModelConfig config = GSON.fromJson(modelData, ModelConfig.class);
                                if (config != null) {
                                    remoteModels.put(uuid, config);
                                }
                            }
                            case "animation_update" -> {
                                UUID uuid = UUID.fromString(msg.get("uuid").getAsString());
                                String controller = msg.get("controller").getAsString();
                                String animation = msg.has("animation") ? msg.get("animation").getAsString() : "";
                                remoteAnimations.computeIfAbsent(uuid, ignored -> new ConcurrentHashMap<>())
                                        .put(controller, animation);
                            }
                            case "player_remove" -> {
                                UUID uuid = UUID.fromString(msg.get("uuid").getAsString());
                                remoteModels.remove(uuid);
                                remoteAnimations.remove(uuid);
                            }
                        }
                    });
                });
    }

    public static void sendModelToServer() {
        ModelConfig config = ModelManager.getActiveModel();
        if (config == null)
            return;

        String modelJson = GSON.toJson(config);
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "model_update");
        msg.addProperty("data", modelJson);

        ClientPlayNetworking.send(NetworkHandler.CHANNEL,
                PacketByteBufs.create().writeString(GSON.toJson(msg)));
    }

    public static void sendAnimation(String controller, String animation) {
        if (!NetworkHandler.serverHasMod || controller == null || controller.isEmpty()
                || controller.length() > 32 || animation == null || animation.length() > 64) {
            return;
        }

        if (animation.equals(localAnimations.put(controller, animation))) {
            return;
        }

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "animation_update");
        msg.addProperty("controller", controller);
        msg.addProperty("animation", animation);

        ClientPlayNetworking.send(NetworkHandler.CHANNEL,
                PacketByteBufs.create().writeString(GSON.toJson(msg)));
    }

    public static boolean hasRemoteAnimation(UUID uuid, String controller) {
        Map<String, String> animations = remoteAnimations.get(uuid);
        return animations != null && animations.containsKey(controller);
    }

    public static String getRemoteAnimation(UUID uuid, String controller) {
        Map<String, String> animations = remoteAnimations.get(uuid);
        return animations == null ? null : animations.get(controller);
    }
}
