package top.csituka.magicaland.client.api;

import java.util.UUID;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.slf4j.LoggerFactory;
import top.csituka.magicaland.api.client.AppearanceOverrides.Visibility;
import top.csituka.magicaland.api.client.Registration;

public final class AppearanceOverrideState {
    private static final OverrideRegistry<Visibility> VISIBILITY = new OverrideRegistry<>(AppearanceOverrideState::failed);
    private static final OverrideRegistry<Entity> GAZE = new OverrideRegistry<>(AppearanceOverrideState::failed);
    private static boolean initialized;

    private AppearanceOverrideState() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            VISIBILITY.clear();
            GAZE.clear();
        });
    }

    public static Registration registerVisibility(String ownerId, int priority, Function<UUID, Visibility> provider) {
        return VISIBILITY.register(ownerId, priority, provider);
    }

    public static Registration registerGaze(String ownerId, int priority, Function<UUID, Entity> provider) {
        return GAZE.register(ownerId, priority, provider);
    }

    public static void unregisterOwner(String ownerId) {
        VISIBILITY.unregisterOwner(ownerId);
        GAZE.unregisterOwner(ownerId);
    }

    public static Visibility visibility(UUID player) {
        return VISIBILITY.resolve(player, value -> value != Visibility.DEFAULT, Visibility.DEFAULT);
    }

    public static Entity gaze(UUID player) {
        var world = MinecraftClient.getInstance().world;
        return GAZE.resolve(player, target -> !target.isRemoved() && target.getWorld() == world, null);
    }

    private static void failed(String ownerId, RuntimeException failure) {
        LoggerFactory.getLogger(AppearanceOverrideState.class).warn(
                "Removed failing appearance override from " + ownerId, failure);
    }
}
