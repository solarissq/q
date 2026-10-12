package com.deccalware.deccal.manager;

import com.deccalware.deccal.util.render.DeccalShaderRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

@SuppressWarnings("All")
public interface IMinecraft {
    MinecraftClient mc = MinecraftClient.getInstance();
    ThreadLocal<BufferBuilder> CURRENT_BUFFER = new ThreadLocal<>();
    ThreadLocal<VertexFormat> CURRENT_FORMAT = new ThreadLocal<>();

    static Tessellator tessellator() {
        return Tessellator.getInstance();
    }

    static BufferBuilder buffer() {
        BufferBuilder buf = CURRENT_BUFFER.get();
        if (buf == null) throw new IllegalStateException("No buffer — call begin() first");
        return buf;
    }

    static BufferBuilder begin(VertexFormat.DrawMode mode, VertexFormat format) {
        BufferBuilder buf = tessellator().begin(mode, format);
        CURRENT_BUFFER.set(buf);
        CURRENT_FORMAT.set(format);
        return buf;
    }

    static void draw(BufferBuilder buffer) {
        // 1.21.4'te BufferRenderer.drawWithGlobalProgram, RenderSystem.setShader ile basilan global
        // programi kullanir.
        //
        // KURAL 1: DeccalShaderRegistry custom programlari (rectangle/glass/border/blur/texture)
        // bagliysa onu ASLA ezme. Ozel shader'lar (JSON gibi gorunen arayuzu uretir) yalnizca kendi
        // vertex akisiyla tutarlidir; ustune POSITION_* baglaninca gri ve saydamliksiz gorunuyor.
        // KURAL 2: bunlarin disinda kalan (vanilla/stock ya da bayat) programlar gecersizdir:
        // olsa bile buffer'in gercek formatina gore dogru programla degistir. Asil sorun sudur:
        // onceki modul (ornegin Backtrack) POSITION_COLOR baglayip farkli bir modul (TargetESP'nin
        // POSITION_TEXTURE_COLOR quads'i) cizerken art arda render edilince texture okunmuyor ve
        // duz RGB kareler olusuyordu.
        net.minecraft.client.render.BuiltBuffer built = buffer.endNullable();
        if (built == null) {
            return;
        }
        ShaderProgram bound = RenderSystem.getShader();
        if (bound != null && isDeccalCustom(bound)) {
            BufferRenderer.drawWithGlobalProgram(built);
            return;
        }
        VertexFormat format = built.getDrawParameters().format();
        if (format == VertexFormats.POSITION_TEXTURE_COLOR || format == VertexFormats.POSITION_TEXTURE_LIGHT_COLOR) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        } else {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        }
        BufferRenderer.drawWithGlobalProgram(built);
    }

    private static boolean isDeccalCustom(ShaderProgram bound) {
        return bound == DeccalShaderRegistry.texture
                || bound == DeccalShaderRegistry.rectangle
                || bound == DeccalShaderRegistry.blur
                || bound == DeccalShaderRegistry.border
                || bound == DeccalShaderRegistry.glass
                || bound == DeccalShaderRegistry.entitySky;
    }

    static float tickDelta() {
        return mc.getRenderTickCounter().getTickDelta(true);
    }
}
