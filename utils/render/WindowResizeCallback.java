package com.deccalware.utils.render;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

/**
 * Simple window resize callback event used by the shader system.
 */
public interface WindowResizeCallback {
    void onWindowResize(MinecraftClient client, Window window);

    Event<WindowResizeCallback> EVENT = EventFactory.createArrayBacked(WindowResizeCallback.class,
            callbacks -> (client, window) -> {
                for (WindowResizeCallback callback : callbacks) {
                    callback.onWindowResize(client, window);
                }
            }
    );
}
