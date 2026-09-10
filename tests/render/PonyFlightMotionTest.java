package top.csituka.magicaland.client.animation;

import java.util.Random;
import org.joml.Matrix4d;
import org.joml.Vector3d;

public final class PonyFlightMotionTest {
    private static int checks;
    public static void main(String[] args) {
        transitions(); motion(); directions(); curlAndVerticalAcceleration(); foldedFrontLegs(); renderIndependence(); safety();
        System.out.println("PASS PonyFlightMotionTest: " + checks + " checks");
    }

    private static void transitions() {
        PonyFlightMotion motion = new PonyFlightMotion();
        motion.observe(0, 0, 0, 0, 0, false, true);
        require(motion.sample(0, 0).equals(PonyFlightMotion.Pose.NONE), "ground has no visual");
        motion.observe(1, 0, 0, 0, 0, true, true);
        close(motion.sample(1, 0).amount(), 0, 0, "takeoff starts without a jump");
        require(motion.sample(1.5, 0).amount() > 0 && motion.sample(1.5, 0).magic() == 0, "horn leads body aura");
        for (int i = 2; i <= 6; i++) motion.observe(i, 0, 0, 0, 0, true, true);
        close(motion.sample(6, 0).amount(), 1, 0, "full pose at 0.25s");
        close(motion.sample(6, 0).magic(), 1, 0, "aura catches up by 0.25s");
        motion.observe(7, 0, 0, 0, 0, false, true);
        close(motion.sample(7, 0).amount(), 1, 0, "landing no pop");
        for (int i = 8; i <= 11; i++) motion.observe(i, 0, 0, 0, 0, false, true);
        require(motion.sample(11, 0).equals(PonyFlightMotion.Pose.NONE), "lands in 0.20s");
        motion.observe(12, 0, 0, 0, 0, true, false);
        motion.observe(14, 0, 0, 0, 0, true, false);
        float before = motion.sample(14, 0).amount();
        motion.observe(14, 0, 0, 0, 0, false, false);
        close(motion.sample(14, 0).amount(), before, 1e-7, "cancelled takeoff is continuous");
        require(motion.sample(14, 0).magic() == 0, "hornless no magic");
        motion.observe(15, 0, 0, 0, 0, false, false);
        before = motion.sample(15, 0).amount();
        motion.observe(15, 0, 0, 0, 0, true, false);
        close(motion.sample(15, 0).amount(), before, 1e-7, "restart is continuous");
    }

    private static void motion() {
        PonyFlightMotion motion = new PonyFlightMotion();
        for (int i = 0; i <= 80; i++) motion.observe(i, 0, 0, i * .3, 0, true, false);
        var frame = motion.sample(80.5, 0);
        require(frame.bodyPitch() > 8 && frame.legPitch() > 8, "forward motion leans and trails");
        for (int i = 81; i <= 180; i++) motion.observe(i, 0, 0, 24, 0, true, false);
        frame = motion.sample(180.5, 0);
        require(Math.abs(frame.bodyPitch()) < .0001 && Math.abs(frame.legPitch()) < .0001, "stop settles");
        require(Math.abs(frame.bob()) > .001, "idle hover retained");
        for (int i = 181; i <= 220; i++) motion.observe(i, 0, 0, 24 - (i - 180) * .3, 0, true, false);
        frame = motion.sample(220.5, 0);
        require(frame.bodyPitch() < -8 && frame.legPitch() < -8, "backward direction reverses reaction");
        motion = new PonyFlightMotion();
        for (int i = 0; i <= 80; i++) motion.observe(i, i * -.3, 0, 0, 90, true, false);
        frame = motion.sample(80.5, 0);
        close(frame.bodyPitch(), 9, .001, "rotated body uses local forward");
        close(frame.bodyRoll(), 0, .001, "straight ahead is not sideways");
    }

    private static void renderIndependence() {
        PonyFlightMotion reference = new PonyFlightMotion();
        PonyFlightMotion busy = new PonyFlightMotion();
        for (int i = 0; i < 600; i++) {
            double x = Math.sin(i * .05), y = Math.sin(i * .1), z = i * .2;
            reference.observe(i, x, y, z, i * .5, true, true);
            busy.observe(i, x, y, z, i * .5, true, true);
            for (int render = 0; render < 40; render++) busy.sample(i + render / 40d, 1);
            require(reference.sample(i + .7, 1).equals(busy.sample(i + .7, 1)), "render count never changes state");
            var paused = busy.sample(i + .7, 1);
            for (int render = 0; render < 5; render++) require(paused.equals(busy.sample(i + .7, 1)), "paused duplicate identical");
        }
    }

