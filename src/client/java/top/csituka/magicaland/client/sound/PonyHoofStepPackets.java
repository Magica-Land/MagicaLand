package top.csituka.magicaland.client.sound;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import top.csituka.magicaland.client.network.ClientNetworkHandler;
import top.csituka.magicaland.sound.HoofStepProtocol;
import top.csituka.magicaland.sound.BlockStepSounds;
import top.csituka.magicaland.sound.PlayerStepSoundScope;

public final class PonyHoofStepPackets {
    private PonyHoofStepPackets() {}

    public static boolean intercept(ClientPlayNetworkHandler handler, PlaySoundFromEntityS2CPacket packet) {
        var client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() != handler || client.world == null || !ClientNetworkHandler.supportsHoofSteps()) return false;
        var entity = client.world.getEntityById(packet.getEntityId());
        var sound = packet.getSound().value();
        if (!HoofStepProtocol.mayIntercept(true, packet.getCategory() == SoundCategory.PLAYERS,
                entity instanceof AbstractClientPlayerEntity, isBlockStep(sound))) return false;
        return PonyHoofSounds.interceptEntityStep((AbstractClientPlayerEntity)entity, sound);
    }
    public static boolean suppressLocal(Object owner, SoundEvent sound) {
        var client = MinecraftClient.getInstance();
        return owner == client.player && PlayerStepSoundScope.matches(owner, sound)
                && PonyHoofSounds.interceptEntityStep(client.player, sound);
    }
    public static boolean isBlockStep(SoundEvent sound) {
        return BlockStepSounds.contains(sound);
    }
}
