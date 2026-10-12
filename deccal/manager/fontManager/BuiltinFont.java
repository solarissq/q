package com.deccalware.deccal.manager.fontManager;

import com.deccalware.deccal.manager.IMinecraft;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public final class BuiltinFont implements IMinecraft {
    private static final float BASE_HEIGHT = 9f;
    private final float scale;

    public BuiltinFont(int size) {
        this.scale = size / BASE_HEIGHT;
    }

    public float getWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0f;
        }
        return mc.textRenderer.getWidth(text) * scale;
    }

    public float getHeight() {
        return mc.textRenderer.fontHeight * scale;
    }

    public void drawLeftAligned(MatrixStack matrices, String text, float x, float y, int color) {
        drawText(matrices, text, x, y, color);
    }

    public void centeredDraw(MatrixStack matrices, String text, float x, float y, int color) {
        drawText(matrices, text, x - getWidth(text) * 0.5f, y, color);
    }

    public void drawRightAligned(MatrixStack matrices, String text, float x, float y, int color) {
        drawText(matrices, text, x - getWidth(text), y, color);
    }

    public void renderGradientText(MatrixStack matrices, String text, float x, float y, int color1, int color2) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int len = text.length();
        float drawX = x;
        for (int i = 0; i < len; i++) {
            String ch = String.valueOf(text.charAt(i));
            float t = len <= 1 ? 0f : (float) i / (len - 1);
            drawLeftAligned(matrices, ch, drawX, y, lerpColor(color1, color2, t));
            drawX += getWidth(ch);
        }
    }

    private void drawText(MatrixStack matrices, String text, float x, float y, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        matrices.push();
        matrices.translate(x, y, 0);
        matrices.scale(scale, scale, 1f);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        TextRenderer textRenderer = mc.textRenderer;
        VertexConsumerProvider.Immediate consumers = mc.getBufferBuilders().getEntityVertexConsumers();
        textRenderer.draw(text, 0, 0, color, false, matrix, consumers, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
        consumers.draw();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        matrices.pop();
    }

    private static int lerpColor(int c1, int c2, float t) {
        int a = (int) (((c1 >>> 24) & 0xFF) * (1f - t) + ((c2 >>> 24) & 0xFF) * t);
        int r = (int) (((c1 >>> 16) & 0xFF) * (1f - t) + ((c2 >>> 16) & 0xFF) * t);
        int g = (int) (((c1 >>> 8) & 0xFF) * (1f - t) + ((c2 >>> 8) & 0xFF) * t);
        int b = (int) ((c1 & 0xFF) * (1f - t) + (c2 & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
