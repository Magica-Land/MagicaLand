package top.csituka.magicaland.client.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;

public final class PonyHeadLookTest {
    private static int checks;

    public static void main(String[] args) {
        angles();
        vanillaDirectionAndEyes();
        isolation();
        System.out.println("PASS PonyHeadLookTest: " + checks + " interpolation/direction/pose/isolation checks");
    }

    private static void angles() {
        require(PonyHeadLookMath.shouldApply(true, false, "Head"), "world Head enabled");
        require(!PonyHeadLookMath.shouldApply(false, false, "Head"), "preview and thumbnail disabled");
        require(!PonyHeadLookMath.shouldApply(true, true, "Head"), "nested rerender cannot double apply");
        require(!PonyHeadLookMath.shouldApply(true, false, "Neck"), "only Head");
        require(!PonyHeadLookMath.shouldApply(true, false, "Emotions"), "eye bones untouched");
        var zero = sample(35, 35, 0);
        near(zero.yaw(), 0, "body/head same direction");
        near(zero.pitch(), 0, "neutral pitch");
        var relative = sample(120, 155, 20);
        near(degrees(relative.yaw()), -35, "relative yaw, not world yaw");
        near(degrees(relative.pitch()), -20, "Gecko pitch sign");
        var wrap = PonyHeadLookMath.sample(170, 170, 179, -179, 10, 30, .5f, PonyHeadLookMath.Pose.NORMAL);
        near(degrees(wrap.yaw()), -10, "head wrap interpolates shortest path");
        near(degrees(wrap.pitch()), -20, "pitch midpoint");
        var bothWrap = PonyHeadLookMath.sample(179, -179, 170, -170, 0, 0, .5f, PonyHeadLookMath.Pose.NORMAL);
        near(bothWrap.yaw(), 0, "body and head boundary interpolate together");
        var start = PonyHeadLookMath.sample(0, 10, 20, 40, 0, 20, -1, PonyHeadLookMath.Pose.NORMAL);
        near(degrees(start.yaw()), -20, "negative partial tick bounded");
        var end = PonyHeadLookMath.sample(0, 10, 20, 40, 0, 20, 2, PonyHeadLookMath.Pose.NORMAL);
        near(degrees(end.yaw()), -30, "large partial tick bounded");
        near(degrees(end.pitch()), -20, "pitch endpoint");
        var normal = sample(0, 170, 90);
        near(degrees(normal.yaw()), -70, "normal yaw limit");
        near(degrees(normal.pitch()), -45, "normal pitch limit");
        var swim = PonyHeadLookMath.sample(0, 0, 170, 170, 90, 90, .5f, PonyHeadLookMath.Pose.SWIMMING);
        near(degrees(swim.yaw()), -40, "swim yaw limit");
        near(degrees(swim.pitch()), -25, "swim pitch limit");
        var fly = PonyHeadLookMath.sample(0, 0, 170, 170, 90, 90, .5f, PonyHeadLookMath.Pose.FLYING);
        near(degrees(fly.yaw()), -35, "fly yaw limit");
        near(degrees(fly.pitch()), -25, "fly pitch limit");
        var asleep = PonyHeadLookMath.sample(0, 0, 120, 120, 60, 60, .5f, PonyHeadLookMath.Pose.SLEEPING);
        require(asleep.equals(PonyHeadLookMath.Rotation.ZERO), "sleep authored pose retained");
        require(PonyHeadLookMath.sample(Float.NaN, 0, 0, 0, 0, 0, .5f, PonyHeadLookMath.Pose.NORMAL)
                .equals(PonyHeadLookMath.Rotation.ZERO), "bad values rejected");
        var extreme = PonyHeadLookMath.sample(Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE,
                Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, .5f, PonyHeadLookMath.Pose.NORMAL);
        require(Float.isFinite(extreme.pitch()) && Float.isFinite(extreme.yaw()), "large finite angles safe");
    }

