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
        softEdges();
        walkRunStopAndReverse();
        keyedHover();
        keyedHoverWhileMoving();
        trails();
        fastTrails();
        movingTrailModes();
        strongerResponse();
        System.out.println("PASS levitation motion: " + checks + " checks");
    }

    private static void restAndResponse() {
        var motion = new LevitationMotion(WORLD);
        check(motion.sample(ZERO, ZERO, 0, 0, false).reset(), "first frame starts at target");
        for (int i = 1; i <= 240; i++)
            close(motion.sample(ZERO, ZERO, 0, i / 60., false).offset().length(), 0, 1e-12, "unkeyed spring has no artificial target motion");
        Pose moving = motion.sample(new Point(0, 0, .06), new Point(0, 0, .06), 0, 4.05, false);
        check(moving.offset().z() < -.025, "forward movement leaves item behind");
        Pose up = motion.sample(new Point(0, .12, .06), new Point(0, .12, .06), 5, 4.10, false);
        check(up.offset().y() < -.05, "jump onset leaves item lower");
        check(up.yaw() < 0, "turn has opposite angular lag");
        Pose end = up;
        for (int i = 1; i <= 120; i++) end = motion.sample(new Point(0, .12, .06), new Point(0, .12, .06), 5, 4.10 + i / 60., false);
        check(end.offset().length() < 1e-5 && Math.abs(end.yaw()) < 1e-4, "settles after stopping: " + end);
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
                check(trail.size() <= MAX_TRAIL_POINTS, "tail point count bounded");
                double length = 0;
                for (int j = 1; j < trail.size(); j++) length += trail.get(j).position().subtract(trail.get(j - 1).position()).length();
                check(length <= motion.trailLength() + 1e-8, "tail length clamp");
                for (var point : trail) check(point.alpha() >= 0 && point.alpha() <= TRAIL_ALPHA, "bounded tail opacity");
            }
            check(trail.size() >= 2, "moving player emits a trail even below sprint speed");
            check(motion.trail(new Point(4, .2, 0), 1 + profile.trailLife() * 2 + .001, false, 0, false).isEmpty(), "stop tail fades completely");
            motion.clear();
            motion.sample(ZERO, ZERO, 0, 0, false);
            for (int i = 1; i < 20; i++) {
                check(motion.trail(new Point(i * .05, 0, 0), i * .05, true, 0, false).isEmpty(), "stationary sprint flag never emits");
                check(motion.trail(new Point(i * .05, 0, 0), i * .05, true, 4, true).isEmpty(), "using item suppresses tail");
            }
        }
    }

    private static void fastTrails() {
        for (Profile profile : List.of(WORLD, FIRST_PERSON)) for (int fps : new int[] {30, 60, 144})
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
                    check(length <= motion.trailLength() + 1e-8, "fast tail interpolated length clamp");
                    check(length > .01, "fast tail has visible length");
                }
            }
    }

    private static void softEdges() {
        Constraint center = constrain(new Point(.1, .02, 0), new Point(1, 2, 3), .42, .26, 1 / 60., 11);
        close(center.velocity().subtract(new Point(1, 2, 3)).length(), 0, 1e-12, "central motion is not damped by boundary");
        Constraint edge = constrain(new Point(.4, 0, 0), new Point(1, 2, 3), .42, .26, 1 / 60., 11);
        check(edge.velocity().x() < 1, "soft zone progressively brakes outward speed");
        close(edge.velocity().y(), 2, 1e-12, "horizontal edge preserves vertical velocity");
        close(edge.velocity().z(), 3, 1e-12, "edge preserves tangential velocity");
        Constraint inward = constrain(new Point(.5, 0, 0), new Point(-2, .3, 3), .42, .26, 1 / 60., 11);
        close(inward.offset().x(), .42, 1e-12, "outer limit remains a safety boundary");
        check(inward.velocity().x() <= -2, "hard limit preserves inward chase rather than clearing it");
        close(inward.velocity().z(), 3, 1e-12, "hard limit preserves tangent");
        Constraint outward = constrain(new Point(.5, 0, 0), new Point(8, .3, 3), .42, .26, 1 / 60., 11);
        check(outward.velocity().x() <= 0, "hard limit removes only outward normal velocity");
        close(outward.velocity().y(), .3, 1e-12, "hard limit never zeros all axes");
        double[] before = constrainAxis(.42 * .72 - 1e-7, 1, .42, 1 / 60., 11);
        double[] after = constrainAxis(.42 * .72 + 1e-7, 1, .42, 1 / 60., 11);
        close(before[1], after[1], 1e-8, "soft zone starts continuously");
        for (int i = 0; i < 360; i++) {
            double angle = i * Math.PI / 180;
            Point normal = new Point(Math.cos(angle), 0, Math.sin(angle));
            Point tangent = new Point(-normal.z(), 0, normal.x());
            Constraint result = constrain(normal.multiply(.5), normal.multiply(2).add(tangent.multiply(3)), .42, .26, 1 / 60., 11);
            close(result.offset().length(), .42, 1e-12, "circular soft limit has no axis bias");
            close(result.velocity().x() * tangent.x() + result.velocity().z() * tangent.z(), 3, 1e-12, "all directions keep tangent");
        }
    }

    private static void walkRunStopAndReverse() {
        for (Profile profile : List.of(WORLD, FIRST_PERSON)) {
            List<Pose> reference = course(profile, 60);
            for (int fps : new int[] {30, 60, 144}) {
                List<Pose> poses = course(profile, fps);
                double overshoot = 0, reverseLag = 0;
                for (int i = 0; i < poses.size(); i++) {
                    Pose pose = poses.get(i);
                    check(pose.offset().finite() && Double.isFinite(pose.yaw()), "course is finite at " + fps);
                    check(Math.hypot(pose.offset().x(), pose.offset().z()) <= profile.horizontalLimit() + 1e-9, "course horizontal bound");
                    check(Math.abs(pose.offset().y()) <= profile.verticalLimit() + 1e-9, "course vertical bound");
                    check(Math.abs(pose.yaw()) <= profile.angleLimit() + 1e-9, "course angular bound");
                    double time = i / (double) fps;
                    if (time > 2 && time < 3) overshoot = Math.max(overshoot, pose.offset().x());
                    if (time > 3.2 && time < 4) reverseLag = Math.max(reverseLag, pose.offset().x());
                    if (i % (fps / 6) == 0) {
                        int refIndex = (int) Math.round(time * 60);
                        close(pose.offset().subtract(reference.get(refIndex).offset()).length(), 0, .018,
                                "nonlinear course agrees across frame rates " + fps);
                        close(pose.yaw(), reference.get(refIndex).yaw(), .25, "turn course agrees across frame rates");
                    }
                }
                check(overshoot > profile.horizontalLimit() * .02, "stop produces a visible but limited chase rebound " + fps + " " + overshoot);
                check(overshoot < profile.horizontalLimit() * .65, "stop rebound never becomes a large oscillation");
                check(reverseLag > profile.horizontalLimit() * .65, "reverse movement develops opposite lag");
                Pose settled = poses.get(poses.size() - 1);
                check(settled.offset().length() < .002, "long-run stop settles without drift");
                System.out.printf(java.util.Locale.ROOT, "course %s %dfps: stop overshoot %.5f, reverse lag %.5f%n",
                        profile == WORLD ? "world" : "first", fps, overshoot, reverseLag);
            }
            var motion = new LevitationMotion(profile);
            motion.sample(ZERO, ZERO, 0, 0, false);
            for (int i = 1; i <= 240; i++) {
                double time = i / 60.;
                Point target = new Point(time * 5.6, Math.sin(time * 5) * .6, 0);
                boolean using = i >= 60 && i < 180;
                Pose pose = motion.sample(target, target, time * 100, time, using);
                double strength = using ? .22 : 1;
                check(Math.hypot(pose.offset().x(), pose.offset().z()) <= profile.horizontalLimit() * strength + 1e-9, "enter/leave item-use keeps strict bounds");
                check(Math.abs(pose.offset().y()) <= profile.verticalLimit() * strength + 1e-9, "using jump offset remains tight");
                check(Math.abs(pose.yaw()) <= profile.angleLimit() * strength + 1e-9, "using rotation remains tight");
            }
            var turn = new LevitationMotion(profile);
            turn.sample(ZERO, ZERO, 0, 0, false);
            check(turn.sample(ZERO, ZERO, 150, 1 / 60., false).reset(), "extreme one-frame turn still resets");
        }
    }

    private static List<Pose> course(Profile profile, int fps) {
        var motion = new LevitationMotion(profile);
        var poses = new java.util.ArrayList<Pose>();
        for (int i = 0; i <= fps * 7; i++) {
            double time = i / (double) fps;
            double x = time < 2 ? time * 5.6 : time < 3 ? 11.2 : time < 5 ? 11.2 - (time - 3) * 5.6 : 0;
            double y = time > .8 && time < 1.6 ? Math.sin((time - .8) / .8 * Math.PI) : 0;
            double yaw = time < .5 ? 0 : time < 1.5 ? (time - .5) * 120 : 120;
            Point target = new Point(x, y, 0);
            poses.add(motion.sample(target, target, yaw, time, false));
        }
        return poses;
    }

    private static void keyedHover() {
        java.util.Random random = new java.util.Random(20260909);
        for (int player = 0; player < 32; player++) {
            UUID id = new UUID(random.nextLong(), random.nextLong());
            for (Profile profile : List.of(WORLD, FIRST_PERSON)) {
                Point main = null, off = null;
                for (boolean hand : new boolean[] {true, false}) {
                    Key key = new Key(id, hand, profile == FIRST_PERSON);
                    Point reference = null;
                    for (int fps : new int[] {30, 60, 144}) {
                        var motion = new LevitationMotion(profile, key);
                        Pose initial = motion.sample(ZERO, ZERO, 0, 12, false);
                        close(initial.offset().length(), 0, 1e-12, "hover fades in after reset, no initial pop");
                        Pose pose = null;
                        double distance = 0;
                        Point previous = ZERO;
                        for (int i = 1; i <= fps * 3; i++) {
                            pose = motion.sample(ZERO, ZERO, 0, 12 + i / (double) fps, false);
                            check(Math.hypot(pose.offset().x(), pose.offset().z()) <= profile.horizontalLimit() * .065,
                                    "keyed hover is much smaller than movement lag");
                            check(Math.abs(pose.offset().y()) <= profile.verticalLimit() * .475 + 1e-12, "fivefold hover stays within existing vertical safety limit");
                            close(pose.offset().y(), previousHoverY(key, profile, 12 + i / (double) fps, 12) * 5,
                                    1e-12, "vertical idle hover is exactly five times the previous formula");
                            distance += pose.offset().subtract(previous).length();
                            previous = pose.offset();
                        }
                        check(distance > .001, "idle animation has perceptible nonzero motion");
                        if (reference == null) reference = pose.offset();
                        else close(reference.subtract(pose.offset()).length(), 0, 1e-12, "hover follows game time, not render rate");
                        Pose paused = pose;
                        for (int i = 0; i < 10; i++) close(motion.sample(ZERO, ZERO, 0, 15, false).offset()
                                .subtract(paused.offset()).length(), 0, 1e-12, "paused hover is frozen");
                        close(motion.sample(ZERO, ZERO, 0, 15 + 1 / 60., true).offset().length(), 0, 1e-12, "using item suppresses idle distraction");
                        check(motion.sample(new Point(10, 0, 0), new Point(10, 0, 0), 0, 16, false).reset(), "keyed hover respects teleport reset");
                    }
                    if (hand) main = reference; else off = reference;
                }
                check(main.subtract(off).length() > 1e-7, "hands do not hover in mechanical unison");
            }
        }
        var store = new Store();
        var world = new Object();
        Key key = new Key(new UUID(9, 20), true, false);
        Pose atRest = null;
        for (int i = 0; i <= 60; i++) {
            store.beginFrame(world, false);
            Entry entry = store.acquire(key, "same", WORLD);
            atRest = store.sample(entry, ZERO, ZERO, 0, i / 60., false);
            check(atRest == store.sample(entry, new Point(99, 0, 0), ZERO, 90, i / 60. + .05, false),
                    "production keyed hover is sampled once for all render passes");
        }
        check(atRest.offset().length() > .0001, "actual Store path enables keyed hover");
    }

    private static double previousHoverY(Key key, Profile profile, double seconds, double started) {
        long seed = key.player().getMostSignificantBits() ^ Long.rotateLeft(key.player().getLeastSignificantBits(), 21)
                ^ (key.mainHand() ? 0x632be59bd9b4e019L : 0x9e3779b97f4a7c15L);
        seed = (seed ^ (seed >>> 30)) * 0xbf58476d1ce4e5b9L;
        seed = (seed ^ (seed >>> 27)) * 0x94d049bb133111ebL;
        seed ^= seed >>> 31;
        double phase = (seed >>> 11) * 0x1.0p-53 * Math.PI * 2;
        double fade = Math.max(0, Math.min(1, (seconds - started) / .4));
        fade = fade * fade * (3 - 2 * fade);
        double wave = .63 * Math.sin(seconds * 1.08 + phase * 1.3) + .37 * Math.sin(seconds * .61 + phase);
        return wave * profile.verticalLimit() * .095 * fade;
    }

    private static void keyedHoverWhileMoving() {
        for (Profile profile : List.of(WORLD, FIRST_PERSON)) for (int fps : new int[] {30, 60, 144}) {
            var key = new Key(new UUID(92, 771), true, profile == FIRST_PERSON);
            var motion = new LevitationMotion(profile, key);
            for (int i = 0; i <= fps * 10; i++) {
                double time = i / (double) fps;
                Point target = new Point(time * 5.6, Math.sin(time * 5) * .6, Math.sin(time * 2) * .4);
                boolean using = time > 3 && time < 6;
                Pose pose = motion.sample(target, target, time * 100, time, using);
                double strength = using ? .22 : 1;
                check(Math.hypot(pose.offset().x(), pose.offset().z()) <= profile.horizontalLimit() * strength + 1e-9,
                        "fivefold hover plus moving inertia preserves horizontal limits");
                check(Math.abs(pose.offset().y()) <= profile.verticalLimit() * strength + 1e-9,
                        "fivefold hover plus jumping inertia preserves original vertical limits");
                check(Math.abs(pose.yaw()) <= profile.angleLimit() * strength + 1e-9,
                        "hover never loosens angular limits");
                check(pose.offset().finite(), "strong hover remains finite during movement and use transitions");
                if (using) close(motion.hover(time, true).length(), 0, 0, "using still completely disables idle wave");
            }
        }
    }

    private static void movingTrailModes() {
        for (Profile profile : List.of(WORLD, FIRST_PERSON)) for (int fps : new int[] {30, 60, 144}) {
            var motion = new LevitationMotion(profile);
            motion.sample(ZERO, ZERO, 0, 0, false);
            motion.trail(ZERO, 0, false, 0, false);
            List<TrailPoint> points = List.of();
            Point actual = ZERO;
            for (int i = 1; i <= fps * 3; i++) {
                double t = i / (double) fps;
                actual = new Point(t * 4.317, 0, 0);
                motion.sample(actual, actual, 0, t, false);
                points = motion.trail(actual, t, false, 4.317, false);
                check(points.size() <= MAX_TRAIL_POINTS, "walking points remain bounded");
            }
            check(points.size() > 2, "walking emits without sprint flag");
            close(trailLength(points), profile.trailLength() * .75, 1e-6, "walking uses three quarters previous length");
            close(points.get(points.size() - 1).alpha(), TRAIL_ALPHA * .8, 1e-7, "walking head is slightly dimmer");
            close(motion.trailWidth(), profile.trailWidth(), 1e-8, "walking width baseline is unchanged");
            double previousWidth = motion.trailWidth();
            for (int i = 1; i <= fps * 3; i++) {
                double t = 3 + i / (double) fps;
                actual = new Point(3 * 4.317 + (t - 3) * 5.6, 0, 0);
                motion.sample(actual, actual, 0, t, false);
                points = motion.trail(actual, t, true, 5.6, false);
                check(motion.trailWidth() >= previousWidth - 1e-8, "sprint grows continuously");
                check(motion.trailWidth() - previousWidth < profile.trailWidth() * .09, "one frame never jumps to sprint width");
                previousWidth = motion.trailWidth();
                check(points.size() <= MAX_TRAIL_POINTS, "long sprint keeps the existing geometry budget");
                check(trailLength(points) <= motion.trailLength() + 1e-8, "dynamic length stays clamped");
            }
            close(trailLength(points), profile.trailLength() * 2, 1e-5, "full sprint reaches twice previous length");
            close(motion.trailWidth(), profile.trailWidth() * 1.5, 1e-7, "full sprint is fifty percent wider");
            close(points.get(points.size() - 1).alpha(), TRAIL_ALPHA, 1e-7, "sprint returns full previous peak opacity");
            Point stopHead = actual;
            float lastAlpha = points.get(points.size() - 1).alpha();
            double stoppedWidth = motion.trailWidth();
            for (int i = 1; i <= fps; i++) {
                double t = 6 + i / (double) fps;
                motion.sample(stopHead, stopHead, 0, t, false);
                points = motion.trail(stopHead.add(new Point(0, .1 * Math.sin(t), 0)), t, false, 0, false);
                close(motion.trailWidth(), stoppedWidth, 1e-8, "stop fades existing trail without abruptly shrinking width");
                if (!points.isEmpty()) {
                    check(points.get(points.size() - 1).position().subtract(stopHead).length() < .19,
                            "stopped trail endpoint stays in existing world history instead of following idle hover");
                    float nextAlpha = points.get(points.size() - 1).alpha();
                    check(nextAlpha <= lastAlpha, "stop opacity only decays");
                    lastAlpha = nextAlpha;
                }
            }
            check(points.isEmpty(), "both profiles fully fade within one second");
            previousWidth = motion.trailWidth();
            for (int i = 1; i <= fps * 3; i++) {
                double t = 7 + i / (double) fps;
                actual = stopHead.add(new Point((t - 7) * 4.317, 0, 0));
                motion.sample(actual, actual, 0, t, false);
                points = motion.trail(actual, t, false, 4.317, false);
                check(motion.trailWidth() <= previousWidth + 1e-8, "returning to walk shrinks smoothly");
                check(previousWidth - motion.trailWidth() < profile.trailWidth() * .09, "resuming walk does not pop to narrow width");
                previousWidth = motion.trailWidth();
            }
            close(trailLength(points), profile.trailLength() * .75, 1e-5, "walk recovers its short tail after sprint");
            close(motion.trailWidth(), profile.trailWidth(), 1e-7, "walk recovers its narrow width after sprint");
            motion.clear();
            motion.sample(ZERO, ZERO, 0, 0, false);
            for (int i = 1; i <= fps; i++) {
                double t = i / (double) fps;
                check(motion.trail(new Point(.03 * Math.sin(t), .05 * Math.cos(t), 0), t, true, 0, false).isEmpty(),
                        "idle hover never emits even when sprint flag remains set");
                check(motion.trail(new Point(t * 5.6, 0, 0), t, true, 5.6, true).isEmpty(), "item use blocks new trails");
            }
            check(motion.trail(ZERO, 2, true, Double.NaN, false).isEmpty(), "invalid speed never emits");
            check(motion.trail(ZERO, Double.NaN, true, 5.6, false).isEmpty(), "invalid time never emits");
        }
    }

    private static void strongerResponse() {
        for (Profile profile : List.of(WORLD, FIRST_PERSON)) {
            Profile old = profile == WORLD ? new Profile(11, .42, .26, 14, .4, 1.35, .055)
                    : new Profile(16, .15, .11, 6, .22, .55, .022);
            var current = new LevitationMotion(profile);
            var previous = new LevitationMotion(old);
            current.sample(ZERO, ZERO, 0, 0, false);
            previous.sample(ZERO, ZERO, 0, 0, false);
            double currentLag = 0, oldLag = 0, currentVertical = 0, oldVertical = 0;
            for (int i = 1; i <= 60; i++) {
                double t = i / 60.;
                Point target = new Point(t * 4.317, t * 2, 0);
                Pose now = current.sample(target, target, 70 * t, t, false);
                Pose before = previous.sample(target, target, 70 * t, t, false);
                currentLag = Math.max(currentLag, Math.abs(now.offset().x()));
                oldLag = Math.max(oldLag, Math.abs(before.offset().x()));
                currentVertical = Math.max(currentVertical, Math.abs(now.offset().y()));
                oldVertical = Math.max(oldVertical, Math.abs(before.offset().y()));
            }
            check(currentLag > oldLag * 1.25, "horizontal inertia is materially stronger than previous release");
            check(currentVertical > oldVertical * 1.25, "jump inertia is materially stronger than previous release");
            check(profile.frequency() < old.frequency(), "chasing response has slowed");
        }
        check(FIRST_PERSON.horizontalLimit() <= WORLD.horizontalLimit() * .4, "first-person horizontal motion stays restrained");
        check(FIRST_PERSON.verticalLimit() <= WORLD.verticalLimit() * .45, "first-person jumping stays restrained");
    }

    private static double trailLength(List<TrailPoint> points) {
        double length = 0;
        for (int i = 1; i < points.size(); i++) length += points.get(i).position().subtract(points.get(i - 1).position()).length();
        return length;
    }

    private static void close(double a, double b, double tolerance, String message) { check(Math.abs(a - b) <= tolerance, message + " " + a + " != " + b); }
    private static void check(boolean valid, String message) { checks++; if (!valid) throw new AssertionError(message); }
}
