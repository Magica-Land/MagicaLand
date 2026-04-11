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
        screen.addConsoleWidget(new CustomButtonWidget(buttonX, y + 60, buttonWidth, 20,
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
    }

    @Override
    public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
        // 滚木
    }
}
