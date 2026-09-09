package top.csituka.magicaland.client.gui.tab.ponycustom;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import top.csituka.magicaland.cutiemark.CutieMarkData;

public final class PixelCanvasWidget extends ClickableWidget {
    private final PixelCanvasHistory editor;
    private final BooleanSupplier currentDraft;
    private final IntConsumer pickedColor;
    private int viewportTop = Integer.MIN_VALUE, viewportBottom = Integer.MAX_VALUE;
    private int keyboardX, keyboardY;

    public PixelCanvasWidget(int x, int width, PixelCanvasHistory editor, BooleanSupplier currentDraft, IntConsumer pickedColor) {
        super(x, 0, width, side(width) + 29, Text.translatable("text.magicaland.customize.mark.canvas"));
        this.editor = editor;
        this.currentDraft = currentDraft;
        this.pickedColor = pickedColor;
    }
    public static int side(int width) { return Math.max(1, Math.min(16, (width - 8) / CutieMarkData.SIZE)) * CutieMarkData.SIZE; }
    public void setViewport(int top, int bottom) { viewportTop = top; viewportBottom = bottom; }
    private int left() { return getX() + (width - side(width)) / 2; }
    private int top() { return getY() + 2; }
    private int pixelX(double x) { return (int) Math.floor((x - left()) / (side(width) / (double) CutieMarkData.SIZE)); }
    private int pixelY(double y) { return (int) Math.floor((y - top()) / (side(width) / (double) CutieMarkData.SIZE)); }
    private boolean overCanvas(double x, double y) {
        return y >= viewportTop && y < viewportBottom && x >= left() && x < left() + side(width)
                && y >= top() && y < top() + side(width);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (!active || !visible || !currentDraft.getAsBoolean() || button != 0 || !overCanvas(x, y)) return false;
        keyboardX = pixelX(x); keyboardY = pixelY(y);
        editor.beginStroke(keyboardX, keyboardY);
        if (editor.tool() == PixelCanvasHistory.Tool.PICKER) pickedColor.accept(editor.color());
        return true;
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button != 0 || !editor.drawing()) return false;
        if (!currentDraft.getAsBoolean()) { editor.finishStroke(); return true; }
        if (overCanvas(x, y)) editor.continueStroke(pixelX(x), pixelY(y));
        else editor.continueStroke(-1, -1);
        return true;
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button != 0 || !editor.drawing()) return false;
        editor.finishStroke();
        return true;
    }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!active || !isFocused() || !currentDraft.getAsBoolean()) return false;
        if ((modifiers & 2) != 0 && key == 90) { if ((modifiers & 1) != 0) editor.redo(); else editor.undo(); return true; }
        if ((modifiers & 2) != 0 && key == 89) { editor.redo(); return true; }
        if (key >= 262 && key <= 265) {
            if (key == 262) keyboardX = Math.min(CutieMarkData.SIZE - 1, keyboardX + 1);
            if (key == 263) keyboardX = Math.max(0, keyboardX - 1);
            if (key == 264) keyboardY = Math.min(CutieMarkData.SIZE - 1, keyboardY + 1);
            if (key == 265) keyboardY = Math.max(0, keyboardY - 1);
            return true;
        }
        if (key == 32 || key == 257 || key == 335) {
            editor.beginStroke(keyboardX, keyboardY); editor.finishStroke();
            if (editor.tool() == PixelCanvasHistory.Tool.PICKER) pickedColor.accept(editor.color());
            return true;
        }
        return false;
    }
    @Override public void renderButton(DrawContext draw, int mouseX, int mouseY, float delta) {
        int[] pixels = editor.pixels();
        int cell = side(width) / CutieMarkData.SIZE;
        draw.fill(left() - 2, top() - 2, left() + side(width) + 2, top() + side(width) + 2, 0xFF78859A);
        for (int y = 0; y < CutieMarkData.SIZE; y++) for (int x = 0; x < CutieMarkData.SIZE; x++) {
            int color = pixels[y * CutieMarkData.SIZE + x];
            if ((color >>> 24) == 0) color = ((x + y) & 1) == 0 ? 0xFF333B49 : 0xFF485263;
            int px = left() + x * cell, py = top() + y * cell;
            draw.fill(px, py, px + cell, py + cell, color);
            if (cell >= 6) draw.drawBorder(px, py, cell, cell, 0x337D8DA8);
        }
        int hx = -1, hy = -1;
        if (overCanvas(mouseX, mouseY)) { hx = pixelX(mouseX); hy = pixelY(mouseY); }
        else if (isFocused()) { hx = keyboardX; hy = keyboardY; }
        if (hx >= 0) draw.drawBorder(left() + hx * cell, top() + hy * cell, cell, cell, 0xFFFFFFFF);
        draw.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,
                Text.translatable("text.magicaland.customize.mark.canvas_size"), getX() + width / 2, top() + side(width) + 5, 0xFFB6C0D2);
        draw.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,
                Text.translatable("text.magicaland.customize.mark.direction"), getX() + width / 2, top() + side(width) + 16, 0xFF94A0B5);
    }
    @Override protected void appendClickableNarrations(NarrationMessageBuilder builder) { appendDefaultNarrations(builder); }
}
