package top.csituka.magicaland.client.gui.tab.ponycustom;

import net.minecraft.client.gui.DrawContext;
import top.csituka.magicaland.client.gui.widget.SettingsList;

public interface PonyCustomPage {
    void build(PonyCustomPageContext context, SettingsList list);

    default void onEnter() {}

    default void render(PonyCustomPageContext context, DrawContext drawContext, int mouseX, int mouseY,
            float delta, float alpha) {}

    default boolean isPreviewActive() {
        return true;
    }

    default boolean usesGlowPreview() {
        return false;
    }
}
