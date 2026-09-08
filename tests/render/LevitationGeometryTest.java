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
            check(sink.effects.get(i) == 0, "ribbon reuses correct soft-flow branch");
            check(sink.alphas.get(i) <= .16f, "subtle opacity");
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

    private static final class Sink implements VertexConsumer {
        final List<Vector3f> points = new ArrayList<>();
        final List<Float> alphas = new ArrayList<>();
        final List<Integer> effects = new ArrayList<>();
        Vector3f point;
        float alpha;
        int effect;
        public VertexConsumer vertex(double x, double y, double z) { point = new Vector3f((float)x, (float)y, (float)z); return this; }
        public VertexConsumer color(int r, int g, int b, int a) { alpha = a / 255f; return this; }
        public VertexConsumer texture(float u, float v) { return this; }
        public VertexConsumer overlay(int u, int v) { effect = v; return this; }
        public VertexConsumer light(int u, int v) { return this; }
        public VertexConsumer normal(float x, float y, float z) { return this; }
        public void next() { points.add(point); alphas.add(alpha); effects.add(effect); }
        public void fixedColor(int r, int g, int b, int a) { alpha = a / 255f; }
        public void unfixColor() {}
    }

    private static void near(Vector3f a, Vector3f b, float tolerance, String message) { check(a.distance(b) <= tolerance, message + " " + a + " / " + b); }
    private static void check(boolean valid, String message) { checks++; if (!valid) throw new AssertionError(message); }
}
