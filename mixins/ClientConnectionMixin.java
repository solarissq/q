package com.deccalware.mixins;

import com.deccalware.DeccaLWare;
import com.deccalware.modules.combat.Velocity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, PacketListener listener,
            CallbackInfo ci) {
        com.deccalware.modules.combat.Backtrack backtrack = (com.deccalware.modules.combat.Backtrack) DeccaLWare.INSTANCE.moduleManager
                .getModuleByName("Backtrack");
        if (backtrack != null && backtrack.onHandlePacket(packet)) {
            ci.cancel();
            return;
        }

        if (packet instanceof ChunkDataS2CPacket p) {
            com.deccalware.modules.misc.NewChunks newChunks = (com.deccalware.modules.misc.NewChunks) DeccaLWare.INSTANCE.moduleManager.getModuleByName("NewChunks");
            if (newChunks != null && newChunks.isEnabled()) {
                newChunks.onPacket(p);
            }
        }

        if (packet instanceof EntityVelocityUpdateS2CPacket p) {
            Velocity velocity = (Velocity) DeccaLWare.INSTANCE.moduleManager.getModuleByName("Velocity");
            if (velocity != null && velocity.isEnabled()) {
                if (p.getEntityId() == net.minecraft.client.MinecraftClient.getInstance().player.getId()) {
                    // Knocback yonunu/direksiyonu yakala -> Velocity.onTick counter-strafe icin kullanir
                    velocity.onKnockback(p.getVelocityX() / 8000.0, p.getVelocityY() / 8000.0, p.getVelocityZ() / 8000.0);
                    // Grim/Packet modlarda paketi iptal etmek Simulation flag'ine yol acar
                    // Velocity dusurme artik Velocity.onTick icinde yapiliyor (hurtTime >0 iken scale)
                    // Paketi iptal etmiyoruz, Grim'in 0.03 ve uncertainty ile handle etmesine izin veriyoruz
                    if (velocity.mode.getValue().equals("Grim") || velocity.mode.getValue().equals("Packet")) {
                        return;
                    }
                    if (velocity.mode.getValue().equals("Vulcan")) {
                        if (velocity.horizontal.getValue() == 0 && velocity.vertical.getValue() == 0) {
                            // Vulcan %0 icin bile tam iptal Grim'de flag, bu yuzden gecir
                            return;
                        }
                        return;
                    }
                }
            }
        } else if (packet instanceof ExplosionS2CPacket) {
            Velocity velocity = (Velocity) DeccaLWare.INSTANCE.moduleManager.getModuleByName("Velocity");
            if (velocity != null && velocity.isEnabled()) {
                // Explosion velocity iptali de Simulation/Explosion flag'i yapar, gecir
                return;
            }
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V", at = @At("HEAD"), cancellable = true)
    private void onSend(Packet<?> packet, net.minecraft.network.PacketCallbacks callbacks, CallbackInfo ci) {
        // Blink Logic: yalnizca movement paketleri buffer'lanir, digerleri gecer
        com.deccalware.modules.movement.Blink blink = (com.deccalware.modules.movement.Blink) DeccaLWare.INSTANCE.moduleManager
                .getModuleByName("Blink");
        if (blink != null && blink.isEnabled() && blink.onPacketSend(packet)) {
            ci.cancel();
        }

        // FreeCam: kamera gövdeden uzaktayken block break/place paketlerini engelle
        // Grim PositionBreakA / FabricatedPlace flag'lerini önlemek için
        com.deccalware.modules.visual.FreeCam freecam = (com.deccalware.modules.visual.FreeCam) DeccaLWare.INSTANCE.moduleManager
                .getModuleByName("FreeCam");
        if (freecam != null && freecam.isEnabled() && freecam.shouldBlockInteraction()) {
            if (packet instanceof net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket pa) {
                // Sadece kazma aksiyonlarını engelle; eşya bırakma / offhand swap normal çalışsın.
                net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action action = pa.getAction();
                if (action == net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.START_DESTROY_BLOCK
                        || action == net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK
                        || action == net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
                    ci.cancel();
                }
            } else if (packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket) {
                ci.cancel();
            }
        }
    }
}
