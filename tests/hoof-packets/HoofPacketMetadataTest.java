package top.csituka.magicaland.sound;

import io.netty.buffer.Unpooled;
import java.lang.reflect.Field;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import sun.misc.Unsafe;

public final class HoofPacketMetadataTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        SharedConstants.createGameVersion();Bootstrap.initialize();
        Field field=Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        var actor=(ServerPlayerEntity)((Unsafe)field.get(null)).allocateInstance(ServerPlayerEntity.class);
        actor.setId(173);
        var sound=Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_STONE_STEP);
        for(float volume:new float[]{0,.05f,.15f,1,2})for(float pitch:new float[]{.5f,.8f,1,1.5f})for(long seed:new long[]{0,1,-1,Long.MIN_VALUE,Long.MAX_VALUE}) {
            var original=new PlaySoundFromEntityS2CPacket(sound,SoundCategory.PLAYERS,actor,volume,pitch,seed);
            PacketByteBuf buffer=new PacketByteBuf(Unpooled.buffer());
            try {
                original.write(buffer);var decoded=new PlaySoundFromEntityS2CPacket(buffer);
                check(decoded.getEntityId()==173,"packet retains exact player identity");
                check(decoded.getSound().value()==SoundEvents.BLOCK_STONE_STEP&&decoded.getCategory()==SoundCategory.PLAYERS,"vanilla sound event and category remain unchanged");
                check(decoded.getVolume()==volume&&decoded.getPitch()==pitch&&decoded.getSeed()==seed,"volume pitch and unmodified random seed survive vanilla codec");
                check(!buffer.isReadable(),"no custom payload appended to vanilla packet");
            } finally { buffer.release(); }
            var fallback=new PlaySoundS2CPacket(sound,SoundCategory.PLAYERS,1.125,64.25,-2.375,volume,pitch,seed);
            buffer=new PacketByteBuf(Unpooled.buffer());
            try {
                fallback.write(buffer);var decoded=new PlaySoundS2CPacket(buffer);
                check(decoded.getX()==1.125&&decoded.getY()==64.25&&decoded.getZ()==-2.375,"untracked actor preserves original coordinate fallback");
                check(decoded.getSound().value()==SoundEvents.BLOCK_STONE_STEP&&decoded.getVolume()==volume&&decoded.getPitch()==pitch&&decoded.getSeed()==seed,"fallback preserves original sound metadata");
            } finally { buffer.release(); }
        }
        int states=0;
        for(var block:Registries.BLOCK)for(var state:block.getStateManager().getStates()) {
            states++;check(BlockStepSounds.contains(state.getSoundGroup().getStepSound()),"every registered state-specific block step is recognized");
        }
        check(!BlockStepSounds.contains(SoundEvents.ENTITY_PLAYER_SWIM)&&!BlockStepSounds.contains(SoundEvents.ENTITY_PLAYER_HURT),"swimming and hurt sounds are not step candidates");
        check(!BlockStepSounds.contains(SoundEvents.ENTITY_HORSE_STEP),"ordinary horse entity audio is not intercepted as a block step");
        System.out.println("PASS HoofPacketMetadataTest: "+checks+" real packet/registry checks; "+states+" block states; no world or game loop");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}
