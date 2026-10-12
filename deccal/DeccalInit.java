package com.deccalware.deccal;

import com.deccalware.deccal.manager.DeccalManager;
import com.deccalware.deccal.util.render.DeccalShaderRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public final class DeccalInit {
    private static boolean initialized;
    private static boolean shadersInitialized;

    private DeccalInit() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        DeccalManager.init();
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            if (!shadersInitialized) {
                DeccalShaderRegistry.init();
                shadersInitialized = true;
            }
        });
        initialized = true;
    }
}
