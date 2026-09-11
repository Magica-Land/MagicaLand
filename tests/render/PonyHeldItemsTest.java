package top.csituka.magicaland.client.render;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import org.joml.*;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.keyframe.event.data.*;
import software.bernie.geckolib.loading.json.typeadapter.BakedAnimationsAdapter;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.util.JsonUtil;
import software.bernie.geckolib.util.RenderUtils;
import top.csituka.magicaland.client.animation.PonyFlightAnimations;

public final class PonyHeldItemsTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        if (args.length > 1 && args[1].equals("--geometry-only")) {
            testGeometry(Path.of(args[0]));
            System.out.println("PASS PonyHeldItemsTest geometry-only: " + checks + " authored bone/matrix checks (not in-game)");
            return;
        }
        SharedConstants.createGameVersion(); Bootstrap.initialize();
        check(PonyHeldItems.grip(ItemStack.EMPTY, true) == PonyHeldItems.Grip.NONE, "empty hand");
        for (Item item : List.of(Items.IRON_SWORD, Items.DIAMOND_PICKAXE, Items.IRON_AXE, Items.WOODEN_HOE,
                Items.SHEARS, Items.TRIDENT, Items.FISHING_ROD, Items.BRUSH)) {
            var stack = new ItemStack(item);
            check(PonyHeldItems.grip(stack, true) == PonyHeldItems.Grip.MOUTH, "tool mouth grip " + item);
            check(PonyHeldItems.grip(stack, false) == PonyHeldItems.Grip.HOOF, "second tool cannot occupy same mouth");
        }
        for (Item item : List.of(Items.STONE, Items.TORCH, Items.APPLE, Items.POTION, Items.BOW))
            check(PonyHeldItems.grip(new ItemStack(item), true) == PonyHeldItems.Grip.HOOF, "ordinary hoof grip");
        check(PonyHeldItems.grip(new ItemStack(Items.SHIELD), true) == PonyHeldItems.Grip.SHIELD, "shield foreleg");
        for (boolean mainLeft : new boolean[]{true, false}) {
            var frame = new PonyHeldItems.Frame(null, ItemStack.EMPTY, ItemStack.EMPTY,
                    PonyHeldItems.Grip.MOUTH, PonyHeldItems.Grip.HOOF, mainLeft, .5f, Hand.MAIN_HAND, PonyHeldItems.Consumption.NONE);
            check(!frame.raises(mainLeft) && frame.raises(!mainLeft), "handedness and independent offhand");
            near(frame.mouthSwing(), .5, "mouth swing only its owning hand");
            frame = new PonyHeldItems.Frame(null, ItemStack.EMPTY, ItemStack.EMPTY,
                    PonyHeldItems.Grip.MOUTH, PonyHeldItems.Grip.HOOF, mainLeft, .5f, Hand.OFF_HAND, PonyHeldItems.Consumption.NONE);
            near(frame.mouthSwing(), 0, "offhand use does not shake mouth");
        }
        var weights = new PonyHeldItemPose.Weights(); weights.update(1, false, false);
        weights.update(2, true, false); near(weights.left, 1, "carrying pose appears immediately");
        near(weights.right, 0, "other arm remains unchanged");
        weights.update(2, true, true); near(weights.left, 1, "repeated frame keeps binary carrying weight");
        near(weights.right, 1, "offhand appears without a four-tick blend");
        weights.update(2, false, false); near(weights.left, 0, "putting away is immediate even in the same tick");
        near(weights.right, 0, "putting away clears both arms");
        weights.update(Double.NaN, true, true); near(weights.left, 0, "invalid time resets carrying state");
        weights.update(3, true, false); weights.reset(); near(weights.left, 0, "reset clears carrying weight");
        testConsumption();
        testGeometry(Path.of(args[0]));
        testFrameCues();
        testScopedApply();
        System.out.println("PASS PonyHeldItemsTest: " + checks + " item rules, original animation, matrix and state restoration checks (not in-game)");
    }
    private static void testGeometry(Path repo) throws Exception {
        var rawModel = JsonUtil.GEO_GSON.fromJson(Files.readString(repo.resolve("src/main/resources/assets/magicaland/geo/mare_geo.json")), Model.class);
        var model = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(GeometryTree.fromModel(rawModel));
        var animations = JsonParser.parseString(Files.readString(repo.resolve("src/main/resources/assets/magicaland/animations/mare_animation.json"))).getAsJsonObject().getAsJsonObject("animations");
        var selected = new JsonObject(); selected.add("fly", animations.get("fly"));
        Gson gson = new GsonBuilder().registerTypeAdapter(BakedAnimations.class, new BakedAnimationsAdapter())
                .registerTypeAdapter(Animation.Keyframes.class, (JsonDeserializer<Animation.Keyframes>)(json, type, ctx) ->
                        new Animation.Keyframes(new SoundKeyframeData[0], new ParticleKeyframeData[0], new CustomInstructionKeyframeData[0])).create();
        var source = gson.fromJson(selected, BakedAnimations.class).getAnimation("fly");
        PonyFlightAnimations.resolve(source);
        for (String side : List.of("L", "R")) {
            var upper = PonyHeldItemPose.carryingPose(side + "ForeLeg");
            var calf = PonyHeldItemPose.carryingPose(side + "FrontCalf");
            var hoof = PonyHeldItemPose.carryingPose(side + "FrontHoof");
            near(upper.ry(), (side.equals("L") ? 1 : -1) * java.lang.Math.PI, "idle carrying uses mirrored shoulder half-turns");
            for (String part : List.of("FrontCalf", "FrontHoof")) {
                var a = PonyFlightAnimations.limb(side + part); var b = PonyHeldItemPose.carryingPose(side + part);
                near(b.rx(), a.rx() * .95f, "authored folded child angle retained");
                near(b.x(), a.x() * .95f, "authored child X compensation retained");
                near(b.y(), a.y() * .95f, "authored child Y compensation retained");
                near(b.z(), a.z() * .95f, "authored child Z compensation retained");
            }
            var sole = new Matrix4f().rotateY(upper.ry()).rotateX(upper.rx()).rotateX(calf.rx()).rotateX(hoof.rx())
                    .transformDirection(new Vector3f(0, -1, 0));
            check(sole.y > .999, "held hoof sole points upward");
            float eatingPitch = (float) java.lang.Math.toRadians(24);
            var eatingUpper = PonyHeldItemPose.carryingPose(side + "ForeLeg", eatingPitch);
            near(eatingUpper.rx(), upper.rx() - eatingPitch, "consumption turns whole arm at shoulder");
            near(eatingUpper.ry() - upper.ry(), (side.equals("L") ? -1 : 1) * java.lang.Math.toRadians(8) * 24 / 28,
                    "consumption adds mirrored eight-degree inward shoulder turn scaled by pitch");
            near(eatingUpper.x(), upper.x(), "consumption preserves shoulder compensation");
            near(eatingUpper.y(), upper.y(), "consumption preserves shoulder Y compensation");
            near(eatingUpper.z(), upper.z(), "consumption preserves shoulder Z compensation");
            for (String part : List.of("FrontCalf", "FrontHoof"))
                check(PonyHeldItemPose.carryingPose(side + part, eatingPitch).equals(PonyHeldItemPose.carryingPose(side + part)),
                        "consumption never changes child joint angles or positions");
            check(hoofPoint(model, side, eatingPitch, false).y > hoofPoint(model, side, 0, false).y + 1,
                    "whole-arm rotation lifts the carried item upward, not down");
            for (int degree = 0; degree <= 32; degree++) {
                float pitch = (float) java.lang.Math.toRadians(degree);
                var actual = hoofPoint(model, side, pitch, false);
                var withoutInward = hoofPoint(model, side, pitch, true);
                float distance = java.lang.Math.abs(actual.x);
                check(Float.isFinite(actual.x) && Float.isFinite(actual.y) && Float.isFinite(actual.z), "authored hoof point stays finite");
                check(degree == 0 || distance < java.lang.Math.abs(withoutInward.x), "actual mare matrices pull the " + side + " hoof toward midline, not outward: " + actual);
                check(actual.x * withoutInward.x > 0, "gentle inward cue does not cross the body centerline");
            }
            for (float degrees : new float[]{-100, 0, 28, 32, 100}) {
                var clamped = PonyHeldItemPose.carryingPose(side + "ForeLeg", (float) java.lang.Math.toRadians(degrees));
                near(clamped.ry() - upper.ry(), (side.equals("L") ? -1 : 1) * java.lang.Math.toRadians(8)
                        * java.lang.Math.max(0, java.lang.Math.min(1, degrees / 28)), "inward turn clamps between zero and eight degrees");
            }
        }
        for (float degrees : new float[]{0, 12, 24, 28, 32}) {
            var left = hoofPoint(model, "L", (float) java.lang.Math.toRadians(degrees), false);
            var right = hoofPoint(model, "R", (float) java.lang.Math.toRadians(degrees), false);
            var leftWithout = hoofPoint(model, "L", (float) java.lang.Math.toRadians(degrees), true);
            var rightWithout = hoofPoint(model, "R", (float) java.lang.Math.toRadians(degrees), true);
            check(degrees == 0 || (left.x - leftWithout.x) * (right.x - rightWithout.x) < 0,
                    "real left/right inward displacements have mirrored direction");
            near(left.y, leftWithout.y, "inward yaw preserves left authored lift height");
            near(right.y, rightWithout.y, "inward yaw preserves right authored lift height");
        }
        check(PonyHeldItemPose.carryingPose("LHindLeg") == null, "hind legs untouched");
        check(PonyHeldItemPose.carryingPose("Head") == null, "head not assigned a leg pose");
        var bone = new GeoBone(null, "LFrontCalf", false, 0d, false, false);
        bone.updateRotation(.3f, .2f, .1f); bone.updatePosition(1, 2, 3);
        var poseCtor = PonyHeldItemPose.class.getDeclaredConstructor(GeoBone.class); poseCtor.setAccessible(true);
        try (var pose = poseCtor.newInstance(bone)) { bone.updateRotation(5, 6, 7); bone.updatePosition(8, 9, 10); }
        near(bone.getRotX(), .3f, "pose restores animation rotation"); near(bone.getPosY(), 2, "pose restores joint offset");
        check(bone.hasRotationChanged() && bone.hasPositionChanged(), "animation dirty flags preserved");
        String renderer = Files.readString(repo.resolve("src/client/java/top/csituka/magicaland/client/render/PonyRenderer.java"));
        check(renderer.contains("PonyHeldItemPose.apply") && renderer.contains("PonyHeldItems.renderAtBone"), "render integration");
        check(renderer.contains("heldWeights.update(heldItems.player().age + (double) partialTick, heldItems)"), "renderer supplies use state");
    }
    private static void testConsumption() {
        for (Item item : List.of(Items.APPLE, Items.GOLDEN_CARROT, Items.POTION, Items.MILK_BUCKET, Items.HONEY_BOTTLE)) {
            var stack = new ItemStack(item);
            var use = PonyHeldItems.consumption(true, Hand.MAIN_HAND, stack, stack.getMaxUseTime() - 3, .5f);
            near(use.ticks(), 3.5, "vanilla use time with fractional render tick");
            check(use.pitch() > 0, "food and drinks animate " + item);
            check(PonyHeldItems.consumption(false, Hand.MAIN_HAND, stack, 20, 0) == PonyHeldItems.Consumption.NONE, "holding alone does not eat");
            check(PonyHeldItems.consumption(true, Hand.MAIN_HAND, stack, 0, 0) == PonyHeldItems.Consumption.NONE, "finished use stops");
        }
        for (Item item : List.of(Items.SHIELD, Items.BOW, Items.CROSSBOW, Items.TRIDENT, Items.SPYGLASS, Items.STONE))
            check(PonyHeldItems.consumption(true, Hand.MAIN_HAND, new ItemStack(item), 10, 0) == PonyHeldItems.Consumption.NONE,
                    "other actions do not trigger consumption " + item);
        check(PonyHeldItems.consumption(true, Hand.MAIN_HAND, ItemStack.EMPTY, 10, 0) == PonyHeldItems.Consumption.NONE, "empty active stack");
        var apple = new ItemStack(Items.APPLE);
        check(Float.isFinite(PonyHeldItems.consumption(true, Hand.MAIN_HAND, apple, 20, Float.NaN).pitch()), "invalid partial tick sanitized");
        for (boolean mainLeft : new boolean[]{true, false}) for (Hand hand : Hand.values()) {
            var use = new PonyHeldItems.Consumption(hand, UseAction.EAT, 0);
            var eating = consumptionFrame(mainLeft, use);
            boolean activeLeft = hand == Hand.MAIN_HAND ? mainLeft : !mainLeft;
            near(eating.consumingPitch(activeLeft), java.lang.Math.toRadians(24), "only active arm eats, including swapped main arm");
            near(eating.consumingPitch(!activeLeft), 0, "inactive arm remains still");
            var idle = consumptionFrame(mainLeft, PonyHeldItems.Consumption.NONE);
            var blend = new PonyHeldItemPose.Weights();
            blend.update(0, idle); blend.update(1, eating);
            near(blend.usePitch(activeLeft), java.lang.Math.toRadians(10), "entry limited to ten degrees per tick");
            blend.update(1, eating); near(blend.usePitch(activeLeft), java.lang.Math.toRadians(10), "duplicate render cannot advance gesture");
            blend.update(3, eating); near(blend.usePitch(activeLeft), use.pitch(), "arm reaches eating pose");
            blend.update(4, idle); near(blend.usePitch(activeLeft), java.lang.Math.toRadians(14), "interrupted use eases out");
            blend.update(6, idle); near(blend.usePitch(activeLeft), 0, "returns to carrying pose");
            blend.update(7, eating); blend.reset(); near(blend.usePitch(activeLeft), 0, "renderer reset clears gesture");
            var fine = new PonyHeldItemPose.Weights(); var coarse = new PonyHeldItemPose.Weights();
            fine.update(0, idle); coarse.update(0, idle);
            for (int i = 1; i <= 30; i++) fine.update(i / 10d, eating);
            coarse.update(3, eating); near(fine.usePitch(activeLeft), coarse.usePitch(activeLeft), "entry is independent of render frequency");
        }
        for (UseAction action : List.of(UseAction.EAT, UseAction.DRINK)) {
            float min = Float.POSITIVE_INFINITY, max = 0;
            for (int tick = 0; tick <= 320; tick++) {
                float pitch = new PonyHeldItems.Consumption(Hand.MAIN_HAND, action, tick / 10f).pitch();
                min = java.lang.Math.min(min, pitch); max = java.lang.Math.max(max, pitch);
                check(pitch >= java.lang.Math.toRadians(20) && pitch <= java.lang.Math.toRadians(31), "bounded consumption gesture");
            }
            check(max - min > java.lang.Math.toRadians(3), "gentle repeating motion for " + action);
        }
    }
    private static void testFrameCues() throws Exception {
        for (boolean mainLeft : new boolean[]{false, true}) {
            var weights = new PonyHeldItemPose.Weights();
            weights.update(0, carryFrame(mainLeft, ItemStack.EMPTY, ItemStack.EMPTY));
            var apple = new ItemStack(Items.APPLE, 3);
            var frame = carryFrame(mainLeft, apple, ItemStack.EMPTY);
            weights.update(1, frame); weights.update(4, frame);
            near(armCue(cue(weights), mainLeft), java.lang.Math.toRadians(3), "drawing main item cues its physical arm");
            near(armCue(cue(weights), !mainLeft), 0, "drawing main item does not cue offhand");
            near(cue(weights).headPitch(), 0, "hoof item does not cue the mouth");
            apple.decrement(1); weights.update(5, frame);
            near(armCue(cue(weights), mainLeft), java.lang.Math.toRadians(3) * .75, "count changes do not restart item-identity cue");
            weights.update(7, carryFrame(mainLeft, apple.copy(), ItemStack.EMPTY));
            near(armCue(cue(weights), mainLeft), 0, "new stack object of the same Item does not restart cue");
            var offhand = carryFrame(mainLeft, apple, new ItemStack(Items.POTION));
            weights.update(8, offhand); weights.update(11, offhand);
            near(armCue(cue(weights), !mainLeft), java.lang.Math.toRadians(3), "offhand drawing cues only the matching physical arm");
            near(armCue(cue(weights), mainLeft), 0, "offhand cue never restarts main arm");
            var changed = carryFrame(mainLeft, new ItemStack(Items.CARROT), offhand.off());
            weights.update(12, changed); weights.update(15, changed);
            near(armCue(cue(weights), mainLeft), java.lang.Math.toRadians(3), "switching item type cues the occupied arm");
            var empty = carryFrame(mainLeft, ItemStack.EMPTY, ItemStack.EMPTY);
            weights.update(16, empty); weights.update(19, empty);
            near(weights.left, 0, "put-away cue does not delay left carrying release");
            near(weights.right, 0, "put-away cue does not delay right carrying release");
            near(cue(weights).leftPitch(), java.lang.Math.toRadians(3), "left put-away gets a short cue");
            near(cue(weights).rightPitch(), java.lang.Math.toRadians(3), "right put-away gets a short cue");
            weights.reset(); near(cue(weights).headPitch(), 0, "reset clears head cue");
            weights.update(20, changed); near(armCue(cue(weights), mainLeft), 0, "reset snapshots existing items without inventing a draw");

            for (Hand hand : Hand.values()) {
                var mouth = hand == Hand.MAIN_HAND ? carryFrame(mainLeft, new ItemStack(Items.IRON_SWORD), ItemStack.EMPTY)
                        : carryFrame(mainLeft, ItemStack.EMPTY, new ItemStack(Items.IRON_SWORD));
                var mouthWeights = new PonyHeldItemPose.Weights(); mouthWeights.update(0, empty);
                mouthWeights.update(1, mouth); mouthWeights.update(4, mouth);
                near(cue(mouthWeights).headPitch(), java.lang.Math.toRadians(2.5), "main or offhand mouth tool cues head");
                near(cue(mouthWeights).leftPitch(), 0, "mouth draw does not raise left arm");
                near(cue(mouthWeights).rightPitch(), 0, "mouth draw does not raise right arm");
                var pickaxe = hand == Hand.MAIN_HAND ? carryFrame(mainLeft, new ItemStack(Items.IRON_PICKAXE), ItemStack.EMPTY)
                        : carryFrame(mainLeft, ItemStack.EMPTY, new ItemStack(Items.IRON_PICKAXE));
                mouthWeights.update(5, pickaxe); mouthWeights.update(8, pickaxe);
                near(cue(mouthWeights).headPitch(), 0, "mouth tool switch cancels nod on either hand");
                var hoof = hand == Hand.MAIN_HAND ? carryFrame(mainLeft, new ItemStack(Items.APPLE), ItemStack.EMPTY)
                        : carryFrame(mainLeft, ItemStack.EMPTY, new ItemStack(Items.APPLE));
                mouthWeights.update(9, hoof); mouthWeights.update(12, hoof);
                near(cue(mouthWeights).headPitch(), 0, "mouth to hoof switch does not nod");
                mouthWeights.update(13, mouth); mouthWeights.update(16, mouth);
                near(cue(mouthWeights).headPitch(), 0, "hoof to mouth switch does not nod");
                mouthWeights.update(17, empty); mouthWeights.update(20, empty);
                near(cue(mouthWeights).headPitch(), java.lang.Math.toRadians(2.5), "mouth to empty keeps stow nod");
                mouthWeights.update(21, hoof);
                near(cue(mouthWeights).headPitch(), 0, "new item interrupts leftover stow nod");
            }
        }
    }
    private static void testScopedApply() throws Exception {
        // The pose only needs a nonnull player token; this fixture has no world or active client.
        var unsafeType = Class.forName("sun.misc.Unsafe");
        var field = unsafeType.getDeclaredField("theUnsafe"); field.setAccessible(true);
        var player = (net.minecraft.client.network.AbstractClientPlayerEntity) unsafeType.getMethod("allocateInstance", Class.class)
                .invoke(field.get(null), net.minecraft.client.network.OtherClientPlayerEntity.class);
        for (boolean mainLeft : new boolean[]{false, true}) for (Hand active : Hand.values()) {
            var empty = carryFrame(mainLeft, ItemStack.EMPTY, ItemStack.EMPTY);
            var frame = consumptionFrame(mainLeft, new PonyHeldItems.Consumption(active, UseAction.EAT, 0));
            frame = withPlayer(frame, player);
            var weights = new PonyHeldItemPose.Weights(); weights.update(0, empty); weights.update(1, frame); weights.update(4, frame);
            for (String side : List.of("L", "R")) for (String part : List.of("ForeLeg", "FrontCalf", "FrontHoof")) {
                var bone = testBone(side + part);
                var source = PonyHeldItemPose.carryingPose(bone.getName(), weights.usePitch(side.equals("L")));
                float cue = part.equals("ForeLeg") ? armCue(cue(weights), side.equals("L")) : 0;
                try (var pose = PonyHeldItemPose.apply(bone, frame, weights, false)) {
                    check(pose != null, "raised arm applies actual scoped pose");
                    near(bone.getRotX(), source.rx() - cue, "cue layers only onto shoulder, not child bend");
                    near(bone.getRotY(), source.ry(), "cue preserves consumption inward yaw");
                    near(bone.getRotZ(), source.rz(), "cue preserves child roll");
                    near(bone.getPosX(), source.x(), "cue preserves authored child X compensation");
                    near(bone.getPosY(), source.y(), "cue preserves authored child Y compensation");
                    near(bone.getPosZ(), source.z(), "cue preserves authored child Z compensation");
                }
                restored(bone);
                check(PonyHeldItemPose.apply(bone, frame, weights, true) == null, "rerender does not apply a second carrying cue");
                restored(bone);
            }
            var mouth = new PonyHeldItems.Frame(player, new ItemStack(Items.IRON_SWORD), ItemStack.EMPTY,
                    PonyHeldItems.Grip.MOUTH, PonyHeldItems.Grip.NONE, mainLeft, .25f, Hand.MAIN_HAND, PonyHeldItems.Consumption.NONE);
            weights.reset(); weights.update(0, empty); weights.update(1, mouth); weights.update(4, mouth);
            var head = testBone("Head");
            try (var pose = PonyHeldItemPose.apply(head, mouth, weights, false)) {
                check(pose != null, "mouth head cue applies");
                near(head.getRotX(), .3 + java.lang.Math.toRadians(2.5) + java.lang.Math.sin(java.lang.Math.PI / 4) * java.lang.Math.toRadians(3), "head cue adds to swing and animated pitch");
                near(head.getRotY(), .2 + (mainLeft ? -1 : 1) * java.lang.Math.toRadians(6), "head cue preserves mirrored mouth swing yaw");
                near(head.getPosY(), 2, "head cue never shifts head position");
            }
            restored(head);
            var held = withPlayer(carryFrame(mainLeft, new ItemStack(Items.APPLE), ItemStack.EMPTY), player);
            var away = withPlayer(empty, player);
            weights.reset(); weights.update(0, held); weights.update(1, away); weights.update(4, away);
            var shoulder = testBone(mainLeft ? "LForeLeg" : "RForeLeg");
            try (var pose = PonyHeldItemPose.apply(shoulder, away, weights, false)) {
                check(pose != null, "put-away cue can apply after carry pose releases");
                near(shoulder.getRotX(), .3 - java.lang.Math.toRadians(3), "put-away nudges existing animation instead of holding old carrying pose");
                near(shoulder.getPosX(), 1, "put-away cue leaves walking offset untouched");
            }
            restored(shoulder);
            check(PonyHeldItemPose.apply(testBone(mainLeft ? "LFrontCalf" : "RFrontCalf"), away, weights, false) == null,
                    "released child joint is never independently nudged");
        }
    }
    private static GeoBone testBone(String name) {
        var bone = new GeoBone(null, name, false, 0d, false, false);
        bone.updateRotation(.3f, .2f, .1f); bone.updatePosition(1, 2, 3); bone.updateScale(.8f, 1.2f, .9f);
        bone.resetStateChanges(); bone.markRotationAsChanged(); bone.markScaleAsChanged();
        return bone;
    }
    private static void restored(GeoBone bone) {
        near(bone.getRotX(), .3f, "scope restores animated pitch"); near(bone.getRotY(), .2f, "scope restores animated yaw");
        near(bone.getRotZ(), .1f, "scope restores animated roll"); near(bone.getPosX(), 1, "scope restores position X");
        near(bone.getPosY(), 2, "scope restores position Y"); near(bone.getPosZ(), 3, "scope restores position Z");
        near(bone.getScaleX(), .8f, "scale remains untouched"); near(bone.getScaleY(), 1.2f, "scale Y remains untouched");
        near(bone.getScaleZ(), .9f, "scale Z remains untouched");
        check(bone.hasRotationChanged() && !bone.hasPositionChanged() && bone.hasScaleChanged(), "scope restores exact animation dirty flags");
    }
    private static PonyHeldItems.Frame withPlayer(PonyHeldItems.Frame frame, net.minecraft.client.network.AbstractClientPlayerEntity player) {
        return new PonyHeldItems.Frame(player, frame.main(), frame.off(), frame.mainGrip(), frame.offGrip(), frame.mainLeft(), frame.swing(), frame.swingingHand(), frame.consumption());
    }
    private static PonyHeldItems.Frame carryFrame(boolean mainLeft, ItemStack main, ItemStack off) {
        var mainGrip = PonyHeldItems.grip(main, true);
        return new PonyHeldItems.Frame(null, main, off, mainGrip, PonyHeldItems.grip(off, mainGrip != PonyHeldItems.Grip.MOUTH),
                mainLeft, 0, Hand.MAIN_HAND, PonyHeldItems.Consumption.NONE);
    }
    private static PonyCarryCue.Pose cue(PonyHeldItemPose.Weights weights) throws Exception {
        var field = PonyHeldItemPose.Weights.class.getDeclaredField("cue"); field.setAccessible(true);
        return (PonyCarryCue.Pose) field.get(weights);
    }
    private static float armCue(PonyCarryCue.Pose cue, boolean left) { return left ? cue.leftPitch() : cue.rightPitch(); }
    private static PonyHeldItems.Frame consumptionFrame(boolean mainLeft, PonyHeldItems.Consumption use) {
        return new PonyHeldItems.Frame(null, new ItemStack(Items.APPLE), new ItemStack(Items.POTION),
                PonyHeldItems.Grip.HOOF, PonyHeldItems.Grip.HOOF, mainLeft, 0, Hand.MAIN_HAND, use);
    }
    private static Vector3f hoofPoint(BakedGeoModel model, String side, float pitch, boolean omitInward) {
        var matrices = new MatrixStack();
        for (String part : List.of("ForeLeg", "FrontCalf", "FrontHoof")) {
            var pose = PonyHeldItemPose.carryingPose(side + part, pitch);
            var bone = model.getBone(side + part).orElseThrow();
            float yaw = omitInward && part.equals("ForeLeg") ? PonyHeldItemPose.carryingPose(side + part).ry() : pose.ry();
            bone.updateRotation(pose.rx(), yaw, pose.rz());
            bone.updatePosition(pose.x(), pose.y(), pose.z());
            RenderUtils.prepMatrixForBone(matrices, bone);
        }
        float x = model.getBone(side + "FrontHoof").orElseThrow().getPivotX();
        return matrices.peek().getPositionMatrix().transformPosition(new Vector3f(x / 16, -3.6f / 16, -3.5f / 16)).mul(16);
    }
    private static void near(double a, double b, String text) { check(Double.isFinite(a) && java.lang.Math.abs(a-b) < 1e-5, text + ": " + a + " != " + b); }
    private static void check(boolean ok, String text) { checks++; if (!ok) throw new AssertionError(text); }
}
