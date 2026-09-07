package top.csituka.magicaland.client.gui.tab;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.gui.ConfigScreen;
import top.csituka.magicaland.client.gui.tab.ponycustom.BodyPage;
import top.csituka.magicaland.client.gui.tab.ponycustom.FacePage;
import top.csituka.magicaland.client.gui.tab.ponycustom.GlowPage;
import top.csituka.magicaland.client.gui.tab.ponycustom.HornPage;
import top.csituka.magicaland.client.gui.tab.ponycustom.MainPage;
import top.csituka.magicaland.client.gui.tab.ponycustom.ManePage;
import top.csituka.magicaland.client.gui.tab.ponycustom.ModelPage;
import top.csituka.magicaland.client.gui.tab.ponycustom.PonyCustomPage;
import top.csituka.magicaland.client.gui.tab.ponycustom.PonyCustomPageContext;
import top.csituka.magicaland.client.gui.widget.ColorPicker;
import top.csituka.magicaland.client.gui.widget.SettingsList;
import top.csituka.magicaland.client.gui.widget.ViewCube;
import top.csituka.magicaland.client.model.GeckoPlayerAnimatable;
import top.csituka.magicaland.client.model.GeckoPlayerModel;
import top.csituka.magicaland.client.render.MagicGlow;
import top.csituka.magicaland.client.render.PonyRenderer;
import top.csituka.magicaland.client.util.RenderLayerHelper;

public class PonyCustom implements TabContent, ViewCube.RotationTarget {
    private static final Logger LOGGER = LoggerFactory.getLogger(PonyCustom.class);
    private static final PonyCustomPage[] PAGES = {
            new ModelPage(),
            new MainPage(),
            new ManePage(),
            new FacePage(),
            new HornPage(),
            new BodyPage(),
            new GlowPage()
    };

    private SettingsList listWidget;
    private SettingsList previousListWidget;
    private PonyCustomPageContext pageContext;
    private int selectedPage = PonyCustomPageContext.Page.MODEL.ordinal();
    private float transitionAlpha = 1.0f;
    private boolean isTransitioning;
    private int transitionDirection = 1;
    private boolean refreshRequested;

    private int rightX;
    private int rightWidth;

    private GeckoPlayerAnimatable ponyAnimatable;
    private GeoObjectRenderer<GeckoPlayerAnimatable> ponyRenderer;

    private float previewYaw = 155.0f;
    private float previewPitch = -10.0f;
    private boolean isDraggingModel;
    private float dragSensitivity = 0.5f;

    @Override
    public void onEnter() {
        refreshRequested = false;
        selectedPage = PonyCustomPageContext.Page.MODEL.ordinal();
        transitionAlpha = 1.0f;
        isTransitioning = false;
        previousListWidget = null;
        previewYaw = 155.0f;
        previewPitch = -10.0f;
        currentPage().onEnter();
    }

