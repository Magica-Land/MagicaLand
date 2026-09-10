import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.resource.*;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import top.csituka.magicaland.client.render.BodyFlightAura;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.ZipFile;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

public class BodyAuraIntegrationTest {
    static int checks;
    static final int S=128;
    static final VertexConsumer VOID = new VertexConsumer() {
        public VertexConsumer vertex(double x,double y,double z){return this;}
        public VertexConsumer color(int r,int g,int b,int a){return this;}
        public VertexConsumer texture(float u,float v){return this;}
        public VertexConsumer overlay(int u,int v){return this;}
        public VertexConsumer light(int u,int v){return this;}
        public VertexConsumer normal(float x,float y,float z){return this;}
        public void next(){} public void fixedColor(int r,int g,int b,int a){} public void unfixColor(){}
    };
    public static void main(String[] args) throws Exception {
        if(!glfwInit())throw new AssertionError("GLFW");
        glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE); glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3); glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);
        long window=glfwCreateWindow(S,S,"Actual aura pipeline test",0,0);
        if(window==0)throw new AssertionError("window");
        glfwMakeContextCurrent(window); GL.createCapabilities();
        RenderSystem.initRenderThread(); RenderSystem.beginInitialization();
        System.out.println("GPU "+glGetString(GL_RENDERER));
        try(ZipFile jar=new ZipFile(args[0])) {
            Path shaders=Path.of(args[1]);
            ResourcePack pack=(ResourcePack)Proxy.newProxyInstance(ResourcePack.class.getClassLoader(),new Class[]{ResourcePack.class},
                    (proxy,method,params)->method.getName().equals("getName")?"aura-harness":null);
            ResourceFactory factory=id->{
                try {
                    String path=id.getPath();
                    byte[] bytes;
                    Path local=shaders.resolve(path.substring(path.lastIndexOf('/')+1));
                    if(Files.exists(local))bytes=Files.readAllBytes(local);
                    else {var entry=jar.getEntry("assets/minecraft/"+path);if(entry==null)return Optional.empty(); try(var in=jar.getInputStream(entry)){bytes=in.readAllBytes();}}
                    if(path.endsWith(".json"))bytes=new String(bytes,java.nio.charset.StandardCharsets.UTF_8).replace("magicaland:","").getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    byte[] content=bytes;
                    return Optional.of(new Resource(pack,()->new ByteArrayInputStream(content)));
                }catch(IOException ex){throw new RuntimeException(ex);}
            };
            ShaderProgram mask=new ShaderProgram(factory,"body_aura_mask",VertexFormats.POSITION_COLOR_TEXTURE);
            ShaderProgram soften=new ShaderProgram(factory,"body_aura_soften",VertexFormats.POSITION_COLOR_TEXTURE);
            ShaderProgram composite=new ShaderProgram(factory,"body_aura_composite",VertexFormats.POSITION_COLOR_TEXTURE);
            ShaderProgram blit=new ShaderProgram(factory,"blit_screen",VertexFormats.POSITION);
            ShaderProgram ordinary=new ShaderProgram(factory,"position_color",VertexFormats.POSITION_COLOR);
            ShaderProgram vignette=new ShaderProgram(factory,"position_tex",VertexFormats.POSITION_TEXTURE);
            ResourceManager resources=(ResourceManager)Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),new Class[]{ResourceManager.class},
                    (proxy,method,params)->switch(method.getName()) {
                        case "getResource" -> factory.getResource((Identifier)params[0]);
                        case "getResourceOrThrow" -> factory.getResourceOrThrow((Identifier)params[0]);
                        default -> throw new AssertionError("Unexpected resource method: "+method);
                    });
            MinecraftClient client=MinecraftClient.getInstance();
            client.gameRenderer.blitScreenProgram=blit;
            client.framebuffer=new SimpleFramebuffer(S,S,true,false);
            client.worldRenderer.entity=new SimpleFramebuffer(S,S,true,false);
            RenderSystem.activeTexture(GL_TEXTURE0);RenderSystem.bindTexture(client.worldRenderer.entity.getColorAttachment());
            require(glGetTexLevelParameteri(GL_TEXTURE_2D,0,GL_TEXTURE_ALPHA_SIZE)==8,"entity target uses actual 8-bit alpha storage");
            Framebuffer presented=new SimpleFramebuffer(S,S,true,false);
            Framebuffer emptyLayer=new SimpleFramebuffer(S,S,true,false);
            Framebuffer composed=new SimpleFramebuffer(S,S,true,false);
            PostEffectPass transparency=new PostEffectPass(resources,"transparency",client.framebuffer,composed);
            transparency.addAuxTarget("DiffuseDepthSampler",client.framebuffer::getDepthAttachment,S,S);
            for(String layer:List.of("Translucent","ItemEntity","Particles","Clouds","Weather")) {
                Framebuffer target=layer.equals("ItemEntity")?client.worldRenderer.entity:emptyLayer;
                transparency.addAuxTarget(layer+"Sampler",target::getColorAttachment,S,S);
                transparency.addAuxTarget(layer+"DepthSampler",target::getDepthAttachment,S,S);
            }
            final int white=GlStateManager._genTexture();
            RenderSystem.activeTexture(GL_TEXTURE0); RenderSystem.bindTexture(white);
            ByteBuffer pixel=org.lwjgl.BufferUtils.createByteBuffer(4).put(new byte[]{-1,-1,-1,-1}); pixel.flip();
            glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,1,1,0,GL_RGBA,GL_UNSIGNED_BYTE,pixel);
            GlStateManager._texParameter(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);
            GlStateManager._texParameter(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
            client.textures.texture=new AbstractTexture(){ public void load(ResourceManager manager){} public int getGlId(){return white;} };
            var vignetteImage=javax.imageio.ImageIO.read(jar.getInputStream(jar.getEntry("assets/minecraft/textures/misc/vignette.png")));
            ByteBuffer vignetteBytes=org.lwjgl.BufferUtils.createByteBuffer(vignetteImage.getWidth()*vignetteImage.getHeight()*4);
            for(int y=0;y<vignetteImage.getHeight();y++)for(int x=0;x<vignetteImage.getWidth();x++){
                int c=vignetteImage.getRGB(x,y);vignetteBytes.put((byte)(c>>16)).put((byte)(c>>8)).put((byte)c).put((byte)(c>>24));
            }
            vignetteBytes.flip();int vignetteTexture=GlStateManager._genTexture();RenderSystem.bindTexture(vignetteTexture);
            glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,vignetteImage.getWidth(),vignetteImage.getHeight(),0,GL_RGBA,GL_UNSIGNED_BYTE,vignetteBytes);
            GlStateManager._texParameter(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);GlStateManager._texParameter(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
            set("maskShader",mask);set("softenShader",soften);set("compositeShader",composite);
            RenderSystem.setProjectionMatrix(new Matrix4f(),VertexSorter.BY_DISTANCE);
            RenderSystem.getModelViewStack().loadIdentity(); RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderFogStart(10); RenderSystem.setShaderFogEnd(100);
            String[] cases={"first allocation","reused buffers","effect ended","downsampled resize",
                    "Fabulous target","Fabulous reused","Fabulous effect ended","return to regular","null cache"};
            int[][] sizes={{128,128},{128,128},{128,128},{1152,648},{256,144},{256,144},{256,144},{128,128},{128,128}};
            for(int frame=0;frame<cases.length;frame++) {
                int width=sizes[frame][0],height=sizes[frame][1];
                boolean active=frame!=2&&frame!=6, fabulous=frame>=4&&frame<=6, nullCache=frame==8;
                client.framebuffer.resize(width,height,false);
                client.worldRenderer.entity.resize(width,height,false);
                presented.resize(width,height,false);
                emptyLayer.resize(width,height,false);composed.resize(width,height,false);
                RenderSystem.setProjectionMatrix(new Matrix4f(),VertexSorter.BY_DISTANCE);
                RenderSystem.getModelViewStack().loadIdentity();RenderSystem.applyModelViewMatrix();
                RenderSystem.depthMask(true);RenderSystem.colorMask(true,true,true,true);RenderSystem.clearDepth(1);RenderSystem.disableScissor();
                call("clear"); set("accepting",true);
                client.framebuffer.setClearColor(.2f,.35f,.5f,1); client.framebuffer.clear(false);
                client.worldRenderer.entity.setClearColor(0,0,0,0);client.worldRenderer.entity.clear(false);
                emptyLayer.setClearColor(0,0,0,0);emptyLayer.clear(false);
                client.worldRenderer.entity.copyDepthFrom(client.framebuffer);client.framebuffer.beginWrite(true);
                // Vanilla world draws leave a standard enabled shader blend cached before the HUD.
                new GlBlendState().enable();
                RenderSystem.setShader(()->vignette);vignette.bind();
                require(!GlBlendState.activeBlendState.isBlendDisabled(),"standard enabled cache baseline");
                RenderSystem.disableDepthTest();RenderSystem.depthMask(false);
                RenderSystem.activeTexture(GL_TEXTURE9);RenderSystem.bindTexture(white);
                if(nullCache)GlBlendState.activeBlendState=null;
                GlBlendState previousBlendCache=GlBlendState.activeBlendState;
                int beforeProgram=glGetInteger(GL_CURRENT_PROGRAM), beforeFbo=glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
                int beforeRead=glGetInteger(GL_READ_FRAMEBUFFER_BINDING),beforeTexture=glGetInteger(GL_ACTIVE_TEXTURE);
                int[] beforeViewport=new int[4];glGetIntegerv(GL_VIEWPORT,beforeViewport);
                var capture=BodyFlightAura.begin(1,0xDD66EE,frame*5);
                var consumer=capture.wrap(VOID,new Identifier("test","white"));
                float[][] corners={{-.3f,-.4f},{.3f,-.4f},{.3f,.4f},{-.3f,.4f}};
                for(float[] corner:corners)consumer.vertex(corner[0],corner[1],0).color(255,255,255,255).texture(.5f,.5f).next();
                capture.finish(); set("accepting",false);
                if(!active)call("clear");
                System.out.println("frame "+frame+" before "+Arrays.toString(read(client.framebuffer,5,5))+" program="+beforeProgram);
                client.framebuffer.beginWrite(true);
                Method draw=BodyFlightAura.class.getDeclaredMethod("draw",boolean.class);draw.setAccessible(true);draw.invoke(null,fabulous);
                require(GlBlendState.activeBlendState==previousBlendCache,"exact shader blend cache identity restored");
                int[] afterViewport=new int[4];glGetIntegerv(GL_VIEWPORT,afterViewport);
                require(Arrays.equals(beforeViewport,afterViewport),"viewport restored");
                require(glGetInteger(GL_READ_FRAMEBUFFER_BINDING)==beforeRead,"read framebuffer restored");
                require(glGetInteger(GL_ACTIVE_TEXTURE)==beforeTexture,"active texture unit restored");
                int error=glGetError();
                require(error==GL_NO_ERROR,"no GL error in draw");
                require(glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)==beforeFbo,"framebuffer restored");
                require(glGetInteger(GL_CURRENT_PROGRAM)==beforeProgram,"GL program restored");
                require(RenderSystem.getShader()==vignette,"RenderSystem shader restored");
                require(glGetInteger(GL_TEXTURE_BINDING_2D)==white,"active unit texture restored");
                System.out.println("frame "+frame+" after "+Arrays.toString(read(client.framebuffer,5,5))+" center="+Arrays.toString(read(client.framebuffer,width/2,height/2))+" GL="+error+" blend="+glIsEnabled(GL_BLEND));
                float[] background=read(client.framebuffer,5,5);
                require(background[0]>.18f&&background[1]>.33f&&background[2]>.48f,"colored frame survives aura");
                float[] worldCenter=read(client.framebuffer,width/2,height/2);
                if(active&&fabulous){
                    float[] glowCenter=read(client.worldRenderer.entity,width/2,height/2);
                    System.out.println("Fabulous target center="+Arrays.toString(glowCenter));
                    require(glowCenter[0]>0,"Fabulous aura reaches entity target");
                    require(Math.abs(worldCenter[0]-background[0])<.005f,"Fabulous pass preserves main target");
                }else if(active)require(worldCenter[0]>background[0]+.005f,"aura is actually drawn");
                if(nullCache){System.out.println("PASS "+cases[frame]);continue;}
                client.framebuffer.beginWrite(true);RenderSystem.enableBlend();RenderSystem.disableDepthTest();RenderSystem.depthMask(false);
                RenderSystem.blendFuncSeparate(GL_ZERO,GL_ONE_MINUS_SRC_COLOR,GL_ONE,GL_ZERO);
                RenderSystem.setShaderColor(.7f,.7f,.7f,1);RenderSystem.setShaderTexture(0,vignetteTexture);RenderSystem.setShader(()->vignette);
                System.out.println("vignette before bind factors="+glGetInteger(GL_BLEND_SRC_RGB)+","+glGetInteger(GL_BLEND_DST_RGB));
                BufferBuilder vb=new BufferBuilder(256);vb.begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION_TEXTURE);
                vb.vertex(-1,-1,0).texture(0,0).next();vb.vertex(1,-1,0).texture(1,0).next();vb.vertex(1,1,0).texture(1,1).next();vb.vertex(-1,1,0).texture(0,1).next();
                BufferRenderer.drawWithGlobalProgram(vb.end());
                float[] postVignette=read(client.framebuffer,width/2,height/2);
                System.out.println("vignette after bind factors="+glGetInteger(GL_BLEND_SRC_RGB)+","+glGetInteger(GL_BLEND_DST_RGB)+" center="+Arrays.toString(postVignette));
                require(glGetInteger(GL_BLEND_SRC_RGB)==GL_ZERO&&glGetInteger(GL_BLEND_DST_RGB)==GL_ONE_MINUS_SRC_COLOR,"vanilla vignette blend factors survive shader bind");
                for(int channel=0;channel<3;channel++)require(Math.abs(postVignette[channel]-worldCenter[channel])<.005f,"vignette preserves world center");
                RenderSystem.setShaderColor(1,1,1,1);RenderSystem.defaultBlendFunc();
                presented.setClearColor(0,0,0,1);presented.clear(false);presented.beginWrite(true);
                client.framebuffer.draw(width,height);
                float[] visible=read(presented,width/2,height/2);
                for(int channel=0;channel<3;channel++)require(Math.abs(visible[channel]-postVignette[channel])<.005f,"final framebuffer presentation preserves world");
                System.out.println("frame "+frame+" present "+Arrays.toString(visible)+" GL="+glGetError());
                if(fabulous) {
                    transparency.setProjectionMatrix(new Matrix4f().setOrtho(0,width,0,height,.1f,1000f));
                    RenderSystem.disableCull();RenderSystem.disableDepthTest();RenderSystem.depthMask(false);
                    transparency.render(0);
                    float[] finalCenter=read(composed,width/2,height/2);
                    float[] entityCenter=read(client.worldRenderer.entity,width/2,height/2);
                    System.out.println("Fabulous final center="+Arrays.toString(finalCenter));
                    if(active)require(entityCenter[3]>0,"Fabulous fill alpha survives 8-bit storage and vanilla try_insert");
                    float change=0;
                    for(int channel=0;channel<3;channel++) {
                        float expected=postVignette[channel]*(1-entityCenter[3])+entityCenter[channel];
                        require(Math.abs(finalCenter[channel]-expected)<.006f,"actual vanilla transparency premultiplied result");
                        change=Math.max(change,Math.abs(finalCenter[channel]-postVignette[channel]));
                    }
                    require(active?change>1f/255:change<.005f,"Fabulous final world changes only while aura is active");
                    require(glGetError()==GL_NO_ERROR,"no GL error in actual vanilla compositor");
                }
                // HUD geometry must remain usable after the actual final presentation shader.
                client.framebuffer.beginWrite(true);RenderSystem.disableDepthTest();RenderSystem.depthMask(false);
                RenderSystem.setProjectionMatrix(new Matrix4f(),VertexSorter.BY_DISTANCE);
                RenderSystem.getModelViewStack().loadIdentity();RenderSystem.applyModelViewMatrix();
                RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShader(()->ordinary);
                BufferBuilder buffer=new BufferBuilder(256);buffer.begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION_COLOR);
                for(float[] corner:corners)buffer.vertex(corner[0],corner[1],0).color(0,255,0,255).next();
                BufferRenderer.drawWithGlobalProgram(buffer.end());
                float[] next=read(client.framebuffer,width/2,height/2);
                System.out.println("frame "+frame+" nextdraw "+Arrays.toString(next)+" GL="+glGetError());
                require(next[1]>.99f&&next[0]<.01f,"subsequent ordinary Minecraft draw succeeds");
                require(glGetError()==GL_NO_ERROR,"no GL error after HUD");
                System.out.println("PASS "+cases[frame]);
            }
            call("release");transparency.close();emptyLayer.delete();composed.delete();presented.delete();client.framebuffer.delete();client.worldRenderer.entity.delete();
            mask.close();soften.close();composite.close();blit.close();ordinary.close();vignette.close();
            GlStateManager._deleteTexture(white);GlStateManager._deleteTexture(vignetteTexture);
            System.out.println("PASS actual Minecraft shader / framebuffer pipeline "+checks+" checks");
        }finally{glfwDestroyWindow(window);glfwTerminate();}
    }
    static float[] read(Framebuffer f,int x,int y){f.beginWrite(false);float[] values=new float[4];glReadPixels(x,y,1,1,GL_RGBA,GL_FLOAT,values);return values;}
    static void set(String name,Object value)throws Exception{Field f=BodyFlightAura.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    static void call(String name)throws Exception{Method m=BodyFlightAura.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(null);}
    static void require(boolean pass,String label){checks++;if(!pass)throw new AssertionError(label);}
}
