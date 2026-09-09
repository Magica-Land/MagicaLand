package top.csituka.magicaland.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.text.Text;
import top.csituka.magicaland.client.gui.widget.CustomButton;
import top.csituka.magicaland.client.gui.widget.TabButton;
import top.csituka.magicaland.client.gui.widget.ColorPicker;
import top.csituka.magicaland.client.gui.widget.ViewCube;
import top.csituka.magicaland.client.gui.tab.TabContent;
import top.csituka.magicaland.client.gui.tab.TabAnimator;
import top.csituka.magicaland.client.gui.tab.Settings;
import top.csituka.magicaland.client.gui.tab.PonyCustom;
import top.csituka.magicaland.client.gui.tab.About;
import net.fabricmc.loader.api.FabricLoader;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.network.ClientNetworkHandler;
import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private Tab currentTab = Tab.SETTINGS;
    private String modVersion = "Unknown";
    private float indicatorY = -1;
    private float targetIndicatorY = -1;

    private TabAnimator tabAnimator = new TabAnimator();
    private ViewCube viewCube;
    private boolean editingAvailable;
    private boolean editingStarted;
    private boolean saveError;
    private boolean confirmingDiscard;
    private boolean editorFinished;
    private CustomButton saveButton;
    private boolean dirtyPreview;
    private long lastDirtyCheck;

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
        if (!editingStarted) {
            editingStarted = true;
            // 旧屏幕 removed 完成后才取得会话，不能借用另一个编辑器的草稿。
            editingAvailable = !ModelManager.isEditing() && ModelManager.beginEditing();
        }
        if (ColorPicker.openPicker != null) {
            ColorPicker.openPicker.open = false;
            ColorPicker.openPicker = null;
        }
        if (currentTab == Tab.PONY_CUSTOM) {
            initCustomization();
            return;
        }

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
                            this.viewCube = null;

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

        saveButton = this.addDrawableChild(new CustomButton(padding, this.height - 54, leftWidth - padding * 2, 20,
                editText("save"), false, button -> saveAndApply()));
        saveButton.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(editText("save_hint")));
        this.addDrawableChild(new CustomButton(padding, this.height - 30, leftWidth - padding * 2, 20,
                editText("close"), false,
                button -> close()));

        if (this.currentTab.getContent() instanceof ViewCube.RotationTarget rotationTarget) {
            int cubeTop = y + 8;
            int cubeBottom = this.height - 30 - 8;
            int cubeSize = Math.min(leftWidth - padding * 2, cubeBottom - cubeTop);
            if (cubeSize >= 40) {
                cubeSize = Math.min(cubeSize, 72);
                int cubeX = padding + (leftWidth - padding * 2 - cubeSize) / 2;
                if (this.viewCube == null) {
                    this.viewCube = new ViewCube(cubeX, cubeBottom - cubeSize, cubeSize, cubeSize, rotationTarget);
                }
                this.addDrawableChild(this.viewCube);
            }
        }

        int rightX = leftWidth + padding;
        int rightWidth = this.width - leftWidth - padding * 2;

        this.currentTab.getContent().init(this, rightX, 0, rightWidth, this.height);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        if (saveButton != null) {
            long now = System.nanoTime();
            if (now - lastDirtyCheck > 100_000_000L) {
                dirtyPreview = editingAvailable && ModelManager.isDirty();
                lastDirtyCheck = now;
            }
            saveButton.active = dirtyPreview && !hasPendingTextEdit();
            saveButton.setMessage(editText("save").copy().append(dirtyPreview ? " *" : ""));
        }
        if (currentTab == Tab.PONY_CUSTOM) {
            if (editingAvailable) currentTab.getContent().render(context, 8, 31, width - 16, height - 39, mouseX, mouseY, delta, 1);
            else {
                var lines = textRenderer.wrapLines(editText("begin_error"), Math.max(32, width - 32));
                int top = (height - lines.size() * textRenderer.fontHeight) / 2;
                for (int i = 0; i < lines.size(); i++)
                    context.drawCenteredTextWithShadow(textRenderer, lines.get(i), width / 2,
                            top + i * textRenderer.fontHeight, 0xFFFF9999);
            }
            for (var element : children()) {
                if (element instanceof net.minecraft.client.gui.widget.ClickableWidget widget) {
                    widget.render(context, mouseX, mouseY, delta);
                }
            }
            if (editingAvailable) currentTab.getContent().postRender(context, 8, 31, width - 16, height - 39,
                    mouseX, mouseY, delta, 1);
            if (ColorPicker.openPicker != null && ColorPicker.openPicker.open) {
                ColorPicker.openPicker.renderOverlay(context, mouseX, mouseY);
                setTooltip(java.util.List.of());
            }
            renderSaveError(context);
            return;
        }

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

        boolean suppressRightChildren = this.currentTab.getContent().suppressChildRendering();

        for (net.minecraft.client.gui.Element element : this.children()) {
            if (element instanceof net.minecraft.client.gui.widget.ClickableWidget widget) {
                if (widget.getX() >= leftWidth) {
                    if (this.tabAnimator.isAnimating()) {
                        context.getMatrices().push();
                        context.getMatrices().translate(0, this.tabAnimator.getContentOffset(), 0);
                        widget.setAlpha(1.0f);
                        widget.render(context, -1, -1, delta);
                        context.getMatrices().pop();
                    } else if (suppressRightChildren) {
                        continue;
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
        
        context.getMatrices().push();
        context.getMatrices().translate(0, this.tabAnimator.getContentOffset(), 0);
        this.currentTab.getContent().postRender(context, rightX, 0, rightWidth - padding, this.height, mouseX, mouseY,
                delta, currentAlpha);
        context.getMatrices().pop();
        if (ColorPicker.openPicker != null && ColorPicker.openPicker.open) {
            setTooltip(java.util.List.of());
        }
        renderSaveError(context);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (canInteractWithContent() && currentTab.getContent() instanceof PonyCustom pony
                && pony.overlayClick(mouseX, mouseY, button)) return true;
        if (ColorPicker.openPicker != null && ColorPicker.openPicker.open) {
            if (ColorPicker.openPicker.isMouseOver(mouseX, mouseY)) {
                if (ColorPicker.openPicker.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            } else {
                ColorPicker.openPicker.open = false;
                ColorPicker.openPicker = null;
                return true;
            }
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return canInteractWithContent() && this.currentTab.getContent().mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (canInteractWithContent() && currentTab.getContent() instanceof PonyCustom pony && pony.overlayScroll(amount)) return true;
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && ColorPicker.openPicker != null && ColorPicker.openPicker.open) {
            ColorPicker.openPicker.open = false;
            ColorPicker.openPicker = null;
            return true;
        }
        if (editingAvailable && currentTab.getContent() instanceof PonyCustom pony && pony.keyPressed(keyCode)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void initCustomization() {
        tabAnimator = new TabAnimator();
        viewCube = null;
        int buttonWidth = Math.min(88, Math.max(36, (width - 182) / 3));
        int x = 8;
        for (Tab tab : Tab.values()) {
            addDrawableChild(new TabButton(x, 6, buttonWidth, 20, tab.getText(), currentTab == tab, button -> {
                if (tab == currentTab) return;
                currentTab.getContent().onExit();
                currentTab = tab;
                currentTab.getContent().onEnter();
                reinitScreen();
            }));
            x += buttonWidth + 2;
        }
        saveButton = addDrawableChild(new CustomButton(width - 166, 6, 100, 20,
                editText("save"), false, button -> saveAndApply()));
        saveButton.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(editText("save_hint")));
        addDrawableChild(new CustomButton(width - 62, 6, 54, 20,
                editText("close"), false, button -> close()));
        if (editingAvailable) currentTab.getContent().init(this, 8, 31, width - 16, height - 39);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (canInteractWithContent() && this.currentTab.getContent().mouseReleased(mouseX, mouseY, button)) {
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
        if (canInteractWithContent() && this.currentTab.getContent().mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
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
        if (editingAvailable && ModelManager.isDirty() && !editorFinished) {
            confirmingDiscard = true;
            boolean[] handled = {false};
            client.setScreen(new ConfirmScreen(discard -> {
                handled[0] = true;
                confirmingDiscard = false;
                if (discard) finishEditor(false);
                else client.setScreen(this);
            }, editText("discard_title"), editText("discard_body"), editText("discard"), editText("continue")) {
                @Override public void removed() {
                    super.removed();
                    if (!handled[0]) {
                        confirmingDiscard = false;
                        cleanupEditor(false);
                    }
                }
            });
            return;
        }
        finishEditor(false);
    }

    private boolean hasPendingTextEdit() {
        return currentTab.getContent() instanceof PonyCustom pony && pony.hasPendingTextEdit();
    }

    private boolean canInteractWithContent() {
        return currentTab != null && (currentTab != Tab.PONY_CUSTOM || editingAvailable);
    }

    private void saveAndApply() {
        if (!editingAvailable || hasPendingTextEdit()) return;
        if (!ModelManager.commitEditing()) {
            saveError = true;
            return;
        }
        finishEditor(true);
        ClientNetworkHandler.publishSavedModel();
    }

    private void finishEditor(boolean saved) {
        cleanupEditor(saved);
        this.client.setScreen(this.parent);
    }

    private void cleanupEditor(boolean saved) {
        if (editorFinished) return;
        if (!saved && editingAvailable) ModelManager.discardEditing();
        editorFinished = true;
        if (ColorPicker.openPicker != null) {
            ColorPicker.openPicker.open = false;
            ColorPicker.openPicker = null;
        }
        this.currentTab.getContent().onExit();
        ((PonyCustom) Tab.PONY_CUSTOM.getContent()).endEditingSession();
        clearChildren();
    }

    @Override
    public void removed() {
        super.removed();
        if (!confirmingDiscard && !editorFinished) {
            cleanupEditor(false);
        }
    }

    private static Text editText(String key) { return Text.translatable("text.magicaland.customize.editor." + key); }

    private void renderSaveError(DrawContext context) {
        if (!saveError) return;
        var lines = textRenderer.wrapLines(editText("save_error"), width - 28);
        int top = height - 12 - lines.size() * textRenderer.fontHeight;
        context.fill(6, top, width - 6, height - 4, 0xEF3B1721);
        for (int i = 0; i < lines.size(); i++)
            context.drawTextWithShadow(textRenderer, lines.get(i), 14, top + 4 + i * textRenderer.fontHeight, 0xFFFFCCCC);
    }
}
