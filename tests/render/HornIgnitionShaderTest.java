import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

/** 实际角部 shader 的逐顶点进度、正反向推进及深度回归。 */
public final class HornIgnitionShaderTest {
    private static final int SIZE = 128, STRIDE = 64;
    private static int shader, buffer, checks;

    public static void main(String[] args) throws Exception {
        if (!glfwInit()) throw new AssertionError("GLFW init");
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        long window = glfwCreateWindow(SIZE, SIZE, "Horn ignition regression", 0, 0);
        if (window == 0) throw new AssertionError("hidden context");
        try (ZipFile mc = new ZipFile(args[0])) {
            glfwMakeContextCurrent(window);
            GL.createCapabilities();
            glBindVertexArray(glGenVertexArrays());
            String fog = new String(mc.getInputStream(mc.getEntry("assets/minecraft/shaders/include/fog.glsl")).readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("(?m)^#version[^\\r\\n]*", "");
            Path folder = Path.of(args[1]);
            shader = program(Files.readString(folder.resolve("horn_aura.vsh")).replace("#moj_import <fog.glsl>", fog),
                    Files.readString(folder.resolve("horn_aura.fsh")).replace("#moj_import <fog.glsl>", fog));
            check(glGetAttribLocation(shader, "UV2") >= 0, "real vertex shader consumes per-vertex progress");
            buffer = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, buffer);
            attribute("Position", 3, 0, false);
            attribute("Color", 4, 12, false);
            attribute("UV0", 2, 28, false);
            attribute("UV1", 2, 36, true);
            attribute("UV2", 2, 44, true);
            attribute("Normal", 3, 52, false);
            int framebuffer = glGenFramebuffers();
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
            int color = texture(GL_RGBA32F, GL_RGBA), depth = texture(GL_DEPTH_COMPONENT32F, GL_DEPTH_COMPONENT);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, color, 0);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depth, 0);
            check(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE, "floating point framebuffer");
            glViewport(0, 0, SIZE, SIZE);
            glUseProgram(shader);
            float[] identity4 = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
            glUniformMatrix4fv(glGetUniformLocation(shader, "ModelViewMat"), false, identity4);
            glUniformMatrix4fv(glGetUniformLocation(shader, "ProjMat"), false, identity4);
            glUniformMatrix3fv(glGetUniformLocation(shader, "IViewRotMat"), false, new float[] {1,0,0,0,1,0,0,0,1});
            glUniform1i(glGetUniformLocation(shader, "FogShape"), 0);
            glUniform1f(glGetUniformLocation(shader, "FogStart"), 100);
            glUniform1f(glGetUniformLocation(shader, "FogEnd"), 200);
            glUniform4f(glGetUniformLocation(shader, "ColorModulator"), 1, 1, 1, 1);
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LEQUAL);
            glDisable(GL_BLEND);

            draw(0, 1, true, 1);
            float[] legacy = image();
            draw(4, 1, true, 1);
            float[] complete = image();
            for (int i = 0; i < legacy.length; i++) close(legacy[i], complete[i], 1e-6f,
                    "full ignition preserves exact legacy horn look");
            draw(4, 0, true, 1);
            for (float value : image()) close(value, 0, 1e-9f, "zero ignition has no colored shell");
            close(depth(SIZE / 2, SIZE / 2), 1, 1e-6f, "unlit horn never writes depth");

            float[] previous = new float[SIZE];
            for (int step = 0; step <= 10; step++) {
                float progress = step / 10f;
                draw(4, progress, true, 1);
                for (int y = 0; y < SIZE; y++) {
                    float alpha = pixel(SIZE / 2, y)[3];
                    check(alpha + 1e-6f >= previous[y], "forward ignition is monotone at every height");
                    previous[y] = alpha;
                }
            }
            for (int step = 9; step >= 0; step--) {
                draw(4, step / 10f, true, 1);
                for (int y = 0; y < SIZE; y++) {
                    float alpha = pixel(SIZE / 2, y)[3];
                    check(alpha <= previous[y] + 1e-6f, "reverse removes tip light without re-lighting lower rows");
                    previous[y] = alpha;
                }
            }
            draw(4, .5f, true, 1);
            check(pixel(64, 28)[3] > .01f, "halfway root is lit");
            close(pixel(64, 100)[3], 0, 1e-9f, "halfway tip is still unlit");
            close(depth(64, 100), 1, 1e-6f, "unlit tip preserves copied Fabulous depth");
            close(depth(64, 28), .3f, 1e-6f, "lit root provides Fabulous compositor depth");
            float fraction = pixel(64, 63)[3] / complete[(63 * SIZE + 64) * 4 + 3];
            check(fraction > .4f && fraction < .6f, "front is a soft gradient, not a chopped ring");
            draw(4, .5f, false, .6f);
            check(pixel(64, 28)[3] > .01f, "ordinary deferred root remains visible");
            close(depth(64, 28), .6f, 1e-6f, "ordinary color-only ignition leaves body depth untouched");
            draw(4, .5f, true, .2f);
            close(pixel(64, 28)[3], 0, 1e-9f, "foreground geometry still blocks root glow");
            close(depth(64, 28), .2f, 1e-6f, "foreground depth unchanged");

            // 一个 draw call 两只角，各自进度必须来自顶点而不是最后一次 uniform。
            clear(1);
            ByteBuffer batch = BufferUtils.createByteBuffer(STRIDE * 12);
            quad(batch, -1, 0, 4, .25f, 0);
            quad(batch, 0, 1, 4, .75f, 0);
            batch.flip(); upload(batch, 12);
            check(pixel(32, 64)[3] == 0 && pixel(96, 64)[3] > .01f, "batched players keep independent progress");
            check(pixel(32, 12)[3] > 0 && pixel(96, 12)[3] > 0, "both batched roots can be lit");
            float[] before = image();
            clear(1);
            batch.clear();
            quad(batch, -1, 0, 4, .75f, 0);
            quad(batch, 0, 1, 4, .25f, 0);
            batch.flip(); upload(batch, 12);
            check(pixel(32, 64)[3] > .01f && pixel(96, 64)[3] == 0, "swapping vertex progress swaps lit horn, no cross-player state");
            check(before[(64 * SIZE + 32) * 4 + 3] != pixel(32, 64)[3], "progress is not stale between batches");
            check(glGetError() == GL_NO_ERROR, "no GPU errors");
            System.out.println("PASS horn ignition GPU: " + checks + " checks; " + glGetString(GL_RENDERER));
            glDeleteBuffers(buffer); glDeleteProgram(shader);
            glDeleteFramebuffers(framebuffer); glDeleteTextures(color); glDeleteTextures(depth);
        } finally {
            glfwDestroyWindow(window);
            glfwTerminate();
        }
    }

    private static void draw(int effect, float progress, boolean writeDepth, float backgroundDepth) {
        clear(backgroundDepth);
        glDepthMask(writeDepth);
        ByteBuffer data = BufferUtils.createByteBuffer(STRIDE * 6);
        quad(data, -1, 1, effect, progress, 0);
        data.flip(); upload(data, 6);
    }

    private static void quad(ByteBuffer data, float left, float right, int effect, float progress, int clock) {
        int[] vertices = {0, 1, 2, 2, 1, 3};
        for (int vertex : vertices) {
            float u = vertex & 1, v = vertex >> 1;
            data.putFloat(u == 0 ? left : right).putFloat(v * 2 - 1).putFloat(-.4f);
            data.putFloat(.6f).putFloat(.2f).putFloat(1).putFloat(.7f);
            data.putFloat(u).putFloat(v);
            data.putInt(clock).putInt(effect);
            data.putInt(Math.round(progress * 32767)).putInt(0);
            data.putFloat(0).putFloat(0).putFloat(1);
        }
    }

    private static void upload(ByteBuffer data, int count) {
        glBindBuffer(GL_ARRAY_BUFFER, buffer);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STREAM_DRAW);
        glDrawArrays(GL_TRIANGLES, 0, count);
    }

    private static void clear(float depth) {
        glDepthMask(true);
        glClearColor(0, 0, 0, 0); glClearDepth(depth);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    private static void attribute(String name, int count, int offset, boolean integer) {
        int location = glGetAttribLocation(shader, name);
        check(location >= 0, "active actual shader attribute " + name);
        glEnableVertexAttribArray(location);
        if (integer) glVertexAttribIPointer(location, count, GL_INT, STRIDE, offset);
        else glVertexAttribPointer(location, count, GL_FLOAT, false, STRIDE, offset);
    }

    private static int texture(int internal, int format) {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, internal, SIZE, SIZE, 0, format, GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        return texture;
    }

    private static float[] image() {
        float[] values = new float[SIZE * SIZE * 4];
        glReadPixels(0, 0, SIZE, SIZE, GL_RGBA, GL_FLOAT, values);
        return values;
    }

    private static float[] pixel(int x, int y) {
        float[] values = new float[4]; glReadPixels(x, y, 1, 1, GL_RGBA, GL_FLOAT, values); return values;
    }

    private static float depth(int x, int y) {
        float[] values = new float[1]; glReadPixels(x, y, 1, 1, GL_DEPTH_COMPONENT, GL_FLOAT, values); return values[0];
    }

    private static int program(String vertex, String fragment) {
        int program = glCreateProgram();
        for (int type : new int[] {GL_VERTEX_SHADER, GL_FRAGMENT_SHADER}) {
            int shader = glCreateShader(type);
            glShaderSource(shader, type == GL_VERTEX_SHADER ? vertex : fragment); glCompileShader(shader);
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) throw new AssertionError(glGetShaderInfoLog(shader));
            glAttachShader(program, shader); glDeleteShader(shader);
        }
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) throw new AssertionError(glGetProgramInfoLog(program));
        return program;
    }

    private static void close(float actual, float expected, float tolerance, String message) {
        check(Math.abs(actual - expected) <= tolerance, message + ": " + actual + " != " + expected);
    }
    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
}
