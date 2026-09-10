package top.csituka.magicaland.client.animation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.keyframe.BoneAnimation;
import software.bernie.geckolib.core.keyframe.Keyframe;
import software.bernie.geckolib.core.keyframe.KeyframeStack;
import com.eliotlash.mclib.math.Constant;
import com.eliotlash.mclib.math.IValue;
import software.bernie.geckolib.core.keyframe.event.data.CustomInstructionKeyframeData;
import software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.core.keyframe.event.data.SoundKeyframeData;
import software.bernie.geckolib.loading.json.typeadapter.BakedAnimationsAdapter;
import software.bernie.geckolib.loading.object.BakedAnimations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class PonyFlightAnimationsTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        Path repo = Path.of(args[0]);
        Gson gson = new GsonBuilder().registerTypeAdapter(BakedAnimations.class, new BakedAnimationsAdapter())
                .registerTypeAdapter(Animation.Keyframes.class, (JsonDeserializer<Animation.Keyframes>) (json, type, context) ->
                        new Animation.Keyframes(new SoundKeyframeData[0], new ParticleKeyframeData[0], new CustomInstructionKeyframeData[0])).create();
        String first = null;
        for (String file : List.of("src/main/resources/assets/magicaland/animations/mare_animation.json",
                "Resources/Animations/mare_animation.json", "Resources/Animations/changeling_animation.json")) {
            JsonObject json = JsonParser.parseString(Files.readString(repo.resolve(file))).getAsJsonObject().getAsJsonObject("animations");
            String original = json.toString();
            JsonObject selected = new JsonObject(); selected.add("fly", json.get("fly"));
            Animation source = gson.fromJson(selected, BakedAnimations.class).getAnimation("fly");
            String before = fingerprint(source);
            Animation pose = PonyFlightAnimations.resolve(source);
            require(pose == PonyFlightAnimations.resolve(source), "cached until resource reload");
            require(pose.name().equals(PonyFlightAnimations.NAME), "internal derivative identifier");
            require(pose.loopType() == Animation.LoopType.LOOP, "static hold loops");
            require(pose.boneAnimations().length == 16, "twelve authored limbs and four standing neutral tracks");
            String fingerprint = fingerprint(pose);
            if (first == null) first = fingerprint;
            else require(first.equals(fingerprint), "mare / changeling / author copies use same joint pose");
            for (BoneAnimation bone : pose.boneAnimations()) {
                if (List.of("Root", "Body", "Neck", "Head").contains(bone.boneName())) {
                    for (var stack : List.of(bone.rotationKeyFrames(), bone.positionKeyFrames(), bone.scaleKeyFrames()))
                        for (var frames : List.of(stack.xKeyframes(), stack.yKeyframes(), stack.zKeyframes())) {
                            require(frames.size() == 1, "body/head hold neutral without cycles");
                            double expected = stack == bone.scaleKeyFrames() ? 1 : 0;
                            near(frames.get(0).startValue().get(), expected, "identity start");
                            near(frames.get(0).endValue().get(), expected, "identity end");
                        }
                    continue;
                }
                JsonObject originalBone = json.getAsJsonObject("fly").getAsJsonObject("bones").getAsJsonObject(bone.boneName());
                var rotations = bone.rotationKeyFrames(); var positions = bone.positionKeyFrames();
                require(bone.scaleKeyFrames().xKeyframes().isEmpty(), "no flight-cycle squash/stretch");
                for (int channel = 0; channel < 2; channel++) {
                    String key = channel == 0 ? "rotation" : "position";
                    var stack = channel == 0 ? rotations : positions;
                    var frames = List.of(stack.xKeyframes(), stack.yKeyframes(), stack.zKeyframes());
                    if (!originalBone.has(key)) { require(frames.stream().allMatch(List::isEmpty), "missing channels stay unbound"); continue; }
                    var expected = originalBone.get(key);
                    if (expected.isJsonObject()) expected = expected.getAsJsonObject().get("0.0");
                    if (expected.isJsonObject()) expected = expected.getAsJsonObject().get("post");
                    for (int axis = 0; axis < 3; axis++) {
                        require(frames.get(axis).size() == 1, "constant pose no flap cycle");
                        var frame = frames.get(axis).get(0);
                        double value = expected.getAsJsonArray().get(axis).getAsDouble();
                        if (channel == 0) value = Math.toRadians(value) * (axis == 2 ? 1 : -1);
                        var held = PonyFlightAnimations.pose(bone.boneName());
                        float[] expectedPose = channel == 0 ? new float[] {held.rx(), held.ry(), held.rz()} : new float[] {held.x(), held.y(), held.z()};
                        near(frame.startValue().get(), expectedPose[axis], "derived animation matches fixed folded chain");
                        near(frame.endValue().get(), expectedPose[axis], "constant for complete loop");
                        double factor = PonyFlightAnimations.isFrontLeg(bone.boneName()) ? .95f : .60f;
                        double lowering = channel == 0 && axis == 0 && PonyFlightAnimations.isFrontUpper(bone.boneName()) ? Math.toRadians(30) : 0;
                        near(expectedPose[axis], value * factor - lowering, "fixed curl and original compensation; only front root lowers");
                        var limb = PonyFlightAnimations.limb(bone.boneName());
                        float[] authored = channel == 0 ? new float[] {limb.rx(), limb.ry(), limb.rz()}
                                : new float[] {limb.x(), limb.y(), limb.z()};
                        near(authored[axis], value, "dynamic curl retains exact authored transform for this resource");
                    }
                }
            }
            frontChain(repo);
            require(before.equals(fingerprint(source)) && original.equals(json.toString()), "source never mutated");
            Animation reloaded = gson.fromJson(selected, BakedAnimations.class).getAnimation("fly");
            require(pose != PonyFlightAnimations.resolve(reloaded), "reload updates source-derived pose");
        }
        sparseAxes();
        require(PonyFlightAnimations.resolve(null).boneAnimations().length == 0, "missing source fails closed");
        require(PonyFlightAnimations.limb("LFrontCalf") == null, "missing/reloaded source never keeps stale limb offsets");
        String animatable = Files.readString(repo.resolve("src/client/java/top/csituka/magicaland/client/model/GeckoPlayerAnimatable.java"));
        require(animatable.contains("new AnimationSelection(action, flightAnimation(action))"), "legacy wire action names retained");
        require(animatable.contains("case \"fly\", \"elytra_fly\" -> flightAnimation(name)"), "remote receiver selects local appearance pose");
        require(animatable.contains("controller.transitionLength(PonyFlightAnimations.transitionTicks("), "uses tested transition policy");
        require(animatable.contains("player.getAbilities().flying && (flightConfig == null || flightConfig.showWings)"), "winged selection preserves original flying flag policy");
        RawAnimation winged = RawAnimation.begin().thenLoop("fly");
        RawAnimation wingedSprint = RawAnimation.begin().thenLoop("elytra_fly");
        RawAnimation idle = RawAnimation.begin().thenLoop("idle");
        RawAnimation current = winged;
        for (int i = 0; i < 20; i++) {
            RawAnimation hover = PonyFlightAnimations.select(true, i % 2 == 0 ? winged : wingedSprint);
            require(hover != current && hover == PonyFlightAnimations.RAW, "wing toggle forces different raw key despite same wire name");
            require(PonyFlightAnimations.transitionTicks(current, hover) == 5, "wingless enter 5 ticks");
            require(PonyFlightAnimations.select(true, wingedSprint) == hover, "sprint changes expression without restarting constant hover pose");
            current = PonyFlightAnimations.select(false, i % 2 == 0 ? winged : wingedSprint);
            require(current != hover && current != PonyFlightAnimations.RAW, "restoring wings restores authored raw key");
            require(PonyFlightAnimations.transitionTicks(hover, current) == 4, "leave hover 4 ticks");
        }
        require(PonyFlightAnimations.transitionTicks(PonyFlightAnimations.RAW, idle) == 4, "landing exit 4 ticks");
        require(PonyFlightAnimations.transitionTicks(idle, winged) == 3, "old winged transitions unchanged");
        String visual = Files.readString(repo.resolve("src/client/java/top/csituka/magicaland/client/animation/PonyFlightVisuals.java"));
        require(visual.contains("getRemoteAnimation(player.getUuid(), \"controller\")"), "remote signal works without abilities sync");
        require(visual.contains("config.showWings || !eligible(player)"), "winged priority and safety gating");
        require(visual.contains("DISCONNECT.register") && visual.contains("world.getPlayerByUuid"), "lifecycle and removal cleanup");
        require(visual.contains("config.showHorn, sprinting(player)")
                && visual.contains("return \"elytra_fly\".equals(ClientNetworkHandler.getRemoteAnimation"),
                "sprint pose follows local sprint and authoritative remote action without extra wire state");
        require(visual.contains("pose.frontLift()"), "smooth front root lift reaches render frame");
        System.out.println("PASS PonyFlightAnimationsTest: " + checks + " checks");
    }

    private static String fingerprint(Animation animation) {
        StringBuilder result = new StringBuilder(animation.name());
        for (BoneAnimation bone : animation.boneAnimations()) {
            result.append(bone.boneName());
            for (var stack : List.of(bone.rotationKeyFrames(), bone.positionKeyFrames(), bone.scaleKeyFrames()))
                for (var frames : List.of(stack.xKeyframes(), stack.yKeyframes(), stack.zKeyframes()))
                    for (var frame : frames) result.append(frame.length()).append(frame.startValue().get()).append(frame.endValue().get());
        }
        return result.toString();
    }

    private static void sparseAxes() {
        var none = new KeyframeStack<Keyframe<IValue>>();
        Animation source = new Animation("fly", 20, Animation.LoopType.LOOP, new BoneAnimation[] {
                new BoneAnimation("LForeLeg", new KeyframeStack<>(axis(1.5f), List.of(), axis(-.08f)), none, none),
                new BoneAnimation("LFrontCalf", new KeyframeStack<>(axis(-1.8f), List.of(), List.of()), none, none),
                new BoneAnimation("LFrontHoof", new KeyframeStack<>(axis(-.8f), List.of(), List.of()), none, none)
        }, new Animation.Keyframes(new SoundKeyframeData[0], new ParticleKeyframeData[0], new CustomInstructionKeyframeData[0]));
        Animation derived = PonyFlightAnimations.resolve(source);
        var hoof = java.util.Arrays.stream(derived.boneAnimations()).filter(bone -> bone.boneName().equals("LFrontHoof")).findFirst().orElseThrow();
        var pose = PonyFlightAnimations.pose("LFrontHoof");
        near(pose.rx(), -.8f * .95f, "sparse hoof retains authored folded angle");
        require(hoof.rotationKeyFrames().yKeyframes().isEmpty() && hoof.rotationKeyFrames().zKeyframes().isEmpty(),
                "no independently generated hoof axes");
        require(hoof.positionKeyFrames().xKeyframes().isEmpty(), "unchanged missing zero channels stay unbound");
        require(PonyFlightAnimations.offset("LFrontHoof", Float.MAX_VALUE) == null, "front child never receives dynamic curl offsets");
    }
    private static List<Keyframe<IValue>> axis(float value) { return List.of(new Keyframe<>(20, new Constant(value), new Constant(value))); }

    private static void frontChain(Path repo) throws Exception {
        Map<String, JsonObject> geometry = new HashMap<>();
        var json = JsonParser.parseString(Files.readString(repo.resolve("src/main/resources/assets/magicaland/geo/mare_geo.json")))
                .getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
        for (var bone : json) geometry.put(bone.getAsJsonObject().get("name").getAsString(), bone.getAsJsonObject());
        for (String side : List.of("L", "R")) {
            String upper = side + "ForeLeg", calf = side + "FrontCalf", hoof = side + "FrontHoof";
            require(geometry.get(calf).get("parent").getAsString().equals(upper)
                    && geometry.get(hoof).get("parent").getAsString().equals(calf), "actual front chain inherits root rotation");
            var a = PonyFlightAnimations.limb(upper);
            var base = PonyFlightAnimations.pose(upper);
            Vector3f kneePivot = vector(geometry.get(calf).getAsJsonArray("pivot")); kneePivot.x = -kneePivot.x;
            float normalKneeY = transform(new Matrix4f(), geometry.get(upper), base)
                    .transformPosition(new Vector3f(kneePivot)).y;
            for (float lift : new float[] {-4, 0, 4, 15, 26, 30, 34}) {
                var pa = new PonyFlightAnimations.Limb(base.rx() + (float) Math.toRadians(lift), base.ry(), base.rz(), base.x(), base.y(), base.z());
                var pb = PonyFlightAnimations.pose(calf);
                var pc = PonyFlightAnimations.pose(hoof);
                for (String name : List.of(calf, hoof)) {
                    var original = PonyFlightAnimations.limb(name); var pose = PonyFlightAnimations.pose(name);
                    near(pose.rx(), original.rx() * .95f, "folded child X unchanged"); near(pose.ry(), original.ry() * .95f, "folded child Y unchanged");
                    near(pose.rz(), original.rz() * .95f, "folded child Z unchanged"); near(pose.x(), original.x() * .95f, "child offset X unchanged");
                    near(pose.y(), original.y() * .95f, "child offset Y unchanged"); near(pose.z(), original.z() * .95f, "child offset Z unchanged");
                }
                if (lift == 30) near(pa.rx(), a.rx() * .95f, "sprint restores original upper height");
                Matrix4f ma = transform(new Matrix4f(), geometry.get(upper), pa);
                Matrix4f mb = transform(ma, geometry.get(calf), pb);
                Matrix4f mc = transform(mb, geometry.get(hoof), pc);
                require(overlap(geometry.get(upper), ma, geometry.get(calf), mb), "upper/calf retain contact " + side + " lift=" + lift);
                require(overlap(geometry.get(calf), mb, geometry.get(hoof), mc), "calf/hoof retain contact " + side + " lift=" + lift);
                if (lift == 30) {
                    require(ma.transformPosition(new Vector3f(kneePivot)).y > normalKneeY + 2, "ordinary root rotation actually lowers knee from sprint");
                }
            }
        }
    }
    private static Quaternionf rotation(PonyFlightAnimations.Limb limb, float factor) {
        return new Quaternionf().rotateZ(limb.rz() * factor).rotateY(limb.ry() * factor).rotateX(limb.rx() * factor);
    }
    private static Vector3f vector(com.google.gson.JsonArray array) {
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }
    private static Matrix4f transform(Matrix4f parent, JsonObject bone, PonyFlightAnimations.Limb pose) {
        Vector3f pivot = vector(bone.getAsJsonArray("pivot")); pivot.x = -pivot.x;
        return new Matrix4f(parent).translate(-pose.x(), pose.y(), pose.z()).translate(pivot).rotate(rotation(pose, 1)).translate(pivot.negate());
    }
    private static boolean overlap(JsonObject first, Matrix4f firstMatrix, JsonObject second, Matrix4f secondMatrix) {
        Vector3f[][] corners = new Vector3f[2][]; Vector3f[][] axes = new Vector3f[2][3];
        JsonObject[] bones = {first, second}; Matrix4f[] matrices = {firstMatrix, secondMatrix};
        for (int box = 0; box < 2; box++) {
            var cube = bones[box].getAsJsonArray("cubes").get(0).getAsJsonObject();
            Vector3f origin = vector(cube.getAsJsonArray("origin")), size = vector(cube.getAsJsonArray("size"));
            origin.x = -origin.x - size.x; corners[box] = new Vector3f[8];
            for (int corner = 0; corner < 8; corner++) corners[box][corner] = matrices[box].transformPosition(new Vector3f(origin)
                    .add((corner & 1) == 0 ? 0 : size.x, (corner & 2) == 0 ? 0 : size.y, (corner & 4) == 0 ? 0 : size.z));
            for (int axis = 0; axis < 3; axis++) axes[box][axis] = matrices[box].transformDirection(new Vector3f(axis == 0 ? 1 : 0, axis == 1 ? 1 : 0, axis == 2 ? 1 : 0)).normalize();
        }
        var candidates = new java.util.ArrayList<Vector3f>(); candidates.addAll(List.of(axes[0])); candidates.addAll(List.of(axes[1]));
        for (Vector3f a : axes[0]) for (Vector3f b : axes[1]) candidates.add(a.cross(b, new Vector3f()));
        for (Vector3f axis : candidates) {
            if (axis.lengthSquared() < 1e-8) continue;
            float[] low = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY}, high = {Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
            for (int box = 0; box < 2; box++) for (Vector3f corner : corners[box]) { float point = corner.dot(axis); low[box] = Math.min(low[box], point); high[box] = Math.max(high[box], point); }
            if (high[0] < low[1] - .0001 || high[1] < low[0] - .0001) return false;
        }
        return true;
    }
    private static void near(double actual, double expected, String label) { require(Math.abs(actual - expected) < 1e-6, label + ": " + actual + " != " + expected); }
    private static void require(boolean condition, String label) { checks++; if (!condition) throw new AssertionError(label); }
}
