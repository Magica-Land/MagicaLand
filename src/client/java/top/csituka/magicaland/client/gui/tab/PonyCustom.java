package top.csituka.magicaland.client.gui.tab;

import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.loading.FileLoader;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.config.style.PonyStylePart;
import top.csituka.magicaland.client.gui.ConfigScreen;
import top.csituka.magicaland.client.gui.tab.ponycustom.*;
import top.csituka.magicaland.client.gui.tab.ponycustom.CustomizationLayout.Rect;
import top.csituka.magicaland.client.gui.widget.ColorPicker;
import top.csituka.magicaland.client.gui.widget.CustomButton;
import top.csituka.magicaland.client.gui.widget.SettingsList;
import top.csituka.magicaland.client.gui.widget.ViewCube;
import top.csituka.magicaland.client.model.GeckoPlayerAnimatable;
import top.csituka.magicaland.client.model.GeckoPlayerModel;
import top.csituka.magicaland.client.model.PonyPreviewAnimatable;
import top.csituka.magicaland.client.render.GlowingItem;
import top.csituka.magicaland.client.render.MagicGlow;
import top.csituka.magicaland.client.render.PonyRenderer;
import top.csituka.magicaland.client.render.PonyGuiGaze;
import top.csituka.magicaland.client.util.RenderLayerHelper;

public class PonyCustom implements TabContent, ViewCube.RotationTarget {
    private static final Logger LOGGER = LoggerFactory.getLogger(PonyCustom.class);
    private static final PonyCustomPage[] PAGES = {
            new ModelPage(), new MainPage(), new ManePage(), new FacePage(), new HornPage(), new BodyPage(), new GlowPage()
    };
    private static final PonyCustomPageContext.Page[] CATEGORIES = {
            PonyCustomPageContext.Page.BODY, PonyCustomPageContext.Page.MANE, PonyCustomPageContext.Page.FACE,
            PonyCustomPageContext.Page.HORN, PonyCustomPageContext.Page.GLOW, PonyCustomPageContext.Page.MODEL
    };
    private static final String[] CATEGORY_KEYS = { "body", "mane", "face", "horn", "glow", "presets" };
    private final double[] scrollPositions = new double[PAGES.length];
    private SettingsList listWidget;
    private PonyCustomPageContext pageContext;
    private CustomizationLayout layout;
    private int selectedPage = PonyCustomPageContext.Page.BODY.ordinal();
    private boolean refreshRequested;
    private boolean renameOpen;
    private String renameDraft = "";
    private String renameTargetName;
    private boolean renameError;
    private boolean showGlowItem;
    private PreviewLighting lighting = PreviewLighting.NOON;
    private PonyPreviewAnimatable ponyAnimatable;
    private GeoObjectRenderer<GeckoPlayerAnimatable> ponyRenderer;
    private float previewYaw = 155.0f;
    private float previewPitch = -10.0f;
    private boolean isDraggingModel;
    private final PreviewCamera camera = new PreviewCamera();
    private PonyStylePart focusedPart;
    private boolean automaticFocus = true;
    private boolean snapCamera;
    private long lastPreviewFrame;

    @Override
    public void onEnter() {
        selectedPage = PonyCustomPageContext.Page.BODY.ordinal();
        refreshRequested = false;
        renameOpen = false;
        renameError = false;
        showGlowItem = false;
        isDraggingModel = false;
        focusedPart = null;
        camera.reset();
        lastPreviewFrame = 0;
        resetCameraAngle();
        java.util.Arrays.fill(scrollPositions, 0);
        for (PonyCustomPage page : PAGES) page.onEnter();
    }

