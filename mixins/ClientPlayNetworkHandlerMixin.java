package com.deccalware.mixins;

import com.deccalware.DeccaLWare;
import com.deccalware.modules.hvh.AutoL;
import com.deccalware.modules.visual.HitSounds;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import com.deccalware.modules.combat.Backtrack;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (message.startsWith(".")) {
            String[] args = message.substring(1).split(" ");
            if (args.length > 0) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (args[0].equalsIgnoreCase("config")) {
                    if (args.length >= 3) {
                        String action = args[1];
                        String name = args[2];
                        if (action.equalsIgnoreCase("save")) {
                            DeccaLWare.INSTANCE.configManager.saveConfig(name);
                            if (client.inGameHud != null)
                                client.inGameHud.getChatHud().addMessage(Text.of("§aSaved config: " + name));
                        } else if (action.equalsIgnoreCase("load")) {
                            DeccaLWare.INSTANCE.configManager.loadConfig(name);
                            if (client.inGameHud != null)
                                client.inGameHud.getChatHud().addMessage(Text.of("§aLoaded config: " + name));
                        }
                    } else {
                        if (client.inGameHud != null)
                            client.inGameHud.getChatHud().addMessage(Text.of("§cUsage: .config <save/load> <name>"));
                    }
                    ci.cancel();
                } else if (args[0].equalsIgnoreCase("save")) {
                    if (args.length >= 2) {
                        DeccaLWare.INSTANCE.configManager.saveConfig(args[1]);
                        if (client.inGameHud != null)
                            client.inGameHud.getChatHud().addMessage(Text.of("§aSaved config: " + args[1]));
                    }
                    ci.cancel();
                } else if (args[0].equalsIgnoreCase("load")) {
                    if (args.length >= 2) {
                        DeccaLWare.INSTANCE.configManager.loadConfig(args[1]);
                        if (client.inGameHud != null)
                            client.inGameHud.getChatHud().addMessage(Text.of("§aLoaded config: " + args[1]));
                    }
                    ci.cancel();
                }
            }
        }
    }

    /**
     * Orijinal Minecraft Totem Sesini Tamamen Engeller (Cancellable)
     */
    @Inject(method = "onPlaySound", at = @At("HEAD"), cancellable = true)
    private void onPlaySound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        if (AutoL.INSTANCE != null && AutoL.INSTANCE.isModuleActive() && AutoL.INSTANCE.customTotemSound.getValue()) {
            // Eğer gelen ses paketi vanilla totem kullanma sesiyse iptal et (Sustur)
            if (packet.getSound().value() == SoundEvents.ITEM_TOTEM_USE) {
                ci.cancel();
            }
        }

        // Cancel vanilla hit sounds when HitSounds module is enabled
        HitSounds hs = (HitSounds) DeccaLWare.INSTANCE.moduleManager.getModuleByName("HitSounds");
        if (hs != null && hs.isEnabled()) {
            if (packet.getSound().value() == SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE ||
                packet.getSound().value() == SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK ||
                packet.getSound().value() == SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP ||
                packet.getSound().value() == SoundEvents.ENTITY_PLAYER_ATTACK_CRIT ||
                packet.getSound().value() == SoundEvents.ENTITY_PLAYER_ATTACK_STRONG ||
                packet.getSound().value() == SoundEvents.ENTITY_PLAYER_HURT ||
                packet.getSound().value() == SoundEvents.ENTITY_GENERIC_HURT) {
                ci.cancel();
            }
        }
    }

    /**
     * Sunucudan gelen durum paketini alır, mesafe ve canSee kontrolü için içeri
     * iletir
     */
    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (DeccaLWare.INSTANCE != null && DeccaLWare.INSTANCE.moduleManager != null) {
            for (com.deccalware.modules.Module module : DeccaLWare.INSTANCE.moduleManager.getEnabledModules()) {
                module.onPacketReceive(packet);
            }
        }
    }

    /**
     * Backtrack: goreceli varlik hareket paketlerini (MoveRelative/RotateAndMoveRelative/
     * MoveAbsolute/Rotate) listener duzeyinde yakalar. ClientConnection.handlePacket
     * ic yapisi surumler arasi degistiginden, garanti calisan nokta burasidir:
     * onEntity, EntityS2CPacket icin TEK giris noktasi ve parametre zaten paketin kendisi.
     * Cancellable oldugu icin ClientConnection uzerinden de buffer'laniyorsa cift
     * kuyruk olusmaz (birincisi iptal edince burasi cagrilmaz).
     */
    @Inject(method = "onEntity", at = @At("HEAD"), cancellable = true)
    private void onEntityMotion(EntityS2CPacket packet, CallbackInfo ci) {
        Backtrack b = (Backtrack) DeccaLWare.INSTANCE.moduleManager.getModuleByName("Backtrack");
        if (b != null && b.onHandlePacket(packet)) {
            ci.cancel();
        }
    }

    /**
     * Teleport (EntityPositionS2CPacket): asla ertelenmez; varsa o varligin
     * kuyrugu flush edilir, paket aninda gecer. Ayrica 1.21.4 oncesi cagrilan
     * onEntityPositionSync tam-pozisyon esitlemesi de ayni sekilde ele alinir.
     */
    @Inject(method = "onEntityPosition", at = @At("HEAD"))
    private void onEntityPositionEvent(EntityPositionS2CPacket packet, CallbackInfo ci) {
        Backtrack b = (Backtrack) DeccaLWare.INSTANCE.moduleManager.getModuleByName("Backtrack");
        if (b != null) b.onHandlePacket(packet);
    }
}