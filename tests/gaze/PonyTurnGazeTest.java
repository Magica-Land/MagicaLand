package top.csituka.magicaland.client.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class PonyTurnGazeTest {
    private static int checks;
    private static final Object OWNER = new Object(), WORLD = new Object();

    public static void main(String[] args) {
        velocity();
        projectionDirection();
        samplesAndPriority();
        resetCases();
        frameRates();
        System.out.println("PASS PonyTurnGazeTest: " + checks + " direction/priority/timing/reset checks");
    }

    private static void velocity() {
        require(PonyTurnGaze.velocity(10, 7).equals(new PonyGazeMath.Offset(-.6f, -.35f)), "positive yaw right, positive pitch down");
        require(PonyTurnGaze.velocity(-10, -7).equals(new PonyGazeMath.Offset(.6f, .35f)), "negative yaw left, negative pitch up");
        require(PonyTurnGaze.velocity(0, 0).equals(PonyGazeMath.Offset.ZERO), "no absolute-angle offset");
        near(PonyTurnGaze.velocity(5, -3.5).x(), -.3f, "half-rate horizontal");
        near(PonyTurnGaze.velocity(5, -3.5).y(), .175f, "half-rate vertical");
        require(PonyTurnGaze.velocity(Double.NaN, 1).equals(PonyGazeMath.Offset.ZERO), "invalid yaw safe");
        require(PonyTurnGaze.velocity(1, Double.POSITIVE_INFINITY).equals(PonyGazeMath.Offset.ZERO), "invalid pitch safe");
        for (int yaw = -400; yaw <= 400; yaw += 4) for (int pitch = -200; pitch <= 200; pitch += 4) {
            var value = PonyTurnGaze.velocity(yaw, pitch);
            require(Math.abs(value.x()) <= .6f && Math.abs(value.y()) <= .35f, "subtle per-axis caps");
            require(value.x() * value.x() + value.y() * value.y() <= 1, "fits existing ellipse without enlarging safety bounds");
        }
    }

    private static void projectionDirection() {
        for (float body : new float[] {-179, -90, 0, 90, 179})
            for (float relative : new float[] {-40, 0, 40})
                for (float pitch : new float[] {-30, 0, 30})
                    for (float turn : new float[] {-5, 5}) {
                        float yaw = body + relative;
                        Matrix4f eye = new Matrix4f().rotateY(rad(180 - body)).rotateY(rad(-relative)).rotateX(rad(-pitch));
                        var horizontal = PonyGazeMath.project(eye, direction(yaw + turn, pitch).mul(5), new Vector3f());
                        var vertical = PonyGazeMath.project(eye, direction(yaw, pitch + turn).mul(5), new Vector3f());
                        var rate = PonyTurnGaze.velocity(turn, turn);
                        require(Math.signum(horizontal.x()) == Math.signum(rate.x()), "world-space future yaw matches pupil X sign");
                        require(Math.signum(vertical.y()) == Math.signum(rate.y()), "world-space future pitch matches pupil Y sign");
                    }
        Matrix4f southFacingPony = new Matrix4f().rotateY(rad(180));
        var futureRight = PonyGazeMath.project(southFacingPony, direction(5, 0).mul(5), new Vector3f());
        require(futureRight.x() < 0, "yaw 0 to +5 produces negative Gecko pupil position.x");
        var futureDown = PonyGazeMath.project(southFacingPony, direction(0, 5).mul(5), new Vector3f());
        require(futureDown.y() < 0, "pitch 0 to +5 produces negative pupil position.y");
    }

    private static void samplesAndPriority() {
        PonyTurnGaze state = new PonyTurnGaze();
        require(sample(state, 0, 100, 20, false).resetSmoothing(), "first observation starts centered");
        var moved = sample(state, 1, 110, 27, false);
        require(moved.offset().equals(new PonyGazeMath.Offset(-.6f, -.35f)), "measured angular speed");
        require(!moved.resetSmoothing(), "ordinary turn keeps smooth history");
        var duplicate = sample(state, 1, 115, 27, false);
        require(duplicate.offset().equals(moved.offset()), "same time does not advance or double integrate");
        var next = sample(state, 2, 115, 27, false);
        near(next.offset().x(), -.3f, "duplicate draw did not consume five degrees");
        near(next.offset().y(), 0, "pitch stops independently");
        require(sample(state, 3, 115, 27, false).offset().equals(PonyGazeMath.Offset.ZERO), "turn stops at nonzero absolute yaw/pitch");
        for (int i = 4; i < 40; i++) {
            var active = sample(state, i, 115 + (i - 3) * 5, 27, true);
            require(active.offset().equals(PonyGazeMath.Offset.ZERO), "entity gets sole priority despite ongoing turn");
        }
        require(sample(state, 40, 295, 27, false).offset().equals(PonyGazeMath.Offset.ZERO), "entity release has no accumulated turn impulse");
        var centerEntity = PonyGazeMath.project(new Matrix4f(), new Vector3f(0, 0, -5), new Vector3f());
        require(centerEntity.x() == 0 && centerEntity.y() == 0, "center target may legitimately project to zero");
        require(sample(state, 41, 305, 27, true).offset().equals(PonyGazeMath.Offset.ZERO), "zero-projection entity still suppresses turn");
        var behindEntity = PonyGazeMath.project(new Matrix4f(), new Vector3f(0, 0, 5), new Vector3f());
        require(behindEntity.equals(PonyGazeMath.Offset.ZERO), "outside eye projection target may be zero");
        require(sample(state, 42, 315, 27, true).offset().equals(PonyGazeMath.Offset.ZERO), "outside projection entity is not mistaken for absent target");
    }

    private static void resetCases() {
        PonyTurnGaze state = new PonyTurnGaze();
        sample(state, 0, 179, 0, false);
        var wrapped = sample(state, 1, -179, 0, false);
        require(!wrapped.resetSmoothing(), "180 boundary is not a rotation jump");
        near(wrapped.offset().x(), -.12f, "179 to -179 is positive two-degree turn");
        sample(state, 2, 179, 0, false);
        near(sample(state, 3, 177, 0, false).offset().x(), .12f, "negative wrap direction");
        require(sample(state, 10, 180, 0, false).resetSmoothing(), "offscreen gap clears velocity and smoothing");
        require(sample(state, 1, 180, 0, false).resetSmoothing(), "clock rewind clears velocity");
        require(state.sample(OWNER, WORLD, 2, 185, 0, 5, 0, 0, false).resetSmoothing(), "five-block teleport clears velocity");
        require(state.sample(OWNER, WORLD, 3, 305, 0, 5, 0, 0, false).resetSmoothing(), "120 degree discontinuity is not an eye fling");
        require(state.sample(OWNER, WORLD, 4, 305, 65, 5, 0, 0, false).resetSmoothing(), "large pitch jump resets");
        require(state.sample(new Object(), WORLD, 5, 305, 65, 5, 0, 0, false).resetSmoothing(), "new entity identity resets");
        require(state.sample(OWNER, new Object(), 6, 305, 65, 5, 0, 0, false).resetSmoothing(), "new world resets");
        require(sample(state, Double.NaN, 0, 0, false).resetSmoothing(), "invalid time resets");
        require(sample(state, 1, Float.NaN, 0, false).offset().equals(PonyGazeMath.Offset.ZERO), "invalid angles centered");
        require(state.sample(OWNER, WORLD, 1, 0, 0, Double.POSITIVE_INFINITY, 0, 0, false).resetSmoothing(), "invalid position resets");
        state.reset();
        require(sample(state, 100, 45, 20, false).offset().equals(PonyGazeMath.Offset.ZERO), "face/config gate reset has no stale impulse");
    }

    private static void frameRates() {
        PonyGazeMath.Offset reference = null;
        for (int fps : new int[] {20, 30, 60, 144}) {
            PonyTurnGaze state = new PonyTurnGaze();
            PonyGazeMath.Smoother smoothing = new PonyGazeMath.Smoother();
            sample(state, 0, 0, 0, false);
            smoothing.step(PonyGazeMath.Offset.ZERO, 0);
            PonyGazeMath.Offset result = null;
            for (int frame = 1; frame <= fps; frame++) {
                double ticks = frame * 20d / fps;
                // 与远端逐 tick 朝向的部分 tick 插值一致，不能按单帧跳变限幅。
                int whole = (int) Math.floor(ticks);
                float partial = (float) (ticks - whole);
                float yaw = whole * 4 + partial * 4, pitch = whole * -2 + partial * -2;
                var raw = sample(state, ticks, yaw, pitch, false);
                require(!raw.resetSmoothing(), "normal frame timing does not reset");
                near(raw.offset().x(), -.24f, "constant angular speed independent of fps");
                result = smoothing.step(raw.offset(), ticks);
                var duplicate = sample(state, ticks, yaw, pitch, false);
                require(duplicate.offset().equals(raw.offset()), "repeated entity render stable");
                var unchanged = smoothing.step(duplicate.offset(), ticks);
                near(unchanged.x(), result.x(), "shared smoothing not advanced by rerender");
            }
            if (reference == null) reference = result;
            near(result.x(), reference.x(), "smoothed horizontal at one second matches frame rates");
            near(result.y(), reference.y(), "smoothed vertical at one second matches frame rates");
            for (int frame = 1; frame <= fps * 2; frame++) {
                double ticks = 20 + frame * 20d / fps;
                result = smoothing.step(sample(state, ticks, 80, -40, false).offset(), ticks);
            }
            near(result.x(), 0, "stopping smoothly recenters despite yaw 80");
            near(result.y(), 0, "stopping smoothly recenters despite pitch -40");
        }
    }

    private static PonyTurnGaze.Sample sample(PonyTurnGaze state, double time, float yaw, float pitch, boolean entity) {
        return state.sample(OWNER, WORLD, time, yaw, pitch, 0, 0, 0, entity);
    }
    private static Vector3f direction(float yaw, float pitch) {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new Vector3f((float) (-Math.sin(y) * Math.cos(p)), (float) -Math.sin(p), (float) (Math.cos(y) * Math.cos(p)));
    }
    private static float rad(float degrees) { return (float) Math.toRadians(degrees); }
    private static void near(float actual, float expected, String label) { require(Math.abs(actual - expected) < .00002f, label + ": " + actual + " / " + expected); }
    private static void require(boolean condition, String label) { checks++; if (!condition) throw new AssertionError(label); }
}
