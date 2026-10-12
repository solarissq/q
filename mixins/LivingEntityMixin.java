package com.deccalware.mixins;

import com.deccalware.DeccaLWare;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeath(net.minecraft.entity.damage.DamageSource source, CallbackInfo ci) {
        if (DeccaLWare.INSTANCE.moduleManager.getModuleByName("KillEffect").isEnabled()) {
            LivingEntity living = (LivingEntity) (Object) this;
            if (living instanceof PlayerEntity) {
                MinecraftClient mc = MinecraftClient.getInstance();
                LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
                lightning.setPosition(living.getPos());
                mc.world.addEntity(lightning);
            }
        }
    }
}
