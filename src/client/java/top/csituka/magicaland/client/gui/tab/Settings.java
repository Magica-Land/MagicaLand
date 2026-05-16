package top.csituka.magicaland.client.gui.tab;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import top.csituka.magicaland.client.config.Config;
import top.csituka.magicaland.client.gui.ConfigScreen;
import top.csituka.magicaland.client.gui.widget.CustomButton;
import top.csituka.magicaland.client.gui.widget.SettingsList;
import top.csituka.magicaland.client.gui.widget.Toggle;

public class Settings implements TabContent {
    private SettingsList listWidget;

    @Override
    public void init(ConfigScreen screen, int x, int y, int width, int height) {
        Config config = Config.getInstance();
        int buttonWidth = Math.min(250, width - 20);
        int buttonX = x + (width - buttonWidth) / 2;

        int topMargin = 40;
        int bottomMargin = 40;

        listWidget = new SettingsList(MinecraftClient.getInstance(), width, height, y + topMargin,
                y + height - bottomMargin, 24);
        listWidget.setLeftPos(x);

        listWidget.addWidget(new Toggle(buttonX, 0, buttonWidth, 20,
                Text.translatable("text.magicaland.config.replace_model.name"),
                config.replacePlayerModel,
                toggle -> {
                    config.replacePlayerModel = toggle.getState();
                    Config.save();
                }));

        listWidget.addWidget(new Toggle(buttonX, 0, buttonWidth, 20,
                Text.translatable("text.magicaland.config.first_person_magic_glow.name"),
                config.firstPersonMagicGlow,
                toggle -> {
                    config.firstPersonMagicGlow = toggle.getState();
                    Config.save();
                }));

        listWidget.addWidget(new CustomButton(buttonX, 0, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low"))
                        button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium"))
                        button.setMessage(Text.literal("占位: High"));
                    else
                        button.setMessage(Text.literal("占位: Low"));
                }));

        listWidget.addWidget(new CustomButton(buttonX, 0, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low"))
                        button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium"))
                        button.setMessage(Text.literal("占位: High"));
                    else
                        button.setMessage(Text.literal("占位: Low"));
                }));

        listWidget.addWidget(new CustomButton(buttonX, 0, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low"))
                        button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium"))
                        button.setMessage(Text.literal("占位: High"));
                    else
                        button.setMessage(Text.literal("占位: Low"));
                }));

        listWidget.addWidget(new CustomButton(buttonX, 0, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low"))
                        button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium"))
                        button.setMessage(Text.literal("占位: High"));
                    else
                        button.setMessage(Text.literal("占位: Low"));
                }));

        listWidget.addWidget(new CustomButton(buttonX, 0, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low"))
                        button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium"))
                        button.setMessage(Text.literal("占位: High"));
                    else
                        button.setMessage(Text.literal("占位: Low"));
                }));

        listWidget.addWidget(new CustomButton(buttonX, 0, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low"))
                        button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium"))
                        button.setMessage(Text.literal("占位: High"));
                    else
                        button.setMessage(Text.literal("占位: Low"));
                }));

        listWidget.addWidget(new CustomButton(buttonX, 0, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low"))
                        button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium"))
                        button.setMessage(Text.literal("占位: High"));
                    else
                        button.setMessage(Text.literal("占位: Low"));
                }));

        listWidget.addWidget(new CustomButton(buttonX, 0, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low"))
                        button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium"))
                        button.setMessage(Text.literal("占位: High"));
                    else
                        button.setMessage(Text.literal("占位: Low"));
                }));

        listWidget.addWidget(new CustomButton(buttonX, 0, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low"))
                        button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium"))
                        button.setMessage(Text.literal("占位: High"));
                    else
                        button.setMessage(Text.literal("占位: Low"));
                }));

        listWidget.addWidget(new CustomButton(buttonX, 0, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low"))
                        button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium"))
                        button.setMessage(Text.literal("占位: High"));
                    else
                        button.setMessage(Text.literal("占位: Low"));
                }));

        listWidget.addWidget(new CustomButton(buttonX, 0, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low"))
                        button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium"))
                        button.setMessage(Text.literal("占位: High"));
                    else
                        button.setMessage(Text.literal("占位: Low"));
                }));

        listWidget.addWidget(new CustomButton(buttonX, 0, buttonWidth, 20,
                Text.literal("占位: Medium"),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    if (msg.endsWith("Low"))
                        button.setMessage(Text.literal("占位: Medium"));
                    else if (msg.endsWith("Medium"))
                        button.setMessage(Text.literal("占位: High"));
                    else
                        button.setMessage(Text.literal("占位: Low"));
                }));

        listWidget.centerIfShort();
        screen.addConsoleElement(listWidget);
    }

    @Override
    public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta,
            float alpha) {
        if (listWidget != null) {
            listWidget.render(context, mouseX, mouseY, delta);
        }
    }
}
