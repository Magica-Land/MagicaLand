import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

/** 使用缓存中的原版 shader 做隐藏窗口 GPU 回归测试，不启动 Minecraft。 */
public final class MagicGlowDepthTest {
    private static int solid, oldGlow, newGlow, opaqueTexture, clearTexture;
    private static final String VERTEX = """
            #version 150
            uniform float z;
            out float vertexDistance;
            out vec4 vertexColor;
            out vec4 overlayColor;
            out vec2 texCoord0;
            out vec4 normal;
            void main() {
                vec2 p = vec2((gl_VertexID & 1) * 2 - 1, (gl_VertexID >> 1) * 2 - 1);
                gl_Position = vec4(p, z * 2.0 - 1.0, 1.0);
                texCoord0 = (p + 1.0) * 0.5;
                vertexDistance = 0.0;
                vertexColor = vec4(1.0);
                overlayColor = vec4(0.0, 0.0, 0.0, 1.0);
                normal = vec4(0.0, 0.0, 1.0, 0.0);
            }
            """;

    public static void main(String[] args) throws Exception {
        if (!glfwInit()) throw new AssertionError("GLFW init failed");
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        long window = glfwCreateWindow(32, 32, "Magic glow depth regression", 0, 0);
        if (window == 0) throw new AssertionError("hidden GL context failed");
        try (ZipFile mc = new ZipFile(args[0])) {
            glfwMakeContextCurrent(window);
            GL.createCapabilities();
            glBindVertexArray(glGenVertexArrays());
            String fog = read(mc, "assets/minecraft/shaders/include/fog.glsl").replaceAll("(?m)^#version[^\\r\\n]*", "");
            oldGlow = program(read(mc, "assets/minecraft/shaders/core/rendertype_eyes.fsh").replace("#moj_import <fog.glsl>", fog));
            String fragment = Files.readString(Path.of(args[1])).replace("#moj_import <fog.glsl>", fog);
            newGlow = program(fragment);
            int actualProgram = program(read(mc, "assets/minecraft/shaders/core/rendertype_eyes.vsh")
                    .replace("#moj_import <fog.glsl>", fog), fragment);
            glDeleteProgram(actualProgram);
            solid = program("#version 150\nuniform vec4 ColorModulator; out vec4 fragColor; void main(){fragColor=ColorModulator;}");
            opaqueTexture = texture(255);
            clearTexture = texture(0);
            glViewport(0, 0, 32, 32);
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LEQUAL);

            background();
            glow(oldGlow, false, opaqueTexture);
            draw(solid, 0.6f, 1, 0, 0, 1, false, true);
            check(pixel()[0] > 0.99f && pixel()[2] < 0.01f, "old color-only layer reproduces late background overwrite");

            background();
            glow(oldGlow, false, opaqueTexture);
            float[] oldColor = pixel();
            background();
            glow(newGlow, true, opaqueTexture);
            check(close(pixel(), oldColor), "retain original glow color and brightness");

            background();
            glow(newGlow, true, opaqueTexture);
            float[] glowPixel = pixel();
            draw(solid, 0.6f, 1, 0, 0, 1, false, true);
            check(close(pixel(), glowPixel), "later background geometry cannot overwrite glow");
            check(Math.abs(depth() - 0.3f) < 1e-5, "glow depth is available to compositor");
            draw(solid, 0.2f, 1, 0, 0, 1, false, true);
            check(pixel()[0] > 0.99f && pixel()[2] < 0.01f, "foreground geometry still occludes glow");

            background();
            draw(solid, 0.2f, 1, 0, 0, 1, false, true);
            glow(newGlow, true, opaqueTexture);
            check(pixel()[2] < 0.01f, "already-rendered foreground also occludes glow");

            background();
            glow(newGlow, true, clearTexture);
            check(Math.abs(depth() - 0.8f) < 1e-5, "transparent sprite pixels do not write depth");
            draw(solid, 0.6f, 1, 0, 0, 1, false, true);
            check(pixel()[0] > 0.99f && pixel()[2] < 0.01f, "background remains visible through transparent edges");
            check(glGetError() == GL_NO_ERROR, "no GL errors");
            System.out.println("PASS GPU depth regression: 9 checks; " + glGetString(GL_RENDERER));
        } finally {
            glfwDestroyWindow(window);
            glfwTerminate();
        }
    }

    private static void background() {
        glDepthMask(true);
        glClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        draw(solid, 0.8f, 0, 0.2f, 0, 1, false, true);
    }

    private static void glow(int shader, boolean writeDepth, int texture) {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        draw(shader, 0.3f, 0.5f, 0, 1, 0.8f, true, writeDepth);
    }

    private static void draw(int shader, float z, float r, float g, float b, float a, boolean blend, boolean writeDepth) {
        glUseProgram(shader);
        glUniform1f(glGetUniformLocation(shader, "z"), z);
        glUniform4f(glGetUniformLocation(shader, "ColorModulator"), r, g, b, a);
        glUniform1f(glGetUniformLocation(shader, "FogStart"), 0);
        glUniform1f(glGetUniformLocation(shader, "FogEnd"), 1);
        glUniform1i(glGetUniformLocation(shader, "Sampler0"), 0);
        if (blend) glEnable(GL_BLEND); else glDisable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        glDepthMask(writeDepth);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }

    private static int texture(int alpha) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        ByteBuffer bytes = BufferUtils.createByteBuffer(4).put(new byte[] {-1, -1, -1, (byte)alpha});
        bytes.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, bytes);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        return id;
    }

    private static int program(String fragment) {
        return program(VERTEX, fragment);
    }

    private static int program(String vertex, String fragment) {
        int program = glCreateProgram();
        for (int type : new int[] {GL_VERTEX_SHADER, GL_FRAGMENT_SHADER}) {
            int shader = glCreateShader(type);
            glShaderSource(shader, type == GL_VERTEX_SHADER ? vertex : fragment);
            glCompileShader(shader);
            check(glGetShaderi(shader, GL_COMPILE_STATUS) != GL_FALSE, glGetShaderInfoLog(shader));
            glAttachShader(program, shader);
            glDeleteShader(shader);
        }
        glLinkProgram(program);
        check(glGetProgrami(program, GL_LINK_STATUS) != GL_FALSE, glGetProgramInfoLog(program));
        return program;
    }

    private static float[] pixel() {
        float[] pixel = new float[4];
        glReadPixels(16, 16, 1, 1, GL_RGBA, GL_FLOAT, pixel);
        return pixel;
    }

    private static float depth() {
        FloatBuffer value = BufferUtils.createFloatBuffer(1);
        glReadPixels(16, 16, 1, 1, GL_DEPTH_COMPONENT, GL_FLOAT, value);
        return value.get(0);
    }

    private static boolean close(float[] a, float[] b) {
        for (int i = 0; i < a.length; i++) if (Math.abs(a[i] - b[i]) > 1e-5) return false;
        return true;
    }

    private static String read(ZipFile zip, String path) throws Exception {
        try (var stream = zip.getInputStream(zip.getEntry(path))) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
