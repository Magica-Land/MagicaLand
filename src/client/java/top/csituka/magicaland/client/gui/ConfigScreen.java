package top.csituka.magicaland.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import top.csituka.magicaland.client.gui.widget.CustomButton;
import top.csituka.magicaland.client.gui.widget.TabButton;
import top.csituka.magicaland.client.gui.widget.ColorPicker;
import top.csituka.magicaland.client.gui.tab.TabContent;
import top.csituka.magicaland.client.gui.tab.TabAnimator;
import top.csituka.magicaland.client.gui.tab.Settings;
import top.csituka.magicaland.client.gui.tab.PonyCustom;
import top.csituka.magicaland.client.gui.tab.About;
import net.fabricmc.loader.api.FabricLoader;
import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private Tab currentTab = Tab.SETTINGS;
    private String modVersion = "Unknown";
    private float indicatorY = -1;
    private float targetIndicatorY = -1;

    private final TabAnimator tabAnimator = new TabAnimator();

    public enum Tab {
        SETTINGS("text.magicaland.console.tab.settings", new Settings()),
        PONY_CUSTOM("text.magicaland.console.tab.pony_custom", new PonyCustom()),
        ABOUT("text.magicaland.console.tab.about", new About());

        private final String translationKey;
        private final TabContent content;

        Tab(String translationKey, TabContent content) {
            this.translationKey = translationKey;
            this.content = content;
        }

        public Text getText() {
            return Text.translatable(translationKey);
        }

        public TabContent getContent() {
            return content;
        }
    }

    public ConfigScreen(Screen parent) {
        this(parent, null);
    }

    public ConfigScreen(Screen parent, Tab initialTab) {
        super(Text.translatable("text.magicaland.console.title"));
        this.parent = parent;
        if (initialTab != null) {
            this.currentTab = initialTab;
        }
        FabricLoader.getInstance().getModContainer("magicaland").ifPresent(container -> {
            this.modVersion = container.getMetadata().getVersion().getFriendlyString();
        });
        this.currentTab.getContent().onEnter();
    }

    public void addConsoleWidget(net.minecraft.client.gui.widget.ClickableWidget widget) {
        this.addDrawableChild(widget);
    }

    public <T extends net.minecraft.client.gui.Element & net.minecraft.client.gui.Drawable & net.minecraft.client.gui.Selectable> void addConsoleElement(
            T element) {
        this.addDrawableChild(element);
    }

    public void reinitScreen() {
        if (ColorPicker.openPicker != null) {
            ColorPicker.openPicker.open = false;
            ColorPicker.openPicker = null;
        }
        this.clearChildren();
        this.init();
    }

    @Override
    protected void init() {
        super.init();

        int leftWidth = 100;
        int padding = 10;

        int tabsCount = Tab.values().length;
        int tabHeight = 20;
        int tabSpacing = 5;
        int totalTabsHeight = tabsCount * tabHeight + (tabsCount - 1) * tabSpacing;
        int y = (this.height - totalTabsHeight) / 2;

        for (Tab tab : Tab.values()) {
            boolean isSelected = (this.currentTab == tab);
            final int currentY = y;
            TabButton tabButton = new TabButton(padding, y, leftWidth - padding * 2, tabHeight,
                    tab.getText(), isSelected, button -> {
                    if (this.currentTab != tab) {
                            this.currentTab.getContent().onExit();

                            List<net.minecraft.client.gui.widget.ClickableWidget> oldWidgets = new ArrayList<>();
                            for (net.minecraft.client.gui.Element element : this.children()) {
                                if (element instanceof net.minecraft.client.gui.widget.ClickableWidget widget) {
                                    if (widget.getX() >= leftWidth) {
                                        oldWidgets.add(widget);
                                    }
                                }
                            }

                            this.tabAnimator.startTransition(this.currentTab, tab, this.height, oldWidgets);
                            this.currentTab = tab;
                            this.currentTab.getContent().onEnter();

                            if (ColorPicker.openPicker != null) {
                                ColorPicker.openPicker.open = false;
                                ColorPicker.openPicker = null;
                            }

                            this.clearChildren();
                            this.init();
                        }
                    });
            this.addDrawableChild(tabButton);

            if (isSelected) {
                this.targetIndicatorY = y;
                if (this.indicatorY == -1) {
                    this.indicatorY = y;
                }
            }

            y += tabHeight + tabSpacing;
        }

        this.addDrawableChild(new CustomButton(padding, this.height - 30, leftWidth - padding * 2, 20,
                Text.translatable("text.magicaland.config.button.done"), false,
                button -> this.client.setScreen(this.parent)));

        int rightX = leftWidth + padding;
        int rightWidth = this.width - leftWidth - padding * 2;

        this.currentTab.getContent().init(this, rightX, 0, rightWidth, this.height);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        int leftWidth = 100;
        int rightWidth = this.width - leftWidth;
        int padding = 10;
        int rightX = leftWidth + padding;

        context.getMatrices().push();
        context.getMatrices().translate(20.0f, 20.0f, 0.0f);
        context.getMatrices().scale(0.5f, 0.5f, 1.0f);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Magical Land v" + this.modVersion + " · Fabric"), 0,
                0,
                0xAAAAAA);
        context.getMatrices().pop();

        context.drawTextWithShadow(this.textRenderer, this.title, 20, 28, 0xFFFFFF);

        if (this.indicatorY != -1) {
            float diff = this.targetIndicatorY - this.indicatorY;
            if (Math.abs(diff) > 0.5f) {
                this.indicatorY += diff * 0.3f;
            } else {
                this.indicatorY = this.targetIndicatorY;
            }
            context.fill(padding, Math.round(this.indicatorY), padding + 2, Math.round(this.indicatorY) + 20,
                    0xFFFFFFFF);
        }

        this.tabAnimator.update();

        int contentTop = 0;
        context.enableScissor(rightX, contentTop, this.width, this.height);

        this.tabAnimator.render(context, rightX, rightWidth, this.height, padding, delta);

        context.getMatrices().push();
        context.getMatrices().translate(0, this.tabAnimator.getContentOffset(), 0);

        float currentAlpha = 1.0f;
        if (this.tabAnimator.isAnimating()) {
            currentAlpha = 1.0f - (Math.abs(this.tabAnimator.getContentOffset()) / this.height);
        }

        this.currentTab.getContent().render(context, rightX, 0, rightWidth - padding, this.height, mouseX, mouseY,
                delta, currentAlpha);

        context.getMatrices().pop();

        for (net.minecraft.client.gui.Element element : this.children()) {
            if (element instanceof net.minecraft.client.gui.widget.ClickableWidget widget) {
                if (widget.getX() >= leftWidth) {
                    if (this.tabAnimator.isAnimating()) {
                        context.getMatrices().push();
                        context.getMatrices().translate(0, this.tabAnimator.getContentOffset(), 0);
                        widget.setAlpha(1.0f);
                        widget.render(context, -1, -1, delta);
                        context.getMatrices().pop();
                    } else {
                        widget.setAlpha(1.0f);
                        widget.render(context, mouseX, mouseY, delta);
                    }
                }
            }
        }

        context.disableScissor();

        for (net.minecraft.client.gui.Element element : this.children()) {
            if (element instanceof net.minecraft.client.gui.widget.ClickableWidget widget) {
                if (widget.getX() < leftWidth) {
                    widget.render(context, mouseX, mouseY, delta);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ColorPicker.openPicker != null && ColorPicker.openPicker.open) {
            if (ColorPicker.openPicker.isMouseOver(mouseX, mouseY)) {
                if (ColorPicker.openPicker.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            } else {
                ColorPicker.openPicker.open = false;
                ColorPicker.openPicker = null;
            }
        }
        if (this.currentTab != null && this.currentTab.getContent().mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.currentTab != null && this.currentTab.getContent().mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (ColorPicker.openPicker != null && ColorPicker.openPicker.open && button == 0) {
            if (ColorPicker.openPicker.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }
        if (this.currentTab != null && this.currentTab.getContent().mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void tick() {
        super.tick();
        for (net.minecraft.client.gui.Element element : this.children()) {
            if (element instanceof net.minecraft.client.gui.widget.TextFieldWidget textField) {
                textField.tick();
            }
        }
    }

    @Override
    public void close() {
        if (ColorPicker.openPicker != null) {
            ColorPicker.openPicker.open = false;
            ColorPicker.openPicker = null;
        }
        this.currentTab.getContent().onExit();
        this.client.setScreen(this.parent);
    }
}
