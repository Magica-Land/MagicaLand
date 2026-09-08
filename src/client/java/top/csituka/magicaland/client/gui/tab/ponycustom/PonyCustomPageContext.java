package top.csituka.magicaland.client.gui.tab.ponycustom;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import top.csituka.magicaland.client.config.style.PonyStylePart;

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
    private final Runnable pageRefresher;
    private final Consumer<PonyStylePart> partFocus;
    private final Runnable scrollReset;

    public PonyCustomPageContext(ConfigScreen screen, int x, int y, int width, int height,
            BiConsumer<Page, Integer> pageSwitcher, Runnable pageRefresher) {
        this(screen, x, y, width, height, pageSwitcher, pageRefresher, part -> {}, () -> {});
    }

    public PonyCustomPageContext(ConfigScreen screen, int x, int y, int width, int height,
            BiConsumer<Page, Integer> pageSwitcher, Runnable pageRefresher,
            Consumer<PonyStylePart> partFocus, Runnable scrollReset) {
        this.screen = screen;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.pageSwitcher = pageSwitcher;
        this.pageRefresher = pageRefresher;
        this.partFocus = partFocus;
        this.scrollReset = scrollReset;
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

    public int getControlWidth() {
        return Math.max(80, width - 22);
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

    public void refreshKeepingScroll() {
        pageRefresher.run();
    }

    public void focusPart(PonyStylePart part) { partFocus.accept(part); }

    public void resetScroll() { scrollReset.run(); }
}
