package com.deccalware.mixins.accessors;

import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PostEffectPass.class)
public interface IPostProcessShader {
    @Accessor("program")
    ShaderProgram getProgram();

    @Accessor("outputTargetId")
    Identifier getOutputTargetId();
}
