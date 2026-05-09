package top.csituka.magicaland.client.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import java.awt.Color;
import java.util.function.Consumer;

public class ColorPicker extends ClickableWidget {
    private static ColorPicker openPicker = null;

    private final Consumer<String> onColorChanged;
    private String currentColor;
    private boolean open = false;
    private float currentAlpha = 0.15f;

    private static final int PICKER_WIDTH = 75;
    private static final int PICKER_HEIGHT = 103;
    private static final int HUE_HEIGHT = 8;
    private static final int PADDING = 4;
    private static final int INPUT_HEIGHT = 12;

    private float h, s, v;
    private String hexInput = "";
    private boolean inputFocused = false;
    private int cursorTick = 0;
    
    private enum DragMode {
        NONE,
        SB_SQUARE,
        HUE_SLIDER
    }
    
    private DragMode dragMode = DragMode.NONE;

    public ColorPicker(int x, int y, int width, int height, Text message, String initialColor, Consumer<String> onColorChanged) {
        super(x, y, width, height, message);
        this.currentColor = initialColor;
        this.onColorChanged = onColorChanged;
        parseColor(initialColor);
        this.hexInput = getHexNoAlpha();
    }

    private void parseColor(String hex) {
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            if (hex.length() > 6) hex = hex.substring(hex.length() - 6);
            int colorVal = Integer.parseInt(hex, 16);
            int r = (colorVal >> 16) & 0xFF;
            int g = (colorVal >> 8) & 0xFF;
            int b = colorVal & 0xFF;

            float[] hsv = Color.RGBtoHSB(r, g, b, null);
            this.h = hsv[0];
            this.s = hsv[1];
            this.v = hsv[2];
        } catch (Exception e) {
            this.h = 0;
            this.s = 0;
            this.v = 1;
        }
    }

    private String getHexColor() {
        int rgb = Color.HSBtoRGB(h, s, v);
        return String.format("#FF%06X", (rgb & 0xFFFFFF));
    }

    private String getHexNoAlpha() {
        int rgb = Color.HSBtoRGB(h, s, v);
        return String.format("#%06X", (rgb & 0xFFFFFF));
    }

    private int getPickerX() { return this.getX() + this.width - PICKER_WIDTH; }
    private int getPickerY() { return this.getY() + this.height + 10; }
    private int getSBSize() { return PICKER_WIDTH - PADDING * 2; }
    private int getSBX() { return getPickerX() + PADDING; }
    private int getSBY() { return getPickerY() + PADDING; }
    private int getHueY() { return getSBY() + getSBSize() + PADDING; }
    private int getInputY() { return getHueY() + HUE_HEIGHT + PADDING; }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!this.visible) return false;
        boolean overButton = mouseX >= this.getX() && mouseX < this.getX() + this.width && mouseY >= this.getY() && mouseY < this.getY() + this.height;
        if (open) {
            int px = getPickerX();
            int py = getPickerY();
            boolean overPicker = mouseX >= px && mouseX < px + PICKER_WIDTH && mouseY >= py && mouseY < py + PICKER_HEIGHT;
            return overButton || overPicker;
        }
        return overButton;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active || button != 0) return false;

        this.dragMode = DragMode.NONE;
        this.inputFocused = false;

        if (mouseX >= this.getX() && mouseX < this.getX() + this.width && mouseY >= this.getY() && mouseY < this.getY() + this.height) {
            if (!this.open) {
                if (openPicker != null && openPicker != this) {
                    openPicker.open = false;
                }
                openPicker = this;
            } else {
                openPicker = null;
            }
            this.open = !this.open;
            if (open) {
                this.hexInput = getHexNoAlpha();
            }
            this.playDownSound(MinecraftClient.getInstance().getSoundManager());
            return true;
        }

        if (open) {
            int sbX = getSBX();
            int sbY = getSBY();
            int sbSize = getSBSize();
            int hueY = getHueY();
            int inputY = getInputY();

            if (mouseX >= sbX && mouseX <= sbX + sbSize && mouseY >= sbY && mouseY <= sbY + sbSize) {
                this.dragMode = DragMode.SB_SQUARE;
                updateFromMouse(mouseX, mouseY);
                return true;
            } 
            
            if (mouseX >= sbX && mouseX <= sbX + sbSize && mouseY >= hueY - 4 && mouseY <= hueY + HUE_HEIGHT + 4) {
                this.dragMode = DragMode.HUE_SLIDER;
                updateFromMouse(mouseX, mouseY);
                return true;
            }

            if (mouseX >= sbX && mouseX <= sbX + sbSize && mouseY >= inputY && mouseY <= inputY + INPUT_HEIGHT) {
                this.inputFocused = true;
                this.setFocused(true);
                return true;
            }

            int px = getPickerX();
            int py = getPickerY();
            if (mouseX >= px && mouseX < px + PICKER_WIDTH && mouseY >= py && mouseY < py + PICKER_HEIGHT) {
                return true;
            }
            
            this.open = false;
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (open && inputFocused) {
            String validChars = "0123456789ABCDEFabcdef#";
            if (validChars.indexOf(chr) != -1) {
                if (hexInput.length() < 7 || (chr == '#' && !hexInput.contains("#"))) {
                    hexInput += chr;
                    tryParseHexInput();
                }
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (open && inputFocused) {
            if (keyCode == 259) {
                if (!hexInput.isEmpty()) {
                    hexInput = hexInput.substring(0, hexInput.length() - 1);
                    tryParseHexInput();
                }
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                inputFocused = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void tryParseHexInput() {
        String hex = hexInput;
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() == 6) {
            try {
                int colorVal = Integer.parseInt(hex, 16);
                int r = (colorVal >> 16) & 0xFF;
                int g = (colorVal >> 8) & 0xFF;
                int b = colorVal & 0xFF;
                float[] hsv = Color.RGBtoHSB(r, g, b, null);
                this.h = hsv[0];
                this.s = hsv[1];
                this.v = hsv[2];
                updateColor();
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (open && button == 0 && dragMode != DragMode.NONE) {
            updateFromMouse(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.dragMode = DragMode.NONE;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateFromMouse(double mouseX, double mouseY) {
        int sbX = getSBX();
        int sbY = getSBY();
        int sbSize = getSBSize();

        if (dragMode == DragMode.SB_SQUARE) {
            this.s = MathHelper.clamp((float) (mouseX - sbX) / sbSize, 0, 1);
            this.v = MathHelper.clamp(1.0f - (float) (mouseY - sbY) / sbSize, 0, 1);
            updateColor();
        } else if (dragMode == DragMode.HUE_SLIDER) {
            this.h = MathHelper.clamp((float) (mouseX - sbX) / sbSize, 0, 1);
            updateColor();
        }
    }

    private void updateColor() {
        this.currentColor = getHexColor();
        if (!inputFocused) {
            this.hexInput = getHexNoAlpha();
        }
        if (onColorChanged != null) {
            onColorChanged.accept(this.currentColor);
        }
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        renderMainButton(context, mouseX, mouseY);
        if (open) {
            renderPicker(context, mouseX, mouseY);
        }
    }

    private void renderMainButton(DrawContext context, int mouseX, int mouseY) {
        boolean hovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
        float targetAlpha = (hovered || open) ? 0.35f : 0.15f;

        if (currentAlpha < targetAlpha) {
            currentAlpha = Math.min(targetAlpha, currentAlpha + 0.05f);
        } else if (currentAlpha > targetAlpha) {
            currentAlpha = Math.max(targetAlpha, currentAlpha - 0.05f);
        }

        int alphaVal = (int) (currentAlpha * this.alpha * 255);
        int textAlpha = (int) (Math.max(0.04f, this.alpha) * 255);

        fillRoundedRect(context, this.getX(), this.getY(), this.width, this.height, (alphaVal << 24) | 0xFFFFFF);

        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, this.getMessage(),
                this.getX() + 6, this.getY() + (this.height - 8) / 2, (textAlpha << 24) | 0xFFFFFF);

        int previewWidth = 30;
        int previewHeight = 14;
        int previewX = this.getX() + this.width - previewWidth - 4;
        int previewY = this.getY() + (this.height - previewHeight) / 2;

        try {
            String hex = currentColor.startsWith("#") ? currentColor.substring(1) : currentColor;
            if (hex.length() > 6) hex = hex.substring(hex.length() - 6);
            int colorInt = (int) Long.parseLong(hex, 16);
            fillRoundedRect(context, previewX, previewY, previewWidth, previewHeight, 0xFF000000 | (colorInt & 0xFFFFFF));
        } catch (Exception e) {
            fillRoundedRect(context, previewX, previewY, previewWidth, previewHeight, 0xFFFFFFFF);
        }
    }

    private void renderPicker(DrawContext context, int mouseX, int mouseY) {
        int px = getPickerX();
        int py = getPickerY();
        int sbX = getSBX();
        int sbY = getSBY();
        int sbSize = getSBSize();
        int hueY = getHueY();
        int inputY = getInputY();

        cursorTick++;

        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 400);

        fillRoundedRect(context, px, py, PICKER_WIDTH, PICKER_HEIGHT, 0x59FFFFFF);

        for (int i = 0; i < sbSize; i++) {
            float s_val = (float) i / sbSize;
            int colorTop = Color.HSBtoRGB(h, s_val, 1.0f);
            int colorBottom = Color.HSBtoRGB(h, s_val, 0.0f);
            context.fillGradient(sbX + i, sbY, sbX + i + 1, sbY + sbSize, 0xFF000000 | (colorTop & 0xFFFFFF), 0xFF000000 | (colorBottom & 0xFFFFFF));
        }

        int markerX = sbX + (int) (s * sbSize);
        int markerY = sbY + (int) ((1.0f - v) * sbSize);
        context.fill(markerX - 2, markerY - 2, markerX + 2, markerY + 2, 0xFFFFFFFF);
        context.fill(markerX - 1, markerY - 1, markerX + 1, markerY + 1, 0xFF000000);

        for (int i = 0; i < sbSize; i++) {
            float h_val = (float) i / sbSize;
            int hueColor = Color.HSBtoRGB(h_val, 1.0f, 1.0f);
            context.fill(sbX + i, hueY, sbX + i + 1, hueY + HUE_HEIGHT, 0xFF000000 | (hueColor & 0xFFFFFF));
        }

        int hMarkerX = sbX + (int) (h * sbSize);
        context.fill(hMarkerX - 1, hueY - 2, hMarkerX + 1, hueY + HUE_HEIGHT + 2, 0xFFFFFFFF);

        fillRoundedRect(context, sbX, inputY, sbSize, INPUT_HEIGHT, 0x40000000);
        if (inputFocused) {
            context.drawHorizontalLine(sbX, sbX + sbSize - 1, inputY, 0xFFFFFFFF);
            context.drawHorizontalLine(sbX, sbX + sbSize - 1, inputY + INPUT_HEIGHT - 1, 0xFFFFFFFF);
            context.drawVerticalLine(sbX, inputY, inputY + INPUT_HEIGHT - 1, 0xFFFFFFFF);
            context.drawVerticalLine(sbX + sbSize - 1, inputY, inputY + INPUT_HEIGHT - 1, 0xFFFFFFFF);
        }

        String displayText = hexInput;
        if (inputFocused && (cursorTick / 10) % 2 == 0) {
            displayText += "_";
        }
        
        context.drawText(MinecraftClient.getInstance().textRenderer, displayText, 
                sbX + 2, inputY + 2, 0xFFFFFFFF, false);

        context.getMatrices().pop();
    }

    private void fillRoundedRect(DrawContext context, int x, int y, int width, int height, int color) {
        int x1 = x;
        int y1 = y;
        int x2 = x + width;
        int y2 = y + height;
        context.fill(x1 + 2, y1, x2 - 2, y1 + 1, color);
        context.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, color);
        context.fill(x1, y1 + 2, x2, y2 - 2, color);
        context.fill(x1 + 1, y2 - 2, x2 - 1, y2 - 1, color);
        context.fill(x1 + 2, y2 - 1, x2 - 2, y2, color);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