    private static void curlAndVerticalAcceleration() {
        PonyFlightMotion motion = new PonyFlightMotion();
        for (int tick = 0; tick <= 40; tick++) motion.observe(tick, 0, 0, 0, 0, true, false, false);
        close(motion.sample(40.9, 0).curlDelta(), 0, 1e-6, "ordinary hind legs keep gentle authored curl");
        motion.observe(41, 0, 0, 0, 0, true, false, true);
        close(motion.sample(41, 0).curlDelta(), 0, 1e-6, "sprint does not snap the pose");
        for (int tick = 42; tick <= 90; tick++) motion.observe(tick, 0, 0, 0, 0, true, false, true);
        close(motion.sample(90.9, 0).curlDelta(), PonyFlightMotion.SPRINT_CURL - PonyFlightMotion.BASE_CURL, 1e-5,
                "sprint settles into stronger curl without changing base animation");
        for (int tick = 91; tick <= 150; tick++) motion.observe(tick, 0, 0, 0, 0, true, false, false);
        close(motion.sample(150.9, 0).curlDelta(), 0, 1e-5, "sprint release smoothly restores normal hover");

        for (int direction : new int[] {-1, 1}) {
            motion = new PonyFlightMotion();
            for (int tick = 0; tick <= 40; tick++) motion.observe(tick, 0, 0, 0, 0, true, false, false);
            for (int step = 1; step <= 8; step++)
                motion.observe(40 + step, 0, direction * .015 * step * step, 0, 0, true, false, false);
            require(motion.sample(48.9, 0).curlDelta() * direction < -.04,
                    "upward acceleration extends; downward acceleration curls/lifts limbs");
            for (int tick = 49; tick <= 120; tick++)
                motion.observe(tick, 0, direction * (.96 + (tick - 48) * .225), 0, 0, true, false, false);
            close(motion.sample(120.9, 0).curlDelta(), 0, 1e-5, "constant vertical velocity returns to baseline");
            close(motion.sample(120.9, 0).legPitch(), 0, 1e-5, "no permanent upward/downward leg pitch bias");
        }
    }

    private static void directions() {
        for (int side : new int[] {-1, 1}) {
            PonyFlightMotion motion = new PonyFlightMotion();
            for (int tick = 0; tick <= 60; tick++) motion.observe(tick, tick * .3 * side, 0, 0, 0, true, false);
            var pose = motion.sample(60.9, 0);
            require(pose.bodyRoll() * side > 0 && pose.legRoll() * side > 0, "side motion has correct model roll sign");
            var body = new Matrix4d().rotateY(Math.PI).rotateZ(Math.toRadians(pose.bodyRoll()))
                    .transformDirection(new Vector3d(0, 1, 0));
            var legs = new Matrix4d().rotateY(Math.PI).rotateZ(Math.toRadians(pose.bodyRoll() + pose.legRoll()))
                    .transformDirection(new Vector3d(0, -1, 0));
            require(body.x * side > 0, "upper body leans toward sideways movement");
            require(legs.x * side < 0, "downward limbs trail sideways movement after parent and local rotations");
            require(Math.abs(pose.bodyRoll() + pose.legRoll()) <= 13, "combined lateral tilt is bounded");
        }
        PonyFlightMotion forward = new PonyFlightMotion();
        for (int tick = 0; tick <= 60; tick++) forward.observe(tick, 0, 0, tick * .3, 0, true, false);
        var pose = forward.sample(60.9, 0);
        var body = new Matrix4d().rotateY(Math.PI).rotateX(-Math.toRadians(pose.bodyPitch()))
                .transformDirection(new Vector3d(0, 1, 0));
        var legs = new Matrix4d().rotateY(Math.PI).rotateX(-Math.toRadians(pose.bodyPitch() + pose.legPitch()))
                .transformDirection(new Vector3d(0, -1, 0));
        require(body.z > 0 && legs.z < 0, "forward body pitch and limb drag follow renderer coordinates");
    }

