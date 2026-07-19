package top.csituka.magicaland.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import top.csituka.magicaland.client.gui.ConfigScreen;
import top.csituka.magicaland.client.model.GeckoPlayerAnimatable;
import top.csituka.magicaland.client.model.GeckoPlayerModel;
import top.csituka.magicaland.client.render.PonyRenderer;

/**
 * 主菜单右下角小马模型 + 捏马按钮
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Unique
    private static GeckoPlayerAnimatable titlePonyAnimatable;

    @Unique
    private static PonyRenderer titlePonyRenderer;

    @Unique
    private static boolean titlePonyInitialized = false;

    @Unique
    private static void initTitlePonyResources() {
        if (titlePonyInitialized)
            return;

        titlePonyAnimatable = new GeckoPlayerAnimatable() {
            @Unique
            private long titleScreenTick = 0;

            @Override
            public double getTick(Object o) {
                return titleScreenTick++;
            }

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

        titlePonyRenderer = new PonyRenderer(new GeckoPlayerModel() {
            @Override
            public void applyMolangQueries(GeckoPlayerAnimatable animatable, double animTime) {
                if (MinecraftClient.getInstance().world == null)
                    return;
                try {
                    super.applyMolangQueries(animatable, animTime);
                } catch (Exception e) {
                }
            }
        }) {
            @Override
            public RenderLayer getRenderType(GeckoPlayerAnimatable animatable,
                    net.minecraft.util.Identifier texture,
                    net.minecraft.client.render.VertexConsumerProvider bufferSource,
                    float partialTick) {
                return RenderLayer.getEntityTranslucent(texture);
            }
        };

        titlePonyInitialized = true;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void magicaland$onInit(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int buttonWidth = 60;
        int buttonHeight = 20;
        int padding = 15;

        int x = screenWidth - buttonWidth - padding;
        int y = screenHeight - buttonHeight - padding - 25;

        ButtonWidget ponyButton = ButtonWidget.builder(
                Text.translatable("text.magicaland.console.tab.pony_custom"),
                button -> client.setScreen(
                        new ConfigScreen((Screen) (Object) this, ConfigScreen.Tab.PONY_CUSTOM)))
                .dimensions(x, y, buttonWidth, buttonHeight)
                .build();

        ((ScreenAccessor) this).invokeAddDrawableChild(ponyButton);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void magicaland$onRender(DrawContext context, int mouseX, int mouseY, float delta,
            CallbackInfo ci) {
        initTitlePonyResources();

        if (titlePonyAnimatable == null || titlePonyRenderer == null)
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int btnHeight = 20;
        int padding = 15;
        int btnX = screenWidth - 60 - padding;
        int btnCenterX = btnX + 30;
        int btnTopY = screenHeight - btnHeight - padding;

        int modelX = btnCenterX;
        int modelY = btnTopY - 60;

        float modelScale = 35.0f;

        context.getMatrices().push();

        context.getMatrices().translate(modelX, modelY, 200);
        context.getMatrices().scale(modelScale, modelScale, modelScale);
        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
        context.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(155.0f));
        context.getMatrices().multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0f));

        // 与捏马页面相同的轴心偏移
        context.getMatrices().push();
        context.getMatrices().translate(-0.5f, -1.5f, -0.7f);

        try {
            RenderLayer renderLayer = titlePonyRenderer.getRenderType(titlePonyAnimatable,
                    titlePonyRenderer.getTextureLocation(titlePonyAnimatable),
                    context.getVertexConsumers(), delta);
            if (renderLayer != null) {
                VertexConsumer vertexConsumer = context.getVertexConsumers().getBuffer(renderLayer);
                titlePonyRenderer.render(context.getMatrices(), titlePonyAnimatable,
                        context.getVertexConsumers(), renderLayer, vertexConsumer, 0xF000F0);
            }
        } catch (Exception e) {
        }

        context.getMatrices().pop();
        context.getMatrices().pop();
    }
}
