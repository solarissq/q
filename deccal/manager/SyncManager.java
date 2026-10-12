package com.deccalware.deccal.manager;

import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.math.MathHelper;

public final class SyncManager implements IMinecraft {
    public float tps = 20f;
    private long lastTime = System.nanoTime();
    private static final float NANOS_IN_SECOND = 1_000_000_000F;

    public void onPacket(Object packet) {
        if (packet instanceof WorldTimeUpdateS2CPacket) {
            long now = System.nanoTime();
            long delay = now - lastTime;
            lastTime = now;
            if (delay <= 0L) {
                return;
            }
            float currentTPS = 20F * (NANOS_IN_SECOND / delay);
            currentTPS = MathHelper.clamp(currentTPS, 0F, 20F);
            tps += (currentTPS - tps) * 0.05F;
        }
    }
}
