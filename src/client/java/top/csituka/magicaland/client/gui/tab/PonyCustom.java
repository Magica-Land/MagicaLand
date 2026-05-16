package top.csituka.magicaland.client.gui.tab;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import top.csituka.magicaland.client.config.Config;
import top.csituka.magicaland.client.gui.ConfigScreen;
import top.csituka.magicaland.client.gui.widget.ColorPicker;
import top.csituka.magicaland.client.gui.widget.CustomButton;
import top.csituka.magicaland.client.gui.widget.SectionLabel;
import top.csituka.magicaland.client.gui.widget.SettingsList;
import top.csituka.magicaland.client.gui.widget.Toggle;
import top.csituka.magicaland.client.model.GeckoPlayerAnimatable;
import top.csituka.magicaland.client.model.GeckoPlayerModel;

import java.util.function.Consumer;

public class PonyCustom implements TabContent {
    private SettingsList listWidget;
    private SettingsList previousListWidget;
    private float transitionAlpha = 1.0f;
    private boolean isTransitioning = false;
    private int transitionDirection = 1;

    private int rightX;
    private int rightWidth;

    private boolean hornMenuOpen = false;
    private boolean maneMenuOpen = false;
    private boolean faceMenuOpen = false;
    private boolean bodyMenuOpen = false;

    private GeckoPlayerAnimatable ponyAnimatable;
    private GeoObjectRenderer<GeckoPlayerAnimatable> ponyRenderer;

    private static final String[] FRONT_MANE_STYLES = {"TS", "RD", "RR", "PP", "AJ", "FS"};
    private static final String[] BACK_MANE_STYLES = {"TS", "RD", "RR", "PP", "AJ", "FS"};
    private static final String[] EYE_STYLES = {"TS", "FS", "RR"};

    @Override
    public void init(ConfigScreen screen, int x, int y, int width, int height) {
        this.rightX = x;
        this.rightWidth = width;

        int topMargin = 40;
        int bottomMargin = 40;
        this.listWidget = new SettingsList(MinecraftClient.getInstance(), width, height, y + topMargin,
                y + height - bottomMargin, 24);
        this.listWidget.setLeftPos(x);

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
        } else if (bodyMenuOpen) {
            initBodyMenu(screen, x, y, width, height);
        } else {
            initMainMenu(screen, x, y, width, height);
        }

