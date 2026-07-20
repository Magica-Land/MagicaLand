package top.csituka.magicaland.client.gui.tab.settings;

import net.minecraft.text.Text;
import top.csituka.magicaland.client.gui.widget.CustomButton;
import top.csituka.magicaland.client.gui.widget.SettingsList;

public class NetworkPage implements SettingsPage {
    @Override
    public void build(SettingsList list, int buttonX, int buttonWidth) {
        list.addWidget(new CustomButton(buttonX, 0, buttonWidth, 20,
                Text.literal("网络设置占位"),
                false, button -> {}));
        list.addWidget(new CustomButton(buttonX, 0, buttonWidth, 20,
                Text.literal("网络设置占位"),
                false, button -> {}));
        list.addWidget(new CustomButton(buttonX, 0, buttonWidth, 20,
                Text.literal("网络设置占位"),
                false, button -> {}));
    }
}
