package top.csituka.magicaland.client.gui.tab;

import net.minecraft.client.gui.DrawContext;
import top.csituka.magicaland.client.gui.ConsoleScreen;

public interface TabContent {
    void init(ConsoleScreen screen, int x, int y, int width, int height);

    void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta);
}
