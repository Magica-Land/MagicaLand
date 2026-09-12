package net.minecraft.server.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ServerPlayerEntity {
    private final UUID uuid;
    public final List<String> sent = new ArrayList<>();
    public boolean supportsChannel = true;
    public ServerPlayerEntity(UUID uuid) { this.uuid = uuid; }
    public UUID getUuid() { return uuid; }
}
