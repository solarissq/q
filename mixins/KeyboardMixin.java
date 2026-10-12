package com.deccalware.mixins;

import com.deccalware.modules.ModuleManager;
import com.deccalware.DeccaLWare;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    // "onKey" is the named mapping. If this warns, we might need the full
    // descriptor.
    // Descriptor: (JIIII)V
    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (action == 1) { // Press
            if (DeccaLWare.INSTANCE != null && DeccaLWare.INSTANCE.moduleManager != null) {
                DeccaLWare.INSTANCE.moduleManager.onKeyPress(key);
            }
        }
    }
}
