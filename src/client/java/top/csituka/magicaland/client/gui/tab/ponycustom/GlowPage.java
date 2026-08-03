package top.csituka.magicaland.client.gui.tab.ponycustom;

import net.minecraft.text.Text;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.gui.widget.ColorPicker;
import top.csituka.magicaland.client.gui.widget.CustomButton;
import top.csituka.magicaland.client.gui.widget.SectionLabel;
import top.csituka.magicaland.client.gui.widget.SettingsList;

public class GlowPage implements PonyCustomPage {
    @Override
    public void build(PonyCustomPageContext context, SettingsList list) {
        ModelConfig config = ModelManager.getActiveModel();
        if (config == null) return;

        int buttonWidth = Math.min(180, context.getWidth() / 2);
        int buttonHeight = 20;
        int buttonX = getButtonX(context, buttonWidth);

        list.addWidget(new CustomButton(buttonX, 0, buttonWidth, buttonHeight,
                Text.literal("\u2190 " + Text.translatable("text.magicaland.config.glow_menu.name").getString()),
                false, button -> context.openPage(PonyCustomPageContext.Page.MAIN, -1), false, true),
                SettingsList.Alignment.RIGHT);
        list.addWidget(new SectionLabel(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.section.mane_colors.name")), SettingsList.Alignment.RIGHT);
        list.addWidget(new ColorPicker(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.glow_color.name"), config.magicGlowColor,
                color -> {
                    config.magicGlowColor = color;
                    ModelManager.saveActiveModel();
                }), SettingsList.Alignment.RIGHT);
    }

    @Override
    public boolean usesGlowPreview() {
        return true;
    }

    private int getButtonX(PonyCustomPageContext context, int buttonWidth) {
        if (context.getWidth() < 250) {
            return context.getX() + (context.getWidth() - buttonWidth) / 2;
        }
        return context.getX() + context.getWidth() - buttonWidth - 20;
    }
}
