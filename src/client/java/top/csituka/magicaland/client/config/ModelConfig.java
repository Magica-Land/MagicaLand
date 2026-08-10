package top.csituka.magicaland.client.config;

import java.util.Locale;
import java.util.Set;

public class ModelConfig {
    public String name = "default";
    
    public String frontManeStyle = "TS";
    public String backManeStyle = "TS";
    public String tailStyle = "TS";
    public String eyeStyle = "TS";

    public String hornColor = "#FFFFFFFF";
    public String wingColor = "#FFFFFFFF";
    public String bodyColor = "#FFFFFFFF";
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

    private static final Set<String> MANE_STYLES = Set.of("TS", "RD", "RR", "PP", "AJ", "FS");
    private static final Set<String> EYE_STYLES = Set.of("TS", "FS", "RR");

    public static ModelConfig sanitize(ModelConfig config) {
        if (config == null) {
            return null;
        }

        config.name = sanitizeText(config.name, "remote", 32);
        config.frontManeStyle = sanitizeStyle(config.frontManeStyle, MANE_STYLES, "TS");
        config.backManeStyle = sanitizeStyle(config.backManeStyle, MANE_STYLES, "TS");
        config.tailStyle = sanitizeStyle(config.tailStyle, MANE_STYLES, "TS");
        config.eyeStyle = sanitizeStyle(config.eyeStyle, EYE_STYLES, "TS");

        config.hornColor = sanitizeColor(config.hornColor, "#FFFFFFFF");
        config.wingColor = sanitizeColor(config.wingColor, "#FFFFFFFF");
        config.bodyColor = sanitizeColor(config.bodyColor, "#FFFFFFFF");
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

    private static String sanitizeStyle(String value, Set<String> allowed, String fallback) {
        return value != null && allowed.contains(value) ? value : fallback;
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
