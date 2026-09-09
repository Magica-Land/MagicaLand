package top.csituka.magicaland.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class LevitationGeometryTest {
    private static int checks;
    private static final Identifier ATLAS = new Identifier("minecraft", "textures/atlas/blocks.png");

    public static void main(String[] args) {
        com.mojang.blaze3d.systems.RenderSystem.initRenderThread();
        actualCenters();
        worldCorrection();
        firstPersonCorrection();
        trailSnapshot();
        trailViews();
        movementSizedTrails();
        System.out.println("PASS levitation geometry: " + checks + " capture-center, basis and ribbon checks");
    }

    private static void actualCenters() {
        for (boolean left : new boolean[] {false, true}) for (int shape = 0; shape < 3; shape++) {
            MatrixStack root = new MatrixStack();
            root.translate(2.2, -.4, -3.7);
            root.multiply(new Quaternionf().rotateXYZ(.17f, -.8f, .3f));
            Matrix4f display = new Matrix4f().translate(left ? -.24f : .24f, .31f, -.12f)
                    .rotateXYZ(.6f, left ? -.4f : .4f, -.23f).scale(shape == 2 ? .65f : .9f);
            Matrix4f actual = new Matrix4f(root.peek().getPositionMatrix()).mul(display);
            Vector3f center = new Vector3f(shape == 0 ? .1f : -.2f, shape == 2 ? .8f : .15f, .07f);
            Vector3f size = shape == 0 ? new Vector3f(.8f, .9f, .015f)
                    : shape == 1 ? new Vector3f(.7f, .7f, .7f) : new Vector3f(.12f, 2.1f, .09f);
            var capture = new ItemAuraGeometry.Capture(root.peek());
            Sink original = new Sink();
            VertexConsumer recorder = capture.wrap(original, ATLAS);
            for (int face = 0; face < 2; face++) for (int corner = 0; corner < 4; corner++) {
                Vector3f p = new Vector3f(center).add((corner == 0 || corner == 3 ? -1 : 1) * size.x / 2,
                        (corner < 2 ? -1 : 1) * size.y / 2, (face == 0 ? -1 : 1) * size.z / 2);
                actual.transformPosition(p);
                recorder.vertex(p.x, p.y, p.z).texture(corner / 3f, face).color(255, 255, 255, 255)
                        .overlay(0, 0).light(240, 240).normal(0, 0, 1).next();
            }
            var mesh = capture.finish();
            Vector3f expected = actual.transformPosition(new Vector3f(center));
            near(mesh.centerInRender(), expected, .00001f, "actual capture center includes display mode/shape/left hand");
            check(mesh.centerInRender().distance(root.peek().getPositionMatrix().transformPosition(new Vector3f())) > .15f,
                    "center is not incorrectly reduced to caller matrix origin");
            check(original.points.size() == 8, "capture still renders original vertices exactly once");
            root.translate(100, 200, 300);
            near(mesh.centerInRender(), expected, .00001f, "delayed center immutable when caller matrix changes");
            Vector3f exposed = mesh.centerInRender();
            exposed.set(0);
            near(mesh.centerInRender(), expected, .00001f, "returned center cannot mutate mesh");
        }
    }

    private static void worldCorrection() {
        for (float yaw : new float[] {0, .8f, 2.9f, -3.1f}) for (boolean left : new boolean[] {false, true}) {
            Matrix4f entityFrame = new Matrix4f().rotateXYZ(.27f, -1.1f, 0).translate(5, .7f, -12);
            Matrix4f item = new Matrix4f(entityFrame).rotateY(yaw).rotateZ(.2f).translate(left ? -.7f : .7f, 1.4f, -.9f)
                    .rotateXYZ(-.8f, .25f, .15f).scale(.8f);
            Vector3f delta = new Vector3f(.12f, -.075f, -.18f);
            Vector3f local = new Matrix3f(item).invert().transform(new Matrix3f(entityFrame).transform(new Vector3f(delta)));
            Vector3f before = new Matrix4f(entityFrame).invert().transformPosition(item.transformPosition(new Vector3f(), new Vector3f()));
            item.translate(local);
            Vector3f after = new Matrix4f(entityFrame).invert().transformPosition(item.transformPosition(new Vector3f(), new Vector3f()));
            near(after.sub(before), delta, .00001f, "world residual survives body/flight/action transforms");
            Vector3f worldUpLocal = new Matrix3f(item).invert().transform(new Matrix3f(entityFrame).transform(new Vector3f(0, 1, 0))).normalize();
            Vector3f origin = item.transformPosition(new Vector3f(), new Vector3f());
            item.rotate(new Quaternionf().rotationAxis(.07f, worldUpLocal));
            near(item.transformPosition(new Vector3f(), new Vector3f()), origin, .00001f, "inertial angle rotates about current held anchor");
        }
    }

    private static void firstPersonCorrection() {
        for (float yaw : new float[] {-2.5f, 0, 1.6f}) for (float pitch : new float[] {-.9f, 0, 1.1f}) {
            Quaternionf camera = new Quaternionf().rotateYXZ(yaw, pitch, 0);
            Matrix3f worldToView = new Matrix3f().rotation(new Quaternionf(camera).conjugate());
            Matrix4f item = new Matrix4f().rotateXYZ(.02f, -.03f, .04f).translate(.6f, -.3f, -1.1f)
                    .rotateXYZ(-.7f, .3f, .2f);
            Vector3f delta = new Vector3f(.03f, -.04f, -.07f);
            Vector3f local = new Matrix3f(item).invert().transform(worldToView.transform(new Vector3f(delta)));
            Vector3f before = item.transformPosition(new Vector3f(), new Vector3f());
            item.translate(local);
            Vector3f renderedDelta = item.transformPosition(new Vector3f(), new Vector3f()).sub(before);
            renderedDelta.rotate(camera);
            near(renderedDelta, delta, .00001f, "first-person correction does not inherit equip/swing direction");
        }
    }

    private static void trailSnapshot() {
        LevitationMotion.Point origin = new LevitationMotion.Point(20_000_000, 64, -20_000_000);
        List<LevitationMotion.TrailPoint> points = List.of(
                new LevitationMotion.TrailPoint(origin.add(new LevitationMotion.Point(-.3, .1, -2)), .06f),
                new LevitationMotion.TrailPoint(origin.add(new LevitationMotion.Point(-.15, .12, -2)), .10f),
                new LevitationMotion.TrailPoint(origin.add(new LevitationMotion.Point(0, .14, -2)), .16f));
        Matrix4f frame = new Matrix4f();
        var trail = LevitationTrail.create(points, frame, origin, .018f);
        frame.translate(999, 999, 999);
        Sink sink = new Sink();
        trail.render(sink, 0xAA66FF, 600);
        check(sink.points.size() == 8, "three path points produce two bounded quads, not copied items");
        for (int i = 0; i < sink.points.size(); i++) {
            Vector3f point = sink.points.get(i);
            check(point.isFinite() && point.length() < 3, "large world coordinates subtract before float conversion");
            check(sink.effects.get(i) == 3, "ribbon uses its isolated trail branch");
            check(sink.alphas.get(i) <= LevitationMotion.TRAIL_ALPHA, "bounded opacity");
        }
        Vector3f newest = new Vector3f(sink.points.get(6)).add(sink.points.get(7)).mul(.5f);
        near(newest, new Vector3f(0, .14f, -2), .00001f, "ribbon newest midpoint equals actual captured item center");
        check(sink.alphas.get(0) == 0 && sink.alphas.get(1) == 0, "tail terminal fades to zero");
        float oldWidth = sink.points.get(0).distance(sink.points.get(1));
        float newWidth = sink.points.get(6).distance(sink.points.get(7));
        check(oldWidth < newWidth, "tail tapers instead of flat rectangle");
        check(LevitationTrail.create(List.of(), new Matrix4f(), origin, .02f).isEmpty(), "preview/empty path has no trail");
        Vector3f actualCenter = new Vector3f(.4f, .7f, -2);
        Vector3f[] recorded = {null};
        LevitationTrail deferred = LevitationTrail.deferred(center -> { recorded[0] = new Vector3f(center); return LevitationTrail.EMPTY; });
        check(deferred.atCenter(actualCenter).isEmpty(), "deferred center resolver stays outside render queue");
        near(recorded[0], actualCenter, 0, "capture center supplied exactly to tracker");
    }

    private static void trailViews() {
        for (float yaw : new float[] {0, .7f, 1.2f, (float) Math.PI / 2, -(float) Math.PI / 2}) {
            for (float width : new float[] {.055f, .022f, .0825f, .033f}) {
                Sink sink = sample(yaw, width, false);
                int radial = 0;
                float radialBudget = 0, previousDepth = -Float.MAX_VALUE;
                for (int i = 0; i < sink.points.size(); i += 4) {
                    float depth = 0;
                    for (int j = 0; j < 4; j++) {
                        Vector3f p = sink.points.get(i + j);
                        check(p.isFinite(), "all viewpoints emit finite geometry");
                        check(sink.effects.get(i + j) == 3, "ribbon and volume use effect 3 exclusively");
                        check(sink.alphas.get(i + j) >= 0 && sink.alphas.get(i + j) <= LevitationMotion.TRAIL_ALPHA,
                                "per-vertex opacity stays bounded");
                        depth += p.z * .25f;
                    }
                    check(depth + .00001f >= previousDepth, "transparent quads sort far to near in camera space");
                    previousDepth = depth;
                    if (sink.us.get(i) < 0) { radial++; radialBudget += sink.alphas.get(i); }
                }
                check(sink.points.size() <= (LevitationMotion.MAX_TRAIL_POINTS - 1 + LevitationTrail.MAX_SUPPORT_SLICES) * 4,
                        "history and volume support have a strict quad budget");
                check(radial <= LevitationTrail.MAX_SUPPORT_SLICES && radialBudget <= LevitationMotion.TRAIL_ALPHA * .85f,
                        "all overlapping support slices share one alpha budget");
                if (Math.abs(yaw) > 1.5f) check(radial > 0, "exact front/back view has nondegenerate soft volume");
                if (yaw == 0) check(radial == 0, "side view needs no extra volume overdraw");
            }
        }
        Sink invalid = new Sink();
        var trail = sampleObject(0, .055f, false);
        trail.render(invalid, 0xFFFFFF, 0, new Matrix4f().scale(0));
        check(invalid.points.isEmpty(), "singular camera transform is ignored safely");
        check(LevitationTrail.create(List.of(new LevitationMotion.TrailPoint(LevitationMotion.ZERO, 1),
                new LevitationMotion.TrailPoint(new LevitationMotion.Point(0, 0, -1), 1)),
                new Matrix4f(), LevitationMotion.ZERO, Float.NaN).isEmpty(), "invalid width is rejected");
    }

    private static LevitationTrail sampleObject(float yaw, float width, boolean reverse) {
        return sampleObject(yaw, width, reverse, 0, 0);
    }

    private static void movementSizedTrails() {
        for (var profile : List.of(LevitationMotion.WORLD, LevitationMotion.FIRST_PERSON)) {
            for (boolean sprint : new boolean[] {false, true}) {
                var motion = new LevitationMotion(profile);
                List<LevitationMotion.TrailPoint> points = List.of();
                LevitationMotion.Point actual = LevitationMotion.ZERO;
                double speed = sprint ? 5.6 : 4.317;
                for (int i = 0; i <= 180; i++) {
                    double time = i / 60.;
                    actual = new LevitationMotion.Point(time * speed, 0, -3);
                    motion.sample(actual, actual, 0, time, false);
                    points = motion.trail(actual, time, sprint, speed, false);
                }
                Sink sink = new Sink();
                var frame = new Matrix4f().translate((float) -actual.x(), 0, 0);
                LevitationTrail.create(points, frame, LevitationMotion.ZERO, motion.trailWidth())
                        .render(sink, 0x9966FF, 0, new Matrix4f());
                check(!sink.points.isEmpty(), "walking and sprinting both generate production geometry");
                check(sink.points.size() <= (LevitationMotion.MAX_TRAIL_POINTS - 1 + LevitationTrail.MAX_SUPPORT_SLICES) * 4,
                        "extended actual trail preserves quad cap");
                float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY, maxY = 0;
                for (Vector3f point : sink.points) {
                    check(point.isFinite(), "extended path produces finite geometry");
                    minX = Math.min(minX, point.x); maxX = Math.max(maxX, point.x);
                    maxY = Math.max(maxY, Math.abs(point.y));
                }
                double expectedLength = profile.trailLength() * (sprint ? 2 : .75);
                check(Math.abs((maxX - minX) - expectedLength) < .0001, "rendered motion path retains new length");
                check(maxY <= profile.trailWidth() * (sprint ? 1.5 : 1) + .0001, "rendered width stays within profile bounds");
                check(maxY >= profile.trailWidth() * (sprint ? 1.45 : .75), "dynamic width reaches renderer, not only motion state");
            }
        }
    }

    private static LevitationTrail sampleObject(float yaw, float width, boolean reverse, float offsetX, float offsetY) {
        List<LevitationMotion.TrailPoint> points = new ArrayList<>();
        for (int i = 0; i < LevitationMotion.MAX_TRAIL_POINTS; i++) {
            float u = (float) i / (LevitationMotion.MAX_TRAIL_POINTS - 1);
            float along = (u - .5f) * 1.35f * (reverse ? -1 : 1);
            points.add(new LevitationMotion.TrailPoint(new LevitationMotion.Point(
                    offsetX + Math.cos(yaw) * along, offsetY, -3 + Math.sin(yaw) * along),
                    LevitationMotion.TRAIL_ALPHA * (.15f + .85f * u) * (.15f + .85f * u)));
        }
        return LevitationTrail.create(points, new Matrix4f(), LevitationMotion.ZERO, width);
    }

    private static Sink sample(float yaw, float width, boolean reverse) {
        Sink sink = new Sink();
        sampleObject(yaw, width, reverse).render(sink, 0x9966FF, 0, new Matrix4f());
        return sink;
    }

    /** GPU 回归直接使用生产几何，避免测试自绘的替代矩形。 */
    public static float[] trailVertices(float yaw, float width, boolean reverse) {
        return trailVertices(yaw, width, reverse, 0, 0);
    }

    public static float[] trailVertices(float yaw, float width, boolean reverse, float offsetX, float offsetY) {
        Sink sink = new Sink();
        sampleObject(yaw, width, reverse, offsetX, offsetY).render(sink, 0x9966FF, 0, new Matrix4f());
        float[] vertices = new float[sink.points.size() / 4 * 6 * 7];
        int index = 0;
        for (int i = 0; i < sink.points.size(); i += 4) for (int corner : new int[] {0, 1, 2, 0, 2, 3}) {
            int j = i + corner;
            Vector3f p = sink.points.get(j);
            vertices[index++] = p.x; vertices[index++] = p.y; vertices[index++] = p.z;
            vertices[index++] = sink.alphas.get(j);
            vertices[index++] = sink.us.get(j); vertices[index++] = sink.vs.get(j);
            vertices[index++] = sink.effects.get(j);
        }
        return vertices;
    }

    private static final class Sink implements VertexConsumer {
        final List<Vector3f> points = new ArrayList<>();
        final List<Float> alphas = new ArrayList<>();
        final List<Integer> effects = new ArrayList<>();
        final List<Float> us = new ArrayList<>(), vs = new ArrayList<>();
        Vector3f point;
        float alpha, u, v;
        int effect;
        public VertexConsumer vertex(double x, double y, double z) { point = new Vector3f((float)x, (float)y, (float)z); return this; }
        public VertexConsumer color(int r, int g, int b, int a) { alpha = a / 255f; return this; }
        public VertexConsumer texture(float u, float v) { this.u = u; this.v = v; return this; }
        public VertexConsumer overlay(int u, int v) { effect = v; return this; }
        public VertexConsumer light(int u, int v) { return this; }
        public VertexConsumer normal(float x, float y, float z) { return this; }
        public void next() { points.add(point); alphas.add(alpha); effects.add(effect); us.add(u); vs.add(v); }
        public void fixedColor(int r, int g, int b, int a) { alpha = a / 255f; }
        public void unfixColor() {}
    }

    private static void near(Vector3f a, Vector3f b, float tolerance, String message) { check(a.distance(b) <= tolerance, message + " " + a + " / " + b); }
    private static void check(boolean valid, String message) { checks++; if (!valid) throw new AssertionError(message); }
}
