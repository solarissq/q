package com.deccalware.mixins;

import com.deccalware.DeccaLWare;
import com.deccalware.modules.visual.SkyShader;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.render.FrameGraphBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PostEffectPass.class)
public class PostEffectPassMixin {

    @Inject(method = "render(Lnet/minecraft/client/render/FrameGraphBuilder;Ljava/util/Map;Lorg/joml/Matrix4f;)V", at = @At("HEAD"))
    private void deccalware_setSkyUniforms(FrameGraphBuilder builder, java.util.Map<net.minecraft.util.Identifier, net.minecraft.client.util.Handle<net.minecraft.client.gl.Framebuffer>> framebuffers, org.joml.Matrix4f matrix, CallbackInfo ci) {
        try {
            PostEffectPass self = (PostEffectPass) (Object) this;
            ShaderProgram program = self.getProgram();
            if (program == null) return;
            SkyShader skyShader = (SkyShader) DeccaLWare.INSTANCE.moduleManager.getModuleByName("SkyShader");
            if (skyShader == null || !skyShader.isEnabled()) return;
            skyShader.applyUniforms(program);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
