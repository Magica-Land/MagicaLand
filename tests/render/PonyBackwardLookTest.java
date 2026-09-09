package top.csituka.magicaland.client.render;

import com.google.gson.JsonParser;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.GeoBone;
import top.csituka.magicaland.client.animation.PonyBackwardLook;
import top.csituka.magicaland.client.animation.PonyExpressions;

import java.nio.file.Files;
import java.nio.file.Path;

public final class PonyBackwardLookTest {
    private static int checks;
    private static final PonyHeadLookMath.Rotation ZERO = PonyHeadLookMath.Rotation.ZERO;

    public static void main(String[] args) throws Exception {
        timeline();
        matchingClients();
        animationReference(Path.of(args[0]));
        quaternionRoundTrips();
        smoothAndLocked();
        shortTapAndInterruption();
        returnEndpoint();
        restoration();
        System.out.println("PASS PonyBackwardLookTest: " + checks + " timing/expression/rotation/isolation checks");
    }

    private static void timeline() {
        PonyBackwardLook state = new PonyBackwardLook();
        near((float) PonyBackwardLook.TURN_TICKS, 5.5f, "turn/return speed doubled");
        near((float) PonyBackwardLook.DELAY_TICKS, 20, "backward delay unchanged");
        require(!state.sample(0).active(), "initial normal");
        state.update(true, 100);
        for (int i = 0; i < 200; i++) {
            double tick = 100 + i / 10d;
            state.update(true, Math.floor(tick));
            require(state.sample(tick).phase() == PonyBackwardLook.Phase.WAITING, "full 1 s waiting");
            require(state.expressionAction("backward_walk", tick).equals("walk"), "no look-back eyes before turn");
            require(PonyExpressions.allowsAutomaticGaze(state.expressionAction("backward_walk", tick)), "waiting normal gaze");
        }
        require(state.sample(120).phase() == PonyBackwardLook.Phase.TURNING, "turn starts at 1 s");
        require(state.expressionAction("backward_walk", 120).equals("backward_walk"), "expression starts with turn");
        require(!PonyExpressions.allowsAutomaticGaze(state.expressionAction("backward_walk", 120)), "author locked eye mode retained");
        near(state.sample(122.75).progress(), .5f, "0.275 s turn midpoint");
        require(state.sample(125.5).phase() == PonyBackwardLook.Phase.HOLDING, "stable hold after 0.275 s");
        state.update(false, 160);
        require(state.sample(160).phase() == PonyBackwardLook.Phase.RETURNING, "return starts continuously");
        near(state.sample(162.75).progress(), .5f, "same faster return");
        require(!state.sample(165.5).active(), "return finishes");
        state.update(true, 180);
        require(state.sample(199.99).phase() == PonyBackwardLook.Phase.WAITING, "new press gets full delay");
        state.update(true, 1);
        require(state.sample(1).phase() == PonyBackwardLook.Phase.WAITING, "clock rewind restarts safely");
        state.update(false, Double.NaN);
        require(!state.sample(5).active(), "invalid time resets");
        state.update(true, 10);
        state.reset();
        require(!state.sample(100).active(), "entity replacement reset");
    }

    private static void matchingClients() {
        PonyBackwardLook local = new PonyBackwardLook(), remote = new PonyBackwardLook();
        for (int tick = 0; tick < 250; tick++) {
            boolean back = tick >= 20 && tick < 80 || tick >= 120 && tick < 129;
            local.update(back, tick);
            remote.update(back, tick + 7000);
            for (float partial : new float[] {0, .25f, .5f, .99f}) {
                var a = local.sample(tick + partial);
                var b = remote.sample(tick + 7000 + (double) partial);
                require(a.phase() == b.phase(), "same synced backward action independent of entity age");
                near(a.progress(), b.progress(), "local/remote subframe timing");
            }
        }
        for (String action : new String[] {"idle", "walk", "jump1", "run", "sleep", "attacked"})
            require(local.expressionAction(action, 250).equals(action), "unrelated expression unchanged");
    }

    private static void animationReference(Path repo) throws Exception {
        try (var reader = Files.newBufferedReader(repo.resolve("appearance/src/main/resources/assets/magicaland/animations/mare_animation.json"))) {
            var bones = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations")
                    .getAsJsonObject("backward_walk").getAsJsonObject("bones");
            for (String name : new String[] {"Neck", "Head"}) {
                var vector = bones.getAsJsonObject(name).getAsJsonObject("rotation").getAsJsonObject("0.0").getAsJsonArray("post");
                var rotation = name.equals("Neck") ? PonyBackwardHeadPose.NECK : PonyBackwardHeadPose.HEAD;
                near(rotation.x(), (float) Math.toRadians(-vector.get(0).getAsFloat()), "author reference x");
                near(rotation.y(), (float) Math.toRadians(-vector.get(1).getAsFloat()), "author reference y");
                near(rotation.z(), (float) Math.toRadians(vector.get(2).getAsFloat()), "author reference z");
            }
        }
    }

