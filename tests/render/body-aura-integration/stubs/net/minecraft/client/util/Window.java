package net.minecraft.client.util;
import net.minecraft.client.MinecraftClient;
public final class Window {
    public int getFramebufferWidth() { return MinecraftClient.getInstance().framebuffer.textureWidth; }
    public int getFramebufferHeight() { return MinecraftClient.getInstance().framebuffer.textureHeight; }
}
