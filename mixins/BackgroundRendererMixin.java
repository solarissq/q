package com.deccalware.mixins;

import com.deccalware.DeccaLWare;
import com.deccalware.modules.visual.NoRender;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
    private static void onApplyFog(Camera camera, BackgroundRenderer.FogType fogType, org.joml.Vector4f color,
            float viewDistance, boolean thickFog, float tickDelta, CallbackInfoReturnable<Fog> cir) {
        NoRender noRender = (NoRender) DeccaLWare.INSTANCE.moduleManager.getModuleByName("NoRender");
        if (noRender != null && noRender.isEnabled() && noRender.fog.getValue()) {
            cir.setReturnValue(Fog.DUMMY);
        }
    }
}
