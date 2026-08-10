package top.csituka.magicaland.client.gui.tab.settings;

import net.minecraft.text.Text;
import net.minecraft.client.gui.tooltip.Tooltip;
import top.csituka.magicaland.client.config.Config;
import top.csituka.magicaland.client.gui.widget.SettingsList;
import top.csituka.magicaland.client.gui.widget.Toggle;

public class GamePage implements SettingsPage {
    @Override
    public void build(SettingsList list, int buttonX, int buttonWidth) {
        Config config = Config.getInstance();

        list.addWidget(new Toggle(buttonX, 0, buttonWidth, 20,
                Text.translatable("text.magicaland.config.replace_model.name"),
                config.replacePlayerModel,
                toggle -> {
                    config.replacePlayerModel = toggle.getState();
                    Config.save();
                }));

        Toggle magicGlowToggle = new Toggle(buttonX, 0, buttonWidth, 20,
                Text.translatable("text.magicaland.config.first_person_magic_glow.name"),
                config.firstPersonMagicGlow,
                toggle -> {
                    config.firstPersonMagicGlow = toggle.getState();
                    Config.save();
                });
        magicGlowToggle.setTooltip(Tooltip.of(
                Text.translatable("text.magicaland.config.first_person_magic_glow.tooltip")));
        list.addWidget(magicGlowToggle);
    }
}
