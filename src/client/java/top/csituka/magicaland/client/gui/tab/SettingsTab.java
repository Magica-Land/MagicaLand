package top.csituka.magicaland.client.gui.tab;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import top.csituka.magicaland.client.config.Config;
import top.csituka.magicaland.client.gui.ConsoleScreen;
import top.csituka.magicaland.client.gui.widget.CustomButtonWidget;
import top.csituka.magicaland.client.gui.widget.ToggleWidget;

public class SettingsTab implements TabContent {

    @Override
    public void init(ConsoleScreen screen, int x, int y, int width, int height) {
        Config config = Config.getInstance();
        int buttonWidth = Math.min(250, width - 20);
        int buttonX = x + (width - buttonWidth) / 2;
        int currentY = y + 60;
        int buttonSpacing = 24;

        screen.addConsoleWidget(new ToggleWidget(buttonX, currentY, buttonWidth, 20,
                Text.translatable("text.magicaland.config.replace_model.name"),
                config.replacePlayerModel,
                toggle -> {
                    config.replacePlayerModel = toggle.getState();
                    Config.save();
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new ToggleWidget(buttonX, currentY, buttonWidth, 20,
                Text.translatable("text.magicaland.config.first_person_magic_glow.name"),
                config.firstPersonMagicGlow,
                toggle -> {
                    config.firstPersonMagicGlow = toggle.getState();
                    Config.save();
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low")) button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium")) button.setMessage(Text.literal("占位: High"));
                    else button.setMessage(Text.literal("占位: Low"));
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low")) button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium")) button.setMessage(Text.literal("占位: High"));
                    else button.setMessage(Text.literal("占位: Low"));
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low")) button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium")) button.setMessage(Text.literal("占位: High"));
                    else button.setMessage(Text.literal("占位: Low"));
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low")) button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium")) button.setMessage(Text.literal("占位: High"));
                    else button.setMessage(Text.literal("占位: Low"));
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low")) button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium")) button.setMessage(Text.literal("占位: High"));
                    else button.setMessage(Text.literal("占位: Low"));
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low")) button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium")) button.setMessage(Text.literal("占位: High"));
                    else button.setMessage(Text.literal("占位: Low"));
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low")) button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium")) button.setMessage(Text.literal("占位: High"));
                    else button.setMessage(Text.literal("占位: Low"));
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low")) button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium")) button.setMessage(Text.literal("占位: High"));
                    else button.setMessage(Text.literal("占位: Low"));
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low")) button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium")) button.setMessage(Text.literal("占位: High"));
                    else button.setMessage(Text.literal("占位: Low"));
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low")) button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium")) button.setMessage(Text.literal("占位: High"));
                    else button.setMessage(Text.literal("占位: Low"));
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low")) button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium")) button.setMessage(Text.literal("占位: High"));
                    else button.setMessage(Text.literal("占位: Low"));
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low")) button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium")) button.setMessage(Text.literal("占位: High"));
                    else button.setMessage(Text.literal("占位: Low"));
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low")) button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium")) button.setMessage(Text.literal("占位: High"));
                    else button.setMessage(Text.literal("占位: Low"));
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low")) button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium")) button.setMessage(Text.literal("占位: High"));
                    else button.setMessage(Text.literal("占位: Low"));
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low")) button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium")) button.setMessage(Text.literal("占位: High"));
                    else button.setMessage(Text.literal("占位: Low"));
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low")) button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium")) button.setMessage(Text.literal("占位: High"));
                    else button.setMessage(Text.literal("占位: Low"));
                }));
        
        currentY += buttonSpacing;

        screen.addConsoleWidget(new CustomButtonWidget(buttonX, currentY, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low")) button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium")) button.setMessage(Text.literal("占位: High"));
                    else button.setMessage(Text.literal("占位: Low"));
                }));
    }

    @Override
    public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
        // 滚木
    }
}
