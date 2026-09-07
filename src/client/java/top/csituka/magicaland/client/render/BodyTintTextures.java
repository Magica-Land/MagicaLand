package top.csituka.magicaland.client.render;

import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.csituka.magicaland.client.config.ModelConfig;

public final class BodyTintTextures {
    private static final Identifier SOURCE = new Identifier("magicaland", "textures/entity/base.png");
    private static final Logger LOGGER = LoggerFactory.getLogger("magicaland/body-shading");
    private static final int MAX_ENTRIES = 128;
    private static final long MAX_BYTES = 64L * 1024 * 1024;
    private static final Map<Palette, Entry> CACHE = new LinkedHashMap<>(16, 0.75f, true);
    private static NativeImage source;
    private static long tick, bytes;
    private static boolean initialized, failed;

    private BodyTintTextures() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override public Identifier getFabricId() { return new Identifier("magicaland", "body_shading"); }
            @Override public void reload(ResourceManager manager) { MinecraftClient.getInstance().execute(BodyTintTextures::clear); }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tick++;
            Iterator<Entry> entries = CACHE.values().iterator();
            while (entries.hasNext()) {
                Entry entry = entries.next();
                if (tick - entry.used > 100 || ((CACHE.size() >= MAX_ENTRIES * 3 / 4 || bytes >= MAX_BYTES * 3 / 4) && tick - entry.used > 5)) {
                    client.getTextureManager().destroyTexture(entry.id);
                    bytes -= entry.bytes;
                    entries.remove();
                }
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> clear());
    }

    /** Null selects the original texture and vertex multiply, including on resource/cache failure. */
    public static Identifier get(ModelConfig config, String partColor) {
        if (config == null || partColor == null || "legacy".equals(config.bodyShadingMode) || failed) return null;
        int base = BodyColorRamp.rgb(partColor);
        Palette key = new Palette(base, BodyPalette.shadow(config, base), BodyPalette.highlight(config, base));
        Entry existing = CACHE.get(key);
        if (existing != null) { existing.used = tick; return existing.id; }
        NativeImage image = null;
        NativeImageBackedTexture texture = null;
        Identifier registered = null;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (source == null) {
                try (InputStream input = client.getResourceManager().getResourceOrThrow(SOURCE).getInputStream()) {
                    source = NativeImage.read(NativeImage.Format.RGBA, input);
                }
                if ((long) source.getWidth() * source.getHeight() > 1024 * 1024) {
                    source.close(); source = null;
                    throw new IllegalArgumentException("Body shading supports textures up to 1 megapixel");
                }
            }
            long cost = (long) source.getWidth() * source.getHeight() * 8;
            // Do not evict a texture while another model's buffered vertices can still reference it.
            if (CACHE.size() >= MAX_ENTRIES || bytes + cost > MAX_BYTES) return null;
            int[] ramp = BodyColorRamp.lookup(key.base, key.shadow, key.highlight);
            image = new NativeImage(NativeImage.Format.RGBA, source.getWidth(), source.getHeight(), false);
            for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++) {
                image.setColor(x, y, BodyColorRamp.recolorAbgr(source.getColor(x, y), ramp));
            }
            texture = new NativeImageBackedTexture(image);
            image = null;
            registered = client.getTextureManager().registerDynamicTexture("magicaland_body", texture);
            texture.setFilter(false, false);
            texture.upload();
            CACHE.put(key, new Entry(registered, tick, cost));
            bytes += cost;
            return registered;
        } catch (Exception exception) {
            if (registered != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(registered);
            else if (texture != null) texture.close();
            else if (image != null) image.close();
            failed = true;
            LOGGER.warn("Body palette unavailable; using legacy tint until resources reload", exception);
            return null;
        }
    }

    public static String colorForBone(ModelConfig config, String name) {
        if (config == null) return null;
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "body" -> config.bodyColor; case "neck" -> config.neckColor;
            case "head" -> config.headColor; case "nose" -> config.noseColor;
            case "leftear" -> config.leftEarColor; case "rightear" -> config.rightEarColor;
            case "horn" -> config.hornColor;
            default -> name.startsWith("LFront") || name.equalsIgnoreCase("LForeLeg") ? config.leftFrontLimbColor
                    : name.startsWith("RFront") || name.equalsIgnoreCase("RForeLeg") ? config.rightFrontLimbColor
                    : name.startsWith("LHind") ? config.leftHindLimbColor
                    : name.startsWith("RHind") ? config.rightHindLimbColor
                    : name.toLowerCase(Locale.ROOT).contains("wing") ? config.wingColor : null;
        };
    }

    private static void clear() {
        for (Entry entry : CACHE.values()) MinecraftClient.getInstance().getTextureManager().destroyTexture(entry.id);
        CACHE.clear(); bytes = 0; failed = false;
        if (source != null) { source.close(); source = null; }
    }

    private record Palette(int base, int shadow, int highlight) {}
    private static final class Entry {
        final Identifier id; final long bytes; long used;
        Entry(Identifier id, long used, long bytes) { this.id = id; this.used = used; this.bytes = bytes; }
    }
}
