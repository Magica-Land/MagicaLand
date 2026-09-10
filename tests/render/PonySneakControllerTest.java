package top.csituka.magicaland.client.animation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.model.CoreBakedGeoModel;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.keyframe.BoneAnimation;
import software.bernie.geckolib.core.keyframe.event.data.CustomInstructionKeyframeData;
import software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.core.keyframe.event.data.SoundKeyframeData;
import software.bernie.geckolib.core.object.PlayState;

public final class PonySneakControllerTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        near(PonySneakController.speed("sneak", .26), 1, "normal flat-ground crouch retains authored pace");
        near(PonySneakController.speed("sneak", .208), .8, "twenty-percent slower motion produces twenty-percent slower pace");
        near(PonySneakController.speed("sneak", .13), .5, "actual slower movement proportionally reduces pace");
        near(PonySneakController.speed("sneak", 1.5), 1, "high speed, damage limb boost or fast sneak does not accelerate existing animation");
        near(PonySneakController.speed("sneak", 0), .25, "very slow input has a bounded nonzero pace");
        near(PonySneakController.speed("sneak", -1), .25, "negative input is safely bounded");
        for (double invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
            near(PonySneakController.speed("sneak", invalid), 1, "invalid motion returns authored pace");
        for (String action : new String[] {null, "sneaking", "idle", "walk", "run", "backward_walk", "fly", "jump1", "attacked", "blink_parallel", "ear_parallel", "tail_parallel"})
            near(PonySneakController.speed(action, .208), 1, "unrelated or stationary action stays normal: " + action);

        var model = new Model();
        var actor = new Actor();
        var continuous = new Probe(actor);
        var original = new Original(actor);
        for (int frame = 0; frame < 1600; frame++) {
            double tick = 100 + frame / 4.0;
            if (frame == 160) actor.action = "idle";
            if (frame == 360) actor.action = null;
            if (frame == 420) actor.action = "sneaking";
            if (frame == 650) actor.action = "sneak";
            if (frame == 810) { continuous.forceAnimationReset(); original.forceAnimationReset(); }
            run(continuous, model, actor, tick); run(original, model, actor, tick);
            equivalent(continuous, original, "unit-speed loop/transition/stop behavior remains vanilla at frame " + frame);
        }

        actor = new Actor(); actor.action = "long";
        continuous = new Probe(actor);
        original = new Original(actor);
        for (int frame = 0; frame <= 40; frame++) {
            run(continuous, model, actor, 100 + frame / 4.0);
            run(original, model, actor, 100 + frame / 4.0);
        }
        double phase = continuous.phase;
        continuous.setPlaybackSpeed(.8); original.setAnimationSpeed(.8);
        run(continuous, model, actor, 110); run(original, model, actor, 110);
        near(continuous.phase, phase, "changing speed on the same frame does not rescale elapsed animation");
        check(original.phase < phase, "real Gecko direct setAnimationSpeed reproduces the avoided backwards jump");
        run(continuous, model, actor, 110.25);
        near(continuous.phase, phase + .2, "subsequent progress uses the new speed incrementally");
        continuous.setPlaybackSpeed(1);
        run(continuous, model, actor, 110.25);
        near(continuous.phase, phase + .2, "returning to normal speed cannot jump forward");
        run(continuous, model, actor, 110.5);
        near(continuous.phase, phase + .45, "normal speed resumes with its original increment");
        run(continuous, model, actor, 110.4);
        near(continuous.phase, phase + .45, "a regressing input clock never rewinds pose");
        run(continuous, model, actor, 110.5);
        near(continuous.phase, phase + .45, "returning to the previous high-water tick cannot count time twice");
        run(continuous, model, actor, 110.75);
        near(continuous.phase, phase + .7, "only progress beyond the high-water tick advances pose");
        run(continuous, model, actor, Double.NaN);
        near(continuous.phase, phase + .7, "invalid time cannot contaminate pose");

        actor = new Actor(); continuous = new Probe(actor); original = new Original(actor);
        double localTick = 900, previous = 900;
        for (int frame = 0; frame < 2400; frame++) {
            double tick = 900 + frame / 5.0;
            double speed = frame < 180 ? 1 : frame < 500 ? .8 : frame < 800 ? .25 : frame < 1100 ? .63 : 1;
            if (frame == 850) actor.action = "idle";
            if (frame == 1150) actor.action = null;
            if (frame == 1200) actor.action = "sneaking";
            if (frame == 1400) actor.action = "sneak";
            if (frame == 1750) { continuous.forceAnimationReset(); original.forceAnimationReset(); }
            continuous.setPlaybackSpeed(speed);
            localTick += (tick - previous) * speed; previous = tick;
            run(continuous, model, actor, tick); run(original, model, actor, localTick);
            equivalent(continuous, original, "mixed-speed playback delegates identical reset/loop semantics at frame " + frame);
        }

        var source = Files.readString(Path.of(args[0], "src/client/java/top/csituka/magicaland/client/model/GeckoPlayerAnimatable.java"));
        check(source.contains("new PonySneakController<>(this, \"controller\", 3, this::predicate)"), "only main controller uses locomotion timeline");
        for (String controller : new String[] {"blink_controller", "expression_controller", "ear_controller", "tail_controller"})
            check(source.contains("new AnimationController<>(this, \"" + controller + "\""), "parallel controller retains original clock: " + controller);
        check(source.contains("player.limbAnimator.getSpeed(state.getPartialTick())"), "world motion feeds pace for local and remote players");
        check(source.contains("PonySneakController.speed(mainAnimationName,"), "pace uses the resolved and remotely synchronized action");
        int predicate = source.indexOf("private PlayState predicate(");
        int normal = source.indexOf("controller.setPlaybackSpeed(1)", predicate);
        check(normal > predicate && normal < source.indexOf("if (player == null)", predicate), "all main predicate early exits restore normal speed");
        check(!source.contains("setAnimationSpeed("), "rendering never dynamically rescales Gecko elapsed ticks");
        var preview = Files.readString(Path.of(args[0], "src/client/java/top/csituka/magicaland/client/model/PonyPreviewAnimatable.java"));
        check(!preview.contains("PonySneakController") && preview.contains("void registerControllers("), "preview retains its independent original controllers");
        System.out.println("PASS PonySneakControllerTest: " + checks + " real Gecko continuity, phase/reset equivalence, speed bounds and integration checks");
    }

    private static void run(AnimationController<Actor> controller, Model model, Actor actor, double tick) {
        controller.process(model, new AnimationState<>(actor, 0, 0, .5f, false).withController(controller), Map.of(), Map.of(), tick, false);
    }

    private static void equivalent(Probe actual, Original expected, String reason) {
        near(actual.phase, expected.phase, reason + " phase");
        check(actual.getAnimationState() == expected.getAnimationState(), reason + " state");
        check(java.util.Objects.equals(actual.getCurrentRawAnimation(), expected.getCurrentRawAnimation()), reason + " animation");
    }

    private static class Probe extends PonySneakController<Actor> {
        double phase;
        Probe(Actor actor) { super(actor, "controller", 3, Actor::handle); }
        @Override protected double adjustTick(double tick) { return phase = super.adjustTick(tick); }
    }

    private static class Original extends AnimationController<Actor> {
        double phase;
        Original(Actor actor) { super(actor, "controller", 3, Actor::handle); }
        @Override protected double adjustTick(double tick) { return phase = super.adjustTick(tick); }
    }

    private static class Actor implements GeoAnimatable {
        String action = "sneak";
        final Map<String, RawAnimation> animations = new HashMap<>();
        static PlayState handle(AnimationState<Actor> state) {
            Actor actor = state.getAnimatable();
            return actor.action == null ? PlayState.STOP : state.setAndContinue(actor.animations.computeIfAbsent(actor.action, name -> RawAnimation.begin().thenLoop(name)));
        }
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return null; }
        @Override public double getTick(Object object) { return 0; }
    }

    private static class Model implements CoreGeoModel<Actor> {
        final AnimationProcessor<Actor> processor = new AnimationProcessor<>(this);
        @Override public CoreBakedGeoModel getBakedGeoModel(String resource) { return null; }
        @Override public AnimationProcessor<Actor> getAnimationProcessor() { return processor; }
        @Override public Animation getAnimation(Actor actor, String name) {
            return new Animation(name, name.equals("long") ? 10000 : 20.834, Animation.LoopType.LOOP, new BoneAnimation[0],
                    new Animation.Keyframes(new SoundKeyframeData[0], new ParticleKeyframeData[0], new CustomInstructionKeyframeData[0]));
        }
        @Override public void handleAnimations(Actor actor, long id, AnimationState<Actor> state) {}
    }

    private static void near(double actual, double expected, String reason) { check(Double.isFinite(actual) && Math.abs(actual - expected) < 1e-8, reason + ": " + actual + " != " + expected); }
    private static void check(boolean condition, String reason) { checks++; if (!condition) throw new AssertionError(reason); }
}