    private static void smoothAndLocked() {
        Object owner = new Object();
        GeoBone neck = bone(null, "Neck"), head = bone(neck, "Head");
        PonyBackwardLook state = new PonyBackwardLook();
        PonyBackwardHeadPose pose = new PonyBackwardHeadPose();
        require(pose.sample(owner, neck, head, state.sample(0), ZERO) == null, "normal returns vanilla head path");
        state.update(true, 1);
        PonyBackwardHeadPose.Rotations previous = null, locked = null;
        for (int frame = 0; frame <= 270; frame++) {
            double ticks = 1 + frame / 3d;
            neck.updateRotation((float) Math.sin(ticks), -.8f, .3f);
            head.updateRotation(-1.5f + (float) Math.sin(ticks) * .2f, -1.3f, 1.6f);
            var sample = pose.sample(owner, neck, head, state.sample(ticks), ZERO);
            require(sample != null, "override all backward frames including wait");
            if (ticks <= 21) {
                near(angle(sample.neck(), new PonyBackwardHeadPose.Rotation(0, 0, 0)), 0, "waiting removes authored neck turn");
                near(angle(sample.head(), new PonyBackwardHeadPose.Rotation(0, 0, 0)), 0, "waiting removes authored head turn");
            }
            if (previous != null) {
                require(angle(previous.head(), sample.head()) < Math.toRadians(12), "double-speed smooth shortest-path turn at 60 fps, tick=" + ticks + ", angle=" + Math.toDegrees(angle(previous.head(), sample.head())));
                require(angle(previous.neck(), sample.neck()) < Math.toRadians(8), "no instantaneous neck turn at doubled speed");
            }
            previous = sample;
            if (ticks >= 26.5) {
                if (locked == null) locked = sample;
                require(sample.equals(locked), "authored oscillations cannot move locked pose");
                var movedCamera = pose.sample(owner, neck, head, state.sample(ticks), new PonyHeadLookMath.Rotation(.7f, -.9f));
                require(movedCamera.equals(locked), "camera cannot move locked neck/head");
            }
        }
        state.update(false, 100);
        var firstReturn = pose.sample(owner, neck, head, state.sample(100), ZERO);
        require(firstReturn.equals(locked), "return starts at exact last rendered pose");
        previous = firstReturn;
        for (int frame = 1; frame / 3d < PonyBackwardLook.TURN_TICKS; frame++) {
            double ticks = 100 + frame / 3d;
            if (frame >= 9) { neck.updateRotation(0, 0, 0); head.updateRotation(0, 0, 0); }
            var sample = pose.sample(owner, neck, head, state.sample(ticks), ZERO);
            require(angle(previous.head(), sample.head()) < Math.toRadians(12), "double-speed continuous return");
            previous = sample;
        }
        require(pose.sample(owner, neck, head, state.sample(105.5), ZERO) == null, "normal camera path restored");
        state.update(true, 112);
        var other = pose.sample(new Object(), neck, head, state.sample(112), ZERO);
        near(angle(other.head(), new PonyBackwardHeadPose.Rotation(0, 0, 0)), 0, "other entity cannot inherit last head pose");
    }

    private static void shortTapAndInterruption() {
        for (double release : new double[] {2, 8, 19, 22, 24}) {
            Object owner = new Object();
            GeoBone neck = bone(null, "Neck"), head = bone(neck, "Head");
            PonyBackwardLook state = new PonyBackwardLook();
            PonyBackwardHeadPose pose = new PonyBackwardHeadPose();
            state.update(true, 0);
            var last = pose.sample(owner, neck, head, state.sample(release), ZERO);
            state.update(false, release);
            var first = pose.sample(owner, neck, head, state.sample(release), ZERO);
            require(first.equals(last), "partial turn/tap release cannot snap");
            if (release < 20) {
                neck.updateRotation(.2f, -.8f, 0);
                head.updateRotation(-1.7f, -1.3f, 1.6f);
                for (double dt : new double[] {0, .5, 1, 2, 3}) {
                    var value = pose.sample(owner, neck, head, state.sample(release + dt), ZERO);
                    near(angle(value.head(), new PonyBackwardHeadPose.Rotation(0, 0, 0)), 0, "short tap never leaks controller look-back pose");
                }
            }
        }
    }

