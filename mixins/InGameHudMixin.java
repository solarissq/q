package com.deccalware.mixins;

import com.deccalware.DeccaLWare;
import com.deccalware.modules.visual.SkyShader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        try {
            SkyShader sky = (SkyShader) DeccaLWare.INSTANCE.moduleManager.getModuleByName("SkyShader");
            if (sky != null && sky.isIrisFallbackActive()) {
                sky.renderHudFallback(context);
            }
        } catch (Exception ignored) {}
    }
}
