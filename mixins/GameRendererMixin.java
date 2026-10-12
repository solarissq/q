package com.deccalware.mixins;

import com.deccalware.DeccaLWare;
import com.deccalware.deccal.hud.DeccalHudRenderer;
import com.deccalware.deccal.screens.dropdown.ClickGUI;
import com.deccalware.modules.visual.NoRender;
import com.deccalware.modules.visual.ViewModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    public void onBobView(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        ViewModel viewModel = (ViewModel) DeccaLWare.INSTANCE.moduleManager.getModuleByName("ViewModel");
        if (viewModel != null && viewModel.isEnabled()) {
        }

        NoRender noRender = (NoRender) DeccaLWare.INSTANCE.moduleManager.getModuleByName("NoRender");
        if (noRender != null && noRender.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;draw()V", ordinal = 1, shift = At.Shift.BEFORE))
    private void onRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        var immediate = client.getBufferBuilders().getEntityVertexConsumers();
        var context = new DrawContext(client, immediate);
        if (!(client.currentScreen instanceof ClickGUI)) {
            DeccalHudRenderer.render(context);
            DeccaLWare.INSTANCE.onRender(context);
        }
    }

    @org.spongepowered.asm.mixin.injection.Redirect(method = "getBasicProjectionMatrix", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;getFramebufferWidth()I"))
    private int redirectWidthForAspectRatio(net.minecraft.client.util.Window window) {
        com.deccalware.modules.visual.AspectRatio aspectRatio = (com.deccalware.modules.visual.AspectRatio) DeccaLWare.INSTANCE.moduleManager.getModuleByName("AspectRatio");
        if (aspectRatio != null && aspectRatio.isEnabled()) {
            return (int) (window.getFramebufferHeight() * aspectRatio.ratio.getValue().floatValue());
        }
        return window.getFramebufferWidth();
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void onTiltViewWhenHurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        com.deccalware.modules.visual.NoHurtCam noHurtCam = (com.deccalware.modules.visual.NoHurtCam) DeccaLWare.INSTANCE.moduleManager.getModuleByName("NoHurtCam");
        if (noHurtCam != null && noHurtCam.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "updateCrosshairTarget", at = @At("RETURN"))
    private void onUpdateCrosshairTarget(float tickDelta, CallbackInfo ci) {
        com.deccalware.modules.visual.FreeCam freeCam = (com.deccalware.modules.visual.FreeCam) DeccaLWare.INSTANCE.moduleManager.getModuleByName("FreeCam");
        // Crosshair her zaman KAMERA (sanal freecam) yonune ayarlanir. Interact toggle'dan
        // BAGIMSIZ çalismali: boylece "Interact (Break/Place)" kapaliyken bile blok koyma/
        // kirma ekran ortasindaki (baktigin) yere gider, karakterin baktigi yere degil.
        // (Grim-safe rotasyon icin Interact toggle'i acik olmali - packet penceresini o acar.)
        if (freeCam != null && freeCam.isEnabled()) {
            freeCam.updateCrosshairTarget(tickDelta);
        }
    }
}
