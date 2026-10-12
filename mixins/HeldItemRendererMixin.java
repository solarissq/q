package com.deccalware.mixins;

import com.deccalware.DeccaLWare;
import com.deccalware.modules.visual.SwingAnimations;
import com.deccalware.modules.visual.ViewModel;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void onRenderItem(
            LivingEntity entity,
            ItemStack itemStack,
            ModelTransformationMode renderMode,
            boolean leftHanded,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        ViewModel viewModel =
                (ViewModel) DeccaLWare.INSTANCE.moduleManager.getModuleByName("ViewModel");

        if (viewModel != null && viewModel.isEnabled() && renderMode.isFirstPerson()) {

            matrices.translate(
                    viewModel.x.getValue(),
                    viewModel.y.getValue(),
                    viewModel.z.getValue()
            );

            matrices.multiply(
                    RotationAxis.POSITIVE_X.rotationDegrees(
                            viewModel.rotX.getValue().floatValue()
                    )
            );

            matrices.multiply(
                    RotationAxis.POSITIVE_Y.rotationDegrees(
                            viewModel.rotY.getValue().floatValue()
                    )
            );

            matrices.multiply(
                    RotationAxis.POSITIVE_Z.rotationDegrees(
                            viewModel.rotZ.getValue().floatValue()
                    )
            );

            float s = viewModel.size.getValue().floatValue();
            matrices.scale(s, s, s);
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void onRenderFirstPersonItem(
            AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        com.deccalware.modules.visual.FreeCam freeCam =
                (com.deccalware.modules.visual.FreeCam) DeccaLWare.INSTANCE.moduleManager.getModuleByName("FreeCam");
        if (freeCam != null && freeCam.isEnabled() && !freeCam.renderHands.getValue()) {
            ci.cancel();
            return;
        }

        SwingAnimations swingAnimations =
                (SwingAnimations) DeccaLWare.INSTANCE.moduleManager.getModuleByName("SwingAnimations");

        if (swingAnimations != null && swingAnimations.isEnabled()) {

            swingAnimations.renderFirstPersonItem(
                    player,
                    tickDelta,
                    pitch,
                    hand,
                    swingProgress,
                    item,
                    equipProgress,
                    matrices,
                    vertexConsumers,
                    light
            );

            ci.cancel();
        }
    }
}