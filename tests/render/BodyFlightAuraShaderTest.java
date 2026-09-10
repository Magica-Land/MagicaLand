import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

/** 独立隐藏 GL 场景：合并剪影、前景遮挡、主深度与 Fabulous。 */
public final class BodyFlightAuraShaderTest {
    private static final int SIZE = 128;
    private static final float[] BODY = {.20f, .34f, .46f, 1}, WALL = {.21f, .42f, .12f, 1};
    private static final float[] MAGIC = {.78f, .30f, .88f, 1};
    private static int maskProgram, softenProgram, compositeProgram, solidProgram, combineProgram, checks;
    private static final String SOLID_VERTEX = """
            #version 150
            uniform float Depth;
            uniform vec4 Bounds;
            out vec2 texCoord;
            void main() {
                vec2 uv=vec2(gl_VertexID & 1, gl_VertexID >> 1);
                gl_Position=vec4(mix(Bounds.xy,Bounds.zw,uv),Depth*2.0-1.0,1.0);
                texCoord=uv;
            }
            """;

    public static void main(String[] args) throws Exception {
        if (!glfwInit()) throw new AssertionError("GLFW init");
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        long window = glfwCreateWindow(SIZE, SIZE, "Body flight aura regression", 0, 0);
        if (window == 0) throw new AssertionError("hidden GL context");
        try (ZipFile minecraft = new ZipFile(args[0])) {
            glfwMakeContextCurrent(window);
            GL.createCapabilities();
            glBindVertexArray(glGenVertexArrays());
            Path sources = Path.of(args[1]);
            String fog = read(minecraft, "assets/minecraft/shaders/include/fog.glsl")
                    .replaceAll("(?m)^#version[^\\r\\n]*", "");
            maskProgram = program(source(sources,"body_aura_mask.vsh",fog), source(sources,"body_aura_mask.fsh",fog));
            softenProgram = program(source(sources,"body_aura_screen.vsh",fog), source(sources,"body_aura_soften.fsh",fog));
            compositeProgram = program(source(sources,"body_aura_screen.vsh",fog), source(sources,"body_aura_composite.fsh",fog));
            solidProgram = program(SOLID_VERTEX, "#version 150\nuniform vec4 Tint; out vec4 fragColor; void main(){fragColor=Tint;}");
            combineProgram = program(SOLID_VERTEX, read(minecraft,"assets/minecraft/shaders/program/transparency.fsh"));
            int slopeProgram = program(source(sources,"body_aura_screen.vsh",fog),
                    "#version 150\nuniform vec4 Tint; out vec4 fragColor; void main(){fragColor=Tint;}");
            Target scene = target(), mask = target(), glow = target(), item = target(), empty = target(), output = target();
            int opaque = alphaTexture(255), transparent = alphaTexture(0);
            background(scene, false);
            silhouette(mask, scene, opaque, false, 1);
            check(near(pixel(mask,64,64)[3],1), "body mask opaque");
            check(near(pixel(mask,45,64)[3],1), "body silhouette reaches expected edge");
            check(near(pixel(mask,35,64)[3],0), "mask does not expand individual cubes");
            soften(glow, mask, scene, 0);
            float[] beforeOverlap = pixel(glow,64,64);
            check(beforeOverlap[3] > .033 && beforeOverlap[3] < .046, "stronger interior tint preserves body detail");
            check(pixel(glow,43,64)[3] > .025, "soft outer aura visible");
            check(pixel(glow,36,64)[3] < pixel(glow,41,64)[3], "halo fades outward");
            float alphaAtZero = pixel(glow,43,64)[3];
            soften(glow, mask, scene, 1.7f);
            check(Math.abs(alphaAtZero - pixel(glow,43,64)[3]) > .00001, "flow changes intensity with time");
            silhouette(mask, scene, opaque, true, 1);
            soften(glow, mask, scene, 0);
            check(close(beforeOverlap, pixel(glow,64,64)), "overlapping limb geometry does not stack brightness");

            composite(scene, glow, false);
            float[] litBody = pixel(scene,64,64);
            check(litBody[0] > BODY[0] && litBody[2] > BODY[2], "body stays visible under faint additive tint");
            check(near(depth(scene,64,64), .6f), "ordinary composite preserves body depth");
            check(near(depth(scene,43,64), .9f), "ordinary halo preserves background depth");
            Path png = Path.of(args[2]);
            Files.createDirectories(png.getParent());
            image(scene,png);

            background(scene, true);
            silhouette(mask, scene, opaque, false, 1);
            check(near(pixel(mask,77,64)[3], 0), "foreground wall prevents mask capture");
            soften(glow, mask, scene, 0);
            composite(scene, glow, false);
            check(close(pixel(scene,77,64), WALL), "wall occludes body and softened aura");
            check(near(depth(scene,77,64), .2f), "wall depth remains intact");

            background(scene, false);
            silhouette(mask, scene, transparent, false, 1);
            soften(glow, mask, scene, 0);
            check(near(pixel(glow,64,64)[3],0), "transparent source texture does not generate body aura");
            silhouette(mask, scene, opaque, false, .5f);
            soften(glow, mask, scene, 0);
            check(near(pixel(glow,64,64)[3], beforeOverlap[3] * .5f), "fade progress halves fill without a mid-fade brightness spike");

            silhouette(mask, scene, opaque, false, 1);
            soften(glow, mask, scene, 0);
            clear(item);
            bind(item); glClearDepth(.9); glClear(GL_DEPTH_BUFFER_BIT);
            composite(item, glow, true);
            check(pixel(item,64,64)[3] > .03f, "Fabulous keeps soft fill alpha instead of squaring it");
            check(depth(item,64,64) < .60001f, "Fabulous target receives actual glow depth");
            check(close(pixel(scene,64,64),BODY) && near(depth(scene,64,64),.6f), "Fabulous pass leaves opaque target unchanged");
            clear(empty);
            combine(scene,item,empty,output);
            check(pixel(output,64,64)[0] > BODY[0], "actual vanilla Fabulous compositor keeps body aura");
            check(pixel(output,43,64)[0] > .08f, "Fabulous retains soft halo outside silhouette");
            bind(scene); solid(.2f,WALL,.12f,-1, .5f,1);
            combine(scene,item,empty,output);
            check(close(pixel(output,77,64),WALL), "Fabulous compositor still honors foreground wall");
            Target high = target(3840,2160), scaledScene = target(1024,576), scaledMask = target(1024,576), scaledGlow = target(1024,576);
            clear(high); solid(.9f,new float[]{.08f,.10f,.13f,1},-1,-1,1,1);
            glUseProgram(slopeProgram); glUniform4fv(glGetUniformLocation(slopeProgram,"Tint"),BODY);
            quad(slopeProgram,-.3f,-.55f,.3f,.55f,-.05f,.6f,1);
            float preservedSlopeDepth=depth(high,1900,1050);
            glBindFramebuffer(GL_READ_FRAMEBUFFER,high.framebuffer); glBindFramebuffer(GL_DRAW_FRAMEBUFFER,scaledScene.framebuffer);
            glBlitFramebuffer(0,0,high.width,high.height,0,0,scaledScene.width,scaledScene.height,GL_DEPTH_BUFFER_BIT,GL_NEAREST);
            silhouetteSetup(scaledMask,scaledScene,opaque);
            quad(maskProgram,-.3f,-.55f,.3f,.55f,-.05f,.6f,1);
            for(int y=155;y<420;y+=29) for(int x=380;x<640;x+=13)
                check(pixel(scaledMask,x,y)[3] > .99f,"4k depth downsampling retains sloped body silhouette at "+x+","+y);
            soften(scaledGlow,scaledMask,scaledScene,0);
            for(int y=155;y<420;y+=29) for(int x=380;x<640;x+=13) {
                float tint=pixel(scaledGlow,x,y)[3];
                check(tint > .033f && tint < .046f,"sloped body tint remains soft and continuous at "+x+","+y);
            }
            long start=System.nanoTime();
            for(int i=0;i<12;i++) { soften(scaledGlow,scaledMask,scaledScene,i*.01f); composite(high,scaledGlow,false); }
            glFinish();
            System.out.println("GPU regression 4k target / 1024 mask average elapsed " + (System.nanoTime()-start)/12_000_000.0 + " ms (includes test setup per pass)");
            check(near(depth(high,1900,1050), preservedSlopeDepth),"4k composite leaves sloped main depth unchanged");
            check(glGetError()==GL_NO_ERROR,"no GPU errors");
            System.out.println("PASS body flight aura GPU: " + checks + " checks; " + glGetString(GL_RENDERER));
            System.out.println("PNG " + png.toAbsolutePath());
            for (Target target : new Target[]{scene,mask,glow,item,empty,output,high,scaledScene,scaledMask,scaledGlow}) {
                glDeleteFramebuffers(target.framebuffer); glDeleteTextures(target.color); glDeleteTextures(target.depth);
            }
            glDeleteTextures(opaque); glDeleteTextures(transparent);
            for (int shader : new int[]{maskProgram,softenProgram,compositeProgram,solidProgram,combineProgram,slopeProgram}) glDeleteProgram(shader);
        } finally { glfwDestroyWindow(window); glfwTerminate(); }
    }

