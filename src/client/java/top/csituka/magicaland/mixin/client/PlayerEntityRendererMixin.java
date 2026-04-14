package top.csituka.magicaland.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import top.csituka.magicaland.client.config.Config;
import top.csituka.magicaland.client.model.GeckoPlayerAnimatable;
import top.csituka.magicaland.client.model.GeckoPlayerModel;
import org.spongepowered.asm.mixin.Mixin;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin
        extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    @Unique
    private GeckoPlayerAnimatable ponyAnimatable;
    @Unique
    private GeoObjectRenderer<GeckoPlayerAnimatable> ponyRenderer;

    @Unique
    private static final Map<UUID, Float> flightRolls = new HashMap<>();

    public PlayerEntityRendererMixin(EntityRendererFactory.Context ctx,
            PlayerEntityModel<AbstractClientPlayerEntity> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {
        this.ponyAnimatable = new GeckoPlayerAnimatable();
        this.ponyRenderer = new GeoObjectRenderer<>(new GeckoPlayerModel());
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(AbstractClientPlayerEntity player, float f, float g, MatrixStack matrixStack,
            VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (Config.getInstance().replacePlayerModel) {
            this.ponyAnimatable.setPlayer(player);

            matrixStack.push();

            if (player.isSleeping()) {
                net.minecraft.util.math.Direction direction = player.getSleepingDirection();
                if (direction != null) {
                    float sleepYaw = direction.asRotation();
                    matrixStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(270.0F - sleepYaw));
                    matrixStack.translate(-1.7, -0.1, 0.0);
                    matrixStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(270.0F));
                    matrixStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
                }
            } else {
                float bodyYaw = net.minecraft.util.math.MathHelper.lerpAngleDegrees(g, player.prevBodyYaw, player.bodyYaw);
                matrixStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));

                if (player.getAbilities().flying && player.isSprinting()) {
                    float yawDelta = net.minecraft.util.math.MathHelper.wrapDegrees(player.bodyYaw - player.prevBodyYaw);
                    float targetRoll = net.minecraft.util.math.MathHelper.clamp(yawDelta * -2.5F, -30.0F, 30.0F);
                    float currentRoll = flightRolls.getOrDefault(player.getUuid(), 0.0F);
                    currentRoll = net.minecraft.util.math.MathHelper.lerp(0.15F, currentRoll, targetRoll);
                    flightRolls.put(player.getUuid(), currentRoll);
                    
                    matrixStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(currentRoll));
                } else if (flightRolls.containsKey(player.getUuid())) {
                    float currentRoll = flightRolls.get(player.getUuid());
                    currentRoll = net.minecraft.util.math.MathHelper.lerp(0.15F, currentRoll, 0.0F);
                    if (Math.abs(currentRoll) < 0.1F) {
                        flightRolls.remove(player.getUuid());
                    } else {
                        flightRolls.put(player.getUuid(), currentRoll);
                        matrixStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(currentRoll));
                    }
                }
            }

            double yOffset = -0.5;
            if (player.hasVehicle()) {
                net.minecraft.entity.Entity vehicle = player.getVehicle();
                if (vehicle instanceof net.minecraft.entity.passive.PigEntity) {
                    yOffset -= 0.08;
                } else if (vehicle instanceof net.minecraft.entity.passive.AbstractHorseEntity) {
                    yOffset -= 0.06;
                } else if (vehicle instanceof net.minecraft.entity.vehicle.BoatEntity || vehicle instanceof net.minecraft.entity.vehicle.AbstractMinecartEntity) {
                    yOffset += 0.4;
                }
            }

            matrixStack.translate(-0.5, yOffset, -0.5);

            RenderLayer renderLayer = this.ponyRenderer.getRenderType(this.ponyAnimatable,
                    this.ponyRenderer.getTextureLocation(this.ponyAnimatable), vertexConsumerProvider, g);
            VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(renderLayer);
            this.ponyRenderer.render(matrixStack, this.ponyAnimatable, vertexConsumerProvider, renderLayer,
                    vertexConsumer, i);

            this.renderMagicHeldItem(player, matrixStack, vertexConsumerProvider, i, g);

            matrixStack.pop();
            ci.cancel();
        }
    }

    @Unique
    private final top.csituka.magicaland.client.render.GlowingItemRenderer magicItemRenderer = new top.csituka.magicaland.client.render.GlowingItemRenderer();

    @Unique
    private void renderMagicHeldItem(AbstractClientPlayerEntity player, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float tickDelta) {
        net.minecraft.item.ItemStack mainHandStack = player.getMainHandStack();
        net.minecraft.item.ItemStack offHandStack = player.getOffHandStack();
        
        if (mainHandStack.isEmpty() && offHandStack.isEmpty()) return;

        float limbPos = 0.0F;
        float limbSpeed = 0.0F;
        if (player.isAlive()) {
            limbPos = player.limbAnimator.getPos(tickDelta);
            limbSpeed = player.limbAnimator.getSpeed(tickDelta);
        }
        
        float swingProgress = player.getHandSwingProgress(tickDelta);
        net.minecraft.util.Arm mainArm = player.getMainArm();

        boolean isSneaking = player.isSneaking();
        float pitch = player.getPitch();
        
        if (!mainHandStack.isEmpty()) {
            boolean isRightArm = mainArm == net.minecraft.util.Arm.RIGHT;
            renderHandItem(player, mainHandStack, matrices, vertexConsumers, light, tickDelta, true, isRightArm, isSneaking, limbPos, limbSpeed, swingProgress, pitch);
        }
        
        if (!offHandStack.isEmpty()) {
            boolean isRightArm = mainArm == net.minecraft.util.Arm.LEFT;
            renderHandItem(player, offHandStack, matrices, vertexConsumers, light, tickDelta, false, isRightArm, isSneaking, limbPos, limbSpeed, swingProgress, pitch);
        }
    }

    @Unique
    private void renderHandItem(AbstractClientPlayerEntity player, net.minecraft.item.ItemStack stack, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float tickDelta, boolean isMainHand, boolean isRightArm, boolean isSneaking, float limbPos, float limbSpeed, float swingProgress, float pitch) {
        matrices.push();
        
        if (isSneaking) {
            matrices.translate(0.0, -0.2, 0.0);
            matrices.translate(0.5, 1.0, 0.5);
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(28.6F));
            matrices.translate(-0.5, -1.0, -0.5);
        }

        float pivotX = isRightArm ? 1.0F : 0.0F;
        float pivotY = 1.4F;
        float pivotZ = 0.0F;
        matrices.translate(pivotX, pivotY, pivotZ);
        
        float armPitch = 0.0F;
        float armYaw = 0.0F;
        float armRoll = 0.0F;
        
        if (player.hasVehicle()) {
            armPitch = -0.62F;
        }

        armPitch += pitch * ((float)Math.PI / 180F) * 0.1F;

        if (swingProgress > 0.0F) {
            net.minecraft.util.Hand activeHand = player.preferredHand; 
            boolean isSwingingArm = (activeHand == net.minecraft.util.Hand.MAIN_HAND && isMainHand) || (activeHand == net.minecraft.util.Hand.OFF_HAND && !isMainHand);
            if (activeHand == null) {
                isSwingingArm = isMainHand;
            }

            if (isSwingingArm) {
                float swing1 = net.minecraft.util.math.MathHelper.sin(swingProgress * (float)Math.PI);
                float swing2 = net.minecraft.util.math.MathHelper.sin(net.minecraft.util.math.MathHelper.sqrt(swingProgress) * (float)Math.PI);
                armPitch -= swing2 * 1.2F + swing1 * 0.4F;
                armYaw += isRightArm ? swing2 * 0.4F : -swing2 * 0.4F;
                armRoll += isRightArm ? swing1 * 0.2F : -swing1 * 0.2F;
            }
        }

        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotation(armRoll));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotation(armYaw));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotation(armPitch));
        
        float offsetX = isRightArm ? 0.2F : -0.2F;
        float offsetY = 0.0F;
        float offsetZ = -0.4F;
        matrices.translate(offsetX, offsetY, offsetZ);

        float time = player.age + tickDelta;
        matrices.translate(0.0, net.minecraft.util.math.MathHelper.sin(time * 0.1F) * 0.05F, 0.0);

        boolean isTridentUsing = stack.isOf(net.minecraft.item.Items.TRIDENT) && player.isUsingItem() && player.getActiveItem() == stack;
        if (isTridentUsing) {
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
        }

        int glowColor = 0x8844AAFF;
        net.minecraft.client.render.item.ItemRenderer itemRenderer = net.minecraft.client.MinecraftClient.getInstance().getItemRenderer();
        
        net.minecraft.client.render.model.json.ModelTransformationMode mode = isRightArm ? 
            net.minecraft.client.render.model.json.ModelTransformationMode.THIRD_PERSON_RIGHT_HAND : 
            net.minecraft.client.render.model.json.ModelTransformationMode.THIRD_PERSON_LEFT_HAND;

        magicItemRenderer.renderItemWithGlow(
            itemRenderer, player, stack, 
            mode, 
            !isRightArm, matrices, vertexConsumers, player.getWorld(), 
            light, net.minecraft.client.render.OverlayTexture.DEFAULT_UV, glowColor, true
        );
        
        matrices.pop();
    }

    @Inject(method = "renderRightArm", at = @At("HEAD"), cancellable = true)
    private void onRenderRightArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
            AbstractClientPlayerEntity player, CallbackInfo ci) {
        if (Config.getInstance().replacePlayerModel) {
            ci.cancel();
        }
    }

    @Inject(method = "renderLeftArm", at = @At("HEAD"), cancellable = true)
    private void onRenderLeftArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
            AbstractClientPlayerEntity player, CallbackInfo ci) {
        if (Config.getInstance().replacePlayerModel) {
            ci.cancel();
        }
    }
}
