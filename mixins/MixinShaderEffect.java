package com.deccalware.mixins;

import org.spongepowered.asm.mixin.Unique;
import com.deccalware.mixins.accessors.IPostProcessShader;
import com.deccalware.utils.interfaces.IShaderEffect;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(PostEffectProcessor.class)
public class MixinShaderEffect implements IShaderEffect {
    @Shadow @Final private List<PostEffectPass> passes;

    @Override
    public void addFakeTargetHook(String name, net.minecraft.client.gl.Framebuffer buffer) {
        // PostEffectProcessor target management changed in 1.21.4.
        // Targets are now managed via FrameGraphBuilder, not direct maps.
    }
}
