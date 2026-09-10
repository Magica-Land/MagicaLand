package top.csituka.magicaland.api.client;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.entity.Entity;
import top.csituka.magicaland.client.api.AppearanceOverrideState;

/** Client-thread overrides. Higher priority wins; ties prefer the earlier registration. */
public final class AppearanceOverrides {
    public enum Visibility { DEFAULT, HIDDEN, VISIBLE }

    private AppearanceOverrides() {}

    /** null yields to lower priorities and finally the saved horn/wing choices. Since API 1.4. */
    public static Registration registerAnatomy(String ownerId, int priority,
            Function<UUID, AnatomyOverride> provider) {
        return AppearanceOverrideState.registerAnatomy(ownerId, priority, provider);
    }

    /** DEFAULT (or null) yields to lower priorities and finally normal held-item rendering. */
    public static Registration registerMainHandVisibility(String ownerId, int priority,
            Function<UUID, Visibility> provider) {
        return AppearanceOverrideState.registerVisibility(ownerId, priority, provider);
    }

    /** null, removed entities and targets outside the current world yield to lower priorities. */
    public static Registration registerGaze(String ownerId, int priority, Function<UUID, Entity> provider) {
        return AppearanceOverrideState.registerGaze(ownerId, priority, provider);
    }

    /** true requests horn glow; false yields and cannot suppress normal held-item glow. Since API 1.2. */
    public static Registration registerMagicActivity(String ownerId, int priority, Predicate<UUID> provider) {
        return AppearanceOverrideState.registerMagicActivity(ownerId, priority, provider);
    }

    /** Removes this owner's registrations in all channels; other owners are preserved. */
    public static void unregisterOwner(String ownerId) {
        AppearanceOverrideState.unregisterOwner(ownerId);
    }

    public static Visibility mainHandVisibility(UUID player) {
        return AppearanceOverrideState.visibility(player);
    }

    public static Entity gazeTarget(UUID player) {
        return AppearanceOverrideState.gaze(player);
    }

    /** Whether an addon requests magic activity; normal equipment and appearance eligibility are separate. */
    public static boolean magicActive(UUID player) {
        return AppearanceOverrideState.magicActive(player);
    }
}
