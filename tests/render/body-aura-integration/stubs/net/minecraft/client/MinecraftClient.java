package net.minecraft.client;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.Window;
public final class MinecraftClient {
    public static final boolean IS_SYSTEM_MAC = false;
    private static final MinecraftClient INSTANCE = new MinecraftClient();
    public Framebuffer framebuffer;
    public final WorldRenderer worldRenderer = new WorldRenderer();
    public final GameRenderer gameRenderer = new GameRenderer();
    public final TextureManager textures = new TextureManager();
    public static MinecraftClient getInstance() { return INSTANCE; }
    public Framebuffer getFramebuffer() { return framebuffer; }
    public TextureManager getTextureManager() { return textures; }
    public Window getWindow() { return new Window(); }
}
