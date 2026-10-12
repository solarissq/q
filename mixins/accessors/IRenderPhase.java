package com.deccalware.mixins.accessors;

import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderPhase.class)
public interface IRenderPhase {
    @Accessor("NO_TEXTURE")
    static RenderPhase.TextureBase deccalware$noTexture() {
        throw new AssertionError();
    }

    @Accessor("ENABLE_CULLING")
    static RenderPhase.Cull deccalware$culling() {
        throw new AssertionError();
    }

    @Accessor("LEQUAL_DEPTH_TEST")
    static RenderPhase.DepthTest deccalware$depthTest() {
        throw new AssertionError();
    }

    @Accessor("TRANSLUCENT_TRANSPARENCY")
    static RenderPhase.Transparency deccalware$transparency() {
        throw new AssertionError();
    }

    @Accessor("ENABLE_LIGHTMAP")
    static RenderPhase.Lightmap deccalware$lightmap() {
        throw new AssertionError();
    }

    @Accessor("ENABLE_OVERLAY_COLOR")
    static RenderPhase.Overlay deccalware$overlay() {
        throw new AssertionError();
    }

    @Accessor("NO_LAYERING")
    static RenderPhase.Layering deccalware$layering() {
        throw new AssertionError();
    }

    @Accessor("MAIN_TARGET")
    static RenderPhase.Target deccalware$target() {
        throw new AssertionError();
    }

    @Accessor("DEFAULT_TEXTURING")
    static RenderPhase.Texturing deccalware$texturing() {
        throw new AssertionError();
    }

    @Accessor("COLOR_MASK")
    static RenderPhase.WriteMaskState deccalware$writeMask() {
        throw new AssertionError();
    }

    @Accessor("FULL_LINE_WIDTH")
    static RenderPhase.LineWidth deccalware$lineWidth() {
        throw new AssertionError();
    }

    @Accessor("NO_COLOR_LOGIC")
    static RenderPhase.ColorLogic deccalware$colorLogic() {
        throw new AssertionError();
    }
}