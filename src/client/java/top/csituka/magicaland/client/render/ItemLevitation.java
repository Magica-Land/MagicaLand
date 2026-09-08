package top.csituka.magicaland.client.render;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class ItemLevitation {
    private static final LevitationMotion.Store STATES = new LevitationMotion.Store();
    private static final Map<LevitationMotion.Key, ItemStamp> ITEMS = new HashMap<>();
    private static boolean initialized;
    private static Object trackedWorld;
    private static boolean trackedFirstPerson;

    private ItemLevitation() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        WorldRenderEvents.START.register(context -> {
            boolean first = MinecraftClient.getInstance().options.getPerspective().isFirstPerson();
            if (trackedWorld != context.world() || trackedFirstPerson != first) clear();
            trackedWorld = context.world();
            trackedFirstPerson = first;
            STATES.beginFrame(context.world(), first);
            ITEMS.values().removeIf(item -> STATES.frame() - item.lastSeen > 120);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }

    public static void clear() {
        STATES.clear();
        ITEMS.clear();
        trackedWorld = null;
    }

    public static void forget(LivingEntity entity, boolean main, boolean first) {
        LevitationMotion.Key key = new LevitationMotion.Key(entity.getUuid(), main, first);
        STATES.remove(key);
        ITEMS.remove(key);
    }

    public static LevitationTrail applyWorld(LivingEntity entity, ItemStack stack, boolean main, boolean left,
            MatrixStack matrices, Matrix4f entityFrame, Matrix4f anchorFrame, float delta) {
        if (!HornAuraPass.isWorld() || inactive(entity, stack)) {
            forget(entity, main, false);
            return LevitationTrail.EMPTY;
        }
        Vec3d player = entity.getLerpedPos(delta);
        Matrix4f inverseFrame = new Matrix4f(entityFrame).invert();
        Vector3f anchor = inverseFrame.transformPosition(anchorFrame.transformPosition(new Vector3f(), new Vector3f()));
        LevitationMotion.Point target = point(player).add(point(anchor));
        Matrix3f worldToRender = new Matrix3f(entityFrame);
        float yaw = MathHelper.lerpAngleDegrees(delta, entity.prevBodyYaw, entity.bodyYaw);
        return apply(entity, stack, main, left, false, matrices, target, point(player), worldToRender,
                new Matrix4f(entityFrame), point(player), yaw, delta);
    }

    public static LevitationTrail applyFirstPerson(LivingEntity entity, ItemStack stack, boolean main, boolean left,
            MatrixStack matrices, float delta) {
        if (inactive(entity, stack)) {
            forget(entity, main, true);
            return LevitationTrail.EMPTY;
        }
        var camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d cameraPosition = camera.getPos();
        Quaternionf rotation = new Quaternionf(camera.getRotation());
        Matrix3f worldToRender = new Matrix3f().rotation(new Quaternionf(rotation).conjugate());
        Vector3f anchor = new Vector3f(left ? -.71f : .71f, -.32f, -1.12f).rotate(rotation);
        LevitationMotion.Point target = point(cameraPosition).add(point(anchor));
        return apply(entity, stack, main, left, true, matrices, target, point(entity.getLerpedPos(delta)),
                worldToRender, new Matrix4f().set(worldToRender), point(cameraPosition),
                entity.getYaw(delta), delta);
    }

    private static LevitationTrail apply(LivingEntity entity, ItemStack stack, boolean main, boolean left,
            boolean first, MatrixStack matrices, LevitationMotion.Point target, LevitationMotion.Point player,
            Matrix3f worldToRender, Matrix4f worldFrame, LevitationMotion.Point origin, float yaw, float delta) {
        double time = (entity.age + (double) delta) / 20;
        LevitationMotion.Key key = new LevitationMotion.Key(entity.getUuid(), main, first);
        LevitationMotion.Profile profile = first ? LevitationMotion.FIRST_PERSON : LevitationMotion.WORLD;
        LevitationMotion.Entry entry = STATES.acquire(key, identity(key, stack, left, time), profile);
        boolean using = entity.isUsingItem();
        STATES.sample(entry, target, player, yaw, time, using);
        if (entry.pose == null || !target.finite()) return LevitationTrail.EMPTY;
        Matrix3f renderToLocal = new Matrix3f(matrices.peek().getPositionMatrix()).invert();
        Vector3f displacement = worldToRender.transform(vector(entry.pose.offset()), new Vector3f());
        renderToLocal.transform(displacement);
        Vector3f yawAxis = worldToRender.transform(new Vector3f(0, 1, 0), new Vector3f());
        renderToLocal.transform(yawAxis);
        if (!displacement.isFinite() || !yawAxis.isFinite() || yawAxis.lengthSquared() < 1e-10f)
            return LevitationTrail.EMPTY;
        matrices.translate(displacement.x, displacement.y, displacement.z);
        matrices.multiply(new Quaternionf().rotationAxis((float) Math.toRadians(-entry.pose.yaw()), yawAxis.normalize()));
        boolean sprinting = entity.isSprinting();
        Matrix4f inverseWorld = new Matrix4f(worldFrame).invert();
        // ItemRenderer 的显示变换也会移动几何中心，等 capture 完成再记录尾迹。
        return LevitationTrail.deferred(center -> {
            Vector3f actualRelative = inverseWorld.transformPosition(new Vector3f(center));
            LevitationMotion.Point actual = origin.add(point(actualRelative));
            var trail = STATES.recordTrail(entry, actual, time, sprinting, using);
            return LevitationTrail.create(trail, worldFrame, origin, (float) profile.trailWidth());
        });
    }

    private static boolean inactive(LivingEntity entity, ItemStack stack) {
        return stack.isEmpty() || !entity.isAlive() || entity.isInvisible() || entity.isSpectator()
                || entity.isSleeping() || entity.hasVehicle() || MinecraftClient.getInstance().world != entity.getWorld();
    }

    private static String identity(LevitationMotion.Key key, ItemStack stack, boolean left, double time) {
        ItemStamp stamp = ITEMS.computeIfAbsent(key, ignored -> new ItemStamp());
        stamp.lastSeen = STATES.frame();
        // 换槽立即比较；同一 stack 的 NBT 仅低频检查，平常不复制/序列化。
        if (stamp.stack != stack || stamp.left != left || time < stamp.checked || time - stamp.checked >= .25) {
            if (stamp.item != stack.getItem() || stamp.left != left || !Objects.equals(stamp.nbt, stack.getNbt())) {
                stamp.item = stack.getItem();
                stamp.left = left;
                stamp.nbt = stack.getNbt() == null ? null : stack.getNbt().copy();
                stamp.signature = Long.toString(++stamp.generation);
            }
            stamp.stack = stack;
            stamp.checked = time;
        }
        if (ITEMS.size() > 512) { ITEMS.clear(); STATES.clear(); }
        return stamp.signature;
    }

    private static final class ItemStamp {
        ItemStack stack;
        Item item;
        NbtCompound nbt;
        boolean left;
        double checked;
        long generation, lastSeen;
        String signature = "0";
    }

    private static LevitationMotion.Point point(Vec3d v) { return new LevitationMotion.Point(v.x, v.y, v.z); }
    private static LevitationMotion.Point point(Vector3f v) { return new LevitationMotion.Point(v.x, v.y, v.z); }
    private static Vector3f vector(LevitationMotion.Point p) { return new Vector3f((float) p.x(), (float) p.y(), (float) p.z()); }
}
