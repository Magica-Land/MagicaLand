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
import top.csituka.magicaland.client.config.style.PonyStylePart;
import top.csituka.magicaland.client.model.GeckoPlayerAnimatable;
import top.csituka.magicaland.client.model.GeckoPlayerModel;

public class PonyRenderer extends GeoObjectRenderer<GeckoPlayerAnimatable> {

    private static final Identifier PONY_BASE = new Identifier("magicaland", "textures/entity/base.png");
    private static final Identifier PONY_TS = new Identifier("magicaland", "textures/entity/mane.png");

    private ModelConfig overrideConfig = null;
    private boolean usingPalette;

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

        ModelConfig config = getEffectiveConfig();
        if (!PonyFacePose.shouldRender(bone.getName(), config == null ? "01" : config.eyeStyle))
            return;

        try (PonyFacePose ignored = "Emotions".equals(bone.getName())
                ? PonyFacePose.apply(bone) : null) {
            String name = bone.getName().toLowerCase();
            // 翅膀使用身体图集，只有鬃毛和尾巴使用第二张贴图。
            boolean isOther = name.contains("mane") || name.contains("tail");
            Identifier texture = isOther ? PONY_TS : PONY_BASE;
            Identifier palette = isOther ? ManeTintTextures.get(config, bone.getName())
                    : BodyTintTextures.get(config, BodyTintTextures.colorForBone(config, bone.getName()));
            if (palette != null) texture = palette;

            RenderLayer newRenderType = this.getRenderType(animatable, texture, bufferSource, partialTick);
            VertexConsumer newBuffer = bufferSource.getBuffer(newRenderType);
            boolean previousPalette = usingPalette;
            usingPalette = palette != null;
            try {
                super.renderRecursively(poseStack, animatable, bone, newRenderType, bufferSource, newBuffer, isReRender,
                        partialTick, packedLight, packedOverlay, red, green, blue, alpha);
            } finally {
                usingPalette = previousPalette;
            }
        }
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

        if (!config.showHorn && bone.getName().equalsIgnoreCase("Horn"))
            return;
        if (!config.showWings && bone.getName().toLowerCase().contains("wing"))
            return;

        String boneName = bone.getName();
        String colorField = BodyTintTextures.colorForBone(config, boneName);
        if (colorField == null) colorField = ManePalette.colorForBone(config, boneName);

        if (colorField != null && !usingPalette) {
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
        ModelConfig config = getEffectiveConfig();
        if (config == null)
            return true;

        if (boneName.toLowerCase().contains("tail")) {
            if (boneName.equalsIgnoreCase("Tail"))
                return true;
            return boneName.startsWith(bonePrefix(config.tailStyle, PonyStylePart.TAIL));
        }

        if (!boneName.toLowerCase().contains("mane"))
            return true;

        if (boneName.equals("Mane") || boneName.equals("FrontMane") || boneName.equals("BackMane"))
            return true;

        // 前发、后发独立选择；AJ 前发已使用自己的 Style05 骨骼。
        if (boneName.startsWith(bonePrefix(config.frontManeStyle, PonyStylePart.FRONT_MANE)))
            return true;
        return boneName.startsWith(bonePrefix(config.backManeStyle, PonyStylePart.BACK_MANE));
    }

    /** Builds the "Style{id}{PartSuffix}" bone-name prefix, e.g. "Style02FrontMane". */
    private static String bonePrefix(String styleId, PonyStylePart part) {
        return "Style" + styleId + part.boneSuffix;
    }

}
