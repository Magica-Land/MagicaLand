package top.csituka.magicaland.client.animation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.keyframe.BoneAnimation;
import software.bernie.geckolib.core.keyframe.event.data.CustomInstructionKeyframeData;
import software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.core.keyframe.event.data.SoundKeyframeData;
import software.bernie.geckolib.loading.json.typeadapter.BakedAnimationsAdapter;
import software.bernie.geckolib.loading.object.BakedAnimations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public final class PonyIdleEarsTest {
    private static int checks;
    private static final UUID ID = new UUID(31, 86);
    private static final Object OWNER = new Object(), WORLD = new Object();
    public static void main(String[] args) throws Exception {
        policy(); distribution(); schedule(); animation(Path.of(args[0]));
        System.out.println("PASS PonyIdleEarsTest: " + checks + " probability/sync/isolation/authored-curve checks");
    }

    private static void policy() {
        require(PonyIdleEars.allowed("idle", true, true), "healthy idle allowed");
        require(!PonyIdleEars.allowed("idle", false, true), "death/hurt/sleep/movement safety gates");
        require(!PonyIdleEars.allowed("idle", true, false), "remote idle permission required");
        for (String action : new String[] {"walk", "backward_walk", "run", "sneak", "sneaking", "jump1", "fall", "land",
                "attacked", "sleep", "sit", "ride", "boat", "swim", "fly", "slow_walk_earsdown", "pp_walk", "break", "wave"})
            require(!PonyIdleEars.allowed(action, true, true), "non-idle / author ear pose wins: " + action);
        require(!PonyIdleEars.allowed(null, true, true), "no guessed idle before main action is available");
    }

    private static void distribution() {
        int[] sides = new int[3];
        int events = 0, skipped = 0, doubles = 0;
        var variants = new HashSet<Integer>(); var offsets = new HashSet<Integer>();
        for (int window = 1; window <= 10000; window++) {
            var event = PonyIdleEars.candidate(ID, window * 100d);
            if (event == null) { skipped++; continue; }
            events++; sides[event.variant() / 12]++;
            if (event.variant() % 12 >= 6) doubles++;
            variants.add(event.variant()); offsets.add((int) event.at() % 100);
            require(event.equals(PonyIdleEars.candidate(ID, window * 100d)), "same UUID/world clock has stable candidate");
            require(event.variant() >= 0 && event.variant() < PonyIdleEarAnimations.VARIANTS, "bounded variant pool");
        }
        require(events > 4600 && events < 5400 && skipped > 4600, "windows sometimes trigger, sometimes skip");
        require(Math.abs(sides[0] - sides[1]) < events * .06, "left and right have balanced opportunities");
        require(sides[2] > events * .06 && sides[2] < events * .14, "both ears occasional, not mirrored every time");
        require(doubles > events * .15 && doubles < events * .25, "single flick is the common pattern");
        require(variants.size() == 36, "all amplitude/speed/side variants reachable");
        require(offsets.size() > 35, "event timing not fixed within each window");
        int different = 0; UUID other = new UUID(31, 87);
        for (int window = 1; window < 100; window++)
            if (!java.util.Objects.equals(PonyIdleEars.candidate(ID, window * 100d), PonyIdleEars.candidate(other, window * 100d))) different++;
        require(different > 65, "players do not share one random sequence");
    }

    private static void schedule() {
        List<PonyIdleEars.Event> reference = null;
        for (int fps : new int[] {20, 30, 60, 144}) {
            var state = new PonyIdleEars(); var events = new ArrayList<PonyIdleEars.Event>();
            PonyIdleEars.Event previous = null;
            for (int frame = 0; frame <= fps * 90; frame++) {
                double ticks = frame * 20d / fps;
                var event = state.sample(OWNER, WORLD, ID, ticks, true);
                require(java.util.Objects.equals(event, state.sample(OWNER, WORLD, ID, ticks, true)), "repeated render does not reroll");
                if (event != null && !event.equals(previous)) events.add(event);
                previous = event;
            }
            require(events.size() > 1, "visible idle eventually gets occasional events");
            if (reference == null) reference = events;
            require(reference.equals(events), "frame rate does not change event choice or scheduled time");
            for (int i = 1; i < events.size(); i++) require(events.get(i).at() - events.get(i - 1).at() >= 53, "no consecutive rapid surprise flicks");
        }
        var activeEvent = reference.get(0); var state = new PonyIdleEars();
        for (int tick = 0; tick <= activeEvent.at(); tick++) state.sample(OWNER, WORLD, ID, tick, true);
        require(state.sample(OWNER, WORLD, ID, activeEvent.at(), false) == null, "movement/hurt/author pose immediately stops ear controller");
        require(state.sample(OWNER, WORLD, ID, activeEvent.at() + 1, true) == null, "return to idle does not replay just-cancelled event");
        require(state.sample(OWNER, WORLD, ID, 0, true) == null, "time rewind does not catch up old flicks");
        require(state.sample(OWNER, WORLD, ID, 10000, true) == null, "offscreen gap has no catch-up burst");
        require(state.sample(new Object(), WORLD, ID, 10001, true) == null, "entity replacement isolated");
        require(state.sample(OWNER, new Object(), ID, 10002, true) == null, "world replacement isolated");
        require(state.sample(OWNER, WORLD, ID, Double.NaN, true) == null, "invalid time safe");
        state.reset();
        require(state.sample(OWNER, WORLD, ID, activeEvent.at(), true) == null, "first render on event time does not instantly twitch");
        require(state.sample(null, WORLD, ID, 0, true) == null, "preview/null owner suppressed");
    }

    private static void animation(Path repo) throws Exception {
        JsonObject all = JsonParser.parseString(Files.readString(repo.resolve("appearance/src/main/resources/assets/magicaland/animations/mare_animation.json")))
                .getAsJsonObject().getAsJsonObject("animations");
        JsonObject earOnly = new JsonObject(); earOnly.add("ear_parallel", all.get("ear_parallel"));
        Gson gson = new GsonBuilder().registerTypeAdapter(BakedAnimations.class, new BakedAnimationsAdapter())
                .registerTypeAdapter(Animation.Keyframes.class, (JsonDeserializer<Animation.Keyframes>) (json, type, context) ->
                        new Animation.Keyframes(new SoundKeyframeData[0], new ParticleKeyframeData[0], new CustomInstructionKeyframeData[0])).create();
        Animation source = gson.fromJson(earOnly, BakedAnimations.class).getAnimation("ear_parallel");
        String fingerprint = fingerprint(source);
        require(source.length() == 220 && source.boneAnimations().length == 1, "current author source is 11-second right-ear loop");
        Animation[] variants = new Animation[36];
        for (int i = 0; i < 36; i++) {
            String name = "internal.idle_ear." + i;
            require(PonyIdleEarAnimations.internal(name), "internal name recognized");
            var animation = PonyIdleEarAnimations.resolve(source, name); variants[i] = animation;
            require(animation == PonyIdleEarAnimations.resolve(source, name), "variant caching stable");
            require(PonyIdleEarAnimations.raw(i) == PonyIdleEarAnimations.raw(i), "raw animation identity stable");
            require(animation.loopType() == Animation.LoopType.PLAY_ONCE, "no fixed repeating flick");
            require(animation.length() > 4 && animation.length() < 16, "old six-second wait removed, bounded short clip");
            require(animation.boneAnimations().length == (i / 12 == 2 ? 2 : 1), "selected one/two ears only");
            for (BoneAnimation bone : animation.boneAnimations()) {
                boolean left = bone.boneName().equals("LeftEar");
                require(left || bone.boneName().equals("RightEar"), "no non-ear bone track");
                require(bone.positionKeyFrames().xKeyframes().isEmpty() && bone.scaleKeyFrames().xKeyframes().isEmpty(), "no geometry position/scale alteration");
                var ys = bone.rotationKeyFrames().yKeyframes(); var zs = bone.rotationKeyFrames().zKeyframes();
                require(ys.get(0).startValue().get() == 0 && zs.get(0).startValue().get() == 0, "starts neutral");
                require(ys.get(ys.size() - 1).endValue().get() == 0 && zs.get(zs.size() - 1).endValue().get() == 0, "finishes neutral before controller stops");
                double peakY = 0, peakZ = 0;
                for (var frame : ys) peakY = Math.max(peakY, Math.abs(frame.endValue().get()));
                for (var frame : zs) {
                    peakZ = Math.max(peakZ, Math.abs(frame.endValue().get()));
                    require(left ? frame.endValue().get() >= 0 : frame.endValue().get() <= 0, "mirrored outward ear direction");
                }
                require(peakY > 0 && peakY <= Math.toRadians(25) * .551, "yaw is 35-55 percent of author curve");
                require(peakZ >= Math.toRadians(60) * .349 && peakZ <= Math.toRadians(60) * .551, "gentle roll, never flattened emotional ears");
            }
            require(fingerprint.equals(fingerprint(source)), "source tracks / spline arguments / length untouched");
        }
        for (int i = 0; i < 12; i++) {
            var left = variants[i].boneAnimations()[0].rotationKeyFrames(); var right = variants[12 + i].boneAnimations()[0].rotationKeyFrames();
            for (int frame = 0; frame < left.zKeyframes().size(); frame++)
                near(left.zKeyframes().get(frame).endValue().get(), -right.zKeyframes().get(frame).endValue().get(), "left/right shape faithfully mirrored");
        }
        for (int i = 24; i < 36; i++) {
            var left = variants[i].boneAnimations()[0].rotationKeyFrames().zKeyframes();
            var right = variants[i].boneAnimations()[1].rotationKeyFrames().zKeyframes();
            boolean leftDelayed = left.get(0).length() == 2 && left.get(0).endValue().get() == 0;
            boolean rightDelayed = right.get(0).length() == 2 && right.get(0).endValue().get() == 0;
            require(leftDelayed != rightDelayed, "two ears stagger rather than perfectly simultaneous");
            require(leftDelayed == (i % 2 == 1), "either ear can lead paired movement");
        }
        Animation reloaded = gson.fromJson(earOnly, BakedAnimations.class).getAnimation("ear_parallel");
        require(PonyIdleEarAnimations.resolve(reloaded, "internal.idle_ear.0") != variants[0], "resource reload replaces bounded variant cache");
        require(PonyIdleEarAnimations.resolve(null, "internal.idle_ear.0").boneAnimations().length == 0, "missing custom-source ear animation safely disabled");
        require(!PonyIdleEarAnimations.internal("ear_parallel") && !PonyIdleEarAnimations.internal("internal.idle_ear.36"), "normal assets / invalid variants not intercepted");
        require(PonyIdleEarAnimations.resolve(source, "idle") == null, "main animation lookup unmodified");
    }

    private static String fingerprint(Animation source) {
        StringBuilder text = new StringBuilder().append(source.name()).append(source.length());
        for (var bone : source.boneAnimations()) {
            text.append(bone.boneName());
            for (var list : List.of(bone.rotationKeyFrames().xKeyframes(), bone.rotationKeyFrames().yKeyframes(), bone.rotationKeyFrames().zKeyframes()))
                for (var frame : list) {
                    text.append(frame.length()).append(frame.startValue().get()).append(frame.endValue().get());
                    for (var arg : frame.easingArgs()) text.append(arg.get());
                }
        }
        return text.toString();
    }
    private static void near(double actual, double expected, String label) { require(Math.abs(actual - expected) < .000001, label); }
    private static void require(boolean condition, String label) { checks++; if (!condition) throw new AssertionError(label); }
}
