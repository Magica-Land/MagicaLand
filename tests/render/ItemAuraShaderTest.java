import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

public final class ItemAuraShaderTest {
    private static int shader, solid, silhouette, clearTexture, checks;
    private static final String VERTEX = """
            #version 150
            uniform float Depth;
            uniform float Time;
            uniform int Effect;
            uniform vec4 Tint;
            out float vertexDistance;
            out vec4 vertexColor;
            out vec2 texCoord0;
            out vec2 auraCoord;
            out vec3 viewPosition;
            out vec3 viewNormal;
            out float flowTime;
            flat out int effect;
            void main() {
                vec2 p = vec2((gl_VertexID & 1) * 2 - 1, (gl_VertexID >> 1) * 2 - 1);
                gl_Position = vec4(p, Depth * 2.0 - 1.0, 1.0);
                texCoord0 = p * 0.5 + 0.5;
                auraCoord = texCoord0;
                vertexDistance = 0.0;
                vertexColor = Tint;
                viewPosition = vec3(0, 0, -1);
                viewNormal = vec3(0, 0, 1);
                flowTime = Time;
                effect = Effect;
            }
            """;

    public static void main(String[] args) throws Exception {
        if (!glfwInit()) throw new AssertionError("GLFW init");
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        long window = glfwCreateWindow(32, 32, "Item aura shader regression", 0, 0);
        if (window == 0) throw new AssertionError("hidden GL context");
        try (ZipFile mc = new ZipFile(args[0])) {
            glfwMakeContextCurrent(window);
            GL.createCapabilities();
            glBindVertexArray(glGenVertexArrays());
            glViewport(0, 0, 32, 32);
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LEQUAL);
            String fog = new String(mc.getInputStream(mc.getEntry("assets/minecraft/shaders/include/fog.glsl")).readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("(?m)^#version[^\\r\\n]*", "");
            Path folder = Path.of(args[1]);
            String fragment = Files.readString(folder.resolve("horn_aura.fsh")).replace("#moj_import <fog.glsl>", fog);
            int actual = program(Files.readString(folder.resolve("horn_aura.vsh")).replace("#moj_import <fog.glsl>", fog), fragment);
            check(glGetAttribLocation(actual, "UV2") >= 0, "actual shader reads packed local item height");
            check(glGetUniformLocation(actual, "Sampler0") >= 0, "actual shader samples original silhouette");
            glDeleteProgram(actual);
            shader = program(VERTEX, fragment);
            solid = program(VERTEX, "#version 150\nin vec4 vertexColor; out vec4 fragColor;void main(){fragColor=vertexColor;}");
            silhouette = texture(false);
            clearTexture = texture(true);
            bind(silhouette);

            clear();
            aura(0, 2, true);
            check(pixel(12, 16)[3] > 0.25f, "item silhouette receives visible glow");
            check(pixel(1, 1)[3] == 0 && depth(1, 1) == 1, "transparent sprite corners neither color nor depth");
            check(pixel(17, 17)[3] == 0 && depth(17, 17) == 1, "interior alpha hole remains empty");
            check(Math.abs(depth(12, 16) - 0.3f) < 1e-5, "Fabulous visible silhouette supplies depth");
            draw(solid, 0.6f, 0, 0, true, 0.8f, 0.1f, 0.1f, 1);
            check(pixel(1, 1)[0] > 0.75f && pixel(17, 17)[0] > 0.75f,
                    "transparent texture holes cannot punch out late background geometry");

            float minimum = 1, maximum = 0;
            for (int phase = 0; phase < 24; phase++) {
                clear();
                aura(phase * 0.5f, 2, false);
                float alpha = pixel(12, 16)[3];
                minimum = Math.min(minimum, alpha); maximum = Math.max(maximum, alpha);
            }
            check(maximum - minimum > 0.07f && maximum - minimum < 0.29f,
                    "slow local wisps vary moderately without a synchronized bright/dark pulse");
            clear(); aura(0, 2, false); float[] start = pixel(12, 16);
            clear(); aura(12, 2, false);
            check(close(start, pixel(12, 16)), "item and horn clocks wrap seamlessly");

            clear();
            draw(solid, 0.6f, 0, 0, true, 0.7f, 0.1f, 0.1f, 1);
            aura(0, 2, false);
            check(Math.abs(depth(12, 16) - 0.6f) < 1e-5, "normal/first-person/GUI flow never writes opaque body depth");
            clear();
            draw(solid, 0.2f, 0, 0, true, 0.1f, 0.7f, 0.1f, 1);
            aura(0, 2, false);
            check(pixel(12, 16)[1] > 0.69f && pixel(12, 16)[2] < 0.11f, "foreground still occludes item glow");

            bind(clearTexture);
            clear(); aura(0, 0, true);
            check(pixel(12, 16)[3] > 0.25f, "horn aura remains independent of bound texture alpha");
            clear(); aura(0, 1, true);
            check(pixel(16, 16)[3] > 0.65f, "tiny stars remain independent of bound texture alpha");
            clear(); aura(0, 2, true);
            check(pixel(12, 16)[3] == 0 && depth(12, 16) == 1, "fully transparent item produces no phantom rectangle");
            check(glGetError() == GL_NO_ERROR, "no GPU errors");
            System.out.println("PASS item aura GPU: " + checks + " checks; " + glGetString(GL_RENDERER));
            glDeleteProgram(shader); glDeleteProgram(solid);
            glDeleteTextures(silhouette); glDeleteTextures(clearTexture);
        } finally {
            glfwDestroyWindow(window);
            glfwTerminate();
        }
    }

