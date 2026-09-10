package top.csituka.magicaland.client.render;

import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.util.JsonUtil;
import software.bernie.geckolib.util.RenderUtils;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.style.PonyStylePart;

public final class ManeMirrorTest {
    private static int checks;
    private record Sample(Vector3f point, Vector3f normal, float u, float v) {}

    public static void main(String[] args) throws Exception {
        Path repo = Path.of(args[0]);
        Gson gson = new Gson();
        ModelConfig old = ModelConfig.sanitize(gson.fromJson("{}", ModelConfig.class));
        check(!old.frontManeMirrored && !old.backManeMirrored && !old.tailMirrored, "old presets retain original direction");
        for (int flags = 0; flags < 8; flags++) {
            ModelConfig config = new ModelConfig();
            config.frontManeMirrored = (flags & 1) != 0;
            config.backManeMirrored = (flags & 2) != 0;
            config.tailMirrored = (flags & 4) != 0;
            ModelConfig copy = ModelConfig.sanitize(gson.fromJson(gson.toJson(config), ModelConfig.class));
            check(copy.frontManeMirrored == config.frontManeMirrored && copy.backManeMirrored == config.backManeMirrored
                    && copy.tailMirrored == config.tailMirrored, "all independent flags roundtrip and sanitize");
            check(copy.frontManeStyle.equals("01") && copy.backManeStyle.equals("01") && copy.tailStyle.equals("01"), "no extra style ids");
            check(!ManeMirror.enabled(copy, PonyStylePart.EYE), "never mirror eyes");
            for (String root : new String[] {"FrontMane", "BackMane", "Tail"})
                check(ManeMirror.rootEnabled(copy, root) == (root.equals("Tail") ? config.tailMirrored : root.equals("FrontMane") ? config.frontManeMirrored : config.backManeMirrored), "only chosen component root");
            for (String name : new String[] {"Mane", "Body", "Butt", "Head", "Emotions", "Style01FrontMane", "Style03Tail01", "TailDecorate"})
                check(!ManeMirror.rootEnabled(copy, name), "children and body are not reflected twice");
        }
        Model raw = JsonUtil.GEO_GSON.fromJson(Files.readString(repo.resolve("src/main/resources/assets/magicaland/geo/mare_geo.json")), Model.class);
        var model = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(GeometryTree.fromModel(raw));
        ModelConfig mirrored = new ModelConfig();
        mirrored.frontManeMirrored = mirrored.backManeMirrored = mirrored.tailMirrored = true;
        for (int pose = 0; pose < 5; pose++) {
            var head = model.getBone("FrontMane").orElseThrow();
            head.setRotX(pose * .035f); head.setRotZ(pose * -.018f);
            var back = model.getBone("BackMane").orElseThrow();
            back.setRotY(pose * .07f);
            // 实际尾巴控制器涉及这些子关节；父级与子级的运动须一并反射。
            for (String name : new String[] {"Style03Tail01", "Style04Tail01", "Style04Tail02"}) {
                var bone = model.getBone(name).orElseThrow();
                bone.setRotX((pose - 2) * .11f); bone.setRotY(pose * -.06f); bone.setRotZ(pose * .09f);
                bone.setPosX(pose * .05f);
            }
            for (String name : new String[] {"FrontMane", "BackMane", "Tail"}) {
                var root = model.getBone(name).orElseThrow();
                check(root.getPivotX() == 0, "mirror plane passes through attachment center: " + name);
                MatrixStack parent = parent(pose);
                Matrix4f matrix = new Matrix4f(parent.peek().getPositionMatrix());
                Matrix3f normals = new Matrix3f(parent.peek().getNormalMatrix());
                Matrix4f reflectedFrame = new Matrix4f(matrix).scale(-1, 1, 1).mul(new Matrix4f(matrix).invert());
                Matrix3f reflectedNormals = new Matrix3f(normals).scale(-1, 1, 1).mul(new Matrix3f(normals).invert());
                String before = boneState(root);
                List<Sample> original = new ArrayList<>(), flipped = new ArrayList<>();
                collect(root, parent, old, original, false);
                collect(root, parent, mirrored, flipped, false);
                check(original.size() == flipped.size() && !original.isEmpty(), "entire component retained: " + name);
                for (int i = 0; i < original.size(); i++) {
                    Sample a = original.get(i), b = flipped.get(i);
                    near(b.point, reflectedFrame.transformPosition(new Vector3f(a.point)), "animated component reflected in its parent frame");
                    near(b.normal, reflectedNormals.transform(new Vector3f(a.normal)), "normal reflected, not inverted by negative scale normalization");
                    check(a.u == b.u && a.v == b.v, "painted texture and dye stay attached to same vertex");
                }
                check(parent.peek().getPositionMatrix().equals(matrix) && parent.peek().getNormalMatrix().equals(normals), "matrix stack restored after complete branch");
                check(before.equals(boneState(root)), "shared animation state and geometry never modified");
            }
        }
        scopeCheck(mirrored);
        String renderer = Files.readString(repo.resolve("src/client/java/top/csituka/magicaland/client/render/PonyRenderer.java"));
        check(renderer.contains("ManeMirror.begin(poseStack, config, bone.getName())")
                && renderer.contains("finally { mirroredMane = previousMirror; }"), "world and preview use scoped branch state");
        check(renderer.contains("if (mirroredMane) ManeMirror.emitReversed"), "culling winding corrected only for mirrored branch");
        System.out.println("PASS ManeMirrorTest: " + checks + " checks");
    }

