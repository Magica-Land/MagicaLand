package top.csituka.magicaland.client.config;

import java.util.Locale;

import top.csituka.magicaland.client.config.style.PonyStylePart;
import top.csituka.magicaland.client.config.style.PonyStyleRegistry;

public class ModelConfig {
    public String name = "default";

    public String frontManeStyle = PonyStyleRegistry.DEFAULT_ID;
    public String backManeStyle = PonyStyleRegistry.DEFAULT_ID;
    public String tailStyle = PonyStyleRegistry.DEFAULT_ID;
    public String eyeStyle = PonyStyleRegistry.DEFAULT_ID;

    public String hornColor = "#FFFFFFFF";
    public String wingColor = "#FFFFFFFF";
    public String bodyColor = "#FFFFFFFF";
    public String bodyShadingMode = "soft";
    public String bodyShadowColor = "#E3E3E3";
    public String bodyHighlightColor = "#FFFFFF";
    public boolean bodyShadowColorLocked = true;
    public boolean bodyHighlightColorLocked = true;
    public String neckColor = "#FFFFFFFF";
    public String headColor = "#FFFFFFFF";
    public String leftEarColor = "#FFFFFFFF";
    public String rightEarColor = "#FFFFFFFF";
    public String limbColor = "#FFFFFFFF";
    public String leftFrontLimbColor = "#FFFFFFFF";
    public String rightFrontLimbColor = "#FFFFFFFF";
    public String leftHindLimbColor = "#FFFFFFFF";
    public String rightHindLimbColor = "#FFFFFFFF";
    public String noseColor = "#FFFFFFFF";
    public String frontManeColor = "#FFFFFFFF";
    public String backManeColor = "#FFFFFFFF";
    public String tailColor = "#FFFFFFFF";
    public String maneShadingMode = "soft";
    public int maneColorLinkVersion = 0;
    public boolean backManeColorLocked = true;
    public boolean tailColorLocked = true;
    public String frontManeShadowColor = "#E3E3E3";
    public String frontManeHighlightColor = "#FFFFFF";
    public String backManeShadowColor = "#E3E3E3";
    public String backManeHighlightColor = "#FFFFFF";
    public String tailShadowColor = "#E3E3E3";
    public String tailHighlightColor = "#FFFFFF";
    public boolean frontManeShadowColorLocked = true;
    public boolean frontManeHighlightColorLocked = true;
    public boolean backManeShadowColorLocked = true;
    public boolean backManeHighlightColorLocked = true;
    public boolean tailShadowColorLocked = true;
    public boolean tailHighlightColorLocked = true;

    public boolean maneDyeEnabled = false;
    public String maneDyePreset = "stripe01";
    public String maneDyeColor = "#E45AA5";
    public String maneDyeAccentColor = "#71318F";
    public String[] frontManeDyeColors;
    public String[] backManeDyeColors;
    public String[] tailDyeColors;

    public String magicGlowColor = "#AA00FF";

    public boolean showHorn = true;
    public boolean showWings = false;

    public boolean hornColorLocked = true;
    public boolean wingColorLocked = true;
    public boolean bodyColorLocked = true;
    public boolean noseColorLocked = true;
    public boolean neckColorLocked = true;
    public boolean headColorLocked = true;
    public boolean leftEarColorLocked = true;
    public boolean rightEarColorLocked = true;
    public boolean leftFrontLimbColorLocked = true;
    public boolean rightFrontLimbColorLocked = true;
    public boolean leftHindLimbColorLocked = true;
    public boolean rightHindLimbColorLocked = true;

