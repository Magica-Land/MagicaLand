package top.csituka.magicaland.api.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import top.csituka.magicaland.client.api.AppearanceVisualBridge;

/** Render-thread visual entry points; caller supplies the current matrices and buffers. */
public final class AppearanceVisuals {
    private AppearanceVisuals() {}

    public static void renderOrb(MatrixStack matrices, int magicColor, double ticks, int seed) {
        AppearanceVisualBridge.renderOrb(matrices, magicColor, ticks, seed);
    }

    public static void renderGlowingItem(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
            VertexConsumerProvider buffers, World world, int light, int seed, int magicColor) {
        AppearanceVisualBridge.renderGlowingItem(stack, mode, matrices, buffers, world, light, seed, magicColor);
    }

    /** Runs one non-nested temporary hand pass and restores its scope even when rendering throws. */
    public static void renderFirstPerson(LivingEntity owner, Entity camera, ItemStack stack,
            VertexConsumerProvider.Immediate buffers, Runnable render) {
        AppearanceVisualBridge.renderFirstPerson(owner, camera, stack, buffers, render);
    }

    /** Runs a hand pass with independent pose, equip and action state. */
    public static void renderFirstPerson(LivingEntity owner, ItemVisualContext context,
            VertexConsumerProvider.Immediate buffers, Runnable render) {
        AppearanceVisualBridge.renderFirstPerson(owner, context, buffers, render);
    }

    /** Adds visual inertia, the existing item glow and trail; restores the caller's matrices. */
    public static void renderLevitatingItem(LivingEntity owner, ItemVisualContext context,
            ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider buffers,
            World world, int light, int seed, int magicColor, float tickDelta) {
        AppearanceVisualBridge.renderLevitatingItem(owner, context, mode, matrices, buffers,
                world, light, seed, magicColor, tickDelta);
    }
}
