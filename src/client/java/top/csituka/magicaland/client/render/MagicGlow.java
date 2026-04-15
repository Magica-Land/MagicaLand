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

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.google.common.base.Suppliers;

import java.util.function.BiFunction;

public abstract class MagicGlow extends RenderPhase {

    private MagicGlow() {
        super(null, null, null);
    }

    private static final java.util.function.Supplier<RenderLayer> MAGIC = Suppliers
            .memoize((com.google.common.base.Supplier<RenderLayer>) () -> {
                return RenderLayer.of(
                        "mod_magic_glow",
                        VertexFormats.POSITION_COLOR_LIGHT,
                        VertexFormat.DrawMode.QUADS,
                        256,
                        RenderLayer.MultiPhaseParameters.builder()
                                .program(EYES_PROGRAM)
                                .writeMaskState(COLOR_MASK)
                                .depthTest(LEQUAL_DEPTH_TEST)
                                .transparency(LIGHTNING_TRANSPARENCY)
                                .lightmap(DISABLE_LIGHTMAP)
                                .cull(DISABLE_CULLING)
                                .build(false));
            })::get;

    private static final BiFunction<Identifier, Integer, RenderLayer> TINTED_LAYER = Util.memoize((texture, color) -> {
        return RenderLayer.of(
                "mod_tint_layer",
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS,
                256,
                true,
                true,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(new Colored(texture, color))
                        .program(EYES_PROGRAM)
                        .writeMaskState(COLOR_MASK)
                        .depthTest(LEQUAL_DEPTH_TEST)
                        .transparency(LIGHTNING_TRANSPARENCY)
                        .lightmap(DISABLE_LIGHTMAP)
                        .cull(DISABLE_CULLING)
                        .build(true));
    });

    public static RenderLayer getRenderLayer() {
        return MAGIC.get();
    }

    public static RenderLayer getColoured(Identifier texture, int color) {
        return TINTED_LAYER.apply(texture, color);
    }

    private static class Colored extends Texture {
        private final float red;
        private final float green;
        private final float blue;
        private final float alpha;

        public Colored(Identifier texture, int color) {
            super(texture, false, false);
            this.red = ((color >> 16) & 0xFF) / 255.0F;
            this.green = ((color >> 8) & 0xFF) / 255.0F;
            this.blue = (color & 0xFF) / 255.0F;
            this.alpha = 0.8F;
        }

        @Override
        public void startDrawing() {
            RenderSystem.setShaderColor(red, green, blue, alpha);
            super.startDrawing();
        }

        @Override
        public void endDrawing() {
            super.endDrawing();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (!(other instanceof Colored otherColored))
                return false;
            return super.equals(other)
                    && otherColored.red == red
                    && otherColored.green == green
                    && otherColored.blue == blue
                    && otherColored.alpha == alpha;
        }
    }
}
