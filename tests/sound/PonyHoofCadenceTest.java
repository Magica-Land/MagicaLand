package top.csituka.magicaland.client.sound;

import java.util.List;
import java.util.Random;

public final class PonyHoofCadenceTest {
    private static int checks;
    public static void main(String[] args) {
        profiles(); basic(); landings(); clocks(); cadence(); fuzz();
        System.out.println("PonyHoofCadenceTest: " + checks + " checks PASS");
    }
    private static void profiles() {
        for(var p:PonyHoofCadence.profiles()) {
            yes(p.lengthTicks()>0,"positive length"); int mask=0;
            for(var c:p.contacts()) { mask|=c.hoofMask(); yes(c.seconds()>=0&&c.seconds()<p.lengthSeconds(),"in clip"); }
            yes(mask==PonyHoofCadence.ALL,"all four feet covered");
            try { p.contacts().clear(); throw new AssertionError("mutable contacts"); } catch(UnsupportedOperationException expected){checks++;}
        }
        yes(PonyHoofCadence.profile(null)==null&&PonyHoofCadence.profile("idle")==null,"unsupported silent");
        near(PonyHoofCadence.profile("walk").lengthTicks(),13.334,"author walk length");
        near(PonyHoofCadence.profile("run").lengthTicks(),7.5,"author run length");
        near(PonyHoofCadence.profile("sneak").lengthTicks(),20.834,"author crouch length");
        near(PonyHoofCadence.speed("sneak",.208),.8,"slower crouch same controller formula");
        near(PonyHoofCadence.speed("sneak",.13),.5,"half crouch");
        near(PonyHoofCadence.speed("sneak",0),.25,"minimum bounded pace");
        for(String action:new String[]{"walk","run","idle","land","backward_walk",null}) near(PonyHoofCadence.speed(action,.1),1,"other pace fixed");
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) near(PonyHoofCadence.speed("sneak",invalid),1,"invalid speed fallback");
    }
    private static void basic() {
        var c=new PonyHoofCadence();
        empty(c.update("walk",0,.26,true,true,5),"first observation silent");
        var hit=c.update("walk",1,.26,true,true,6);
        yes(hit.size()==1&&hit.get(0).hoofMask()==(PonyHoofCadence.LEFT_FRONT|PonyHoofCadence.RIGHT_HIND),"near-simultaneous diagonal merged");
        empty(c.update("walk",1,.26,true,true,6),"repeat same tick silent");
        empty(c.update("walk",2,.26,true,true,6),"unchanged phase silent");
        empty(c.update("run",3,.26,true,true,6),"changing clip does not replay");
        empty(c.update("run",4,.26,true,false,7),"stopped feet silent");
        empty(c.update("run",5,.26,true,true,0),"resumed movement rebases");
        c=new PonyHoofCadence(); c.update("run",0,.26,true,true,4);
        hit=c.update("run",4,.26,true,true,1);
        yes(hit.size()==1&&hit.get(0).hoofMask()==PonyHoofCadence.ALL,"multiple crossings around wrap coalesced once");
        empty(c.update("run",5,.26,true,true,.99),"tiny backward phase jitter not a new loop");
    }
    private static void landings() {
        for(String action:new String[]{"land","larger_land","walk","idle",null}) {
            var c=new PonyHoofCadence(); empty(c.update(action,10,.26,true,false,0),"first ground observation not landing");
            empty(c.update("jump1",11,.26,false,false,0),"air silent");
            c.update("fall",12,.26,false,false,0);
            c.update("fall",13,.26,false,false,0);
            var landing=c.update(action,14,.26,true,true,0);
            yes(landing.size()==1&&landing.get(0).landing()&&landing.get(0).hoofMask()==15,"actual landing immediate combined once");
            empty(c.update(action,14,.26,true,true,0),"same landing cannot repeat");
            for(int i=15;i<18;i++) {
                var next=c.update(action,i,.26,true,false,i-12);
                yes(next.stream().noneMatch(PonyHoofCadence.Impact::landing),"land rebound is not a second impact");
            }
        }
        var c=new PonyHoofCadence(); c.update("jump1",0,.26,false,false,0);
        empty(c.update("land",5,.26,true,false,0),"long observation gap cannot invent landing");
        c=new PonyHoofCadence(); c.update("walk",0,.26,true,true,4);
        empty(c.update("walk",1,.26,false,true,5),"half-slab brief air silent");
        var slab=c.update("walk",2,.26,true,true,6);
        yes(slab.stream().noneMatch(PonyHoofCadence.Impact::landing),"one-tick slab contact never heavy landing");
        c=new PonyHoofCadence(); c.update("walk",0,.26,true,true,4);
        c.update("jump1",1,.26,false,false,0);
        yes(c.update("larger_land",2,.26,true,false,0).get(0).landing(),"explicit authored landing can confirm brief observed fall");
    }
    private static void clocks() {
        var c=new PonyHoofCadence(); c.update("run",100,.26,true,true,4);
        empty(c.update("run",105,.26,true,true,1),"long stall skips old impacts");
        empty(c.update("run",99,.26,true,true,4),"regressing game clock resets quietly");
        empty(c.update("run",Double.NaN,.26,true,true,5),"invalid clock resets quietly");
        empty(c.update("run",100,.26,true,true,5),"recovery creates baseline only");
        c.reset(); empty(c.update("walk",0,.26,true,true,Double.NaN),"fallback baseline");
        for(int i=1;i<=8;i++) empty(c.update("walk",i,.26,true,true,Double.NaN),"fallback respects controller transition");
        yes(c.update("walk",9,.26,true,true,Double.NaN).size()==1,"fallback first full touchdown at authored phase plus transition");
        c=new PonyHoofCadence(); c.update("sneak",0,.26,true,true,4);
        yes(c.update("sneak",1,.13,true,true,Double.NaN).size()==1,"new speed does not rescale old phase; prior interval was full speed");
        empty(c.update("sneak",2,.13,true,true,Double.NaN),"subsequent interval uses new speed");
        c=new PonyHoofCadence(); c.update("walk",0,.26,true,true,0);
        empty(c.update("walk",1,.26,true,true,8),"large phase correction never catch-up plays");
        c.update("walk",2,.26,true,true,9);
        empty(c.update("walk",3,.26,true,true,5),"large reverse phase correction silent");
        empty(c.update("walk",4,.26,true,true,6),"revisited marker behind high water cannot duplicate");
    }
    private static void cadence() {
        for(String action:new String[]{"walk","run","backward_walk","sneak"}) for(double speed:new double[]{.25,.5,.8,1}) {
            double limb=action.equals("sneak")?.26*speed:.26;
            double rate=PonyHoofCadence.speed(action,limb), length=PonyHoofCadence.profile(action).lengthTicks();
            int[] reference=null;
            for(int fps:new int[]{20,30,60,144}) {
                var c=new PonyHoofCadence(); int[] totals=new int[4]; double duration=length*12/rate;
                c.update(action,0,limb,true,true,0);
                for(int frame=1;frame<=Math.ceil(duration*fps/20);frame++) {
                    double t=Math.min(duration,frame*20.0/fps), phase=(t*rate)%length;
                    var hits=c.update(action,t,limb,true,true,phase);
                    yes(hits.size()<=1,"bounded group per update");
                    for(var impact:hits)for(int hoof=0;hoof<4;hoof++)if((impact.hoofMask()&(1<<hoof))!=0)totals[hoof]++;
                    empty(c.update(action,t,limb,true,true,phase),"duplicate render pass never advances");
                }
                if(reference==null)reference=totals;
                for(int hoof=0;hoof<4;hoof++)yes(totals[hoof]==reference[hoof],"frame-rate independent marker count");
            }
        }
    }
    private static void fuzz() {
        var random=new Random(56741); String[] actions={"walk","run","sneak","backward_walk","land","idle",null};
        for(int n=0;n<200;n++) {
            var c=new PonyHoofCadence(); double t=0;
            for(int i=0;i<500;i++) {
                t+=random.nextInt(12)==0?10:random.nextDouble()*2;
                var action=actions[random.nextInt(actions.length)]; boolean grounded=random.nextBoolean(),moving=random.nextBoolean();
                var hits=c.update(action,t,random.nextDouble(),grounded,moving,random.nextBoolean()?Double.NaN:random.nextDouble()*20);
                yes(hits.size()<=1,"bounded under discontinuous observations");
                for(var h:hits) yes(grounded&&h.hoofMask()>0&&h.hoofMask()<=15,"valid grounded output");
            }
        }
    }
    private static void near(double a,double b,String why){yes(Math.abs(a-b)<1e-7,why);}
    private static void empty(List<?> value,String why){yes(value.isEmpty(),why);}
    private static void yes(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
}
