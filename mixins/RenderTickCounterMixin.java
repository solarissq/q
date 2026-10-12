package com.deccalware.mixins;

import com.deccalware.modules.misc.Timer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RenderTickCounter.Dynamic.class)
public class RenderTickCounterMixin {

    @ModifyVariable(method = "beginRenderTick(J)I", at = @At("STORE"), ordinal = 0)
    private int deccalware$modifyTickCount(int tickCount) {
        Timer timer = Timer.getInstance();
        if (timer != null && timer.isEnabled()) {
            float speed = timer.speed.getValue().floatValue();
            return Math.min((int) Math.max(0, tickCount * speed), 10);
        }
        return tickCount;
    }
}
