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
}
