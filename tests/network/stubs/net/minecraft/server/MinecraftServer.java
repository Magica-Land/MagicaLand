package net.minecraft.server;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;

public final class MinecraftServer {
    private final Players players = new Players();
    public Players getPlayerManager() { return players; }
    public void execute(Runnable action) { action.run(); }
    public static final class Players {
        public final List<ServerPlayerEntity> online = new ArrayList<>();
        public List<ServerPlayerEntity> getPlayerList() { return List.copyOf(online); }
        public ServerPlayerEntity getPlayer(UUID uuid) {
            return online.stream().filter(player -> player.getUuid().equals(uuid)).findFirst().orElse(null);
        }
    }
}
