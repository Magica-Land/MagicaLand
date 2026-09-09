package top.csituka.magicaland.client.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class PonyGuiGazeTest {
    private static int checks;
    private static final Object SCREEN = new Object(), PLAYER = new Object();

    public static void main(String[] args) {
        scope();
        directions();
        projection();
        smoothing();
        invalid();
        System.out.println("PASS PonyGuiGazeTest: " + checks + " projection/scope/pause/isolation checks");
    }

    private static void scope() {
        check(PonyGuiGaze.current() == null, "no GUI target during world rendering");
        try (var first = PonyGuiGaze.beginAt(SCREEN, PLAYER, 10, 20, 800, 600, 0)) {
            var frame = PonyGuiGaze.current();
            check(frame.owner() == SCREEN && frame.entity() == PLAYER, "exact GUI and rendered entity identity");
            try (var second = PonyGuiGaze.beginAt(new Object(), null, 30, 40, 800, 600, 1)) {
                check(PonyGuiGaze.current() != frame && PonyGuiGaze.current().entity() == null, "title-screen preview without player");
                throw new IllegalStateException("render failure");
            } catch (IllegalStateException expected) {
                check(PonyGuiGaze.current() == frame, "nested render exception restores previous frame");
            }
            first.close(); first.close();
            check(PonyGuiGaze.current() == null, "close is idempotent");
        }
        check(PonyGuiGaze.current() == null, "GUI target never leaks after draw");
    }

    private static void directions() {
        var matrix = new Matrix4f().translation(200, 180, 50).scale(40, -40, -40);
        var view = new Matrix4f().translation(0, 0, -11000);
        var p = ortho(800, 600);
        var center = new Vector3f();
        var atCenter = at(200, 180, matrix, view, p, center, 800, 600);
        near(atCenter, PonyGazeMath.Offset.ZERO, "mouse at eye center is forward");
        check(at(250, 180, matrix, view, p, center, 800, 600).x() < 0, "screen right follows Gecko X inversion");
        check(at(150, 180, matrix, view, p, center, 800, 600).x() > 0, "screen left follows Gecko X inversion");
        check(at(200, 130, matrix, view, p, center, 800, 600).y() > 0, "screen up looks up");
        check(at(200, 230, matrix, view, p, center, 800, 600).y() < 0, "screen down looks down");
        float previous = 0;
        for (int dx = 1; dx <= 240; dx++) {
            var offset = at(200 + dx, 180, matrix, view, p, center, 800, 600);
            check(offset.x() <= previous + .00001f, "horizontal response continuous and monotonic");
            check(Math.abs(offset.y()) < .0001f, "horizontal movement does not create vertical movement");
            previous = offset.x();
        }
        var close = at(201, 180, matrix, view, p, center, 800, 600);
        check(Math.abs(close.x()) < .03f, "one pixel does not saturate gaze");
        var small = new Matrix4f().translation(100, 180, 50).scale(12, -12, -12);
        for (int x = 200; x <= 1920; x += 20) {
            near(at(x, 180, small, view, ortho(1920, 1080), center, 1920, 1080),
                    new PonyGazeMath.Offset(-1, 0), "far cursor remains at horizontal limit instead of fading to neutral");
            near(at(200 - x, 180, small, view, ortho(1920, 1080), center, 1920, 1080),
                    new PonyGazeMath.Offset(1, 0), "far left cursor remains at horizontal limit");
        }
        var zoom = new Matrix4f().translation(400, 360, 100).scale(80, -80, -80);
        near(at(230, 200, matrix, view, p, center, 800, 600),
                at(460, 400, zoom, view, ortho(1600, 1200), center, 1600, 1200), "GUI scale invariant");
        var backwards = new Matrix4f().translation(200, 180, 50).scale(40, -40, 40);
        near(at(200, 180, backwards, view, p, center, 800, 600), PonyGazeMath.Offset.ZERO, "rear view does not flip gaze through skull");
        PonyGazeMath.Offset previousSide = null;
        for (int step = 800; step <= 1000; step++) {
            var side = new Matrix4f(matrix).rotateY((float) Math.toRadians(step / 10d));
            var value = at(200, 180, side, view, p, center, 800, 600);
            if (previousSide != null) check(Math.abs(value.x() - previousSide.x()) < .03f, "side-to-back crossing fades without jumping");
            previousSide = value;
        }
    }

    private static void projection() {
        Vector3f center = new Vector3f(-.00625f, 1.59375f, -.596875f);
        for (int width : new int[] {320, 800, 1920}) for (int height : new int[] {240, 600, 1080})
            for (float scale : new float[] {12, 40, 160}) for (int yaw : new int[] {0, 25, 90, 145, 155, 180, 270})
                for (int pitch : new int[] {-70, -10, 0, 30, 70}) for (boolean inventory : new boolean[] {false, true}) {
                    var eye = new Matrix4f().translation(width * .27f, height * .62f, 150);
                    if (inventory) eye.scale(scale, scale, -scale).rotateZ((float) Math.PI);
                    else eye.scale(scale).rotateZ((float) Math.PI);
                    eye.rotateY((float) Math.toRadians(yaw)).rotateX((float) Math.toRadians(pitch));
                    eye.translate(-.5f, -.51f, -.5f).rotateZ(.07f);
                    var view = new Matrix4f().translation(3, -5, -11000).rotateZ(.03f);
                    var eyeToView = new Matrix4f(view).mul(eye);
                    var visibleCenter = eyeToView.transformPosition(new Vector3f(center));
                    var p = ortho(width, height);
                    for (float[] direction : new float[][] {{0, 0}, {.8f, .4f}, {-.8f, -.4f}, {2, -2}}) {
                        var target = new Vector3f(visibleCenter).add(scale * direction[0], scale * direction[1], scale * 2);
                        var pixel = pixel(p, target, width, height);
                        var expected = facing(eyeToView, target, center);
                        var actual = at(pixel.x, pixel.y, eye, view, p, center, width, height);
                        near(actual, expected, "GUI " + width + "x" + height + " scale=" + scale + " yaw=" + yaw
                                + " pitch=" + pitch + " inventory=" + inventory + " cursor=" + java.util.Arrays.toString(direction));
                        check(Math.hypot(actual.x(), actual.y()) <= 1.00001, "all diagonal movements bounded");
                    }
                }
        var perspective = new Matrix4f().perspective((float) Math.toRadians(70), 800f / 600, .1f, 100);
        var eye = new Matrix4f().translation(0, 0, -8).rotateY((float) Math.PI);
        var target = new Vector3f(.8f, .4f, -6);
        var pixel = pixel(perspective, target, 800, 600);
        near(at(pixel.x, pixel.y, eye, new Matrix4f(), perspective, new Vector3f(), 800, 600),
                PonyGazeMath.project(eye, target, new Vector3f()), "perspective homogeneous division");
    }

    private static void smoothing() {
        var desired = new PonyGazeMath.Offset(.6f, -.4f);
        for (int fps : new int[] {20, 30, 60, 144}) {
            var tracker = new PonyGuiGaze.Tracker();
            PonyGazeMath.Offset value = null;
            for (int i = 0; i <= fps; i++) {
                var f = frame(10, 20, 800, 600, i * 20d / fps);
                value = tracker.step(f, desired);
                near(value, tracker.step(f, desired), "repeated drawing does not advance smoothing");
            }
            float weight = (float) (1 - Math.exp(-20d / 2.5));
            near(value, new PonyGazeMath.Offset(desired.x() * weight, desired.y() * weight), "UI time works while player/world ticks are paused");
            near(tracker.step(frame(0, 0, 800, 600, 30), desired), PonyGazeMath.Offset.ZERO, "reopening after a gap starts neutral");
            tracker.step(frame(0, 0, 800, 600, 31), desired);
            near(tracker.step(new PonyGuiGaze.Frame(new Object(), PLAYER, 0, 0, 800, 600, 32), desired), PonyGazeMath.Offset.ZERO, "different screen never inherits gaze");
        }
        var gui = new PonyGuiGaze.Tracker();
        var world = new PonyGazeMath.Smoother();
        world.step(desired, 0);
        var before = world.step(desired, 2);
        for (int i = 0; i < 30; i++) gui.step(frame(0, 0, 800, 600, i), new PonyGazeMath.Offset(-1, 0));
        near(world.step(desired, 2), before, "GUI leaves world smoother untouched");
    }

    private static void invalid() {
        var eye = new Matrix4f().translation(200, 180, 50).scale(40, -40, -40);
        var view = new Matrix4f().translation(0, 0, -11000);
        var p = ortho(800, 600);
        for (double bad : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            near(at(bad, 30, eye, view, p, new Vector3f(), 800, 600), PonyGazeMath.Offset.ZERO, "invalid mouse safe");
            near(at(30, bad, eye, view, p, new Vector3f(), 800, 600), PonyGazeMath.Offset.ZERO, "invalid mouse safe");
        }
        near(at(30, 40, eye, view, p, new Vector3f(), 0, 600), PonyGazeMath.Offset.ZERO, "zero screen size");
        near(at(30, 40, eye, view, new Matrix4f().zero(), new Vector3f(), 800, 600), PonyGazeMath.Offset.ZERO, "singular projection");
        near(at(30, 40, new Matrix4f().scale(0), view, p, new Vector3f(), 800, 600), PonyGazeMath.Offset.ZERO, "hidden blink scale");
        check(Math.abs(p.determinant()) < .000001f, "real orthographic projection has tiny but valid determinant");
        check(at(250, 180, eye, view, p, new Vector3f(), 800, 600).x() < -.1f, "tiny determinant is not incorrectly rejected");
    }

    private static PonyGuiGaze.Frame frame(double x, double y, int width, int height, double time) {
        return new PonyGuiGaze.Frame(SCREEN, PLAYER, x, y, width, height, time);
    }

    private static PonyGazeMath.Offset facing(Matrix4f matrix, Vector3f target, Vector3f center) {
        var result = PonyGazeMath.project(matrix, target, center);
        var delta = new Matrix4f(matrix).invert().transformPosition(new Vector3f(target)).sub(center);
        double t = Math.max(0, Math.min(1, -delta.z / .2));
        float visibility = (float) (t * t * (3 - 2 * t));
        return new PonyGazeMath.Offset(result.x() * visibility, result.y() * visibility);
    }

    private static PonyGazeMath.Offset at(double x, double y, Matrix4f eye, Matrix4f view, Matrix4f projection,
            Vector3f center, int width, int height) {
        return PonyGuiGaze.project(frame(x, y, width, height, 0), eye, view, projection, center);
    }

    private static Matrix4f ortho(int width, int height) {
        return new Matrix4f().setOrtho(0, width, height, 0, 1000, 21000);
    }

    private static Vector3f pixel(Matrix4f p, Vector3f point, int width, int height) {
        var clip = p.transform(new Vector4f(point, 1));
        return new Vector3f((clip.x / clip.w + 1) * width * .5f, (1 - clip.y / clip.w) * height * .5f, clip.z / clip.w);
    }

    private static void near(PonyGazeMath.Offset actual, PonyGazeMath.Offset expected, String why) {
        check(Math.abs(actual.x() - expected.x()) < .0015f && Math.abs(actual.y() - expected.y()) < .0015f,
                why + ": " + actual + " != " + expected);
    }

    private static void check(boolean condition, String why) {
        checks++;
        if (!condition) throw new AssertionError(why);
    }
}
