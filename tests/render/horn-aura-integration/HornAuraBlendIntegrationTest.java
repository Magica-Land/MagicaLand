import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlBlendState;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.resource.*;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import top.csituka.magicaland.client.render.MagicGlow;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipFile;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

/** 实际 RenderLayer.draw / ShaderProgram.bind / GlBlendState 缓存，不手工替代 draw 的混合步骤。 */
public final class HornAuraBlendIntegrationTest extends RenderPhase {
    private static int checks;
    private static net.minecraft.client.gl.ShaderProgram aura;
    private static final Identifier FIRST = new Identifier("test", "first"), SECOND = new Identifier("test", "second");
    private HornAuraBlendIntegrationTest() { super("test", () -> {}, () -> {}); }

    public static void main(String[] args) throws Exception {
        if (!glfwInit()) throw new AssertionError("GLFW");
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3); glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        long window = glfwCreateWindow(64, 64, "Actual horn blend regression", 0, 0);
        if (window == 0) throw new AssertionError("hidden window");
        glfwMakeContextCurrent(window); GL.createCapabilities();
        RenderSystem.initRenderThread(); RenderSystem.beginInitialization();
        try (ZipFile mc = new ZipFile(args[0])) {
            Path shaders = Path.of(args[1]);
            ResourcePack pack = (ResourcePack) Proxy.newProxyInstance(ResourcePack.class.getClassLoader(), new Class[] {ResourcePack.class},
                    (proxy, method, params) -> method.getName().equals("getName") ? "horn-blend-test" : null);
            ResourceFactory factory = id -> {
                try {
                String path = id.getPath();
                Path local = shaders.resolve(path.substring(path.lastIndexOf('/') + 1));
                byte[] data;
                if (Files.exists(local)) data = Files.readAllBytes(local);
                else {
                    var entry = mc.getEntry("assets/minecraft/" + path);
                    if (entry == null) return Optional.empty();
                    try (var in = mc.getInputStream(entry)) { data = in.readAllBytes(); }
                }
                if (path.endsWith(".json")) data = new String(data, StandardCharsets.UTF_8).replace("magicaland:", "").getBytes(StandardCharsets.UTF_8);
                byte[] bytes = data;
                return Optional.of(new Resource(pack, () -> new ByteArrayInputStream(bytes)));
                } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
            };
            aura = new net.minecraft.client.gl.ShaderProgram(factory, "horn_aura", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
            var field = MagicGlow.class.getDeclaredField("auraShader"); field.setAccessible(true); field.set(null, aura);
            var client = MinecraftClient.getInstance();
            client.framebuffer = new SimpleFramebuffer(64, 64, true, false);
            client.worldRenderer.entity = new SimpleFramebuffer(64, 64, true, false);
            int white1 = texture(), white2 = texture();
            client.textures.textures.put(FIRST, new AbstractTexture() { public void load(ResourceManager manager) {} public int getGlId() { return white1; } });
            client.textures.textures.put(SECOND, new AbstractTexture() { public void load(ResourceManager manager) {} public int getGlId() { return white2; } });
            RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorter.BY_DISTANCE);
            RenderSystem.getModelViewStack().loadIdentity(); RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderFogStart(10); RenderSystem.setShaderFogEnd(100);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            for (boolean fabulous : new boolean[] {false, true}) {
                client.fabulous = fabulous;
                var target = fabulous ? client.worldRenderer.entity : client.framebuffer;
                for (boolean production : new boolean[] {false, true}) {
                    RenderSystem.depthMask(true); RenderSystem.colorMask(true, true, true, true);
                    target.setClearColor(0, 0, 0, 0); target.clear(false); client.framebuffer.beginWrite(true);
                    // 原版 shader 缓存从 disabled 切到 aura；第二批必须命中同一缓存。
                    new GlBlendState().enable();
                    RenderLayer first = production ? layer(fabulous, FIRST) : control(fabulous, FIRST);
                    RenderLayer second = production ? layer(fabulous, SECOND) : control(fabulous, SECOND);
                    draw(first, -.8f, -.2f);
                    GlBlendState cached = GlBlendState.activeBlendState;
                    draw(second, .2f, .8f);
                    require(cached == GlBlendState.activeBlendState && !cached.isBlendDisabled(), "second texture uses same enabled shader blend cache");
                    target.beginWrite(false);
                    float[] left = pixel(16, 32), right = pixel(48, 32);
                    require(left[3] > .3f && right[3] > .1f, "both actual texture layers draw visible aura");
                    require(Math.abs(left[0] - right[0]) < .005f, "RGB additive factors are unchanged between texture layers");
                    if (production) require(Math.abs(left[3] - right[3]) < .005f, "production alpha is identical after cached shader rebind");
                    else require(right[3] < left[3] * .7f, "negative control reproduces old LIGHTNING alpha-squared regression");
                    require(glGetError() == GL_NO_ERROR, "actual RenderLayer pipeline has no GL errors");
                    System.out.println((production ? "PASS fixed" : "PASS negative control") + " fabulous=" + fabulous + " alpha=" + left[3] + "," + right[3]);
                }
            }
            client.framebuffer.delete(); client.worldRenderer.entity.delete();
            aura.close(); GlStateManager._deleteTexture(white1); GlStateManager._deleteTexture(white2);
            System.out.println("PASS actual RenderLayer / ShaderProgram cached texture batches: " + checks + " checks; " + glGetString(GL_RENDERER));
        } finally { glfwDestroyWindow(window); glfwTerminate(); }
    }

    private static RenderLayer layer(boolean fabulous, Identifier texture) throws Exception {
        var method = MagicGlow.class.getDeclaredMethod("aura", boolean.class, Identifier.class); method.setAccessible(true);
        return (RenderLayer) method.invoke(null, fabulous, texture);
    }
    private static RenderLayer control(boolean fabulous, Identifier texture) {
        return RenderLayer.of("old_lightning", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS, 256, false, true, RenderLayer.MultiPhaseParameters.builder()
                        .program(new ShaderProgram(() -> aura)).texture(new Texture(texture, false, false))
                        .writeMaskState(fabulous ? ALL_MASK : COLOR_MASK).depthTest(LEQUAL_DEPTH_TEST)
                        .target(fabulous ? ITEM_ENTITY_TARGET : MAIN_TARGET).transparency(LIGHTNING_TRANSPARENCY)
                        .lightmap(DISABLE_LIGHTMAP).cull(DISABLE_CULLING).build(false));
    }
    private static void draw(RenderLayer layer, float left, float right) {
        var buffers = VertexConsumerProvider.immediate(new BufferBuilder(256));
        VertexConsumer buffer = buffers.getBuffer(layer);
        float[][] corners = {{left, -.5f}, {right, -.5f}, {right, .5f}, {left, .5f}};
        for (float[] corner : corners) buffer.vertex(corner[0], corner[1], 0).color(1f, .2f, .6f, .5f)
                .texture(-1.5f, .5f).overlay(1200, 5).light(0, 0).normal(0, 0, 1).next();
        buffers.draw(layer);
    }
    private static int texture() {
        int texture = GlStateManager._genTexture(); RenderSystem.activeTexture(GL_TEXTURE0); RenderSystem.bindTexture(texture);
        ByteBuffer pixel = org.lwjgl.BufferUtils.createByteBuffer(4).put(new byte[] {-1, -1, -1, -1}); pixel.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST); GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        return texture;
    }
    private static float[] pixel(int x, int y) { float[] values = new float[4]; glReadPixels(x, y, 1, 1, GL_RGBA, GL_FLOAT, values); return values; }
    private static void require(boolean pass, String message) { checks++; if (!pass) throw new AssertionError(message); }
}
