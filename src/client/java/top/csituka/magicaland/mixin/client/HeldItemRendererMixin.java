package top.csituka.magicaland.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
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
import top.csituka.magicaland.client.render.GlowingItemRenderer;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Unique
    private final GlowingItemRenderer magicItemRenderer = new GlowingItemRenderer();

    @Redirect(
        method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/ItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V"
        )
    )
    private void redirectRenderItem(ItemRenderer instance, LivingEntity entity, ItemStack item, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, World world, int light, int overlay, int seed) {
        boolean isThirdPerson = renderMode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND || renderMode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
        boolean isTridentUsing = isThirdPerson && item.isOf(Items.TRIDENT) && entity.isUsingItem() && entity.getActiveItem() == item;

        matrices.push();

        if (isTridentUsing && entity instanceof AbstractClientPlayerEntity && Config.getInstance().replacePlayerModel) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
        }

        if (entity instanceof AbstractClientPlayerEntity && Config.getInstance().replacePlayerModel) {
            boolean isFirstPerson = renderMode.isFirstPerson();
            
            if (isFirstPerson && !Config.getInstance().firstPersonMagicGlow) {
                instance.renderItem(entity, item, renderMode, leftHanded, matrices, vertexConsumers, world, light, overlay, seed);
            } else {
                matrices.push();
                if (isFirstPerson) {
                    double xOffset = leftHanded ? -0.15 : 0.15;
                    matrices.translate(xOffset, 0.2, -0.4);
                }
                magicItemRenderer.renderItemWithGlow(
                    instance, entity, item, renderMode, leftHanded, matrices, vertexConsumers, world, light, seed, 0x8844AAFF, true
                );
                matrices.pop();
            }
        } else {
            instance.renderItem(entity, item, renderMode, leftHanded, matrices, vertexConsumers, world, light, overlay, seed);
        }

        matrices.pop();
    }
}
