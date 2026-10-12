package com.deccalware.mixins;

import com.deccalware.DeccaLWare;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends net.minecraft.entity.player.PlayerEntity {

    private float deccalRealYaw;
    private float deccalRealPitch;
    private boolean deccalRotSwapped = false;

    private float deccalTickRealYaw;
    private float deccalTickRealPitch;
    private boolean deccalTickSwapped = false;

    private static final float[][] DISCRETE_INPUTS = {
        {1.0f, 0.0f},   // Forward
        {1.0f, -1.0f},  // Forward-Right
        {0.0f, -1.0f},  // Right
        {-1.0f, -1.0f}, // Back-Right
        {-1.0f, 0.0f},  // Back
        {-1.0f, 1.0f},  // Back-Left
        {0.0f, 1.0f},   // Left
        {1.0f, 1.0f}    // Forward-Left
    };

    public ClientPlayerEntityMixin(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, float yaw,
            com.mojang.authlib.GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean isUsingItem(ClientPlayerEntity instance) {
        if (DeccaLWare.INSTANCE.moduleManager.getModuleByName("NoSlow").isEnabled()) {
            return false;
        }
        return instance.isUsingItem();
    }

    private static Vec3d getMovementVector(float yaw, float forward, float sideways) {
        float rad = yaw * 0.017453292F;
        float sin = MathHelper.sin(rad);
        float cos = MathHelper.cos(rad);
        double x = sideways * cos - forward * sin;
        double z = forward * cos + sideways * sin;
        return new Vec3d(x, 0, z);
    }

    private static float[] getBestDiscreteInput(float realYaw, float targetYaw, float rawForward, float rawSideways) {
        Vec3d desired = getMovementVector(realYaw, rawForward, rawSideways);
        double desiredLen = Math.hypot(desired.x, desired.z);
        if (desiredLen < 1e-5) {
            return new float[]{0.0f, 0.0f};
        }
        Vec3d desiredNorm = desired.multiply(1.0 / desiredLen);

        float bestF = rawForward;
        float bestS = rawSideways;
        double maxDot = -Double.MAX_VALUE;

        for (float[] inp : DISCRETE_INPUTS) {
            Vec3d cand = getMovementVector(targetYaw, inp[0], inp[1]);
            double candLen = Math.hypot(cand.x, cand.z);
            if (candLen < 1e-5) continue;
            Vec3d candNorm = cand.multiply(1.0 / candLen);
            double dot = desiredNorm.x * candNorm.x + desiredNorm.z * candNorm.z;
            if (dot > maxDot) {
                maxDot = dot;
                bestF = inp[0];
                bestS = inp[1];
            }
        }
        return new float[]{bestF, bestS};
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTickMovementHead(CallbackInfo ci) {
        try {
            ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
            // FreeCam 'Freeze Body': vucut WASD ile yuruyemesin (TURN only) -> disable'da
            // geri ziplama (rubberband) imkansiz olur. Kamera tuslari bu input objesini
            // degil, options'i (mc.options.forwardKey...) okur -> goruntu etkilenmez.
            com.deccalware.modules.visual.FreeCam fcam = (com.deccalware.modules.visual.FreeCam)
                    DeccaLWare.INSTANCE.moduleManager.getModuleByName("FreeCam");
            if (fcam != null && fcam.isEnabled() && fcam.freeze.getValue() && self.input != null) {
                self.input.movementForward = 0.0f;
                self.input.movementSideways = 0.0f;
                self.input.playerInput = new net.minecraft.util.PlayerInput(false, false, false, false, false, false, false);
            }
            com.deccalware.modules.combat.KillAura ka = (com.deccalware.modules.combat.KillAura)
                    DeccaLWare.INSTANCE.moduleManager.getModuleByName("KillAura");
            if (ka != null && ka.isEnabled() && ka.silent.getValue()) {
                ka.ensureAimUpToDate();
                if (com.deccalware.modules.combat.KillAura.target != null && self.input != null) {
                    float targetYaw = ka.targetYaw;
                    if (!Float.isNaN(targetYaw)) {
                        String mode = ka.moveFix.getValue();
                        if (mode.equals("Target")) {
                            deccalTickRealYaw = self.getYaw();
                            deccalTickRealPitch = self.getPitch();
                            self.setYaw(targetYaw);
                            deccalTickSwapped = true;
                        } else if (mode.equals("Camera")) {
                            float forward = self.input.movementForward;
                            float sideways = self.input.movementSideways;
                            if (forward != 0 || sideways != 0) {
                                float realYaw = self.getYaw();
                                float[] best = getBestDiscreteInput(realYaw, targetYaw, forward, sideways);
                                self.input.movementForward = best[0];
                                self.input.movementSideways = best[1];
                                deccalTickRealYaw = realYaw;
                                deccalTickRealPitch = self.getPitch();
                                self.setYaw(targetYaw);
                                deccalTickSwapped = true;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @Inject(method = "tickMovement", at = @At("RETURN"))
    private void onTickMovementReturn(CallbackInfo ci) {
        if (deccalTickSwapped) {
            ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
            self.setYaw(deccalTickRealYaw);
            self.setPitch(deccalTickRealPitch);
            deccalTickSwapped = false;
        }
    }

// FreeCam "Freeze Body" = Blink tarzı donma (sadece keşif sırasinda).
    // Interact penceresi acikken gövdeyi DONDURMA: gövde gerçek yerçekimi/onGround
    // ile çalişir ki Grim oyuncuyu havada ("pre-flying") sanmasin -> RotationPlace
    // ve PositionPlace flag'leri kaybolur. Blink sadece tuş basili degilken aktif.
    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    private void onFreeCamFreezeTick(CallbackInfo ci) {
        com.deccalware.modules.visual.FreeCam fc = (com.deccalware.modules.visual.FreeCam)
                DeccaLWare.INSTANCE.moduleManager.getModuleByName("FreeCam");
        if (fc != null && fc.isEnabled() && fc.freeze.getValue() && (!fc.isInteracting() || fc.isSpoofing())) {
            ci.cancel();
        }
    }

    // Freeze aktifken konum paketlerini yollama (Blink): server son gördüğü konumda tutar.
    // Ancak Grim-safe interact penceresi acikken iptal etME: spoof edilen rotasyon
    // LookAndOnGround paketiyle gitsin diye. Gövde fizigi zaten iptal oldugu icin
    // konum paketi gecmez, sadece rotasyon (Look) paketi gecer -> rotasyon flag yemez.
    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void onFreeCamFreezeSend(CallbackInfo ci) {
        com.deccalware.modules.visual.FreeCam fc = (com.deccalware.modules.visual.FreeCam)
                DeccaLWare.INSTANCE.moduleManager.getModuleByName("FreeCam");
        if (fc != null && fc.isEnabled() && fc.freeze.getValue() && (!fc.isInteracting() || fc.isSpoofing())) {
            ci.cancel();
        }
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void onSendMovementPacketsHead(CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        com.deccalware.modules.combat.KillAura ka = (com.deccalware.modules.combat.KillAura)
                DeccaLWare.INSTANCE.moduleManager.getModuleByName("KillAura");
        if (ka != null && ka.isEnabled()) {
            ka.ensureAimUpToDate();
        }
        // AutoWeb yerlesim penceresi acikken rotasyonu web yuzune cek.
        // KillAura ensureAimUpToDate her tick rotationManager'i kendi hedefine yazar;
        // bu block, AutoWeb'in kendi face(hitVec) rotasyonunu swap'tan ONCE geri koyar,
        // Boylece yerlesim paketi ile movement rotasyonu ayni yuzu gosterir (Grim-safe).
        com.deccalware.modules.combat.AutoWeb aw = (com.deccalware.modules.combat.AutoWeb)
                DeccaLWare.INSTANCE.moduleManager.getModuleByName("AutoWeb");
        if (aw != null && aw.isEnabled() && aw.isPlaceWindow()
                && DeccaLWare.INSTANCE.rotationManager != null) {
            float[] awRot = aw.getCurrentPlaceRotation();
            if (awRot != null) {
                DeccaLWare.INSTANCE.rotationManager.setRotations(awRot[0], awRot[1]);
            }
        }
        // FreeCam interact: kırma/koyma penceresi acikken rotasyonu crosshairTarget'e hizala.
        // Boylemce asagi yukari ayni tick icinde yollanan LookAndOnGround, server'a dogru
        // rotasyonu gosterir (Grim rotationPlace/positionPlace icin kritik). Tail'de clearAim
        // ile rotationManager sifirlanir, kalici takili kalmaz.
        com.deccalware.modules.visual.FreeCam fci = (com.deccalware.modules.visual.FreeCam)
                DeccaLWare.INSTANCE.moduleManager.getModuleByName("FreeCam");
        if (fci != null && fci.isEnabled() && fci.interact.getValue() && fci.isInteracting()) {
            fci.aimAtTarget();
        }
        if (DeccaLWare.INSTANCE.rotationManager != null && DeccaLWare.INSTANCE.rotationManager.active) {
            deccalRealYaw = self.getYaw();
            deccalRealPitch = self.getPitch();
            self.setYaw(DeccaLWare.INSTANCE.rotationManager.yaw);
            self.setPitch(DeccaLWare.INSTANCE.rotationManager.pitch);
            deccalRotSwapped = true;
        }
        DeccaLWare.INSTANCE.moduleManager.onPreMotion();
    }

    @Inject(method = "sendMovementPackets", at = @At("RETURN"))
    private void onSendMovementPacketsReturn(CallbackInfo ci) {
        if (deccalRotSwapped) {
            ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
            self.setYaw(deccalRealYaw);
            self.setPitch(deccalRealPitch);
            deccalRotSwapped = false;
        }
        DeccaLWare.INSTANCE.moduleManager.onPostMotion();
        // FreeCam interact aim'i kaldir; boylece bir sonraki tick'te aktif kalmaz.
        com.deccalware.modules.visual.FreeCam fci = (com.deccalware.modules.visual.FreeCam)
                DeccaLWare.INSTANCE.moduleManager.getModuleByName("FreeCam");
        if (fci != null && fci.isAiming()) {
            fci.clearAim();
        }
    }
}
