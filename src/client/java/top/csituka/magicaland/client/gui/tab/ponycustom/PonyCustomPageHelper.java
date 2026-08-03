package top.csituka.magicaland.client.gui.tab.ponycustom;

import java.util.function.Consumer;

import net.minecraft.text.Text;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.gui.widget.ColorPicker;
import top.csituka.magicaland.client.gui.widget.CustomButton;

public final class PonyCustomPageHelper {
    private PonyCustomPageHelper() {}

    public static CustomButton createStyleButton(int x, int y, int width, int height, Text label,
            String currentStyle, String[] styles, Consumer<String> onStyleChanged) {
        return new CustomButton(x, y, width, height,
                Text.literal(label.getString() + ": " + currentStyle),
                false,
                button -> {
                    String message = button.getMessage().getString();
                    String current = message.substring(message.lastIndexOf(": ") + 2);
                    String next = getNextStyle(current, styles);
                    button.setMessage(Text.literal(label.getString() + ": " + next));
                    onStyleChanged.accept(next);
                });
    }

    public static ColorPicker createBodyColorPicker(int x, int y, int width, int height, Text label,
            String initialColor, boolean initiallyLocked,
            Consumer<String> onColorChanged, Consumer<Boolean> onLockChanged) {
        final ColorPicker[] pickerHolder = new ColorPicker[1];

        ColorPicker picker = new ColorPicker(x, y, width, height, label, initialColor, newColor -> {
            onColorChanged.accept(newColor);
            if (pickerHolder[0] != null && pickerHolder[0].isLocked()) {
                syncLockedBodyColors(newColor);
            }
            ModelManager.saveActiveModel();
        });

        pickerHolder[0] = picker;
        ColorPicker.addToBodyLinkGroup(picker, initiallyLocked);
        picker.setLocked(initiallyLocked);
        picker.setOnLockChanged(locked -> {
            onLockChanged.accept(locked);
            ModelManager.saveActiveModel();
        });
        return picker;
    }

    private static String getNextStyle(String current, String[] styles) {
        for (int i = 0; i < styles.length; i++) {
            if (styles[i].equals(current)) {
                return styles[(i + 1) % styles.length];
            }
        }
        return styles[0];
    }

    private static void syncLockedBodyColors(String color) {
        ModelConfig config = ModelManager.getActiveModel();
        if (config == null) return;

        if (config.hornColorLocked) config.hornColor = color;
        if (config.bodyColorLocked) config.bodyColor = color;
        if (config.neckColorLocked) config.neckColor = color;
        if (config.headColorLocked) config.headColor = color;
        if (config.noseColorLocked) config.noseColor = color;
        if (config.leftEarColorLocked) config.leftEarColor = color;
        if (config.rightEarColorLocked) config.rightEarColor = color;
        if (config.leftFrontLimbColorLocked) config.leftFrontLimbColor = color;
        if (config.rightFrontLimbColorLocked) config.rightFrontLimbColor = color;
        if (config.leftHindLimbColorLocked) config.leftHindLimbColor = color;
        if (config.rightHindLimbColorLocked) config.rightHindLimbColor = color;
    }
}
