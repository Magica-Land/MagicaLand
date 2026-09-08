import com.google.gson.Gson;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import top.csituka.magicaland.client.config.ModelConfig;
import top.csituka.magicaland.client.render.BodyColorRamp;
import top.csituka.magicaland.client.render.ManeDye;
import top.csituka.magicaland.client.render.ManeDyeMask;
import top.csituka.magicaland.client.render.ManePalette;
import top.csituka.magicaland.client.render.ManePalette.Part;

public class ManeDyeTest {
    private static int checks;
    private static void check(boolean v, String message) { checks++; if (!v) throw new AssertionError(message); }
    private static void rejects(String json) {
        boolean failed=false;
        try { ManeDyeMask.read(new StringReader(json)); } catch (RuntimeException expected) { failed=true; }
        check(failed,"Malformed mask accepted");
    }
    private static String resource(int version, String regions) {
        return "{\"version\":"+version+",\"preset\":\"stripe01\",\"texture_width\":2,\"texture_height\":2,\"regions\":"+regions+"}";
    }
    private static String region(String part, int channel, String runs) {
        return "{\"part\":\""+part+"\",\"channel\":"+channel+",\"runs\":"+runs+"}";
    }
    public static void main(String[] args) throws Exception {
        Gson gson=new Gson();
        ModelConfig c=ModelConfig.sanitize(gson.fromJson("{}",ModelConfig.class));
        check(!c.maneDyeEnabled,"Old save must remain dye-off");
        c.maneDyeEnabled=true;
        for(Part part:Part.values())check(ManeDye.enabled(c,part),"01 unsupported");
        c.backManeStyle="02";
        check(!ManeDye.enabled(c,Part.BACK)&&ManeDye.enabled(c,Part.FRONT),"Mixed-style dye leaked");
        c.backManeStyle="01";
        check(ManeDye.retiredOverlay("Style01FrontManeHighlight")&&!ManeDye.retiredOverlay("Style02FrontManeHighlight"),"Overlay scope");
        ModelConfig old=ModelConfig.sanitize(gson.fromJson("{\"maneDyeEnabled\":true,\"maneDyeColor\":\"#23BBDD\",\"maneDyeAccentColor\":\"#AA44BB\"}",ModelConfig.class));
        for(Part part:Part.values()) {
            check(ManeDye.colors(old,part,3).base()==0x23BBDD,"Legacy primary color migration");
            check(ManeDye.colors(old,part,4).base()==(part==Part.BACK?0xAA44BB:0x23BBDD),"Legacy stripe split migration");
        }
        for(Part part:Part.values())ManeDye.resetRegions(c,part);
        c.frontManeColor="#483D79";c.maneColorLinkVersion=1;c.backManeColorLocked=true;c.tailColorLocked=true;
        c.frontManeShadowColorLocked=false;c.frontManeHighlightColorLocked=false;
        c.frontManeShadowColor="#314555";c.frontManeHighlightColor="#AACCDD";
        ModelConfig.sanitize(c);
        for(Part part:Part.values())for(int region=0;region<6;region++) {
            check(ManeDye.linked(c,part,region),"Reset should link every region");
            check(ManeDye.colors(c,part,region).equals(ManePalette.colors(c,part)),"Linked shading not identical");
            var before=ManeDye.palette(c,part,true,false);
            ManeDye.setLinked(c,part,region,false);
            check(before.equals(ManeDye.palette(c,part,true,false)),"Unlock changed visible manual shading");
            var otherBefore=ManeDye.palette(c,part==Part.FRONT?Part.BACK:Part.FRONT,true,false);
            ManeDye.setColor(c,part,region,"#22BBAA");
            check(ManeDye.colors(c,part,region).base()==0x22BBAA,"Region color not applied");
            check(otherBefore.equals(ManeDye.palette(c,part==Part.FRONT?Part.BACK:Part.FRONT,true,false)),"Region leaked to another part/cache key");
            for(int i=0;i<6;i++)if(i!=region)check(ManeDye.colors(c,part,i).equals(ManePalette.colors(c,part)),"Region leaked to another slot");
            check(c.frontManeColor.equals("#483D79"),"Region changed base");
            ManeDye.setLinked(c,part,region,true);
            check(before.equals(ManeDye.palette(c,part,true,false)),"Relock left a seam");
            ManeDye.setColor(c,part,region,"#FF0000");
            check(ManeDye.linked(c,part,region),"Locked region accepted editing");
        }
        ManeDye.setLinked(c,Part.BACK,2,false);ManeDye.setColor(c,Part.BACK,2,"#123456");
        c.frontManeColor="#765432";ModelConfig.sanitize(c);
        check(ManeDye.colors(c,Part.BACK,0).base()==0x765432&&ManeDye.colors(c,Part.BACK,2).base()==0x123456,"Main-color follow/fixed-region behavior");
        c.backManeColorLocked=false;c.backManeColor="#AABBCC";ModelConfig.sanitize(c);
        check(ManeDye.colors(c,Part.BACK,0).base()==0xAABBCC,"Region should follow independent part base");
        ModelConfig copy=ModelConfig.sanitize(gson.fromJson(gson.toJson(c),ModelConfig.class));
        check(gson.toJson(copy).equals(gson.toJson(c)),"JSON round trip");
        copy.backManeDyeColors[2]="#CC0000";
        check(!copy.backManeDyeColors[2].equals(c.backManeDyeColors[2]),"Copied config shares arrays");
        c.frontManeDyeColors=new String[]{"broken",null,"#aabbcc"};
        c.backManeDyeColors=new String[20];c.tailDyeColors=new String[0];
        c.maneDyePreset="../bad";ModelConfig.sanitize(c);
        check(c.frontManeDyeColors.length==6&&c.frontManeDyeColors[0]==null&&c.frontManeDyeColors[2].equals("#AABBCC"),"Invalid/short array sanitation");
        check(c.backManeDyeColors.length==6&&c.tailDyeColors.length==6&&c.maneDyePreset.equals("stripe01"),"Array bounds/preset sanitation");
        String stable=gson.toJson(c);ModelConfig.sanitize(c);check(stable.equals(gson.toJson(c)),"Sanitation not idempotent");
        List<ManePalette.Colors> disabled=ManeDye.palette(c,Part.FRONT,false,false);
        ManeDye.setLinked(c,Part.FRONT,1,false);ManeDye.setColor(c,Part.FRONT,1,"#FF0000");
        check(disabled.equals(ManeDye.palette(c,Part.FRONT,false,false)),"Disabled cache depends on unused colors");
        boolean immutable=false;try{disabled.clear();}catch(UnsupportedOperationException expected){immutable=true;}
        check(immutable,"Mutable texture cache key");
        check(gson.toJson(c).length()<16384,"Network model payload too large");

        String data=Files.readString(Path.of(args[0]));
        ManeDyeMask mask=ManeDyeMask.read(new StringReader(data));
        check(mask.compatible(256,256)&&mask.compatible(512,512)&&!mask.compatible(512,256),"Resolution compatibility");
        int total=0;
        for(Part part:Part.values()) {
            int[] counts=new int[7];
            for(int y=0;y<256;y++)for(int x=0;x<256;x++) {
                int ch=mask.channel(part,x,y,256,256);counts[ch]++;
                check(ch==mask.channel(part,2*x,2*y,512,512),"Upscaled channel mismatch");
                check(mask.contains(part,x,y,256,256)==(ch>0),"contains/channel mismatch");
                if(ch>0)total++;
            }
            for(int ch=1;ch<=6;ch++)check(counts[ch]>0,"Missing full-coverage color slot");
        }
        check(!mask.contains(Part.FRONT,-1,0,256,256)&&!mask.contains(Part.TAIL,0,0,512,256),"Invalid sampling");
        rejects(data.replace("\"version\":3","\"version\":4"));
        rejects(data.replace("\"version\":3","\"version\":2"));
        rejects(data.replace("\"channel\":6","\"channel\":7"));
        rejects(data.replace("\"texture_width\":256","\"texture_width\":0"));
        rejects(resource(3,"["+region("FRONT",1,"[[0,-1,2]]")+"]"));
        rejects(resource(3,"["+region("FRONT",1,"[[0,0,2],[0,1,2]]")+"]"));
        rejects(resource(3,"["+region("BACK",1,"[[0,0,2]]")+","+region("BACK",6,"[[0,1,2]]")+"]"));
        rejects(resource(3,"["+region("TAIL",1,"[]")+","+region("TAIL",1,"[]")+"]"));
        rejects(resource(3,"["+region("FRONT",1,"[[0,0.5,2]]")+"]"));
        rejects(resource(3,"["+region("BAD",1,"[]")+"]"));
        rejects(resource(2,"["+region("FRONT",2,"[[0,0,1]]")+"]"));
        rejects(resource(1,"["+region("BACK",2,"[[0,0,1]]")+"]"));
        check(ManeDyeMask.read(new StringReader(resource(1,"["+region("BACK",1,"[[0,0,1]]")+"]"))).channel(Part.BACK,0,0,2,2)==4,"V1 channel migration");
        check(ManeDyeMask.read(new StringReader(resource(2,"["+region("BACK",2,"[[0,0,1]]")+"]"))).channel(Part.BACK,0,0,2,2)==5,"V2 channel migration");

        int[] bases={0x483D79,0xC5415E,0xD89231,0xD6CE52,0x42AA77,0x438DCF,0xB16CCE};
        int[][] ramps=new int[7][];for(int i=0;i<7;i++)ramps[i]=ManeDye.ramp(bases[i]);
        for(boolean legacy:new boolean[]{false,true})for(int alpha:new int[]{0,1,127,255})for(int gray=0;gray<256;gray++)for(int channel=0;channel<=6;channel++){
            int pixel=alpha<<24|gray<<16|gray<<8|gray;
            int actual=ManeDye.recolor(pixel,channel,legacy,bases,ramps);
            int expected=legacy?ManeDye.tintAbgr(pixel,bases[channel]):BodyColorRamp.recolorAbgr(pixel,ramps[channel]);
            check(actual==expected&&(actual>>>24)==alpha,"Channel routing/shading/alpha");
        }
        int pixel=0xFFAAAAAA;
        check(ManeDye.recolor(pixel,7,false,bases,ramps)==ManeDye.recolor(pixel,0,false,bases,ramps),"Unknown channel fallback");
        System.out.println("PASS "+checks+" assertions: 18 independent regions, "+total+" masked atlas pixels including padding; locks/manual-base shading, migration, JSON, mask V1/V2/V3, bounds, cache-key isolation and soft/legacy alpha routing.");
    }
}
