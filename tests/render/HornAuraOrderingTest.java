import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

/** 隐藏 GPU 场景：迟绘身体、柔边深度与原版 Fabulous 合成。 */
public final class HornAuraOrderingTest {
    private static final int SIZE = 32;
    private static final float[] BODY = {0.7f, 0.2f, 0.12f, 1};
    private static final float[] FRONT = {0.1f, 0.65f, 0.2f, 1};
    private static int aura, solid, combine, checks;
    private static final String VERTEX = """
            #version 150
            uniform float Depth;
            uniform vec4 Tint;
            out float vertexDistance;
            out vec4 vertexColor;
            out vec2 texCoord0;
            out vec2 auraCoord;
            out vec2 texCoord;
            out vec3 viewPosition;
            out vec3 viewNormal;
            out float flowTime;
            flat out int effect;
            void main() {
                vec2 p = vec2((gl_VertexID & 1) * 2 - 1, (gl_VertexID >> 1) * 2 - 1);
                gl_Position = vec4(p, Depth * 2.0 - 1.0, 1.0);
                texCoord = p * 0.5 + 0.5;
                texCoord0 = vec2(0.5, 0.35);
                auraCoord = texCoord0;
                vertexDistance = 0.0;
                vertexColor = Tint;
                viewPosition = vec3(0, 0, -1);
                viewNormal = vec3(0, 0, 1);
                flowTime = 0.0;
                effect = 0;
            }
            """;