    private static void restoration() {
        GeoBone neck = bone(null, "Neck"), head = bone(neck, "Head"), eye = bone(head, "Emotions"), leg = bone(null, "LForeLeg");
        neck.updateRotation(.1f, .2f, .3f); head.updateRotation(.4f, .5f, .6f);
        eye.updateRotation(.7f, .8f, .9f); leg.updateRotation(1, 2, 3);
        var target = new PonyBackwardHeadPose.Rotations(PonyBackwardHeadPose.NECK, PonyBackwardHeadPose.HEAD);
        for (int flags = 0; flags < 8; flags++) for (int i = 0; i < 100; i++) {
            for (GeoBone b : new GeoBone[] {neck, head}) {
                b.resetStateChanges();
                if ((flags & 1) != 0) b.markRotationAsChanged();
                if ((flags & 2) != 0) b.markPositionAsChanged();
                if ((flags & 4) != 0) b.markScaleAsChanged();
            }
            try (var scope = new PonyRenderer.HeadPose(null, neck, head, target)) {
                require(PonyBackwardHeadPose.current(neck).equals(target.neck()), "paired neck target");
                require(PonyBackwardHeadPose.current(head).equals(target.head()), "paired head target");
                near(eye.getRotX(), .7f, "eye animation bones untouched");
                near(leg.getRotZ(), 3, "leg animation untouched");
            }
            near(neck.getRotX(), .1f, "exact neck restore"); near(head.getRotZ(), .6f, "exact head restore");
            for (GeoBone b : new GeoBone[] {neck, head})
                require(b.hasRotationChanged() == ((flags & 1) != 0) && b.hasPositionChanged() == ((flags & 2) != 0)
                        && b.hasScaleChanged() == ((flags & 4) != 0), "all animation dirty flags restored");
        }
        try (var scope = new PonyRenderer.HeadPose(null, neck, head, target)) { throw new IllegalStateException("expected"); }
        catch (IllegalStateException expected) { near(neck.getRotX(), .1f, "failure restores neck"); near(head.getRotZ(), .6f, "failure restores head"); }
    }

    private static void returnEndpoint() {
        Object owner = new Object();
        GeoBone neck = bone(null, "Neck"), head = bone(neck, "Head");
        PonyBackwardLook state = new PonyBackwardLook();
        PonyBackwardHeadPose pose = new PonyBackwardHeadPose();
        state.update(true, 0);
        pose.sample(owner, neck, head, state.sample(30), ZERO);
        state.update(false, 30);
        pose.sample(owner, neck, head, state.sample(30), ZERO);
        neck.updateRotation(.12f, -.25f, .32f);
        head.updateRotation(-.3f, .4f, -.2f);
        var view = new PonyHeadLookMath.Rotation(.2f, -.3f);
        var result = pose.sample(owner, neck, head, state.sample(35.5 - .0001), view);
        var normalNeck = PonyBackwardHeadPose.current(neck);
        var normalHead = PonyBackwardHeadPose.current(head).add(view.pitch(), view.yaw(), 0);
        require(Math.abs(quaternion(result.neck()).dot(quaternion(normalNeck))) > .999999f,
                "shortened return fully rejoins non-neutral neck pose before ending");
        require(Math.abs(quaternion(result.head()).dot(quaternion(normalHead))) > .999999f,
                "shortened return fully rejoins current animation plus camera before ending");
        require(pose.sample(owner, neck, head, state.sample(35.5), view) == null,
                "exact endpoint delegates unchanged normal camera path");
    }

    private static void quaternionRoundTrips() {
        var targets = new PonyBackwardHeadPose.Rotation[] {PonyBackwardHeadPose.NECK, PonyBackwardHeadPose.HEAD,
                new PonyBackwardHeadPose.Rotation(.7f, -.9f, .4f), new PonyBackwardHeadPose.Rotation(-1.1f, .8f, -.5f)};
        for (var from : targets) for (var to : targets) for (int i = 0; i <= 1000; i++) {
            float t = i / 1000f;
            var result = PonyBackwardHeadPose.blend(from, to, t);
            Quaternionf expected = quaternion(from).slerp(quaternion(to), t).normalize();
            Quaternionf actual = quaternion(result).normalize();
            require(Math.abs(expected.dot(actual)) > .999999f, "ZYX reconstruction matches exact quaternion path");
        }
    }

    private static GeoBone bone(GeoBone parent, String name) {
        GeoBone bone = new GeoBone(parent, name, false, 0d, false, false);
        bone.saveInitialSnapshot();
        if (parent != null) parent.getChildBones().add(bone);
        return bone;
    }
    private static float angle(PonyBackwardHeadPose.Rotation a, PonyBackwardHeadPose.Rotation b) {
        Quaternionf qa = quaternion(a).normalize();
        Quaternionf qb = quaternion(b).normalize();
        return (float) (2 * Math.acos(Math.min(1, Math.abs(qa.dot(qb)))));
    }
    private static Quaternionf quaternion(PonyBackwardHeadPose.Rotation rotation) {
        return new Quaternionf().rotationZYX(rotation.z(), rotation.y(), rotation.x());
    }
    private static void near(float actual, float expected, String label) { require(Math.abs(actual - expected) < .0001f, label); }
    private static void require(boolean condition, String label) { checks++; if (!condition) throw new AssertionError(label); }
}
