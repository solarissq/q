package com.deccalware.mixins;

import com.deccalware.deccal.util.render.DeccalShaderRegistry;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShaderLoader.class)
public class ShaderLoaderMixin {
    @Inject(method = "getOrCreateProgram", at = @At("HEAD"), cancellable = true)
    private void deccalware$entitySkyProgram(ShaderProgramKey key, CallbackInfoReturnable<ShaderProgram> cir) {
        if (key.equals(DeccalShaderRegistry.ENTITY_SKY_KEY) && DeccalShaderRegistry.entitySky != null) {
            cir.setReturnValue(DeccalShaderRegistry.entitySky);
        }
    }
}