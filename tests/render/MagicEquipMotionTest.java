package top.csituka.magicaland.client.render;

public final class MagicEquipMotionTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static void near(double actual, double expected) {
        check(Math.abs(actual - expected) < 1e-5, actual + " != " + expected);
    }
    public static void main(String[] args) {
        near(MagicEquipMotion.DURATION, .25);
        near(MagicEquipMotion.ENTRANCE_DELAY, .05);
        near(MagicEquipMotion.SCALE_DURATION, .10);
        MagicEquipMotion existing = new MagicEquipMotion();
        existing.observe(true, 4);
        near(existing.progress(4), 1);
        MagicEquipMotion state = new MagicEquipMotion();
        state.observe(false, 0);
        state.observe(true, 1);
        near(state.progress(1), 0);
        near(state.progress(1.05), 0);
        near(state.progress(1.175), .5);
        near(state.progress(1.30), 1);
        state.observe(true, 1.6);
        near(state.progress(1.6), 1);
        state.observe(false, 2);
        near(state.progress(2), 1);
        near(state.progress(2.125), .5);
        near(state.progress(2.25), 0);
        for (int fps : new int[] {20, 30, 60, 144}) {
            var phase = new MagicEquipMotion();
            phase.observe(false, 0);
            phase.observe(true, 1);
            float previous = 0;
            for (int frame = 0; frame <= fps; frame++) {
                double now = 1 + frame / (double) fps;
                phase.observe(true, now);
                float p = phase.progress(now);
                check(p >= previous && p >= 0 && p <= 1, "monotonic entrance");
                previous = p;
            }
            phase.observe(false, 2);
            for (int frame = 0; frame <= fps; frame++) {
                double now = 2 + frame / (double) fps;
                float p = phase.progress(now);
                check(p <= previous && p >= 0 && p <= 1, "monotonic exit");
                previous = p;
            }
        }
        state.observe(true, 3);
        state.observe(false, 3.15);
        near(state.progress(3.15), .4);
        near(state.progress(3.2), .2);
        state.observe(true, 3.2);
        near(state.progress(3.2), .2);
        near(state.progress(3.6), 1);
        state.observe(false, 4);
        state.observe(true, 4.2);
        state.settle(4.2);
        near(state.progress(4.2), 1);
        state.observe(false, 1);
        near(state.progress(1), 0);
        near(state.progress(Double.NaN), 0);
        for (int i = 0; i <= 1000; i++) {
            float p = i / 1000f;
            for (boolean first : new boolean[] {false, true}) {
                var a = MagicEquipMotion.offset(p, false, first);
                var b = MagicEquipMotion.offset(p, true, first);
                check(a.finite() && b.finite(), "finite route");
                near(a.x(), -b.x()); near(a.y(), b.y()); near(a.z(), b.z());
                check(a.length() <= 1.7, "bounded route");
            }
            near(MagicEquipMotion.flight(p), MagicEquipMotion.flight(1 - (1 - p)));
        }
        near(MagicEquipMotion.offset(1, true, true).length(), 0);
        near(MagicEquipMotion.offset(1, false, false).length(), 0);
        near(MagicEquipMotion.flight(.2f), .104);
        deliberateHiddenPause();
        partialReversal();
        exchange();
        arrivalMomentum();
        fullFlightVelocity();
        scaleAndHandoff();
        renderClock();
        System.out.println("PASS MagicEquipMotionTest: " + checks + " checks");
    }

    private static void deliberateHiddenPause() {
        near(MagicEquipMotion.flight(0), 0);
        near(MagicEquipMotion.flight(1), 1);
        for (boolean first : new boolean[] {false, true}) for (boolean left : new boolean[] {false, true})
            for (int fps : new int[] {20, 30, 60, 144}) {
                var phase = new MagicEquipMotion();
                var motion = new LevitationMotion(first ? LevitationMotion.FIRST_PERSON : LevitationMotion.WORLD);
                phase.observe(false, 0); phase.observe(true, 1);
                var start = MagicEquipMotion.offset(0, left, first);
                motion.sample(MagicEquipMotion.springTarget(0, left, first), LevitationMotion.ZERO, 0, 1, false);
                double previous = 0;
                for (int frame = 1; frame / (double) fps <= .15 + 1e-6; frame++) {
                    double time = 1 + frame / (double) fps;
                    float progress = phase.progress(time);
                    near(progress, Math.max(0, (time - 1 - MagicEquipMotion.ENTRANCE_DELAY) / MagicEquipMotion.DURATION));
                    var path = MagicEquipMotion.offset(progress, left, first);
                    var pose = motion.sample(MagicEquipMotion.springTarget(progress, left, first), LevitationMotion.ZERO, 0, time, false);
                    var visual = path.add(pose.offset().multiply(MagicEquipMotion.flight(progress)));
                    double travelled = visual.subtract(start).length();
                    if (time <= 1 + MagicEquipMotion.ENTRANCE_DELAY + 1e-8) {
                        near(travelled, 0);
                        check(MagicEquipMotion.scale(progress) <= MagicEquipMotion.MIN_VISIBLE_SCALE,
                                "intentional preflight pause is completely hidden");
                    } else check(travelled > previous + 1e-6, "flight moves immediately after intentional hidden pause");
                    previous = travelled;
                }
                phase.observe(false, 1.5);
                float almostGone = phase.progress(1.75 - 1e-6);
                check(almostGone > 0, "reverse stays drawable until full duration");
                near(MagicEquipMotion.offset(almostGone, left, first).subtract(start).length(), 0);
                near(phase.progress(1.75), 0);
            }
        for (double time : new double[] {1, 1.01, 1.049999, 1.05, 1.075, 1.10, 1.15, 1.30}) {
            var phase = new MagicEquipMotion();
            phase.observe(false, 0); phase.observe(true, 1);
            float progress = phase.progress(time), scale = MagicEquipMotion.scale(progress);
            for (int pass = 0; pass < 20; pass++) {
                phase.observe(true, time);
                near(phase.progress(time), progress);
                near(MagicEquipMotion.scale(phase.progress(time)), scale);
            }
        }
        var cancelled = new MagicEquipMotion();
        cancelled.observe(false, 0); cancelled.observe(true, 1); cancelled.observe(false, 1.025);
        near(cancelled.progress(1.025), 0); near(cancelled.progress(1.2), 0);
        cancelled.observe(true, 1.3);
        near(cancelled.progress(1.35), 0); near(cancelled.progress(1.4), .2);
    }

    private static void partialReversal() {
        for (double forwardTime : new double[] {.025, .06, .10, .15, .20, .275}) {
            var phase = new MagicEquipMotion();
            phase.observe(false, 0); phase.observe(true, 1);
            double turn = 1 + forwardTime;
            float before = phase.progress(turn), beforeScale = MagicEquipMotion.scale(before);
            phase.observe(false, turn);
            near(phase.progress(turn), before);
            near(MagicEquipMotion.scale(phase.progress(turn)), beforeScale);
            double reverseDuration = before * MagicEquipMotion.DURATION;
            near(phase.progress(turn + reverseDuration), 0);
            if (before == 0) continue;
            double resume = turn + reverseDuration / 2;
            float midReturn = phase.progress(resume);
            phase.observe(true, resume);
            near(phase.progress(resume), midReturn);
            near(phase.progress(resume + .01), Math.min(1, midReturn + .01 / MagicEquipMotion.DURATION));
            near(phase.progress(resume + (1 - midReturn) * MagicEquipMotion.DURATION), 1);
        }
    }

    private static void exchange() {
        for (int a = 0; a < 3; a++) for (int b = 0; b < 3; b++)
            for (int nextA = 0; nextA < 3; nextA++) for (int nextB = 0; nextB < 3; nextB++) {
                boolean exchanged = MagicEquipMotion.exchanged(nextA == a, nextB == b, nextA == b, nextB == a);
                check(exchanged == (a != b && nextA == b && nextB == a), "only true hand swaps bypass entrance");
            }
        var main = new MagicEquipMotion(); var off = new MagicEquipMotion();
        main.observe(false, 0); off.observe(false, 0);
        main.observe(true, 1);
        near(main.progress(1.1), .2);
        main.observe(false, 1.1); off.observe(true, 1.1);
        main.settle(1.1); off.settle(1.1);
        near(main.progress(1.1), 0); near(off.progress(1.1), 1);
        main.observe(true, 1.2); off.observe(false, 1.2);
        main.settle(1.2); off.settle(1.2);
        near(main.progress(1.2), 1); near(off.progress(1.2), 0);
        main.observe(false, 2);
        near(main.progress(2.125), .5);
        near(main.progress(2.25), 0);
        main.observe(true, 3);
        near(main.progress(3.125), .3);
        for (double time : new double[] {3.01, 3.04, 3.1, 3.2}) {
            var swapped = new MagicEquipMotion();
            swapped.observe(false, 0); swapped.observe(true, 3);
            swapped.settle(time);
            near(swapped.progress(time), 1);
            near(MagicEquipMotion.scale(swapped.progress(time)), 1);
            swapped.observe(true, time + .01);
            near(swapped.progress(time + .01), 1);
        }
    }

    private static void arrivalMomentum() {
        for (boolean first : new boolean[] {false, true}) for (int fps : new int[] {30, 60, 144}) {
            var phase = new MagicEquipMotion();
            var profile = first ? LevitationMotion.FIRST_PERSON : LevitationMotion.WORLD;
            var motion = new LevitationMotion(profile);
            phase.observe(false, 0); phase.observe(true, 1);
            double overshoot = 0;
            LevitationMotion.Point previous = null;
            for (int frame = 0; frame <= fps * 2; frame++) {
                double seconds = 1 + frame / (double) fps;
                float p = phase.progress(seconds);
                var path = MagicEquipMotion.offset(p, false, first);
                var spring = motion.sample(MagicEquipMotion.springTarget(p, false, first), LevitationMotion.ZERO, 0, seconds, false);
                var visual = path.add(spring.offset().multiply(MagicEquipMotion.flight(p)));
                check(visual.finite(), "finite incoming inertia");
                check(Math.hypot(spring.offset().x(), spring.offset().z()) <= profile.horizontalLimit() + 1e-6,
                        "arrival horizontal safety bound");
                check(Math.abs(spring.offset().y()) <= profile.verticalLimit() + 1e-6, "arrival vertical safety bound");
                if (seconds >= 1.30) overshoot = Math.max(overshoot, first ? -visual.x() : -visual.z());
                if (previous != null && seconds > 1.30)
                    check(visual.subtract(previous).length() < 12.0 / fps, "bounded arrival velocity");
                previous = visual;
            }
            check(overshoot > .004, "visible residual arrival momentum even when player is standing");
            check(previous.length() < .001, "arrival settles without permanent offset");

            var precisePhase = new MagicEquipMotion();
            var preciseMotion = new LevitationMotion(profile);
            precisePhase.observe(false, 0); precisePhase.observe(true, 1);
            for (int frame = 0; frame < 300; frame++) {
                double seconds = 1 + frame / 1000d;
                var exactPath = MagicEquipMotion.offset(precisePhase.progress(seconds), false, first);
                preciseMotion.sample(MagicEquipMotion.springTarget(precisePhase.progress(seconds), false, first), LevitationMotion.ZERO, 0, seconds, false);
            }
            LevitationMotion.Point endpointPrevious = null;
            for (double seconds : new double[] {1.30 - 1e-7, 1.30, 1.30 + 1e-7}) {
                float phaseValue = precisePhase.progress(seconds);
                var exactPath = MagicEquipMotion.offset(phaseValue, false, first);
                var pose = preciseMotion.sample(MagicEquipMotion.springTarget(phaseValue, false, first), LevitationMotion.ZERO, 0, seconds, false);
                var exactVisible = exactPath.add(pose.offset().multiply(MagicEquipMotion.flight(phaseValue)));
                if (endpointPrevious != null)
                    check(exactVisible.subtract(endpointPrevious).length() < 1e-5, "no position jump at precise flight end");
                endpointPrevious = exactVisible;
            }

            phase = new MagicEquipMotion(); motion = new LevitationMotion(profile);
            phase.observe(false, 0); phase.observe(true, 1);
            for (int frame = 0; frame <= fps / 10; frame++) {
                double seconds = 1 + frame / (double) fps;
                var path = MagicEquipMotion.offset(phase.progress(seconds), false, first);
                motion.sample(MagicEquipMotion.springTarget(phase.progress(seconds), false, first), LevitationMotion.ZERO, 0, seconds, false);
            }
            double reversal = 1.125;
            float p = phase.progress(reversal);
            var path = MagicEquipMotion.offset(p, false, first);
            var before = motion.sample(MagicEquipMotion.springTarget(p, false, first), LevitationMotion.ZERO, 0, reversal, false);
            var visibleBefore = path.add(before.offset().multiply(MagicEquipMotion.flight(p)));
            phase.observe(false, reversal);
            var afterPath = MagicEquipMotion.offset(phase.progress(reversal), false, first);
            var after = motion.sample(MagicEquipMotion.springTarget(phase.progress(reversal), false, first), LevitationMotion.ZERO, 0, reversal, false);
            var visibleAfter = afterPath.add(after.offset().multiply(MagicEquipMotion.flight(phase.progress(reversal))));
            near(visibleBefore.subtract(visibleAfter).length(), 0);
        }
    }

    private static void fullFlightVelocity() {
        for (boolean first : new boolean[] {false, true}) for (boolean left : new boolean[] {false, true})
            for (int fps : new int[] {30, 60, 144}) {
                var profile = first ? LevitationMotion.FIRST_PERSON : LevitationMotion.WORLD;
                var oldMotion = new LevitationMotion(profile);
                var newMotion = new LevitationMotion(profile);
                LevitationMotion.Point previous = null, previousOld = null, previousPath = null;
                double minimumRatio = 10, oldRatio = 10;
                for (int frame = 0; frame <= Math.ceil(MagicEquipMotion.DURATION * fps); frame++) {
                    double time = Math.min(MagicEquipMotion.DURATION, frame / (double) fps);
                    float p = (float) (time / MagicEquipMotion.DURATION);
                    var path = MagicEquipMotion.offset(p, left, first);
                    var oldPose = oldMotion.sample(path, LevitationMotion.ZERO, 0, 1 + time, false);
                    var pose = newMotion.sample(MagicEquipMotion.springTarget(p, left, first), LevitationMotion.ZERO, 0, 1 + time, false);
                    var visual = path.add(pose.offset().multiply(MagicEquipMotion.flight(p)));
                    var oldVisual = path.add(oldPose.offset().multiply(MagicEquipMotion.flight(p)));
                    if (previous != null) {
                        double side = left ? -1 : 1;
                        double travel = first ? (previous.x() - visual.x()) * side : previous.z() - visual.z();
                        double rawTravel = first ? (previousPath.x() - path.x()) * side : previousPath.z() - path.z();
                        double oldTravel = first ? (previousOld.x() - oldVisual.x()) * side : previousOld.z() - oldVisual.z();
                        check(travel > 0, "whole 0..0.25s route never stalls or reverses " + first + "/" + fps + " p=" + p);
                        check(visual.subtract(previous).length() < 14.0 / fps, "flight step has bounded speed");
                        if (p >= .2 && p <= .85) {
                            minimumRatio = Math.min(minimumRatio, travel / rawTravel);
                            oldRatio = Math.min(oldRatio, oldTravel / rawTravel);
                            check(travel / rawTravel > .7, "middle flight retains at least 70% of authored path speed");
                        }
                    }
                    previous = visual; previousOld = oldVisual; previousPath = path;
                }
                check(minimumRatio > oldRatio + .05, "new path does not cancel itself like full-strength spring input");
                if (!left) System.out.printf(java.util.Locale.ROOT,
                        "flight %s %dfps middle min speed ratio old=%.4f new=%.4f%n", first ? "first" : "world", fps, oldRatio, minimumRatio);
            }
    }

    private static void scaleAndHandoff() {
        near(MagicEquipMotion.scale(0), 0); near(MagicEquipMotion.scale(1), 1);
        near(MagicEquipMotion.scale(-1), 0); near(MagicEquipMotion.scale(2), 1);
        near(MagicEquipMotion.scale(.2f), .5); near(MagicEquipMotion.scale(.4f), 1);
        float previous = 0;
        for (int i = 0; i <= 1000; i++) {
            float p = i / 1000f;
            float scale = MagicEquipMotion.scale(p);
            check(scale >= previous && scale <= 1, "scale rises smoothly to normal size");
            if (p >= .4f) near(scale, 1);
            var offset = MagicEquipMotion.offset(p, false, false);
            var matrix = new org.joml.Matrix4f().translation((float) offset.x(), (float) offset.y(), (float) offset.z())
                    .scale(scale);
            var origin = matrix.transformPosition(new org.joml.Vector3f());
            near(origin.x, offset.x()); near(origin.y, offset.y()); near(origin.z, offset.z());
            previous = scale;
        }
        var phase = new MagicEquipMotion();
        phase.observe(false, 0); phase.observe(true, 1);
        near(MagicEquipMotion.scale(phase.progress(1.05)), 0);
        near(MagicEquipMotion.scale(phase.progress(1.10)), .5);
        near(MagicEquipMotion.scale(phase.progress(1.15)), 1);
        check(phase.progress(1.15) < 1, "full scale reached before flight ends");
        phase.observe(false, 2);
        near(MagicEquipMotion.scale(phase.progress(2.15)), 1);
        near(MagicEquipMotion.scale(phase.progress(2.20)), .5);
        near(MagicEquipMotion.scale(phase.progress(2.25)), 0);
        for (double boundary : new double[] {1.05, 1.15, 1.30, 2.15, 2.25}) {
            var forward = new MagicEquipMotion();
            forward.observe(false, 0); forward.observe(true, 1);
            if (boundary >= 2) forward.observe(false, 2);
            double before = MagicEquipMotion.scale(forward.progress(boundary - 1e-7));
            double after = MagicEquipMotion.scale(forward.progress(boundary + 1e-7));
            check(Math.abs(after - before) < 1e-5, "scale is continuous at pause/full-size/arrival/retraction boundary");
        }
        var handoff = new MagicEquipMotion.Handoff();
        check(!handoff.owns(false, true, true, .8f), "ordinary item equip is not captured");
        handoff.begin();
        check(handoff.owns(true, true, true, 0), "flight owns initial vanilla transform");
        check(handoff.owns(false, false, true, 0), "finished flight waits for vanilla item to match");
        check(handoff.owns(false, true, true, .4f), "finished flight never jumps to a still-lowered vanilla hand");
        check(!handoff.owns(false, true, true, 0), "settled vanilla hand relinquishes override");
        check(!handoff.owns(false, true, true, .7f), "later ordinary equip remains original");
        handoff.begin(); handoff.release();
        check(!handoff.owns(false, false, true, .5f), "F or nonempty slot change releases handoff immediately");
        handoff.begin();
        check(!handoff.owns(false, true, false, .8f), "empty outro releases once vanilla also empty");
    }

    private static void renderClock() {
        for (int fps : new int[] {30, 60, 144}) {
            var clock = new MagicEquipMotion.Clock();
            var phase = new MagicEquipMotion();
            phase.observe(false, clock.read(20, .65f));
            double start = clock.read(20, .8f);
            phase.observe(true, start);
            near(phase.progress(start), 0);
            phase.observe(true, clock.read(20, 0));
            near(phase.progress(clock.seconds()), 0);
            double previous = start;
            float previousProgress = 0;
            for (int frame = 1; frame <= fps / 2; frame++) {
                double raw = start + frame / (double) fps;
                int age = (int) Math.floor(raw * 20);
                float delta = (float) (raw * 20 - age);
                double time = clock.read(age, delta);
                phase.observe(true, time);
                float progress = phase.progress(time);
                check(time >= previous && progress >= previousProgress, "subtick render clock is monotonic");
                for (int pass = 0; pass < 5; pass++) {
                    near(clock.read(age, delta), time);
                    near(clock.read(age, 0), time);
                    phase.observe(true, clock.seconds());
                    near(phase.progress(clock.seconds()), progress);
                }
                previous = time; previousProgress = progress;
            }
            near(previousProgress, 1);
            double rewound = clock.read(0, 0);
            near(rewound, 0);
            check(clock.age() == 0, "actual age rewind is detectable for runtime Entry reset");
        }
    }
}
