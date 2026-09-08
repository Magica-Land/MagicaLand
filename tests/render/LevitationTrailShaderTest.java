import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

/** 隐藏 GPU 回归：薄拖尾的柔边、渐细淡出和独立透明目标深度。 */
public final class LevitationTrailShaderTest {
    private static final int SIZE = 256;
    private static final float[] BODY = {0.16f, 0.23f, 0.12f, 1};
    private static final float[] FRONT = {0.35f, 0.18f, 0.1f, 1};
    private static int trail, solid, checks;
    private static final String VERTEX = """
            #version 150
            uniform float Depth;
            uniform float Time;
            uniform float HalfWidth;
            uniform float Taper;
            uniform float Fade;
            uniform vec4 Tint;
            out float vertexDistance;
            out vec4 vertexColor;
            out vec2 texCoord0;
            out vec3 viewPosition;
            out vec3 viewNormal;
            out float flowTime;
            flat out int effect;
            void main() {
                vec2 uv = vec2(gl_VertexID & 1, gl_VertexID >> 1);
                float width = HalfWidth * mix(1.0, mix(0.18, 1.0, uv.x), Taper);
                gl_Position = vec4(uv.x * 2.0 - 1.0, (uv.y * 2.0 - 1.0) * width,
                        Depth * 2.0 - 1.0, 1.0);
                vertexDistance = 0.0;
                vertexColor = vec4(Tint.rgb, Tint.a * mix(1.0, uv.x, Fade));
                texCoord0 = uv;
                viewPosition = vec3(0, 0, -1);
                viewNormal = vec3(0, 0, 1);
                flowTime = Time;
                effect = 0;
            }
            """;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Minecraft jar and shader folder required");
        if (!glfwInit()) throw new AssertionError("GLFW init");
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        long window = glfwCreateWindow(SIZE, SIZE, "Levitation trail shader regression", 0, 0);
        if (window == 0) {
            glfwTerminate();
            throw new AssertionError("hidden GL context");
        }
        Target scene = null, item = null;
        int vao = 0;
        try (ZipFile mc = new ZipFile(args[0])) {
            glfwMakeContextCurrent(window);
            GL.createCapabilities();
            vao = glGenVertexArrays();
            glBindVertexArray(vao);
            String fog;
            try (var stream = mc.getInputStream(mc.getEntry("assets/minecraft/shaders/include/fog.glsl"))) {
                fog = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                        .replaceAll("(?m)^#version[^\\r\\n]*", "");
            }
            String fragment = Files.readString(Path.of(args[1]).resolve("horn_aura.fsh"))
                    .replace("#moj_import <fog.glsl>", fog);
            trail = program(fragment);
            solid = program("#version 150\nin vec4 vertexColor; out vec4 fragColor; void main(){fragColor=vertexColor;}");
            scene = target();
            item = target();
            glViewport(0, 0, SIZE, SIZE);
            glDepthFunc(GL_LEQUAL);

            clear(scene);
            ribbon(0, false, false, true, false);
            float core = pixel(128, 128)[3];
            check(core > 0.06f && core <= 0.16f, "effect 0 thin strip has a low-alpha visible core");
            check(pixel(128, 96)[3] == 0 && pixel(128, 159)[3] == 0,
                    "both lateral soft edges discard before reaching the strip boundary");
            check(near(depth(128, 96), 1) && near(depth(128, 159), 1),
                    "transparent lateral edges never write depth");
            check(pixel(128, 100)[3] > 0 && pixel(128, 100)[3] < core,
                    "lateral edge fades smoothly rather than a hard cutoff");
            check(near(depth(128, 128), 0.3f), "visible low-alpha strip owns its isolated target depth");
            check(pixel(128, 95)[3] == 0 && pixel(128, 160)[3] == 0,
                    "the thin strip does not shade outside its geometry");
            float minimum = 1, maximum = 0;
            for (int phase = 0; phase < 8; phase++) {
                clear(scene);
                ribbon(phase * 0.25f, false, false, true, false);
                float alpha = pixel(128, 128)[3];
                minimum = Math.min(minimum, alpha);
                maximum = Math.max(maximum, alpha);
                check(alpha >= 0 && alpha <= 0.16001f, "flow cannot exceed the 0.16 vertex-alpha cap");
            }
            check(maximum - minimum > 0.06f, "flow changes the thin strip over time");
            clear(scene);
            ribbon(12, false, false, true, false);
            check(Math.abs(pixel(128, 128)[3] - core) < 0.0001f, "packed flow clock wraps seamlessly");

            clear(scene);
            ribbon(0, true, true, true, false);
            float[] tail = column(48), head = column(207), oldest = column(0);
            check(tail[0] < head[0] * 0.65f && tail[0] > 0,
                    "old trail end is geometrically narrower than the fresh end");
            check(tail[1] < 0.031f && head[1] > 0.065f,
                    "old trail end fades while the fresh end remains visible");
            check(oldest[0] == 0 && near(depth(0, 128), 1),
                    "zero-alpha oldest endpoint does not leave a color or depth cap");

            clear(scene);
            fill(0.6f, BODY);
            ribbon(0, false, false, false, true);
            float[] ordinary = pixel(128, 128);
            check(ordinary[2] > BODY[2] + 0.04f && ordinary[0] >= BODY[0],
                    "ordinary additive trail remains visible over the background");
            check(near(depth(128, 128), 0.6f) && near(depth(128, 96), 0.6f),
                    "ordinary trail never changes opaque scene depth");
            check(close(pixel(128, 96), BODY), "ordinary discarded edge preserves scene color");
            fill(0.2f, FRONT);
            ribbon(0, false, false, false, true);
            check(close(pixel(128, 128), FRONT) && near(depth(128, 128), 0.2f),
                    "foreground opaque geometry occludes ordinary trail");

            clear(scene);
            fill(0.6f, BODY);
            copyDepth(scene, item);
            bind(item);
            ribbon(0, false, false, true, true);
            check(pixel(128, 128)[2] > 0.04f && near(depth(128, 128), 0.3f),
                    "Fabulous visible trail supplies actual depth to its isolated target");
            check(pixel(128, 96)[3] == 0 && near(depth(128, 96), 0.6f),
                    "Fabulous discarded lateral corner retains copied scene depth");
            bind(scene);
            check(close(pixel(128, 128), BODY) && near(depth(128, 128), 0.6f),
                    "Fabulous trail leaves the main framebuffer color and depth unchanged");
            fill(0.2f, FRONT);
            copyDepth(scene, item);
            bind(item);
            ribbon(0, false, false, true, true);
            check(pixel(128, 128)[3] == 0 && near(depth(128, 128), 0.2f),
                    "copied foreground depth occludes the isolated Fabulous trail");
            check(glGetError() == GL_NO_ERROR, "no GPU errors");
            System.out.println("PASS levitation trail shader GPU: " + checks + " checks; " + glGetString(GL_RENDERER));
        } finally {
            if (scene != null) scene.close();
            if (item != null) item.close();
            if (trail != 0) glDeleteProgram(trail);
            if (solid != 0) glDeleteProgram(solid);
            if (vao != 0) glDeleteVertexArrays(vao);
            glfwDestroyWindow(window);
            glfwTerminate();
        }
    }

    private record Target(int framebuffer, int color, int depth) {
        void close() {
            glDeleteFramebuffers(framebuffer);
            glDeleteTextures(color);
            glDeleteTextures(depth);
        }
    }

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

    private static void copyDepth(Target scene, Target item) {
        clear(item);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, scene.framebuffer);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, item.framebuffer);
        glBlitFramebuffer(0, 0, SIZE, SIZE, 0, 0, SIZE, SIZE, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
    }

    private static void fill(float depth, float[] tint) {
        draw(solid, depth, 0, 1, false, false, true, false, tint);
    }

    private static void ribbon(float time, boolean taper, boolean fade, boolean writeDepth, boolean blend) {
        draw(trail, 0.3f, time, 0.25f, taper, fade, writeDepth, blend,
                new float[] {0.6f, 0.2f, 1, 0.16f});
    }

    private static void draw(int program, float depth, float time, float halfWidth, boolean taper,
                             boolean fade, boolean writeDepth, boolean blend, float[] tint) {
        glUseProgram(program);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(writeDepth);
        if (blend) glEnable(GL_BLEND); else glDisable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        glUniform1f(glGetUniformLocation(program, "Depth"), depth);
        glUniform1f(glGetUniformLocation(program, "Time"), time);
        glUniform1f(glGetUniformLocation(program, "HalfWidth"), halfWidth);
        glUniform1f(glGetUniformLocation(program, "Taper"), taper ? 1 : 0);
        glUniform1f(glGetUniformLocation(program, "Fade"), fade ? 1 : 0);
        glUniform4f(glGetUniformLocation(program, "Tint"), tint[0], tint[1], tint[2], tint[3]);
        glUniform4f(glGetUniformLocation(program, "ColorModulator"), 1, 1, 1, 1);
        glUniform1f(glGetUniformLocation(program, "FogStart"), 0);
        glUniform1f(glGetUniformLocation(program, "FogEnd"), 100);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }

    private static float[] column(int x) {
        float[] values = new float[SIZE * 4];
        glReadPixels(x, 0, 1, SIZE, GL_RGBA, GL_FLOAT, values);
        float count = 0, maximum = 0;
        for (int y = 0; y < SIZE; y++) {
            float alpha = values[y * 4 + 3];
            if (alpha > 0) count++;
            maximum = Math.max(maximum, alpha);
        }
        return new float[] {count, maximum};
    }

    private static float[] pixel(int x, int y) {
        float[] values = new float[4];
        glReadPixels(x, y, 1, 1, GL_RGBA, GL_FLOAT, values);
        return values;
    }

    private static float depth(int x, int y) {
        float[] values = new float[1];
        glReadPixels(x, y, 1, 1, GL_DEPTH_COMPONENT, GL_FLOAT, values);
        return values[0];
    }

    private static boolean near(float a, float b) { return Math.abs(a - b) < 0.0001f; }

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
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE)
                throw new AssertionError(glGetShaderInfoLog(shader));
            glAttachShader(program, shader);
            glDeleteShader(shader);
        }
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE)
            throw new AssertionError(glGetProgramInfoLog(program));
        return program;
    }

    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message);
    }
}
