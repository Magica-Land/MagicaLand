package top.csituka.magicaland.client.render;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class MagicFlameTest {
    private static int checks;
    public static void main(String[] args) {
        var motion = new MagicFlameMotion();
        motion.tick(0, new MagicFlameMotion.Point(0, 0, 0));
        for (int tick = 1; tick <= 20; tick++) motion.tick(tick, new MagicFlameMotion.Point(tick * .4, 0, 0));
        var moving = motion.sample(1, 20, 5);
        check(moving.tail().x() < -.35, "real positive X movement leaves a negative X wake");
        var before = motion.sample(.5f, 20.5, 5);
        for (int render = 0; render < 100; render++) check(before.equals(motion.sample(.5f, 20.5, 5)), "render sampling is immutable");
        motion.tick(20, new MagicFlameMotion.Point(99, 0, 0));
        check(before.equals(motion.sample(.5f, 20.5, 5)), "one sample per entity tick");
        for (int tick = 21; tick <= 40; tick++) motion.tick(tick, new MagicFlameMotion.Point(8, 0, 0));
        check(Math.abs(motion.sample(1, 40, 5).tail().x()) < .001, "stopping settles without permanent drift");
        motion.tick(41, new MagicFlameMotion.Point(1000, 0, 0));
        check(motion.sample(1, 41, 5).tail().x() == 0, "teleport resets accumulated velocity");
        motion.tick(42, new MagicFlameMotion.Point(Double.NaN, 0, 0));
        check(motion.sample(1, 42, 5).tail().finite(), "invalid input fails closed");
        for (var velocity : new MagicFlameMotion.Point[] {new MagicFlameMotion.Point(0,.6,0), new MagicFlameMotion.Point(0,-.6,0),
                new MagicFlameMotion.Point(.9,0,0), new MagicFlameMotion.Point(0,0,-.9), MagicFlameMotion.Point.ZERO}) {
            var frame = MagicFlameMotion.frame(velocity, 37, 21);
            check(frame.tail().length() <= 1.051 && frame.tail().finite(), "flame extent bounded");
            if (velocity.y() > 0) check(frame.tail().y() < 0, "ascending flame trails below its core");
            if (velocity.y() < 0) check(frame.tail().y() > 0, "descending flame trails above its core");
            for (float yaw : new float[] {0, .7f, 1.5f, 3.14f}) for (float pitch : new float[] {-1.4f, 0, 1.4f}) {
                var view = new Matrix4f().rotateX(pitch).rotateY(yaw);
                var root = new Matrix4f().translate(.4f, 1.2f, -3).rotateY(-yaw);
                var vertices = MagicFlame.geometry(root, new Matrix3f(), view, frame);
                check(vertices.size() >= 28 && vertices.size() <= MagicFlame.MAX_VERTICES && vertices.size() % 4 == 0, "bounded geometry from every view");
                Vector3f origin = root.transformPosition(new Vector3f());
                for (var vertex : vertices) {
                    check(vertex.point().isFinite() && vertex.point().distance(origin) < 1.25, "finite local silhouette");
                    check(vertex.alpha() >= 0 && vertex.alpha() <= .7f, "alpha budget per layer");
                }
            }
        }
        for (int tick = 0; tick <= 240; tick++) {
            var frame = MagicFlameMotion.frame(MagicFlameMotion.Point.ZERO, tick, 7);
            check(frame.radius() > .088 && frame.radius() < .102, "idle flicker does not pump the whole orb");
            var repeated = MagicFlameMotion.frame(MagicFlameMotion.Point.ZERO, tick + 240, 7);
            check(Math.abs(frame.radius() - repeated.radius()) < 1e-6 && Math.abs(frame.sway() - repeated.sway()) < 1e-6, "12-second animation seam");
        }
        check(MagicFlame.geometry(new Matrix4f(), new Matrix3f(), new Matrix4f().zero(), moving).isEmpty(), "singular camera basis is rejected");
        System.out.println("PASS flame motion and geometry: " + checks + " checks");
    }
    private static void check(boolean pass, String message) { checks++; if (!pass) throw new AssertionError(message); }
}
