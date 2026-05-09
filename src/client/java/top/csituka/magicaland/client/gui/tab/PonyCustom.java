package top.csituka.magicaland.client.gui.tab;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import top.csituka.magicaland.client.config.Config;
import top.csituka.magicaland.client.gui.ConfigScreen;
import top.csituka.magicaland.client.gui.widget.ColorPicker;
import top.csituka.magicaland.client.gui.widget.CustomButton;
import top.csituka.magicaland.client.gui.widget.Toggle;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import top.csituka.magicaland.client.model.GeckoPlayerAnimatable;
import top.csituka.magicaland.client.model.GeckoPlayerModel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PonyCustom implements TabContent {
    private final List<ClickableWidget> widgets = new ArrayList<>();
    private int rightX;
    private int rightWidth;
    private int rightHeight;
    private float currentAlpha = 1.0f;

    private boolean hornMenuOpen = false;
    private boolean maneMenuOpen = false;
    private boolean faceMenuOpen = false;

    private GeckoPlayerAnimatable ponyAnimatable;
    private GeoObjectRenderer<GeckoPlayerAnimatable> ponyRenderer;

    private ConfigScreen screen;

    private static final String[] FRONT_MANE_STYLES = {"TS", "RD", "RR", "PP", "AJ", "FS"};
    private static final String[] BACK_MANE_STYLES = {"TS", "RD", "RR", "PP", "AJ", "FS"};
    private static final String[] EYE_STYLES = {"TS", "FS", "RR"};

    @Override
    public void init(ConfigScreen screen, int x, int y, int width, int height) {
        Config config = Config.getInstance();
        this.rightX = x;
        this.rightWidth = width;
        this.rightHeight = height;
        this.screen = screen;
        this.widgets.clear();

        if (this.ponyAnimatable == null) {
            this.ponyAnimatable = new GeckoPlayerAnimatable() {
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
            this.ponyRenderer = createPonyRenderer();
        }
        this.ponyAnimatable.setPlayer(MinecraftClient.getInstance().player);

        if (hornMenuOpen) {
            initHornMenu(screen, x, y, width, height);
        } else if (maneMenuOpen) {
            initManeMenu(screen, x, y, width, height);
        } else if (faceMenuOpen) {
            initFaceMenu(screen, x, y, width, height);
        } else {
            initMainMenu(screen, x, y, width, height);
        }
    }

    private void initMainMenu(ConfigScreen screen, int x, int y, int width, int height) {
        Config config = Config.getInstance();
        int buttonWidth = Math.min(180, width / 2);
        int buttonHeight = 20;
        int spacing = 10;

        int totalContentHeight = buttonHeight * 3 + spacing * 2;
        int startY = (height - totalContentHeight) / 2;

        int btnX = x + width - buttonWidth - 20;
        if (width < 250) {
            btnX = x + (width - buttonWidth) / 2;
        }

        CustomButton maneBtn = new CustomButton(btnX, startY, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.mane_menu.name"),
                false, button -> {
                    maneMenuOpen = true;
                    reinit(screen);
                }, true);
        this.widgets.add(maneBtn);
        screen.addConsoleWidget(maneBtn);

        CustomButton faceBtn = new CustomButton(btnX, startY + buttonHeight + spacing, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.face_menu.name"),
                false, button -> {
                    faceMenuOpen = true;
                    reinit(screen);
                }, true);
        this.widgets.add(faceBtn);
        screen.addConsoleWidget(faceBtn);

        CustomButton hornBtn = new CustomButton(btnX, startY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.horn_menu.name"),
                false, button -> {
                    hornMenuOpen = true;
                    reinit(screen);
                }, true);
        this.widgets.add(hornBtn);
        screen.addConsoleWidget(hornBtn);
    }

    private void initManeMenu(ConfigScreen screen, int x, int y, int width, int height) {
        Config config = Config.getInstance();
        int buttonWidth = Math.min(180, width / 2);
        int buttonHeight = 20;
        int spacing = 10;

        int totalContentHeight = buttonHeight * 3 + spacing * 2;
        int startY = (height - totalContentHeight) / 2;

        int btnX = x + width - buttonWidth - 20;
        if (width < 250) {
            btnX = x + (width - buttonWidth) / 2;
        }

        int backBtnWidth = 60;
        int backBtnX = btnX + buttonWidth - backBtnWidth;
        CustomButton backBtn = new CustomButton(backBtnX, startY, backBtnWidth, buttonHeight,
                Text.translatable("text.magicaland.config.button.back"),
                false, button -> {
                    maneMenuOpen = false;
                    reinit(screen);
                });
        this.widgets.add(backBtn);
        screen.addConsoleWidget(backBtn);

        int frontBtnY = startY + buttonHeight + spacing;
        CustomButton frontBtn = createStyleButton(btnX, frontBtnY, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.front_mane_style.name"),
                config.frontManeStyle, FRONT_MANE_STYLES, newStyle -> {
                    config.frontManeStyle = newStyle;
                    Config.save();
                });
        this.widgets.add(frontBtn);
        screen.addConsoleWidget(frontBtn);

        int backBtnY = frontBtnY + buttonHeight + spacing;
        CustomButton backManeBtn = createStyleButton(btnX, backBtnY, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.back_mane_style.name"),
                config.backManeStyle, BACK_MANE_STYLES, newStyle -> {
                    config.backManeStyle = newStyle;
                    Config.save();
                });
        this.widgets.add(backManeBtn);
        screen.addConsoleWidget(backManeBtn);
    }

    private void initFaceMenu(ConfigScreen screen, int x, int y, int width, int height) {
        Config config = Config.getInstance();
        int buttonWidth = Math.min(180, width / 2);
        int buttonHeight = 20;
        int spacing = 10;

        int totalContentHeight = buttonHeight * 2 + spacing;
        int startY = (height - totalContentHeight) / 2;

        int btnX = x + width - buttonWidth - 20;
        if (width < 250) {
            btnX = x + (width - buttonWidth) / 2;
        }

        int backBtnWidth = 60;
        int backBtnX = btnX + buttonWidth - backBtnWidth;
        CustomButton backBtn = new CustomButton(backBtnX, startY, backBtnWidth, buttonHeight,
                Text.translatable("text.magicaland.config.button.back"),
                false, button -> {
                    faceMenuOpen = false;
                    reinit(screen);
                });
        this.widgets.add(backBtn);
        screen.addConsoleWidget(backBtn);

        int eyeBtnY = startY + buttonHeight + spacing;
        CustomButton eyeBtn = createStyleButton(btnX, eyeBtnY, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.eye_style.name"),
                config.eyeStyle, EYE_STYLES, newStyle -> {
                    config.eyeStyle = newStyle;
                    Config.save();
                });
        this.widgets.add(eyeBtn);
        screen.addConsoleWidget(eyeBtn);
    }

    private void initHornMenu(ConfigScreen screen, int x, int y, int width, int height) {
        Config config = Config.getInstance();
        int buttonWidth = Math.min(180, width / 2);
        int buttonHeight = 20;
        int spacing = 10;

        int totalContentHeight = buttonHeight * 3 + spacing * 2;
        int startY = (height - totalContentHeight) / 2;

        int btnX = x + width - buttonWidth - 20;
        if (width < 250) {
            btnX = x + (width - buttonWidth) / 2;
        }

        int backBtnWidth = 60;
        int backBtnX = btnX + buttonWidth - backBtnWidth;
        CustomButton backBtn = new CustomButton(backBtnX, startY, backBtnWidth, buttonHeight,
                Text.translatable("text.magicaland.config.button.back"),
                false, button -> {
                    hornMenuOpen = false;
                    reinit(screen);
                });
        this.widgets.add(backBtn);
        screen.addConsoleWidget(backBtn);

        int toggleY = startY + buttonHeight + spacing;
        Toggle hornToggle = new Toggle(btnX, toggleY, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.show_horn.name"),
                config.showHorn, toggle -> {
                    config.showHorn = toggle.getState();
                    Config.save();
                });
        this.widgets.add(hornToggle);
        screen.addConsoleWidget(hornToggle);

        int colorBtnY = toggleY + buttonHeight + spacing;
        Text colorLabel = Text.translatable("text.magicaland.config.horn_color.name");
        ColorPicker colorPicker = new ColorPicker(btnX, colorBtnY, buttonWidth, buttonHeight,
                colorLabel, config.hornColor, newColor -> {
                    config.hornColor = newColor;
                    Config.save();
                });
        this.widgets.add(colorPicker);
        screen.addConsoleWidget(colorPicker);
    }

    private void reinit(ConfigScreen screen) {
        screen.reinitScreen();
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
    public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta,
            float alpha) {
        if (this.ponyAnimatable == null)
            return;

        this.currentAlpha = alpha;
        this.ponyAnimatable.setPlayer(MinecraftClient.getInstance().player);

        float baseScale = Math.min(width / 6.0f, height / 4.0f);
        float modelScale = Math.max(30.0f, Math.min(baseScale, 150.0f));

        int btnWidth = Math.min(180, this.rightWidth / 2);
        int btnX = this.rightX + this.rightWidth - btnWidth - 20;
        int modelX = (this.rightX * 3 + btnX) / 4;
        int modelY = y + height / 2 + (int)(height * 0.42f);

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(modelX, modelY, 100);
        matrices.scale(modelScale * alpha, modelScale * alpha, modelScale * alpha);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(155.0f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0f));

        try {
            RenderLayer renderLayer = this.ponyRenderer.getRenderType(this.ponyAnimatable,
                    this.ponyRenderer.getTextureLocation(this.ponyAnimatable), context.getVertexConsumers(), delta);
            if (renderLayer != null) {
                VertexConsumer vertexConsumer = context.getVertexConsumers().getBuffer(renderLayer);
                this.ponyRenderer.render(matrices, this.ponyAnimatable, context.getVertexConsumers(), renderLayer,
                        vertexConsumer, 0xF000F0);
            }
        } catch (Exception e) {
            // 滚木
        }

        matrices.pop();
    }

    private GeoObjectRenderer<GeckoPlayerAnimatable> createPonyRenderer() {
        return new GeoObjectRenderer<>(new GeckoPlayerModel() {
            @Override
            public void applyMolangQueries(GeckoPlayerAnimatable animatable, double animTime) {
                try {
                    super.applyMolangQueries(animatable, animTime);
                } catch (Exception ignored) {
                    // 滚木
                }
            }
        }) {
            private static final Identifier PONY_BASE = new Identifier("magicaland", "textures/entity/base.png");
            private static final Identifier PONY_TS = new Identifier("magicaland", "textures/entity/mane.png");

            @Override
            public RenderLayer getRenderType(GeckoPlayerAnimatable animatable, Identifier texture,
                    VertexConsumerProvider bufferSource, float partialTick) {
                return RenderLayer.getEntityTranslucent(texture);
            }

            @Override
            public void renderRecursively(MatrixStack poseStack, GeckoPlayerAnimatable animatable,
                    GeoBone bone, RenderLayer renderType,
                    VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                    int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
                String name = bone.getName().toLowerCase();

                boolean isOther = name.contains("mane") || name.contains("tail") || name.contains("wing");

                Identifier texture = isOther ? PONY_TS : PONY_BASE;
                RenderLayer newRenderType = this.getRenderType(animatable, texture, bufferSource, partialTick);
                VertexConsumer newBuffer = bufferSource.getBuffer(newRenderType);

                super.renderRecursively(poseStack, animatable, bone, newRenderType, bufferSource, newBuffer, isReRender,
                        partialTick, packedLight, packedOverlay, red, green, blue, alpha);
            }

            @Override
            public void renderCubesOfBone(MatrixStack poseStack, GeoBone bone,
                    VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue,
                    float alpha) {
                Config config = Config.getInstance();

                if (!shouldRenderSelectedMane(bone.getName())) {
                    return;
                }

                if (!shouldRenderSelectedEye(bone.getName())) {
                    return;
                }

                if (!config.showHorn && bone.getName().equalsIgnoreCase("Horn")) {
                    return;
                }

                if (config.showHorn && bone.getName().equalsIgnoreCase("Horn")) {
                    int color = parseColor(config.hornColor);
                    float cr = ((color >> 16) & 0xFF) / 255.0f;
                    float cg = ((color >> 8) & 0xFF) / 255.0f;
                    float cb = (color & 0xFF) / 255.0f;
                    red *= cr;
                    green *= cg;
                    blue *= cb;
                }

                if (bone.getName().equalsIgnoreCase("body")) {
                    red *= 0.2f;
                    green *= 0.5f;
                    blue *= 1.0f;
                }
                super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            }

            private int parseColor(String hex) {
                try {
                    if (hex.startsWith("#")) {
                        hex = hex.substring(1);
                    }
                    return (int) Long.parseLong(hex, 16);
                } catch (Exception e) {
                    return 0xFFFFFFFF;
                }
            }

            private boolean shouldRenderSelectedMane(String boneName) {
                Config config = Config.getInstance();
                String lower = boneName.toLowerCase();

                if (boneName.equals("Bun")) {
                    return false;
                }

                if (!lower.contains("mane")) {
                    return true;
                }

                if (boneName.equals("Mane") || boneName.equals("FrontMane") || boneName.equals("BackMane")) {
                    return true;
                }

                String frontStyle = config.frontManeStyle;
                String backStyle = config.backManeStyle;

                if (boneName.startsWith(frontStyle + "FrontMane")) {
                    return true;
                }

                if (boneName.startsWith(backStyle + "BackMane")) {
                    return true;
                }

                return false;
            }

            private boolean shouldRenderSelectedEye(String boneName) {
                Config config = Config.getInstance();
                String eyeStyle = config.eyeStyle;

                boolean isEyeBone = boneName.equals("CommonFace") || boneName.equals("leye") || boneName.equals("reye")
                        || boneName.equals("FSCommonFace") || boneName.equals("leye2") || boneName.equals("reye2")
                        || boneName.equals("RRCommonFace") || boneName.equals("leye3") || boneName.equals("reye3");

                if (!isEyeBone) {
                    return true;
                }

                switch (eyeStyle) {
                    case "TS":
                        return boneName.equals("CommonFace") || boneName.equals("leye") || boneName.equals("reye");
                    case "FS":
                        return boneName.equals("FSCommonFace") || boneName.equals("leye2") || boneName.equals("reye2");
                    case "RR":
                        return boneName.equals("RRCommonFace") || boneName.equals("leye3") || boneName.equals("reye3");
                    default:
                        return boneName.equals("CommonFace") || boneName.equals("leye") || boneName.equals("reye");
                }
            }
        };
    }
}