    private record Target(int framebuffer,int color,int depth,int width,int height) {}
    private static Target target() { return target(SIZE,SIZE); }
    private static Target target(int width,int height) {
        int fbo=glGenFramebuffers(); glBindFramebuffer(GL_FRAMEBUFFER,fbo);
        int color=texture(GL_RGBA32F,GL_RGBA,width,height), depth=texture(GL_DEPTH_COMPONENT32F,GL_DEPTH_COMPONENT,width,height);
        glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,color,0);
        glFramebufferTexture2D(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,depth,0);
        check(glCheckFramebufferStatus(GL_FRAMEBUFFER)==GL_FRAMEBUFFER_COMPLETE,"framebuffer complete");
        return new Target(fbo,color,depth,width,height);
    }
    private static int texture(int internal,int format,int width,int height) {
        int id=glGenTextures(); glBindTexture(GL_TEXTURE_2D,id);
        glTexImage2D(GL_TEXTURE_2D,0,internal,width,height,0,format,GL_FLOAT,(ByteBuffer)null);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
        return id;
    }
    private static int alphaTexture(int alpha) {
        int id=glGenTextures(); glBindTexture(GL_TEXTURE_2D,id);
        ByteBuffer bytes=org.lwjgl.BufferUtils.createByteBuffer(4).put((byte)255).put((byte)255).put((byte)255).put((byte)alpha);
        bytes.flip(); glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,1,1,0,GL_RGBA,GL_UNSIGNED_BYTE,bytes);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST); return id;
    }
    private static void bind(Target target) { glBindFramebuffer(GL_FRAMEBUFFER,target.framebuffer); glViewport(0,0,target.width,target.height); }
    private static void clear(Target target) {
        bind(target); glDepthMask(true); glColorMask(true,true,true,true); glClearColor(0,0,0,0); glClearDepth(1);
        glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
    }
    private static void background(Target scene,boolean wall) {
        clear(scene); solid(.9f,new float[]{.08f,.10f,.13f,1},-1,-1,1,1);
        solid(.6f,BODY,-.30f,-.55f,.30f,.55f);
        if(wall) solid(.2f,WALL,.12f,-1,.5f,1);
    }
    private static void solid(float z,float[] tint,float x0,float y0,float x1,float y1) {
        glUseProgram(solidProgram); glEnable(GL_DEPTH_TEST); glDepthFunc(GL_LEQUAL); glDepthMask(true); glDisable(GL_BLEND);
        glUniform1f(glGetUniformLocation(solidProgram,"Depth"),z);
        glUniform4f(glGetUniformLocation(solidProgram,"Bounds"),x0,y0,x1,y1);
        glUniform4fv(glGetUniformLocation(solidProgram,"Tint"),tint); glDrawArrays(GL_TRIANGLE_STRIP,0,4);
    }
    private static void sampler(int program,String name,int unit,int texture) {
        glActiveTexture(GL_TEXTURE0+unit); glBindTexture(GL_TEXTURE_2D,texture);
        glUniform1i(glGetUniformLocation(program,name),unit);
    }
    private static void silhouette(Target mask,Target scene,int texture,boolean overlap,float progress) {
        silhouetteSetup(mask,scene,texture);
        quad(maskProgram,-.3f,-.55f,.3f,.55f,.2f,progress);
        if(overlap) quad(maskProgram,-.1f,-.3f,.1f,.3f,.2f,progress);
    }
    private static void silhouetteSetup(Target mask,Target scene,int texture) {
        clear(mask); glUseProgram(maskProgram); glEnable(GL_DEPTH_TEST); glDepthFunc(GL_LEQUAL); glDisable(GL_BLEND);
        sampler(maskProgram,"Sampler0",0,texture); sampler(maskProgram,"Sampler1",1,scene.depth);
        glUniform2f(glGetUniformLocation(maskProgram,"MaskSize"),mask.width,mask.height);
        glUniform1f(glGetUniformLocation(maskProgram,"FogStart"),10); glUniform1f(glGetUniformLocation(maskProgram,"FogEnd"),100);
        glUniformMatrix4fv(glGetUniformLocation(maskProgram,"ModelViewMat"),false,identity());
        glUniformMatrix4fv(glGetUniformLocation(maskProgram,"ProjMat"),false,identity());
        glUniformMatrix3fv(glGetUniformLocation(maskProgram,"IViewRotMat"),false,new float[]{1,0,0,0,1,0,0,0,1});
        glUniform1i(glGetUniformLocation(maskProgram,"FogShape"),0);
    }
    private static float[] identity() { return new float[]{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1}; }
    private static void quad(int program,float x0,float y0,float x1,float y1,float z,float alpha) {
        quad(program,x0,y0,x1,y1,z,z,alpha);
    }
    private static void quad(int program,float x0,float y0,float x1,float y1,float z0,float z1,float alpha) {
        float[] data={x0,y0,z0,0,0, x1,y0,z1,1,0, x0,y1,z0,0,1, x1,y1,z1,1,1};
        int buffer=glGenBuffers(); glBindBuffer(GL_ARRAY_BUFFER,buffer); glBufferData(GL_ARRAY_BUFFER,data,GL_STREAM_DRAW);
        int p=glGetAttribLocation(program,"Position"), uv=glGetAttribLocation(program,"UV0"), color=glGetAttribLocation(program,"Color");
        glEnableVertexAttribArray(p); glVertexAttribPointer(p,3,GL_FLOAT,false,20,0);
        if(uv>=0) { glEnableVertexAttribArray(uv); glVertexAttribPointer(uv,2,GL_FLOAT,false,20,12); }
        if(color>=0) { glDisableVertexAttribArray(color); glVertexAttrib4f(color,MAGIC[0],MAGIC[1],MAGIC[2],alpha); }
        glDrawArrays(GL_TRIANGLE_STRIP,0,4);
        glDisableVertexAttribArray(p); if(uv>=0)glDisableVertexAttribArray(uv); glDeleteBuffers(buffer);
    }
    private static void soften(Target glow,Target mask,Target scene,float time) {
        clear(glow); glUseProgram(softenProgram); glEnable(GL_DEPTH_TEST); glDepthFunc(GL_ALWAYS); glDisable(GL_BLEND);
        sampler(softenProgram,"Sampler0",0,mask.color); sampler(softenProgram,"Sampler1",1,mask.depth); sampler(softenProgram,"Sampler2",2,scene.depth);
        glUniform2f(glGetUniformLocation(softenProgram,"MaskSize"),mask.width,mask.height);
        glUniform2f(glGetUniformLocation(softenProgram,"ProjectionDepth"),-1,-.1f);
        glUniform1f(glGetUniformLocation(softenProgram,"ProjectionScale"),1);
        glUniform1f(glGetUniformLocation(softenProgram,"FlowTime"),time);
        quad(softenProgram,-1,-1,1,1,0,1);
    }
    private static void composite(Target scene,Target glow,boolean writeDepth) {
        bind(scene); glUseProgram(compositeProgram); glEnable(GL_DEPTH_TEST); glDepthFunc(GL_LEQUAL); glDepthMask(writeDepth);
        glEnable(GL_BLEND); glBlendEquation(GL_FUNC_ADD); glBlendFuncSeparate(GL_SRC_ALPHA,GL_ONE,GL_ONE,GL_ONE_MINUS_SRC_ALPHA);
        sampler(compositeProgram,"Sampler0",0,glow.color); sampler(compositeProgram,"Sampler1",1,glow.depth);
        quad(compositeProgram,-1,-1,1,1,0,1);
    }
    private static void combine(Target scene,Target item,Target empty,Target output) {
        bind(output); glUseProgram(combineProgram); glDisable(GL_DEPTH_TEST); glDisable(GL_BLEND);
        String[] names={"Diffuse","Translucent","ItemEntity","Particles","Weather","Clouds"};
        Target[] targets={scene,empty,item,empty,empty,empty};
        for(int i=0;i<names.length;i++) { sampler(combineProgram,names[i]+"Sampler",i*2,targets[i].color); sampler(combineProgram,names[i]+"DepthSampler",i*2+1,targets[i].depth); }
        glUniform1f(glGetUniformLocation(combineProgram,"Depth"),.5f);
        glUniform4f(glGetUniformLocation(combineProgram,"Bounds"),-1,-1,1,1); glDrawArrays(GL_TRIANGLE_STRIP,0,4);
    }
    private static float[] pixel(Target target,int x,int y) { bind(target); float[] value=new float[4]; glReadPixels(x,y,1,1,GL_RGBA,GL_FLOAT,value); return value; }
    private static float depth(Target target,int x,int y) { bind(target); float[] value=new float[1]; glReadPixels(x,y,1,1,GL_DEPTH_COMPONENT,GL_FLOAT,value); return value[0]; }
    private static void image(Target target,Path path)throws Exception {
        bind(target); ByteBuffer bytes=org.lwjgl.BufferUtils.createByteBuffer(SIZE*SIZE*4); glReadPixels(0,0,SIZE,SIZE,GL_RGBA,GL_UNSIGNED_BYTE,bytes);
        BufferedImage image=new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++){int i=(y*SIZE+x)*4; image.setRGB(x,SIZE-y-1,0xFF000000|(bytes.get(i)&255)<<16|(bytes.get(i+1)&255)<<8|bytes.get(i+2)&255);}
        ImageIO.write(image,"png",path.toFile());
    }
    private static int program(String vertex,String fragment) {
        int program=glCreateProgram();
        for(int type:new int[]{GL_VERTEX_SHADER,GL_FRAGMENT_SHADER}){int shader=glCreateShader(type);glShaderSource(shader,type==GL_VERTEX_SHADER?vertex:fragment);glCompileShader(shader);check(glGetShaderi(shader,GL_COMPILE_STATUS)==GL_TRUE,glGetShaderInfoLog(shader));glAttachShader(program,shader);glDeleteShader(shader);}
        glLinkProgram(program);check(glGetProgrami(program,GL_LINK_STATUS)==GL_TRUE,glGetProgramInfoLog(program));return program;
    }
    private static String source(Path root,String file,String fog)throws Exception{return Files.readString(root.resolve(file)).replace("#moj_import <fog.glsl>",fog);}
    private static String read(ZipFile zip,String path)throws Exception{try(var stream=zip.getInputStream(zip.getEntry(path))){return new String(stream.readAllBytes(),StandardCharsets.UTF_8);}}
    private static boolean near(float a,float b){return Math.abs(a-b)<.0001f;}
    private static boolean close(float[] a,float[] b){for(int i=0;i<a.length;i++)if(!near(a[i],b[i]))return false;return true;}
    private static void check(boolean pass,String message){checks++;if(!pass)throw new AssertionError(message);}
}
