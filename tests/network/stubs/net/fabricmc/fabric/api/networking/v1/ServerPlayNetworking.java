package net.fabricmc.fabric.api.networking.v1;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class ServerPlayNetworking {
    public static Receiver receiver;
    public interface Receiver {
        void receive(MinecraftServer server, ServerPlayerEntity player, Object handler,
                PacketByteBufs.Buffer buffer, Object sender);
    }
    public static void registerGlobalReceiver(Identifier channel, Receiver callback) { receiver = callback; }
    public static boolean canSend(ServerPlayerEntity player, Identifier channel) { return player.supportsChannel; }
    public static void send(ServerPlayerEntity player, Identifier channel, PacketByteBufs.Buffer buffer) {
        player.sent.add(buffer.readString(32767));
    }
}
