import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.lwjgl.opengl.GL;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

/** 云层顺序回归：生产事件字节码、实际魔法 shader 与原版透明层合成。 */
public final class MagicCloudOrderingTest {
    private static final int SIZE = 32;
    private static final float[] SKY = {.04f, .08f, .12f, 1}, CLOUD = {.4f, .4f, .4f, .8f};
    private static final float[] WALL = {.2f, .1f, .05f, 1};
    private static int solid, horn, body, combine, checks, bodySourceAlpha, bodyDestAlpha;
    private static final String VERTEX = """
            #version 150
            uniform float Depth;
            uniform vec4 Tint;
            out vec4 vertexColor;
            out float vertexDistance;
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
                texCoord = p * .5 + .5;
                texCoord0 = vec2(.5, .35);
                auraCoord = texCoord0;
                vertexColor = Tint;
                vertexDistance = 0;
                viewPosition = vec3(0,0,-1);
                viewNormal = vec3(0,0,1);
                flowTime = 0;
                effect = 0;
            }
            """;

    public static void main(String[] args) throws Exception {
        Path classes = Path.of(args[2]);
        route(classes, "HornAuraPass");
        route(classes, "BodyFlightAura");
        if (!glfwInit()) throw new AssertionError("GLFW");
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        long window = glfwCreateWindow(SIZE, SIZE, "Cloud magic depth regression", 0, 0);
        if (window == 0) throw new AssertionError("hidden context");
        try (ZipFile minecraft = new ZipFile(args[0])) {
            glfwMakeContextCurrent(window); GL.createCapabilities();
            glBindVertexArray(glGenVertexArrays()); glViewport(0, 0, SIZE, SIZE);
            String fog = read(minecraft, "assets/minecraft/shaders/include/fog.glsl")
                    .replaceAll("(?m)^#version[^\\r\\n]*", "");
            Path shaders = Path.of(args[1]);
            var blend = com.google.gson.JsonParser.parseString(Files.readString(shaders.resolve("body_aura_composite.json")))
                    .getAsJsonObject().getAsJsonObject("blend");
            bodySourceAlpha = factor(blend.get(blend.has("srcalpha") ? "srcalpha" : "srcrgb").getAsString());
            bodyDestAlpha = factor(blend.get(blend.has("dstalpha") ? "dstalpha" : "dstrgb").getAsString());
            solid = program(VERTEX, "#version 150\nin vec4 vertexColor;out vec4 fragColor;void main(){fragColor=vertexColor;}");
            horn = program(VERTEX, Files.readString(shaders.resolve("horn_aura.fsh")).replace("#moj_import <fog.glsl>", fog));
            body = program(Files.readString(shaders.resolve("body_aura_screen.vsh")), Files.readString(shaders.resolve("body_aura_composite.fsh")));
            combine = program(VERTEX, read(minecraft, "assets/minecraft/shaders/program/transparency.fsh"));
            Target scene = target(), item = target(true), clouds = target(), empty = target(), output = target(), bodyGlow = target(true);
            clear(empty); clear(bodyGlow);
            draw(solid, .3f, new float[] {.2f, .08f, .65f, .04f}, false, true, false);
            for (int effect : new int[] {horn, body}) {
                clear(scene); draw(solid, .9f, SKY, false, true, false);
                magic(effect, bodyGlow, false);
                draw(solid, .6f, CLOUD, true, true, false);
                float[] old = pixel(scene);

                clear(scene); draw(solid, .9f, SKY, false, true, false);
                draw(solid, .6f, CLOUD, true, true, false);
                float[] cloudBase = pixel(scene);
                magic(effect, bodyGlow, false);
                float[] fixed = pixel(scene);
                check(fixed[2] > old[2] + .01f, "rear cloud no longer attenuates foreground magic: " + effect);
                check(fixed[2] > cloudBase[2] + .01f, "foreground magic visibly adds over cloud: " + effect);
                check(near(depth(scene), .6f), "ordinary glow preserves cloud scene depth");

                clear(scene); draw(solid, .9f, SKY, false, true, false);
                draw(solid, .15f, CLOUD, true, true, false);
                float[] frontCloud = pixel(scene);
                magic(effect, bodyGlow, false);
                check(close(pixel(scene), frontCloud), "genuinely foreground cloud still occludes ordinary magic");
                check(near(depth(scene), .15f), "foreground cloud depth retained");

                clear(scene); draw(solid, .9f, SKY, false, true, false);
                draw(solid, .1f, WALL, false, true, false);
                draw(solid, .6f, CLOUD, true, true, false);
                magic(effect, bodyGlow, false);
                check(close(pixel(scene), WALL), "foreground wall still occludes late magic");

                clear(scene); draw(solid, .9f, SKY, false, true, false);
                if (effect == body) {
                    int savedSource = bodySourceAlpha, savedDestination = bodyDestAlpha;
                    bodySourceAlpha = GL_SRC_ALPHA; bodyDestAlpha = GL_ONE;
                    clear(item); magic(body, bodyGlow, true);
                    float[] lost = pixel(item);
                    check(lost[2] > .01f && lost[3] == 0, "old squared alpha reproduces nonzero RGB with zero 8-bit coverage");
                    composite(scene, item, empty, empty, output);
                    check(close(pixel(output), SKY), "actual vanilla compositor drops old zero-alpha glow despite its RGB");
                    bodySourceAlpha = savedSource; bodyDestAlpha = savedDestination;
                }
                clear(item); magic(effect, bodyGlow, true);
                check(pixel(item)[3] > .001f, "faint magic retains coverage in actual 8-bit entity format");
                check(near(depth(item), .3f), "Fabulous magic supplies sorting depth");
                clear(clouds); draw(solid, .6f, CLOUD, true, true, false);
                composite(scene, empty, clouds, empty, output);
                float[] fabulousBase = pixel(output);
                composite(scene, item, clouds, empty, output);
                check(pixel(output)[2] > fabulousBase[2] + .005f, "Fabulous actual compositor sorts rear cloud behind faint magic");
                clear(clouds); draw(solid, .15f, new float[] {.4f, .4f, .4f, 1}, false, true, false);
                composite(scene, item, clouds, empty, output);
                check(close(pixel(output), new float[] {.4f, .4f, .4f, 1}), "Fabulous foreground opaque cloud hides magic");
                bind(scene); draw(solid, .1f, WALL, false, true, false);
                composite(scene, item, clouds, empty, output);
                check(close(pixel(output), WALL), "Fabulous foreground wall still wins");
            }
            check(glGetError() == GL_NO_ERROR, "GL error-free");
            System.out.println("PASS magic cloud ordering: " + checks + " checks; " + glGetString(GL_RENDERER));
            for (Target target : new Target[] {scene,item,clouds,empty,output,bodyGlow}) {
                glDeleteFramebuffers(target.fbo); glDeleteTextures(target.color); glDeleteTextures(target.depth);
            }
            glDeleteProgram(solid); glDeleteProgram(horn); glDeleteProgram(body); glDeleteProgram(combine);
        } finally { glfwDestroyWindow(window); glfwTerminate(); }
    }

