package top.csituka.magicaland.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.csituka.magicaland.client.config.Config;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.render.GlowingItem;
import top.csituka.magicaland.client.render.ItemLevitation;
import top.csituka.magicaland.client.render.LevitationTrail;

/**
 * 第一人称手持物品渲染
 * 原版物品动作上叠加受限魔法惯性；不改变实际物品使用。
 */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Unique
    private final GlowingItem magicItemRenderer = new GlowingItem();

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("HEAD"))
    private void magicaland$beginHandAura(float delta, MatrixStack matrices, VertexConsumerProvider.Immediate buffers,
            ClientPlayerEntity player, int light, CallbackInfo ci) {
        GlowingItem.beginFirstPersonPass();
    }

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("TAIL"))
    private void magicaland$endHandAura(float delta, MatrixStack matrices, VertexConsumerProvider.Immediate buffers,
            ClientPlayerEntity player, int light, CallbackInfo ci) {
        GlowingItem.endFirstPersonPass(buffers);
    }

    @Redirect(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V"))
    private void redirectRenderItem(ItemRenderer instance, LivingEntity entity, ItemStack item,
            ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, World world, int light, int overlay, int seed) {
        boolean isThirdPerson = renderMode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND
                || renderMode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
        boolean isTridentUsing = isThirdPerson && item.isOf(Items.TRIDENT) && entity.isUsingItem()
                && entity.getActiveItem() == item;

        matrices.push();

        if (isTridentUsing && entity instanceof AbstractClientPlayerEntity && Config.getInstance().replacePlayerModel) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
        }

        // 获取当前模型配置，检查 showHorn 设置
        ModelConfig modelConfig = ModelManager.getAppliedModel();
        boolean enableHornEffect = modelConfig == null || modelConfig.showHorn;

        if (entity instanceof AbstractClientPlayerEntity && Config.getInstance().replacePlayerModel && enableHornEffect) {
            boolean isFirstPerson = renderMode.isFirstPerson();

            if (isFirstPerson && !Config.getInstance().firstPersonMagicGlow) {
                ItemLevitation.forget(entity, magicaland$isMainHand(entity, leftHanded), true);
                instance.renderItem(entity, item, renderMode, leftHanded, matrices, vertexConsumers, world, light,
                        overlay, seed);
            } else {
                matrices.push();
                LevitationTrail trail = LevitationTrail.EMPTY;
                if (isFirstPerson) {
                    double xOffset = leftHanded ? -0.15 : 0.15;
                    matrices.translate(xOffset, 0.2, -0.4);
                    trail = ItemLevitation.applyFirstPerson(entity, item, magicaland$isMainHand(entity, leftHanded),
                            leftHanded, matrices, MinecraftClient.getInstance().getTickDelta());
                }
                magicItemRenderer.renderItemWithGlow(
                        instance, entity, item, renderMode, leftHanded, matrices, vertexConsumers, world, light, seed,
                        GlowingItem.getCurrentGlowColor(), true, trail);
                matrices.pop();
            }
        } else {
            if (renderMode.isFirstPerson()) ItemLevitation.forget(entity, magicaland$isMainHand(entity, leftHanded), true);
            instance.renderItem(entity, item, renderMode, leftHanded, matrices, vertexConsumers, world, light, overlay,
                    seed);
        }

        matrices.pop();
    }

    @Unique
    private static boolean magicaland$isMainHand(LivingEntity entity, boolean left) {
        return left == (entity.getMainArm() == net.minecraft.util.Arm.LEFT);
    }
}
