package top.csituka.magicaland.client.gui.ponycustom;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Vector3f;

public final class PreviewLightingRig {
    private PreviewLightingRig() {}

    public static void apply() {
        // 屏幕坐标 Y 向下，+Z 朝镜头；主光位于镜头左上后侧 45 度。
        RenderSystem.setShaderLights(new Vector3f(-.5f, -.5f, .7071068f),
                new Vector3f(.35f, -.20f, .9151503f));
    }
}
