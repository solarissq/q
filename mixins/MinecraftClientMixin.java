package com.deccalware.mixins;

import com.deccalware.DeccaLWare;
import java.io.File;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.crash.CrashReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    private static boolean deccalware$reportingCrash = false;

    @Inject(
        method = "printCrashReport(Lnet/minecraft/client/MinecraftClient;Ljava/io/File;Lnet/minecraft/util/crash/CrashReport;)V",
        at = @At("HEAD")
    )
    private static void onPrintCrashReport(MinecraftClient client, File runDirectory, CrashReport report, CallbackInfo ci) {
        if (deccalware$reportingCrash) return;
        deccalware$reportingCrash = true;
        try {
            String username = "Unknown";
            try {
                MinecraftClient mc = client != null ? client : MinecraftClient.getInstance();
                if (mc != null && mc.getSession() != null) {
                    username = mc.getSession().getUsername();
                }
            } catch (Exception ignored) {
            }
            if (DeccaLWare.INSTANCE != null && report != null) {
                DeccaLWare.INSTANCE.sendCrashNotification(username, report.getMessage(), report.getCause());
            }
        } catch (Exception ignored) {
        } finally {
            deccalware$reportingCrash = false;
        }
    }
}
