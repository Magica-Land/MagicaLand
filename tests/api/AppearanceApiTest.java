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
        check(!ApiVersion.isCompatible(2, 0) && !ApiVersion.isCompatible(1, 1)
                && !ApiVersion.isCompatible(1, -1), "incompatible requests rejected");
        fails(IllegalStateException.class, () -> ApiVersion.requireCompatible(2, 0));
        registry();
        gazeAndVisibility();
        snapshots();
        firstPerson();
        System.out.println("Appearance API: " + checks + " checks passed");
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
}
