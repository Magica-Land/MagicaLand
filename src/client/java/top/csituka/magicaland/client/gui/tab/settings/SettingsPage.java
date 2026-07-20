package top.csituka.magicaland.client.gui.tab.settings;

import top.csituka.magicaland.client.gui.widget.SettingsList;

@FunctionalInterface
public interface SettingsPage {
    void build(SettingsList list, int buttonX, int buttonWidth);
}
