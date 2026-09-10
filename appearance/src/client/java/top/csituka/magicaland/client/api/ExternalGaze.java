package top.csituka.magicaland.client.api;

import java.util.UUID;
import java.util.function.Function;
import net.minecraft.entity.Entity;

public final class ExternalGaze {
    private static Function<UUID,Entity> provider=id -> null;
    private ExternalGaze() {}
    public static void setProvider(Function<UUID,Entity> value) { provider=value==null?id -> null:value; }
    public static Entity target(UUID owner) {
        Entity target=provider.apply(owner);
        return target==null || target.isRemoved()?null:target;
    }
}
