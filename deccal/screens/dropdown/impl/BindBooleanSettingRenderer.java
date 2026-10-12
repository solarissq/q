package com.deccalware.deccal.screens.dropdown.impl;

import net.minecraft.client.gui.DrawContext;
import com.deccalware.deccal.manager.DeccalManager;
import com.deccalware.deccal.manager.IMinecraft;
import com.deccalware.deccal.modules.setting.BindBooleanSetting;
import com.deccalware.deccal.screens.dropdown.DescriptionRenderQueue;
import com.deccalware.deccal.screens.dropdown.SettingRenderer;
import com.deccalware.deccal.util.color.ColorUtil;
import com.deccalware.deccal.manager.fontManager.FontUtils;
import com.deccalware.deccal.util.render.RenderUtil;
import com.deccalware.deccal.util.render.Scissor;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class BindBooleanSettingRenderer implements SettingRenderer<BindBooleanSetting>, IMinecraft {

    private static final int HEIGHT = 16;
    private static final int SWITCH_WIDTH = 22;
    private static final int SWITCH_HEIGHT = 12;

    private final Map<BindBooleanSetting, Float> toggleProgressMap = new HashMap<>();
    private final Map<BindBooleanSetting, Float> scrollOffsetMap = new HashMap<>();

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void render(DrawContext ctx, BindBooleanSetting setting, int x, int y, int width, int height) {
        float progress = toggleProgressMap.getOrDefault(setting, setting.get() ? 1f : 0f);
        float target = setting.get() ? 1f : 0f;
        progress += (target - progress) * 0.15f;
        toggleProgressMap.put(setting, progress);

        double mouseX = mc.mouse.getX() / mc.getWindow().getScaleFactor();
        double mouseY = mc.mouse.getY() / mc.getWindow().getScaleFactor();

        int switchX = x + width - SWITCH_WIDTH + 4;
        int switchY = y + (HEIGHT - SWITCH_HEIGHT) / 2 - 2;
        Color offColor = new Color(50, 50, 50, 200);
        int onColor = DeccalManager.STYLE_MANAGER.getFirstColor();
        int bgColor = ColorUtil.interpolateColor(offColor.getRGB(), onColor, progress);

        RenderUtil.drawRoundedRect(ctx.getMatrices(), switchX, switchY, SWITCH_WIDTH, SWITCH_HEIGHT, 5, bgColor);
        int knobRadius = 8;
        float knobX = switchX + 3 + (SWITCH_WIDTH - knobRadius - 5) * progress;
        float knobY = switchY + (SWITCH_HEIGHT - knobRadius) / 2f;
        RenderUtil.drawCircle(ctx.getMatrices(), knobX + knobRadius / 2f, knobY + knobRadius / 2f, knobRadius, Color.WHITE.getRGB());

        int textX = x;
        int textY = (int) (y + (HEIGHT - FontUtils.durman[13].getHeight()) / 2) - 2;
        String text = setting.getName();
        float textWidth = FontUtils.durman[13].getWidth(text);
        float maxTextWidth = switchX - textX - 12;
        float overflow = textWidth - maxTextWidth;
        boolean shouldScroll = RenderUtil.isInRegion(mouseX, mouseY, x, y, width, height) && overflow > 0;
        float offset = scrollOffsetMap.getOrDefault(setting, 0f);
        if (shouldScroll) offset = Math.min(offset + 0.5f, overflow);
        else offset = Math.max(offset - 0.5f, 0);
        scrollOffsetMap.put(setting, offset);

        Scissor.push();
        Scissor.setFromComponentCoordinates(textX, textY - 1, maxTextWidth, FontUtils.durman[13].getHeight() + 2);
        FontUtils.durman[13].drawLeftAligned(ctx.getMatrices(), text, textX - offset, textY, Color.WHITE.getRGB());
        Scissor.pop();

        boolean isHovered = mouseX >= switchX && mouseX <= switchX + SWITCH_WIDTH && mouseY >= switchY && mouseY <= switchY + SWITCH_HEIGHT;
        if (isHovered && setting.getDesc() != null && !setting.getDesc().isEmpty()) {
            DescriptionRenderQueue.add(setting.getDesc(), (float) mouseX + 6, (float) mouseY + 6);
        }
    }

    @Override
    public boolean mouseClicked(BindBooleanSetting setting, double mouseX, double mouseY, int button, int x, int y, int width, int height) {
        if (button != 0) return false;

        int switchX = x + width - SWITCH_WIDTH + 4;
        int switchY = y + (HEIGHT - SWITCH_HEIGHT) / 2 - 2;
        if (RenderUtil.isInRegion(mouseX, mouseY, switchX, switchY, SWITCH_WIDTH, SWITCH_HEIGHT)) {
            setting.set(!setting.get());
            return true;
        }

        return false;
    }

}