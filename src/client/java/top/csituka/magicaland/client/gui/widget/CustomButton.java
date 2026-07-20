package top.csituka.magicaland.client.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

import net.minecraft.client.gui.tooltip.Tooltip;

public class CustomButton extends PressableWidget {
    private final Consumer<CustomButton> onPress;
    private float currentAlpha;
    private final boolean showArrow;
    private final boolean noBackground;
    private final boolean textAlignLeft;
    private String valueText;
    private static final String ARROW = "  →";

    public CustomButton(int x, int y, int width, int height, Text message, boolean isSelected,
            Consumer<CustomButton> onPress) {
        this(x, y, width, height, message, isSelected, onPress, false, false);
    }

    public CustomButton(int x, int y, int width, int height, Text message, Text tooltipText, boolean isSelected,
            Consumer<CustomButton> onPress) {
        this(x, y, width, height, message, isSelected, onPress, false, false);
        if (tooltipText != null) {
            this.setTooltip(Tooltip.of(tooltipText));
        }
    }

    public CustomButton(int x, int y, int width, int height, Text message, boolean isSelected,
            Consumer<CustomButton> onPress, boolean showArrow) {
        this(x, y, width, height, message, isSelected, onPress, showArrow, false);
    }

    public CustomButton(int x, int y, int width, int height, Text message, boolean isSelected,
            Consumer<CustomButton> onPress, boolean showArrow, boolean noBackground) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.currentAlpha = isSelected ? 0.35f : 0.15f;
        this.showArrow = showArrow;
        this.noBackground = noBackground;
        this.textAlignLeft = showArrow || noBackground;
    }

    public CustomButton(int x, int y, int width, int height, Text label, String value, boolean isSelected,
            Consumer<CustomButton> onPress) {
        super(x, y, width, height, label);
        this.onPress = onPress;
        this.currentAlpha = isSelected ? 0.35f : 0.15f;
        this.showArrow = false;
        this.noBackground = false;
        this.textAlignLeft = false;
        this.valueText = value;
    }

    public void setValue(String value) {
        this.valueText = value;
    }

    @Override
    public void onPress() {
        if (this.onPress != null) {
            this.onPress.accept(this);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
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
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.alpha < 0.05f) return;
        boolean hovered = this.isHovered();
        float targetAlpha = 0.15f;

        if (!this.active || hovered) {
            targetAlpha = 0.35f;
        }

        if (currentAlpha < targetAlpha) {
            currentAlpha = Math.min(targetAlpha, currentAlpha + 0.05f);
        } else if (currentAlpha > targetAlpha) {
            currentAlpha = Math.max(targetAlpha, currentAlpha - 0.05f);
        }

        float buttonAlpha = this.alpha;
        if (!this.active) {
            buttonAlpha *= 0.4f;
        }

        int alpha = (int) (currentAlpha * buttonAlpha * 255);
        int textAlpha = (int) (buttonAlpha * 255);

        if (!this.noBackground && alpha > 0) {
            fillRoundedRect(context, this.getX(), this.getY(), this.width, this.height, (alpha << 24) | 0xFFFFFF);
        }

        if (textAlpha > 0) {
            var textRenderer = MinecraftClient.getInstance().textRenderer;
            int textY = this.getY() + (this.height - 8) / 2;

            if (valueText != null) {
                context.drawTextWithShadow(textRenderer, this.getMessage(),
                        this.getX() + 6, textY, (textAlpha << 24) | 0xFFFFFF);
                int valueWidth = textRenderer.getWidth(valueText);
                context.drawTextWithShadow(textRenderer, valueText,
                        this.getX() + this.width - 6 - valueWidth, textY,
                        (textAlpha << 24) | 0xFFFFFF);
            } else if (this.textAlignLeft) {
                int textX = this.getX() + 6;
                context.drawTextWithShadow(textRenderer, this.getMessage(),
                        textX, textY, (textAlpha << 24) | 0xFFFFFF);
                if (this.showArrow) {
                    int arrowX = this.getX() + this.width - textRenderer.getWidth(ARROW) - 6;
                    context.drawTextWithShadow(textRenderer, ARROW,
                            arrowX, textY, (textAlpha << 24) | 0xFFFFFF);
                }
            } else {
                context.drawCenteredTextWithShadow(textRenderer, this.getMessage(),
                        this.getX() + this.width / 2, textY, (textAlpha << 24) | 0xFFFFFF);
            }
        }
    }
}
