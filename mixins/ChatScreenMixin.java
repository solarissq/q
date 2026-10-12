package com.deccalware.mixins;

import com.deccalware.deccal.manager.dragManager.Dragging;
import com.deccalware.deccal.manager.dragManager.DragManager;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow
    @Final
    private TextFieldWidget chatField;

    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void deccaLware$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // Chat mesaj yazma alanina tiklanirsa HUD elemani suruklemesi BASLATMA:
        // aksi halde chat'e odaklanmak icin yapilan tiklamalar (ornegin watermark/arraylist
        // o bolgeye tasinmissa) yanlislikla HUD'u surukleyip ekran disina kaydiriyordu.
        if (button == 0 && (chatField == null || !chatField.isMouseOver(mouseX, mouseY))) {
            for (Dragging dragging : DragManager.draggables.values()) {
                if (dragging.onClick(mouseX, mouseY, button)) {
                    cir.setReturnValue(true);
                    cir.cancel();
                    return;
                }
            }
        }
    }
}