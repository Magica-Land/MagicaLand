package top.csituka.magicaland.client.render;

import java.util.Objects;
import java.util.Random;

/** Pure timing and item-token tests; no renderer, world, or Minecraft startup. */
public final class PonyCarryCueTest {
    private static final Object APPLE = new Object(), CARROT = new Object(), SWORD = new Object();
    private static int checks;

    public static void main(String[] args) {
        snapshotAndChanges();
        mouthOnlyAtEmptyBoundary();
        envelopeAndFrameRate();
        discontinuities();
        deterministicSequence();
        System.out.println("PASS PonyCarryCueTest: " + checks + " pure timing/token checks (not in-game)");
    }
    private static void snapshotAndChanges() {
        var cue = new PonyCarryCue();
        zero(cue.sample(0, APPLE, CARROT, SWORD), "initial held items only establish a snapshot");
        zero(cue.sample(3, APPLE, CARROT, SWORD), "snapshot does not become a delayed event");
        zero(cue.sample(4, null, CARROT, SWORD), "put-away starts at zero without snapping");
        pose(cue.sample(7, null, CARROT, SWORD), 3, 0, 0, "put-away cues only its left shoulder");
        pose(cue.sample(8, null, APPLE, null), 2.25, 0, 0, "new right and mouth changes do not reset existing left cue");
        pose(cue.sample(11, null, APPLE, null), 0, 3, 0, "switching to a hoof item does not nod");
        zero(cue.sample(14, null, APPLE, null), "all short cues finish");
        zero(cue.sample(15, SWORD, null, CARROT), "three new changes start together at zero");
        pose(cue.sample(18, SWORD, null, CARROT), 3, 3, 0, "switching from a hoof item to mouth does not nod");
        zero(cue.sample(18, APPLE, SWORD, null), "rapid replacement restarts each changed lane without accumulating peaks");
        pose(cue.sample(21, APPLE, SWORD, null), 3, 3, 0, "item replacement only cues shoulders");
        zero(cue.sample(24, APPLE, SWORD, null), "unchanged tokens do not loop");
        zero(cue.sample(25, APPLE, SWORD, null), "unchanged tokens remain settled");
    }
    private static void mouthOnlyAtEmptyBoundary() {
        var cue = new PonyCarryCue(); cue.sample(0, null, null, null);
        cue.sample(1, null, null, SWORD);
        pose(cue.sample(4, null, null, SWORD), 0, 0, 2.5, "empty to mouth keeps draw nod");
        cue.sample(5, null, null, CARROT);
        zero(cue.sample(8, null, null, CARROT), "mouth item replacement cancels unfinished nod");
        cue.sample(9, null, null, null);
        pose(cue.sample(12, null, null, null), 0, 0, 2.5, "mouth to completely empty keeps stow nod");
        cue.sample(13, APPLE, null, null);
        near(cue.sample(14, APPLE, null, null).headPitch(), 0, "new hoof item cancels residual stow nod");
        cue.sample(15, null, null, SWORD);
        near(cue.sample(18, null, null, SWORD).headPitch(), 0, "hoof to mouth item switch stays quiet");
        cue.sample(19, CARROT, null, null);
        near(cue.sample(22, CARROT, null, null).headPitch(), 0, "mouth to hoof item switch stays quiet");
        for (boolean left : new boolean[]{true, false}) {
            cue.reset(); cue.sample(0, left ? APPLE : null, left ? null : APPLE, SWORD);
            cue.sample(1, left ? APPLE : null, left ? null : APPLE, null);
            zero(cue.sample(4, left ? APPLE : null, left ? null : APPLE, null), "remaining hoof item is not completely empty");
            cue.sample(5, null, null, SWORD);
            near(cue.sample(8, null, null, SWORD).headPitch(), 0, "switch to mouth with other item changes is not a draw");
            near(cue.sample(9, null, null, SWORD).headPitch(), 0, "hand transfer of same mouth item does not nod");
        }
    }
    private static void envelopeAndFrameRate() {
        var cue = new PonyCarryCue(); cue.sample(0, null, null, null); cue.sample(1, APPLE, CARROT, SWORD);
        double previous = -1;
        for (int frame = 0; frame <= 120; frame++) {
            double age = frame / 20d;
            var pose = cue.sample(1 + age, APPLE, CARROT, SWORD);
            double wave = age <= 0 || age >= 6 ? 0 : Math.pow(Math.sin(age / 6 * Math.PI), 2);
            pose(pose, 3 * wave, 3 * wave, 2.5 * wave, "six-tick sin-squared envelope");
            check(pose.leftPitch() >= 0 && pose.leftPitch() <= Math.toRadians(3) + 1e-7, "shoulder angle bounded");
            check(pose.headPitch() >= 0 && pose.headPitch() <= Math.toRadians(2.5) + 1e-7, "head angle bounded");
            check(age > 3 || pose.leftPitch() + 1e-7 >= previous, "cue rises smoothly until midpoint");
            check(age <= 3 || pose.leftPitch() <= previous + 1e-7, "cue fades smoothly after midpoint");
            previous = pose.leftPitch();
            check(pose.equals(cue.sample(1 + age, APPLE, CARROT, SWORD)), "duplicate render of a frame cannot advance pulse");
        }
        zero(cue.sample(9, APPLE, CARROT, SWORD), "expired pulse does not recur");
        for (int fps : new int[]{10, 20, 30, 60, 144, 240}) {
            var fine = new PonyCarryCue(); var sparse = new PonyCarryCue();
            fine.sample(0, null, null, null); sparse.sample(0, null, null, null);
            fine.sample(1, APPLE, null, null); sparse.sample(1, APPLE, null, null);
            for (int checkpoint = 1; checkpoint <= 12; checkpoint++) {
                double start = 1 + (checkpoint - 1) / 2d, end = 1 + checkpoint / 2d;
                for (int frame = 1; start + frame * 20d / fps < end; frame++)
                    fine.sample(start + frame * 20d / fps, APPLE, null, null);
                check(fine.sample(end, APPLE, null, null).equals(sparse.sample(end, APPLE, null, null)), "same event/sample time is independent of render frequency " + fps);
            }
        }
        var endpoints = new PonyCarryCue(); endpoints.sample(0, null, null, null); endpoints.sample(1, APPLE, null, null);
        check(endpoints.sample(1.0001, APPLE, null, null).leftPitch() < 1e-8, "sin-squared attack has no angular jump");
        check(endpoints.sample(6.9999, APPLE, null, null).leftPitch() < 1e-8, "sin-squared release has no angular jump");
    }
    private static void discontinuities() {
        for (double discontinuity : new double[]{-1, 25, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            var cue = new PonyCarryCue(); cue.sample(0, null, null, null); cue.sample(1, APPLE, CARROT, SWORD);
            pose(cue.sample(4, APPLE, CARROT, SWORD), 3, 3, 2.5, "cue exists before reset condition");
            zero(cue.sample(discontinuity, SWORD, APPLE, CARROT), "invalid, backward, or over-twenty-tick time clears cue");
            double resume = Double.isFinite(discontinuity) ? discontinuity + 1 : 5;
            zero(cue.sample(resume, SWORD, APPLE, CARROT), "resume does not resurrect interrupted cue");
            zero(cue.sample(resume + 3, SWORD, APPLE, CARROT), "cleared event never peaks later");
        }
        var exact = new PonyCarryCue(); exact.sample(0, null, null, null);
        zero(exact.sample(20, APPLE, null, null), "exactly twenty ticks can still begin a real change");
        pose(exact.sample(23, APPLE, null, null), 3, 0, 0, "twenty-tick interval is accepted, not greater-than cutoff");
        exact.reset(); zero(exact.sample(24, CARROT, SWORD, APPLE), "explicit reset makes next state a snapshot");
        zero(exact.sample(27, CARROT, SWORD, APPLE), "reset clears all three lanes");
        exact.sample(28, null, SWORD, APPLE);
        pose(exact.sample(31, null, SWORD, APPLE), 3, 0, 0, "new event still works after reset");
    }
    private static void deterministicSequence() {
        var random = new Random(0xCA771E);
        var cue = new PonyCarryCue();
        Object[] tokens = new Object[3], previous = new Object[3], choices = {null, APPLE, CARROT, SWORD};
        double tick = 0, last = Double.NaN;
        double[] began = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (int frame = 0; frame < 2000; frame++) {
            tick += frame % 177 == 0 ? 21 : frame % 113 == 0 ? -2 : random.nextDouble() * .8;
            if (random.nextInt(5) == 0) tokens[random.nextInt(3)] = choices[random.nextInt(choices.length)];
            boolean reset = !Double.isFinite(last) || tick < last || tick - last > 20;
            for (int lane = 0; lane < 2; lane++) {
                if (reset) began[lane] = Double.NEGATIVE_INFINITY;
                else if (!Objects.equals(previous[lane], tokens[lane])) began[lane] = tick;
            }
            if (reset) began[2] = Double.NEGATIVE_INFINITY;
            else if (!Objects.equals(previous[2], tokens[2])) {
                boolean draw = previous[0] == null && previous[1] == null && previous[2] == null && tokens[2] != null;
                boolean stow = previous[2] != null && tokens[0] == null && tokens[1] == null && tokens[2] == null;
                began[2] = draw || stow ? tick : Double.NEGATIVE_INFINITY;
            } else if (tokens[2] == null && (tokens[0] != null || tokens[1] != null)) began[2] = Double.NEGATIVE_INFINITY;
            System.arraycopy(tokens, 0, previous, 0, 3);
            var actual = cue.sample(tick, tokens[0], tokens[1], tokens[2]);
            pose(actual, expected(tick - began[0], 3), expected(tick - began[1], 3), expected(tick - began[2], 2.5), "seeded mixed draw/stow/switch timeline");
            last = tick;
        }
    }
    private static double expected(double age, double peak) { return age <= 0 || age >= 6 ? 0 : peak * Math.pow(Math.sin(age * Math.PI / 6), 2); }
    private static void zero(PonyCarryCue.Pose actual, String label) { pose(actual, 0, 0, 0, label); }
    private static void pose(PonyCarryCue.Pose actual, double leftDegrees, double rightDegrees, double headDegrees, String label) {
        near(actual.leftPitch(), Math.toRadians(leftDegrees), label + " left");
        near(actual.rightPitch(), Math.toRadians(rightDegrees), label + " right");
        near(actual.headPitch(), Math.toRadians(headDegrees), label + " head");
    }
    private static void near(double actual, double expected, String label) { check(Double.isFinite(actual) && Math.abs(actual - expected) < 1e-7, label + ": " + actual + " != " + expected); }
    private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
}
