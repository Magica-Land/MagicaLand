package top.csituka.magicaland.client.gui.tab;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
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
import top.csituka.magicaland.client.gui.widget.ColorPicker;
import top.csituka.magicaland.client.gui.widget.CustomButton;
import top.csituka.magicaland.client.gui.widget.DropdownBox;
import top.csituka.magicaland.client.gui.widget.SectionLabel;
import top.csituka.magicaland.client.gui.widget.SettingsList;
import top.csituka.magicaland.client.gui.widget.Toggle;
import top.csituka.magicaland.client.model.GeckoPlayerAnimatable;
import top.csituka.magicaland.client.model.GeckoPlayerModel;
import top.csituka.magicaland.client.render.PonyRenderer;

import java.util.List;
import java.util.function.Consumer;

public class PonyCustom implements TabContent {
    private static final Logger LOGGER = LoggerFactory.getLogger(PonyCustom.class);
    private SettingsList listWidget;
    private SettingsList previousListWidget;
    private float transitionAlpha = 1.0f;
    private boolean isTransitioning = false;
    private int transitionDirection = 1;

    private int rightX;
    private int rightWidth;
    private int rightY;
    private int rightHeight;

    private boolean hornMenuOpen = false;
    private boolean maneMenuOpen = false;
    private boolean faceMenuOpen = false;
    private boolean bodyMenuOpen = false;
    private boolean createNewOpen = false;
    private boolean deleteConfirmOpen = false;
    private String modelToDelete = null;
    private boolean isEditing = false;

    @Override
    public void onEnter() {
        this.hornMenuOpen = false;
        this.maneMenuOpen = false;
        this.faceMenuOpen = false;
        this.bodyMenuOpen = false;
        this.createNewOpen = false;
        this.deleteConfirmOpen = false;
        this.modelToDelete = null;
        this.isEditing = false;
        this.previewYaw = 155.0f;
        this.previewPitch = -10.0f;
    }

