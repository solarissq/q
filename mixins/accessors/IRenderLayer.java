package com.deccalware.mixins.accessors;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderLayer.class)
public interface IRenderLayer {
    @Invoker("of")
    static RenderLayer.MultiPhase deccalware$of(String name, VertexFormat vertexFormat,
            VertexFormat.DrawMode drawMode, int expectedBufferSize,
            boolean hasCrumbling, boolean translucent,
            RenderLayer.MultiPhaseParameters phaseData) {
        throw new AssertionError();
    }
}