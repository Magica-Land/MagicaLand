package top.csituka.magicaland.client.gui.tab;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import top.csituka.magicaland.client.gui.ConfigScreen;

import java.util.ArrayList;
import java.util.List;

public class TabAnimator {
    private boolean isAnimatingContent = false;
    private long transitionStartNanos;
    private float previousAlpha = 0;
    private float currentAlpha = 1;

    private static final long FADE_OUT_NANOS = 180_000_000L;
    private static final long DELAY_NANOS = 100_000_000L;
    private static final long FADE_IN_NANOS = 180_000_000L;

    private ConfigScreen.Tab previousTab = null;
    private final List<ClickableWidget> previousWidgets = new ArrayList<>();

    public void startTransition(ConfigScreen.Tab previousTab, ConfigScreen.Tab currentTab, int height,
            List<ClickableWidget> oldWidgets) {
        this.previousTab = previousTab;
        this.isAnimatingContent = true;
        this.transitionStartNanos = System.nanoTime();
        this.previousAlpha = 1;
        this.currentAlpha = 0;

        this.previousWidgets.clear();
        this.previousWidgets.addAll(oldWidgets);
    }

    public boolean isAnimating() {
        return this.isAnimatingContent;
    }

    public float getCurrentAlpha() {
        return this.currentAlpha;
    }

    public void update() {
        if (!this.isAnimatingContent) return;
        long elapsed = System.nanoTime() - this.transitionStartNanos;
        if (elapsed >= FADE_OUT_NANOS + DELAY_NANOS + FADE_IN_NANOS) {
            this.previousAlpha = 0;
            this.currentAlpha = 1;
            this.isAnimatingContent = false;
            this.previousTab = null;
            this.previousWidgets.clear();
            return;
        }
        if (elapsed < FADE_OUT_NANOS) {
            float progress = smoothstep(elapsed / (float) FADE_OUT_NANOS);
            this.previousAlpha = 1 - progress;
            this.currentAlpha = 0;
        } else if (elapsed < FADE_OUT_NANOS + DELAY_NANOS) {
            this.previousAlpha = 0;
            this.currentAlpha = 0;
        } else {
            float progress = smoothstep((elapsed - FADE_OUT_NANOS - DELAY_NANOS) / (float) FADE_IN_NANOS);
            this.previousAlpha = 0;
            this.currentAlpha = progress;
        }
    }

    public void render(DrawContext context, int rightX, int rightWidth, int height, int padding, float delta) {
        if (!this.isAnimatingContent || this.previousTab == null) {
            return;
        }

        context.getMatrices().push();
        this.previousTab.getContent().render(context, rightX, 0, rightWidth - padding, height, -1, -1, delta,
                this.previousAlpha);
        this.previousTab.getContent().postRender(context, rightX, 0, rightWidth - padding, height, -1, -1, delta,
                this.previousAlpha);

        for (ClickableWidget widget : this.previousWidgets) {
            widget.setAlpha(this.previousAlpha);
            widget.render(context, -1, -1, delta);
        }

        context.getMatrices().pop();
    }

    private static float smoothstep(float progress) {
        float value = Math.max(0, Math.min(1, progress));
        return value * value * (3 - 2 * value);
    }
}