    @Override
    public void onExit() {
        ColorPicker.clearBodyLinkGroup();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= this.rightX && mouseX <= this.rightX + this.rightWidth) {
            this.isDraggingModel = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDraggingModel = false;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && this.isDraggingModel) {
            this.previewYaw -= (float) deltaX * dragSensitivity;
            this.previewPitch += (float) deltaY * dragSensitivity;
            this.previewPitch = Math.max(-90.0f, Math.min(90.0f, this.previewPitch));
            return true;
        }
        return false;
    }

    private GeckoPlayerAnimatable ponyAnimatable;
    private GeoObjectRenderer<GeckoPlayerAnimatable> ponyRenderer;

    private float previewYaw = 155.0f;
    private float previewPitch = -10.0f;
    private boolean isDraggingModel = false;
    private float dragSensitivity = 0.5f;

    private DropdownBox modelDropdown;
    private CustomButton editBtn;
    private CustomButton newBtn;
    private CustomButton deleteBtn;
    private TextFieldWidget newModelNameField;
    private CustomButton confirmCreateBtn;
    private CustomButton cancelCreateBtn;

    private static final String[] FRONT_MANE_STYLES = {"TS", "RD", "RR", "PP", "AJ", "FS"};
    private static final String[] BACK_MANE_STYLES = {"TS", "RD", "RR", "PP", "AJ", "FS"};
    private static final String[] TAIL_STYLES = {"TS", "RD", "RR", "PP", "AJ", "FS"};
    private static final String[] EYE_STYLES = {"TS", "FS", "RR"};

    @Override
    public void init(ConfigScreen screen, int x, int y, int width, int height) {
        ColorPicker.clearBodyLinkGroup();
        this.rightX = x;
        this.rightWidth = width;
        this.rightY = y;
        this.rightHeight = height;

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

        ModelConfig activeModel = ModelManager.getActiveModel();

        if (!isEditing || activeModel == null) {
            initWelcomeScreen(screen, x, y, width, height);
        } else {
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
        }

        this.listWidget.centerIfShort();
        screen.addConsoleElement(this.listWidget);
    }

    private void initWelcomeScreen(ConfigScreen screen, int x, int y, int width, int height) {
        ModelManager.refreshModelList();
        List<String> models = ModelManager.getAvailableModels();

        int dropdownWidth = Math.min(120, width / 2 - 30);
        int dropdownHeight = 20;
        int iconBtnWidth = 20;

        int totalWidth = dropdownWidth + iconBtnWidth * 3 + 15;
        int startX = x + width - totalWidth - 10;
        if (width < 250) {
            startX = x + (width - totalWidth) / 2;
        }
        int centerY = y + height / 2;

        if (createNewOpen) {
            int fieldWidth = 150;
            int createX = x + width - fieldWidth - 20;
            if (width < 250) {
                createX = x + (width - fieldWidth) / 2;
            }
            var textRenderer = MinecraftClient.getInstance().textRenderer;
            this.newModelNameField = new TextFieldWidget(textRenderer, createX, centerY - 20, fieldWidth, 20, Text.translatable("text.magicaland.config.create_model.name_placeholder"));
            this.newModelNameField.setMaxLength(32);
            this.newModelNameField.setFocused(true);
            screen.addConsoleWidget(this.newModelNameField);

            this.confirmCreateBtn = new CustomButton(createX, centerY + 10, 70, 20, Text.translatable("text.magicaland.config.button.confirm"), false, button -> {
                String name = this.newModelNameField.getText();
                if (ModelManager.createModel(name)) {
                    this.createNewOpen = false;
                    reinit(screen);
                }
            });
            screen.addConsoleWidget(this.confirmCreateBtn);

            this.cancelCreateBtn = new CustomButton(createX + 80, centerY + 10, 70, 20, Text.translatable("text.magicaland.config.button.cancel"), false, button -> {
                this.createNewOpen = false;
                reinit(screen);
            });
            screen.addConsoleWidget(this.cancelCreateBtn);
        } else if (deleteConfirmOpen) {
            int boxWidth = 150;
            int deleteX = x + width - boxWidth - 20;
            if (width < 250) {
                deleteX = x + (width - boxWidth) / 2;
            }
            this.confirmCreateBtn = new CustomButton(deleteX, centerY + 10, 70, 20, Text.translatable("text.magicaland.config.button.confirm"), false, button -> {
                if (modelToDelete != null) {
                    if (ModelManager.deleteModel(modelToDelete)) {
                        this.deleteConfirmOpen = false;
                        this.modelToDelete = null;
                        reinit(screen);
                    }
                }
            });
            screen.addConsoleWidget(this.confirmCreateBtn);

            this.cancelCreateBtn = new CustomButton(deleteX + 80, centerY + 10, 70, 20, Text.translatable("text.magicaland.config.button.cancel"), false, button -> {
                this.deleteConfirmOpen = false;
                this.modelToDelete = null;
                reinit(screen);
            });
            screen.addConsoleWidget(this.cancelCreateBtn);
        } else {
            int initialSelect = -1;
            ModelConfig activeModel = ModelManager.getActiveModel();
            if (activeModel != null) {
                initialSelect = models.indexOf(activeModel.name);
            }
            this.modelDropdown = new DropdownBox(startX, centerY, dropdownWidth, dropdownHeight, models, initialSelect, index -> {
                this.editBtn.active = true;
                this.deleteBtn.active = models.size() > 1;
                String selected = models.get(index);
                ModelManager.loadModel(selected);
            });
            screen.addConsoleWidget(this.modelDropdown);

            this.editBtn = new CustomButton(startX + dropdownWidth + 5, centerY, iconBtnWidth, dropdownHeight, Text.literal("✎"), Text.translatable("text.magicaland.config.tooltip.edit"), false, button -> {
                String selected = this.modelDropdown.getSelectedOption();
                if (selected != null) {
                    if (ModelManager.loadModel(selected)) {
                        this.isEditing = true;
                        reinit(screen);
                    }
                }
            });
            this.editBtn.active = activeModel != null;
            screen.addConsoleWidget(this.editBtn);

            this.deleteBtn = new CustomButton(startX + dropdownWidth + iconBtnWidth + 10, centerY, iconBtnWidth, dropdownHeight, Text.literal("✖"), Text.translatable("text.magicaland.config.tooltip.delete"), false, button -> {
                String selected = this.modelDropdown.getSelectedOption();
                if (selected != null && ModelManager.getAvailableModels().size() > 1) {
                    this.deleteConfirmOpen = true;
                    this.modelToDelete = selected;
                    reinit(screen);
                }
            });
            this.deleteBtn.active = models.size() > 1;
            screen.addConsoleWidget(this.deleteBtn);

            this.newBtn = new CustomButton(startX + dropdownWidth + iconBtnWidth * 2 + 15, centerY, iconBtnWidth, dropdownHeight, Text.literal("+"), Text.translatable("text.magicaland.config.tooltip.new"), false, button -> {
                this.createNewOpen = true;
                reinit(screen);
            });
            screen.addConsoleWidget(this.newBtn);
        }
    }

    private void initMainMenu(ConfigScreen screen, int x, int y, int width, int height) {
        int buttonWidth = Math.min(180, width / 2);
        int buttonHeight = 20;

        int btnX = x + width - buttonWidth - 20;
        if (width < 250) {
            btnX = x + (width - buttonWidth) / 2;
        }

        CustomButton quitBtn = new CustomButton(btnX, 0, buttonWidth, buttonHeight,
                Text.literal("← " + Text.translatable("text.magicaland.config.button.quit_edit").getString()),
                false, button -> {
                    ModelManager.saveActiveModel();
                    this.isEditing = false;
                    reinit(screen);
                }, false, true);
        this.listWidget.addWidget(quitBtn, SettingsList.Alignment.RIGHT);

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
        ModelConfig config = ModelManager.getActiveModel();
        if (config == null) return;

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

        SectionLabel maneStyleLabel = new SectionLabel(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.section.mane_styles.name"));
        this.listWidget.addWidget(maneStyleLabel, SettingsList.Alignment.RIGHT);

        CustomButton frontBtn = createStyleButton(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.front_mane_style.name"),
                config.frontManeStyle, FRONT_MANE_STYLES, newStyle -> {
                    config.frontManeStyle = newStyle;
                    ModelManager.saveActiveModel();
                });
        this.listWidget.addWidget(frontBtn, SettingsList.Alignment.RIGHT);

        CustomButton backManeBtn = createStyleButton(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.back_mane_style.name"),
                config.backManeStyle, BACK_MANE_STYLES, newStyle -> {
                    config.backManeStyle = newStyle;
                    ModelManager.saveActiveModel();
                });
        this.listWidget.addWidget(backManeBtn, SettingsList.Alignment.RIGHT);

        CustomButton tailBtn = createStyleButton(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.tail_style.name"),
                config.tailStyle, TAIL_STYLES, newStyle -> {
                    config.tailStyle = newStyle;
                    ModelManager.saveActiveModel();
                });
        this.listWidget.addWidget(tailBtn, SettingsList.Alignment.RIGHT);

        SectionLabel maneColorLabel = new SectionLabel(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.section.mane_colors.name"));
        this.listWidget.addWidget(maneColorLabel, SettingsList.Alignment.RIGHT);

        ColorPicker frontManeColorPicker = new ColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.front_mane_color.name"),
                config.frontManeColor, newColor -> {
                    config.frontManeColor = newColor;
                    ModelManager.saveActiveModel();
                });
        this.listWidget.addWidget(frontManeColorPicker, SettingsList.Alignment.RIGHT);

        ColorPicker backManeColorPicker = new ColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.back_mane_color.name"),
                config.backManeColor, newColor -> {
                    config.backManeColor = newColor;
                    ModelManager.saveActiveModel();
                });
        this.listWidget.addWidget(backManeColorPicker, SettingsList.Alignment.RIGHT);

        ColorPicker tailColorPicker = new ColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.tail_color.name"),
                config.tailColor, newColor -> {
                    config.tailColor = newColor;
                    ModelManager.saveActiveModel();
                });
        this.listWidget.addWidget(tailColorPicker, SettingsList.Alignment.RIGHT);
    }

    private void initFaceMenu(ConfigScreen screen, int x, int y, int width, int height) {
        ModelConfig config = ModelManager.getActiveModel();
        if (config == null) return;

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

        SectionLabel faceStyleLabel = new SectionLabel(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.section.mane_styles.name"));
        this.listWidget.addWidget(faceStyleLabel, SettingsList.Alignment.RIGHT);

        CustomButton eyeBtn = createStyleButton(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.eye_style.name"),
                config.eyeStyle, EYE_STYLES, newStyle -> {
                    config.eyeStyle = newStyle;
                    ModelManager.saveActiveModel();
                });
        this.listWidget.addWidget(eyeBtn, SettingsList.Alignment.RIGHT);

        SectionLabel faceColorLabel = new SectionLabel(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.section.mane_colors.name"));
        this.listWidget.addWidget(faceColorLabel, SettingsList.Alignment.RIGHT);

        ColorPicker nosePicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.nose_color.name"),
                config.noseColor, config.noseColorLocked,
                newColor -> { config.noseColor = newColor; ModelManager.saveActiveModel(); },
                locked -> { config.noseColorLocked = locked; ModelManager.saveActiveModel(); });
        this.listWidget.addWidget(nosePicker, SettingsList.Alignment.RIGHT);
    }

    private void initHornMenu(ConfigScreen screen, int x, int y, int width, int height) {
        ModelConfig config = ModelManager.getActiveModel();
        if (config == null) return;

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

        SectionLabel hornStyleLabel = new SectionLabel(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.section.mane_styles.name"));
        this.listWidget.addWidget(hornStyleLabel, SettingsList.Alignment.RIGHT);

        Toggle hornToggle = new Toggle(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.show_horn.name"),
                config.showHorn, toggle -> {
                    config.showHorn = toggle.getState();
                    ModelManager.saveActiveModel();
                });
        this.listWidget.addWidget(hornToggle, SettingsList.Alignment.RIGHT);

        Toggle wingToggle = new Toggle(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.show_wings.name"),
                config.showWings, toggle -> {
                    config.showWings = toggle.getState();
                    ModelManager.saveActiveModel();
                });
        this.listWidget.addWidget(wingToggle, SettingsList.Alignment.RIGHT);

        SectionLabel hornColorLabel = new SectionLabel(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.section.mane_colors.name"));
        this.listWidget.addWidget(hornColorLabel, SettingsList.Alignment.RIGHT);

        ColorPicker hornColorPicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.horn_color.name"),
                config.hornColor, config.hornColorLocked,
                newColor -> { config.hornColor = newColor; ModelManager.saveActiveModel(); },
                locked -> { config.hornColorLocked = locked; ModelManager.saveActiveModel(); });
        this.listWidget.addWidget(hornColorPicker, SettingsList.Alignment.RIGHT);

        ColorPicker wingColorPicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.wing_color.name"),
                config.wingColor, config.wingColorLocked,
                newColor -> { config.wingColor = newColor; ModelManager.saveActiveModel(); },
                locked -> { config.wingColorLocked = locked; ModelManager.saveActiveModel(); });
        this.listWidget.addWidget(wingColorPicker, SettingsList.Alignment.RIGHT);
    }

    private void initBodyMenu(ConfigScreen screen, int x, int y, int width, int height) {
        ModelConfig config = ModelManager.getActiveModel();
        if (config == null) return;

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
                newColor -> { config.bodyColor = newColor; ModelManager.saveActiveModel(); },
                locked -> { config.bodyColorLocked = locked; ModelManager.saveActiveModel(); });
        this.listWidget.addWidget(bodyPicker, SettingsList.Alignment.RIGHT);

        ColorPicker neckPicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.neck_color.name"),
                config.neckColor, config.neckColorLocked,
                newColor -> { config.neckColor = newColor; ModelManager.saveActiveModel(); },
                locked -> { config.neckColorLocked = locked; ModelManager.saveActiveModel(); });
        this.listWidget.addWidget(neckPicker, SettingsList.Alignment.RIGHT);

        ColorPicker headPicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.head_color.name"),
                config.headColor, config.headColorLocked,
                newColor -> { config.headColor = newColor; ModelManager.saveActiveModel(); },
                locked -> { config.headColorLocked = locked; ModelManager.saveActiveModel(); });
        this.listWidget.addWidget(headPicker, SettingsList.Alignment.RIGHT);

        SectionLabel earLabel = new SectionLabel(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.section.ears.name"));
        this.listWidget.addWidget(earLabel, SettingsList.Alignment.RIGHT);

        ColorPicker leftEarPicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.left_ear_color.name"),
                config.leftEarColor, config.leftEarColorLocked,
                newColor -> { config.leftEarColor = newColor; ModelManager.saveActiveModel(); },
                locked -> { config.leftEarColorLocked = locked; ModelManager.saveActiveModel(); });
        this.listWidget.addWidget(leftEarPicker, SettingsList.Alignment.RIGHT);

        ColorPicker rightEarPicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.right_ear_color.name"),
                config.rightEarColor, config.rightEarColorLocked,
                newColor -> { config.rightEarColor = newColor; ModelManager.saveActiveModel(); },
                locked -> { config.rightEarColorLocked = locked; ModelManager.saveActiveModel(); });
        this.listWidget.addWidget(rightEarPicker, SettingsList.Alignment.RIGHT);

        SectionLabel limbLabel = new SectionLabel(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.section.limbs.name"));
        this.listWidget.addWidget(limbLabel, SettingsList.Alignment.RIGHT);

        ColorPicker leftFrontLimbPicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.left_front_limb_color.name"),
                config.leftFrontLimbColor, config.leftFrontLimbColorLocked,
                newColor -> { config.leftFrontLimbColor = newColor; ModelManager.saveActiveModel(); },
                locked -> { config.leftFrontLimbColorLocked = locked; ModelManager.saveActiveModel(); });
        this.listWidget.addWidget(leftFrontLimbPicker, SettingsList.Alignment.RIGHT);

        ColorPicker rightFrontLimbPicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.right_front_limb_color.name"),
                config.rightFrontLimbColor, config.rightFrontLimbColorLocked,
                newColor -> { config.rightFrontLimbColor = newColor; ModelManager.saveActiveModel(); },
                locked -> { config.rightFrontLimbColorLocked = locked; ModelManager.saveActiveModel(); });
        this.listWidget.addWidget(rightFrontLimbPicker, SettingsList.Alignment.RIGHT);

        ColorPicker leftHindLimbPicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.left_hind_limb_color.name"),
                config.leftHindLimbColor, config.leftHindLimbColorLocked,
                newColor -> { config.leftHindLimbColor = newColor; ModelManager.saveActiveModel(); },
                locked -> { config.leftHindLimbColorLocked = locked; ModelManager.saveActiveModel(); });
        this.listWidget.addWidget(leftHindLimbPicker, SettingsList.Alignment.RIGHT);

        ColorPicker rightHindLimbPicker = createBodyColorPicker(btnX, 0, buttonWidth, buttonHeight,
                Text.translatable("text.magicaland.config.right_hind_limb_color.name"),
                config.rightHindLimbColor, config.rightHindLimbColorLocked,
                newColor -> { config.rightHindLimbColor = newColor; ModelManager.saveActiveModel(); },
                locked -> { config.rightHindLimbColorLocked = locked; ModelManager.saveActiveModel(); });
        this.listWidget.addWidget(rightHindLimbPicker, SettingsList.Alignment.RIGHT);
    }

    private ColorPicker createBodyColorPicker(int x, int y, int width, int height, Text label,
            String initialColor, boolean initiallyLocked,
            Consumer<String> onColorChanged, Consumer<Boolean> onLockChanged) {
        final ColorPicker[] pickerHolder = new ColorPicker[1];
        
        ColorPicker picker = new ColorPicker(x, y, width, height, label, initialColor, newColor -> {
            onColorChanged.accept(newColor);
            if (pickerHolder[0] != null && pickerHolder[0].isLocked()) {
                syncLockedBodyColors(newColor);
            }
            ModelManager.saveActiveModel();
        });
        
        pickerHolder[0] = picker;
        ColorPicker.addToBodyLinkGroup(picker, initiallyLocked);
        picker.setLocked(initiallyLocked);
        picker.setOnLockChanged(locked -> {
            onLockChanged.accept(locked);
            ModelManager.saveActiveModel();
        });
        return picker;
    }

    private void syncLockedBodyColors(String color) {
        ModelConfig config = ModelManager.getActiveModel();
        if (config == null) return;
        
        if (config.hornColorLocked) config.hornColor = color;
        if (config.bodyColorLocked) config.bodyColor = color;
        if (config.neckColorLocked) config.neckColor = color;
        if (config.headColorLocked) config.headColor = color;
        if (config.noseColorLocked) config.noseColor = color;
        if (config.leftEarColorLocked) config.leftEarColor = color;
        if (config.rightEarColorLocked) config.rightEarColor = color;
        if (config.leftFrontLimbColorLocked) config.leftFrontLimbColor = color;
        if (config.rightFrontLimbColorLocked) config.rightFrontLimbColor = color;
        if (config.leftHindLimbColorLocked) config.leftHindLimbColor = color;
        if (config.rightHindLimbColorLocked) config.rightHindLimbColor = color;
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
        ModelConfig activeModel = ModelManager.getActiveModel();

        if ((!isEditing && (createNewOpen || deleteConfirmOpen)) || activeModel == null) {
            var textRenderer = MinecraftClient.getInstance().textRenderer;
            int textAlpha = (int) (alpha * 255);
            
            if (createNewOpen) {
                int fieldWidth = 150;
                int createX = x + width - fieldWidth - 20;
                int labelY = y + height / 2 - 40;
                
                if (width < 250) {
                    context.drawCenteredTextWithShadow(textRenderer, Text.translatable("text.magicaland.config.create_model.title"), x + width / 2, labelY, (textAlpha << 24) | 0xFFFFFF);
                } else {
                    context.drawTextWithShadow(textRenderer, Text.translatable("text.magicaland.config.create_model.title"), createX, labelY, (textAlpha << 24) | 0xFFFFFF);
                }
                
                if (newModelNameField != null) {
                    newModelNameField.render(context, mouseX, mouseY, delta);
                }
            } else if (deleteConfirmOpen) {
                int boxWidth = 150;
                int deleteX = x + width - boxWidth - 20;
                int labelY = y + height / 2 - 40;
                
                if (width < 250) {
                    context.drawCenteredTextWithShadow(textRenderer, Text.translatable("text.magicaland.config.delete_confirm.title"), x + width / 2, labelY, (textAlpha << 24) | 0xFFFFFF);
                    context.drawCenteredTextWithShadow(textRenderer, Text.translatable("text.magicaland.config.delete_confirm.message"), x + width / 2, labelY + 15, (textAlpha << 24) | 0xE06060);
                } else {
                    context.drawTextWithShadow(textRenderer, Text.translatable("text.magicaland.config.delete_confirm.title"), deleteX, labelY, (textAlpha << 24) | 0xFFFFFF);
                    context.drawTextWithShadow(textRenderer, Text.translatable("text.magicaland.config.delete_confirm.message"), deleteX, labelY + 15, (textAlpha << 24) | 0xE06060);
                }
            }
            return;
        }

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
        int modelY = y + height / 2 + (int)(height * 0.1f);

        MatrixStack matrices = context.getMatrices();
        context.disableScissor();
        matrices.push();

        matrices.translate(modelX, modelY, 100);
        matrices.scale(modelScale * alpha, modelScale * alpha, modelScale * alpha);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(previewYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(previewPitch));

        matrices.push();
        float pivotOffsetX = -0.5f;
        float pivotOffsetY = -1.5f;
        float pivotOffsetZ = -0.7f;
        matrices.translate(pivotOffsetX, pivotOffsetY, pivotOffsetZ);

        try {
            RenderLayer renderLayer = this.ponyRenderer.getRenderType(this.ponyAnimatable,
                    this.ponyRenderer.getTextureLocation(this.ponyAnimatable), context.getVertexConsumers(), delta);
            if (renderLayer != null) {
                VertexConsumer vertexConsumer = context.getVertexConsumers().getBuffer(renderLayer);
                this.ponyRenderer.render(matrices, this.ponyAnimatable, context.getVertexConsumers(), renderLayer,
                        vertexConsumer, 0xF000F0);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to render pony model preview in GUI", e);
        }

        matrices.pop();

        matrices.pop();

        context.enableScissor(this.rightX, 0, this.rightWidth + 120, this.rightHeight);
    }

    private GeoObjectRenderer<GeckoPlayerAnimatable> createPonyRenderer() {
        return new PonyRenderer(new GeckoPlayerModel() {
            @Override
            public void applyMolangQueries(GeckoPlayerAnimatable animatable, double animTime) {
                if (MinecraftClient.getInstance().world == null)
                    return;
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
}
