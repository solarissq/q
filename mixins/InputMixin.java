package com.deccalware.mixins;

import com.deccalware.DeccaLWare;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class InputMixin {

    // FreeCam gövde dondurulmuşken vanilla klavye girdisini engelle.
    // Interact penceresi (kırma/koyma tuşu basılı) AÇIKken giriş serbest kalır ki
    // gövde gerçek fizikle (onGround) çalışsın -> Grim pre-flying/rotationPlace yemez.
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void deccalwareBlockInput(CallbackInfo ci) {
        com.deccalware.modules.visual.FreeCam fc = (com.deccalware.modules.visual.FreeCam)
                DeccaLWare.INSTANCE.moduleManager.getModuleByName("FreeCam");
        if (fc != null && fc.isEnabled() && fc.freeze.getValue() && !fc.isInteracting()) {
            Input input = (Input) (Object) this;
            input.movementForward = 0.0F;
            input.movementSideways = 0.0F;
            input.playerInput = PlayerInput.DEFAULT;
            ci.cancel();
        }
    }
}