    public static void main(String[] args) throws Exception {
        if (!glfwInit()) throw new AssertionError("GLFW init");
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        long window = glfwCreateWindow(SIZE, SIZE, "Horn aura ordering regression", 0, 0);
        if (window == 0) throw new AssertionError("hidden GL context");
        try (ZipFile mc = new ZipFile(args[0])) {
            glfwMakeContextCurrent(window);
            GL.createCapabilities();
            glBindVertexArray(glGenVertexArrays());
            glViewport(0, 0, SIZE, SIZE);
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LEQUAL);
            String fog = read(mc, "assets/minecraft/shaders/include/fog.glsl")
                    .replaceAll("(?m)^#version[^\\r\\n]*", "");
            Path source = Path.of(args[1]);
            if (Files.isDirectory(source)) source = source.resolve("horn_aura.fsh");
            aura = program(Files.readString(source).replace("#moj_import <fog.glsl>", fog));
            solid = program("#version 150\nin vec4 vertexColor; out vec4 fragColor; void main(){fragColor=vertexColor;}");
            combine = program(read(mc, "assets/minecraft/shaders/program/transparency.fsh"));
            Target scene = target(), item = target(), empty = target(), output = target();
            clear(empty);

            background(scene);
            glow(true);
            drawBody();
            check(pixel(scene)[0] < BODY[0] - 0.2f,
                    "old mid-bone ALL_MASK reproduces missing late body");
            check(near(depth(scene), 0.3f), "old soft aura incorrectly owns main depth");

            background(scene);
            glow(false);
            drawBody();
            check(close(pixel(scene), BODY),
                    "COLOR_MASK alone at old time loses aura under later body");

            background(scene);
            drawBody();
            float[] opaque = pixel(scene);
            glow(false);
            float[] lateGlow = pixel(scene);
            check(lateGlow[0] >= opaque[0] && lateGlow[2] > opaque[2] + 0.08f,
                    "deferred color-only aura retains body and visible glow");
            check(near(depth(scene), 0.6f), "deferred ordinary/preview aura leaves body depth intact");

            background(scene);
            drawBody();
            draw(solid, 0.2f, FRONT, false, true);
            glow(false);
            check(close(pixel(scene), FRONT), "foreground still occludes deferred glow");
            check(near(depth(scene), 0.2f), "foreground depth unchanged by glow");

            background(scene);
            copyDepth(scene, item);
            bind(scene);
            drawBody();
            bind(item);
            glow(false);
            check(near(depth(item), 0.8f), "Fabulous color-only target retains copied terrain depth");
            composite(scene, item, empty, output);
            check(close(pixel(output), BODY),
                    "actual Fabulous shader swallows color-only aura sorted behind opaque body");

            background(scene);
            copyDepth(scene, item);
            bind(scene);
            drawBody();
            bind(item);
            glow(true);
            float[] auraTarget = pixel(item);
            check(auraTarget[3] > 0.001f && near(depth(item), 0.3f),
                    "Fabulous deferred target has glow color and correct compositor depth");
            check(near(depth(scene), 0.6f) && close(pixel(scene), BODY),
                    "Fabulous glow never modifies opaque body target");
            composite(scene, item, empty, output);
            float[] expected = new float[4];
            for (int i = 0; i < 3; i++) expected[i] = BODY[i] * (1 - auraTarget[3]) + auraTarget[i];
            expected[3] = 1;
            check(close(pixel(output), expected),
                    "actual Fabulous compositor combines deferred aura over intact body");
            check(pixel(output)[2] > BODY[2] + 0.08f, "Fabulous glow survives combine");

            bind(scene);
            draw(solid, 0.2f, FRONT, false, true);
            composite(scene, item, empty, output);
            check(close(pixel(output), FRONT),
                    "actual Fabulous compositor honors foreground opaque depth");

            // 合成后的新 target 内容不会追溯改变已经输出的画面，必须在合成前排队绘制。
            clear(item);
            composite(scene, item, empty, output);
            float[] alreadyCombined = pixel(output);
            bind(item);
            glow(true);
            check(close(pixel(output), alreadyCombined),
                    "drawing into item target after combine cannot update that frame output");
            check(glGetError() == GL_NO_ERROR, "no GPU errors");
            System.out.println("PASS horn aura ordering GPU: " + checks + " checks; " + glGetString(GL_RENDERER));
            for (Target target : new Target[] {scene, item, empty, output}) {
                glDeleteFramebuffers(target.framebuffer);
                glDeleteTextures(target.color);
                glDeleteTextures(target.depth);
            }
            glDeleteProgram(aura); glDeleteProgram(solid); glDeleteProgram(combine);
        } finally {
            glfwDestroyWindow(window);
            glfwTerminate();
        }
    }

    private record Target(int framebuffer, int color, int depth) {}

    private static Target target() {
        int framebuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        int color = texture(GL_RGBA32F, GL_RGBA);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, color, 0);
        int depth = texture(GL_DEPTH_COMPONENT32F, GL_DEPTH_COMPONENT);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depth, 0);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new AssertionError("floating point framebuffer incomplete");
        return new Target(framebuffer, color, depth);
    }

    private static int texture(int internal, int format) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexImage2D(GL_TEXTURE_2D, 0, internal, SIZE, SIZE, 0, format, GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        return id;
    }

    private static void bind(Target target) { glBindFramebuffer(GL_FRAMEBUFFER, target.framebuffer); }

    private static void clear(Target target) {
        bind(target);
        glDepthMask(true);
        glClearColor(0, 0, 0, 0);
        glClearDepth(1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    private static void background(Target scene) {
        clear(scene);
        draw(solid, 0.8f, new float[] {0.08f, 0.15f, 0.1f, 1}, false, true);
    }

    private static void copyDepth(Target scene, Target item) {
        clear(item);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, scene.framebuffer);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, item.framebuffer);
        glBlitFramebuffer(0, 0, SIZE, SIZE, 0, 0, SIZE, SIZE, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
    }

    private static void drawBody() { draw(solid, 0.6f, BODY, false, true); }

    private static void glow(boolean depth) {
        draw(aura, 0.3f, new float[] {0.3f, 0.1f, 0.8f, 0.55f}, true, depth);
    }

    private static void draw(int program, float depth, float[] tint, boolean blend, boolean writeDepth) {
        glUseProgram(program);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(writeDepth);
        if (blend) glEnable(GL_BLEND); else glDisable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        glUniform1f(glGetUniformLocation(program, "Depth"), depth);
        glUniform4f(glGetUniformLocation(program, "Tint"), tint[0], tint[1], tint[2], tint[3]);
        glUniform4f(glGetUniformLocation(program, "ColorModulator"), 1, 1, 1, 1);
        glUniform1f(glGetUniformLocation(program, "FogStart"), 0);
        glUniform1f(glGetUniformLocation(program, "FogEnd"), 100);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }

    private static void composite(Target scene, Target item, Target empty, Target output) {
        bind(output);
        glUseProgram(combine);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
        String[] names = {"Diffuse", "Translucent", "ItemEntity", "Particles", "Weather", "Clouds"};
        Target[] targets = {scene, empty, item, empty, empty, empty};
        for (int i = 0; i < names.length; i++) {
            glActiveTexture(GL_TEXTURE0 + i * 2);
            glBindTexture(GL_TEXTURE_2D, targets[i].color);
            glUniform1i(glGetUniformLocation(combine, names[i] + "Sampler"), i * 2);
            glActiveTexture(GL_TEXTURE0 + i * 2 + 1);
            glBindTexture(GL_TEXTURE_2D, targets[i].depth);
            glUniform1i(glGetUniformLocation(combine, names[i] + "DepthSampler"), i * 2 + 1);
        }
        glUniform1f(glGetUniformLocation(combine, "Depth"), 0.5f);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glActiveTexture(GL_TEXTURE0);
    }

    private static float[] pixel(Target target) {
        bind(target);
        float[] result = new float[4];
        glReadPixels(SIZE / 2, SIZE / 2, 1, 1, GL_RGBA, GL_FLOAT, result);
        return result;
    }

    private static float depth(Target target) {
        bind(target);
        float[] result = new float[1];
        glReadPixels(SIZE / 2, SIZE / 2, 1, 1, GL_DEPTH_COMPONENT, GL_FLOAT, result);
        return result[0];
    }

    private static boolean near(float a, float b) { return Math.abs(a - b) < 1e-4f; }

    private static boolean close(float[] a, float[] b) {
        for (int i = 0; i < a.length; i++) if (!near(a[i], b[i])) return false;
        return true;
    }

    private static int program(String fragment) {
        int program = glCreateProgram();
        for (int type : new int[] {GL_VERTEX_SHADER, GL_FRAGMENT_SHADER}) {
            int shader = glCreateShader(type);
            glShaderSource(shader, type == GL_VERTEX_SHADER ? VERTEX : fragment);
            glCompileShader(shader);
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) throw new AssertionError(glGetShaderInfoLog(shader));
            glAttachShader(program, shader);
            glDeleteShader(shader);
        }
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) throw new AssertionError(glGetProgramInfoLog(program));
        return program;
    }

    private static String read(ZipFile zip, String path) throws Exception {
        try (var stream = zip.getInputStream(zip.getEntry(path))) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message);
    }
}
