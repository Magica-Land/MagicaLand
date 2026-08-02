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
 * 包含魔法悬浮效果：延迟跟随、惯性摆动、轻度漂浮、走路摆动
 */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Unique
    private final GlowingItem magicItemRenderer = new GlowingItem();

    // 魔法悬浮效果的状态变量
    @Unique
    private float magicalOffsetX = 0.0f;  // X轴偏移（左右摆动）
    @Unique
    private float magicalOffsetY = 0.0f;  // Y轴偏移（上下漂浮+走路上下颠簸）
    @Unique
    private float magicalOffsetZ = 0.0f;  // Z轴偏移（前后摆动）

    // 追踪上一帧的视角旋转，用于计算旋转速度
    @Unique
    private float lastYaw = 0.0f;
    @Unique
    private float lastPitch = 0.0f;

    // 目标位置（延迟跟随的目标）
    @Unique
    private float targetOffsetX = 0.0f;
    @Unique
    private float targetOffsetY = 0.0f;
    @Unique
    private float targetOffsetZ = 0.0f;

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
                    matrices.translate(xOffset + magicalOffsetX, 0.2 + magicalOffsetY, -0.4 + magicalOffsetZ);
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
     * 更新魔法悬浮效果
     * 包含：延迟跟随 + 惯性摆动 + 轻微漂浮 + 走路颠簸
     */
    @Unique
    private void updateMagicalEffect(LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // 获取当前视角旋转
        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();

        // 计算视角旋转速度（也就是鼠标转动速度）
        float yawDelta = currentYaw - lastYaw;
        float pitchDelta = currentPitch - lastPitch;

        // 处理角度跨越 -180/180 的情况
        if (yawDelta > 180) yawDelta -= 360;
        if (yawDelta < -180) yawDelta += 360;

        // 更新上一帧的旋转值
        lastYaw = currentYaw;
        lastPitch = currentPitch;

        // ==================== 计算移动速度（用于延迟跟随） ====================
        // 获取玩家的移动输入
        float moveForward = 0;
        float moveSide = 0;
        if (client.options.forwardKey.isPressed()) moveForward = 1;
        if (client.options.backKey.isPressed()) moveForward = -1;
        if (client.options.leftKey.isPressed()) moveSide = 1;
        if (client.options.rightKey.isPressed()) moveSide = -1;

        // 根据玩家朝向转换移动方向
        float playerYaw = client.player.getYaw();
        float rad = (float) Math.toRadians(playerYaw);
        float sin = (float) Math.sin(rad);
        float cos = (float) Math.cos(rad);

        // 计算世界坐标系中的移动偏移
        float moveOffsetX = moveSide * cos - moveForward * sin;
        float moveOffsetZ = moveSide * sin + moveForward * cos;

        // ==================== 延迟跟随计算 - 用于制造延迟效果 ====================
        // 这里计算物品应该偏移到的"目标位置"
        // 数值越大，物品偏离原位的幅度越大（延迟距离）
        float maxOffset = 0.35f;  // 最大偏移（延迟距离）

        // 视角转动引起的偏移（X是左右，Z是上下/前后）
        // 乘数越大，转视角时物品摆动幅度越大
        targetOffsetX = -yawDelta * 5.0f + moveOffsetX * 0.05f;  // 移动偏移更小
        targetOffsetZ = -pitchDelta * 4.0f + moveOffsetZ * 0.05f;

        // 限制最大偏移
        targetOffsetX = Math.max(-maxOffset, Math.min(maxOffset, targetOffsetX));
        targetOffsetZ = Math.max(-maxOffset, Math.min(maxOffset, targetOffsetZ));

        // ==================== 惯性延迟效果 - 制造延迟的核心 ====================
        // 使用 lerp (线性插值) 来实现延迟效果
        // lerpFactor 越小，物品跟随目标位置的速度越慢，延迟越大
        // 典型值：0.001-0.02，越小延迟越大
        float viewLerpFactor = 0.006f;  // 延迟系数
        magicalOffsetX = magicalOffsetX + (targetOffsetX - magicalOffsetX) * viewLerpFactor;
        magicalOffsetZ = magicalOffsetZ + (targetOffsetZ - magicalOffsetZ) * viewLerpFactor;

        // ==================== 轻微漂浮效果（正弦波） ====================
        float floatSpeed = 0.12f;
        float floatAmplitude = 0.05f;
        magicalOffsetY = (float) Math.sin(entity.age * floatSpeed) * floatAmplitude;
    }
}