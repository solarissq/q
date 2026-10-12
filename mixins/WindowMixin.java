package com.deccalware.mixins;

import net.minecraft.client.MinecraftClient;
import com.deccalware.DeccaLWare;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class WindowMixin {
    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    private void getWindowTitle(CallbackInfoReturnable<String> info) {
        info.setReturnValue(DeccaLWare.MOD_NAME);
    }
}
