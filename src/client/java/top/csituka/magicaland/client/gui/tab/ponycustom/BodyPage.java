package top.csituka.magicaland.client.gui.tab.ponycustom;

import net.minecraft.text.Text;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.gui.widget.CustomButton;
import top.csituka.magicaland.client.gui.widget.SectionLabel;
import top.csituka.magicaland.client.gui.widget.SettingsList;

public class BodyPage implements PonyCustomPage {
    @Override
    public void build(PonyCustomPageContext context, SettingsList list) {
        ModelConfig config = ModelManager.getActiveModel();
        if (config == null) return;

        int buttonWidth = Math.min(180, context.getWidth() / 2);
        int buttonHeight = 20;
        int buttonX = getButtonX(context, buttonWidth);

        list.addWidget(new CustomButton(buttonX, 0, buttonWidth, buttonHeight,
                Text.literal("\u2190 " + Text.translatable("text.magicaland.config.body_menu.name").getString()),
                false, button -> context.openPage(PonyCustomPageContext.Page.MAIN, -1), false, true),
                SettingsList.Alignment.RIGHT);

        addColorPicker(list, buttonX, buttonWidth, buttonHeight,
                "text.magicaland.config.body_color.name", config.bodyColor, config.bodyColorLocked,
                color -> config.bodyColor = color, locked -> config.bodyColorLocked = locked);
        addColorPicker(list, buttonX, buttonWidth, buttonHeight,
                "text.magicaland.config.neck_color.name", config.neckColor, config.neckColorLocked,
                color -> config.neckColor = color, locked -> config.neckColorLocked = locked);
        addColorPicker(list, buttonX, buttonWidth, buttonHeight,
                "text.magicaland.config.head_color.name", config.headColor, config.headColorLocked,
                color -> config.headColor = color, locked -> config.headColorLocked = locked);

        list.addWidget(new SectionLabel(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.section.ears.name")), SettingsList.Alignment.RIGHT);
        addColorPicker(list, buttonX, buttonWidth, buttonHeight,
                "text.magicaland.config.left_ear_color.name", config.leftEarColor, config.leftEarColorLocked,
                color -> config.leftEarColor = color, locked -> config.leftEarColorLocked = locked);
        addColorPicker(list, buttonX, buttonWidth, buttonHeight,
                "text.magicaland.config.right_ear_color.name", config.rightEarColor, config.rightEarColorLocked,
                color -> config.rightEarColor = color, locked -> config.rightEarColorLocked = locked);

        list.addWidget(new SectionLabel(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.section.limbs.name")), SettingsList.Alignment.RIGHT);
        addColorPicker(list, buttonX, buttonWidth, buttonHeight,
                "text.magicaland.config.left_front_limb_color.name", config.leftFrontLimbColor,
                config.leftFrontLimbColorLocked,
                color -> config.leftFrontLimbColor = color, locked -> config.leftFrontLimbColorLocked = locked);
        addColorPicker(list, buttonX, buttonWidth, buttonHeight,
                "text.magicaland.config.right_front_limb_color.name", config.rightFrontLimbColor,
                config.rightFrontLimbColorLocked,
                color -> config.rightFrontLimbColor = color, locked -> config.rightFrontLimbColorLocked = locked);
        addColorPicker(list, buttonX, buttonWidth, buttonHeight,
                "text.magicaland.config.left_hind_limb_color.name", config.leftHindLimbColor,
                config.leftHindLimbColorLocked,
                color -> config.leftHindLimbColor = color, locked -> config.leftHindLimbColorLocked = locked);
        addColorPicker(list, buttonX, buttonWidth, buttonHeight,
                "text.magicaland.config.right_hind_limb_color.name", config.rightHindLimbColor,
                config.rightHindLimbColorLocked,
                color -> config.rightHindLimbColor = color, locked -> config.rightHindLimbColorLocked = locked);
    }

    private void addColorPicker(SettingsList list, int x, int width, int height, String labelKey,
            String color, boolean locked, java.util.function.Consumer<String> onColorChanged,
            java.util.function.Consumer<Boolean> onLockChanged) {
        list.addWidget(PonyCustomPageHelper.createBodyColorPicker(x, 0, width, height,
                Text.translatable(labelKey), color, locked, onColorChanged, onLockChanged),
                SettingsList.Alignment.RIGHT);
    }

    private int getButtonX(PonyCustomPageContext context, int buttonWidth) {
        if (context.getWidth() < 250) {
            return context.getX() + (context.getWidth() - buttonWidth) / 2;
        }
        return context.getX() + context.getWidth() - buttonWidth - 20;
    }
}
