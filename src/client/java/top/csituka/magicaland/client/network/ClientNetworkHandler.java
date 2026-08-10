package top.csituka.magicaland.client.network;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
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
    private static final Map<String, String> pendingAnimations = new ConcurrentHashMap<>();
    private static final GeckoPlayerAnimatable localAnimationTracker = new GeckoPlayerAnimatable();

    private static int ticksSinceJoin = -1;
    private static final int HANDSHAKE_TIMEOUT_TICKS = 60;
    private static final long MODEL_SEND_INTERVAL_NANOS = 100_000_000L;
    private static final long ANIMATION_SEND_INTERVAL_NANOS = 50_000_000L;
    private static String lastSentModelJson;
    private static String pendingModelJson;
    private static long lastModelSendNanos;
    private static long lastAnimationSendNanos;

    public static void register() {

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            flushPendingModel();
            flushPendingAnimations();

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
            pendingAnimations.clear();
            lastSentModelJson = null;
            pendingModelJson = null;
            lastModelSendNanos = 0L;
            lastAnimationSendNanos = 0L;
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            NetworkHandler.serverHasMod = false;
            ticksSinceJoin = -1;
            remoteModels.clear();
            remoteAnimations.clear();
            localAnimations.clear();
            pendingAnimations.clear();
            lastSentModelJson = null;
            pendingModelJson = null;
            lastModelSendNanos = 0L;
            lastAnimationSendNanos = 0L;
            localAnimationTracker.setPlayer(null);
        });

        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.CHANNEL,
                (client, handler, buf, responseSender) -> {
                    String json;
                    try {
                        json = buf.readString(NetworkHandler.MAX_MESSAGE_LENGTH);
                    } catch (RuntimeException ignored) {
                        return;
                    }

                    JsonObject msg;
                    try {
                        JsonElement parsed = JsonParser.parseString(json);
                        if (!parsed.isJsonObject()) {
                            return;
                        }
                        msg = parsed.getAsJsonObject();
                    } catch (RuntimeException ignored) {
                        return;
                    }

                    client.execute(() -> handleMessage(msg));
                });
    }

    public static void sendModelToServer() {
        if (!NetworkHandler.serverHasMod || !ClientPlayNetworking.canSend(NetworkHandler.CHANNEL)) {
            return;
        }

        ModelConfig config = ModelManager.getActiveModel();
        if (config == null)
            return;

        String modelJson = GSON.toJson(config);
        if (modelJson.length() > NetworkHandler.MAX_MODEL_DATA_LENGTH) {
            return;
        }
        if (modelJson.equals(lastSentModelJson)) {
            pendingModelJson = null;
            return;
        }

        long now = System.nanoTime();
        if (now - lastModelSendNanos < MODEL_SEND_INTERVAL_NANOS) {
            pendingModelJson = modelJson;
            return;
        }

        sendModelPacket(modelJson);
    }

    public static void sendAnimation(String controller, String animation) {
        if (!NetworkHandler.serverHasMod || !NetworkHandler.isAllowedAnimation(controller, animation)
                || !ClientPlayNetworking.canSend(NetworkHandler.CHANNEL)) {
            return;
        }

        if (animation.equals(localAnimations.get(controller))) {
            return;
        }

        localAnimations.put(controller, animation);
        pendingAnimations.put(controller, animation);
        flushPendingAnimations();
    }

    public static boolean hasRemoteAnimation(UUID uuid, String controller) {
        Map<String, String> animations = remoteAnimations.get(uuid);
        return animations != null && animations.containsKey(controller);
    }

    public static String getRemoteAnimation(UUID uuid, String controller) {
        Map<String, String> animations = remoteAnimations.get(uuid);
        return animations == null ? null : animations.get(controller);
    }

    private static void handleMessage(JsonObject msg) {
        try {
            String type = readString(msg, "type", 32);
            if (type == null) {
                return;
            }

            switch (type) {
                case "handshake" -> {
                    NetworkHandler.serverHasMod = true;
                    ticksSinceJoin = -1;
                    sendModelToServer();
                }
                case "model_update" -> {
                    UUID uuid = readUuid(msg, "uuid");
                    String modelData = readString(msg, "data", NetworkHandler.MAX_MODEL_DATA_LENGTH);
                    ModelConfig config = parseRemoteModel(modelData);
                    if (uuid != null && config != null) {
                        remoteModels.put(uuid, config);
                    }
                }
                case "animation_update" -> {
                    UUID uuid = readUuid(msg, "uuid");
                    String controller = readString(msg, "controller", 32);
                    String animation = msg.has("animation")
                            ? readString(msg, "animation", NetworkHandler.MAX_ANIMATION_LENGTH)
                            : "";
                    if (uuid != null && NetworkHandler.isAllowedAnimation(controller, animation)) {
                        remoteAnimations.computeIfAbsent(uuid, ignored -> new ConcurrentHashMap<>())
                                .put(controller, animation);
                    }
                }
                case "player_remove" -> {
                    UUID uuid = readUuid(msg, "uuid");
                    if (uuid != null) {
                        remoteModels.remove(uuid);
                        remoteAnimations.remove(uuid);
                    }
                }
                default -> {
                }
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static ModelConfig parseRemoteModel(String modelData) {
        if (modelData == null) {
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(modelData);
            if (!parsed.isJsonObject()) {
                return null;
            }
            return ModelConfig.sanitize(GSON.fromJson(parsed, ModelConfig.class));
        } catch (RuntimeException ignored) {
            return null;
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

    private static UUID readUuid(JsonObject object, String name) {
        String value = readString(object, name, 36);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void flushPendingModel() {
        if (pendingModelJson == null || !NetworkHandler.serverHasMod
                || !ClientPlayNetworking.canSend(NetworkHandler.CHANNEL)) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastModelSendNanos < MODEL_SEND_INTERVAL_NANOS) {
            return;
        }

        String modelJson = pendingModelJson;
        pendingModelJson = null;
        if (!modelJson.equals(lastSentModelJson)) {
            sendModelPacket(modelJson);
        }
    }

    private static void sendModelPacket(String modelJson) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "model_update");
        msg.addProperty("data", modelJson);

        try {
            ClientPlayNetworking.send(NetworkHandler.CHANNEL,
                    PacketByteBufs.create().writeString(GSON.toJson(msg), NetworkHandler.MAX_MESSAGE_LENGTH));
            lastSentModelJson = modelJson;
            lastModelSendNanos = System.nanoTime();
        } catch (RuntimeException ignored) {
            pendingModelJson = modelJson;
        }
    }

    private static void flushPendingAnimations() {
        if (pendingAnimations.isEmpty() || !NetworkHandler.serverHasMod
                || !ClientPlayNetworking.canSend(NetworkHandler.CHANNEL)) {
            return;
        }

        long now = System.nanoTime();
        if (now - lastAnimationSendNanos < ANIMATION_SEND_INTERVAL_NANOS) {
            return;
        }

        Map.Entry<String, String> entry = pendingAnimations.entrySet().iterator().next();
        String controller = entry.getKey();
        String animation = entry.getValue();
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "animation_update");
        msg.addProperty("controller", controller);
        msg.addProperty("animation", animation);

        try {
            ClientPlayNetworking.send(NetworkHandler.CHANNEL,
                    PacketByteBufs.create().writeString(GSON.toJson(msg), NetworkHandler.MAX_MESSAGE_LENGTH));
            pendingAnimations.remove(controller, animation);
            lastAnimationSendNanos = System.nanoTime();
        } catch (RuntimeException ignored) {
        }
    }
}
