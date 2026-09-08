package top.csituka.magicaland.client.render;

import java.util.List;
import java.util.UUID;
import static top.csituka.magicaland.client.render.LevitationMotion.*;

public final class LevitationMotionTest {
    private static int checks;
    public static void main(String[] args) {
        restAndResponse();
        frameRate();
        limitsAndReset();
        isolation();
        trails();
        fastTrails();
        System.out.println("PASS levitation motion: " + checks + " checks");
    }

    private static void restAndResponse() {
        var motion = new LevitationMotion(WORLD);
        check(motion.sample(ZERO, ZERO, 0, 0, false).reset(), "first frame starts at target");
        for (int i = 1; i <= 240; i++)
            close(motion.sample(ZERO, ZERO, 0, i / 60., false).offset().length(), 0, 1e-12, "no perpetual sine at rest");
        Pose moving = motion.sample(new Point(0, 0, .06), new Point(0, 0, .06), 0, 4.05, false);
        check(moving.offset().z() < -.025, "forward movement leaves item behind");
        Pose up = motion.sample(new Point(0, .12, .06), new Point(0, .12, .06), 5, 4.10, false);
        check(up.offset().y() < -.05, "jump onset leaves item lower");
        check(up.yaw() < 0, "turn has opposite angular lag");
        Pose end = up;
        for (int i = 1; i <= 120; i++) end = motion.sample(new Point(0, .12, .06), new Point(0, .12, .06), 5, 4.10 + i / 60., false);
        check(end.offset().length() < 1e-6 && Math.abs(end.yaw()) < 1e-6, "settles after stopping");
        Pose pause = motion.sample(new Point(0, .12, .06), new Point(0, .12, .06), 5, 6.10, false);
        for (int i = 0; i < 200; i++) {
            Pose same = motion.sample(new Point(0, .12, .06), new Point(0, .12, .06), 5, 6.10, false);
            close(same.offset().subtract(pause.offset()).length(), 0, 1e-12, "paused game clock never advances");
        }
    }

    private static void frameRate() {
        for (Profile profile : List.of(WORLD, FIRST_PERSON)) {
            Pose reference = linear(60, profile);
            for (int fps : new int[] {20, 30, 48, 75, 120, 144, 240}) {
                Pose other = linear(fps, profile);
                close(other.offset().subtract(reference.offset()).length(), 0, 1e-9, "linear target independent of FPS " + fps);
                close(other.yaw(), reference.yaw(), 1e-8, "angular response independent of FPS " + fps);
            }
        }
    }

    private static Pose linear(int fps, Profile profile) {
        var motion = new LevitationMotion(profile);
        motion.sample(ZERO, ZERO, 0, 0, false);
        Pose result = null;
        for (int i = 1; i <= fps; i++) {
            double t = i / (double) fps;
            Point position = new Point(t * .5, t * .15, t * .3);
            result = motion.sample(position, position, t * 20, t, false);
        }
        return result;
    }

    private static void limitsAndReset() {
        for (Profile profile : List.of(WORLD, FIRST_PERSON)) for (boolean using : new boolean[] {false, true}) {
            var motion = new LevitationMotion(profile);
            motion.sample(ZERO, ZERO, 179, 0, using);
            Pose result = null;
            for (int i = 1; i <= 400; i++) {
                double t = i / 120.;
                Point position = new Point(t * 6, Math.sin(t * 3) * .5, t * 2);
                result = motion.sample(position, position, 179 + t * 110, t, using);
                double strength = using ? .22 : 1;
                check(Math.hypot(result.offset().x(), result.offset().z()) <= profile.horizontalLimit() * strength + 1e-9,
                        "horizontal clamp");
                check(Math.abs(result.offset().y()) <= profile.verticalLimit() * strength + 1e-9, "vertical clamp");
                check(Math.abs(result.yaw()) <= profile.angleLimit() * strength + 1e-9, "angle clamp");
                check(result.offset().finite(), "finite under moving target");
            }
            check(motion.sample(new Point(40, 1, 10), new Point(40, 1, 10), 0, 3.4, using).reset(), "teleport resets");
            check(motion.sample(new Point(40.1, 1, 10), new Point(40.1, 1, 10), 0, 4.4, using).reset(), "long render gap resets");
            check(motion.sample(new Point(40.1, 1, 10), new Point(40.1, 1, 10), 0, 4, using).reset(), "backward time resets");
            check(motion.sample(new Point(Double.NaN, 0, 0), ZERO, 0, 5, using).reset(), "invalid input resets safely");
        }
        var wrap = new LevitationMotion(WORLD);
        wrap.sample(ZERO, ZERO, 179, 0, false);
        Pose crossing = wrap.sample(ZERO, ZERO, -179, .05, false);
        check(!crossing.reset() && Math.abs(crossing.yaw()) < 2.1, "yaw wrap takes shortest path");
    }

