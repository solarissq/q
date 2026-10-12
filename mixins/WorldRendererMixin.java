package com.deccalware.mixins;

import com.deccalware.DeccaLWare;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.ObjectAllocator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "render", at = @At("RETURN"))
    public void render(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera,
            GameRenderer gameRenderer, org.joml.Matrix4f positionMatrix, org.joml.Matrix4f projectionMatrix,
            CallbackInfo ci) {
        DeccaLWare.INSTANCE.lastModelViewMatrix = new org.joml.Matrix4f(positionMatrix);
        DeccaLWare.INSTANCE.lastProjectionMatrix = new org.joml.Matrix4f(projectionMatrix);
        // Moduller vertex'i kendi positionMatrix (model-view) ile bake ediyor (VertexConsumer
        // default vertex(Matrix4f): matrix.transformPosition). drawWithGlobalProgram ise
        // RenderSystem.getModelViewMatrix * getProjectionMatrix * vertex cayirir. Cift transform
        // olmamasi icin modul cizimi sirasinda RenderSystem model-view'i IDENTITY tutulmali.
        // Ayrica shader state'ini deterministik yap: vanilla dongusunden kalan stale bir program
        // modulleri bozar (IMinecraft.draw sadece shader NULL iken fallback baglar).
        RenderSystem.clearShader();
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        MatrixStack matrices = new MatrixStack();
        matrices.peek().getPositionMatrix().set(positionMatrix);
        DeccaLWare.INSTANCE.moduleManager.onWorldRender(matrices);
        RenderSystem.getModelViewStack().popMatrix();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline,
            Camera camera, GameRenderer gameRenderer, org.joml.Matrix4f projectionMatrix, org.joml.Matrix4f modelViewMatrix,
            CallbackInfo ci) {
        try {
            com.deccalware.modules.visual.SkyShader sky =
                    (com.deccalware.modules.visual.SkyShader) DeccaLWare.INSTANCE.moduleManager.getModuleByName("SkyShader");
            if (sky != null && sky.isEnabled()) {
                // World (all terrain/blocks) is fully drawn and written to the depth buffer.
                // Draw the fog with GL_LEQUAL + far-depth so it only shows where the depth is the
                // far plane (empty sky) - walls/buildings/ground block it -> no xray. Held items
                // are drawn afterwards, so they stay clean too.
                sky.renderSky();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
