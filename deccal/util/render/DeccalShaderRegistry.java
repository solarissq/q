package com.deccalware.deccal.util.render;

import com.deccalware.mixins.accessors.IRenderLayer;
import com.deccalware.mixins.accessors.IRenderPhase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.CompiledShader;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramDefinition;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DeccalShaderRegistry {
    public static ShaderProgram texture;
    public static ShaderProgram rectangle;
    public static ShaderProgram blur;
    public static ShaderProgram border;
    public static ShaderProgram glass;
    public static ShaderProgram entitySky;

    public static RenderLayer entitySkyLayer;

    public static final ShaderProgramKey ENTITY_SKY_KEY = new ShaderProgramKey(
            Identifier.of("deccalware", "entity_sky"),
            VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
            Defines.EMPTY);

    private static final Pattern MOJ_IMPORT_PATTERN = Pattern.compile("#moj_import\\s+<([^>]+)>");

    private DeccalShaderRegistry() {
    }

    public static void init() {
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            texture = load(client, VertexFormats.POSITION_TEXTURE_COLOR,
                    new String[][]{
                            {"ModelViewMat", "matrix4x4", "16"},
                            {"ProjMat", "matrix4x4", "16"},
                            {"Size", "float", "2"},
                            {"Radius", "float", "4"},
                            {"Smoothness", "float", "1"},
                    },
                    "shaders/core/position_texture_color.vsh", "shaders/core/texture/fragment.fsh");

            rectangle = load(client, VertexFormats.POSITION_COLOR,
                    new String[][]{
                            {"ModelViewMat", "matrix4x4", "16"},
                            {"ProjMat", "matrix4x4", "16"},
                            {"Size", "float", "2"},
                            {"Radius", "float", "4"},
                            {"Smoothness", "float", "1"},
                    },
                    "shaders/core/position_color.vsh", "shaders/core/rectangle/fragment.fsh");

            blur = load(client, VertexFormats.POSITION_COLOR,
                    new String[][]{
                            {"ModelViewMat", "matrix4x4", "16"},
                            {"ProjMat", "matrix4x4", "16"},
                            {"Size", "float", "2"},
                            {"Radius", "float", "4"},
                            {"Smoothness", "float", "1"},
                            {"BlurRadius", "float", "1"},
                    },
                    "shaders/core/blur/vertex.vsh", "shaders/core/blur/fragment.fsh");

            border = load(client, VertexFormats.POSITION_COLOR,
                    new String[][]{
                            {"ModelViewMat", "matrix4x4", "16"},
                            {"ProjMat", "matrix4x4", "16"},
                            {"Size", "float", "2"},
                            {"Radius", "float", "4"},
                            {"Thickness", "float", "1"},
                            {"Smoothness", "float", "2"},
                    },
                    "shaders/core/position_color.vsh", "shaders/core/border/fragment.fsh");

            glass = load(client, VertexFormats.POSITION_TEXTURE_COLOR,
                    new String[][]{
                            {"ModelViewMat", "matrix4x4", "16"},
                            {"ProjMat", "matrix4x4", "16"},
                            {"Size", "float", "2"},
                            {"Radius", "float", "4"},
                            {"Smoothness", "float", "1"},
                            {"CornerSmoothness", "float", "1"},
                            {"GlobalAlpha", "float", "1"},
                            {"FresnelPower", "float", "1"},
                            {"FresnelColor", "float", "3"},
                            {"FresnelAlpha", "float", "1"},
                            {"BaseAlpha", "float", "1"},
                            {"FresnelInvert", "int", "1"},
                            {"FresnelMix", "float", "1"},
                            {"DistortStrength", "float", "1"},
                    },
                    "shaders/core/glass/vertex.vsh", "shaders/core/glass/fragment.fsh");

            entitySky = load(client, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                    new String[][]{
                            {"ModelViewMat", "matrix4x4", "16"},
                            {"ProjMat", "matrix4x4", "16"},
                            {"Mode", "float", "1"},
                            {"time", "float", "1"},
                            {"CameraPosition", "float", "3"},
                            {"SkyPitch", "float", "1"},
                            {"SkyYaw", "float", "1"},
                            {"TanHalfFov", "float", "1"},
                            {"Aspect", "float", "1"},
                            {"Intensity", "float", "1"},
                            {"FirstColor", "float", "4"},
                            {"SecondColor", "float", "4"},
                            {"PurpleColor", "float", "4"},
                    },
                    "shaders/core/entity_sky/vertex.vsh", "shaders/core/entity_sky/fragment.fsh");

            entitySkyLayer = buildEntitySkyLayer();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Deccal shaders", e);
        }
    }

    private static RenderLayer buildEntitySkyLayer() {
        RenderLayer.MultiPhaseParameters params = RenderLayer.MultiPhaseParameters.builder()
                .texture(IRenderPhase.deccalware$noTexture())
                .program(new RenderPhase.ShaderProgram(ENTITY_SKY_KEY))
                .transparency(IRenderPhase.deccalware$transparency())
                .depthTest(IRenderPhase.deccalware$depthTest())
                .cull(IRenderPhase.deccalware$culling())
                .lightmap(IRenderPhase.deccalware$lightmap())
                .overlay(IRenderPhase.deccalware$overlay())
                .layering(IRenderPhase.deccalware$layering())
                .target(IRenderPhase.deccalware$target())
                .texturing(IRenderPhase.deccalware$texturing())
                .writeMaskState(IRenderPhase.deccalware$writeMask())
                .lineWidth(IRenderPhase.deccalware$lineWidth())
                .colorLogic(IRenderPhase.deccalware$colorLogic())
                .build(RenderLayer.OutlineMode.NONE);
        return IRenderLayer.deccalware$of(
                "deccalware:entity_sky",
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS,
                512, false, true, params);
    }

    private static ShaderProgram load(MinecraftClient client, VertexFormat format, String[][] uniformDefs,
                                      String vertexPath, String fragmentPath) throws Exception {
        Identifier vshId = Identifier.of("deccalware", vertexPath);
        Identifier fshId = Identifier.of("deccalware", fragmentPath);

        String vshSource = resolveShaderSource(client, vshId);
        String fshSource = resolveShaderSource(client, fshId);

        CompiledShader vertShader = CompiledShader.compile(vshId, CompiledShader.Type.VERTEX, vshSource);
        CompiledShader fragShader = CompiledShader.compile(fshId, CompiledShader.Type.FRAGMENT, fshSource);

        ShaderProgram program = ShaderProgram.create(vertShader, fragShader, format);

        List<ShaderProgramDefinition.Uniform> uniforms = new ArrayList<>();
        for (String[] def : uniformDefs) {
            uniforms.add(new ShaderProgramDefinition.Uniform(def[0], def[1], Integer.parseInt(def[2]), List.of()));
        }
        program.set(uniforms, List.of(new ShaderProgramDefinition.Sampler("Sampler0")));

        return program;
    }

    private static String resolveShaderSource(MinecraftClient client, Identifier id) throws Exception {
        Resource resource = client.getResourceManager().getResourceOrThrow(id);
        String source;
        try (InputStream stream = resource.getInputStream()) {
            source = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }

        Matcher matcher = MOJ_IMPORT_PATTERN.matcher(source);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String importTarget = matcher.group(1);
            Identifier includeId;
            if (importTarget.startsWith("deccalware:") || importTarget.startsWith("zenith:")) {
                String name = importTarget.substring(importTarget.indexOf(':') + 1);
                includeId = Identifier.of("deccalware", "shaders/include/" + name);
            } else if (importTarget.contains(":")) {
                String[] parts = importTarget.split(":", 2);
                includeId = Identifier.of(parts[0], "shaders/include/" + parts[1]);
            } else {
                includeId = Identifier.ofVanilla("shaders/include/" + importTarget);
            }

            String includedContent = resolveShaderSource(client, includeId);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(includedContent));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}