import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.style.PonyStylePart;
import top.csituka.magicaland.client.config.style.PonyStyleRegistry;
import top.csituka.magicaland.client.render.ManeDye;
import top.csituka.magicaland.client.render.ManeDyeMask;
import top.csituka.magicaland.client.render.ManePalette;
import top.csituka.magicaland.client.render.ManePalette.Part;

public class ManeDyeRoutingTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static void rejects(Runnable action, String message) {
        boolean rejected = false;
        try { action.run(); } catch (RuntimeException expected) { rejected = true; }
        check(rejected, message);
    }
    private static PonyStylePart registryPart(Part part) {
        return switch (part) {
            case FRONT -> PonyStylePart.FRONT_MANE;
            case BACK -> PonyStylePart.BACK_MANE;
            case TAIL -> PonyStylePart.TAIL;
        };
    }
    private static void select(ModelConfig config, Part part, String id) {
        switch (part) {
            case FRONT -> config.frontManeStyle = id;
            case BACK -> config.backManeStyle = id;
            case TAIL -> config.tailStyle = id;
        }
    }
    private static ModelConfig config() {
        ModelConfig config = ModelConfig.sanitize(new ModelConfig());
        config.maneDyeEnabled = true;
        config.backManeColorLocked = true;
        config.tailColorLocked = true;
        for (Part part : Part.values()) ManeDye.resetRegions(config, part);
        return config;
    }
    private static String mask(String preset, String part, int channel) {
        return "{\"version\":3,\"preset\":\"" + preset
                + "\",\"texture_width\":2,\"texture_height\":2,\"regions\":[{\"part\":\""
                + part + "\",\"channel\":" + channel + ",\"runs\":[[0,0,2]]}]}";
    }
    private static ManeDyeMask read(String preset, String part, int channel) {
        return ManeDyeMask.read(new StringReader(mask(preset, part, channel)), preset);
    }

    public static void main(String[] args) throws Exception {
        ModelConfig config = config();
        Set<ManeDye.TextureKey> keys = new HashSet<>();
        ManeDye.TextureKey plain = ManeDye.textureKey(config, Part.FRONT, false, false);
        int combinations = 0;
        for (Part part : Part.values()) {
            for (var style : PonyStyleRegistry.stylesFor(registryPart(part))) {
                combinations++;
                select(config, part, style.id);
                String expected = "01".equals(style.id) ? "stripe01" : "style" + style.id;
                check(ManeDye.supports(config, part) && ManeDye.enabled(config, part), "Registered style not supported");
                check(expected.equals(ManeDye.maskName(config, part)), "Wrong resource for " + part + style.id);
                var key = ManeDye.textureKey(config, part, true, false);
                check(expected.equals(key.maskName()) && key.dyePart() == part, "Masked cache lost route identity");
                check(key.colors().size() == 7, "Masked cache lost a color slot");
                check(keys.add(key), "Equal colors collided between styles or parts");
                check(plain.equals(ManeDye.textureKey(config, part, false, false)), "Plain equal-color texture cannot share");
                check(!key.equals(ManeDye.textureKey(config, part, true, true)), "Legacy/soft cache collision");
                ManeDyeMask mask = read(expected, part.name(), 6);
                check(mask.hasPart(part) && mask.channel(part, 0, 0, 2, 2) == 6, "Sixth channel unavailable");
                for (Part other : Part.values()) if (other != part) {
                    check(!mask.hasPart(other) && mask.channel(other, 0, 0, 2, 2) == 0, "Missing part should remain plain");
                }
            }
            select(config, part, "01");
        }
        check(combinations == 22 && keys.size() == 22, "Enabled part inventory changed; audit resources");

        for (var front : PonyStyleRegistry.stylesFor(PonyStylePart.FRONT_MANE)) {
            for (var back : PonyStyleRegistry.stylesFor(PonyStylePart.BACK_MANE)) {
                for (var tail : PonyStyleRegistry.stylesFor(PonyStylePart.TAIL)) {
                    config.frontManeStyle = front.id;
                    config.backManeStyle = back.id;
                    config.tailStyle = tail.id;
                    var frontKey = ManeDye.textureKey(config, Part.FRONT, true, false);
                    var backKey = ManeDye.textureKey(config, Part.BACK, true, false);
                    var tailKey = ManeDye.textureKey(config, Part.TAIL, true, false);
                    check(!frontKey.equals(backKey) && !frontKey.equals(tailKey) && !backKey.equals(tailKey), "Mixed-style cache collision");
                    check(keys.contains(frontKey) && keys.contains(backKey) && keys.contains(tailKey), "Mixed-style routing differs from standalone");
                    ManeDye.setLinked(config, Part.BACK, 1, false);
                    ManeDye.setColor(config, Part.BACK, 1, "#DDCC33");
                    check(frontKey.equals(ManeDye.textureKey(config, Part.FRONT, true, false))
                            && tailKey.equals(ManeDye.textureKey(config, Part.TAIL, true, false)), "Back color changed other-part keys");
                    check(!backKey.equals(ManeDye.textureKey(config, Part.BACK, true, false)), "Back edit failed to invalidate its key");
                    ManeDye.resetRegions(config, Part.BACK);
                }
            }
        }
        config = config();
        for (String invalid : new String[]{null, "", "00", "09", "1", "TS", "../../stripe01"}) {
            for (Part part : Part.values()) {
                select(config, part, invalid);
                check(!ManeDye.supports(config, part) && !ManeDye.enabled(config, part)
                        && ManeDye.maskName(config, part) == null, "Unregistered style accepted");
                check(plain.equals(ManeDye.textureKey(config, part, true, false)), "Unsupported style produced masked texture");
                select(config, part, "01");
            }
        }
        config.backManeStyle = "07"; config.tailStyle = "08";
        check(!ManeDye.supports(config, Part.BACK) && !ManeDye.supports(config, Part.TAIL), "Deferred geometry unexpectedly supported");
        check(!ManeDye.supports(null, Part.FRONT) && !ManeDye.supports(config, null), "Null style route accepted");
        rejects(() -> read("style07", "BACK", 1), "Absent 07 back mask accepted");
        rejects(() -> read("style08", "TAIL", 1), "Absent 08 tail mask accepted");
        rejects(() -> read("style01", "FRONT", 1), "01 resource alias silently changed");
        rejects(() -> read("style09", "FRONT", 1), "Unknown mask accepted");
        rejects(() -> ManeDyeMask.read(new StringReader(mask("style02", "FRONT", 1)), "style05"), "Misnamed resource accepted");
        rejects(() -> ManeDyeMask.read(new StringReader(mask("style02", "FRONT", 1).replace("\"version\":3", "\"version\":2"))), "New style accepted legacy channel interpretation");
        check(!ManeDye.retiredOverlay("Style07FrontManeHighlight") && !ManeDye.retiredOverlay("Style07FrontMane02"), "Valid 07 geometry retired");

        config = config();
        config.frontManeDyeColors[0] = "#FF0000";
        config.frontManeDyeColors[5] = "#0000FF";
        Map<ManeDye.TextureKey, Integer> textureCache = new HashMap<>();
        for (String id : new String[]{"02", "05", "02", "05"}) {
            config.frontManeStyle = id;
            int channel = "02".equals(id) ? 1 : 6;
            ManeDyeMask mask = read("style" + id, "FRONT", channel);
            var key = ManeDye.textureKey(config, Part.FRONT, true, true);
            int color = textureCache.computeIfAbsent(key, ignored -> {
                int[] bases = key.colors().stream().mapToInt(ManePalette.Colors::base).toArray();
                return ManeDye.recolor(0xFFFFFFFF, mask.channel(Part.FRONT, 0, 0, 2, 2), true, bases, null);
            });
            check(color == (channel == 1 ? 0xFF0000FF : 0xFFFF0000), "Shared atlas reused another style's dye pixels");
        }
        check(textureCache.size() == 2, "Same style route not reused");
        var maskedKey = ManeDye.textureKey(config, Part.FRONT, true, false);
        var mutableColors = new ArrayList<>(maskedKey.colors());
        var defensiveKey = new ManeDye.TextureKey(mutableColors, false, Part.FRONT, maskedKey.maskName());
        mutableColors.clear();
        check(defensiveKey.equals(maskedKey), "Caller can mutate a stored key");
        rejects(() -> defensiveKey.colors().clear(), "Cache exposes mutable colors");
        config.maneDyeEnabled = false;
        check(plain.equals(ManeDye.textureKey(config, Part.FRONT, true, false)), "Disabled dye kept mask route/colors in cache");
        config.maneDyeEnabled = true; config.maneDyePreset = "style02";
        check(!ManeDye.enabled(config, Part.FRONT), "Resource identity replaced config preset semantics");

        if (args.length > 0) verifyResources(Path.of(args[0]), args.length > 1 ? Path.of(args[1]) : null);
        System.out.println("PASS " + checks + " mane style/mixed/cache/resource routing assertions; 22 supported parts, 392 mixed outfits.");
    }

    private static void verifyResources(Path directory, Path fallback) throws Exception {
        ModelConfig config = config();
        Map<String, ManeDyeMask> masks = new HashMap<>();
        for (Part part : Part.values()) for (var style : PonyStyleRegistry.stylesFor(registryPart(part))) {
            select(config, part, style.id);
            String name = ManeDye.maskName(config, part);
            if (!masks.containsKey(name)) {
                Path file = directory.resolve(name + ".json");
                if (!Files.exists(file) && fallback != null) file = fallback.resolve(name + ".json");
                check(Files.size(file) <= 262144, "Resource exceeds runtime byte limit");
                try (var reader = Files.newBufferedReader(file)) { masks.put(name, ManeDyeMask.read(reader, name)); }
            }
            ManeDyeMask mask = masks.get(name);
            check(mask.hasPart(part) && mask.compatible(256, 256), "Registered part missing its compatible mask");
            int[] counts = new int[7];
            for (int y = 0; y < 256; y++) for (int x = 0; x < 256; x++) {
                int channel = mask.channel(part, x, y, 256, 256);
                counts[channel]++;
                check(channel == mask.channel(part, x * 2, y * 2, 512, 512), "Resource scale routing changed");
            }
            for (int channel = 1; channel <= 6; channel++) check(counts[channel] > 0, "Registered part lost region " + channel);
        }
        check(masks.size() == 8 && !masks.get("style07").hasPart(Part.BACK) && !masks.get("style08").hasPart(Part.TAIL), "Mask inventory differs from registered geometry");
    }
}