    private static void bind(int texture) { glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, texture); }
    private static int texture(boolean empty) {
        int id = glGenTextures();
        bind(id);
        ByteBuffer pixels = BufferUtils.createByteBuffer(8 * 8 * 4);
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
            boolean opaque = !empty && (x >= 2 && x <= 5 || y >= 3 && y <= 4) && !(x == 4 && y == 4);
            pixels.put((byte)255).put((byte)255).put((byte)255).put((byte)(opaque ? 255 : 0));
        }
        pixels.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 8, 8, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        return id;
    }
    private static void clear() {
        glDepthMask(true); glClearColor(0, 0, 0, 0); glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }
    private static void aura(float time, int effect, boolean depth) { draw(shader, 0.3f, time, effect, depth, 0.3f, 0.1f, 0.8f, 0.7f); }
    private static void draw(int program, float depth, float time, int effect, boolean writeDepth, float r, float g, float b, float a) {
        glUseProgram(program); glDisable(GL_BLEND); glDepthMask(writeDepth);
        glUniform1f(glGetUniformLocation(program, "Depth"), depth);
        glUniform1f(glGetUniformLocation(program, "Time"), time);
        glUniform1i(glGetUniformLocation(program, "Effect"), effect);
        glUniform4f(glGetUniformLocation(program, "Tint"), r, g, b, a);
        glUniform4f(glGetUniformLocation(program, "ColorModulator"), 1, 1, 1, 1);
        glUniform1f(glGetUniformLocation(program, "FogStart"), 0);
        glUniform1f(glGetUniformLocation(program, "FogEnd"), 100);
        glUniform1i(glGetUniformLocation(program, "Sampler0"), 0);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }
    private static float[] pixel(int x, int y) { float[] p = new float[4]; glReadPixels(x, y, 1, 1, GL_RGBA, GL_FLOAT, p); return p; }
    private static float depth(int x, int y) { float[] p = new float[1]; glReadPixels(x, y, 1, 1, GL_DEPTH_COMPONENT, GL_FLOAT, p); return p[0]; }
    private static boolean close(float[] a, float[] b) { for (int i = 0; i < a.length; i++) if (Math.abs(a[i] - b[i]) > 0.01f) return false; return true; }
    private static int program(String vertex, String fragment) {
        int program = glCreateProgram();
        for (int type : new int[] {GL_VERTEX_SHADER, GL_FRAGMENT_SHADER}) {
            int shader = glCreateShader(type); glShaderSource(shader, type == GL_VERTEX_SHADER ? vertex : fragment); glCompileShader(shader);
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) throw new AssertionError(glGetShaderInfoLog(shader));
            glAttachShader(program, shader); glDeleteShader(shader);
        }
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) throw new AssertionError(glGetProgramInfoLog(program));
        return program;
    }
    private static void check(boolean valid, String message) { checks++; if (!valid) throw new AssertionError(message); }
}
