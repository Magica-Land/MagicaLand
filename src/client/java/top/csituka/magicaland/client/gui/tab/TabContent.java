package top.csituka.magicaland.client.gui.tab;

import net.minecraft.client.gui.DrawContext;
import top.csituka.magicaland.client.gui.ConfigScreen;

public interface TabContent {
    void init(ConfigScreen screen, int x, int y, int width, int height);

    void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta, float alpha);

    default void onEnter() {}

    default void onExit() {}

    default boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }

    default boolean mouseReleased(double mouseX, double mouseY, int button) { return false; }

    default boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) { return false; }

    default void postRender(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta, float alpha) {}

    default boolean suppressChildRendering() { return false; }
}