    private static MatrixStack parent(int pose) {
        MatrixStack stack = new MatrixStack();
        stack.translate(.31, 1.12, -.57);
        stack.peek().getPositionMatrix().rotateY(.23f + pose * .31f).rotateX(-.12f).rotateZ(.03f);
        stack.peek().getNormalMatrix().rotateY(.23f + pose * .31f).rotateX(-.12f).rotateZ(.03f);
        return stack;
    }

    private static void collect(GeoBone bone, MatrixStack stack, ModelConfig config, List<Sample> samples, boolean reflected) {
        try (var scope = ManeMirror.begin(stack, config, bone.getName())) {
            boolean active = reflected ^ (scope != null);
            stack.push();
            try {
                RenderUtils.prepMatrixForBone(stack, bone);
                for (var cube : bone.getCubes()) {
                    stack.push();
                    try {
                        RenderUtils.translateToPivotPoint(stack, cube);
                        RenderUtils.rotateMatrixAroundCube(stack, cube);
                        RenderUtils.translateAwayFromPivotPoint(stack, cube);
                        for (var quad : cube.quads()) if (quad != null) {
                            Vector3f normal = stack.peek().getNormalMatrix().transform(new Vector3f(quad.normal()));
                            RenderUtils.fixInvertedFlatCube(cube, normal);
                            var vertices = quad.vertices();
                            for (var v : vertices) samples.add(new Sample(stack.peek().getPositionMatrix().transformPosition(new Vector3f(v.position())), new Vector3f(normal), v.texU(), v.texV()));
                            if (active) {
                                Sink sink = new Sink();
                                ManeMirror.emitReversed(quad, stack.peek().getPositionMatrix(), normal, sink, 0x123456, 0x654321, .2f, .4f, .6f, .8f);
                                check(sink.samples.size() == vertices.length, "reversed primitive complete");
                                for (int i = 0; i < vertices.length; i++) {
                                    var v = vertices[vertices.length - 1 - i];
                                    Sample result = sink.samples.get(i);
                                    near(result.point, stack.peek().getPositionMatrix().transformPosition(new Vector3f(v.position())), "emitted reverse order");
                                    near(result.normal, normal, "emitted normal preserved");
                                    check(result.u == v.texU() && result.v == v.texV(), "reversed vertex keeps its UV, never reverses texture twice");
                                }
                                Vector3f a = sink.samples.get(0).point, b = sink.samples.get(1).point, c = sink.samples.get(2).point;
                                Vector3f cross = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(b));
                                if (cross.lengthSquared() > 1e-10) check(cross.normalize().dot(new Vector3f(normal).normalize()) > .999f, "outward winding matches normal after reflection");
                            }
                        }
                    } finally { stack.pop(); }
                }
                for (GeoBone child : bone.getChildBones()) collect(child, stack, config, samples, active);
            } finally { stack.pop(); }
        }
    }

    private static void scopeCheck(ModelConfig mirrored) {
        MatrixStack stack = parent(3);
        Matrix4f before = new Matrix4f(stack.peek().getPositionMatrix());
        Matrix3f normal = new Matrix3f(stack.peek().getNormalMatrix());
        try { try (var ignored = ManeMirror.begin(stack, mirrored, "Tail")) { throw new IllegalStateException("test"); } }
        catch (IllegalStateException expected) {}
        check(stack.peek().getPositionMatrix().equals(before) && stack.peek().getNormalMatrix().equals(normal), "exception restores parent matrices");
        ManeMirror.reflect(stack); ManeMirror.reflect(stack);
        check(stack.peek().getPositionMatrix().equals(before) && stack.peek().getNormalMatrix().equals(normal), "double reflection is identity");
    }

    private static String boneState(GeoBone bone) {
        StringBuilder state = new StringBuilder(bone.getName()).append(bone.getPosX()).append(bone.getPosY()).append(bone.getPosZ())
                .append(bone.getRotX()).append(bone.getRotY()).append(bone.getRotZ()).append(bone.getScaleX()).append(bone.getScaleY()).append(bone.getScaleZ());
        for (var cube : bone.getCubes()) for (var q : cube.quads()) if (q != null) for (var v : q.vertices()) state.append(v.position()).append(v.texU()).append(v.texV());
        for (var child : bone.getChildBones()) state.append(boneState(child));
        return state.toString();
    }

    private static final class Sink implements VertexConsumer {
        final List<Sample> samples = new ArrayList<>();
        public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v, int overlay, int light, float nx, float ny, float nz) {
            check(r == .2f && g == .4f && b == .6f && a == .8f && overlay == 0x654321 && light == 0x123456, "lighting and color payload unchanged");
            samples.add(new Sample(new Vector3f(x, y, z), new Vector3f(nx, ny, nz), u, v));
        }
        public VertexConsumer vertex(double x, double y, double z) { throw new AssertionError(); }
        public VertexConsumer color(int r, int g, int b, int a) { throw new AssertionError(); }
        public VertexConsumer texture(float u, float v) { throw new AssertionError(); }
        public VertexConsumer overlay(int u, int v) { throw new AssertionError(); }
        public VertexConsumer light(int u, int v) { throw new AssertionError(); }
        public VertexConsumer normal(float x, float y, float z) { throw new AssertionError(); }
        public void next() { throw new AssertionError(); }
        public void fixedColor(int r, int g, int b, int a) { throw new AssertionError(); }
        public void unfixColor() { throw new AssertionError(); }
    }
    private static void near(Vector3f actual, Vector3f expected, String message) { check(actual.distance(expected) < 1e-4f, message + ": " + actual + " / " + expected); }
    private static void check(boolean valid, String message) { checks++; if (!valid) throw new AssertionError(message); }
}
