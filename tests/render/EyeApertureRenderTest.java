package top.csituka.magicaland.client.render;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.IdentityHashMap;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.util.JsonUtil;
import software.bernie.geckolib.util.RenderUtils;
import top.csituka.magicaland.client.animation.PonyExpressions;

public final class EyeApertureRenderTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        var path = Path.of(args[0], "src/main/resources/assets/magicaland/geo/mare_geo.json");
        var raw = JsonUtil.GEO_GSON.fromJson(Files.readString(path), Model.class);
        var model = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(GeometryTree.fromModel(raw));
        var root = model.getBone("Emotions").orElseThrow();
        var sourceLeft = model.getBone("leye").orElseThrow();
        var sourceRight = model.getBone("reye").orElseThrow();
        for (String style : new String[] {"01", "02", "03"}) {
            assertLimits(style);
            for (float scale : new float[] {.9f, 1, 1.07f}) {
                float authorOffset = scale == 1 ? 0 : .08f;
                sourceLeft.updatePosition(authorOffset, -authorOffset * .5f, authorOffset * .25f);
                sourceRight.updatePosition(-authorOffset * .75f, authorOffset * .375f, -authorOffset * .25f);
                if (authorOffset == 0) {
                    sourceLeft.updatePosition(0, 0, 0); sourceRight.updatePosition(0, 0, 0);
                }
                sourceLeft.updateScale(scale, scale, 1); sourceRight.updateScale(scale, scale, 1);
                sourceLeft.updateRotation(0, 0, .03f); sourceRight.updateRotation(0, 0, -.03f);
                for (String name : new String[] {"CommonFace", "emot"}) model.getBone(name).orElseThrow().updateScale(1, 1, 1);
                for (String name : new String[] {"Angry", "Smeile", "close", "ScrunchedEyes", "shut"})
                    model.getBone(name).orElseThrow().updateScale(0, 0, 0);
                for (float[] direction : new float[][] {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1},
                        {.15f, 0}, {-.15f, 0}, {.5f, .5f}, {-.5f, .5f}}) {
                    var before = new IdentityHashMap<GeoBone, Vector3f>();
                    for (var b : List.of(sourceLeft, sourceRight, model.getBone("leye2").orElseThrow(), model.getBone("reye2").orElseThrow(),
                            model.getBone("leye3").orElseThrow(), model.getBone("reye3").orElseThrow()))
                        before.put(b, new Vector3f(b.getPosX(), b.getPosY(), b.getPosZ()));
                    try (var face = PonyFacePose.apply(root)) {
                        var initialLeft = position(face.pupil(style, true));
                        var initialRight = position(face.pupil(style, false));
                        require(initialLeft.equals(before.get(sourceLeft)), "left author pose copied without automatic translation " + style);
                        require(initialRight.equals(before.get(sourceRight)), "right author pose copied without automatic translation " + style);
                        for (int repeat = 0; repeat < 3; repeat++) {
                            face.gaze(style, 0, 0);
                            require(initialLeft.equals(position(face.pupil(style, true))), "zero gaze preserves left default and author track without accumulation");
                            require(initialRight.equals(position(face.pupil(style, false))), "zero gaze preserves right default and author track without accumulation");
                        }
                        face.gaze(style, direction[0], direction[1]);
                        var movedLeft = position(face.pupil(style, true));
                        var movedRight = position(face.pupil(style, false));
                        face.gaze(style, 0, 0);
                        face.gaze(style, 0, 0);
                        require(movedLeft.equals(position(face.pupil(style, true))), "zero gaze does not reset or accumulate left active pose");
                        require(movedRight.equals(position(face.pupil(style, false))), "zero gaze does not reset or accumulate right active pose");
                        for (boolean left : new boolean[] {true, false}) {
                            var pupil = face.pupil(style, left);
                            var eye = EyeApertures.forPupil(pupil);
                            require(eye != null && !eye.aperture().isEmpty(), "all six real pupils have an aperture");
                            require(EyeApertures.forPupil(pupil) == eye, "baked model aperture cached");
                            float movement = pupil.getPosX() - (left ? initialLeft.x : initialRight.x);
                            if (direction[0] != 0) require(Math.signum(movement) == Math.signum(direction[0]), "both eyes move in either horizontal direction");
                            var bounded = PonyGazeMath.bounded(direction[0], direction[1]);
                            var expected = expectedLimits(style, left);
                            boolean inward = left ? bounded.x() < 0 : bounded.x() > 0;
                            float horizontalLimit = inward ? .15f : expected[0];
                            near(movement, bounded.x() * horizontalLimit, "outward unchanged and inward softly limited " + style);
                            if (inward) {
                                require(Math.abs(movement) <= .15001f, "noseward motion stays within small allowance");
                                require(Math.abs(movement) > 0, "noseward eye may move a little instead of being locked");
                            }
                            near(pupil.getPosY() - (left ? initialLeft.y : initialRight.y),
                                    bounded.y() * (bounded.y() >= 0 ? expected[1] : expected[2]), "exact vertical travel " + style);
                            near(pupil.getPosZ(), left ? initialLeft.z : initialRight.z, "gaze preserves author depth track " + style);
                            for (float yaw : new float[] {0, .75f, -1.2f, 3.14f}) {
                                MatrixStack parent = new MatrixStack();
                                parent.translate(2, -3, .7);
                                parent.multiply(new org.joml.Quaternionf().rotationYXZ(yaw, .35f, -.25f));
                                parent.scale(.8f, 1.2f, .9f);
                                Matrix4f inverse = new Matrix4f(parent.peek().getPositionMatrix()).invert();
                                var render = EyeApertureRender.begin(parent, pupil);
                                require(render != null, "renderer recognizes pupil bone");
                                parent.push(); RenderUtils.prepMatrixForBone(parent, pupil);
                                int total = 0;
                                for (var cube : pupil.getCubes()) {
                                    Sink sink = new Sink(); parent.push();
                                    render.cube(parent, cube, sink, 0x00F000F0, 0, 1, 1, 1, 1);
                                    parent.pop(); total += sink.vertices.size();
                                    require(sink.vertices.size() % 4 == 0, "standard quad buffer stays aligned");
                                    float minU = Float.POSITIVE_INFINITY, maxU = Float.NEGATIVE_INFINITY;
                                    float minV = Float.POSITIVE_INFINITY, maxV = Float.NEGATIVE_INFINITY;
                                    for (var quad : cube.quads()) if (quad != null) for (var v : quad.vertices()) {
                                        minU = Math.min(minU, v.texU()); maxU = Math.max(maxU, v.texU());
                                        minV = Math.min(minV, v.texV()); maxV = Math.max(maxV, v.texV());
                                    }
                                    for (var v : sink.vertices) {
                                        var local = inverse.transformPosition(new Vector3f(v.position)).mul(16);
                                        require(inside(style, left, local.x, local.y), "no emitted pupil fragment outside fixed eye " + style + " " + local);
                                        require(v.position.isFinite() && v.normal.isFinite(), "finite rotated/side/back vertices and normals");
                                        require(v.alpha == 255, "no transparency workaround");
                                        require(v.u >= minU - 1e-6 && v.u <= maxU + 1e-6 && v.v >= minV - 1e-6 && v.v <= maxV + 1e-6, "original texture coordinates retained");
                                    }
                                }
                                parent.pop(); require(total > 0, "pupil does not disappear at enlarged movement limits");
                            }
                        }
                    }
                    before.forEach((b, position) -> require(position.equals(new Vector3f(b.getPosX(), b.getPosY(), b.getPosZ())), "per-render pupil position restored"));
                }
            }
        }
        require(EyeApertures.forPupil(model.getBone("Head").orElseThrow()) == null, "non-eye geometry unaffected");
        invalidResourceFallback();
        System.out.println("PASS EyeApertureRenderTest: " + checks + " real-model author-default/both-eye/UV/clipping/pose checks");
    }

    private static Vector3f position(GeoBone bone) { return new Vector3f(bone.getPosX(), bone.getPosY(), bone.getPosZ()); }

    private static float[] expectedLimits(String style, boolean left) {
        return switch (style) {
            case "02" -> new float[] {1.2f, .45f, 1.2f};
            case "03" -> new float[] {left ? .60f : .65f, .85f, 1.2f};
            default -> new float[] {1.2f, 1.05f, 1.2f};
        };
    }

    private static void assertLimits(String style) {
        var limits = PonyExpressions.gazeLimits(style);
        var expected = expectedLimits(style, false);
        near(limits.horizontal(), expected[0], "exact eye-style horizontal limit " + style);
        near(limits.up(), expected[1], "exact eye-style upward limit " + style);
        near(limits.down(), expected[2], "unchanged eye-style downward limit " + style);
        for (boolean left : new boolean[] {true, false}) {
            var eye = limits.eye(left);
            var perEye = expectedLimits(style, left);
            near(eye.outward(), perEye[0], "exact independent horizontal limit " + style);
            near(eye.up(), perEye[1], "exact independent upward limit " + style);
            near(eye.down(), perEye[2], "unchanged independent downward limit " + style);
        }
    }

    private static void invalidResourceFallback() {
        var parent = new GeoBone(null, "CommonFace", false, null, false, false);
        var pupil = new GeoBone(parent, "leye", false, null, false, false);
        var vertex = new software.bernie.geckolib.cache.object.GeoVertex(new Vector3f(-.1f, 1.5f, -.6f), .25f / 128, 62.25f / 128);
        var quad = new software.bernie.geckolib.cache.object.GeoQuad(
                new software.bernie.geckolib.cache.object.GeoVertex[] {vertex, vertex, vertex, vertex}, new Vector3f(0, 0, -1), null);
        parent.getCubes().add(new software.bernie.geckolib.cache.object.GeoCube(
                new software.bernie.geckolib.cache.object.GeoQuad[] {quad}, net.minecraft.util.math.Vec3d.ZERO,
                net.minecraft.util.math.Vec3d.ZERO, net.minecraft.util.math.Vec3d.ZERO, 0, false));
        require(EyeApertures.forPupil(pupil) == null, "degenerate resource-pack white window safely falls back");
        require(EyeApertures.forPupil(pupil) == null, "unsupported result stays cached without throwing");
        quad.normal().set(Float.NaN, 0, -1);
        require(!EyeApertures.whiteFront(quad), "invalid normal is not treated as sclera");
    }

    private static boolean inside(String style, boolean left, float x, float y) {
        double a = left ? -x : x, e = .0002;
        if (!style.equals("03")) return a >= .5 - e && a <= 3.5 + e && y >= 23.5 - e && y <= (style.equals("02") ? 26.3 : 27.2) + e;
        if (a >= .6 - e && a <= 3.6 + e && y >= 23.5 - e && y <= 25.6 + e) return true;
        if (a >= .6 - e && a <= (left ? 2.8 : 3.1) + e && y >= 25.6 - e && y <= 27.2 + e) return true;
        double[][] rotated = {{1.895818,24.601887},{3.743577,25.367254},{3.016478,27.122625},{1.168719,26.357258}};
        for (int i = 0; i < 4; i++) {
            var p = rotated[i]; var q = rotated[(i + 1) % 4];
            if ((q[0] - p[0]) * (y - p[1]) - (q[1] - p[1]) * (a - p[0]) < -e) return false;
        }
        return true;
    }

    private static final class Sample { Vector3f position = new Vector3f(), normal = new Vector3f(); float u, v; int alpha; }
    static final class Sink implements VertexConsumer {
        final List<Sample> vertices = new ArrayList<>(); Sample current = new Sample();
        public VertexConsumer vertex(double x, double y, double z) { current.position.set((float) x, (float) y, (float) z); return this; }
        public VertexConsumer color(int r, int g, int b, int a) { current.alpha = a; return this; }
        public VertexConsumer texture(float u, float v) { current.u = u; current.v = v; return this; }
        public VertexConsumer overlay(int u, int v) { return this; }
        public VertexConsumer light(int u, int v) { return this; }
        public VertexConsumer normal(float x, float y, float z) { current.normal.set(x, y, z); return this; }
        public void next() { vertices.add(current); current = new Sample(); }
        public void fixedColor(int r, int g, int b, int a) { current.alpha = a; }
        public void unfixColor() {}
    }
    private static void near(float a, float b, String text) { require(Math.abs(a - b) < .00001f, text); }
    private static void require(boolean value, String text) { checks++; if (!value) throw new AssertionError(text); }
}
