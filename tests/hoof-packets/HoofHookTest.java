package top.csituka.magicaland.sound;

import java.nio.file.Files;
import java.nio.file.Path;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class HoofHookTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        var entity=read("net/minecraft/entity/Entity");
        var player=read("net/minecraft/entity/player/PlayerEntity");
        var local=read("net/minecraft/client/network/ClientPlayerEntity");
        var network=read("net/minecraft/client/network/ClientPlayNetworkHandler");
        var serverWorld=read("net/minecraft/server/world/ServerWorld");
        var step=method(player,"playStepSound","(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V");
        check(calls(step,"playSwimSound")&&calls(step,"playSecondaryStepSound")&&calls(step,"playCombinationStepSounds"),"player step still delegates wading and covering combinations to vanilla");
        check(calls(method(entity,"playStepSounds","(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"),"playAmethystChimeSound"),"crystal chimes live outside player step scope");
        check(calls(method(entity,"stepOnBlock","(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;ZZLnet/minecraft/util/math/Vec3d;)Z"),"emitGameEvent"),"STEP game event is separate from replaced sound");
        var playerSound=method(player,"playSound","(Lnet/minecraft/sound/SoundEvent;FF)V");
        var localSound=method(local,"playSound","(Lnet/minecraft/sound/SoundEvent;FF)V");
        check(calls(playerSound,"playSound")&&calls(localSound,"playSound"),"local override needs its own exact playSound hook");
        var onEntity=method(network,"onPlaySoundFromEntity","(Lnet/minecraft/network/packet/s2c/play/PlaySoundFromEntityS2CPacket;)V");
        check(callIndex(onEntity,"forceMainThread")<callIndex(onEntity,"getEntityById"),"packet hook must run after forceMainThread");
        check(calls(onEntity,"getEntityId")&&calls(onEntity,"playSoundFromEntity"),"vanilla recipient resolves exact entity ID");
        var send=method(serverWorld,"playSoundFromEntity","(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/sound/SoundCategory;FFJ)V");
        check(calls(send,"sendToAround"),"vanilla transmission uses original audible range and excluded owner");
        var scope=read("top/csituka/magicaland/mixin/PlayerStepSoundMixin");
        var wrapper=scope.methods.stream().filter(m->m.name.equals("magicaland$stepScope")).findFirst().orElseThrow();
        check(calls(wrapper,"isTouchingWater")&&calls(wrapper,"enter")&&calls(wrapper,"close"),"exact player step has water bypass and scope cleanup");
        check(!wrapper.tryCatchBlocks.isEmpty(),"scope closes when original step throws");
        String repo=args[0];
        String bridge=Files.readString(Path.of(repo,"src/client/java/top/csituka/magicaland/client/sound/PonyHoofStepPackets.java"));
        check(bridge.contains("client.getNetworkHandler() != handler")&&bridge.contains("supportsHoofSteps()"),"connection identity and negotiated capability protect interception");
        check(bridge.contains("entity instanceof AbstractClientPlayerEntity")&&bridge.contains("SoundCategory.PLAYERS"),"ordinary horses and monsters are outside replacement");
        String common=Files.readString(Path.of(repo,"src/main/java/top/csituka/magicaland/sound/PlayerStepSoundPackets.java"));
        check(common.contains("PlayerLookup.tracking(player)")&&common.contains("Delivery.POSITION")&&common.contains("observer == player"),"per-recipient tracking fallback and owner exclusion are present");
        check(!common.contains("setSeed")&&!common.contains("MAGIC_SEED"),"original random seed carries no protocol marker");
        String client=Files.readString(Path.of(repo,"src/client/java/top/csituka/magicaland/client/network/ClientNetworkHandler.java"));
        check(client.split("serverHoofSteps = false;",-1).length-1==2,"join and disconnect both reset hoof capability");
        check(client.contains("client.getNetworkHandler() == handler"),"late handshake from an old connection cannot enable replacement");
        System.out.println("PASS HoofHookTest: "+checks+" actual Minecraft method and narrow integration checks");
    }
    private static ClassNode read(String name) throws Exception {
        try(var input=HoofHookTest.class.getClassLoader().getResourceAsStream(name+".class")) {
            if(input==null)throw new AssertionError("Missing class "+name);
            var node=new ClassNode();new ClassReader(input).accept(node,0);return node;
        }
    }
    private static MethodNode method(ClassNode type,String name,String desc){checks++;return type.methods.stream().filter(m->m.name.equals(name)&&m.desc.equals(desc)).findFirst().orElseThrow(()->new AssertionError(type.name+" missing "+name+desc));}
    private static boolean calls(MethodNode method,String name){return callIndex(method,name)>=0;}
    private static int callIndex(MethodNode method,String name){int i=0;for(var node:method.instructions){if(node instanceof MethodInsnNode call&&call.name.equals(name))return i;i++;}return -1;}
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}
