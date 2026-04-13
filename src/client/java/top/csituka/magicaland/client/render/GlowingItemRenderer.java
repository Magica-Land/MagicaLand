package top.csituka.magicaland.client.render;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.render.*;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.world.World;

import top.csituka.magicaland.client.util.RenderLayerUtil;

public class GlowingItemRenderer {

    private VertexConsumerProvider createGlowProvider(
            int glowColor, 
            VertexConsumerProvider originalContext) {
        return layer -> {
            if (layer.getVertexFormat() != VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL) {
                return originalContext.getBuffer(layer);
            }
            return originalContext.getBuffer(
                MagicGlow.getColoured(
                    RenderLayerUtil.getTexture(layer)
                        .orElse(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE), 
                    glowColor
                )
            );
        };
    }

    public void renderItemWithGlow(
            ItemRenderer itemRenderer,
            @Nullable LivingEntity entity,
            ItemStack stack,
            ModelTransformationMode mode,
            boolean left,
            MatrixStack matrices,
            VertexConsumerProvider renderContext,
            @Nullable World world,
            int lightUv,
            int seed,
            int glowColor,
            boolean renderGlow) {
        
        boolean shouldRenderGlow = renderGlow && (
            mode.isFirstPerson()
            || mode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND
            || mode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND
        );

        if (shouldRenderGlow) {
            int fixedGlowColor = 0xAA00FF;
            matrices.push();

            itemRenderer.renderItem(
                entity, stack, mode, left, 
                matrices, renderContext, world, 
                lightUv, OverlayTexture.DEFAULT_UV, seed
            );

            VertexConsumerProvider glowContext = createGlowProvider(fixedGlowColor, renderContext);

            matrices.scale(1.1F, 1.1F, 1.1F);
            matrices.translate(0.015F, 0.01F, 0.01F);
            itemRenderer.renderItem(
                entity, stack, mode, left, 
                matrices, glowContext, world, 
                lightUv, OverlayTexture.DEFAULT_UV, seed
            );

            matrices.translate(-0.03F, -0.02F, -0.02F);
            itemRenderer.renderItem(
                entity, stack, mode, left, 
                matrices, glowContext, world, 
                lightUv, OverlayTexture.DEFAULT_UV, seed
            );

            matrices.pop();
        } else {
            itemRenderer.renderItem(
                entity, stack, mode, left, 
                matrices, renderContext, world, 
                lightUv, OverlayTexture.DEFAULT_UV, seed
            );
        }
    }
}
