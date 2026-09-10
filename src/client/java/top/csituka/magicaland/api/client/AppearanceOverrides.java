package top.csituka.magicaland.api.client;

import java.util.UUID;
import java.util.function.Function;
import net.minecraft.entity.Entity;
import top.csituka.magicaland.client.api.AppearanceOverrideState;

/** Client-thread overrides. Higher priority wins; ties prefer the earlier registration. */
public final class AppearanceOverrides {
    public enum Visibility { DEFAULT, HIDDEN, VISIBLE }

    private AppearanceOverrides() {}

    /** DEFAULT (or null) yields to lower priorities and finally normal held-item rendering. */
    public static Registration registerMainHandVisibility(String ownerId, int priority,
            Function<UUID, Visibility> provider) {
        return AppearanceOverrideState.registerVisibility(ownerId, priority, provider);
    }

    /** null, removed entities and targets outside the current world yield to lower priorities. */
    public static Registration registerGaze(String ownerId, int priority, Function<UUID, Entity> provider) {
        return AppearanceOverrideState.registerGaze(ownerId, priority, provider);
    }

    /** Removes this owner's registrations in both channels; other owners are preserved. */
    public static void unregisterOwner(String ownerId) {
        AppearanceOverrideState.unregisterOwner(ownerId);
    }

    public static Visibility mainHandVisibility(UUID player) {
        return AppearanceOverrideState.visibility(player);
    }

    public static Entity gazeTarget(UUID player) {
        return AppearanceOverrideState.gaze(player);
    }
}
