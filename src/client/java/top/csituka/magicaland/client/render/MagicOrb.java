package top.csituka.magicaland.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector3f;

public final class MagicOrb {
    private static final float RADIUS=.075f;
    private static final float[] SOFT_OPACITY={.075f,.067f,.059f,.052f,.045f,.038f,.032f,.027f,.022f,.017f,.012f,.007f};
    private MagicOrb() {}
    public static void render(MatrixStack matrices,int color,double ticks,int seed) {
        var sprite=MinecraftClient.getInstance().getBlockRenderManager().getModels().getModelParticleSprite(Blocks.WHITE_CONCRETE.getDefaultState());
        float u=(sprite.getMinU()+sprite.getMaxU())*.5f,v=(sprite.getMinV()+sprite.getMaxV())*.5f;
        List<ItemAuraGeometry.Vertex> vertices=new ArrayList<>();
        float[][] normals={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        float[][][] faces={
                {{1,-1,-1},{1,1,-1},{1,1,1},{1,-1,1}},
                {{-1,-1,1},{-1,1,1},{-1,1,-1},{-1,-1,-1}},
                {{-1,1,-1},{-1,1,1},{1,1,1},{1,1,-1}},
                {{-1,-1,1},{-1,-1,-1},{1,-1,-1},{1,-1,1}},
                {{1,-1,1},{1,1,1},{-1,1,1},{-1,-1,1}},
                {{-1,-1,-1},{-1,1,-1},{1,1,-1},{1,-1,-1}}};
        for (int face=0;face<faces.length;face++) for (float[] corner : faces[face]) {
            float[] normal=normals[face];
            vertices.add(new ItemAuraGeometry.Vertex(corner[0]*RADIUS,corner[1]*RADIUS,corner[2]*RADIUS,
                    u,v,normal[0],normal[1],normal[2],1));
        }
        var batch=new ItemAuraGeometry.Batch(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE,vertices);
        var pose=matrices.peek();
        var mesh=new ItemAuraGeometry.Mesh(pose.getPositionMatrix(),pose.getNormalMatrix(),List.of(batch),
                new Vector3f(-RADIUS),new Vector3f(RADIUS));
        int clock=(int)Math.round((ticks%240+240)%240*50);
        HornAuraPass.submit(batch.texture(),buffer -> {
            mesh.render(batch,buffer,color,clock,SOFT_OPACITY,4.5f); mesh.stars(buffer,color,ticks,seed,clock);
        });
    }
}
