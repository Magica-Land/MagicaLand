package top.csituka.magicaland.client.render;

import software.bernie.geckolib.cache.object.GeoBone;
import top.csituka.magicaland.client.animation.PonyFlightVisuals;
import top.csituka.magicaland.client.animation.PonyFlightAnimations;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.keyframe.BoneAnimation;
import software.bernie.geckolib.core.keyframe.Keyframe;
import software.bernie.geckolib.core.keyframe.KeyframeStack;
import software.bernie.geckolib.core.keyframe.event.data.CustomInstructionKeyframeData;
import software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.core.keyframe.event.data.SoundKeyframeData;
import com.eliotlash.mclib.math.Constant;
import com.eliotlash.mclib.math.IValue;
import java.util.List;

public final class PonyFlightPoseTest {
    private static int checks;
    private static final PonyFlightVisuals.Frame FRAME = new PonyFlightVisuals.Frame(1, 1, 7, 3, 12, -4, .15f);

    public static void main(String[] args) {
        for (String name : new String[] {"Body", "LForeLeg", "RForeLeg", "LHindLeg", "RHindLeg"}) {
            GeoBone bone = bone(name);
            for (int flags = 0; flags < 8; flags++) {
                flags(bone, flags);
                for (int frame = 0; frame < 600; frame++) {
                    var pose = PonyFlightPose.apply(bone, FRAME, true, false);
                    require(pose != null, "active scope");
                    boolean body = name.equals("Body");
                    near(bone.getRotX(), .12f - (float) Math.toRadians(body ? 7 : 12), "pitch sign");
                    near(bone.getRotZ(), .34f + (float) Math.toRadians(body ? 3 : -4), "roll sign");
                    near(bone.getPosY(), body ? 2.15f : 2, "only body bobs");
                    pose.close();
                    pose.close();
                    unchanged(bone, flags);
                }
                try (var pose = PonyFlightPose.apply(bone, FRAME, true, false)) {
                    throw new Expected();
                } catch (Expected expected) { unchanged(bone, flags); }
            }
            require(PonyFlightPose.apply(bone, FRAME, false, false) == null, "GUI excluded");
            require(PonyFlightPose.apply(bone, FRAME, true, true) == null, "rerender excluded");
            require(PonyFlightPose.apply(bone, PonyFlightVisuals.Frame.NONE, true, false) == null, "ground excluded");
            require(PonyFlightPose.apply(bone, new PonyFlightVisuals.Frame(1, 1, Float.NaN, 0, Float.NaN, 0, 0), true, false) == null,
                    "invalid rotation excluded");
        }
        for (String name : new String[] {"Root", "Neck", "Head", "Emotions", "Tail", "LFrontCalf", "RFrontHoof", "LHindCalf", "RHindHoof"}) {
            GeoBone bone = bone(name);
            flags(bone, 0);
            require(PonyFlightPose.apply(bone, FRAME, true, false) == null, "unrelated/authored compensation untouched " + name);
            unchanged(bone, 0);
        }
        curlScopes(); foldedFrontScopes();
        System.out.println("PASS PonyFlightPoseTest: " + checks + " pose/isolation checks");
    }

    private static void curlScopes() {
        var source = new Animation("fly", 20, Animation.LoopType.LOOP,
                new BoneAnimation[] {new BoneAnimation("LHindCalf", stack(.218f, 0, 0), stack(0, .45f, 2), new KeyframeStack<>())},
                new Animation.Keyframes(new SoundKeyframeData[0], new ParticleKeyframeData[0], new CustomInstructionKeyframeData[0]));
        PonyFlightAnimations.resolve(source);
        for (float curl : new float[] {-.16f, -.04f, .1f, .35f, .4f}) {
            for (int mask = 0; mask < 8; mask++) {
                GeoBone bone = bone("LHindCalf"); flags(bone, mask);
                var frame = new PonyFlightVisuals.Frame(1, 1, 0, 0, 12, -4, .15f, curl);
                try (var pose = PonyFlightPose.apply(bone, frame, true, false)) {
                    require(pose != null, "hind calf retains coherent curl");
                    near(bone.getRotX(), .12f + .218f * curl, "authored hind knee rotation follows curl");
                    near(bone.getPosY(), 2 + .45f * curl, "authored vertical compensation follows same curl");
                    near(bone.getPosZ(), 3 + 2 * curl, "authored depth compensation follows same curl");
                    near(bone.getRotZ(), .34f, "upper-leg inertia is not applied twice to calf");
                    near(bone.getPivotY(), 0, "joint pivot stays fixed");
                }
                unchanged(bone, mask);
                require(PonyFlightPose.apply(bone, frame, false, false) == null, "curl excluded from GUI");
                require(PonyFlightPose.apply(bone, frame, true, true) == null, "curl excluded from rerender");
            }
        }
        var invalid = new PonyFlightVisuals.Frame(1, 1, 0, 0, 0, 0, 0, Float.NaN);
        require(PonyFlightPose.apply(bone("LHindCalf"), invalid, true, false) == null, "nonfinite curl fails closed");
        PonyFlightAnimations.resolve(null);
        require(PonyFlightPose.apply(bone("LHindCalf"), new PonyFlightVisuals.Frame(1, 1, 0, 0, 0, 0, 0, .3f), true, false) == null,
                "resource reset removes prior curl channels");
    }

