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

            matrixStack.pop();
            ci.cancel();
        }
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
