import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

public final class HornAuraShaderTest {
    private static int shader, checks;
    private static final String VERTEX = """
            #version 150
            uniform float Time;
            uniform float Facing;
            uniform float Depth;
            uniform int Effect;
            out float vertexDistance;
            out vec4 vertexColor;
            out vec2 texCoord0;
            out vec3 viewPosition;
            out vec3 viewNormal;
            out float flowTime;
            flat out int effect;
            void main() {
                vec2 p = vec2((gl_VertexID & 1) * 2 - 1, (gl_VertexID >> 1) * 2 - 1);
                gl_Position = vec4(p, Depth * 2.0 - 1.0, 1.0);
                vertexDistance = 0.0;
                vertexColor = vec4(0.6, 0.2, 1.0, 0.7);
                texCoord0 = p * 0.5 + 0.5;
                viewPosition = vec3(0, 0, -1);
                viewNormal = vec3(sqrt(max(0.0, 1.0 - Facing * Facing)), 0, Facing);
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
        long window = glfwCreateWindow(64, 64, "Horn aura shader checks", 0, 0);
        if (window == 0) throw new AssertionError("hidden context");
        try (ZipFile mc = new ZipFile(args[0])) {
            glfwMakeContextCurrent(window);
            GL.createCapabilities();
            glBindVertexArray(glGenVertexArrays());
            String fog = new String(mc.getInputStream(mc.getEntry("assets/minecraft/shaders/include/fog.glsl")).readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("(?m)^#version[^\\r\\n]*", "");
            Path folder = Path.of(args[1]);
            String fragment = Files.readString(folder.resolve("horn_aura.fsh")).replace("#moj_import <fog.glsl>", fog);
            int actual = program(Files.readString(folder.resolve("horn_aura.vsh")).replace("#moj_import <fog.glsl>", fog), fragment);
            check(glGetAttribLocation(actual, "UV1") >= 0, "actual shader receives batched flow time and effect type");
            glDeleteProgram(actual);
            shader = program(VERTEX, fragment);
            glViewport(0, 0, 64, 64);
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LEQUAL);
            glDepthMask(true);
            glDisable(GL_BLEND);

            draw(0, 1, 0, true, 0.3f);
            float middle = pixel(32, 20)[3];
            check(middle > 0.25f, "visible flow core");
            check(pixel(32, 0)[3] < 0.01f && pixel(32, 63)[3] < 0.01f, "soft root and tip");
            check(depth(32, 0) == 1, "invisible soft edge never writes depth");
            check(Math.abs(depth(32, 20) - 0.3f) < 1e-5, "visible aura supplies compositor depth");
            float minimum = 1, maximum = 0;
            for (int phase = 0; phase < 8; phase++) {
                draw(phase * 0.25f, 1, 0, true, 0.3f);
                float opacity = pixel(32, 20)[3];
                minimum = Math.min(minimum, opacity); maximum = Math.max(maximum, opacity);
            }
            check(maximum - minimum > 0.3f, "flow moves across a full cycle");
            draw(12, 1, 0, true, 0.3f);
            check(Math.abs(pixel(32, 20)[3] - middle) < 0.01f, "packed clock wraps seamlessly");
            draw(0, 0.05f, 0, true, 0.3f);
            check(pixel(32, 20)[3] < middle * 0.12f, "grazing silhouette fades instead of hard box edge");

            draw(0, 1, 1, true, 0.3f);
            check(pixel(32, 32)[3] > 0.65f, "tiny star center visible");
            check(pixel(48, 32)[3] > 0.1f && pixel(48, 48)[3] == 0, "four-point star, not square particle");
            check(depth(63, 63) == 1, "star corners remain transparent to depth");
            float[] before = pixel(32, 32);
            draw(0.5f, 1, 0, false, 0.6f);
            check(Math.abs(pixel(32, 32)[3] - before[3]) < 0.01f, "later background cannot overwrite star");
            draw(0.5f, 1, 0, false, 0.2f);
            check(Math.abs(depth(32, 32) - 0.2f) < 1e-5, "nearer geometry still wins");
            check(glGetError() == GL_NO_ERROR, "no GPU errors");
            System.out.println("PASS horn aura GPU: " + checks + " checks; " + glGetString(GL_RENDERER));
        } finally {
            glfwDestroyWindow(window);
            glfwTerminate();
        }
    }

    private static void draw(float time, float facing, int effect, boolean clear, float depth) {
        if (clear) {
            glClearColor(0, 0, 0, 0);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        }
        glUseProgram(shader);
        glUniform1f(glGetUniformLocation(shader, "Time"), time);
        glUniform1f(glGetUniformLocation(shader, "Facing"), facing);
        glUniform1f(glGetUniformLocation(shader, "Depth"), depth);
        glUniform1i(glGetUniformLocation(shader, "Effect"), effect);
        glUniform4f(glGetUniformLocation(shader, "ColorModulator"), 1, 1, 1, 1);
        glUniform1f(glGetUniformLocation(shader, "FogStart"), 0);
        glUniform1f(glGetUniformLocation(shader, "FogEnd"), 100);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }

    private static float[] pixel(int x, int y) {
        FloatBuffer values = BufferUtils.createFloatBuffer(4);
        glReadPixels(x, y, 1, 1, GL_RGBA, GL_FLOAT, values);
        return new float[] {values.get(0), values.get(1), values.get(2), values.get(3)};
    }

    private static float depth(int x, int y) {
        FloatBuffer value = BufferUtils.createFloatBuffer(1);
        glReadPixels(x, y, 1, 1, GL_DEPTH_COMPONENT, GL_FLOAT, value);
        return value.get(0);
    }

    private static int program(String vertex, String fragment) {
        int program = glCreateProgram();
        int v = compile(GL_VERTEX_SHADER, vertex), f = compile(GL_FRAGMENT_SHADER, fragment);
        glAttachShader(program, v); glAttachShader(program, f); glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) throw new AssertionError(glGetProgramInfoLog(program));
        glDeleteShader(v); glDeleteShader(f);
        return program;
    }

    private static int compile(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source); glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) throw new AssertionError(glGetShaderInfoLog(shader));
        return shader;
    }

    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message);
    }
}
