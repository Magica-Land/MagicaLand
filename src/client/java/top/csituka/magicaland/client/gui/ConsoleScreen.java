package top.csituka.magicaland.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import top.csituka.magicaland.client.gui.widget.CustomButtonWidget;
import top.csituka.magicaland.client.gui.tab.TabContent;
import top.csituka.magicaland.client.gui.tab.SettingsTab;
import top.csituka.magicaland.client.gui.tab.AboutTab;
import net.fabricmc.loader.api.FabricLoader;

public class ConsoleScreen extends Screen {
    private final Screen parent;
    private Tab currentTab = Tab.SETTINGS;
    private String modVersion = "Unknown";

    public enum Tab {
        SETTINGS("text.magicaland.console.tab.settings", new SettingsTab()),
        ABOUT("text.magicaland.console.tab.about", new AboutTab());

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

    public ConsoleScreen(Screen parent) {
        super(Text.translatable("text.magicaland.console.title"));
        this.parent = parent;
        FabricLoader.getInstance().getModContainer("magicaland").ifPresent(container -> {
            this.modVersion = container.getMetadata().getVersion().getFriendlyString();
        });
    }

    public void addConsoleWidget(net.minecraft.client.gui.widget.ClickableWidget widget) {
        this.addDrawableChild(widget);
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
            CustomButtonWidget tabButton = new CustomButtonWidget(padding, y, leftWidth - padding * 2, tabHeight,
                    tab.getText(), isSelected, button -> {
                        this.currentTab = tab;
                        this.clearChildren();
                        this.init();
                    });
            tabButton.active = !isSelected;
            this.addDrawableChild(tabButton);
            y += tabHeight + tabSpacing;
        }

        this.addDrawableChild(new CustomButtonWidget(padding, this.height - 30, leftWidth - padding * 2, 20,
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

        int titleX = leftWidth + (rightWidth - this.textRenderer.getWidth(this.title)) / 2;
        context.drawTextWithShadow(this.textRenderer, this.title, titleX, 15, 0xFFFFFF);

        context.getMatrices().push();
        context.getMatrices().translate(20.0f, 20.0f, 0.0f);
        context.getMatrices().scale(0.5f, 0.5f, 1.0f);
        context.drawTextWithShadow(this.textRenderer, Text.literal("MagicaLand v" + this.modVersion + " Fabric"), 0, 0,
                0xAAAAAA);
        context.getMatrices().pop();

        this.currentTab.getContent().render(context, rightX, 0, rightWidth - padding, this.height, mouseX, mouseY,
                delta);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}
