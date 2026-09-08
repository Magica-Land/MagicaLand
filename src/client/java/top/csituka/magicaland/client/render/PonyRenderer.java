package top.csituka.magicaland.client.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.RenderUtils;
import top.csituka.magicaland.client.animation.ClientGaze;
import top.csituka.magicaland.client.config.Config;
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
    private Matrix4f gazeFrame;
    private float gazePartialTick;
    private final PonyGazeMath.Smoother gazeSmoother = new PonyGazeMath.Smoother();

    public void setGazeFrame(Matrix4f frame, float partialTick) {
        gazeFrame = frame == null ? null : new Matrix4f(frame);
        gazePartialTick = partialTick;
    }

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

        try (PonyFacePose face = "Emotions".equals(bone.getName())
                ? PonyFacePose.apply(bone) : null) {
            if (face != null) applyGaze(poseStack, bone, face, animatable, config == null ? "01" : config.eyeStyle);
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

    private void applyGaze(MatrixStack stack, GeoBone root, PonyFacePose face, GeckoPlayerAnimatable animatable, String style) {
        var player = animatable.getPlayer();
        if (gazeFrame == null || !Config.getInstance().automaticGaze || !animatable.allowsAutomaticGaze()
                || player == null || !player.isAlive() || player.isSleeping() || !face.allowsGaze()) {
            gazeSmoother.reset();
            return;
        }
        GeoBone left = face.pupil(style, true);
        GeoBone right = face.pupil(style, false);
        if (left == null || right == null || left.getParent() != right.getParent()) {
            gazeSmoother.reset();
            return;
        }
        PonyGazeMath.Offset desired = PonyGazeMath.Offset.ZERO;
        var target = ClientGaze.targetFor(player);
        if (target != null) {
            Vec3d relative = target.getLerpedPos(gazePartialTick).add(0, target.getEyeHeight(target.getPose()), 0)
                    .subtract(player.getLerpedPos(gazePartialTick));
            Vector3f targetInRender = gazeFrame.transformPosition(new Vector3f((float) relative.x, (float) relative.y, (float) relative.z));
            var path = new java.util.ArrayDeque<GeoBone>();
            for (GeoBone bone = left.getParent(); bone != null; bone = bone.getParent()) {
                path.addFirst(bone);
                if (bone == root) break;
            }
            if (path.peekFirst() != root) return;
            stack.push();
            try {
                for (GeoBone bone : path) {
                    // 眨眼隐藏父骨骼时保留已平滑的注视，不求逆退化矩阵。
                    if (Math.abs(bone.getScaleX() * bone.getScaleY() * bone.getScaleZ()) < 0.001f) return;
                    RenderUtils.prepMatrixForBone(stack, bone);
                }
                Vector3f center = new Vector3f(
                        (left.getPivotX() + right.getPivotX() - left.getPosX() - right.getPosX()) / 32f,
                        (left.getPivotY() + right.getPivotY() + left.getPosY() + right.getPosY()) / 32f,
                        (left.getPivotZ() + right.getPivotZ() + left.getPosZ() + right.getPosZ()) / 32f);
                desired = PonyGazeMath.project(stack.peek().getPositionMatrix(), targetInRender, center);
            } finally {
                stack.pop();
            }
        }
        var offset = gazeSmoother.step(desired, player.age + gazePartialTick);
        face.gaze(style, offset.x(), offset.y());
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
