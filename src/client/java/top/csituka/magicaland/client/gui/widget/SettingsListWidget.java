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
        this.addEntry(new Entry(widget));
    }

    @Override
    public int getRowWidth() {
        return this.width - 20;
    }

    @Override
    protected int getScrollbarPositionX() {
        return this.left + this.width - 6;
    }

    public static class Entry extends ElementListWidget.Entry<Entry> {
        public final ClickableWidget widget;

        public Entry(ClickableWidget widget) {
            this.widget = widget;
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
            this.widget.render(context, mouseX, mouseY, tickDelta);
        }
    }
}