    private static void vanillaDirectionAndEyes() {
        for (float body : new float[] {-179, -90, 0, 90, 179}) {
            for (float delta : new float[] {-35, 0, 35}) for (float pitch : new float[] {-30, 0, 30}) {
                float head = body + delta;
                var look = sample(body, head, pitch);
                Matrix4f eyeToWorld = new Matrix4f().rotateY((float) Math.toRadians(180 - body))
                        .rotateY(look.yaw()).rotateX(look.pitch());
                Vector3f direction = eyeToWorld.transformDirection(new Vector3f(0, 0, -1));
                float yawRadians = (float) Math.toRadians(head), pitchRadians = (float) Math.toRadians(pitch);
                Vector3f vanilla = new Vector3f(-(float) Math.sin(yawRadians) * (float) Math.cos(pitchRadians),
                        -(float) Math.sin(pitchRadians), (float) Math.cos(yawRadians) * (float) Math.cos(pitchRadians));
                require(direction.distance(vanilla) < .00001, "matches vanilla look direction");
                var residual = PonyGazeMath.project(eyeToWorld, new Vector3f(vanilla).mul(5), new Vector3f());
                near(residual.x(), 0, "existing eye inverse automatically removes applied head yaw");
                near(residual.y(), 0, "existing eye inverse automatically removes applied head pitch");
            }
        }
    }

    private static void isolation() {
        GeoBone head = new GeoBone(null, "Head", false, 0d, false, false);
        head.updateRotation(.12f, .23f, .34f);
        head.updatePosition(1, 2, 3);
        head.updateScale(.9f, 1.1f, 1.2f);
        head.updatePivot(0, 21, -3);
        head.saveInitialSnapshot();
        var snapshot = head.getInitialSnapshot();
        var offset = sample(0, 35, 20);
        for (int flags = 0; flags < 8; flags++) {
            for (int frame = 0; frame < 250; frame++) {
                head.resetStateChanges();
                if ((flags & 1) != 0) head.markRotationAsChanged();
                if ((flags & 2) != 0) head.markPositionAsChanged();
                if ((flags & 4) != 0) head.markScaleAsChanged();
                try (var applied = new PonyRenderer.HeadPose(null, head, offset)) {
                    near(head.getRotX(), .12f + offset.pitch(), "adds to current animation pitch");
                    near(head.getRotY(), .23f + offset.yaw(), "adds to current animation yaw");
                    near(head.getRotZ(), .34f, "animation roll preserved");
                }
                require(Float.floatToIntBits(head.getRotX()) == Float.floatToIntBits(.12f)
                        && Float.floatToIntBits(head.getRotY()) == Float.floatToIntBits(.23f)
                        && Float.floatToIntBits(head.getRotZ()) == Float.floatToIntBits(.34f), "exact restoration, no accumulation");
                require(head.hasRotationChanged() == ((flags & 1) != 0)
                        && head.hasPositionChanged() == ((flags & 2) != 0)
                        && head.hasScaleChanged() == ((flags & 4) != 0), "animation flags restored");
                require(head.getInitialSnapshot() == snapshot, "initial snapshot untouched");
            }
        }
        try {
            try (var applied = new PonyRenderer.HeadPose(null, head, offset)) { throw new IllegalStateException("render failure"); }
        } catch (IllegalStateException expected) {
            near(head.getRotX(), .12f, "failure restores head");
        }
        var nextPlayer = sample(0, -20, -10);
        try (var applied = new PonyRenderer.HeadPose(null, head, nextPlayer)) {
            near(head.getRotY(), .23f + nextPlayer.yaw(), "next player receives no previous offset");
        }
        near(head.getRotY(), .23f, "preview sees original pose afterwards");
        near(head.getPosX(), 1, "position unchanged");
        near(head.getScaleY(), 1.1f, "scale unchanged");
        near(head.getPivotY(), 21, "pivot unchanged");
    }

    private static PonyHeadLookMath.Rotation sample(float body, float head, float pitch) {
        return PonyHeadLookMath.sample(body, body, head, head, pitch, pitch, .5f, PonyHeadLookMath.Pose.NORMAL);
    }
    private static float degrees(float radians) { return (float) Math.toDegrees(radians); }
    private static void near(float actual, float expected, String label) { require(Math.abs(actual - expected) < .00001f, label); }
    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
