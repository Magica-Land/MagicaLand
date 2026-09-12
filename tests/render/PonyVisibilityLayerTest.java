package top.csituka.magicaland.client.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import sun.misc.Unsafe;

public final class PonyVisibilityLayerTest {
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.createGameVersion();
        net.minecraft.Bootstrap.initialize();
        var field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        var renderer = (PonyRenderer) ((Unsafe) field.get(null)).allocateInstance(PonyRenderer.class);
        int checks = 0;
        for (String part : new String[] {"body", "mane", "eye", "pupil"}) {
            Identifier texture = new Identifier("magicaland", "test/" + part);
            renderer.setBodyVisibility(PonyVisibility.TRANSLUCENT);
            if (renderer.getRenderType(null, texture, null, 0) != RenderLayer.getItemEntityTranslucentCull(texture))
                throw new AssertionError("translucent material " + part);
            if (Math.abs(renderer.getRenderColor(null, 0, 0).getAlphaFloat() - .15f) > 1f / 255)
                throw new AssertionError("translucent alpha " + part);
            renderer.setBodyVisibility(PonyVisibility.OUTLINE);
            if (renderer.getRenderType(null, texture, null, 0) != RenderLayer.getOutline(texture))
                throw new AssertionError("outline material " + part);
            if (renderer.getRenderColor(null, 0, 0).getAlphaFloat() != 1)
                throw new AssertionError("outline alpha " + part);
            renderer.setBodyVisibility(PonyVisibility.VISIBLE);
            if (renderer.getRenderColor(null, 0, 0).getAlphaFloat() != 1)
                throw new AssertionError("visible alpha restored " + part);
            checks += 5;
        }
        System.out.println("PASS PonyVisibilityLayerTest: " + checks + " actual renderer material/alpha checks");
    }
}
