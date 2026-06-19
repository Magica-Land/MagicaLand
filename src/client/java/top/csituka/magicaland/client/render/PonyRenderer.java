package top.csituka.magicaland.client.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.model.GeckoPlayerAnimatable;
import top.csituka.magicaland.client.model.GeckoPlayerModel;

public class PonyRenderer extends GeoObjectRenderer<GeckoPlayerAnimatable> {

    private static final Identifier PONY_BASE = new Identifier("magicaland", "textures/entity/base.png");
    private static final Identifier PONY_TS = new Identifier("magicaland", "textures/entity/mane.png");

    public PonyRenderer() {
        super(new GeckoPlayerModel());
    }

    public PonyRenderer(GeckoPlayerModel model) {
        super(model);
    }

    @Override
    public void renderRecursively(MatrixStack poseStack, GeckoPlayerAnimatable animatable,
                                   GeoBone bone, RenderLayer renderType,
                                   VertexConsumerProvider bufferSource, VertexConsumer buffer,
                                   boolean isReRender, float partialTick,
                                   int packedLight, int packedOverlay,
                                   float red, float green, float blue, float alpha) {
        String name = bone.getName().toLowerCase();
        boolean isOther = name.contains("mane") || name.contains("tail") || name.contains("wing");
        Identifier texture = isOther ? PONY_TS : PONY_BASE;

        RenderLayer newRenderType = this.getRenderType(animatable, texture, bufferSource, partialTick);
        VertexConsumer newBuffer = bufferSource.getBuffer(newRenderType);

        super.renderRecursively(poseStack, animatable, bone, newRenderType, bufferSource, newBuffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void renderCubesOfBone(MatrixStack poseStack, GeoBone bone,
                                   VertexConsumer buffer, int packedLight, int packedOverlay,
                                   float red, float green, float blue, float alpha) {
        ModelConfig config = ModelManager.getActiveModel();
        if (config == null) {
            super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }

        if (!shouldRenderSelectedMane(bone.getName())) return;
        if (!shouldRenderSelectedEye(bone.getName())) return;

        if (!config.showHorn && bone.getName().equalsIgnoreCase("Horn")) return;

        if (config.showHorn && bone.getName().equalsIgnoreCase("Horn")) {
            int color = parseHexColor(config.hornColor);
            float cr = ((color >> 16) & 0xFF) / 255.0f;
            float cg = ((color >> 8) & 0xFF) / 255.0f;
            float cb = (color & 0xFF) / 255.0f;
            red *= cr;
            green *= cg;
            blue *= cb;
        }

        String boneName = bone.getName();
        String colorField = null;
        if (boneName.equalsIgnoreCase("Body")) {
            colorField = config.bodyColor;
        } else if (boneName.equalsIgnoreCase("Neck")) {
            colorField = config.neckColor;
        } else if (boneName.equalsIgnoreCase("Head")) {
            colorField = config.headColor;
        } else if (boneName.equalsIgnoreCase("Nose")) {
            colorField = config.noseColor;
        } else if (boneName.equalsIgnoreCase("LeftEar")) {
            colorField = config.leftEarColor;
        } else if (boneName.equalsIgnoreCase("RightEar")) {
            colorField = config.rightEarColor;
        } else if (boneName.startsWith("LFront") || boneName.equalsIgnoreCase("LForeLeg")) {
            colorField = config.leftFrontLimbColor;
        } else if (boneName.startsWith("RFront") || boneName.equalsIgnoreCase("RForeLeg")) {
            colorField = config.rightFrontLimbColor;
        } else if (boneName.startsWith("LHind")) {
            colorField = config.leftHindLimbColor;
        } else if (boneName.startsWith("RHind")) {
            colorField = config.rightHindLimbColor;
        }

        if (colorField != null) {
            int color = parseHexColor(colorField);
            float cr = ((color >> 16) & 0xFF) / 255.0f;
            float cg = ((color >> 8) & 0xFF) / 255.0f;
            float cb = (color & 0xFF) / 255.0f;
            red *= cr;
            green *= cg;
            blue *= cb;
        }

        super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    protected int parseHexColor(String hex) {
        try {
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            return (int) Long.parseLong(hex, 16);
        } catch (Exception e) {
            return 0xFFFFFFFF;
        }
    }

    private boolean shouldRenderSelectedMane(String boneName) {
        ModelConfig config = ModelManager.getActiveModel();
        if (config == null) return true;

        if (boneName.equals("Bun")) return false;

        if (boneName.toLowerCase().contains("tail")) {
            if (boneName.equalsIgnoreCase("Tail")) return true;
            return boneName.startsWith(config.tailStyle + "Tail");
        }

        if (!boneName.toLowerCase().contains("mane")) return true;

        if (boneName.equals("Mane") || boneName.equals("FrontMane") || boneName.equals("BackMane")) return true;

        String frontStyle = config.frontManeStyle;
        String backStyle = config.backManeStyle;

        if (boneName.startsWith(frontStyle + "FrontMane")) return true;
        return boneName.startsWith(backStyle + "BackMane");
    }

    private boolean shouldRenderSelectedEye(String boneName) {
        ModelConfig config = ModelManager.getActiveModel();
        if (config == null) return true;
        String eyeStyle = config.eyeStyle;

        boolean isEyeBone = boneName.equals("CommonFace") || boneName.equals("leye") || boneName.equals("reye")
                || boneName.equals("FSCommonFace") || boneName.equals("leye2") || boneName.equals("reye2")
                || boneName.equals("RRCommonFace") || boneName.equals("leye3") || boneName.equals("reye3");

        if (!isEyeBone) return true;

        return switch (eyeStyle) {
            case "FS" -> boneName.equals("FSCommonFace") || boneName.equals("leye2") || boneName.equals("reye2");
            case "RR" -> boneName.equals("RRCommonFace") || boneName.equals("leye3") || boneName.equals("reye3");
            default -> boneName.equals("CommonFace") || boneName.equals("leye") || boneName.equals("reye");
        };
    }
}
