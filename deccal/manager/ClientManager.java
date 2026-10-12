package com.deccalware.deccal.manager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import com.deccalware.deccal.util.KeyMappings;
import com.deccalware.deccal.util.math.MathUtil;

import java.util.Locale;

public final class ClientManager implements IMinecraft {
    private static float fps;

    private ClientManager() {
    }

    public static int getFps() {
        int currentFps = mc != null ? mc.getCurrentFps() : 0;
        fps = MathUtil.fast(fps, currentFps, 6);
        return Math.round(fps);
    }

    public static String getBps(Entity entity) {
        if (mc == null || entity == null) {
            return "0.00";
        }
        double dx = entity.getX() - entity.prevX;
        double dz = entity.getZ() - entity.prevZ;
        return String.format(Locale.ROOT, "%.2f", Math.hypot(dx, dz) * 20.0D);
    }

    public static String getPing() {
        if (mc == null || mc.player == null || mc.getNetworkHandler() == null) {
            return "N/A";
        }
        var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry == null ? "N/A" : Integer.toString(entry.getLatency());
    }

    public static String getKey(int keyCode) {
        return KeyMappings.keyMappings(keyCode);
    }
}
