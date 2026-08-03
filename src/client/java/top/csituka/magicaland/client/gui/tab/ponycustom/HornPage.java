package top.csituka.magicaland.client.gui.tab.ponycustom;

import net.minecraft.text.Text;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.gui.widget.CustomButton;
import top.csituka.magicaland.client.gui.widget.SectionLabel;
import top.csituka.magicaland.client.gui.widget.SettingsList;
import top.csituka.magicaland.client.gui.widget.Toggle;

public class HornPage implements PonyCustomPage {
    @Override
    public void build(PonyCustomPageContext context, SettingsList list) {
        ModelConfig config = ModelManager.getActiveModel();
        if (config == null) return;

        int buttonWidth = Math.min(180, context.getWidth() / 2);
        int buttonHeight = 20;
        int buttonX = getButtonX(context, buttonWidth);

        list.addWidget(new CustomButton(buttonX, 0, buttonWidth, buttonHeight,
                Text.literal("\u2190 " + Text.translatable("text.magicaland.config.horn_menu.name").getString()),
                false, button -> context.openPage(PonyCustomPageContext.Page.MAIN, -1), false, true),
                SettingsList.Alignment.RIGHT);
        list.addWidget(new SectionLabel(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.section.mane_styles.name")), SettingsList.Alignment.RIGHT);

        final Toggle[] hornToggle = new Toggle[1];
        Toggle wingToggle = new Toggle(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.show_wings.name"), config.showWings,
                toggle -> {
                    config.showWings = toggle.getState();
                    if (config.showWings) {
                        config.showHorn = false;
                        hornToggle[0].setState(false);
                    }
                    ModelManager.saveActiveModel();
                });
        list.addWidget(wingToggle, SettingsList.Alignment.RIGHT);

        hornToggle[0] = new Toggle(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.show_horn.name"), config.showHorn,
                toggle -> {
                    config.showHorn = toggle.getState();
                    if (config.showHorn) {
                        config.showWings = false;
                        wingToggle.setState(false);
                    }
                    ModelManager.saveActiveModel();
                });
        list.addWidget(hornToggle[0], SettingsList.Alignment.RIGHT);

        list.addWidget(new SectionLabel(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.section.mane_colors.name")), SettingsList.Alignment.RIGHT);
        list.addWidget(PonyCustomPageHelper.createBodyColorPicker(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.horn_color.name"), config.hornColor,
                config.hornColorLocked,
                color -> config.hornColor = color,
                locked -> config.hornColorLocked = locked), SettingsList.Alignment.RIGHT);
        list.addWidget(PonyCustomPageHelper.createBodyColorPicker(buttonX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.wing_color.name"), config.wingColor,
                config.wingColorLocked,
                color -> config.wingColor = color,
                locked -> config.wingColorLocked = locked), SettingsList.Alignment.RIGHT);
    }

    private int getButtonX(PonyCustomPageContext context, int buttonWidth) {
        if (context.getWidth() < 250) {
            return context.getX() + (context.getWidth() - buttonWidth) / 2;
        }
        return context.getX() + context.getWidth() - buttonWidth - 20;
    }
}