    @Override
    public void onExit() {
        refreshRequested = false;
        isDraggingModel = false;
        renameOpen = false;
        ColorPicker.clearBodyLinkGroup();
        PonyStyleThumbnails.clear();
        if (ponyRenderer instanceof PonyRenderer renderer) renderer.clearOverride();
        if (ponyAnimatable != null) ponyAnimatable.reset();
        ponyAnimatable = null;
        ponyRenderer = null;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (renameOpen || currentPage().isEditingPreset() || layout == null || !isPreviewActive()
                || !layout.model().contains(x, y)) return false;
        if (button == 1) {
            resetCameraAngle();
            return true;
        }
        isDraggingModel = button == 0;
        return isDraggingModel;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        boolean dragging = isDraggingModel;
        if (button == 0) isDraggingModel = false;
        return dragging;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button != 0 || !isDraggingModel) return false;
        setPreviewRotation(previewYaw - (float) dx * 0.5f, previewPitch - (float) dy * 0.5f);
        return true;
    }

    public boolean keyPressed(int key) {
        if (!renameOpen) return currentPage().keyPressed(key);
        if (key == 256) {
            renameOpen = false;
            renameError = false;
            pageContext.reinit();
            return true;
        }
        if (key == 257 || key == 335) {
            commitRename();
            return true;
        }
        return false;
    }

    public boolean hasPendingTextEdit() { return renameOpen || currentPage().isEditingPreset(); }

    @Override public float getPreviewYaw() { return previewYaw; }
    @Override public float getPreviewPitch() { return previewPitch; }
    @Override public void setPreviewRotation(float yaw, float pitch) {
        previewYaw = yaw;
        previewPitch = Math.max(-80, Math.min(80, pitch));
        camera.manual(previewYaw, previewPitch);
    }
    @Override public boolean isPreviewActive() { return ModelManager.getActiveModel() != null; }

    @Override
    public void init(ConfigScreen screen, int x, int y, int width, int height) {
        isDraggingModel = false;
        ColorPicker.clearBodyLinkGroup();
        layout = CustomizationLayout.of(x, y, width, height);
        initRenderer();
        Rect details = layout.details();
        pageContext = new PonyCustomPageContext(screen, details.x(), details.y(), details.width(), details.height(),
                this::switchPage, () -> refreshRequested = true, this::focusPart, () -> {
                    scrollPositions[selectedPage] = 0;
                    if (listWidget != null) listWidget.restoreScrollAmount(0);
                });
        listWidget = new CustomizationList(MinecraftClient.getInstance(), details);
        currentPage().build(pageContext, listWidget);
        if (renameOpen) for (SettingsList.Entry entry : listWidget.children()) entry.widget.active = false;
        listWidget.restoreScrollAmount(scrollPositions[selectedPage]);
        screen.addConsoleElement(listWidget);
        for (int i = 0; i < CATEGORIES.length; i++) {
            int index = i;
            int tabX = x + i % layout.tabColumns() * (layout.tabWidth() + 4);
            int tabY = y + i / layout.tabColumns() * 23;
            CustomButton tab = new CustomButton(tabX, tabY, layout.tabWidth(), 20,
                    tr("category." + CATEGORY_KEYS[i]), selectedPage == CATEGORIES[i].ordinal(),
                    button -> switchPage(CATEGORIES[index], 0));
            tab.active = !renameOpen;
            screen.addConsoleWidget(tab);
        }
        buildPreviewControls(screen);
    }

    private void buildPreviewControls(ConfigScreen screen) {
        Rect panel = layout.preview();
        int x = panel.x() + 5;
        int y = panel.y() + 5;
        int width = panel.width() - 10;
        ModelConfig active = ModelManager.getActiveModel();
        if (renameOpen && active != null) {
            TextFieldWidget field = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, x, y,
                    width - 44, 20, tr("preset.rename"));
            field.setMaxLength(32);
            field.setText(renameDraft);
            field.setChangedListener(value -> { renameDraft = value; renameError = false; });
            screen.addConsoleWidget(field);
            screen.setFocused(field);
            field.setFocused(true);
            screen.addConsoleWidget(new CustomButton(x + width - 42, y, 20, 20, Text.literal("✓"),
                    tr("preset.rename"), false, button -> commitRename()));
            screen.addConsoleWidget(new CustomButton(x + width - 20, y, 20, 20, Text.literal("×"),
                    Text.translatable("text.magicaland.config.button.cancel"), false, button -> {
                        renameOpen = false;
                        renameError = false;
                        pageContext.reinit();
                    }));
        } else {
            List<String> models = List.copyOf(ModelManager.getAvailableModels());
            String activeName = active == null ? "" : active.name;
            CustomButton selector = new CustomButton(x, y, width - 24, 20, Text.literal(activeName),
                    tr("preset.switch_hint"), false, button -> {
                        if (models.size() < 2) { switchPage(PonyCustomPageContext.Page.MODEL, 0); return; }
                        int next = (models.indexOf(activeName) + 1) % models.size();
                        if (ModelManager.loadModel(models.get(next))) pageContext.refreshKeepingScroll();
                    });
            screen.addConsoleWidget(selector);
            selector.active = !currentPage().isEditingPreset();
            CustomButton rename = new CustomButton(x + width - 22, y, 22, 20, Text.literal("✎"),
                    tr("preset.rename"), false, button -> {
                        renameDraft = ModelManager.getActiveModel().name;
                        renameTargetName = renameDraft;
                        renameOpen = true;
                        renameError = false;
                        pageContext.reinit();
                    });
            rename.active = active != null && !currentPage().isEditingPreset();
            screen.addConsoleWidget(rename);
        }
        int lightWidth = (width - 4) / 3;
        for (PreviewLighting option : PreviewLighting.values()) {
            CustomButton button = new CustomButton(x + option.ordinal() * (lightWidth + 2), layout.lightingY(),
                    lightWidth, 20, tr("lighting." + option.key), option == lighting, pressed -> lighting = option);
            button.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(tr("lighting.hint")));
            screen.addConsoleWidget(button);
        }
        CustomButton focus = new CustomButton(x, panel.y() + 28, width - 24, 20,
                Text.literal(automaticFocus ? "☑ " : "☐ ").append(tr("preview.auto_focus")),
                tr("preview.auto_focus_hint"), false, button -> {
                    automaticFocus = !automaticFocus;
                    resetCameraAngle();
                    snapCamera = !automaticFocus;
                    pageContext.refreshKeepingScroll();
                });
        screen.addConsoleWidget(focus);
        screen.addConsoleWidget(new CustomButton(x + width - 22, panel.y() + 28, 22, 20,
                Text.literal("↺"), tr("preview.reset_view"), false, button -> resetCameraAngle()));
        if (currentPage().usesGlowPreview()) {
            screen.addConsoleWidget(new CustomButton(layout.model().x() + 2, layout.model().y() + 2,
                    Math.min(78, layout.model().width() - 4), 18,
                    tr(showGlowItem ? "preview.item" : "preview.pony"), false, button -> {
                        showGlowItem = !showGlowItem;
                        pageContext.refreshKeepingScroll();
                    }));
        }
        if (!automaticFocus && layout.model().width() >= 170 && layout.model().height() >= 140) {
            screen.addConsoleWidget(new ViewCube(layout.model().right() - 46, layout.model().y() + 2, 44, 44, this));
        }
    }

    private void commitRename() {
        if (ModelManager.getActiveModel() == null || !ModelManager.getActiveModel().name.equals(renameTargetName)) {
            renameOpen = false;
            renameError = true;
            pageContext.reinit();
            return;
        }
        if (ModelManager.renameActiveModel(renameDraft)) {
            renameOpen = false;
            renameError = false;
        } else renameError = true;
        pageContext.reinit();
    }

    private void applyPendingRefresh() {
        if (!refreshRequested) return;
        refreshRequested = false;
        scrollPositions[selectedPage] = listWidget.getScrollAmount();
        int focusedIndex = listWidget.children().indexOf(listWidget.getFocused());
        pageContext.reinit();
        if (!renameOpen && focusedIndex >= 0 && focusedIndex < listWidget.children().size()) {
            SettingsList.Entry entry = listWidget.children().get(focusedIndex);
            listWidget.setFocused(entry);
            entry.setFocused(entry.widget);
            pageContext.getScreen().setFocused(listWidget);
        }
    }

    private void switchPage(PonyCustomPageContext.Page page, int direction) {
        if (page == PonyCustomPageContext.Page.MAIN) page = PonyCustomPageContext.Page.BODY;
        if (pageContext == null || page.ordinal() == selectedPage) return;
        scrollPositions[selectedPage] = listWidget.getScrollAmount();
        selectedPage = page.ordinal();
        if (page == PonyCustomPageContext.Page.FACE) focusPart(PonyStylePart.EYE);
        else if (page == PonyCustomPageContext.Page.MANE) focusPart(((ManePage) currentPage()).selectedStylePart());
        else focusPart(null);
        refreshRequested = false;
        pageContext.reinit();
    }

    private PonyCustomPage currentPage() { return PAGES[selectedPage]; }
    private static Text tr(String key) { return Text.translatable("text.magicaland.customize." + key); }

    private void focusPart(PonyStylePart part) {
        if (focusedPart == part) return;
        focusedPart = part;
        if (automaticFocus) resetCameraAngle();
    }

    private void resetCameraAngle() {
        float yaw = 155, pitch = -10;
        if (automaticFocus && focusedPart != null) {
            yaw = switch (focusedPart) { case FRONT_MANE -> 155; case EYE -> 145; case BACK_MANE -> 25; case TAIL -> 35; };
            pitch = focusedPart == PonyStylePart.TAIL ? 10 : focusedPart == PonyStylePart.BACK_MANE ? 6 : -8;
        }
        camera.angle(yaw, pitch);
        if (camera.pose() == null) { previewYaw = yaw; previewPitch = pitch; }
    }

    @Override
    public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY,
            float delta, float alpha) {
        if (layout == null) return;
        applyPendingRefresh();
        Rect panel = layout.preview();
        Rect details = layout.details();
        context.fillGradient(panel.x(), panel.y(), panel.right(), panel.bottom(), lighting.top, lighting.bottom);
        context.drawBorder(panel.x(), panel.y(), panel.width(), panel.height(), 0x557B8DA9);
        context.fill(details.x(), details.y(), details.right(), details.bottom(), 0x800E1420);
        for (int i = 0; i < CATEGORIES.length; i++) if (CATEGORIES[i].ordinal() == selectedPage) {
            int tabX = x + i % layout.tabColumns() * (layout.tabWidth() + 4);
            int tabY = y + i / layout.tabColumns() * 23;
            context.fill(tabX + 3, tabY + 19, tabX + layout.tabWidth() - 3, tabY + 21, 0xFFC4B0EB);
        }
        int lightWidth = (panel.width() - 14) / 3;
        int lightX = panel.x() + 5 + lighting.ordinal() * (lightWidth + 2);
        context.fill(lightX + 2, layout.lightingY() + 19, lightX + lightWidth - 2, layout.lightingY() + 20, 0xFFE6D8AA);
        renderPreview(context, delta, mouseX, mouseY);
        if (renameError) PonyCustomPageHelper.drawWrapped(context, tr("preset.rename_error"),
                panel.x() + 5, layout.model().y() + 2, panel.width() - 10, 0xFFFF9999);
        currentPage().render(pageContext, context, mouseX, mouseY, delta, alpha);
        listWidget.render(context, mouseX, mouseY, delta);
        scrollPositions[selectedPage] = listWidget.getScrollAmount();
    }

    private void renderPreview(DrawContext context, float delta, int mouseX, int mouseY) {
        ModelConfig config = ModelManager.getActiveModel();
        if (config == null || ponyRenderer == null) return;
        Rect area = layout.model();
        MatrixStack matrices = context.getMatrices();
        context.draw();
        float[] previousColor = RenderSystem.getShaderColor().clone();
        context.enableScissor(area.x(), area.y(), area.right(), area.bottom());
        matrices.push();
        try {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            PreviewLightingRig.apply();
            var box = PreviewGeometryBounds.framingBounds(config, automaticFocus ? focusedPart : null);
            long now = System.nanoTime();
            double seconds = lastPreviewFrame == 0 ? 1.0 / 60 : (now - lastPreviewFrame) / 1.0e9;
            lastPreviewFrame = now;
            PreviewCamera.Pose pose = camera.update(new PreviewCamera.Box(box.minX(), box.minY(), box.minZ(),
                    box.maxX(), box.maxY(), box.maxZ()), area.width(), area.height(), seconds, snapCamera);
            snapCamera = false;
            previewYaw = pose.yaw(); previewPitch = pose.pitch();
            if (currentPage().usesGlowPreview() && showGlowItem) {
                renderGrassBlockPreview(context, config, area);
            } else {
                ponyAnimatable.beginFrame(now);
                ponyAnimatable.setPlayer(MinecraftClient.getInstance().player);
                ((PonyRenderer) ponyRenderer).setOverrideConfig(config);
                float scale = pose.scale();
                matrices.translate(area.x() + area.width() * .5f, area.y() + area.height() * .5f, 150);
                matrices.scale(scale, scale, scale);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(previewYaw));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(previewPitch));
                matrices.translate(-pose.x() - .5f, -pose.y() - .51f, -pose.z() - .5f);
                RenderLayer layer = ponyRenderer.getRenderType(ponyAnimatable,
                        ponyRenderer.getTextureLocation(ponyAnimatable), context.getVertexConsumers(), delta);
                if (layer != null) {
                    VertexConsumer consumer = context.getVertexConsumers().getBuffer(layer);
                    var window = MinecraftClient.getInstance().getWindow();
                    try (var gaze = PonyGuiGaze.begin(this, ponyAnimatable.getPlayer(), mouseX, mouseY,
                            window.getScaledWidth(), window.getScaledHeight())) {
                        ponyRenderer.render(matrices, ponyAnimatable, context.getVertexConsumers(), layer, consumer, 0xF000F0);
                    }
                }
            }
            context.draw();
        } catch (Exception e) {
            LOGGER.warn("Failed to render pony customization preview", e);
        } finally {
            try {
                context.draw();
            } finally {
                matrices.pop();
                context.disableScissor();
                DiffuseLighting.enableGuiDepthLighting();
                RenderSystem.setShaderColor(previousColor[0], previousColor[1], previousColor[2], previousColor[3]);
            }
        }
    }

    private void initRenderer() {
        if (ponyAnimatable != null) return;
        ponyAnimatable = new PonyPreviewAnimatable();
        ponyAnimatable.setPlayer(MinecraftClient.getInstance().player);
        ponyRenderer = new PonyRenderer(new GeckoPlayerModel() {
            private BakedGeoModel sharedSource, previewModel;

            @Override
            public BakedGeoModel getBakedModel(Identifier location) {
                BakedGeoModel source = super.getBakedModel(location);
                if (source != sharedSource) {
                    var raw = FileLoader.loadModelFile(location, MinecraftClient.getInstance().getResourceManager());
                    previewModel = BakedModelFactory.getForNamespace(location.getNamespace())
                            .constructGeoModel(GeometryTree.fromModel(raw));
                    // 动画只写私有骨骼；共享缓存仅用来识别资源重载。
                    getAnimationProcessor().setActiveModel(previewModel);
                    sharedSource = source;
                }
                return previewModel;
            }

            @Override
            public void handleAnimations(GeckoPlayerAnimatable animatable, long instanceId,
                    AnimationState<GeckoPlayerAnimatable> state) {
                ((PonyPreviewAnimatable) animatable).prepareAnimationFrame(instanceId, state);
                super.handleAnimations(animatable, instanceId, state);
            }

            @Override
            public void applyMolangQueries(GeckoPlayerAnimatable animatable, double animTime) {
                if (MinecraftClient.getInstance().world == null) return;
                super.applyMolangQueries(animatable, animTime);
            }
        }) {
            @Override
            public RenderLayer getRenderType(GeckoPlayerAnimatable animatable, Identifier texture,
                    VertexConsumerProvider buffers, float partialTick) {
                return RenderLayer.getEntityTranslucent(texture);
            }
            @Override
            public Color getRenderColor(GeckoPlayerAnimatable animatable, float partialTick, int packedLight) {
                return Color.ofOpaque(lighting.tint);
            }
        };
    }

    private void renderGrassBlockPreview(DrawContext context, ModelConfig config, Rect area) {
        MinecraftClient client = MinecraftClient.getInstance();
        ItemStack stack = new ItemStack(Blocks.GRASS_BLOCK);
        MatrixStack matrices = context.getMatrices();
        VertexConsumerProvider buffers = context.getVertexConsumers();
        float scale = Math.min(area.width() / 2.2f, area.height() / 2.2f);
        matrices.translate(area.x() + area.width() / 2f, area.y() + area.height() / 2f, 150);
        matrices.scale(scale, scale, scale);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(previewYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(previewPitch));
        RenderSystem.setShaderColor((lighting.tint >> 16 & 255) / 255f, (lighting.tint >> 8 & 255) / 255f,
                (lighting.tint & 255) / 255f, 1);
        GlowingItem.renderPreviewWithGlow(client.getItemRenderer(), stack, ModelTransformationMode.NONE,
                matrices, buffers, client.world, 0xF000F0, 0, GlowingItem.getGlowColor(config));
    }

    private enum PreviewLighting {
        DAY("day", 0xFFF1DE, 0xE06B8490, 0xE040515E),
        NOON("noon", 0xFFFFFF, 0xE0788B9D, 0xE04C5D6C),
        NIGHT("night", 0x7A91C7, 0xF01C253F, 0xF010162A);
        final String key;
        final int tint;
        final int top;
        final int bottom;
        PreviewLighting(String key, int tint, int top, int bottom) {
            this.key = key; this.tint = tint; this.top = top; this.bottom = bottom;
        }
    }
}
