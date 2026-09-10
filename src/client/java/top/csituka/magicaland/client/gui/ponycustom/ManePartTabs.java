package top.csituka.magicaland.client.gui.ponycustom;

import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import top.csituka.magicaland.client.render.ManePalette.Part;

public final class ManePartTabs extends ClickableWidget {
    private final Part selected;
    private final Consumer<Part> onSelect;

    public ManePartTabs(int x, int width, Part selected, Consumer<Part> onSelect) {
        super(x, 0, width, 20, label(selected));
        this.selected = selected;
        this.onSelect = onSelect;
    }

    private static Text label(Part part) {
        return Text.translatable("text.magicaland.customize.styles.part." + part.name().toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public void renderButton(DrawContext draw, int mouseX, int mouseY, float delta) {
        var font = MinecraftClient.getInstance().textRenderer;
        for (Part part : Part.values()) {
            int left = getX() + part.ordinal() * width / 3;
            int right = getX() + (part.ordinal() + 1) * width / 3 - 2;
            draw.fill(left, getY(), right, getY() + height, part == selected ? 0xBB77628F : 0x77546271);
            draw.drawCenteredTextWithShadow(font, font.trimToWidth(label(part).getString(), right - left - 4),
                    (left + right) / 2, getY() + 6, part == selected ? 0xFFFFFFFF : 0xFFCCCCCC);
            if (part == selected && isFocused()) draw.drawBorder(left, getY(), right - left, height, 0xFFE8D590);
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (!active || !visible || button != 0 || !clicked(x, y)) return false;
        Part part = Part.values()[Math.min(2, (int) ((x - getX()) * 3 / width))];
        onSelect.accept(part);
        playDownSound(MinecraftClient.getInstance().getSoundManager());
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!active || !visible || !isFocused()) return false;
        if (key != 262 && key != 263 && key != 257 && key != 335 && key != 32) return false;
        int next = key == 262 ? Math.min(2, selected.ordinal() + 1)
                : key == 263 ? Math.max(0, selected.ordinal() - 1) : selected.ordinal();
        onSelect.accept(Part.values()[next]);
        return true;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, label(selected));
        builder.put(NarrationPart.USAGE, Text.translatable("text.magicaland.customize.styles.part_hint"));
    }
}
