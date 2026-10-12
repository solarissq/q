package com.deccalware.mixins;

import com.deccalware.modules.player.HitBox;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityDimensionsMixin {

    @Inject(method = "getDimensions(Lnet/minecraft/entity/EntityPose;)Lnet/minecraft/entity/EntityDimensions;", at = @At("RETURN"), cancellable = true)
    private void deccalware$expandHitbox(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        HitBox hitBox = HitBox.getInstance();
        if (hitBox != null && hitBox.isEnabled()) {
            EntityDimensions original = cir.getReturnValue();
            float expand = hitBox.expand.getValue().floatValue();
            cir.setReturnValue(EntityDimensions.changing(
                    original.width() + expand,
                    original.height()));
        }
    }
}
