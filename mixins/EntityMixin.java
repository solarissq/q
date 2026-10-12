package com.deccalware.mixins;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "getTargetingMargin", at = @At("HEAD"), cancellable = true)
    private void deccalware$getTargetingMargin(CallbackInfoReturnable<Float> cir) {
        com.deccalware.modules.player.HitBox hitBox = (com.deccalware.modules.player.HitBox) com.deccalware.DeccaLWare.INSTANCE.moduleManager
                .getModuleByName("HitBox");

        if (hitBox != null && hitBox.isEnabled()) {
            cir.setReturnValue(hitBox.expand.getValue().floatValue());
        }
    }

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void deccalware$onChangeLookDirection(double cursorDeltaX, double cursorDeltaY, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if ((Object) this == net.minecraft.client.MinecraftClient.getInstance().player) {
            com.deccalware.modules.visual.FreeCam freeCam = (com.deccalware.modules.visual.FreeCam) com.deccalware.DeccaLWare.INSTANCE.moduleManager
                    .getModuleByName("FreeCam");
            if (freeCam != null && freeCam.isEnabled()) {
                freeCam.changeLookDirection(cursorDeltaX, cursorDeltaY);
                ci.cancel();
            }
        }
    }
}