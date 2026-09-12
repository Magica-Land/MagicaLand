package net.fabricmc.fabric.api.event.lifecycle.v1;

import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Event;
import net.minecraft.server.MinecraftServer;

public final class ServerTickEvents {
    public static final Event<Consumer<MinecraftServer>> END_SERVER_TICK = new Event<>();
}
