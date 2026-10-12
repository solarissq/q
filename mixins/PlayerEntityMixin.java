package com.deccalware.mixins;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

import com.deccalware.DeccaLWare;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void onGetName(CallbackInfoReturnable<Text> cir) {
        if (DeccaLWare.INSTANCE.moduleManager.getModuleByName("NameProtect").isEnabled()) {
            if ((Object) this == net.minecraft.client.MinecraftClient.getInstance().player) {
                cir.setReturnValue(Text.literal("DeccaL"));
            }
        }
    }

    @Inject(method = "getEntityInteractionRange", at = @At("HEAD"), cancellable = true)
    private void onGetEntityInteractionRange(CallbackInfoReturnable<Double> cir) {
        com.deccalware.modules.player.Reach reach = (com.deccalware.modules.player.Reach) DeccaLWare.INSTANCE.moduleManager.getModuleByName("Reach");
        if (reach != null && reach.isEnabled()) {
            cir.setReturnValue(reach.distance.getValue().doubleValue());
        }
    }

    @Inject(method = "getBlockInteractionRange", at = @At("HEAD"), cancellable = true)
    private void onGetBlockInteractionRange(CallbackInfoReturnable<Double> cir) {
        com.deccalware.modules.player.Reach reach = (com.deccalware.modules.player.Reach) DeccaLWare.INSTANCE.moduleManager.getModuleByName("Reach");
        if (reach != null && reach.isEnabled()) {
            cir.setReturnValue(reach.distance.getValue().doubleValue());
        }
    }
}

