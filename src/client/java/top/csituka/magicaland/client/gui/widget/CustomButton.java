package top.csituka.magicaland.client.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class CustomButton extends PressableWidget {
    private final Consumer<CustomButton> onPress;
    private float currentAlpha;
    private final boolean showArrow;
    private final boolean noBackground;
    private final boolean textAlignLeft;
    private static final String ARROW = "  →";

    public CustomButton(int x, int y, int width, int height, Text message, boolean isSelected,
            Consumer<CustomButton> onPress) {
        this(x, y, width, height, message, isSelected, onPress, false, false);
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

        int alpha = (int) (currentAlpha * this.alpha * 255);
        int textAlpha = (int) (Math.max(0.04f, this.alpha) * 255);

        if (!this.noBackground) {
            fillRoundedRect(context, this.getX(), this.getY(), this.width, this.height, (alpha << 24) | 0xFFFFFF);
        }

        if (this.alpha > 0.05f) {
            if (this.textAlignLeft) {
                var textRenderer = MinecraftClient.getInstance().textRenderer;
                int textX = this.getX() + 6;
                int textY = this.getY() + (this.height - 8) / 2;
                context.drawTextWithShadow(textRenderer, this.getMessage(),
                        textX, textY, (textAlpha << 24) | 0xFFFFFF);
                if (this.showArrow) {
                    int arrowX = this.getX() + this.width - textRenderer.getWidth(ARROW) - 6;
                    context.drawTextWithShadow(textRenderer, ARROW,
                            arrowX, textY, (textAlpha << 24) | 0xFFFFFF);
                }
            } else {
                context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, this.getMessage(),
                        this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, (textAlpha << 24) | 0xFFFFFF);
            }
        }
    }
}
