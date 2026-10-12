package com.deccalware.mixins;

import com.deccalware.DeccaLWare;
import com.deccalware.modules.visual.AttackParticles;
import com.deccalware.modules.visual.HitSounds;
import com.deccalware.modules.visual.JojoStand;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        boolean crit = player.fallDistance > 0.0F && !player.isOnGround() && !player.isClimbing()
                && !player.isTouchingWater() && !player.hasVehicle() && !player.isSprinting();

        JojoStand js = (JojoStand) DeccaLWare.INSTANCE.moduleManager.getModuleByName("JojoStand");
        if (js != null && js.isEnabled()) {
            js.setAttack(target, crit);
        }

        HitSounds hs = (HitSounds) DeccaLWare.INSTANCE.moduleManager.getModuleByName("HitSounds");
        if (hs != null && hs.isEnabled()) {
            hs.onAttack(crit);
        }
        AttackParticles ap = (AttackParticles) DeccaLWare.INSTANCE.moduleManager.getModuleByName("AttackParticles");
        if (ap != null && ap.isEnabled()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            int count = ap.count.getValue().intValue();
            for (int i = 0; i < count; i++) {
                mc.world.addParticle(ParticleTypes.CRIT, target.getX(), target.getY() + target.getHeight() / 2,
                        target.getZ(),
                        (Math.random() - 0.5) * 0.5, (Math.random() - 0.5) * 0.5, (Math.random() - 0.5) * 0.5);
            }
        }
    }
}
