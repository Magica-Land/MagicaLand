package net.fabricmc.fabric.api.networking.v1;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ServerPlayConnectionEvents {
    public static final Event<Join> JOIN = new Event<>();
    public static final Event<Disconnect> DISCONNECT = new Event<>();
    public interface Join { void onJoin(Handler handler, Object sender, MinecraftServer server); }
    public interface Disconnect { void onDisconnect(Handler handler, MinecraftServer server); }
    public record Handler(ServerPlayerEntity player) {
        public ServerPlayerEntity getPlayer() { return player; }
    }
    public static final class Event<T> {
        public T listener;
        public void register(T callback) { listener = callback; }
    }
}
