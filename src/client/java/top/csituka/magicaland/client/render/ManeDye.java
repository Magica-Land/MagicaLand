package top.csituka.magicaland.client.render;

import java.util.ArrayList;
import java.util.List;
import top.csituka.magicaland.client.config.ModelConfig;

public final class ManeDye {
    public static final int REGION_COUNT = 6;
    private ManeDye() {}

    private static String[] regionColors(ModelConfig config, ManePalette.Part part) {
        return switch (part) {
            case FRONT -> config.frontManeDyeColors;
            case BACK -> config.backManeDyeColors;
            case TAIL -> config.tailDyeColors;
        };
    }

    private static void validRegion(int region) {
        if (region < 0 || region >= REGION_COUNT) throw new IllegalArgumentException("Invalid mane region");
    }

    public static boolean linked(ModelConfig config, ManePalette.Part part, int region) {
        validRegion(region);
        String[] colors = regionColors(config, part);
        return colors == null || region >= colors.length || colors[region] == null;
    }

    public static ManePalette.Colors colors(ModelConfig config, ManePalette.Part part, int region) {
        validRegion(region);
        if (linked(config, part, region)) return ManePalette.colors(config, part);
        int color = BodyColorRamp.rgb(regionColors(config, part)[region]);
        // 相同主色复用整个色阶，手动阴影下也不留下隐形分区边界。
        if (color == ManePalette.base(config, part)) return ManePalette.colors(config, part);
        return new ManePalette.Colors(color, BodyColorRamp.automaticShadow(color), BodyColorRamp.automaticHighlight(color));
    }

    public static void setLinked(ModelConfig config, ManePalette.Part part, int region, boolean linked) {
        ModelConfig.sanitize(config);
        validRegion(region);
        String[] colors = regionColors(config, part);
        if (linked) colors[region] = null;
        else if (colors[region] == null) colors[region] = BodyPalette.hex(ManePalette.base(config, part));
    }

    public static void setColor(ModelConfig config, ManePalette.Part part, int region, String color) {
        ModelConfig.sanitize(config);
        validRegion(region);
        if (!linked(config, part, region)) {
            regionColors(config, part)[region] = color;
            ModelConfig.sanitize(config);
        }
    }

    public static void resetRegions(ModelConfig config, ManePalette.Part part) {
        ModelConfig.sanitize(config);
        java.util.Arrays.fill(regionColors(config, part), null);
    }

    public static List<ManePalette.Colors> palette(ModelConfig config, ManePalette.Part part, boolean enabled, boolean legacy) {
        List<ManePalette.Colors> result = new ArrayList<>();
        result.add(ManePalette.colors(config, part));
        if (enabled) for (int region = 0; region < REGION_COUNT; region++) result.add(colors(config, part, region));
        if (legacy) result.replaceAll(c -> new ManePalette.Colors(c.base(), c.base(), c.base()));
        return List.copyOf(result);
    }

    public static int recolor(int source, int channel, boolean legacy, int[] bases, int[][] ramps) {
        int slot = channel >= 0 && channel < bases.length ? channel : 0;
        return legacy ? tintAbgr(source, bases[slot]) : BodyColorRamp.recolorAbgr(source, ramps[slot]);
    }

    public static boolean supports(ModelConfig config, ManePalette.Part part) {
        if (config == null || part == null) return false;
        return "01".equals(switch (part) {
            case FRONT -> config.frontManeStyle;
            case BACK -> config.backManeStyle;
            case TAIL -> config.tailStyle;
        });
    }

    public static boolean enabled(ModelConfig config, ManePalette.Part part) {
        return supports(config, part) && config.maneDyeEnabled && "stripe01".equals(config.maneDyePreset);
    }

    public static boolean retiredOverlay(String name) {
        return "Style01FrontManeHighlight".equalsIgnoreCase(name);
    }

    public static int[] ramp(int color) {
        return BodyColorRamp.lookup(color, BodyColorRamp.automaticShadow(color), BodyColorRamp.automaticHighlight(color));
    }

    public static int tintAbgr(int pixel, int color) {
        if ((pixel >>> 24) == 0) return pixel;
        int r = (pixel & 255) * (color >> 16 & 255) / 255;
        int g = (pixel >> 8 & 255) * (color >> 8 & 255) / 255;
        int b = (pixel >> 16 & 255) * (color & 255) / 255;
        return pixel & 0xFF000000 | r | g << 8 | b << 16;
    }

    public static int recolor(int source, boolean dye, boolean legacy, int base, int dyeColor,
            int[] baseRamp, int[] dyeRamp) {
        if (legacy) return tintAbgr(source, dye ? dyeColor : base);
        return BodyColorRamp.recolorAbgr(source, dye ? dyeRamp : baseRamp);
    }

    public static int recolor(int source, int channel, boolean legacy, int base, int dyeColor, int accentColor,
            int[] baseRamp, int[] dyeRamp, int[] accentRamp) {
        if (channel == 2) return recolor(source, true, legacy, base, accentColor, baseRamp, accentRamp);
        return recolor(source, channel == 1, legacy, base, dyeColor, baseRamp, dyeRamp);
    }
}