    @Override
    public void onExit() {
        refreshRequested = false;
        ColorPicker.clearBodyLinkGroup();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= rightX && mouseX <= rightX + rightWidth * 0.55) {
            isDraggingModel = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDraggingModel = false;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && isDraggingModel) {
            previewYaw -= (float) deltaX * dragSensitivity;
            previewPitch += (float) deltaY * dragSensitivity;
            previewPitch = Math.max(-90.0f, Math.min(90.0f, previewPitch));
            return true;
        }
        return false;
    }

    @Override
    public float getPreviewYaw() {
        return previewYaw;
    }

    @Override
    public float getPreviewPitch() {
        return previewPitch;
    }

    @Override
    public void setPreviewRotation(float yaw, float pitch) {
        previewYaw = yaw;
        previewPitch = Math.max(-90.0f, Math.min(90.0f, pitch));
    }

    @Override
    public boolean isPreviewActive() {
        return ModelManager.getActiveModel() != null && currentPage().isPreviewActive();
    }

    @Override
    public void init(ConfigScreen screen, int x, int y, int width, int height) {
        ColorPicker.clearBodyLinkGroup();
        rightX = x;
        rightWidth = width;

        if (ponyAnimatable == null) {
            ponyAnimatable = new GeckoPlayerAnimatable() {
                @Override
                public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
                    controllers.add(new AnimationController<>(this, "controller", 0, state -> {
                        state.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
                        return PlayState.CONTINUE;
                    }));
                    controllers.add(new AnimationController<>(this, "blink_controller", 0, state -> {
                        state.getController().setAnimation(RawAnimation.begin().thenLoop("blink_parallel"));
                        return PlayState.CONTINUE;
                    }));
                    controllers.add(new AnimationController<>(this, "ear_controller", 0, state -> {
                        state.getController().setAnimation(RawAnimation.begin().thenLoop("ear_parallel"));
                        return PlayState.CONTINUE;
                    }));
                    controllers.add(new AnimationController<>(this, "tail_controller", 0, state -> {
                        state.getController().setAnimation(RawAnimation.begin().thenLoop("tail_parallel"));
                        return PlayState.CONTINUE;
                    }));
                }
            };
            ponyRenderer = createPonyRenderer();
        }
        ponyAnimatable.setPlayer(MinecraftClient.getInstance().player);

        pageContext = new PonyCustomPageContext(screen, x, y, width, height, this::switchPage, this::refreshCurrentPage);
        listWidget = new SettingsList(MinecraftClient.getInstance(), width, height, y + 40,
                y + height - 40, 24);
        listWidget.setLeftPos(x);
        currentPage().build(pageContext, listWidget);
        listWidget.centerIfShort();
        screen.addConsoleElement(listWidget);
    }

    private void refreshCurrentPage() {
        refreshRequested = true;
    }

    private void applyPendingRefresh() {
        if (!refreshRequested) return;
        refreshRequested = false;
        double scroll = listWidget.getScrollAmount();
        int focusedIndex = listWidget.children().indexOf(listWidget.getFocused());
        pageContext.reinit();
        if (focusedIndex >= 0 && focusedIndex < listWidget.children().size()) {
            SettingsList.Entry entry = listWidget.children().get(focusedIndex);
            listWidget.setFocused(entry);
            entry.setFocused(entry.widget);
            pageContext.getScreen().setFocused(listWidget);
        }
        listWidget.restoreScrollAmount(scroll);
    }

    private void switchPage(PonyCustomPageContext.Page page, int direction) {
        if (pageContext == null || page.ordinal() == selectedPage) return;
        refreshRequested = false;

        if (direction == 0) {
            previousListWidget = null;
            isTransitioning = false;
        } else {
            previousListWidget = listWidget;
            isTransitioning = true;
            transitionDirection = direction;
            transitionAlpha = 0.0f;
        }

        selectedPage = page.ordinal();
        currentPage().onEnter();
        pageContext.reinit();
    }

    private PonyCustomPage currentPage() {
        return PAGES[selectedPage];
    }

    @Override
    public boolean suppressChildRendering() {
        return isTransitioning;
    }

    @Override
    public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY,
            float delta, float alpha) {
        if (pageContext == null) return;
        applyPendingRefresh();
        currentPage().render(pageContext, context, mouseX, mouseY, delta, alpha);

        if (isTransitioning) {
            transitionAlpha += 0.08f;
            if (transitionAlpha >= 1.0f) {
                transitionAlpha = 1.0f;
                isTransitioning = false;
                previousListWidget = null;
            }
        }

        if (isTransitioning && previousListWidget != null) {
            float t = transitionAlpha;
            float progress = 1.0f - (1.0f - t) * (1.0f - t);

            float oldAlpha = (1.0f - t) * alpha;
            float oldOffset = -progress * 60 * transitionDirection;
            previousListWidget.setBaseAlpha(oldAlpha);
            context.getMatrices().push();
            context.getMatrices().translate(oldOffset, 0, 0);
            previousListWidget.render(context, -1, -1, delta);
            context.getMatrices().pop();

            float newAlpha = t * alpha;
            float newOffset = (1.0f - progress) * 60 * transitionDirection;
            listWidget.setBaseAlpha(newAlpha);
            context.getMatrices().push();
            context.getMatrices().translate(newOffset, 0, 0);
            listWidget.render(context, mouseX, mouseY, delta);
            context.getMatrices().pop();
        } else if (listWidget != null) {
            listWidget.setBaseAlpha(alpha);
            listWidget.render(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public void postRender(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY,
            float delta, float alpha) {
        ModelConfig activeModel = ModelManager.getActiveModel();
        if (activeModel == null || !currentPage().isPreviewActive() || ponyAnimatable == null) return;

        if (currentPage().usesGlowPreview()) {
            renderGrassBlockPreview(context, x, y, width, height, alpha, activeModel);
            return;
        }

        ponyAnimatable.setPlayer(MinecraftClient.getInstance().player);

        float baseScale = Math.min(width / 6.0f, height / 4.0f);
        float modelScale = Math.max(30.0f, Math.min(baseScale, 150.0f));

        int buttonWidth = Math.min(180, rightWidth / 2);
        int buttonX = rightX + rightWidth - buttonWidth - 20;
        int modelX = (rightX * 3 + buttonX) / 4;
        int modelY = y + height / 2 + (int) (height * 0.1f);

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(modelX, modelY, 100);
        matrices.scale(modelScale * alpha, modelScale * alpha, modelScale * alpha);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(previewYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(previewPitch));

        matrices.push();
        matrices.translate(-0.5f, -1.5f, -0.7f);
        try {
            RenderLayer renderLayer = ponyRenderer.getRenderType(ponyAnimatable,
                    ponyRenderer.getTextureLocation(ponyAnimatable), context.getVertexConsumers(), delta);
            if (renderLayer != null) {
                VertexConsumer vertexConsumer = context.getVertexConsumers().getBuffer(renderLayer);
                ponyRenderer.render(matrices, ponyAnimatable, context.getVertexConsumers(), renderLayer,
                        vertexConsumer, 0xF000F0);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to render pony model preview in GUI", e);
        }
        matrices.pop();
        matrices.pop();
    }

    private GeoObjectRenderer<GeckoPlayerAnimatable> createPonyRenderer() {
        return new PonyRenderer(new GeckoPlayerModel() {
            @Override
            public void applyMolangQueries(GeckoPlayerAnimatable animatable, double animTime) {
                if (MinecraftClient.getInstance().world == null) return;
                try {
                    super.applyMolangQueries(animatable, animTime);
                } catch (Exception e) {
                    LOGGER.warn("Failed to apply Molang queries during pony preview", e);
                }
            }
        }) {
            @Override
            public RenderLayer getRenderType(GeckoPlayerAnimatable animatable, Identifier texture,
                    VertexConsumerProvider bufferSource, float partialTick) {
                return RenderLayer.getEntityTranslucent(texture);
            }
        };
    }

    private void renderGrassBlockPreview(DrawContext context, int x, int y, int width, int height, float alpha,
            ModelConfig config) {
        if (alpha <= 0) return;

        int buttonWidth = Math.min(180, rightWidth / 2);
        int buttonX = rightX + rightWidth - buttonWidth - 20;
        int centerX = (rightX * 3 + buttonX) / 4;
        int centerY = y + height / 2 + (int) (height * 0.1f);

        String hex = config.magicGlowColor;
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() > 6) hex = hex.substring(hex.length() - 6);
        int glowColorParsed;
        try {
            glowColorParsed = Integer.parseInt(hex, 16);
        } catch (Exception e) {
            glowColorParsed = 0xAA00FF;
        }
        final int glowColor = glowColorParsed;

        MinecraftClient client = MinecraftClient.getInstance();
        ItemStack grassStack = new ItemStack(Blocks.GRASS_BLOCK);
        VertexConsumerProvider vertexConsumers = context.getVertexConsumers();

        float baseScale = Math.min(width / 8.0f, height / 6.0f);
        float modelScale = Math.max(20.0f, Math.min(baseScale, 80.0f));

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(centerX, centerY, 150);
        matrices.scale(modelScale * alpha, modelScale * alpha, modelScale * alpha);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-previewYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-previewPitch));

        client.getItemRenderer().renderItem(grassStack, ModelTransformationMode.NONE,
                0xF000F0, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, client.world, 0);

        VertexConsumerProvider glowProvider = layer -> {
            if (layer.getVertexFormat() != VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL) {
                return vertexConsumers.getBuffer(layer);
            }
            return vertexConsumers.getBuffer(
                    MagicGlow.getColoured(
                            RenderLayerHelper.getTexture(layer)
                                    .orElse(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE),
                            glowColor));
        };

        matrices.scale(1.1F, 1.1F, 1.1F);
        matrices.translate(0.015F, 0.01F, 0.01F);
        client.getItemRenderer().renderItem(grassStack, ModelTransformationMode.NONE,
                0xF000F0, OverlayTexture.DEFAULT_UV, matrices, glowProvider, client.world, 0);

        matrices.translate(-0.03F, -0.02F, -0.02F);
        client.getItemRenderer().renderItem(grassStack, ModelTransformationMode.NONE,
                0xF000F0, OverlayTexture.DEFAULT_UV, matrices, glowProvider, client.world, 0);
        matrices.pop();
    }
}