    private static void isolation() {
        var store = new Store();
        Object world = new Object();
        var main = new Key(new UUID(1, 1), true, false);
        var off = new Key(new UUID(1, 1), false, false);
        var other = new Key(new UUID(2, 2), true, false);
        var first = new Key(main.player(), true, true);
        store.beginFrame(world, true);
        var entry = store.acquire(main, "sword1", WORLD);
        var initial = store.sample(entry, ZERO, ZERO, 0, 0, false);
        check(initial == store.sample(entry, new Point(1, 0, 0), ZERO, 90, .05, false), "same-frame multipass cannot advance");
        check(store.acquire(off, "sword1", WORLD) != entry, "logical hands isolated even same item");
        check(store.acquire(other, "sword1", WORLD) != entry, "players isolated");
        check(store.acquire(first, "sword1", FIRST_PERSON) != entry, "view context isolated");
        check(store.size() == 4, "four independent states");
        store.beginFrame(world, true);
        entry = store.acquire(main, "sword1", WORLD);
        store.sample(entry, new Point(.15, 0, 0), new Point(.15, 0, 0), 4, .05, false);
        var points = store.recordTrail(entry, new Point(.15, 0, 0), .05, true, false);
        check(points == store.recordTrail(entry, new Point(99, 0, 0), .08, true, false), "same-frame trail only records once");
        check(store.acquire(main, "sword2", WORLD) != entry, "changed item/NBT identity resets state");
        entry = store.acquire(main, "sword2", WORLD);
        store.sample(entry, ZERO, ZERO, 0, .1, false);
        store.beginFrame(world, true);
        store.beginFrame(world, true);
        entry = store.acquire(main, "sword2", WORLD);
        check(entry.previousPlayer == null && entry.speed == 0 && entry.pose == null, "invisible return clears driver cache");
        check(store.sample(entry, new Point(.5, 0, 0), new Point(.5, 0, 0), 12, .2, false).reset(), "return snaps without chase");
        store.beginFrame(world, false);
        check(store.size() == 0, "perspective switch clears all states");
        store.acquire(main, "sword", WORLD);
        store.beginFrame(new Object(), false);
        check(store.size() == 0, "world change clears states");
        for (int i = 0; i < 1400; i++) store.acquire(new Key(new UUID(3, i), true, false), "item", WORLD);
        check(store.size() <= 512, "state memory bounded");
        for (int i = 0; i < 121; i++) store.beginFrame(world, false);
        check(store.size() == 0, "unseen state eviction");
    }

    private static void trails() {
        for (Profile profile : List.of(WORLD, FIRST_PERSON)) {
            var motion = new LevitationMotion(profile);
            motion.sample(ZERO, ZERO, 0, 0, false);
            List<TrailPoint> trail = List.of();
            for (int i = 1; i <= 120; i++) {
                double t = i / 120.;
                Point actual = new Point(t * 4, .2, 0);
                motion.sample(actual, actual, 0, t, false);
                trail = motion.trail(actual, t, true, 4, false);
                check(trail.size() <= 9, "tail bounded to eight segments");
                double length = 0;
                for (int j = 1; j < trail.size(); j++) length += trail.get(j).position().subtract(trail.get(j - 1).position()).length();
                check(length <= profile.trailLength() + 1e-8, "tail length clamp");
                for (var point : trail) check(point.alpha() >= 0 && point.alpha() <= .16f, "small bounded tail opacity");
            }
            check(trail.size() >= 2, "moving sprint emits a short trail");
            check(motion.trail(new Point(4, .2, 0), 1 + profile.trailLife() + .001, false, 0, false).isEmpty(), "stop tail fades completely");
            motion.clear();
            motion.sample(ZERO, ZERO, 0, 0, false);
            for (int i = 1; i < 20; i++) {
                check(motion.trail(new Point(i * .05, 0, 0), i * .05, false, 4, false).isEmpty(), "walking never emits sprint tail");
                check(motion.trail(new Point(i * .05, 0, 0), i * .05, true, 0, false).isEmpty(), "stationary sprint flag never emits");
                check(motion.trail(new Point(i * .05, 0, 0), i * .05, true, 4, true).isEmpty(), "using item suppresses tail");
            }
        }
    }

    private static void fastTrails() {
        for (Profile profile : List.of(WORLD, FIRST_PERSON)) for (int fps : new int[] {30, 60})
            for (double speed : new double[] {5.6, 10, 20}) {
                var motion = new LevitationMotion(profile);
                motion.sample(ZERO, ZERO, 0, 0, false);
                motion.trail(ZERO, 0, true, speed, false);
                for (int i = 1; i < fps * 3; i++) {
                    double time = i / (double) fps;
                    Point actual = new Point(speed * time, .2, 0);
                    motion.sample(actual, actual, 0, time, false);
                    List<TrailPoint> trail = motion.trail(actual, time, true, speed, false);
                    check(trail.size() >= 2, "high speed trail never drops to one point at " + speed + "/" + fps);
                    close(trail.get(trail.size() - 1).position().subtract(actual).length(), 0, 1e-9, "tail head stays at actual center");
                    double length = 0;
                    for (int j = 1; j < trail.size(); j++) length += trail.get(j).position().subtract(trail.get(j - 1).position()).length();
                    check(length <= profile.trailLength() + 1e-8, "fast tail interpolated length clamp");
                    check(length > .01, "fast tail has visible length");
                }
            }
    }

    private static void close(double a, double b, double tolerance, String message) { check(Math.abs(a - b) <= tolerance, message + " " + a + " != " + b); }
    private static void check(boolean valid, String message) { checks++; if (!valid) throw new AssertionError(message); }
}
