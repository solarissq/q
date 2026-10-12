package com.deccalware.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public class PolarBypass {

    private final Random rand = new Random();

    private float currentYaw;
    private float currentPitch;
    private float targetYaw;
    private float targetPitch;

    private float pendingYawDelta;
    private float pendingPitchDelta;

    private float bezierProgress;
    private float bezierSpeed;
    private boolean bezierActive;

    private float b0Yaw, b0Pitch;
    private float b1Yaw, b1Pitch;
    private float b2Yaw, b2Pitch;
    private float b3Yaw, b3Pitch;

    private int jitterTicks;
    private int yawJitterDir;
    private int pitchJitterDir;
    private int yawJitterAccum;
    private int pitchJitterAccum;

    private float savedYaw;
    private float savedPitch;
    private float savedPrevYaw;
    private float savedPrevPitch;

    private boolean complete;

    private static final float TOLERANCE = 1.5f;
    private static final float MIN_STEP = 0.01f;

    public PolarBypass() {
        this.complete = true;
    }

    public void startRotation(float currentYaw, float currentPitch, float targetYaw, float targetPitch, float speed) {
        this.currentYaw = currentYaw;
        this.currentPitch = currentPitch;
        this.targetYaw = targetYaw;
        this.targetPitch = targetPitch;
        this.pendingYawDelta = 0;
        this.pendingPitchDelta = 0;
        this.complete = false;
        this.bezierActive = true;
        this.bezierProgress = 0;
        this.bezierSpeed = Math.max(0.02f, Math.min(speed, 0.8f));

        float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        float jitter = 0.15f + rand.nextFloat() * 0.2f;

        this.b0Yaw = currentYaw;
        this.b0Pitch = currentPitch;

        this.b1Yaw = currentYaw + yawDiff * (0.15f + rand.nextFloat() * jitter)
                + (rand.nextFloat() - 0.5f) * Math.abs(yawDiff) * jitter;
        this.b1Pitch = currentPitch + pitchDiff * (0.15f + rand.nextFloat() * jitter)
                + (rand.nextFloat() - 0.5f) * Math.abs(pitchDiff) * jitter;

        this.b2Yaw = currentYaw + yawDiff * (0.55f + rand.nextFloat() * jitter)
                + (rand.nextFloat() - 0.5f) * Math.abs(yawDiff) * jitter;
        this.b2Pitch = currentPitch + pitchDiff * (0.55f + rand.nextFloat() * jitter)
                + (rand.nextFloat() - 0.5f) * Math.abs(pitchDiff) * jitter;

        this.b3Yaw = targetYaw;
        this.b3Pitch = targetPitch;
    }

    public void captureRotation(MinecraftClient mc) {
        if (mc.player == null) return;
        this.savedYaw = mc.player.getYaw();
        this.savedPitch = mc.player.getPitch();
        this.savedPrevYaw = mc.player.prevYaw;
        this.savedPrevPitch = mc.player.prevPitch;
    }

    public void restoreRotation(MinecraftClient mc) {
        if (mc.player == null) return;
        mc.player.setYaw(this.savedYaw);
        mc.player.setPitch(this.savedPitch);
        mc.player.prevYaw = this.savedPrevYaw;
        mc.player.prevPitch = this.savedPrevPitch;
    }

    public void applyPendingMovement(MinecraftClient mc) {
        if (mc.player == null || complete) return;

        updateJitter();

        float adjustedYaw = this.pendingYawDelta + (float) this.yawJitterAccum;
        float adjustedPitch = this.pendingPitchDelta + (float) this.pitchJitterAccum;

        int yawSteps = (int) adjustedYaw;
        int pitchSteps = (int) adjustedPitch;

        float remainYaw = adjustedYaw - (float) yawSteps;
        float remainPitch = adjustedPitch - (float) pitchSteps;

        float mouseScale = getMouseScale();
        float yawDelta = (float) yawSteps * mouseScale * 0.15f;
        float pitchDelta = (float) pitchSteps * mouseScale * 0.15f;

        mc.player.setYaw(mc.player.getYaw() + yawDelta);
        mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() - pitchDelta, -90.0f, 90.0f));

        this.pendingYawDelta = remainYaw;
        this.pendingPitchDelta = remainPitch;
        this.yawJitterAccum = 0;
        this.pitchJitterAccum = 0;
    }

    public boolean update() {
        if (complete) return true;

        float mouseScale = getMouseScale();
        float rotPerStep = mouseScale * 0.15f;

        float predictedYaw = currentYaw + (float) (int) pendingYawDelta * mouseScale * 0.15f;
        float predictedPitch = currentPitch - (float) (int) (-pendingPitchDelta) * mouseScale * 0.15f;

        float yawError = MathHelper.wrapDegrees(targetYaw - predictedYaw);
        float pitchError = targetPitch - predictedPitch;

        float absYawErr = Math.abs(yawError);
        float absPitchErr = Math.abs(pitchError);

        if (absYawErr < TOLERANCE && absPitchErr < TOLERANCE) {
            this.complete = true;
            return true;
        }

        float yawStep = bezierSpeed * 0.25f;
        float pitchStep = bezierSpeed * 0.25f;

        if (bezierActive) {
            float t = bezierProgress;
            float mt = 1.0f - t;
            float mt2 = mt * mt;
            float mt3 = mt2 * mt;
            float t2 = t * t;
            float t3 = t2 * t;

            float bezierYaw = mt3 * b0Yaw + 3 * mt2 * t * b1Yaw + 3 * mt * t2 * b2Yaw + t3 * b3Yaw;
            float bezierPitch = mt3 * b0Pitch + 3 * mt2 * t * b1Pitch + 3 * mt * t2 * b2Pitch + t3 * b3Pitch;

            float bYawDiff = MathHelper.wrapDegrees(bezierYaw - predictedYaw);
            float bPitchDiff = bezierPitch - predictedPitch;

            yawStep = Math.abs(bYawDiff) * 0.3f + rand.nextFloat() * 0.15f;
            pitchStep = Math.abs(bPitchDiff) * 0.3f + rand.nextFloat() * 0.1f;

            yawStep = Math.max(yawStep, MIN_STEP);
            pitchStep = Math.max(pitchStep, MIN_STEP);

            yawStep = Math.min(yawStep, 3.0f);
            pitchStep = Math.min(pitchStep, 2.0f);

            float maxSteps = Math.max(8.0f, 30.0f / Math.max(bezierSpeed, 0.01f));
            bezierProgress += 1.0f / maxSteps;
            if (bezierProgress >= 1.0f) {
                bezierProgress = 1.0f;
                bezierActive = false;
            }
        } else {
            float distFactor = Math.max(absYawErr, absPitchErr) / 90.0f;
            float accel = 1.0f + distFactor * 1.5f;
            yawStep *= accel;
            pitchStep *= accel;

            yawStep = Math.max(yawStep, MIN_STEP);
            pitchStep = Math.max(pitchStep, MIN_STEP);
            yawStep = Math.min(yawStep, 2.5f);
            pitchStep = Math.min(pitchStep, 1.8f);
        }

        yawStep *= (0.85f + rand.nextFloat() * 0.3f);
        pitchStep *= (0.85f + rand.nextFloat() * 0.3f);

        this.pendingYawDelta = yawError > 0
                ? this.pendingYawDelta + yawStep
                : this.pendingYawDelta - yawStep;
        this.pendingPitchDelta = pitchError > 0
                ? this.pendingPitchDelta + pitchStep
                : this.pendingPitchDelta - pitchStep;

        this.currentYaw = predictedYaw;
        this.currentPitch = predictedPitch;

        return false;
    }

    private void updateJitter() {
        this.jitterTicks++;
        if (this.jitterTicks >= 200 + this.rand.nextInt(80)) {
            this.jitterTicks = -50 - this.rand.nextInt(50);
            this.yawJitterDir = this.rand.nextInt(3) - 1;
            this.pitchJitterDir = this.rand.nextInt(3) - 1;
        }

        int yawStep = this.yawJitterDir;
        int pitchStep = this.pitchJitterDir;

        if (this.rand.nextInt(10) < 3) yawStep = 0;
        if (this.rand.nextInt(10) < 3) pitchStep = 0;
        if (this.jitterTicks < 0) {
            yawStep = 0;
            pitchStep = 0;
        }

        if (this.rand.nextInt(15) == 1) {
            this.yawJitterAccum += yawStep;
            this.pitchJitterAccum += pitchStep;
        }

        if (this.pendingYawDelta > 0 && this.yawJitterAccum < 0
                || this.pendingYawDelta < 0 && this.yawJitterAccum > 0) {
            this.yawJitterAccum = 0;
        }
        if (this.pendingPitchDelta > 0 && this.pitchJitterAccum < 0
                || this.pendingPitchDelta < 0 && this.pitchJitterAccum > 0) {
            this.pitchJitterAccum = 0;
        }
    }

    public float getMouseScale() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return 1.0f;
        float sens = mc.options.getMouseSensitivity().getValue().floatValue();
        float f = sens * 0.6f + 0.2f;
        return f * f * f * 8.0f;
    }

    public boolean isComplete() {
        return complete;
    }

    public void cancel() {
        this.complete = true;
        this.bezierActive = false;
        this.pendingYawDelta = 0;
        this.pendingPitchDelta = 0;
    }

    public float getTargetYaw() { return targetYaw; }
    public float getTargetPitch() { return targetPitch; }
}
