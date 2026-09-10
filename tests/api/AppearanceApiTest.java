package top.csituka.magicaland.client.api;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import top.csituka.magicaland.api.ApiVersion;
import top.csituka.magicaland.api.client.*;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.config.ModelManager;
import top.csituka.magicaland.client.network.ClientNetworkHandler;
import top.csituka.magicaland.client.render.GlowingItem;
import top.csituka.magicaland.client.render.MagicEquip;
import top.csituka.magicaland.client.render.MagicFlame;

public final class AppearanceApiTest {
    private static int checks;
    private static final UUID PLAYER = UUID.randomUUID();

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private static void fails(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable failure) { check(type.isInstance(failure), "expected " + type + ", got " + failure); return; }
        throw new AssertionError("Expected " + type);
    }

    public static void main(String[] args) {
        check(ApiVersion.isCompatible(1, 0), "v1.0 supported");
        check(ApiVersion.isCompatible(1, 1), "independent visuals require v1.1");
        check(ApiVersion.isCompatible(1, 2), "magic activity requires v1.2");
        check(ApiVersion.isCompatible(1, 3), "flame requires v1.3");
        check(!ApiVersion.isCompatible(2, 0) && !ApiVersion.isCompatible(1, ApiVersion.MINOR + 1)
                && !ApiVersion.isCompatible(1, -1), "incompatible requests rejected");
        fails(IllegalStateException.class, () -> ApiVersion.requireCompatible(2, 0));
        registry();
        gazeAndVisibility();
        magicActivity();
        hornActivity();
        snapshots();
        firstPerson();
        visualContext();
        flame();
        System.out.println("Appearance API: " + checks + " checks passed");
    }

    private static void flame() {
        var client = MinecraftClient.getInstance();
        var source = new Entity(UUID.randomUUID(), client.world);
        var matrices = new net.minecraft.client.util.math.MatrixStack();
        matrices.push();
        AppearanceVisuals.renderFlame(matrices, source, 0x123456, .25f);
        check(MagicFlame.calls == 1 && MagicFlame.source == source && MagicFlame.color == 0x123456
                && MagicFlame.delta == .25f && matrices.depth == 1, "flame arguments forwarded and caller stack preserved");
        MagicFlame.failRender = true;
        fails(IllegalStateException.class, () -> AppearanceVisuals.renderFlame(matrices, source, 0, .5f));
        MagicFlame.failRender = false;
        check(matrices.depth == 1, "flame failure restores caller stack");
        int calls = MagicFlame.calls;
        for (float delta : new float[] {Float.NaN, Float.POSITIVE_INFINITY, -.001f, 1.001f})
            fails(IllegalArgumentException.class, () -> AppearanceVisuals.renderFlame(matrices, source, 0, delta));
        fails(NullPointerException.class, () -> AppearanceVisuals.renderFlame(null, source, 0, 0));
        fails(NullPointerException.class, () -> AppearanceVisuals.renderFlame(matrices, null, 0, 0));
        AppearanceVisuals.renderFlame(matrices, new Entity(UUID.randomUUID(), new World()), 0, .5f);
        source.removed = true;
        AppearanceVisuals.renderFlame(matrices, source, 0, .5f);
        check(MagicFlame.calls == calls && matrices.depth == 1, "invalid and stale flame sources leave renderer untouched");
        source.removed = false;
        AppearanceVisuals.renderFlame(matrices, source, 0, 0);
        AppearanceVisuals.renderFlame(matrices, source, 0, 1);
        check(MagicFlame.calls == calls + 2, "interpolation endpoints accepted");
        matrices.pop();
    }

    private static void registry() {
        List<String> failures = new ArrayList<>();
        var registry = new OverrideRegistry<String>((owner, failure) -> failures.add(owner));
        check(registry.resolve(PLAYER, value -> true, "native").equals("native"), "empty registry fallback");
        var low = registry.register("one:normal", Integer.MIN_VALUE, id -> "low");
        var high = registry.register("two:remote", Integer.MAX_VALUE, id -> "high");
        var tie = registry.register("three:remote", Integer.MAX_VALUE, id -> "tie");
        check(high.ownerId().equals("two:remote") && high.priority() == Integer.MAX_VALUE, "registration identity");
        check(registry.resolve(PLAYER, value -> true, "native").equals("high"), "priority and stable tie");
        high.close(); high.close();
        check(!high.isRegistered() && registry.resolve(PLAYER, value -> true, "native").equals("tie"), "close restores next");
        registry.unregisterOwner("three:remote");
        check(!tie.isRegistered() && low.isRegistered(), "owner cleanup preserves other owners");
        var abstain = registry.register("one:abstain", 50, id -> null);
        check(registry.resolve(PLAYER, value -> true, "native").equals("low"), "null abstains");
        var rejected = registry.register("one:removed", 60, id -> "removed");
        check(registry.resolve(PLAYER, value -> !value.equals("removed"), "native").equals("low"), "invalid target falls through");
        var broken = registry.register("broken:provider", 70, id -> { throw new IllegalStateException("bad provider"); });
        check(registry.resolve(PLAYER, value -> !value.equals("removed"), "native").equals("low"), "provider failure falls through");
        check(!broken.isRegistered() && failures.equals(List.of("broken:provider")), "failing provider removed and reported once");
        Registration[] self = new Registration[1];
        self[0] = registry.register("self:closing", 80, id -> { self[0].close(); return "stale"; });
        check(registry.resolve(PLAYER, value -> !value.equals("removed"), "native").equals("low"), "self-closed result discarded");
        registry.clear();
        check(!low.isRegistered() && !abstain.isRegistered() && !rejected.isRegistered(), "clear invalidates handles");
        low.close();
        check(registry.resolve(PLAYER, value -> true, "native").equals("native"), "clear restores native fallback");
        fails(IllegalArgumentException.class, () -> registry.register("bad owner", 0, id -> "x"));
        fails(NullPointerException.class, () -> registry.register("test:null", 0, null));
    }

    private static void gazeAndVisibility() {
        var client = MinecraftClient.getInstance();
        client.world = new World();
        Entity valid = new Entity(PLAYER, client.world);
        Entity otherWorld = new Entity(PLAYER, new World());
        Entity removed = new Entity(PLAYER, client.world); removed.removed = true;
        AppearanceOverrideState.init();
        var gaze = AppearanceOverrides.registerGaze("test:base", 0, id -> valid);
        var absent = AppearanceOverrides.registerGaze("test:absent", 1, id -> null);
        var stale = AppearanceOverrides.registerGaze("test:stale", 2, id -> removed);
        var foreign = AppearanceOverrides.registerGaze("test:foreign", 3, id -> otherWorld);
        check(AppearanceOverrides.gazeTarget(PLAYER) == valid, "removed and other-world gaze ignored");
        var hidden = AppearanceOverrides.registerMainHandVisibility("test:base", 0, id -> AppearanceOverrides.Visibility.HIDDEN);
        var visible = AppearanceOverrides.registerMainHandVisibility("test:visible", 1, id -> AppearanceOverrides.Visibility.VISIBLE);
        check(AppearanceOverrides.mainHandVisibility(PLAYER) == AppearanceOverrides.Visibility.VISIBLE, "visible wins over lower hidden");
        visible.close();
        check(AppearanceOverrides.mainHandVisibility(PLAYER) == AppearanceOverrides.Visibility.HIDDEN, "closing visibility restores lower hidden");
        AppearanceOverrides.unregisterOwner("test:base");
        check(!gaze.isRegistered() && !hidden.isRegistered(), "owner removed from both channels");
        check(AppearanceOverrides.gazeTarget(PLAYER) == null
                && AppearanceOverrides.mainHandVisibility(PLAYER) == AppearanceOverrides.Visibility.DEFAULT, "native fallback restored");
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.fire();
        check(!absent.isRegistered() && !stale.isRegistered() && !foreign.isRegistered(), "disconnect invalidates all handles");
        var rejoined = AppearanceOverrides.registerGaze("test:base", 0, id -> valid);
        check(AppearanceOverrides.gazeTarget(PLAYER) == valid, "fresh registration works after disconnect");
        rejoined.close();
    }

    private static void snapshots() {
        var client = MinecraftClient.getInstance();
        client.player = new LivingEntity(PLAYER, client.world);
        check(Appearances.find(PLAYER).isEmpty() && Appearances.magicColor(PLAYER) == 0xAA00FF, "missing appearance has default color");
        ModelConfig model = new ModelConfig(); model.magicColor = 0x123456; model.showHorn = true;
        ModelManager.applied = model;
        var snapshot = Appearances.find(PLAYER).orElseThrow();
        check(snapshot.hasHorn() && !snapshot.hasWings() && snapshot.modelReplacementEnabled(), "snapshot reads applied appearance");
        model.magicColor = 0x654321; model.showHorn = false;
        check(snapshot.magicColor() == 0x123456 && snapshot.hasHorn(), "snapshot detached from mutable config");
        check(Appearances.magicColor(PLAYER) == 0x654321, "query obtains current applied color");
        UUID remote = UUID.randomUUID();
        ClientNetworkHandler.remoteModels.put(remote, model);
        check(Appearances.magicColor(remote) == 0x654321, "remote color delegated through API");
        top.csituka.magicaland.network.NetworkHandler.serverHasMod = false;
        check(!Appearances.find(remote).orElseThrow().modelReplacementEnabled(), "remote replacement respects sync availability");
        fails(NullPointerException.class, () -> Appearances.find(null));
    }

    private static void firstPerson() {
        var client = MinecraftClient.getInstance();
        LivingEntity owner = client.player;
        Entity camera = new Entity(PLAYER, client.world);
        ItemStack stack = new ItemStack();
        var buffers = new VertexConsumerProvider.Immediate();
        var previous = FirstPersonItemView.open(owner, owner, new ItemStack());
        AppearanceVisuals.renderFirstPerson(owner, camera, stack, buffers, () -> {
            var view = FirstPersonItemView.forOwner(owner);
            check(view != null && view.camera == camera && view.stack == stack, "temporary camera and stack visible during callback");
            GlowingItem.events.add("render");
        });
        check(GlowingItem.events.equals(List.of("begin", "render", "end")), "original pass order retained");
        check(FirstPersonItemView.forOwner(owner) == previous, "previous item scope restored");
        GlowingItem.events.clear();
        fails(IllegalArgumentException.class, () -> AppearanceVisuals.renderFirstPerson(owner, camera, stack, buffers,
                () -> { throw new IllegalArgumentException("render failed"); }));
        check(GlowingItem.events.equals(List.of("begin", "end")) && FirstPersonItemView.forOwner(owner) == previous,
                "callback failure still finishes pass and restores previous scope");
        GlowingItem.failEnd = true;
        fails(IllegalStateException.class, () -> AppearanceVisuals.renderFirstPerson(owner, camera, stack, buffers, () -> {}));
        check(FirstPersonItemView.forOwner(owner) == previous, "flush failure restores scope");
        GlowingItem.failEnd = false;
        AppearanceVisuals.renderFirstPerson(owner, camera, stack, buffers, () ->
                fails(IllegalStateException.class, () -> AppearanceVisuals.renderFirstPerson(owner, camera, stack, buffers, () -> {})));
        check(FirstPersonItemView.forOwner(owner) == previous, "reentry rejected without corrupting outer scope");
        var inner = FirstPersonItemView.open(owner, camera, stack);
        fails(IllegalStateException.class, previous::close);
        inner.close(); inner.close(); previous.close(); previous.close();
        check(FirstPersonItemView.forOwner(owner) == null, "idempotent LIFO close fully restores default");
    }

    private static void magicActivity() {
        check(!AppearanceOverrides.magicActive(PLAYER), "no addon magic by default");
        UUID other = UUID.randomUUID();
        var active = AppearanceOverrides.registerMagicActivity("test:active", -10, PLAYER::equals);
        var inactive = AppearanceOverrides.registerMagicActivity("test:inactive", 100, id -> false);
        check(AppearanceOverrides.magicActive(PLAYER), "high priority false cannot suppress lower active provider");
        check(!AppearanceOverrides.magicActive(other), "magic activity scoped to player UUID");
        var second = AppearanceOverrides.registerMagicActivity("test:second", 0, PLAYER::equals);
        active.close(); active.close();
        check(!active.isRegistered() && AppearanceOverrides.magicActive(PLAYER), "closing one provider preserves another");
        var broken = AppearanceOverrides.registerMagicActivity("test:broken", 200,
                id -> { throw new IllegalStateException("broken magic provider"); });
        check(AppearanceOverrides.magicActive(PLAYER) && !broken.isRegistered(), "failing magic provider removed with fallback");
        var hidden = AppearanceOverrides.registerMainHandVisibility("test:second", 0, id -> AppearanceOverrides.Visibility.HIDDEN);
        var gaze = AppearanceOverrides.registerGaze("test:second", 0, id -> null);
        AppearanceOverrides.unregisterOwner("test:second");
        check(!second.isRegistered() && !hidden.isRegistered() && !gaze.isRegistered(), "owner cleanup covers all three channels");
        check(inactive.isRegistered() && !AppearanceOverrides.magicActive(PLAYER), "owner cleanup preserves other providers");
        var disconnected = AppearanceOverrides.registerMagicActivity("test:join", 0, id -> true);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.fire();
        check(!inactive.isRegistered() && !disconnected.isRegistered() && !AppearanceOverrides.magicActive(PLAYER),
                "disconnect releases magic state and handles");
        try (var rejoined = AppearanceOverrides.registerMagicActivity("test:join", 0, id -> true)) {
            check(AppearanceOverrides.magicActive(PLAYER), "magic can register in next session");
        }
        fails(NullPointerException.class, () -> AppearanceOverrides.registerMagicActivity("test:null", 0, null));
        fails(IllegalArgumentException.class, () -> AppearanceOverrides.registerMagicActivity("bad owner", 0, id -> true));
        fails(NullPointerException.class, () -> AppearanceOverrides.magicActive(null));
    }

    private static void hornActivity() {
        var client = MinecraftClient.getInstance();
        client.world = new World();
        var player = new net.minecraft.client.network.AbstractClientPlayerEntity(PLAYER, client.world);
        client.player = player;
        var model = new ModelConfig(); model.showHorn = true;
        ModelManager.applied = model;
        MagicEquip.init();
        check(MagicEquip.hornProgress(player, 0) == 0, "empty normal hands leave horn off");
        var magic = AppearanceOverrides.registerMagicActivity("test:remote", 0, PLAYER::equals);
        player.age = 20;
        check(MagicEquip.hornProgress(player, 0) == 0, "addon starts existing smooth ignition");
        player.age = 28;
        check(MagicEquip.hornProgress(player, 0) == 1, "empty-handed remote powers horn");
        check(MagicEquip.progress(player, true, 0) == 0 && MagicEquip.progress(player, false, 0) == 0,
                "magic activity never equips body hands");
        check(MagicEquip.visualStack(player, true, 0).isEmpty()
                && MagicEquip.visualStack(player, false, 0).isEmpty(), "activity never invents body held items");
        magic.close();
        player.age = 40;
        check(MagicEquip.hornProgress(player, 0) == 1, "closing remote starts existing fade");
        player.age = 46;
        check(MagicEquip.hornProgress(player, 0) == 0, "horn extinguishes after empty-handed remote ends");
        player.main = new ItemStack();
        player.age = 60; MagicEquip.hornProgress(player, 0);
        player.age = 68;
        check(MagicEquip.hornProgress(player, 0) == 1, "normal main hand still ignites horn");
        try (var inactive = AppearanceOverrides.registerMagicActivity("test:inactive", 999, id -> false)) {
            check(MagicEquip.hornProgress(player, 0) == 1, "inactive addon cannot extinguish regular hand glow");
        }
        player.off = player.main; player.main = ItemStack.EMPTY; player.age = 70;
        check(MagicEquip.hornProgress(player, 0) == 1, "normal hand swap preserves settled horn");
        var remote = AppearanceOverrides.registerMagicActivity("test:remote", 0, PLAYER::equals);
        player.off = ItemStack.EMPTY; player.age = 72;
        check(MagicEquip.hornProgress(player, 0) == 1, "transferring item into remote inventory does not extinguish horn");
        model.showHorn = false;
        check(MagicEquip.hornProgress(player, 0) == 0, "addon cannot bypass horn eligibility");
        model.showHorn = true; player.invisible = true;
        check(MagicEquip.hornProgress(player, 0) == 0, "addon cannot reveal invisible player");
        player.invisible = false;
        check(MagicEquip.hornProgress(player, 0) == 1, "eligible remote body regains glow");
        UUID observerTarget = UUID.randomUUID();
        var remoteBody = new net.minecraft.client.network.AbstractClientPlayerEntity(observerTarget, client.world);
        ClientNetworkHandler.remoteModels.put(observerTarget, model);
        var observedMagic = AppearanceOverrides.registerMagicActivity("test:observed", 0, observerTarget::equals);
        check(MagicEquip.hornProgress(remoteBody, 0) == 1, "other clients' empty-handed remote body can glow");
        check(remoteBody.getMainHandStack().isEmpty() && remoteBody.getOffHandStack().isEmpty(),
                "observed glow never changes remote body equipment");
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.fire();
        check(!remote.isRegistered() && !observedMagic.isRegistered()
                && MagicEquip.hornProgress(player, 0) == 0 && MagicEquip.hornProgress(remoteBody, 0) == 0,
                "disconnect clears local and observed horn caches and activity");
        ClientNetworkHandler.remoteModels.remove(observerTarget);
        ModelManager.applied = null;
    }

    private static void visualContext() {
        var client = MinecraftClient.getInstance();
        LivingEntity owner = client.player;
        LivingEntity other = new LivingEntity(UUID.randomUUID(), client.world);
        Entity source = new Entity(UUID.randomUUID(), client.world);
        ItemStack stack = new ItemStack();
        var context = new ItemVisualContext(source, stack, .4f, .6f, false, true);
        stack.count = 0;
        check(!context.stack().isEmpty(), "context detaches input stack");
        context.stack().count = 0;
        check(!context.stack().isEmpty(), "context never exposes mutable stack");
        check(context.source() == source && context.equipProgress() == .4f && context.swingProgress() == .6f
                && !context.usingItem() && context.sprinting(), "caller visual state retained without reading owner");
        fails(NullPointerException.class, () -> new ItemVisualContext(null, stack, 1, 0, false, false));
        fails(IllegalArgumentException.class, () -> new ItemVisualContext(source, stack, Float.NaN, 0, false, false));
        fails(IllegalArgumentException.class, () -> new ItemVisualContext(source, stack, 1, 2, false, false));
        var buffers = new VertexConsumerProvider.Immediate();
        var previous = FirstPersonItemView.open(owner, owner, stack);
        AppearanceVisuals.renderFirstPerson(owner, context, buffers, () -> {
            var view = FirstPersonItemView.forOwner(owner);
            check(view.context == context && view.camera == source && !view.stack.isEmpty(), "context overload enters source scope");
            check(FirstPersonItemView.forOwner(other) == null, "scope cannot affect another appearance owner");
            fails(IllegalStateException.class, () -> AppearanceVisuals.renderFirstPerson(owner, source, stack, buffers, () -> {}));
            fails(IllegalStateException.class, () -> AppearanceVisuals.renderFirstPerson(owner, context, buffers, () -> {}));
            check(FirstPersonItemView.forOwner(owner) == view, "both nested overloads preserve outer context");
        });
        check(FirstPersonItemView.forOwner(owner) == previous, "context restores prior legacy scope");
        fails(NullPointerException.class, () -> AppearanceVisuals.renderFirstPerson(owner, (ItemVisualContext) null, buffers, () -> {}));
        fails(IllegalArgumentException.class, () -> AppearanceVisuals.renderFirstPerson(owner, context, buffers,
                () -> { throw new IllegalArgumentException("context render failed"); }));
        GlowingItem.failEnd = true;
        fails(IllegalStateException.class, () -> AppearanceVisuals.renderFirstPerson(owner, context, buffers, () -> {}));
        GlowingItem.failEnd = false;
        check(FirstPersonItemView.forOwner(owner) == previous, "new overload restores prior scope after render/flush failure");
        previous.close();
        var matrices = new net.minecraft.client.util.math.MatrixStack();
        var mode = net.minecraft.client.render.model.json.ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
        GlowingItem.events.clear();
        AppearanceVisuals.renderLevitatingItem(owner, context, mode, matrices, buffers, client.world, 0, 1, 0xAA00FF, .5f);
        check(GlowingItem.events.equals(List.of("world-inertia", "world-render")) && matrices.depth == 0,
                "world path applies inertia then existing glow and restores matrix");
        GlowingItem.failRender = true;
        fails(IllegalStateException.class, () -> AppearanceVisuals.renderLevitatingItem(owner, context, mode,
                matrices, buffers, client.world, 0, 1, 0xAA00FF, .5f));
        GlowingItem.failRender = false;
        check(matrices.depth == 0, "world render failure restores matrices");
        GlowingItem.events.clear();
        fails(IllegalArgumentException.class, () -> AppearanceVisuals.renderLevitatingItem(owner, context,
                net.minecraft.client.render.model.json.ModelTransformationMode.GROUND,
                matrices, buffers, client.world, 0, 1, 0xAA00FF, .5f));
        fails(IllegalArgumentException.class, () -> AppearanceVisuals.renderLevitatingItem(owner, context, mode,
                matrices, buffers, new World(), 0, 1, 0xAA00FF, .5f));
        check(GlowingItem.events.isEmpty() && matrices.depth == 0, "invalid world input rejected before state changes");
    }
}
