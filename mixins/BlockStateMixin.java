package com.deccalware.mixins;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// DİKKAT: BlockState yerine AbstractBlock.AbstractBlockState kullanmalısın
@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class BlockStateMixin {

    @Inject(
        // Metodun tam imzasını (descriptor) ekledik
        method = "isSideInvisible(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void deccalware$isSideInvisible(BlockState state, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        // Tüm blokların yüzeylerini görünür kılarak X-Ray etkisi yaratır
        cir.setReturnValue(false);
    }
}