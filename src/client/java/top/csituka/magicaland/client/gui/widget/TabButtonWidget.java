package top.csituka.magicaland.client.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class TabButtonWidget extends PressableWidget {
    private final Consumer<TabButtonWidget> onPress;
    private final ItemStack icon;

    public TabButtonWidget(int x, int y, int width, int height, Text message, boolean isSelected,
            ItemStack icon, Consumer<TabButtonWidget> onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.icon = icon;
        this.active = !isSelected;
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

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = this.isHovered();
        boolean isSelected = !this.active;

        int textColor = isSelected ? 0xFFFFFFFF : (hovered ? 0xFFDDDDDD : 0xFFAAAAAA);

        int textX = this.getX() + 8;
        if (this.icon != null && !this.icon.isEmpty()) {
            context.drawItem(this.icon, this.getX() + 8, this.getY() + (this.height - 16) / 2);
            textX = this.getX() + 30;
        }

        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, this.getMessage(),
                textX, this.getY() + (this.height - 8) / 2, textColor);
    }
}
