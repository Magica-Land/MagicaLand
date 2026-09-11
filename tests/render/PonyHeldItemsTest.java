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
        weights.update(2, true, false); near(weights.left, .25, "one tick blend");
        weights.update(2, true, true); near(weights.left, .25, "render count cannot advance blend");
        weights.update(5, true, false); near(weights.left, 1, "four ticks into carrying");
        weights.update(9, false, false); near(weights.left, 0, "four ticks out of carrying");
        testConsumption();
        Path repo = Path.of(args[0]);
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
            check(side.equals("L") ? upper.ry() > 0 : upper.ry() < 0, "left/right transition turns outward");
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
            near(eatingUpper.ry(), upper.ry(), "consumption preserves outward shoulder orientation");
            near(eatingUpper.x(), upper.x(), "consumption preserves shoulder compensation");
            for (String part : List.of("FrontCalf", "FrontHoof"))
                check(PonyHeldItemPose.carryingPose(side + part, eatingPitch).equals(PonyHeldItemPose.carryingPose(side + part)),
                        "consumption never changes child joint angles or positions");
            check(hoofHeight(model, side, eatingPitch) > hoofHeight(model, side, 0) + 1,
                    "whole-arm rotation lifts the carried item upward, not down");
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
        System.out.println("PASS PonyHeldItemsTest: " + checks + " item rules, original animation, sole orientation and state restoration checks");
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
    private static PonyHeldItems.Frame consumptionFrame(boolean mainLeft, PonyHeldItems.Consumption use) {
        return new PonyHeldItems.Frame(null, new ItemStack(Items.APPLE), new ItemStack(Items.POTION),
                PonyHeldItems.Grip.HOOF, PonyHeldItems.Grip.HOOF, mainLeft, 0, Hand.MAIN_HAND, use);
    }
    private static float hoofHeight(BakedGeoModel model, String side, float pitch) {
        var matrices = new MatrixStack();
        for (String part : List.of("ForeLeg", "FrontCalf", "FrontHoof")) {
            var pose = PonyHeldItemPose.carryingPose(side + part, pitch);
            var bone = model.getBone(side + part).orElseThrow();
            bone.updateRotation(pose.rx(), pose.ry(), pose.rz());
            bone.updatePosition(pose.x(), pose.y(), pose.z());
            RenderUtils.prepMatrixForBone(matrices, bone);
        }
        float x = model.getBone(side + "FrontHoof").orElseThrow().getPivotX();
        return matrices.peek().getPositionMatrix().transformPosition(new Vector3f(x / 16, -3.6f / 16, -3.5f / 16)).y * 16;
    }
    private static void near(double a, double b, String text) { check(java.lang.Math.abs(a-b) < 1e-5, text + ": " + a); }
    private static void check(boolean ok, String text) { checks++; if (!ok) throw new AssertionError(text); }
}
