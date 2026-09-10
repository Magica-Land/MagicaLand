package top.csituka.magicaland.client.gui.ponycustom;

import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

final class ActionRowWidget extends ClickableWidget {
    record Action(Text label, BooleanSupplier enabled, BooleanSupplier selected, Runnable run) {}
    private final List<Action> actions;
    private int focusedAction;

    ActionRowWidget(int x, int width, Action... actions) {
        super(x, 0, width, 20, Text.empty());
        this.actions = List.of(actions);
    }
    static Action action(Text label, BooleanSupplier enabled, BooleanSupplier selected, Runnable run) {
        return new Action(label, enabled, selected, run);
    }
    static Action action(Text label, Runnable run) { return action(label, () -> true, () -> false, run); }
    private int left(int index) { return getX() + index * width / actions.size(); }
    private int right(int index) { return getX() + (index + 1) * width / actions.size() - 3; }
    private void invoke(int index) {
        Action action = actions.get(index);
        if (!action.enabled.getAsBoolean()) return;
        focusedAction = index;
        action.run.run();
        playDownSound(MinecraftClient.getInstance().getSoundManager());
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (!active || !visible || button != 0 || !isMouseOver(x, y)) return false;
        for (int i = 0; i < actions.size(); i++) if (x >= left(i) && x < right(i)) { invoke(i); return true; }
        return true;
    }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!active || !visible || !isFocused()) return false;
        if (key == 262 || key == 263) { focusedAction = Math.floorMod(focusedAction + (key == 262 ? 1 : -1), actions.size()); return true; }
        if (key == 257 || key == 335 || key == 32) { invoke(focusedAction); return true; }
        return false;
    }
    @Override public void renderButton(DrawContext draw, int mouseX, int mouseY, float delta) {
        var font = MinecraftClient.getInstance().textRenderer;
        for (int i = 0; i < actions.size(); i++) {
            Action action = actions.get(i);
            boolean enabled = active && action.enabled.getAsBoolean();
            boolean selected = action.selected.getAsBoolean();
            boolean hover = mouseX >= left(i) && mouseX < right(i) && mouseY >= getY() && mouseY < getY() + height;
            draw.fill(left(i), getY(), right(i), getY() + height, selected ? 0x995F477D : hover && enabled ? 0x664B586C : 0x333D4858);
            if (isFocused() && focusedAction == i) draw.drawBorder(left(i), getY(), right(i) - left(i), height, 0xFFA7BDE2);
            draw.drawCenteredTextWithShadow(font, font.trimToWidth(action.label.getString(), Math.max(0, right(i) - left(i) - 6)),
                    (left(i) + right(i)) / 2, getY() + 6, enabled ? 0xFFFFFFFF : 0xFF777F8D);
        }
        setMessage(actions.get(focusedAction).label);
    }
    @Override protected void appendClickableNarrations(NarrationMessageBuilder builder) { appendDefaultNarrations(builder); }
}
