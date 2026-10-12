package com.deccalware.mixins;

import com.deccalware.DeccaLWare;
import com.deccalware.modules.visual.FreeCam;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {

    @Shadow
    protected void setPos(double x, double y, double z) {
    }

    @Shadow
    protected void setRotation(float yaw, float pitch) {
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void onCameraUpdate(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseViewport, float tickDelta, CallbackInfo ci) {
        FreeCam freeCam = (FreeCam) DeccaLWare.INSTANCE.moduleManager.getModuleByName("FreeCam");
        if (freeCam != null && freeCam.isEnabled()) {
            // Vücuttan koparılmış kamerayı pürüzsüz enterpolasyonla uygula
            setRotation(freeCam.getYaw(tickDelta), freeCam.getPitch(tickDelta));
            setPos(freeCam.getX(tickDelta), freeCam.getY(tickDelta), freeCam.getZ(tickDelta));
        }
    }

    @Inject(method = "isThirdPerson", at = @At("HEAD"), cancellable = true)
    private void onIsThirdPerson(CallbackInfoReturnable<Boolean> cir) {
        FreeCam freeCam = (FreeCam) DeccaLWare.INSTANCE.moduleManager.getModuleByName("FreeCam");
        if (freeCam != null && freeCam.isEnabled() && freeCam.showPlayer.getValue()) {
            cir.setReturnValue(true);
        }
    }
}