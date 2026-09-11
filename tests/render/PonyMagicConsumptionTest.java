package top.csituka.magicaland.client.render;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class PonyMagicConsumptionTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        activeHand(); transitions(); pausedFrames(); lifecycle(); useMotion(); itemIdentity();
        transformedMouth(); sharedAnchor(); displayTranslation(); invalidAndStackIsolation(); boundaries(Path.of(args[0]));
        System.out.println("PASS PonyMagicConsumptionTest: " + checks + " real MatrixStack/JOML and consumption-state checks");
    }

    private static PonyHeldItems.Consumption use(Hand hand, UseAction action, float ticks) {
        return new PonyHeldItems.Consumption(hand, action, ticks);
    }
    private static void activeHand() {
        for (Hand hand : Hand.values()) for (UseAction action : new UseAction[]{UseAction.EAT, UseAction.DRINK}) {
            var state = new PonyMagicConsumption();
            state.update(0, PonyHeldItems.Consumption.NONE, true);
            for (int tick = 1; tick <= 50; tick++) {
                state.update(tick, use(hand, action, tick), true);
                check(state.pose(hand == Hand.MAIN_HAND).weight() > 0, "only the actual use hand approaches the mouth");
                check(state.pose(hand != Hand.MAIN_HAND).equals(PonyMagicConsumption.Pose.NONE), "unused second hand remains entirely unchanged");
            }
        }
        for (UseAction action : UseAction.values()) if (action != UseAction.EAT && action != UseAction.DRINK) {
            var state = new PonyMagicConsumption();
            state.update(0, use(Hand.MAIN_HAND, action, 5), true);
            check(state.pose(true).equals(PonyMagicConsumption.Pose.NONE), "bows, shields and tridents cannot start a consumption pose: " + action);
        }
    }

    private static void transitions() {
        var state = new PonyMagicConsumption();
        state.update(0, PonyHeldItems.Consumption.NONE, true);
        for (int frame = 1; frame <= 16; frame++) {
            float elapsed = frame * .25f;
            state.update(elapsed, use(Hand.MAIN_HAND, UseAction.EAT, elapsed), true);
            near(state.pose(true).weight(), elapsed / 4, "four-tick entry is continuous across partial ticks");
        }
        for (int frame = 1; frame <= 16; frame++) {
            float elapsed = frame * .25f;
            state.update(4 + elapsed, PonyHeldItems.Consumption.NONE, true);
            near(state.pose(true).weight(), 1 - elapsed / 4, "four-tick return is continuous after release");
        }
        check(state.pose(true).equals(PonyMagicConsumption.Pose.NONE), "return reaches exact identity pose");
        state.update(9, use(Hand.OFF_HAND, UseAction.DRINK, 1), true);
        near(state.pose(false).weight(), .25, "offhand enters independently after a main-hand action");
        check(state.pose(true).equals(PonyMagicConsumption.Pose.NONE), "new offhand action cannot revive the main hand");
    }

    private static void pausedFrames() {
        var state = new PonyMagicConsumption();
        state.update(10, PonyHeldItems.Consumption.NONE, true);
        state.update(11, use(Hand.MAIN_HAND, UseAction.DRINK, 1), true);
        var pose = state.pose(true);
        for (int pass = 0; pass < 1000; pass++) {
            state.update(11, use(Hand.MAIN_HAND, UseAction.DRINK, 1), true);
            check(state.pose(true).equals(pose), "repeated render passes never advance entry time");
        }
        state.update(12, PonyHeldItems.Consumption.NONE, true);
        check(state.pose(true).equals(PonyMagicConsumption.Pose.NONE), "partial entry reverses from its current weight");
    }

    private static void lifecycle() {
        var state = new PonyMagicConsumption();
        state.update(100, use(Hand.MAIN_HAND, UseAction.EAT, 15), true);
        near(state.pose(true).weight(), 1, "first observed actor already eating starts at the authoritative use pose");
        state.update(101, use(Hand.MAIN_HAND, UseAction.EAT, 16), false);
        check(state.pose(true).equals(PonyMagicConsumption.Pose.NONE), "disabled render context clears both hands");
        state.update(102, use(Hand.OFF_HAND, UseAction.DRINK, 5), true);
        state.update(Double.NaN, null, true);
        check(state.pose(false).equals(PonyMagicConsumption.Pose.NONE), "invalid time clears stale pose");
        state.update(103, use(Hand.MAIN_HAND, UseAction.EAT, 1), true);
        state.update(10, PonyHeldItems.Consumption.NONE, true);
        check(state.pose(true).equals(PonyMagicConsumption.Pose.NONE), "time rewind cannot replay an old consumption");
        state.update(11, use(Hand.OFF_HAND, UseAction.EAT, 2), true);
        state.update(40, PonyHeldItems.Consumption.NONE, true);
        check(state.pose(false).equals(PonyMagicConsumption.Pose.NONE), "long render gaps reset stale retreat");
    }

    private static void useMotion() {
        for (UseAction action : new UseAction[]{UseAction.EAT, UseAction.DRINK}) {
            var state = new PonyMagicConsumption();
            for (int frame = 0; frame < 160; frame++) {
                float tick = frame * .25f;
                state.update(tick, use(Hand.MAIN_HAND, action, tick), true);
                var pose = state.pose(true);
                check(Float.isFinite(pose.pitch()) && Float.isFinite(pose.bob()), "finite authored consumption wave");
                double degrees = Math.toDegrees(pose.pitch());
                check(action == UseAction.DRINK ? degrees >= 39.99 && degrees <= 44.01 : degrees >= 4.99 && degrees <= 11.01,
                        "food and drink have distinct bounded tilt");
                check(Math.abs(pose.bob()) <= (action == UseAction.DRINK ? .00601 : .01501), "mouth bob stays small");
            }
        }
    }

    private static void itemIdentity() {
        var state = new PonyMagicConsumption();
        Object apple = new Object(), trident = new Object(), bow = new Object(), potion = new Object(), bottle = new Object();
        state.update(0, use(Hand.MAIN_HAND, UseAction.EAT, 5), true, apple, potion);
        near(state.pose(true).weight(), 1, "active food state is associated with its item identity");
        state.update(1, PonyHeldItems.Consumption.NONE, true, apple, potion);
        near(state.pose(true).weight(), .75, "same food retains a smooth retreat when eating stops");
        state.update(2, PonyHeldItems.Consumption.NONE, true, trident, potion);
        check(state.pose(true).equals(PonyMagicConsumption.Pose.NONE), "new trident never inherits previous food retreat");
        state.update(3, use(Hand.OFF_HAND, UseAction.DRINK, 1), true, trident, potion);
        near(state.pose(false).weight(), .25, "offhand drink starts independently of the swapped main item");
        state.update(4, use(Hand.OFF_HAND, UseAction.DRINK, 2), true, bow, potion);
        near(state.pose(false).weight(), .5, "changing unused hand does not restart active drink");
        check(state.pose(true).equals(PonyMagicConsumption.Pose.NONE), "unused bow remains at ordinary holding position");
        state.update(5, PonyHeldItems.Consumption.NONE, true, bow, bottle);
        check(state.pose(false).equals(PonyMagicConsumption.Pose.NONE), "finished drink container cannot inherit stale mouth pose");
        state.update(6, use(Hand.MAIN_HAND, UseAction.EAT, 1), true, apple, bottle);
        state.update(7, use(Hand.MAIN_HAND, UseAction.EAT, 2), true, null, bottle);
        check(state.pose(true).equals(PonyMagicConsumption.Pose.NONE), "empty current hand wins over stale active-use metadata");
        state.update(8, use(Hand.MAIN_HAND, UseAction.EAT, Float.NaN), true, apple, bottle);
        check(Float.isFinite(state.pose(true).pitch()) && Float.isFinite(state.pose(true).bob()), "invalid consumption clock is sanitized");
    }

    private static void transformedMouth() {
        for (float yaw : new float[]{-170, -65, 0, 60, 170}) for (float pitch : new float[]{-65, 0, 65})
            for (float scale : new float[]{.4f, 1, 1.5f}) for (float tilt : new float[]{8, 42}) {
                Matrix4f mouth = new Matrix4f().translation(5, -2, 7)
                        .rotateY((float) Math.toRadians(yaw)).rotateX((float) Math.toRadians(pitch)).scale(scale);
                Matrix4f originalMouth = new Matrix4f(mouth);
                var matrices = new MatrixStack();
                matrices.translate(-2, 4, -5); matrices.scale(scale, scale, scale);
                Matrix4f anchor = new Matrix4f(matrices.peek().getPositionMatrix()).translate(.2f, 0, -.4f);
                var pose = new PonyMagicConsumption.Pose(1, (float) Math.toRadians(tilt), .01f);
                PonyMagicConsumption.apply(matrices, anchor, mouth, pose);
                Matrix4f target = new Matrix4f(mouth).translate(0, 22.05f / 16 + pose.bob(), -14.5f / 16).rotateX(pose.pitch());
                near(matrices.peek().getPositionMatrix(), target, "full use reaches the actual transformed head frame");
                near(anchor, target, "inertia anchor reaches the same mouth target");
                near(mouth, originalMouth, "captured head matrix is never mutated by consumption");
                Vector3f forward = matrices.peek().getPositionMatrix().transformDirection(new Vector3f(0, 0, -1)).normalize();
                Vector3f targetForward = target.transformDirection(new Vector3f(0, 0, -1)).normalize();
                near(forward, targetForward, "item tilt follows yawed/pitched head axes, not world axes");
                near(matrices.peek().getPositionMatrix().getScale(new Vector3f()), new Vector3f(scale), "model scale unchanged");
                Matrix3f expectedNormal = new Matrix3f(matrices.peek().getPositionMatrix()).invert().transpose();
                near(matrices.peek().getNormalMatrix(), expectedNormal, "normal follows resulting rotation and scale");
            }
    }

    private static void sharedAnchor() {
        for (int sample = 0; sample <= 20; sample++) {
            float weight = sample / 20f;
            var matrices = new MatrixStack();
            matrices.translate(3, 1.5, 7); matrices.multiply(new Quaternionf().rotationXYZ(.1f, -.4f, .3f));
            matrices.scale(.6f, .8f, 1.2f);
            Matrix4f original = new Matrix4f(matrices.peek().getPositionMatrix());
            Matrix4f anchor = new Matrix4f(original);
            Matrix4f mouth = new Matrix4f().translation(-2, 5, 3).rotateXYZ(-.7f, 1.1f, 0).scale(1.3f);
            var pose = new PonyMagicConsumption.Pose(weight, .3f, .005f);
            PonyMagicConsumption.apply(matrices, anchor, mouth, pose);
            near(anchor, matrices.peek().getPositionMatrix(), "render and inertial anchor share exactly the same blend");
            near(matrices.peek().getPositionMatrix().getScale(new Vector3f()), original.getScale(new Vector3f()),
                    "transition preserves original nonuniform item scale");
            Matrix4f target = new Matrix4f(mouth).translate(0, 22.05f / 16 + pose.bob(), -14.5f / 16).rotateX(pose.pitch());
            Vector3f position = original.getTranslation(new Vector3f()).lerp(target.getTranslation(new Vector3f()), weight);
            near(anchor.getTranslation(new Vector3f()), position, "mouth approach is continuous at all weights");
        }
    }

    private static void displayTranslation() {
        for (boolean left : new boolean[]{false, true}) for (float scale : new float[]{.5f, 1, 1.7f})
            for (float yaw : new float[]{-1.2f, 0, 1.1f}) for (float pitch : new float[]{-.6f, 0, .8f}) {
                var matrices = new MatrixStack();
                matrices.translate(4, 2, -5); matrices.scale(scale, scale, scale);
                var anchor = new Matrix4f(matrices.peek().getPositionMatrix());
                var mouth = new Matrix4f().translation(2, 3, -7).rotateXYZ(pitch, yaw, .1f).scale(scale);
                var display = new Vector3f(left ? -.08f : .08f, .1875f, .0625f);
                var untouched = new Vector3f(display);
                var pose = new PonyMagicConsumption.Pose(1, (float) Math.toRadians(42), .005f);
                PonyMagicConsumption.apply(matrices, anchor, mouth, pose, display);
                Vector3f renderedCenter = matrices.peek().getPositionMatrix().transformPosition(new Vector3f(display));
                Vector3f mouthCenter = new Matrix4f(mouth).translate(0, 22.05f / 16 + pose.bob(), -14.5f / 16)
                        .transformPosition(new Vector3f());
                near(renderedCenter, mouthCenter, "left/right third-person display translation cancels at the actual mouth center");
                near(display, untouched, "shared model display translation is never changed");
                near(anchor, matrices.peek().getPositionMatrix(), "compensated inertia and render matrices agree");
            }
    }

    private static void invalidAndStackIsolation() {
        var matrices = new MatrixStack();
        matrices.translate(3, 4, 5); matrices.multiply(new Quaternionf().rotationXYZ(.2f, -.5f, .1f));
        matrices.scale(.7f, .8f, .9f);
        Matrix4f before = new Matrix4f(matrices.peek().getPositionMatrix());
        Matrix3f beforeNormal = new Matrix3f(matrices.peek().getNormalMatrix());
        for (var pose : new PonyMagicConsumption.Pose[]{PonyMagicConsumption.Pose.NONE,
                new PonyMagicConsumption.Pose(Float.NaN, 0, 0), new PonyMagicConsumption.Pose(1, Float.NaN, 0),
                new PonyMagicConsumption.Pose(1, 0, Float.NaN)}) {
            Matrix4f anchor = new Matrix4f(before);
            PonyMagicConsumption.apply(matrices, anchor, new Matrix4f(), pose);
            near(matrices.peek().getPositionMatrix(), before, "invalid/unused pose leaves original draw matrix untouched");
            near(matrices.peek().getNormalMatrix(), beforeNormal, "invalid/unused pose leaves original normal matrix untouched");
            near(anchor, before, "invalid/unused pose leaves inertia anchor untouched");
        }
        Matrix4f anchor = new Matrix4f(before);
        PonyMagicConsumption.apply(matrices, anchor, null, new PonyMagicConsumption.Pose(1, .2f, 0));
        near(matrices.peek().getPositionMatrix(), before, "missing head capture safely keeps original item position");
        matrices.push();
        PonyMagicConsumption.apply(matrices, anchor, new Matrix4f().translate(9, 9, 9), new PonyMagicConsumption.Pose(.75f, .2f, 0));
        check(!matrices.peek().getPositionMatrix().equals(before, .0001f), "active consumption changes only pushed entry");
        matrices.pop();
        near(matrices.peek().getPositionMatrix(), before, "popping the hand restores other render geometry");
        near(matrices.peek().getNormalMatrix(), beforeNormal, "popping the hand restores other render normals");
        Matrix4f singular = new Matrix4f().scaling(0, 1, 1);
        PonyMagicConsumption.apply(matrices, singular, new Matrix4f(), new PonyMagicConsumption.Pose(1, .2f, 0));
        near(matrices.peek().getPositionMatrix(), before, "invalid anchor cannot half-apply a new draw pose");
    }

    private static void boundaries(Path repo) throws Exception {
        String root = "src/client/java/top/csituka/magicaland/";
        String renderer = Files.readString(repo.resolve(root + "client/render/PonyRenderer.java"));
        String player = Files.readString(repo.resolve(root + "mixin/client/PlayerEntityRendererMixin.java"));
        String first = Files.readString(repo.resolve(root + "mixin/client/HeldItemRendererMixin.java"));
        check(renderer.contains("magicMouthFrame = null;") && renderer.contains("config.showHorn"), "head capture refreshed per actor render and only magic users consume");
        int posed = renderer.indexOf("HeadPose head = applyHeadLook"), capture = renderer.indexOf("magicMouthFrame = new Matrix4f");
        int afterGeometry = renderer.indexOf("super.renderRecursively", posed);
        check(posed >= 0 && afterGeometry > posed && capture > afterGeometry, "capture occurs inside final posed head recursion after geometry");
        check(renderer.contains("!isReRender && gazeFrame != null && \"Head\".equals(bone.getName()) && !bone.isHidden()"),
                "no duplicated armor rerender or hidden-head capture");
        check(renderer.contains("new Matrix4f(magicMouthFrame)"), "consumers receive copied head transform");
        check(player.indexOf("PonyMagicConsumption.apply") < player.indexOf("ItemLevitation.applyWorld"), "inertia sees the already moved mouth anchor");
        check(player.contains("ponyRenderer.magicConsumption(isMainHand)"), "per-hand consumption dispatch preserves handedness");
        check(player.contains("new org.joml.Vector3f(display.translation)") && player.contains("if (!isRightArm) translation.x = -translation.x"),
                "display translation is copied and mirrored only for the selected left hand");
        check(renderer.contains("player.getMainHandStack().getItem()") && renderer.contains("player.getOffHandStack().getItem()"),
                "both active and unused hands are checked against actual currently equipped item identities");
        check(!first.contains("PonyMagicConsumption"), "first-person vanilla consumption stays untouched");
    }

    private static void near(Matrix4f actual, Matrix4f expected, String label) { check(actual.equals(expected, .00015f), label); }
    private static void near(Matrix3f actual, Matrix3f expected, String label) { check(actual.equals(expected, .00015f), label); }
    private static void near(Vector3f actual, Vector3f expected, String label) { check(actual.distance(expected) < .00015f, label); }
    private static void near(double actual, double expected, String label) { check(Math.abs(actual - expected) < .00001, label); }
    private static void check(boolean condition, String label) { if (!condition) throw new AssertionError(label); checks++; }
}
