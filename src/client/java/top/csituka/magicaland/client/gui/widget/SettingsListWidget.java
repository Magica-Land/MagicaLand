package top.csituka.magicaland.client.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;

import java.util.Collections;
import java.util.List;

public class SettingsListWidget extends ElementListWidget<SettingsListWidget.Entry> {
    private long lastInteractionTime = 0;
    private double lastScrollAmount = 0.0;
    private long lastRenderTime = 0;
    private float scrollbarAlpha = 0.0f;
    private double targetScrollAmount = 0.0;
    private boolean isDraggingScrollbar = false;

    public SettingsListWidget(MinecraftClient minecraftClient, int width, int height, int top, int bottom,
            int itemHeight) {
        super(minecraftClient, width, height, top, bottom, itemHeight);
        this.setRenderBackground(false);
        this.setRenderHorizontalShadows(false);
    }

    public void addWidget(ClickableWidget widget) {
        this.addEntry(new Entry(widget, this));
    }

    @Override
    public int getRowWidth() {
        return this.width - 20;
    }

    @Override
    protected int getScrollbarPositionX() {
        return this.left + this.width - 6;
    }

    private void renderRoundedScrollbar(DrawContext context, int x, int y, int width, int height, float alpha) {
        if (alpha <= 0.01f)
            return;

        int alphaInt = (int) (alpha * 255.0f);
        int color = (alphaInt << 24) | 0x00C0C0C0;

        int x1 = x;
        int y1 = y;
        int x2 = x + width;
        int y2 = y + height;

        context.fill(x1 + 1, y1, x2 - 1, y1 + 1, color);
        context.fill(x1, y1 + 1, x2, y2 - 1, color);
        context.fill(x1 + 1, y2 - 1, x2 - 1, y2, color);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.targetScrollAmount = net.minecraft.util.math.MathHelper.clamp(this.targetScrollAmount, 0.0,
                this.getMaxScroll());
        if (Math.abs(this.targetScrollAmount - this.getScrollAmount()) > 0.1) {
            double newScroll = net.minecraft.util.math.MathHelper.lerp(0.3, this.getScrollAmount(),
                    this.targetScrollAmount);
            this.setScrollAmount(newScroll);
        } else {
            this.setScrollAmount(this.targetScrollAmount);
        }

        context.getMatrices().push();
        this.renderList(context, mouseX, mouseY, delta);
        context.getMatrices().pop();

        int maxScroll = this.getMaxScroll();
        if (maxScroll > 0) {
            long currentTime = System.currentTimeMillis();
            long deltaMs = currentTime - (this.lastRenderTime == 0 ? currentTime : this.lastRenderTime);
            this.lastRenderTime = currentTime;

            int i = this.getScrollbarPositionX();

            boolean isHovering = mouseX >= i - 20 && mouseX <= i + 26 && mouseY >= this.top && mouseY <= this.bottom;
            boolean isScrolling = Math.abs(this.getScrollAmount() - this.lastScrollAmount) > 0.01;

            if (isHovering || isScrolling) {
                this.lastInteractionTime = currentTime;
            }
            this.lastScrollAmount = this.getScrollAmount();

            long timeSinceLastInteraction = currentTime - this.lastInteractionTime;

            if (timeSinceLastInteraction < 1000) {
                this.scrollbarAlpha = Math.min(1.0f, this.scrollbarAlpha + deltaMs / 200.0f);
            } else {
                this.scrollbarAlpha = Math.max(0.0f, this.scrollbarAlpha - deltaMs / 500.0f);
            }

            if (this.scrollbarAlpha > 0.0f) {
                int j = (int) ((float) (this.bottom - this.top) * (float) (this.bottom - this.top)
                        / (float) this.getMaxPosition());
                j = net.minecraft.util.math.MathHelper.clamp(j, 32, this.bottom - this.top - 8);
                int k = (int) this.getScrollAmount() * (this.bottom - this.top - j) / maxScroll + this.top;
                if (k < this.top) {
                    k = this.top;
                }
                this.renderRoundedScrollbar(context, i, k, 6, j, this.scrollbarAlpha);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.updateScrollingState(mouseX, mouseY, button);
        if (this.isMouseOver(mouseX, mouseY)) {
            int i = this.getScrollbarPositionX();
            if (button == 0 && mouseX >= (double) i && mouseX <= (double) (i + 6)) {
                this.isDraggingScrollbar = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDraggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        this.targetScrollAmount = net.minecraft.util.math.MathHelper
                .clamp(this.targetScrollAmount - amount * this.itemHeight, 0.0, this.getMaxScroll());
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.isDraggingScrollbar) {
            int maxScroll = this.getMaxScroll();
            if (maxScroll > 0) {
                int j = (int) ((float) (this.bottom - this.top) * (float) (this.bottom - this.top)
                        / (float) this.getMaxPosition());
                j = net.minecraft.util.math.MathHelper.clamp(j, 32, this.bottom - this.top - 8);
                double d = Math.max(1.0, maxScroll / (double) (this.bottom - this.top - j));
                this.targetScrollAmount = net.minecraft.util.math.MathHelper.clamp(this.targetScrollAmount + deltaY * d,
                        0.0, maxScroll);
                this.setScrollAmount(this.targetScrollAmount);
                return true;
            }
        }
        boolean result = super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        this.targetScrollAmount = this.getScrollAmount();
        return result;
    }

    public static class Entry extends ElementListWidget.Entry<Entry> {
        public final ClickableWidget widget;
        private final SettingsListWidget parent;

        public Entry(ClickableWidget widget, SettingsListWidget parent) {
            this.widget = widget;
            this.parent = parent;
        }

        @Override
        public List<? extends Element> children() {
            return Collections.singletonList(this.widget);
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return Collections.singletonList(this.widget);
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
                int mouseY, boolean hovered, float tickDelta) {
            this.widget.setX(x + (entryWidth - this.widget.getWidth()) / 2);
            this.widget.setY(y);

            int widgetTop = y;
            int widgetBottom = y + this.widget.getHeight();

            int fadeDistance = 15;
            float alpha = 1.0f;

            if (widgetTop < this.parent.top) {
                alpha = Math.max(0.0f, 1.0f - (float) (this.parent.top - widgetTop) / fadeDistance);
            } else if (widgetBottom > this.parent.bottom) {
                alpha = Math.max(0.0f, 1.0f - (float) (widgetBottom - this.parent.bottom) / fadeDistance);
            }

            this.widget.setAlpha(alpha);

            this.widget.render(context, mouseX, mouseY, tickDelta);
        }
    }
}
