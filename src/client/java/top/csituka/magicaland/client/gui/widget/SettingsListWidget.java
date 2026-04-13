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

    public SettingsListWidget(MinecraftClient minecraftClient, int width, int height, int top, int bottom, int itemHeight) {
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

    private void renderRoundedScrollbar(DrawContext context, int x, int y, int width, int height) {
        int x1 = x;
        int y1 = y;
        int x2 = x + width;
        int y2 = y + height;
        
        context.fill(x1 + 1, y1, x2 - 1, y1 + 1, 0xFFC0C0C0);
        context.fill(x1, y1 + 1, x2, y2 - 1, 0xFFC0C0C0);
        context.fill(x1 + 1, y2 - 1, x2 - 1, y2, 0xFFC0C0C0);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.getMatrices().push();
        this.renderList(context, mouseX, mouseY, delta);
        context.getMatrices().pop();
        
        int maxScroll = this.getMaxScroll();
        if (maxScroll > 0) {
            int i = this.getScrollbarPositionX();
            int j = (int)((float)(this.bottom - this.top) * (float)(this.bottom - this.top) / (float)this.getMaxPosition());
            j = net.minecraft.util.math.MathHelper.clamp(j, 32, this.bottom - this.top - 8);
            int k = (int)this.getScrollAmount() * (this.bottom - this.top - j) / maxScroll + this.top;
            if (k < this.top) {
                k = this.top;
            }
            this.renderRoundedScrollbar(context, i, k, 6, j);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.updateScrollingState(mouseX, mouseY, button);
        if (this.isMouseOver(mouseX, mouseY)) {
            int i = this.getScrollbarPositionX();
            if (mouseX >= (double)i && mouseX <= (double)(i + 6)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.getScrollbarPositionX() <= mouseX && mouseX <= this.getScrollbarPositionX() + 6) {
            int maxScroll = this.getMaxScroll();
            if (maxScroll > 0) {
                int j = (int)((float)(this.bottom - this.top) * (float)(this.bottom - this.top) / (float)this.getMaxPosition());
                j = net.minecraft.util.math.MathHelper.clamp(j, 32, this.bottom - this.top - 8);
                double d = Math.max(1.0, maxScroll / (double)(this.bottom - this.top - j));
                this.setScrollAmount(this.getScrollAmount() + deltaY * d);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
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
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            this.widget.setX(x + (entryWidth - this.widget.getWidth()) / 2);
            this.widget.setY(y);

            int widgetTop = y;
            int widgetBottom = y + this.widget.getHeight();
            
            int fadeDistance = 15;
            float alpha = 1.0f;
            
            if (widgetTop < this.parent.top) {
                alpha = Math.max(0.0f, 1.0f - (float)(this.parent.top - widgetTop) / fadeDistance);
            } else if (widgetBottom > this.parent.bottom) {
                alpha = Math.max(0.0f, 1.0f - (float)(widgetBottom - this.parent.bottom) / fadeDistance);
            }
            
            this.widget.setAlpha(alpha);
            
            this.widget.render(context, mouseX, mouseY, tickDelta);
        }
    }
}