    public static ModelConfig sanitize(ModelConfig config) {
        if (config == null) {
            return null;
        }

        config.name = sanitizeText(config.name, "remote", 32);
        config.frontManeStyle = sanitizeStyle(config.frontManeStyle, PonyStylePart.FRONT_MANE);
        config.backManeStyle = sanitizeStyle(config.backManeStyle, PonyStylePart.BACK_MANE);
        config.tailStyle = sanitizeStyle(config.tailStyle, PonyStylePart.TAIL);
        config.eyeStyle = sanitizeStyle(config.eyeStyle, PonyStylePart.EYE);

        config.hornColor = sanitizeColor(config.hornColor, "#FFFFFFFF");
        config.wingColor = sanitizeColor(config.wingColor, "#FFFFFFFF");
        config.bodyColor = sanitizeColor(config.bodyColor, "#FFFFFFFF");
        if ("custom".equals(config.bodyShadingMode)) {
            config.bodyShadowColorLocked = false;
            config.bodyHighlightColorLocked = false;
        }
        if (!"legacy".equals(config.bodyShadingMode)) {
            config.bodyShadingMode = "soft";
        }
        config.bodyShadowColor = sanitizeColor(config.bodyShadowColor, "#E3E3E3");
        config.bodyHighlightColor = sanitizeColor(config.bodyHighlightColor, "#FFFFFF");
        config.neckColor = sanitizeColor(config.neckColor, "#FFFFFFFF");
        config.headColor = sanitizeColor(config.headColor, "#FFFFFFFF");
        config.leftEarColor = sanitizeColor(config.leftEarColor, "#FFFFFFFF");
        config.rightEarColor = sanitizeColor(config.rightEarColor, "#FFFFFFFF");
        config.limbColor = sanitizeColor(config.limbColor, "#FFFFFFFF");
        config.leftFrontLimbColor = sanitizeColor(config.leftFrontLimbColor, "#FFFFFFFF");
        config.rightFrontLimbColor = sanitizeColor(config.rightFrontLimbColor, "#FFFFFFFF");
        config.leftHindLimbColor = sanitizeColor(config.leftHindLimbColor, "#FFFFFFFF");
        config.rightHindLimbColor = sanitizeColor(config.rightHindLimbColor, "#FFFFFFFF");
        config.noseColor = sanitizeColor(config.noseColor, "#FFFFFFFF");
        config.frontManeColor = sanitizeColor(config.frontManeColor, "#FFFFFFFF");
        config.backManeColor = sanitizeColor(config.backManeColor, "#FFFFFFFF");
        config.tailColor = sanitizeColor(config.tailColor, "#FFFFFFFF");
        if (!"legacy".equals(config.maneShadingMode)) config.maneShadingMode = "soft";
        if (config.maneColorLinkVersion <= 0) {
            // 旧配置已有异色时保留独立颜色，只迁移一次。
            config.backManeColorLocked = sameRgb(config.frontManeColor, config.backManeColor);
            config.tailColorLocked = sameRgb(config.frontManeColor, config.tailColor);
        }
        config.maneColorLinkVersion = 1;
        if (config.backManeColorLocked) config.backManeColor = config.frontManeColor;
        if (config.tailColorLocked) config.tailColor = config.frontManeColor;
        config.frontManeShadowColor = sanitizeColor(config.frontManeShadowColor, "#E3E3E3");
        config.frontManeHighlightColor = sanitizeColor(config.frontManeHighlightColor, "#FFFFFF");
        config.backManeShadowColor = sanitizeColor(config.backManeShadowColor, "#E3E3E3");
        config.backManeHighlightColor = sanitizeColor(config.backManeHighlightColor, "#FFFFFF");
        config.tailShadowColor = sanitizeColor(config.tailShadowColor, "#E3E3E3");
        config.tailHighlightColor = sanitizeColor(config.tailHighlightColor, "#FFFFFF");
        if (!"stripe01".equals(config.maneDyePreset)) config.maneDyePreset = "stripe01";
        config.maneDyeColor = sanitizeColor(config.maneDyeColor, "#E45AA5");
        config.maneDyeAccentColor = sanitizeColor(config.maneDyeAccentColor, "#71318F");
        config.frontManeDyeColors = sanitizeDyeColors(config.frontManeDyeColors, config.maneDyeColor, config.maneDyeColor);
        config.backManeDyeColors = sanitizeDyeColors(config.backManeDyeColors, config.maneDyeColor, config.maneDyeAccentColor);
        config.tailDyeColors = sanitizeDyeColors(config.tailDyeColors, config.maneDyeColor, config.maneDyeColor);
        config.magicGlowColor = sanitizeColor(config.magicGlowColor, "#AA00FF");
        return config;
    }

    private static String sanitizeText(String value, String fallback, int maxLength) {
        if (value == null || value.isEmpty() || value.length() > maxLength) {
            return fallback;
        }
        return value;
    }

    /**
     * Validates a style field, transparently upgrading old character-abbreviation
     * save values (e.g. "RD") to the new numeric id along the way. Old and new
     * values never overlap ("RD" vs "02"), so this is safe to run unconditionally
     * on every load/sync, with no separate one-time migration step or version flag.
     */
    private static String sanitizeStyle(String value, PonyStylePart part) {
        String legacyMapped = PonyStyleRegistry.legacyCodeToId(part, value);
        String id = legacyMapped != null ? legacyMapped : value;
        return PonyStyleRegistry.isValidStyleId(part, id) ? id : PonyStyleRegistry.DEFAULT_ID;
    }

    private static String sanitizeColor(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String hex = value.startsWith("#") ? value.substring(1) : value;
        if ((hex.length() != 6 && hex.length() != 8) || !isHex(hex)) {
            return fallback;
        }
        return "#" + hex.toUpperCase(Locale.ROOT);
    }

    private static boolean isHex(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static String[] sanitizeDyeColors(String[] value, String oldDye, String oldAccent) {
        // null 色槽跟随部件主色；缺少数组的旧配置保留原挑染色。
        if (value == null) return new String[] {null, null, null, oldDye, oldAccent, null};
        if (value.length != 6) value = java.util.Arrays.copyOf(value, 6);
        for (int i = 0; i < value.length; i++)
            if (value[i] != null) value[i] = sanitizeColor(value[i], null);
        return value;
    }

    private static boolean sameRgb(String a, String b) {
        return a.substring(a.length() - 6).equals(b.substring(b.length() - 6));
    }
}
