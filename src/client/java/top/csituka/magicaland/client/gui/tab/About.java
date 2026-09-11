package top.csituka.magicaland.client.gui.tab;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import top.csituka.magicaland.client.gui.ConfigScreen;

public class About implements TabContent {

    private static final Identifier ICON = new Identifier("magicaland", "icon-full.png");
    private static final String VERSION = FabricLoader.getInstance().getModContainer("magicaland")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("Unknown");

    @Override
    public void init(ConfigScreen screen, int x, int y, int width, int height) {
        // 滚木
    }

    @Override
    public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta,
            float alpha) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        int iconWidth = 128;
        int iconHeight = 64;
        int iconX = x + (width - iconWidth) / 2;
        int iconY = y + (height - iconHeight - 40) / 2;

        RenderSystem.setShaderColor(1, 1, 1, alpha);
        context.drawTexture(ICON, iconX, iconY, 0, 0, iconWidth, iconHeight, iconWidth, iconHeight);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        Text descriptionText = Text.translatable("text.magicaland.console.about.description");
        int descriptionX = x + (width - textRenderer.getWidth(descriptionText)) / 2;
        int descriptionY = iconY + iconHeight + 10;
        context.drawTextWithShadow(textRenderer, descriptionText, descriptionX, descriptionY,
                withAlpha(0xFFFFFF, alpha));

        Text versionText = Text.literal("Version " + VERSION);
        int versionX = x + (width - textRenderer.getWidth(versionText)) / 2;
        int versionY = descriptionY + textRenderer.fontHeight + 5;
        context.drawTextWithShadow(textRenderer, versionText, versionX, versionY, withAlpha(0xAAAAAA, alpha));
    }

    private static int withAlpha(int color, float alpha) {
        return (Math.round(255 * Math.max(0, Math.min(1, alpha))) << 24) | color;
    }
}
