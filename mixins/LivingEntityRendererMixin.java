package com.deccalware.mixins;

import com.deccalware.DeccaLWare;
import com.deccalware.deccal.util.render.DeccalShaderRegistry;
import com.deccalware.modules.visual.Chams;
import com.deccalware.modules.visual.PlayerShader;
import com.deccalware.modules.visual.HitColor;
import com.deccalware.modules.visual.SkyShader;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.IdentityHashMap;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    private final java.util.Map<LivingEntityRenderState, Boolean> deccalware$isSelfState = new IdentityHashMap<>();

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("HEAD"))
    private void deccalware$markSelf(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        deccalware$isSelfState.put(state, entity == MinecraftClient.getInstance().player);
    }

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("RETURN"))
    private void deccalware$applySelfRotation(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (entity == MinecraftClient.getInstance().player && DeccaLWare.INSTANCE.rotationManager.active) {
            float yaw = DeccaLWare.INSTANCE.rotationManager.yaw;
            float bodyYawDiff = MathHelper.wrapDegrees(yaw - state.bodyYaw);
            if (Math.abs(bodyYawDiff) > 50.0f) {
                state.bodyYaw += (bodyYawDiff > 0 ? bodyYawDiff - 50.0f : bodyYawDiff + 50.0f);
            }
            state.yawDegrees = MathHelper.wrapDegrees(yaw - state.bodyYaw);
            state.pitch = DeccaLWare.INSTANCE.rotationManager.pitch;
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void onRenderHead(LivingEntityRenderState state, MatrixStack matrixStack,
            VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        PlayerShader playerShader = PlayerShader.getInstance();
        Chams chams = (Chams) DeccaLWare.INSTANCE.moduleManager.getModuleByName("Chams");

        if (playerShader != null && playerShader.isEnabled()) {
            boolean isSelf = Boolean.TRUE.equals(deccalware$isSelfState.get(state));
            boolean shouldRender = (isSelf && playerShader.self.getValue()) || (!isSelf && playerShader.enemies.getValue());

            if (shouldRender) {
                if (playerShader.thruWalls.getValue()) {
                    RenderSystem.polygonOffset(-1.0f, -1100000.0f);
                    RenderSystem.enablePolygonOffset();
                }

                if ("Sky".equals(playerShader.mode.getValue())) {
                    setupEntitySkyShader();
                } else {
                    float time = (System.currentTimeMillis() % 10000L) / 1000.0f * playerShader.speed.getValue().floatValue();
                    float red = playerShader.r.getValue().floatValue() / 255f;
                    float green = playerShader.g.getValue().floatValue() / 255f;
                    float blue = playerShader.b.getValue().floatValue() / 255f;
                    float alpha = playerShader.a.getValue().floatValue();

                    switch (playerShader.mode.getValue()) {
                        case "Aqua":
                            red = 0.0f;
                            green = 0.5f + 0.5f * (float) Math.sin(time * 2.0);
                            blue = 0.8f + 0.2f * (float) Math.cos(time * 1.5);
                            break;
                        case "Purple":
                            red = 0.7f + 0.3f * (float) Math.sin(time * 2.5);
                            green = 0.1f;
                            blue = 0.8f + 0.2f * (float) Math.cos(time * 2.0);
                            break;
                        case "Galaxy":
                            red = 0.2f + 0.3f * (float) Math.sin(time * 1.8);
                            green = 0.1f + 0.2f * (float) Math.cos(time * 2.2);
                            blue = 0.7f + 0.3f * (float) Math.sin(time * 3.0);
                            break;
                        case "Nebula":
                            red = 0.8f + 0.2f * (float) Math.sin(time * 2.0);
                            green = 0.2f + 0.3f * (float) Math.cos(time * 1.7);
                            blue = 0.7f + 0.3f * (float) Math.sin(time * 2.4);
                            break;
                        case "Solar":
                            red = 0.9f + 0.1f * (float) Math.sin(time * 3.0);
                            green = 0.4f + 0.3f * (float) Math.cos(time * 2.0);
                            blue = 0.05f;
                            break;
                        case "Vortex":
                            red = 0.1f + 0.3f * (float) Math.cos(time * 2.5);
                            green = 0.6f + 0.4f * (float) Math.sin(time * 2.0);
                            blue = 0.9f;
                            break;
                        case "Cyber":
                            red = 0.0f;
                            green = 0.85f + 0.15f * (float) Math.sin(time * 4.0);
                            blue = 0.45f + 0.35f * (float) Math.cos(time * 3.0);
                            break;
                        case "Rainbow":
                            int rgb = java.awt.Color.HSBtoRGB((time * 0.4f) % 1.0f, 0.85f, 1.0f);
                            red = ((rgb >> 16) & 0xFF) / 255f;
                            green = ((rgb >> 8) & 0xFF) / 255f;
                            blue = (rgb & 0xFF) / 255f;
                            break;
                        case "Solid":
                        default:
                            break;
                    }

                    RenderSystem.setShaderColor(red, green, blue, alpha);
                }
            }
        } else if (chams != null && chams.isEnabled()) {
            boolean isSelf = Boolean.TRUE.equals(deccalware$isSelfState.get(state));
            boolean shouldRender = (isSelf && chams.self.getValue()) || (!isSelf && chams.enemies.getValue());

            if (shouldRender) {
                if (chams.thruWalls.getValue()) {
                    RenderSystem.polygonOffset(-1.0f, -1100000.0f);
                    RenderSystem.enablePolygonOffset();
                }
                RenderSystem.setShaderColor(
                        chams.r.getValue().floatValue() / 255f,
                        chams.g.getValue().floatValue() / 255f,
                        chams.b.getValue().floatValue() / 255f,
                        chams.a.getValue().floatValue());
            }
        }

        HitColor hitColor = (HitColor) DeccaLWare.INSTANCE.moduleManager.getModuleByName("HitColor");
        if (hitColor != null && hitColor.isEnabled() && hitColor.customColor.getValue() && state.hurt) {
            RenderSystem.setShaderColor(
                    hitColor.r.getValue().floatValue() / 255f,
                    hitColor.g.getValue().floatValue() / 255f,
                    hitColor.b.getValue().floatValue() / 255f,
                    hitColor.a.getValue().floatValue());
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("RETURN"))
    private void onRenderReturn(LivingEntityRenderState state, MatrixStack matrixStack,
            VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        PlayerShader playerShader = PlayerShader.getInstance();
        Chams chams = (Chams) DeccaLWare.INSTANCE.moduleManager.getModuleByName("Chams");

        if (playerShader != null && playerShader.isEnabled()) {
            if (playerShader.thruWalls.getValue()) {
                RenderSystem.polygonOffset(0.0f, 0.0f);
                RenderSystem.disablePolygonOffset();
            }
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        } else if (chams != null && chams.isEnabled()) {
            if (chams.thruWalls.getValue()) {
                RenderSystem.polygonOffset(0.0f, 0.0f);
                RenderSystem.disablePolygonOffset();
            }
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        HitColor hitColor = (HitColor) DeccaLWare.INSTANCE.moduleManager.getModuleByName("HitColor");
        if (hitColor != null && hitColor.isEnabled() && hitColor.customColor.getValue() && state.hurt) {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }

    @Inject(method = "getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;", at = @At("HEAD"), cancellable = true)
    private void onGetRenderLayer(LivingEntityRenderState state, boolean showBody, boolean translucent, boolean outline,
            CallbackInfoReturnable<RenderLayer> cir) {
        PlayerShader playerShader = PlayerShader.getInstance();
        if (playerShader == null || !playerShader.isEnabled() || !"Sky".equals(playerShader.mode.getValue())) {
            return;
        }
        boolean isSelf = Boolean.TRUE.equals(deccalware$isSelfState.get(state));
        boolean shouldRender = (isSelf && playerShader.self.getValue()) || (!isSelf && playerShader.enemies.getValue());
        if (!shouldRender) {
            return;
        }
        // Sky modunda entity geometry'si entitySkyLayer ile (KEY -> ShaderLoaderMixin redirect ->
        // manuel yüklenen DeccalShaderRegistry.entitySky programı) render edilir. Program flush
        // anında vanilla tarafından seçilir; ModelViewMat/ProjMat de vanilla tarafından set edilir.
        // HEAD'te yalnızca özel uniform'lar kurulur. SkyShader modülü kapalı olsa bile bu çalışır.
        if (DeccalShaderRegistry.entitySkyLayer != null) {
            cir.setReturnValue(DeccalShaderRegistry.entitySkyLayer);
        }
    }

    private void setupEntitySkyShader() {
        MinecraftClient mc = MinecraftClient.getInstance();
        SkyShader skyShader = (SkyShader) DeccaLWare.INSTANCE.moduleManager.getModuleByName("SkyShader");
        ShaderProgram program = DeccalShaderRegistry.entitySky;
        if (mc.world == null || program == null) {
            return;
        }

        try {
            var camera = mc.gameRenderer.getCamera();
            if (camera == null) {
                return;
            }

            // SkyShader modülü kapalı olsa bile canlı bir zaman üret: 'animationTime' yalnızca
            // SkyShader modülünün kendi render'ı çalışırken artıyordu; mod kapalıyken animasyon
            // donuyordu. Saat tabanlı fallback bunu sürekli canlı tutar.
            float time = skyShader != null && skyShader.isEnabled()
                    ? skyShader.getAnimationTime()
                    : ((System.currentTimeMillis() % 60000L) / 1000f);

            var camPos = camera.getPos();
            double fov = mc.options.getFov().getValue();
            int bufW = mc.getWindow().getFramebufferWidth();
            int bufH = mc.getWindow().getFramebufferHeight();

            float mode = skyShader != null ? skyShader.getModeIndex() : 0.0f;
            float intensity = skyShader != null ? skyShader.getIntensity() : 1.0f;
            float fr = skyShader != null ? skyShader.getFirstR() : 0.55f;
            float fg = skyShader != null ? skyShader.getFirstG() : 0.30f;
            float fb = skyShader != null ? skyShader.getFirstB() : 0.90f;
            float sr = skyShader != null ? skyShader.getSecondR() : 0.35f;
            float sg = skyShader != null ? skyShader.getSecondG() : 0.55f;
            float sb = skyShader != null ? skyShader.getSecondB() : 1.0f;
            float pr = skyShader != null ? skyShader.getPurpleR() : 0.727f;
            float pg = skyShader != null ? skyShader.getPurpleG() : 0.221f;
            float pb = skyShader != null ? skyShader.getPurpleB() : 0.765f;

            program.getUniform("Mode").set(mode);
            program.getUniform("time").set(time);
            program.getUniform("CameraPosition").set((float) camPos.x, (float) camPos.y, (float) camPos.z);
            program.getUniform("SkyPitch").set((float) Math.toRadians(camera.getPitch()));
            program.getUniform("SkyYaw").set((float) Math.toRadians(camera.getYaw()));
            program.getUniform("TanHalfFov").set((float) Math.tan(Math.toRadians(fov) / 2.0));
            program.getUniform("Aspect").set(bufH > 0 ? (float) bufW / (float) bufH : 1.0f);
            program.getUniform("Intensity").set(intensity);
            program.getUniform("FirstColor").set(fr, fg, fb, 1.0f);
            program.getUniform("SecondColor").set(sr, sg, sb, 1.0f);
            program.getUniform("PurpleColor").set(pr, pg, pb, 1.0f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}