        this.listWidget.centerIfShort();
        screen.addConsoleElement(this.listWidget);
    }

    private void initMainMenu(ConfigScreen screen, int x, int y, int width, int height) {
        int buttonWidth = Math.min(180, width / 2);
        int buttonHeight = 20;

        int btnX = x + width - buttonWidth - 20;
        if (width < 250) {
            btnX = x + (width - buttonWidth) / 2;
        }

        CustomButton maneBtn = new CustomButton(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.mane_menu.name"),
                false, button -> switchMenu(screen, 1, () -> maneMenuOpen = true), true);
        this.listWidget.addWidget(maneBtn, SettingsList.Alignment.RIGHT);

        CustomButton faceBtn = new CustomButton(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.face_menu.name"),
                false, button -> switchMenu(screen, 1, () -> faceMenuOpen = true), true);
        this.listWidget.addWidget(faceBtn, SettingsList.Alignment.RIGHT);

        CustomButton hornBtn = new CustomButton(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.horn_menu.name"),
                false, button -> switchMenu(screen, 1, () -> hornMenuOpen = true), true);
        this.listWidget.addWidget(hornBtn, SettingsList.Alignment.RIGHT);

        CustomButton bodyBtn = new CustomButton(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.body_menu.name"),
                false, button -> switchMenu(screen, 1, () -> bodyMenuOpen = true), true);
        this.listWidget.addWidget(bodyBtn, SettingsList.Alignment.RIGHT);
    }

    private void initManeMenu(ConfigScreen screen, int x, int y, int width, int height) {
        Config config = Config.getInstance();
        int buttonWidth = Math.min(180, width / 2);
        int buttonHeight = 20;

        int btnX = x + width - buttonWidth - 20;
        if (width < 250) {
            btnX = x + (width - buttonWidth) / 2;
        }

        CustomButton backBtn = new CustomButton(btnX, 0, buttonWidth, buttonHeight,
                Text.literal("← " + Text.translatable("text.magicaland.config.mane_menu.name").getString()),
                false, button -> switchMenu(screen, -1, () -> maneMenuOpen = false), false, true);
        this.listWidget.addWidget(backBtn, SettingsList.Alignment.RIGHT);

        CustomButton frontBtn = createStyleButton(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.front_mane_style.name"),
                config.frontManeStyle, FRONT_MANE_STYLES, newStyle -> {
                    config.frontManeStyle = newStyle;
                    Config.save();
                });
        this.listWidget.addWidget(frontBtn, SettingsList.Alignment.RIGHT);

        CustomButton backManeBtn = createStyleButton(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.back_mane_style.name"),
                config.backManeStyle, BACK_MANE_STYLES, newStyle -> {
                    config.backManeStyle = newStyle;
                    Config.save();
                });
        this.listWidget.addWidget(backManeBtn, SettingsList.Alignment.RIGHT);
    }

    private void initFaceMenu(ConfigScreen screen, int x, int y, int width, int height) {
        Config config = Config.getInstance();
        int buttonWidth = Math.min(180, width / 2);
        int buttonHeight = 20;

        int btnX = x + width - buttonWidth - 20;
        if (width < 250) {
            btnX = x + (width - buttonWidth) / 2;
        }

        CustomButton backBtn = new CustomButton(btnX, 0, buttonWidth, buttonHeight,
                Text.literal("← " + Text.translatable("text.magicaland.config.face_menu.name").getString()),
                false, button -> switchMenu(screen, -1, () -> faceMenuOpen = false), false, true);
        this.listWidget.addWidget(backBtn, SettingsList.Alignment.RIGHT);

        CustomButton eyeBtn = createStyleButton(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.eye_style.name"),
                config.eyeStyle, EYE_STYLES, newStyle -> {
                    config.eyeStyle = newStyle;
                    Config.save();
                });
        this.listWidget.addWidget(eyeBtn, SettingsList.Alignment.RIGHT);
    }

    private void initHornMenu(ConfigScreen screen, int x, int y, int width, int height) {
        Config config = Config.getInstance();
        int buttonWidth = Math.min(180, width / 2);
        int buttonHeight = 20;

        int btnX = x + width - buttonWidth - 20;
        if (width < 250) {
            btnX = x + (width - buttonWidth) / 2;
        }

        CustomButton backBtn = new CustomButton(btnX, 0, buttonWidth, buttonHeight,
                Text.literal("← " + Text.translatable("text.magicaland.config.horn_menu.name").getString()),
                false, button -> switchMenu(screen, -1, () -> hornMenuOpen = false), false, true);
        this.listWidget.addWidget(backBtn, SettingsList.Alignment.RIGHT);

        Toggle hornToggle = new Toggle(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.show_horn.name"),
                config.showHorn, toggle -> {
                    config.showHorn = toggle.getState();
                    Config.save();
                });
        this.listWidget.addWidget(hornToggle, SettingsList.Alignment.RIGHT);

        Text colorLabel = Text.translatable("text.magicaland.config.horn_color.name");
        ColorPicker colorPicker = new ColorPicker(btnX, 0, buttonWidth, buttonHeight,
                colorLabel, config.hornColor, newColor -> {
                    config.hornColor = newColor;
                    Config.save();
                });
        this.listWidget.addWidget(colorPicker, SettingsList.Alignment.RIGHT);
    }

    private void initBodyMenu(ConfigScreen screen, int x, int y, int width, int height) {
        Config config = Config.getInstance();
        int buttonWidth = Math.min(180, width / 2);
        int buttonHeight = 20;

        int btnX = x + width - buttonWidth - 20;
        if (width < 250) {
            btnX = x + (width - buttonWidth) / 2;
        }

        CustomButton backBtn = new CustomButton(btnX, 0, buttonWidth, buttonHeight,
                Text.literal("← " + Text.translatable("text.magicaland.config.body_menu.name").getString()),
                false, button -> switchMenu(screen, -1, () -> bodyMenuOpen = false), false, true);
        this.listWidget.addWidget(backBtn, SettingsList.Alignment.RIGHT);

        ColorPicker bodyPicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.body_color.name"),
                config.bodyColor, config.bodyColorLocked,
                newColor -> { config.bodyColor = newColor; Config.save(); },
                locked -> { config.bodyColorLocked = locked; Config.save(); });
        this.listWidget.addWidget(bodyPicker, SettingsList.Alignment.RIGHT);

        ColorPicker neckPicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.neck_color.name"),
                config.neckColor, config.neckColorLocked,
                newColor -> { config.neckColor = newColor; Config.save(); },
                locked -> { config.neckColorLocked = locked; Config.save(); });
        this.listWidget.addWidget(neckPicker, SettingsList.Alignment.RIGHT);

        ColorPicker headPicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.head_color.name"),
                config.headColor, config.headColorLocked,
                newColor -> { config.headColor = newColor; Config.save(); },
                locked -> { config.headColorLocked = locked; Config.save(); });
        this.listWidget.addWidget(headPicker, SettingsList.Alignment.RIGHT);

        SectionLabel earLabel = new SectionLabel(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.section.ears.name"));
        this.listWidget.addWidget(earLabel, SettingsList.Alignment.RIGHT);

        ColorPicker leftEarPicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.left_ear_color.name"),
                config.leftEarColor, config.leftEarColorLocked,
                newColor -> { config.leftEarColor = newColor; Config.save(); },
                locked -> { config.leftEarColorLocked = locked; Config.save(); });
        this.listWidget.addWidget(leftEarPicker, SettingsList.Alignment.RIGHT);

        ColorPicker rightEarPicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.right_ear_color.name"),
                config.rightEarColor, config.rightEarColorLocked,
                newColor -> { config.rightEarColor = newColor; Config.save(); },
                locked -> { config.rightEarColorLocked = locked; Config.save(); });
        this.listWidget.addWidget(rightEarPicker, SettingsList.Alignment.RIGHT);

        SectionLabel limbLabel = new SectionLabel(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.section.limbs.name"));
        this.listWidget.addWidget(limbLabel, SettingsList.Alignment.RIGHT);

        ColorPicker leftFrontLimbPicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.left_front_limb_color.name"),
                config.leftFrontLimbColor, config.leftFrontLimbColorLocked,
                newColor -> { config.leftFrontLimbColor = newColor; Config.save(); },
                locked -> { config.leftFrontLimbColorLocked = locked; Config.save(); });
        this.listWidget.addWidget(leftFrontLimbPicker, SettingsList.Alignment.RIGHT);

        ColorPicker rightFrontLimbPicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.right_front_limb_color.name"),
                config.rightFrontLimbColor, config.rightFrontLimbColorLocked,
                newColor -> { config.rightFrontLimbColor = newColor; Config.save(); },
                locked -> { config.rightFrontLimbColorLocked = locked; Config.save(); });
        this.listWidget.addWidget(rightFrontLimbPicker, SettingsList.Alignment.RIGHT);

        ColorPicker leftHindLimbPicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.left_hind_limb_color.name"),
                config.leftHindLimbColor, config.leftHindLimbColorLocked,
                newColor -> { config.leftHindLimbColor = newColor; Config.save(); },
                locked -> { config.leftHindLimbColorLocked = locked; Config.save(); });
        this.listWidget.addWidget(leftHindLimbPicker, SettingsList.Alignment.RIGHT);

        ColorPicker rightHindLimbPicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.right_hind_limb_color.name"),
                config.rightHindLimbColor, config.rightHindLimbColorLocked,
                newColor -> { config.rightHindLimbColor = newColor; Config.save(); },
                locked -> { config.rightHindLimbColorLocked = locked; Config.save(); });
        this.listWidget.addWidget(rightHindLimbPicker, SettingsList.Alignment.RIGHT);
    }

    private ColorPicker createBodyColorPicker(int x, int y, int width, int height, Text label,
            String initialColor, boolean initiallyLocked,
            Consumer<String> onColorChanged, Consumer<Boolean> onLockChanged) {
        ColorPicker picker = new ColorPicker(x, y, width, height, label, initialColor, onColorChanged);
        ColorPicker.addToBodyLinkGroup(picker, initiallyLocked);
        picker.setLocked(initiallyLocked);
        picker.setOnLockChanged(locked -> {
            onLockChanged.accept(locked);
            Config.save();
        });
        return picker;
    }

    private void switchMenu(ConfigScreen screen, int direction, Runnable action) {
        this.previousListWidget = this.listWidget;
        action.run();
        this.isTransitioning = true;
        this.transitionDirection = direction;
        this.transitionAlpha = 0.0f;
        screen.reinitScreen();
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
        if (isTransitioning) {
            transitionAlpha += 0.1f;
            if (transitionAlpha >= 1.0f) {
                transitionAlpha = 1.0f;
                isTransitioning = false;
                previousListWidget = null;
            }
        }

        if (isTransitioning && previousListWidget != null) {
            float progress = transitionAlpha;

            float oldAlpha = (1.0f - progress) * alpha;
            float oldOffset = -progress * 60 * transitionDirection;
            previousListWidget.setBaseAlpha(oldAlpha);
            context.getMatrices().push();
            context.getMatrices().translate(oldOffset, 0, 0);
            previousListWidget.render(context, -1, -1, delta);
            context.getMatrices().pop();

            float newAlpha = progress * alpha;
            float newOffset = (1.0f - progress) * 60 * transitionDirection;
            listWidget.setBaseAlpha(newAlpha);
            context.getMatrices().push();
            context.getMatrices().translate(newOffset, 0, 0);
            listWidget.render(context, mouseX, mouseY, delta);
            context.getMatrices().pop();
        } else if (this.listWidget != null) {
            this.listWidget.setBaseAlpha(alpha);
            this.listWidget.render(context, mouseX, mouseY, delta);
        }

        if (this.ponyAnimatable == null)
            return;

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

                String boneName = bone.getName();
                String colorField = null;
                if (boneName.equalsIgnoreCase("Body")) {
                    colorField = config.bodyColor;
                } else if (boneName.equalsIgnoreCase("Neck")) {
                    colorField = config.neckColor;
                } else if (boneName.equalsIgnoreCase("Head")) {
                    colorField = config.headColor;
                } else if (boneName.equalsIgnoreCase("LeftEar")) {
                    colorField = config.leftEarColor;
                } else if (boneName.equalsIgnoreCase("RightEar")) {
                    colorField = config.rightEarColor;
                } else if (boneName.startsWith("LFront") || boneName.equalsIgnoreCase("LForeLeg")) {
                    colorField = config.leftFrontLimbColor;
                } else if (boneName.startsWith("RFront") || boneName.equalsIgnoreCase("RForeLeg")) {
                    colorField = config.rightFrontLimbColor;
                } else if (boneName.startsWith("LHind")) {
                    colorField = config.leftHindLimbColor;
                } else if (boneName.startsWith("RHind")) {
                    colorField = config.rightHindLimbColor;
                }

                if (colorField != null) {
                    int color = parseColor(colorField);
                    float cr = ((color >> 16) & 0xFF) / 255.0f;
                    float cg = ((color >> 8) & 0xFF) / 255.0f;
                    float cb = (color & 0xFF) / 255.0f;
                    red *= cr;
                    green *= cg;
                    blue *= cb;
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
