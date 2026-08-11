package top.csituka.magicaland.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
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
import top.csituka.magicaland.client.config.Config;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.render.GlowingItem;

/**
 * 第一人称手持物品渲染
 * 包含魔法悬浮效果：轻度上下漂浮
 * （之前还做过视角转动时的滞后跟随，试下来手感不对，先去掉，只保留漂浮）
 */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Unique
    private final GlowingItem magicItemRenderer = new GlowingItem();

    // Y轴偏移（上下漂浮）
    @Unique
    private float magicalOffsetY = 0.0f;

    // 上一次更新的真实时间（纳秒），用于计算 dt，做到帧率无关
    @Unique
    private long lastUpdateNanos = 0L;

    // 上下漂浮用的真实时间累加器（避免用 entity.age 导致高帧率下的阶梯感）
    @Unique
    private float floatTime = 0.0f;

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
        ModelConfig modelConfig = ModelManager.getActiveModel();
        boolean enableHornEffect = modelConfig == null || modelConfig.showHorn;

        if (entity instanceof AbstractClientPlayerEntity && Config.getInstance().replacePlayerModel && enableHornEffect) {
            boolean isFirstPerson = renderMode.isFirstPerson();

            if (isFirstPerson && !Config.getInstance().firstPersonMagicGlow) {
                instance.renderItem(entity, item, renderMode, leftHanded, matrices, vertexConsumers, world, light,
                        overlay, seed);
            } else {
                matrices.push();
                if (isFirstPerson) {
                    // 计算魔法悬浮效果
                    this.updateMagicalEffect(entity);

                    double xOffset = leftHanded ? -0.15 : 0.15;
                    // 应用魔法悬浮偏移
                    matrices.translate(xOffset, 0.2 + magicalOffsetY, -0.4);
                }
                magicItemRenderer.renderItemWithGlow(
                        instance, entity, item, renderMode, leftHanded, matrices, vertexConsumers, world, light, seed,
                        GlowingItem.getCurrentGlowColor(), true);
                matrices.pop();
            }
        } else {
            instance.renderItem(entity, item, renderMode, leftHanded, matrices, vertexConsumers, world, light, overlay,
                    seed);
        }

        matrices.pop();
    }

    /**
     * 更新魔法悬浮效果：只做轻微上下漂浮（正弦波），按真实时间推进，
     * 避免用 entity.age（整数 tick）导致高帧率下的阶梯感。
     */
    @Unique
    private void updateMagicalEffect(LivingEntity entity) {
        long now = System.nanoTime();
        if (lastUpdateNanos == 0L) {
            lastUpdateNanos = now;
            return;
        }

        // clamp 防止切后台/卡顿恢复后 dt 过大导致跳变
        float dt = Math.min(0.05f, (now - lastUpdateNanos) / 1_000_000_000.0f);
        lastUpdateNanos = now;

        floatTime += dt;
        float floatSpeed = 2.4f;
        float floatAmplitude = 0.05f;
        magicalOffsetY = (float) Math.sin(floatTime * floatSpeed) * floatAmplitude;
    }
}