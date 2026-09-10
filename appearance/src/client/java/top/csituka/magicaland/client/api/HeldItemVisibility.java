package top.csituka.magicaland.client.api;

import java.util.UUID;
import java.util.function.Predicate;

public final class HeldItemVisibility {
    private static Predicate<UUID> externalMainHand = uuid -> false;
    private HeldItemVisibility() {}
    public static void setExternalMainHand(Predicate<UUID> provider) {
        externalMainHand = provider == null ? uuid -> false : provider;
    }
    public static boolean externalMainHand(UUID player) { return externalMainHand.test(player); }
}
