package top.csituka.magicaland.client.gui.tab.ponycustom;

import java.util.function.BiConsumer;

import top.csituka.magicaland.client.gui.ConfigScreen;

public final class PonyCustomPageContext {
    public enum Page {
        MODEL,
        MAIN,
        MANE,
        FACE,
        HORN,
        BODY,
        GLOW
    }

    private final ConfigScreen screen;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final BiConsumer<Page, Integer> pageSwitcher;

    public PonyCustomPageContext(ConfigScreen screen, int x, int y, int width, int height,
            BiConsumer<Page, Integer> pageSwitcher) {
        this.screen = screen;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.pageSwitcher = pageSwitcher;
    }

    public ConfigScreen getScreen() {
        return screen;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void openPage(Page page, int direction) {
        pageSwitcher.accept(page, direction);
    }

    public void reinit() {
        screen.reinitScreen();
    }
}