    private static void foldedFrontLegs() {
        for (int direction : new int[] {-1, 1}) {
            PonyFlightMotion motion = new PonyFlightMotion();
            for (int tick = 0; tick <= 40; tick++) motion.observe(tick, 0, 0, 0, 0, true, false, false);
            close(motion.sample(40.9, 0).frontLift(), 0, 1e-6, "ordinary folded chain starts lowered at root");
            for (int step = 1; step <= 8; step++) {
                motion.observe(40 + step, 0, direction * .015 * step * step, 0, 0, true, false, false);
                for (int sub = 0; sub < 10; sub++) {
                    var pose = motion.sample(40 + step + sub / 10d, 0);
                    require(Math.abs(pose.frontLift()) <= 4, "ordinary whole-chain acceleration remains within four degrees");
                }
            }
            var accelerated = motion.sample(48.9, 0);
            require(accelerated.frontLift() * direction < -2, "upward acceleration lowers folded leg; downward acceleration lifts it");
            for (int tick = 49; tick <= 120; tick++)
                motion.observe(tick, 0, direction * (.96 + (tick - 48) * .225), 0, 0, true, false, false);
            close(motion.sample(120.9, 0).frontLift(), 0, 1e-5, "constant rise/descent restores lowered baseline");
        }
        PonyFlightMotion motion = new PonyFlightMotion();
        for (int tick = 0; tick <= 40; tick++) motion.observe(tick, 0, 0, 0, 0, true, false, false);
        var before = motion.sample(41, 0);
        motion.observe(41, 0, 0, 0, 0, true, false, true);
        close(motion.sample(41, 0).frontLift(), before.frontLift(), 0, "sprint starts without a root snap");
        float previous = motion.sample(41, 0).frontLift();
        for (int tick = 42; tick <= 100; tick++) {
            motion.observe(tick, 0, 0, 0, 0, true, false, true);
            for (int sub = 0; sub < 10; sub++) {
                float lift = motion.sample(tick + sub / 10d, 0).frontLift();
                require(Math.abs(lift - previous) < 6, "sprint raises whole folded leg smoothly");
                previous = lift;
            }
        }
        close(motion.sample(100.9, 0).frontLift(), 30, 1e-5, "sprint restores original folded height by raising thirty degrees");
        before = motion.sample(101, 0);
        motion.observe(101, 0, 0, 0, 0, true, false, false);
        close(motion.sample(101, 0).frontLift(), before.frontLift(), 0, "sprint release does not snap");
        for (int tick = 102; tick <= 180; tick++) motion.observe(tick, 0, 0, 0, 0, true, false, false);
        close(motion.sample(180.9, 0).frontLift(), 0, 1e-5, "sprint release settles thirty degrees lower");
        for (int direction : new int[] {-1, 1}) {
            motion = new PonyFlightMotion();
            for (int tick = 0; tick <= 100; tick++) motion.observe(tick, 0, 0, 0, 0, true, false, true);
            for (int step = 1; step <= 8; step++) {
                motion.observe(100 + step, 0, direction * .015 * step * step, 0, 0, true, false, true);
                var pose = motion.sample(100 + step + .9, 0);
                require(Math.abs(pose.frontLift() - 30) <= 4, "sprint acceleration is also a bounded root-only response");
            }
            require((motion.sample(108.9, 0).frontLift() - 30) * direction < -2, "sprint vertical lag follows the same direction");
        }
    }

    private static void safety() {
        PonyFlightMotion motion = new PonyFlightMotion();
        Random random = new Random(300);
        double x = 0, y = 0, z = 0;
        for (int tick = 0; tick < 15000; tick++) {
            x += random.nextDouble() - .5; y += random.nextDouble() - .5; z += random.nextDouble() - .5;
            motion.observe(tick, x, y, z, random.nextDouble() * 1440 - 720, tick % 100 < 90, true, tick % 40 < 20);
            var pose = motion.sample(tick + .6, .42);
            require(pose.amount() >= 0 && pose.amount() <= 1, "amount bounded");
            require(pose.magic() >= 0 && pose.magic() <= pose.amount(), "magic bounded");
            require(Math.abs(pose.bodyPitch()) <= 10 && Math.abs(pose.bodyRoll()) <= 5, "body limits");
            require(Math.abs(pose.legPitch()) <= 18 && Math.abs(pose.legRoll()) <= 8, "limb limits");
            require(Math.abs(pose.bob()) <= .175, "bob subtle and bounded");
            require(pose.curlDelta() >= (PonyFlightMotion.MIN_CURL - PonyFlightMotion.BASE_CURL) * pose.amount() - 1e-6
                    && pose.curlDelta() <= (PonyFlightMotion.MAX_CURL - PonyFlightMotion.BASE_CURL) * pose.amount() + 1e-6,
                    "curl never exceeds validated standing-to-authored-flight range");
            require(pose.frontLift() >= -PonyFlightMotion.FRONT_ACCEL_DEGREES * pose.amount() - 1e-5
                    && pose.frontLift() <= (PonyFlightMotion.FRONT_LOWER_DEGREES + PonyFlightMotion.FRONT_ACCEL_DEGREES) * pose.amount() + 1e-5,
                    "folded front root remains bounded through normal/sprint transitions");
        }
        motion.observe(15000, 1e6, 1e6, 1e6, 0, true, true);
        require(motion.sample(15000, 0).equals(PonyFlightMotion.Pose.NONE), "teleport clean restart");
        motion.observe(0, 0, 0, 0, 0, true, true);
        require(motion.sample(0, 0).equals(PonyFlightMotion.Pose.NONE), "rewind resets");
        motion.observe(1, Double.NaN, 0, 0, 0, true, true);
        require(motion.sample(1, 0).equals(PonyFlightMotion.Pose.NONE), "invalid safe");
    }

    private static void close(double a, double b, double epsilon, String label) { require(Math.abs(a - b) <= epsilon, label + ": " + a + " != " + b); }
    private static void require(boolean condition, String label) { checks++; if (!condition) throw new AssertionError(label); }
}
