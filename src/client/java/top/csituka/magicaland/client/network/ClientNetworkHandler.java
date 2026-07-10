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
import top.csituka.magicaland.network.NetworkHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientNetworkHandler {

    private static final Gson GSON = new Gson();

    public static final Map<UUID, ModelConfig> remoteModels = new ConcurrentHashMap<>();

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
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            NetworkHandler.serverHasMod = false;
            ticksSinceJoin = 0;
            remoteModels.clear();
            sendModelToServer();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            NetworkHandler.serverHasMod = false;
            ticksSinceJoin = -1;
            remoteModels.clear();
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
                            case "player_remove" -> {
                                UUID uuid = UUID.fromString(msg.get("uuid").getAsString());
                                remoteModels.remove(uuid);
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
}
