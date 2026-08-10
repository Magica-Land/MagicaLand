package top.csituka.magicaland.client.gui.tab.ponycustom;

import net.minecraft.text.Text;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.gui.widget.ColorPicker;
import top.csituka.magicaland.client.gui.widget.CustomButton;
import top.csituka.magicaland.client.gui.widget.SectionLabel;
import top.csituka.magicaland.client.gui.widget.SettingsList;

public class ManePage implements PonyCustomPage {
    private static final String[] FRONT_MANE_STYLES = {"TS", "RD", "RR", "PP", "AJ", "FS"};
    private static final String[] BACK_MANE_STYLES = {"TS", "RD", "RR", "PP", "AJ", "FS"};
    private static final String[] TAIL_STYLES = {"TS", "RD", "RR", "PP", "AJ", "FS"};

    @Override
    public void build(PonyCustomPageContext context, SettingsList list) {
        ModelConfig config = ModelManager.getActiveModel();
        if (config == null) return;

        int buttonWidth = Math.min(180, context.getWidth() / 2);
        int buttonHeight = 20;
        int buttonX = getButtonX(context, buttonWidth);

        list.addWidget(createBackButton(context, buttonX, buttonWidth, buttonHeight,
                "text.magicaland.config.mane_menu.name"), SettingsList.Alignment.RIGHT);
        list.addWidget(new SectionLabel(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.section.mane_styles.name")), SettingsList.Alignment.RIGHT);

        list.addWidget(PonyCustomPageHelper.createStyleButton(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.front_mane_style.name"), config.frontManeStyle,
                FRONT_MANE_STYLES, style -> {
                    config.frontManeStyle = style;
                    ModelManager.saveActiveModel();
                }), SettingsList.Alignment.RIGHT);
        list.addWidget(PonyCustomPageHelper.createStyleButton(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.back_mane_style.name"), config.backManeStyle,
                BACK_MANE_STYLES, style -> {
                    config.backManeStyle = style;
                    ModelManager.saveActiveModel();
                }), SettingsList.Alignment.RIGHT);
        list.addWidget(PonyCustomPageHelper.createStyleButton(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.tail_style.name"), config.tailStyle,
                TAIL_STYLES, style -> {
                    config.tailStyle = style;
                    ModelManager.saveActiveModel();
                }), SettingsList.Alignment.RIGHT);

        list.addWidget(new SectionLabel(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.section.mane_colors.name")), SettingsList.Alignment.RIGHT);
        list.addWidget(new ColorPicker(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.front_mane_color.name"), config.frontManeColor,
                color -> {
                    config.frontManeColor = color;
                    ModelManager.requestSaveActiveModel();
                }), SettingsList.Alignment.RIGHT);
        list.addWidget(new ColorPicker(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.back_mane_color.name"), config.backManeColor,
                color -> {
                    config.backManeColor = color;
                    ModelManager.requestSaveActiveModel();
                }), SettingsList.Alignment.RIGHT);
        list.addWidget(new ColorPicker(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.tail_color.name"), config.tailColor,
                color -> {
                    config.tailColor = color;
                    ModelManager.requestSaveActiveModel();
                }), SettingsList.Alignment.RIGHT);
    }

    private CustomButton createBackButton(PonyCustomPageContext context, int x, int width, int height,
            String labelKey) {
        return new CustomButton(x, 0, width, height,
                Text.literal("\u2190 " + Text.translatable(labelKey).getString()), false,
                button -> context.openPage(PonyCustomPageContext.Page.MAIN, -1), false, true);
    }

    private int getButtonX(PonyCustomPageContext context, int buttonWidth) {
        if (context.getWidth() < 250) {
            return context.getX() + (context.getWidth() - buttonWidth) / 2;
        }
        return context.getX() + context.getWidth() - buttonWidth - 20;
    }
}
