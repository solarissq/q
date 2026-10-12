package com.deccalware.managers;

public class RotationManager {
    public float yaw, pitch;
    public boolean active;
    private boolean lastActive;

    public void setRotations(float yaw, float pitch) {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player != null) {
            float baseYaw = active ? this.yaw : mc.player.getYaw();
            this.yaw = baseYaw + net.minecraft.util.math.MathHelper.wrapDegrees(yaw - baseYaw);
        } else {
            this.yaw = yaw;
        }
        this.pitch = net.minecraft.util.math.MathHelper.clamp(pitch, -89.9f, 89.9f);
        this.active = true;
    }

    public boolean isUpdating() {
        return active;
    }

    public void reset() {
        this.active = false;
    }

    public float getYaw() {
        return active ? yaw
                : (net.minecraft.client.MinecraftClient.getInstance().player != null
                        ? net.minecraft.client.MinecraftClient.getInstance().player.getYaw()
                        : 0);
    }

    public float getPitch() {
        return active ? pitch
                : (net.minecraft.client.MinecraftClient.getInstance().player != null
                        ? net.minecraft.client.MinecraftClient.getInstance().player.getPitch()
                        : 0);
    }
}