    private static void route(Path classes, String name) throws Exception {
        ClassNode type = new ClassNode();
        new ClassReader(Files.readAllBytes(classes.resolve("top/csituka/magicaland/client/render/" + name + ".class"))).accept(type, 0);
        MethodNode init = type.methods.stream().filter(m -> m.name.equals("init")).findFirst().orElseThrow();
        for (String event : new String[] {"AFTER_TRANSLUCENT", "LAST"}) {
            String lambda = null;
            for (AbstractInsnNode node : init.instructions) {
                if (!(node instanceof FieldInsnNode field) || !field.name.equals(event)) continue;
                for (AbstractInsnNode next = node.getNext(); next != null; next = next.getNext()) {
                    if (next instanceof InvokeDynamicInsnNode dynamic) {
                        for (Object argument : dynamic.bsmArgs) if (argument instanceof Handle handle) lambda = handle.getName();
                        break;
                    }
                }
            }
            check(lambda != null, name + " registers " + event);
            String lambdaName = lambda;
            MethodNode callback = type.methods.stream().filter(m -> m.name.equals(lambdaName)).findFirst().orElseThrow();
            boolean guarded = false, mode = false, drawn = false;
            for (AbstractInsnNode node : callback.instructions) {
                if (node instanceof MethodInsnNode call && call.name.equals("advancedTranslucency")) {
                    AbstractInsnNode next = executable(node.getNext());
                    int skip = event.equals("LAST") ? Opcodes.IFNE : Opcodes.IFEQ;
                    guarded = next instanceof JumpInsnNode && next.getOpcode() == skip;
                }
                if (node instanceof MethodInsnNode call && call.name.equals("drawWorld")) {
                    AbstractInsnNode previous = node.getPrevious();
                    while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
                    mode = previous != null && previous.getOpcode() == (event.equals("LAST") ? Opcodes.ICONST_0 : Opcodes.ICONST_1);
                    drawn = true;
                }
            }
            check(guarded && drawn && mode, name + " keeps Fabulous early and ordinary after clouds: " + event);
        }
    }
    private static AbstractInsnNode executable(AbstractInsnNode node) { while (node != null && node.getOpcode() < 0) node = node.getNext(); return node; }
    private record Target(int fbo, int color, int depth) {}
    private static Target target() { return target(false); }
    private static Target target(boolean eightBit) {
        int fbo = glGenFramebuffers(); glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        int color = texture(eightBit ? GL_RGBA8 : GL_RGBA32F, GL_RGBA), depth = texture(GL_DEPTH_COMPONENT32F, GL_DEPTH_COMPONENT);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, color, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depth, 0);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) throw new AssertionError("FBO");
        return new Target(fbo, color, depth);
    }
    private static int texture(int internal, int format) {
        int texture = glGenTextures(); glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, internal, SIZE, SIZE, 0, format, GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST); glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE); glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        return texture;
    }
    private static void bind(Target target) { glBindFramebuffer(GL_FRAMEBUFFER, target.fbo); }
    private static void clear(Target target) { bind(target); glDepthMask(true); glClearColor(0,0,0,0); glClearDepth(1); glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); }
    private static void draw(int program, float depth, float[] tint, boolean blend, boolean write, boolean additive) {
        glUseProgram(program); glEnable(GL_DEPTH_TEST); glDepthFunc(GL_LEQUAL); glDepthMask(write);
        if (blend) glEnable(GL_BLEND); else glDisable(GL_BLEND);
        glBlendFuncSeparate(GL_SRC_ALPHA, additive ? GL_ONE : GL_ONE_MINUS_SRC_ALPHA, GL_ONE, additive ? GL_ONE : GL_ONE_MINUS_SRC_ALPHA);
        glUniform1f(glGetUniformLocation(program,"Depth"),depth); glUniform4fv(glGetUniformLocation(program,"Tint"),tint);
        glUniform4f(glGetUniformLocation(program,"ColorModulator"),1,1,1,1);
        glUniform1f(glGetUniformLocation(program,"FogStart"),0); glUniform1f(glGetUniformLocation(program,"FogEnd"),100);
        glDrawArrays(GL_TRIANGLE_STRIP,0,4);
    }
    private static void magic(int program, Target glow, boolean write) {
        if (program == horn) draw(horn,.3f,new float[] {.3f,.1f,.8f,.55f},true,write,true);
        else {
            glUseProgram(body); glEnable(GL_DEPTH_TEST); glDepthFunc(GL_LEQUAL); glDepthMask(write);
            glEnable(GL_BLEND); glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE, bodySourceAlpha, bodyDestAlpha);
            glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D,glow.color); glUniform1i(glGetUniformLocation(body,"Sampler0"),0);
            glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D,glow.depth); glUniform1i(glGetUniformLocation(body,"Sampler1"),1);
            int buffer = glGenBuffers(); glBindBuffer(GL_ARRAY_BUFFER,buffer);
            glBufferData(GL_ARRAY_BUFFER,new float[] {-1,-1,0,0,0, 1,-1,0,1,0, -1,1,0,0,1, 1,1,0,1,1},GL_STREAM_DRAW);
            int position = glGetAttribLocation(body,"Position"), uv = glGetAttribLocation(body,"UV0");
            glEnableVertexAttribArray(position); glVertexAttribPointer(position,3,GL_FLOAT,false,20,0);
            glEnableVertexAttribArray(uv); glVertexAttribPointer(uv,2,GL_FLOAT,false,20,12);
            glDrawArrays(GL_TRIANGLE_STRIP,0,4); glDisableVertexAttribArray(position); glDisableVertexAttribArray(uv); glDeleteBuffers(buffer);
            glActiveTexture(GL_TEXTURE0);
        }
    }
    private static void composite(Target scene, Target item, Target clouds, Target empty, Target output) {
        bind(output); glUseProgram(combine); glDisable(GL_DEPTH_TEST); glDisable(GL_BLEND);
        String[] names = {"Diffuse","Translucent","ItemEntity","Particles","Weather","Clouds"};
        Target[] targets = {scene,empty,item,empty,empty,clouds};
        for (int i=0;i<names.length;i++) {
            glActiveTexture(GL_TEXTURE0+i*2); glBindTexture(GL_TEXTURE_2D,targets[i].color); glUniform1i(glGetUniformLocation(combine,names[i]+"Sampler"),i*2);
            glActiveTexture(GL_TEXTURE0+i*2+1); glBindTexture(GL_TEXTURE_2D,targets[i].depth); glUniform1i(glGetUniformLocation(combine,names[i]+"DepthSampler"),i*2+1);
        }
        glUniform1f(glGetUniformLocation(combine,"Depth"),.5f); glDrawArrays(GL_TRIANGLE_STRIP,0,4); glActiveTexture(GL_TEXTURE0);
    }
    private static float[] pixel(Target target) { bind(target); float[] result=new float[4]; glReadPixels(SIZE/2,SIZE/2,1,1,GL_RGBA,GL_FLOAT,result); return result; }
    private static float depth(Target target) { bind(target); float[] result=new float[1]; glReadPixels(SIZE/2,SIZE/2,1,1,GL_DEPTH_COMPONENT,GL_FLOAT,result); return result[0]; }
    private static boolean near(float a,float b) { return Math.abs(a-b)<.0001f; }
    private static boolean close(float[] a,float[] b) { for(int i=0;i<a.length;i++) if(!near(a[i],b[i])) return false; return true; }
    private static int program(String vertex, String fragment) {
        int program=glCreateProgram();
        for (int type : new int[] {GL_VERTEX_SHADER,GL_FRAGMENT_SHADER}) {
            int shader=glCreateShader(type); glShaderSource(shader,type==GL_VERTEX_SHADER?vertex:fragment); glCompileShader(shader);
            if(glGetShaderi(shader,GL_COMPILE_STATUS)==GL_FALSE) throw new AssertionError(glGetShaderInfoLog(shader));
            glAttachShader(program,shader); glDeleteShader(shader);
        }
        glLinkProgram(program); if(glGetProgrami(program,GL_LINK_STATUS)==GL_FALSE)throw new AssertionError(glGetProgramInfoLog(program)); return program;
    }
    private static String read(ZipFile zip,String path) throws Exception { try(var stream=zip.getInputStream(zip.getEntry(path))){return new String(stream.readAllBytes(),StandardCharsets.UTF_8);} }
    private static int factor(String name) { return switch(name) { case "srcalpha" -> GL_SRC_ALPHA; case "one" -> GL_ONE; case "1-srcalpha" -> GL_ONE_MINUS_SRC_ALPHA; default -> throw new AssertionError("unhandled blend factor: "+name); }; }
    private static void check(boolean valid,String message) { checks++; if(!valid)throw new AssertionError(message); }
}
