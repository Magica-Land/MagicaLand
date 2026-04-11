package top.csituka.magicaland.client.gui.tab;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import top.csituka.magicaland.client.gui.ConsoleScreen;

public class AboutTab implements TabContent {

    @Override
    public void init(ConsoleScreen screen, int x, int y, int width, int height) {
        // 滚木
    }

    @Override
    public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int currentY = y + (height - 15) / 2;

        Text descriptionText = Text.translatable("text.magicaland.console.about.description");
        int descriptionX = x + (width - textRenderer.getWidth(descriptionText)) / 2;
        context.drawTextWithShadow(textRenderer, descriptionText, descriptionX, currentY, 0xFFFFFF);
    }
}
