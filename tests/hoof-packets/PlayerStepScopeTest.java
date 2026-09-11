package top.csituka.magicaland.sound;

import com.google.gson.JsonParser;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PlayerStepScopeTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        Object pony=new Object(), other=new Object(), step=new Object(), covering=new Object(), swim=new Object();
        check(!PlayerStepSoundScope.matches(pony,step),"outside a step scope never replaces sounds");
        try(var scope=PlayerStepSoundScope.enter(pony,step,covering)) {
            check(PlayerStepSoundScope.matches(pony,step)&&PlayerStepSoundScope.matches(pony,covering),"primary and covering block sounds match");
            check(!PlayerStepSoundScope.matches(pony,swim)&&!PlayerStepSoundScope.matches(other,step),"unrelated sound or another actor is never captured");
            try(var inner=PlayerStepSoundScope.enter(other,swim,null)) {
                check(!PlayerStepSoundScope.matches(pony,step)&&PlayerStepSoundScope.matches(other,swim),"nested actor owns only its own scope");
            }
            check(PlayerStepSoundScope.matches(pony,step),"nested scope restores enclosing step");
            AtomicBoolean leaked=new AtomicBoolean(true);
            Thread thread=new Thread(()->leaked.set(PlayerStepSoundScope.matches(pony,step)));
            thread.start();thread.join();check(!leaked.get(),"integrated server and client threads do not share scope");
        }
        check(!PlayerStepSoundScope.matches(pony,step),"normal return releases scope");
        try {
            try(var scope=PlayerStepSoundScope.enter(pony,step,covering)) { throw new IllegalStateException("expected"); }
        } catch(IllegalStateException expected) { check(!PlayerStepSoundScope.matches(pony,step),"exception releases scope"); }
        for(boolean owner:new boolean[]{false,true})for(boolean sameWorld:new boolean[]{false,true})for(boolean audible:new boolean[]{false,true})for(boolean tracked:new boolean[]{false,true}) {
            var expected=owner||!sameWorld||!audible?HoofStepProtocol.Delivery.NONE:tracked?HoofStepProtocol.Delivery.ENTITY:HoofStepProtocol.Delivery.POSITION;
            check(HoofStepProtocol.delivery(owner,sameWorld,audible,tracked)==expected,"exact routing matrix: owner excluded, unloaded target gets position fallback");
        }
        for(boolean supported:new boolean[]{false,true})for(boolean players:new boolean[]{false,true})for(boolean loaded:new boolean[]{false,true})for(boolean stepSound:new boolean[]{false,true})
            check(HoofStepProtocol.mayIntercept(supported,players,loaded,stepSound)==(supported&&players&&loaded&&stepSound),"interception needs handshake, player category, known actor, and registered step");
        for(String json:new String[]{"{}","{\"hoof_step_version\":0}","{\"hoof_step_version\":2}","{\"hoof_step_version\":1.5}","{\"hoof_step_version\":\"1\"}","{\"hoof_step_version\":true}","{\"hoof_step_version\":null}","{\"hoof_step_version\":{}}","{\"hoof_step_version\":[]}"})
            check(!HoofStepProtocol.supported(JsonParser.parseString(json).getAsJsonObject()),"missing, malformed and unsupported versions retain vanilla sound");
        check(HoofStepProtocol.supported(JsonParser.parseString("{\"hoof_step_version\":1,\"future_optional\":true}").getAsJsonObject()),"v1 capability tolerates unrelated future fields");
        check(!HoofStepProtocol.supported(null),"absent handshake remains unsupported");
        System.out.println("PASS PlayerStepScopeTest: "+checks+" scope, routing, capability and actor checks");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}
