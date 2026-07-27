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

    private ModelConfig overrideConfig = null;

    public PonyRenderer() {
        super(new GeckoPlayerModel());
    }

    public PonyRenderer(GeckoPlayerModel model) {
        super(model);
    }

    public void setOverrideConfig(ModelConfig config) {
        this.overrideConfig = config;
    }

    public void clearOverride() {
        this.overrideConfig = null;
    }

    private ModelConfig getEffectiveConfig() {
        if (overrideConfig != null) {
            return overrideConfig;
        }
        return ModelManager.getActiveModel();
    }

    @Override
    public void renderRecursively(MatrixStack poseStack, GeckoPlayerAnimatable animatable,
            GeoBone bone, RenderLayer renderType,
            VertexConsumerProvider bufferSource, VertexConsumer buffer,
            boolean isReRender, float partialTick,
            int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {

        if (animatable.getPlayer() != null) {
            boolean sleepingOrSneaking = animatable.getPlayer().isSleeping()
                    || animatable.getPlayer().isSneaking();
            if (sleepingOrSneaking) {
                String boneName = bone.getName();
                if (boneName.equals("close")) {
                    bone.setScaleX(1);
                    bone.setScaleY(1);
                    bone.setScaleZ(1);
                } else if (boneName.equals("emot")
                        || boneName.equals("CommonFace") || boneName.equals("leye") || boneName.equals("reye")
                        || boneName.equals("FSCommonFace") || boneName.equals("leye2") || boneName.equals("reye2")
                        || boneName.equals("RRCommonFace") || boneName.equals("leye3") || boneName.equals("reye3")) {
                    return;
                }
            }
        }

        String name = bone.getName().toLowerCase();
        // The wing UVs are part of the base skin atlas.  Rendering them with
        // mane.png makes the wing faces sample transparent/incorrect pixels,
        // so only mane and tail bones use the secondary texture.
        boolean isOther = name.contains("mane") || name.contains("tail");
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
        ModelConfig config = getEffectiveConfig();
        if (config == null) {
            super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }

        if (!shouldRenderSelectedMane(bone.getName()))
            return;
        if (!shouldRenderSelectedEye(bone.getName()))
            return;

        if (!config.showHorn && bone.getName().equalsIgnoreCase("Horn"))
            return;
        if (!config.showWings && bone.getName().toLowerCase().contains("wing"))
            return;

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

        String boneLC = boneName.toLowerCase();
        if (colorField == null && (boneLC.contains("mane") || boneLC.contains("tail") || boneLC.contains("wing"))) {
            String maneColorField = null;
            if (boneLC.contains("tail")) {
                maneColorField = config.tailColor;
            } else if (boneLC.contains("wing")) {
                maneColorField = config.wingColor;
            } else if (boneLC.contains("mane")) {
                String frontStyle = config.frontManeStyle;
                boolean isFrontMane = boneName.equals("Mane") || boneName.equals("FrontMane")
                        || boneName.startsWith(frontStyle + "FrontMane")
                        || (boneName.startsWith("RD/AJFrontMane")
                                && (frontStyle.equals("RD") || frontStyle.equals("AJ")))
                        || (boneLC.contains("frontmane") && !boneName.startsWith(config.backManeStyle + "BackMane"));
                maneColorField = isFrontMane ? config.frontManeColor : config.backManeColor;
            }
            if (maneColorField != null) {
                int color = parseHexColor(maneColorField);
                red *= ((color >> 16) & 0xFF) / 255.0f;
                green *= ((color >> 8) & 0xFF) / 255.0f;
                blue *= (color & 0xFF) / 255.0f;
            }
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
        ModelConfig config = getEffectiveConfig();
        if (config == null)
            return true;

        if (boneName.equals("Bun"))
            return false;

        if (boneName.toLowerCase().contains("tail")) {
            if (boneName.equalsIgnoreCase("Tail"))
                return true;
            return boneName.startsWith(config.tailStyle + "Tail");
        }

        if (!boneName.toLowerCase().contains("mane"))
            return true;

        if (boneName.equals("Mane") || boneName.equals("FrontMane") || boneName.equals("BackMane"))
            return true;

        String frontStyle = config.frontManeStyle;
        String backStyle = config.backManeStyle;

        // RD and AJ share the combined "RD/AJFrontMane" bone in the geo
        if ((frontStyle.equals("RD") || frontStyle.equals("AJ")) && boneName.startsWith("RD/AJFrontMane"))
            return true;

        if (boneName.startsWith(frontStyle + "FrontMane"))
            return true;
        return boneName.startsWith(backStyle + "BackMane");
    }

    private boolean shouldRenderSelectedEye(String boneName) {
        ModelConfig config = getEffectiveConfig();
        if (config == null)
            return true;
        String eyeStyle = config.eyeStyle;

        boolean isEyeBone = boneName.equals("CommonFace") || boneName.equals("leye") || boneName.equals("reye")
                || boneName.equals("FSCommonFace") || boneName.equals("leye2") || boneName.equals("reye2")
                || boneName.equals("RRCommonFace") || boneName.equals("leye3") || boneName.equals("reye3");

        if (!isEyeBone)
            return true;

        return switch (eyeStyle) {
            case "FS" -> boneName.equals("FSCommonFace") || boneName.equals("leye2") || boneName.equals("reye2");
            case "RR" -> boneName.equals("RRCommonFace") || boneName.equals("leye3") || boneName.equals("reye3");
            default -> boneName.equals("CommonFace") || boneName.equals("leye") || boneName.equals("reye");
        };
    }
}
