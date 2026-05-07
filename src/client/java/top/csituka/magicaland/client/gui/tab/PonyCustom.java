package top.csituka.magicaland.client.gui.tab;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import top.csituka.magicaland.client.config.Config;
import top.csituka.magicaland.client.gui.ConfigScreen;
import top.csituka.magicaland.client.gui.widget.CustomButton;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PonyCustom implements TabContent {
    private final List<ClickableWidget> widgets = new ArrayList<>();
    private int rightX;
    private int rightWidth;
    private int rightHeight;

    private static final String[] FRONT_MANE_STYLES = {"TS", "RD", "RR", "PP", "AJ", "FS"};

    private static final String[] BACK_MANE_STYLES = {"TS", "RD", "RR", "PP", "AJ", "FS"};

    @Override
    public void init(ConfigScreen screen, int x, int y, int width, int height) {
        Config config = Config.getInstance();
        this.rightX = x;
        this.rightWidth = width;
        this.rightHeight = height;
        this.widgets.clear();

        int buttonWidth = Math.min(200, width - 10);
        int buttonHeight = 20;
        int spacing = 5;

        int totalContentHeight = buttonHeight * 2 + spacing;

        int startY = (height - totalContentHeight) / 2;

        int btnX = width - buttonWidth - 4;

        CustomButton frontBtn = createStyleButton(btnX, startY, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.front_mane_style.name"),
                config.frontManeStyle, FRONT_MANE_STYLES, newStyle -> {
                    config.frontManeStyle = newStyle;
                    Config.save();
                });
        this.widgets.add(frontBtn);
        screen.addConsoleWidget(frontBtn);

        CustomButton backBtn = createStyleButton(btnX, startY + buttonHeight + spacing, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.back_mane_style.name"),
                config.backManeStyle, BACK_MANE_STYLES, newStyle -> {
                    config.backManeStyle = newStyle;
                    Config.save();
                });
        this.widgets.add(backBtn);
        screen.addConsoleWidget(backBtn);
    }

    private CustomButton createStyleButton(int x, int y, int width, int height, Text label, String currentStyle,
            String[] styles, Consumer<String> onStyleChanged) {
        return new CustomButton(x, y, width, height,
                Text.literal(label.getString() + ": " + currentStyle),
                false,
                button -> {
                    String msg = button.getMessage().getString();
                    String current = msg.substring(msg.lastIndexOf(": ") + 2);
                    String next = getNextStyle(current, styles);
                    button.setMessage(Text.literal(label.getString() + ": " + next));
                    onStyleChanged.accept(next);
                });
    }

    private String getNextStyle(String current, String[] styles) {
        for (int i = 0; i < styles.length; i++) {
            if (styles[i].equals(current)) {
                return styles[(i + 1) % styles.length];
            }
        }
        return styles[0];
    }

    @Override
    public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
        // 滚木
    }
}