    private static KeyframeStack<Keyframe<IValue>> stack(float x, float y, float z) {
        return new KeyframeStack<>(axis(x), axis(y), axis(z));
    }

    private static void foldedFrontScopes() {
        var source = new Animation("fly", 20, Animation.LoopType.LOOP,
                new BoneAnimation[] {
                        new BoneAnimation("LForeLeg", stack(1.66f, 0, -.087f), stack(0, 0, 0), new KeyframeStack<>()),
                        new BoneAnimation("RForeLeg", stack(1.57f, 0, .087f), stack(0, 0, 0), new KeyframeStack<>()),
                        new BoneAnimation("LFrontCalf", stack(-1.8326f, 0, 0), stack(0, .45f, 2), new KeyframeStack<>()),
                        new BoneAnimation("RFrontCalf", stack(-1.8326f, 0, 0), stack(0, .4f, 2.1f), new KeyframeStack<>()),
                        new BoneAnimation("LFrontHoof", stack(-.916f, 0, 0), stack(0, -1, 0), new KeyframeStack<>()),
                        new BoneAnimation("RFrontHoof", stack(-.61f, 0, 0), stack(0, -1, 0), new KeyframeStack<>()),
                        new BoneAnimation("LHindCalf", stack(.218f, 0, 0), stack(0, 0, 0), new KeyframeStack<>())},
                new Animation.Keyframes(new SoundKeyframeData[0], new ParticleKeyframeData[0], new CustomInstructionKeyframeData[0]));
        PonyFlightAnimations.resolve(source);
        for (float lift : new float[] {-4, 0, 4, 15, 26, 30, 34}) {
            var frame = new PonyFlightVisuals.Frame(1, 1, 0, 0, 12, -4, .15f, -.16f, lift);
            for (String name : new String[] {"LForeLeg", "RForeLeg", "LFrontCalf", "RFrontCalf", "LFrontHoof", "RFrontHoof", "LHindCalf"}) {
                var authored = PonyFlightAnimations.limb(name);
                boolean upper = PonyFlightAnimations.isFrontUpper(name), hind = name.equals("LHindCalf");
                GeoBone bone = bone(name); flags(bone, 3);
                try (var pose = PonyFlightPose.apply(bone, frame, true, false)) {
                    require((pose != null) == (upper || hind), "only front root or original hind curl changes");
                    near(bone.getRotX(), .12f + (upper ? (float) Math.toRadians(lift - 12) : hind ? authored.rx() * -.16f : 0),
                            "root receives lift plus existing inertia; child local fold stays fixed");
                    near(bone.getRotZ(), .34f + (upper ? (float) Math.toRadians(-4) : 0), "horizontal roll remains root-only");
                    near(bone.getPosY(), 2, "front joint Y compensation never changes with root lift");
                    near(bone.getPosZ(), 3, "front joint Z compensation never changes with root lift");
                }
                unchanged(bone, 3);
                require(PonyFlightPose.apply(bone, frame, false, false) == null, "root lift excluded from GUI");
                require(PonyFlightPose.apply(bone, frame, true, true) == null, "root lift excluded from rerender");
            }
        }
        var invalid = new PonyFlightVisuals.Frame(1, 1, 0, 0, 0, 0, 0, .1f, Float.NaN);
        require(PonyFlightPose.apply(bone("LForeLeg"), invalid, true, false) == null, "invalid root lift fails closed");
        PonyFlightAnimations.resolve(null);
    }
    private static List<Keyframe<IValue>> axis(float value) {
        return List.of(new Keyframe<>(20, new Constant(value), new Constant(value)));
    }

    private static GeoBone bone(String name) {
        GeoBone bone = new GeoBone(null, name, false, 0d, false, false);
        bone.updateRotation(.12f, .23f, .34f);
        bone.updatePosition(1, 2, 3);
        bone.updateScale(.9f, 1.1f, 1.2f);
        return bone;
    }
    private static void flags(GeoBone bone, int flags) {
        bone.resetStateChanges();
        if ((flags & 1) != 0) bone.markRotationAsChanged();
        if ((flags & 2) != 0) bone.markPositionAsChanged();
        if ((flags & 4) != 0) bone.markScaleAsChanged();
    }
    private static void unchanged(GeoBone b, int flags) {
        exact(b.getRotX(), .12f); exact(b.getRotY(), .23f); exact(b.getRotZ(), .34f);
        exact(b.getPosX(), 1); exact(b.getPosY(), 2); exact(b.getPosZ(), 3);
        exact(b.getScaleX(), .9f); exact(b.getScaleY(), 1.1f); exact(b.getScaleZ(), 1.2f);
        require(b.hasRotationChanged() == ((flags & 1) != 0), "rotation flag restored");
        require(b.hasPositionChanged() == ((flags & 2) != 0), "position flag restored");
        require(b.hasScaleChanged() == ((flags & 4) != 0), "scale flag restored");
    }
    private static void exact(float a, float b) { require(Float.floatToIntBits(a) == Float.floatToIntBits(b), "exact transform restore"); }
    private static void near(float a, float b, String message) { require(Math.abs(a - b) < .00001, message); }
    private static void require(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
    private static final class Expected extends RuntimeException {}
}
