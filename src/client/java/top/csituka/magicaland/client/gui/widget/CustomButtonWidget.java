package top.csituka.magicaland.client.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class CustomButtonWidget extends PressableWidget {
    private final Consumer<CustomButtonWidget> onPress;
    private float currentAlpha;

    public CustomButtonWidget(int x, int y, int width, int height, Text message, boolean isSelected,
            Consumer<CustomButtonWidget> onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.currentAlpha = isSelected ? 0.35f : 0.15f;
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
        fillRoundedRect(context, this.getX(), this.getY(), this.width, this.height, (alpha << 24) | 0xFFFFFF);

        if (this.alpha > 0.05f) {
            context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, this.getMessage(),
                    this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, (textAlpha << 24) | 0xFFFFFF);
        }
    }
}
