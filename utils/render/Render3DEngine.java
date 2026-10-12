package com.deccalware.utils.render;

import net.minecraft.client.MinecraftClient;

/**
 * Minimal stub implementation of Render3DEngine used for shader uniform updates.
 * Only provides the getTickDelta method required by ShaderManager.
 */
public class Render3DEngine {
    /**
     * Returns the current tick delta from the Minecraft client.
     */
    public static float getTickDelta() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.getRenderTickCounter().getTickDelta(true);
    }
}
