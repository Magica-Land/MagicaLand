package top.csituka.magicaland.client.gui.tab;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import top.csituka.magicaland.client.config.Config;
import top.csituka.magicaland.client.gui.ConsoleScreen;
import top.csituka.magicaland.client.gui.widget.CustomButtonWidget;

public class SettingsTab implements TabContent {

    @Override
    public void init(ConsoleScreen screen, int x, int y, int width, int height) {
        Config config = Config.getInstance();
        int buttonWidth = Math.min(250, width - 20);
        int buttonX = x + (width - buttonWidth) / 2;
        int currentY = y + 60;
        int buttonSpacing = 24;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.translatable(config.replacePlayerModel ? "text.magicaland.config.replace_model.on"
                        : "text.magicaland.config.replace_model.off"),
                false,
                button -> {
                    config.replacePlayerModel = !config.replacePlayerModel;
                    Config.save();
                    button.setMessage(
                            Text.translatable(config.replacePlayerModel ? "text.magicaland.config.replace_model.on"
                                    : "text.magicaland.config.replace_model.off"));
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.literal("开关: ON"),
                false,
                button -> {
                    if (button.getMessage().getString().endsWith("ON")) {
                        button.setMessage(Text.literal("开关: OFF"));
                    } else {
                        button.setMessage(Text.literal("开关: ON"));
                    }
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.literal("Placeholder Setting 2: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low")) button.setMessage(Text.literal("Placeholder Setting 2: Medium"));
                    else if (msg.endsWith("Medium")) button.setMessage(Text.literal("Placeholder Setting 2: High"));
                    else button.setMessage(Text.literal("Placeholder Setting 2: Low"));
                }));
    }

    @Override
    public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
        // 滚木
    }
}
