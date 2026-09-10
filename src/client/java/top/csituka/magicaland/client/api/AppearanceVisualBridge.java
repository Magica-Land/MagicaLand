package top.csituka.magicaland.client.api;

import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import top.csituka.magicaland.client.render.GlowingItem;
import top.csituka.magicaland.client.render.MagicOrb;

public final class AppearanceVisualBridge {
    private static boolean firstPersonPass;

    private AppearanceVisualBridge() {}

    public static void renderOrb(MatrixStack matrices, int color, double ticks, int seed) {
        MagicOrb.render(matrices, color, ticks, seed);
    }

    public static void renderGlowingItem(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
            VertexConsumerProvider buffers, World world, int light, int seed, int color) {
        GlowingItem.renderPreviewWithGlow(MinecraftClient.getInstance().getItemRenderer(), stack, mode,
                matrices, buffers, world, light, seed, color);
    }

    public static void renderFirstPerson(LivingEntity owner, Entity camera, ItemStack stack,
            VertexConsumerProvider.Immediate buffers, Runnable render) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(camera, "camera");
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(buffers, "buffers");
        Objects.requireNonNull(render, "render");
        if (firstPersonPass) throw new IllegalStateException("Temporary first-person passes cannot be nested");
        firstPersonPass = true;
        try (var view = FirstPersonItemView.open(owner, camera, stack)) {
            GlowingItem.beginFirstPersonPass();
            try {
                render.run();
            } finally {
                GlowingItem.endFirstPersonPass(buffers);
            }
        } finally {
            firstPersonPass = false;
        }
    }
}
