package net.minecraft.client;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.texture.TextureManager;
public final class MinecraftClient {
    public static final boolean IS_SYSTEM_MAC = false;
    private static final MinecraftClient INSTANCE = new MinecraftClient();
    public Framebuffer framebuffer;
    public final WorldRenderer worldRenderer = new WorldRenderer();
    public final GameRenderer gameRenderer = null;
    public final TextureManager textures = new TextureManager();
    public boolean fabulous;
    public static MinecraftClient getInstance() { return INSTANCE; }
    public static boolean isFabulousGraphicsOrBetter() { return INSTANCE.fabulous; }
    public Framebuffer getFramebuffer() { return framebuffer; }
    public TextureManager getTextureManager() { return textures; }
}
