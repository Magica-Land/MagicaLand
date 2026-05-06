package top.csituka.magicaland.client.gui.tab;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import top.csituka.magicaland.client.gui.ConfigScreen;

import java.util.ArrayList;
import java.util.List;

public class TabAnimator {
    private float contentOffset = 0;
    private float targetContentOffset = 0;
    private boolean isAnimatingContent = false;
    private int animationDirection = 1;
    private int animationHeight = 0;

    private float animationProgress = 0;
    private static final float ANIMATION_SPEED = 0.08f;

    private ConfigScreen.Tab previousTab = null;
    private final List<ClickableWidget> previousWidgets = new ArrayList<>();

    public void startTransition(ConfigScreen.Tab previousTab, ConfigScreen.Tab currentTab, int height,
                                List<ClickableWidget> oldWidgets) {
        this.previousTab = previousTab;
        int prevIndex = previousTab.ordinal();
        int newIndex = currentTab.ordinal();
        this.animationDirection = newIndex > prevIndex ? 1 : -1;
        this.animationHeight = height;

        this.isAnimatingContent = true;
        this.animationProgress = 0;
        this.contentOffset = this.animationDirection * height;
        this.targetContentOffset = 0;

        this.previousWidgets.clear();
        this.previousWidgets.addAll(oldWidgets);
    }

    public boolean isAnimating() {
        return this.isAnimatingContent;
    }

    public float getContentOffset() {
        return this.contentOffset;
    }

    public float getAnimationProgress() {
        return cubicBezier(this.animationProgress, 0.42f, 0.00f, 0.58f, 1.00f);
    }

    public void update() {
        if (this.isAnimatingContent) {
            this.animationProgress += ANIMATION_SPEED;
            if (this.animationProgress >= 1.0f) {
                this.animationProgress = 1.0f;
                this.contentOffset = this.targetContentOffset;
                this.isAnimatingContent = false;
                this.previousTab = null;
                this.previousWidgets.clear();
            } else {
                float t = cubicBezier(this.animationProgress, 0.42f, 0.00f, 0.58f, 1.00f);
                this.contentOffset = this.animationDirection * this.animationHeight * (1.0f - t);
            }
        }
    }

    private float cubicBezier(float t, float x1, float y1, float x2, float y2) {
        float cx = 3.0f * x1;
        float bx = 3.0f * (x2 - x1) - cx;
        float ax = 1.0f - cx - bx;

        float cy = 3.0f * y1;
        float by = 3.0f * (y2 - y1) - cy;
        float ay = 1.0f - cy - by;
        
        return ((ay * t + by) * t + cy) * t;
    }

    public void render(DrawContext context, int rightX, int rightWidth, int height, int padding, float delta) {
        if (!this.isAnimatingContent || this.previousTab == null) {
            return;
        }

        float t = getAnimationProgress();
        float prevAlpha = 1.0f - t;
        float prevOffset = this.contentOffset - (this.animationDirection * this.animationHeight);

        context.getMatrices().push();
        context.getMatrices().translate(0, prevOffset, 0);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, prevAlpha);

        this.previousTab.getContent().render(context, rightX, 0, rightWidth - padding, height, -1, -1, delta);

        for (ClickableWidget widget : this.previousWidgets) {
            widget.render(context, -1, -1, delta);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        context.getMatrices().pop();
    }
}
