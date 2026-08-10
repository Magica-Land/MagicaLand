/*
 * 此处代码实现参考了 MineLittlePony (https://github.com/MineLittlePony/MineLittlePony) 的实现方式
 * The code implementation here references the implementation approach of MineLittlePony (https://github.com/MineLittlePony/MineLittlePony).
 *
 * ---
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Mine Little Pony
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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

import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.util.RenderLayerHelper;

public class GlowingItem {

    /**
     * 从当前活跃模型中读取魔法光颜色
     */
    public static int getCurrentGlowColor() {
        return getGlowColor(ModelManager.getActiveModel());
    }

    public static int getGlowColor(ModelConfig config) {
        if (config == null) return 0xAA00FF;
        String hex = config.magicGlowColor;
        if (hex == null) return 0xAA00FF;
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() > 6) hex = hex.substring(hex.length() - 6);
        try {
            return Integer.parseInt(hex, 16);
        } catch (Exception e) {
            return 0xAA00FF;
        }
    }

    private VertexConsumerProvider createGlowProvider(
            int glowColor,
            VertexConsumerProvider originalContext) {
        return layer -> {
            if (layer.getVertexFormat() != VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL) {
                return originalContext.getBuffer(layer);
            }
            return originalContext.getBuffer(
                    MagicGlow.getColoured(
                            RenderLayerHelper.getTexture(layer)
                                    .orElse(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE),
                            glowColor));
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

        boolean shouldRenderGlow = renderGlow && (mode.isFirstPerson()
                || mode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND
                || mode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND);

        if (shouldRenderGlow) {
            matrices.push();

            itemRenderer.renderItem(
                    entity, stack, mode, left,
                    matrices, renderContext, world,
                    lightUv, OverlayTexture.DEFAULT_UV, seed);

            VertexConsumerProvider glowContext = createGlowProvider(glowColor, renderContext);

            matrices.scale(1.1F, 1.1F, 1.1F);
            matrices.translate(0.015F, 0.01F, 0.01F);
            itemRenderer.renderItem(
                    entity, stack, mode, left,
                    matrices, glowContext, world,
                    lightUv, OverlayTexture.DEFAULT_UV, seed);

            matrices.translate(-0.03F, -0.02F, -0.02F);
            itemRenderer.renderItem(
                    entity, stack, mode, left,
                    matrices, glowContext, world,
                    lightUv, OverlayTexture.DEFAULT_UV, seed);

            matrices.pop();
        } else {
            itemRenderer.renderItem(
                    entity, stack, mode, left,
                    matrices, renderContext, world,
                    lightUv, OverlayTexture.DEFAULT_UV, seed);
        }
    }
}
