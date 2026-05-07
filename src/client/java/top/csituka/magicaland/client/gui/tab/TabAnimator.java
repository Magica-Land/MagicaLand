package top.csituka.magicaland.client.gui.tab;

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

    private ConfigScreen.Tab previousTab = null;
    private final List<ClickableWidget> previousWidgets = new ArrayList<>();

    public void startTransition(ConfigScreen.Tab previousTab, ConfigScreen.Tab currentTab, int height,
            List<ClickableWidget> oldWidgets) {
        this.previousTab = previousTab;
        int prevIndex = previousTab.ordinal();
        int newIndex = currentTab.ordinal();
        this.animationDirection = newIndex > prevIndex ? 1 : -1;

        this.isAnimatingContent = true;
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

    public void update() {
        if (this.isAnimatingContent) {
            float diff = this.targetContentOffset - this.contentOffset;
            if (Math.abs(diff) > 0.5f) {
                this.contentOffset += diff * 0.2f;
            } else {
                this.contentOffset = this.targetContentOffset;
                this.isAnimatingContent = false;
                this.previousTab = null;
                this.previousWidgets.clear();
            }
        }
    }

    public void render(DrawContext context, int rightX, int rightWidth, int height, int padding, float delta) {
        if (!this.isAnimatingContent || this.previousTab == null) {
            return;
        }

        float prevOffset = this.contentOffset - (this.animationDirection * height);

        context.getMatrices().push();
        context.getMatrices().translate(0, prevOffset, 0);

        this.previousTab.getContent().render(context, rightX, 0, rightWidth - padding, height, -1, -1, delta);

        for (ClickableWidget widget : this.previousWidgets) {
            widget.render(context, -1, -1, delta);
        }

        context.getMatrices().pop();
    }
}
