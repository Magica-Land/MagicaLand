package top.csituka.magicaland.client.gui.ponycustom;

import net.minecraft.text.Text;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.gui.widget.CustomButton;
import top.csituka.magicaland.client.gui.widget.ColorPicker;
import top.csituka.magicaland.client.render.BodyColorRamp;
import top.csituka.magicaland.client.render.BodyPalette;
import top.csituka.magicaland.client.gui.widget.SectionLabel;
import top.csituka.magicaland.client.gui.widget.SettingsList;

public class BodyPage implements PonyCustomPage {
    private boolean shadingExpanded;

    @Override
    public void onEnter() {
        shadingExpanded = false;
    }

    @Override
    public void build(PonyCustomPageContext context, SettingsList list) {
        ModelConfig config = ModelManager.getActiveModel();
        if (config == null) return;

        int buttonWidth = context.getControlWidth();
        int buttonHeight = 20;
        int buttonX = getButtonX(context, buttonWidth);


        addShadingMode(context, list, config, buttonX, buttonWidth, buttonHeight);
        ColorPicker body = addColorPicker(list, buttonX, buttonWidth, buttonHeight,
                "text.magicaland.config.body_color.name", config.bodyColor, config.bodyColorLocked,
                color -> config.bodyColor = color, locked -> config.bodyColorLocked = locked);
        if (!"legacy".equals(config.bodyShadingMode)) {
            body.setDisclosure(shadingExpanded, expanded -> {
                shadingExpanded = expanded;
                context.refreshKeepingScroll();
            });
            if (shadingExpanded) {
                addShadingControls(context, list, config, buttonX + 12, buttonWidth - 12, buttonHeight);
            }
        }
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

    private ColorPicker addColorPicker(SettingsList list, int x, int width, int height, String labelKey,
            String color, boolean locked, java.util.function.Consumer<String> onColorChanged,
            java.util.function.Consumer<Boolean> onLockChanged) {
        ColorPicker picker = PonyCustomPageHelper.createBodyColorPicker(x, 0, width, height,
                Text.translatable(labelKey), color, locked, onColorChanged, onLockChanged);
        list.addWidget(picker, SettingsList.Alignment.RIGHT);
        return picker;
    }

    private void addShadingMode(PonyCustomPageContext context, SettingsList list, ModelConfig config,
            int x, int width, int height) {
        CustomButton mode = new CustomButton(x, 0, width, height,
                Text.translatable("text.magicaland.config.body_shading.name"),
                Text.translatable("text.magicaland.config.body_shading." + config.bodyShadingMode).getString(), false,
                button -> {
                    config.bodyShadingMode = "legacy".equals(config.bodyShadingMode) ? "soft" : "legacy";
                    ModelManager.saveActiveModel();
                    context.refreshKeepingScroll();
                });
        list.addWidget(mode, SettingsList.Alignment.RIGHT);
    }

    private void addShadingControls(PonyCustomPageContext context, SettingsList list, ModelConfig config,
            int x, int width, int height) {
        int base = BodyColorRamp.rgb(config.bodyColor);
        ColorPicker shadow = new ColorPicker(x, 0, width, height,
                Text.translatable("text.magicaland.config.body_shadow.name"), BodyPalette.hex(BodyPalette.shadow(config, base)),
                color -> {
                    if (!config.bodyShadowColorLocked) { config.bodyShadowColor = color; ModelManager.requestSaveActiveModel(); }
                });
        shadow.setAutomaticColor(() -> BodyPalette.hex(BodyColorRamp.automaticShadow(BodyColorRamp.rgb(config.bodyColor))));
        shadow.setLocked(config.bodyShadowColorLocked);
        shadow.setOnLockChanged(locked -> {
            BodyPalette.setShadowLocked(config, locked);
            ModelManager.saveActiveModel();
            context.refreshKeepingScroll();
        });
        list.addWidget(shadow, SettingsList.Alignment.RIGHT);
        ColorPicker highlight = new ColorPicker(x, 0, width, height,
                Text.translatable("text.magicaland.config.body_highlight.name"), BodyPalette.hex(BodyPalette.highlight(config, base)),
                color -> {
                    if (!config.bodyHighlightColorLocked) { config.bodyHighlightColor = color; ModelManager.requestSaveActiveModel(); }
                });
        highlight.setAutomaticColor(() -> BodyPalette.hex(BodyColorRamp.automaticHighlight(BodyColorRamp.rgb(config.bodyColor))));
        highlight.setLocked(config.bodyHighlightColorLocked);
        highlight.setOnLockChanged(locked -> {
            BodyPalette.setHighlightLocked(config, locked);
            ModelManager.saveActiveModel();
            context.refreshKeepingScroll();
        });
        list.addWidget(highlight, SettingsList.Alignment.RIGHT);
        list.addWidget(new CustomButton(x, 0, width, height,
                Text.translatable("text.magicaland.config.body_shading.reset"), false, button -> {
                    BodyPalette.resetAutomatic(config);
                    ModelManager.saveActiveModel();
                    context.refreshKeepingScroll();
                }), SettingsList.Alignment.RIGHT);
    }

    private int getButtonX(PonyCustomPageContext context, int buttonWidth) {
        if (context.getWidth() < 250) {
            return context.getX() + (context.getWidth() - buttonWidth) / 2;
        }
        return context.getX() + context.getWidth() - buttonWidth - 20;
    }
}
