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

    private static final java.util.function.Supplier<RenderLayer> MAGIC = 
        Suppliers.memoize((com.google.common.base.Supplier<RenderLayer>) () -> {
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
                    .build(false)
            );
        })::get;

    private static final BiFunction<Identifier, Integer, RenderLayer> TINTED_LAYER = 
        Util.memoize((texture, color) -> {
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
                    .build(true)
            );
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
            if (this == other) return true;
            if (!(other instanceof Colored otherColored)) return false;
            return super.equals(other)
                    && otherColored.red == red
                    && otherColored.green == green
                    && otherColored.blue == blue
                    && otherColored.alpha == alpha;
        }
    }
}
