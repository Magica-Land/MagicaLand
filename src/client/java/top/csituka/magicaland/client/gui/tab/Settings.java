package top.csituka.magicaland.client.gui.tab;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import top.csituka.magicaland.client.gui.ConfigScreen;
import top.csituka.magicaland.client.gui.tab.settings.GeneralPage;
import top.csituka.magicaland.client.gui.tab.settings.GamePage;
import top.csituka.magicaland.client.gui.tab.settings.NetworkPage;
import top.csituka.magicaland.client.gui.tab.settings.SettingsPage;
import top.csituka.magicaland.client.gui.widget.SettingsList;

public class Settings implements TabContent {
    private SettingsList listWidget;
    private ConfigScreen screenRef;

    private static final int TAB_BAR_HEIGHT = 32;
    private static final int TAB_BAR_GAP = 8;
    private static final String[] TAB_KEYS = {
            "text.magicaland.config.tab.general",
            "text.magicaland.config.tab.game",
            "text.magicaland.config.tab.network"
    };
    private static final SettingsPage[] PAGES = {
            new GeneralPage(),
            new GamePage(),
            new NetworkPage()
    };
    private int selectedTab = 1;

    private int buttonX;
    private int buttonWidth;
    private int tabBarY;

    private float indicatorX = -1;
    private float targetIndicatorX = -1;

    @Override
    public void onEnter() {
        selectedTab = 1;
        indicatorX = -1;
        targetIndicatorX = -1;
    }

    @Override
    public void init(ConfigScreen screen, int x, int y, int width, int height) {
        this.screenRef = screen;
        buttonWidth = Math.min(250, width - 20);
        buttonX = x + (width - buttonWidth) / 2;

        int topMargin = 30;
        int bottomMargin = 40;
        tabBarY = y + topMargin;

        // 配置列表从分页栏 + 间隙之后开始
        int listTop = y + topMargin + TAB_BAR_HEIGHT + TAB_BAR_GAP;
        int listBottom = y + height - bottomMargin;

        listWidget = new SettingsList(MinecraftClient.getInstance(), width, height,
                listTop, listBottom, 24);
        listWidget.setLeftPos(x);

        // 计算分页布局，更新指示器目标位置
        int tabWidth = buttonWidth / TAB_KEYS.length;
        int totalWidth = tabWidth * TAB_KEYS.length;
        int startX = buttonX + (buttonWidth - totalWidth) / 2;
        float newTarget = startX + tabWidth * selectedTab;
        targetIndicatorX = newTarget;
        if (indicatorX < 0) {
            indicatorX = newTarget;
        }

        // 委托当前分页构建配置项
        PAGES[selectedTab].build(listWidget, buttonX, buttonWidth);

        screen.addConsoleElement(listWidget);
    }

    @Override
    public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta,
            float alpha) {
        // 动画更新指示器位置
        if (targetIndicatorX >= 0) {
            float diff = targetIndicatorX - indicatorX;
            if (Math.abs(diff) > 0.5f) {
                indicatorX += diff * 0.3f;
            } else {
                indicatorX = targetIndicatorX;
            }
        }

        renderTabBar(context, mouseX, mouseY);
        if (listWidget != null) {
            listWidget.render(context, mouseX, mouseY, delta);
        }
    }

    private void renderTabBar(DrawContext context, int mouseX, int mouseY) {
        int tabCount = TAB_KEYS.length;
        int tabWidth = buttonWidth / tabCount;
        int totalWidth = tabWidth * tabCount;
        int startX = buttonX + (buttonWidth - totalWidth) / 2;

        var textRenderer = MinecraftClient.getInstance().textRenderer;

        for (int i = 0; i < tabCount; i++) {
            int tabLeft = startX + tabWidth * i;
            int tabRight = tabLeft + tabWidth;
            boolean hovered = mouseX >= tabLeft && mouseX < tabRight
                    && mouseY >= tabBarY && mouseY < tabBarY + TAB_BAR_HEIGHT;

            int textColor;
            if (i == selectedTab) {
                textColor = 0xFFFFFFFF;
            } else if (hovered) {
                textColor = 0xFFCCCCCC;
            } else {
                textColor = 0xFF777777;
            }

            Text label = Text.translatable(TAB_KEYS[i]);
            int textWidth = textRenderer.getWidth(label);
            int textX = tabLeft + (tabWidth - textWidth) / 2;
            context.drawText(textRenderer, label,
                    textX, tabBarY + 6, textColor, false);
        }

        // 选中分页下方的短指示线，长度与文字宽度匹配，紧靠文字下方
        int lineY = tabBarY + 20;
        Text selLabel = Text.translatable(TAB_KEYS[selectedTab]);
        int selTextWidth = textRenderer.getWidth(selLabel);
        int indWidth = selTextWidth + 8;
        int selLeft = startX + tabWidth * selectedTab;
        int indCenter = selLeft + tabWidth / 2;
        if (indicatorX >= 0) {
            // 以 indicatorX 为分页中心，绘制定宽的指示线
            int indCenterX = Math.round(indicatorX) + tabWidth / 2;
            context.fill(indCenterX - indWidth / 2, lineY,
                    indCenterX + indWidth / 2, lineY + 2, 0xFFFFFFFF);
        } else {
            context.fill(indCenter - indWidth / 2, lineY,
                    indCenter + indWidth / 2, lineY + 2, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || screenRef == null) return false;

        int tabCount = TAB_KEYS.length;
        int tabWidth = buttonWidth / tabCount;
        int totalWidth = tabWidth * tabCount;
        int startX = buttonX + (buttonWidth - totalWidth) / 2;

        if (mouseY >= tabBarY && mouseY < tabBarY + TAB_BAR_HEIGHT) {
            for (int i = 0; i < tabCount; i++) {
                int tabLeft = startX + tabWidth * i;
                if (mouseX >= tabLeft && mouseX < tabLeft + tabWidth) {
                    if (i != selectedTab) {
                        selectedTab = i;
                        screenRef.reinitScreen();
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
