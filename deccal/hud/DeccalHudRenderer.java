package com.deccalware.deccal.hud;

import com.deccalware.DeccaLWare;
import com.deccalware.deccal.manager.ClientManager;
import com.deccalware.deccal.manager.DeccalManager;
import com.deccalware.deccal.manager.dragManager.Dragging;
import com.deccalware.deccal.manager.dragManager.DragManager;
import com.deccalware.deccal.manager.fontManager.BuiltinFont;
import com.deccalware.deccal.manager.fontManager.FontUtils;
import com.deccalware.deccal.modules.DeccalFunction;
import com.deccalware.deccal.modules.visual.DeccalClickGuiModule;
import com.deccalware.deccal.util.color.ColorUtil;
import com.deccalware.deccal.util.render.RenderUtil;
import com.deccalware.modules.Module;
import com.deccalware.modules.visual.HUD;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class DeccalHudRenderer {
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static final Map<Module, Float> animations = new HashMap<>();
    private static final Map<Module, Float> yAnimations = new HashMap<>();
    private static Dragging watermarkDrag;
    private static Dragging arrayListDrag;

    private DeccalHudRenderer() {
    }

    public static void render(DrawContext context) {
        // Bu cerceve cizilen widget'lari isaretle (gorusur olmayanlar tiklanamaz).
        for (Dragging d : DragManager.draggables.values()) {
            d.rendered = false;
        }

        // Cozunurluk degisimi (F11 tam ekran, pencere resize) once ele alinir:
        // widget'lar kirpilmak yerine oranla yeni cozunurluge tasiir.
        DragManager.handleScreenResize();

        // Surukleme/isaret guncellemesi HUD modulunden bagimsiz calisir: aksi halde
        // HUD kapaliyken diger widget'lar (spotify/keystrokes/target/totem)
        // konumlandirilamaz olurdu.
        dragTick(context);

        if (MC.player == null || MC.world == null || DeccalManager.FUNCTION_MANAGER == null) {
            return;
        }

        HUD hud = (HUD) DeccaLWare.INSTANCE.moduleManager.getModuleByName("HUD");
        if (hud == null || !hud.isEnabled()) {
            return;
        }

        if (watermarkDrag == null) {
            watermarkDrag = DragManager.getOrCreate(DragManager.WATERMARK, 10, 10);
            watermarkDrag.rightAnchored = false;
            watermarkDrag.bottomAnchored = false;
        }
        if (arrayListDrag == null) {
            arrayListDrag = DragManager.getOrCreate(DragManager.ARRAYLIST,
                    DragManager.screenWidth() - 8, 5);
        }
        watermarkDrag.clampToScreen();

        if (hud.watermark.getValue()) {
            renderWatermark(context, hud);
            watermarkDrag.rendered = true;
        }
        if (hud.arrayList.getValue()) {
            renderArrayList(context, hud);
            arrayListDrag.rendered = true;
        }
    }

    private static boolean draggingChatWasOpen = false;

    private static void dragTick(DrawContext context) {
        boolean chatOpen = MC.currentScreen instanceof ChatScreen;
        if (draggingChatWasOpen && !chatOpen) {
            DeccalManager.DRAG_MANAGER.save();
        }
        draggingChatWasOpen = chatOpen;

        if (!chatOpen) {
            return;
        }

        long windowHandle = MC.getWindow().getHandle();
        boolean leftDown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(windowHandle, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

        boolean anyDragging = false;
        for (Dragging d : DragManager.draggables.values()) {
            if (d.isDragging() && !leftDown) {
                d.onRelease(0);
            }
            if (d.isDragging()) {
                anyDragging = true;
            }
        }

        if (!anyDragging) {
            return;
        }

        double scaleX = (double) MC.getWindow().getScaledWidth() / MC.getWindow().getWidth();
        double scaleY = (double) MC.getWindow().getScaledHeight() / MC.getWindow().getHeight();
        double mouseX = MC.mouse.getX() * scaleX;
        double mouseY = MC.mouse.getY() * scaleY;

        for (Dragging d : DragManager.draggables.values()) {
            if (d.isDragging()) {
                d.onDraw(mouseX, mouseY, MC.getWindow());
            }
        }
        for (Dragging d : DragManager.draggables.values()) {
            if (d.isDragging()) {
                d.renderGuides(MC.getWindow());
            }
        }
    }

    private static void renderWatermark(DrawContext context, HUD hud) {
        float x = watermarkDrag.getX();
        float y = watermarkDrag.getY();

        switch (hud.watermarkStyle.getValue()) {
            case "Sade" -> {
                renderWatermarkSade(context, x, y, hud);
                return;
            }
            case "Gradyan" -> {
                renderWatermarkGradyan(context, x, y, hud);
                return;
            }
            case "Neon" -> {
                renderWatermarkNeon(context, x, y, hud);
                return;
            }
            case "Rozet" -> {
                renderWatermarkRozet(context, x, y, hud);
                return;
            }
            case "Terminal" -> {
                renderWatermarkTerminal(context, x, y, hud);
                return;
            }
            default -> {
            }
        }

        String textLogo = DeccaLWare.HUD_WATERMARK;
        String textInfo = DeccalManager.USER_PROFILE.getName() + " | " + ClientManager.getFps() + " FPS | "
                + ClientManager.getPing() + " MS | " + ClientManager.getBps(MC.player) + " BPS";

        var matrices = context.getMatrices();
        var fontBig = FontUtils.durman[16];
        var fontSmall = FontUtils.durman[15];

        float logoWidth = fontBig.getWidth(textLogo);
        float infoWidth = fontSmall.getWidth(textInfo);
        int alpha = 220;
        int infoBoxColor = new java.awt.Color(22, 22, 22, alpha).getRGB();

        float logoBoxWidth = 12 + logoWidth;
        float infoBoxWidth = 10 + infoWidth;
        RenderUtil.drawRoundedRect(matrices, x, y, logoBoxWidth, 18, new Vector4f(5, 5, 0, 0), ColorUtil.hud_color);
        RenderUtil.drawRoundedRect(matrices, x, y, 2, 18, new Vector4f(2, 0, 0, 2), ColorUtil.getColorStyle(30));
        RenderUtil.drawRoundedRect(matrices, x + logoBoxWidth - 2, y, infoBoxWidth, 18, new Vector4f(0, 0, 3, 3), infoBoxColor);

        fontBig.renderGradientText(matrices, textLogo, x + 6, y + 4, ColorUtil.getColorStyle(180),
                ColorUtil.getColorStyle(30));
        fontSmall.drawLeftAligned(matrices, textInfo, x + logoBoxWidth + 4, y + 4.5f, -1);
        int ping;
        try {
            ping = Integer.parseInt(ClientManager.getPing());
        } catch (NumberFormatException e) {
            ping = 600;
        }
        int pingColor = ping < 60 ? new java.awt.Color(100, 255, 100).getRGB() : ping < 120 ? new java.awt.Color(255, 220, 80).getRGB() : new java.awt.Color(255, 80, 80).getRGB();
        String pingText = ping + " MS";
        float pingX = x + logoBoxWidth + 4 + fontSmall.getWidth(DeccalManager.USER_PROFILE.getName() + " | " + ClientManager.getFps() + " FPS | ");
        fontSmall.drawLeftAligned(matrices, pingText, pingX, y + 4.5f, pingColor);

        watermarkDrag.setHeight(18);
        watermarkDrag.setWidth((int) (logoBoxWidth + infoBoxWidth));
    }

    private static int shift(float speed) {
        return (int) ((System.currentTimeMillis() / speed) % 360);
    }

    private static String infoText() {
        return DeccalManager.USER_PROFILE.getName() + " | " + ClientManager.getFps() + " FPS | "
                + ClientManager.getPing() + " MS | " + ClientManager.getBps(MC.player) + " BPS";
    }

    private static int pingColor() {
        int ping;
        try {
            ping = Integer.parseInt(ClientManager.getPing());
        } catch (NumberFormatException e) {
            ping = 600;
        }
        return ping < 60 ? new java.awt.Color(100, 255, 100).getRGB() : ping < 120 ? new java.awt.Color(255, 220, 80).getRGB() : new java.awt.Color(255, 80, 80).getRGB();
    }

    private static String pingText() {
        int ping;
        try {
            ping = Integer.parseInt(ClientManager.getPing());
        } catch (NumberFormatException e) {
            ping = 600;
        }
        return ping + " MS";
    }

    private static void renderWatermarkSade(DrawContext context, float x, float y, HUD hud) {
        String textLogo = DeccaLWare.HUD_WATERMARK;
        String textInfo = infoText();

        var matrices = context.getMatrices();
        var fontBig = FontUtils.durman[16];
        var fontSmall = FontUtils.durman[13];

        float logoWidth = fontBig.getWidth(textLogo);
        float infoWidth = fontSmall.getWidth(textInfo);
        float animShift = hud.animatedWatermark.getValue() ? shift(60) : 0;

        int shadow = new java.awt.Color(0, 0, 0, 110).getRGB();
        fontBig.drawLeftAligned(matrices, textLogo, x + 1, y + 2, shadow);
        fontBig.renderGradientText(matrices, textLogo, x, y, ColorUtil.getColorStyle(180 + animShift),
                ColorUtil.getColorStyle(30 + animShift));
        fontSmall.drawLeftAligned(matrices, textInfo, x + 1, y + 21, shadow);
        fontSmall.drawLeftAligned(matrices, textInfo, x, y + 20, new java.awt.Color(190, 190, 190, 230).getRGB());

        watermarkDrag.setHeight(32);
        watermarkDrag.setWidth((int) Math.max(logoWidth, infoWidth) + 6);
    }

    private static void renderWatermarkGradyan(DrawContext context, float x, float y, HUD hud) {
        String textLogo = DeccaLWare.HUD_WATERMARK;
        String textInfo = infoText();

        var matrices = context.getMatrices();
        var fontBig = FontUtils.durman[16];
        var fontSmall = FontUtils.durman[13];

        float logoWidth = fontBig.getWidth(textLogo);
        float infoWidth = fontSmall.getWidth(textInfo);
        float animShift = hud.animatedWatermark.getValue() ? shift(50) : 0;

        float barWidth = Math.max(logoWidth, infoWidth) + 40;
        float barHeight = 36;

        RenderUtil.rectRGB(matrices, x, y, barWidth, barHeight, 4,
                ColorUtil.getColorStyle(200 + animShift, 90),
                ColorUtil.getColorStyle(120 + animShift, 90),
                ColorUtil.getColorStyle(40 + animShift, 90),
                ColorUtil.getColorStyle(280 + animShift, 90));
        if (hud.animatedWatermark.getValue()) {
            float waveX = (System.currentTimeMillis() % 1200) / 1200f * barWidth;
            RenderUtil.drawRoundedRect(matrices, x + waveX - 60, y, 60, barHeight, 2, ColorUtil.applyAlpha(java.awt.Color.WHITE.getRGB(), 0.10f));
        }
        RenderUtil.drawRoundedRect(matrices, x, y, barWidth, 3, new Vector4f(4, 4, 4, 4), ColorUtil.getColorStyle(30 + animShift));
        RenderUtil.drawRoundedRect(matrices, x, y + barHeight - 2, barWidth, 2, new Vector4f(2, 2, 4, 4), new java.awt.Color(0, 0, 0, 80).getRGB());

        fontBig.renderGradientText(matrices, textLogo, x + barWidth / 2 - fontBig.getWidth(textLogo) / 2, y + 5,
                ColorUtil.getColorStyle(180 + animShift), ColorUtil.getColorStyle(30 + animShift));
        fontSmall.centeredDraw(matrices, textInfo, x + barWidth / 2, y + 22, new java.awt.Color(220, 220, 220, 240).getRGB());

        watermarkDrag.setHeight((int) barHeight);
        watermarkDrag.setWidth((int) barWidth);
    }

    private static void renderWatermarkNeon(DrawContext context, float x, float y, HUD hud) {
        String textLogo = DeccaLWare.HUD_WATERMARK;
        String textInfo = infoText();

        var matrices = context.getMatrices();
        var fontBig = FontUtils.durman[16];
        var fontSmall = FontUtils.durman[13];

        float logoWidth = fontBig.getWidth(textLogo);
        float infoWidth = fontSmall.getWidth(textInfo);
        float animShift = hud.animatedWatermark.getValue() ? shift(45) : 0;
        float pulse = 0.6f + 0.4f * (float) Math.sin(System.currentTimeMillis() / 180.0);

        int glow = ColorUtil.applyAlpha(ColorUtil.getColorStyle(30 + animShift), 0.18f * pulse);
        int low = new java.awt.Color(0, 0, 0, 70).getRGB();

        float xPos = x;
        float yPos = y + 2;
        fontBig.drawLeftAligned(matrices, textLogo, xPos + 1, yPos + 1, low);
        fontBig.drawLeftAligned(matrices, textLogo, xPos - 1, yPos, glow);
        fontBig.drawLeftAligned(matrices, textLogo, xPos + 1, yPos, glow);
        fontBig.drawLeftAligned(matrices, textLogo, xPos, yPos - 1, glow);
        fontBig.drawLeftAligned(matrices, textLogo, xPos, yPos + 1, glow);
        fontBig.renderGradientText(matrices, textLogo, xPos, yPos, ColorUtil.getColorStyle(180 + animShift),
                ColorUtil.getColorStyle(30 + animShift));

        float lineY = yPos + fontBig.getHeight() + 2;
        RenderUtil.drawRoundedRect(matrices, xPos, lineY, logoWidth, 2, 1, ColorUtil.applyAlpha(ColorUtil.getColorStyle(30 + animShift), pulse));
        RenderUtil.drawRoundedRect(matrices, xPos, lineY, logoWidth * pulse, 2, 1, ColorUtil.getColorStyle(30 + animShift));

        fontSmall.drawLeftAligned(matrices, textInfo, xPos, lineY + 6, new java.awt.Color(170, 170, 170, 220).getRGB());

        watermarkDrag.setHeight((int) (lineY + 6 + fontSmall.getHeight()) - (int) y + 2);
        watermarkDrag.setWidth((int) Math.max(logoWidth, infoWidth) + 4);
    }

    private static void renderWatermarkRozet(DrawContext context, float x, float y, HUD hud) {
        String textLogo = DeccaLWare.HUD_WATERMARK;
        String textInfo = infoText();

        var matrices = context.getMatrices();
        var fontBig = FontUtils.durman[16];
        var fontSmall = FontUtils.durman[13];

        float logoWidth = fontBig.getWidth(textLogo);
        float infoWidth = fontSmall.getWidth(textInfo);
        float animShift = hud.animatedWatermark.getValue() ? shift(55) : 0;

        float boxHeight = 22;
        float logoBoxWidth = 14 + logoWidth;
        float infoBoxWidth = 12 + infoWidth;
        float totalWidth = logoBoxWidth + infoBoxWidth;

        RenderUtil.drawRoundedRect(matrices, x, y, logoBoxWidth, boxHeight, new Vector4f(11, 0, 0, 11), ColorUtil.getColorStyle(30 + animShift));
        RenderUtil.drawRoundedRect(matrices, x + logoBoxWidth, y, infoBoxWidth, boxHeight, new Vector4f(0, 11, 11, 0), new java.awt.Color(20, 20, 20, 215).getRGB());
        RenderUtil.drawRoundedBorder(matrices, x - 0.5f, y - 0.5f, totalWidth + 1, boxHeight + 1, 11, 1.2f, ColorUtil.applyAlpha(ColorUtil.getColorStyle(200 + animShift), 0.65f));
        RenderUtil.drawCircle(matrices, x + logoBoxWidth, y + boxHeight / 2.0f, 1.5f, java.awt.Color.WHITE.getRGB());

        fontBig.renderGradientText(matrices, textLogo, x + 7, y + (boxHeight - fontBig.getHeight()) / 2f,
                ColorUtil.getColorStyle(200 + animShift), ColorUtil.getColorStyle(40 + animShift));
        fontSmall.drawLeftAligned(matrices, textInfo, x + logoBoxWidth + 6, y + (boxHeight - fontSmall.getHeight()) / 2f - 1, new java.awt.Color(230, 230, 230, 235).getRGB());

        watermarkDrag.setHeight((int) boxHeight);
        watermarkDrag.setWidth((int) totalWidth);
    }

    private static void renderWatermarkTerminal(DrawContext context, float x, float y, HUD hud) {
        String textLogo = "> " + DeccaLWare.HUD_WATERMARK;
        String textInfo = "[" + infoText() + "]";
        boolean blink = (System.currentTimeMillis() / 500L) % 2 == 0;

        var matrices = context.getMatrices();
        var fontBig = FontUtils.durman[16];
        var fontSmall = FontUtils.durman[13];

        float logoWidth = fontBig.getWidth(textLogo);
        float infoWidth = fontSmall.getWidth(textInfo);
        int dim = new java.awt.Color(0, 0, 0, 130).getRGB();

        fontBig.drawLeftAligned(matrices, textLogo, x + 1, y + 1, dim);
        fontBig.drawLeftAligned(matrices, textLogo, x, y, ColorUtil.getColorStyle(30));
        if (blink) {
            fontBig.drawLeftAligned(matrices, "_", x + logoWidth + 2, y, ColorUtil.getColorStyle(30));
        }
        fontSmall.drawLeftAligned(matrices, textInfo, x, y + 20, new java.awt.Color(150, 150, 150, 230).getRGB());

        float underlineW = Math.max(logoWidth, infoWidth);
        RenderUtil.drawRoundedRect(matrices, x, y + 20 + fontSmall.getHeight() + 3, underlineW, 1, 1, ColorUtil.applyAlpha(ColorUtil.getColorStyle(30), 0.55f));

        watermarkDrag.setHeight((int) (20 + fontSmall.getHeight() + 7));
        watermarkDrag.setWidth((int) Math.max(logoWidth, infoWidth) + 8);
    }

    private static final int AL_ROW_HEIGHT = 12;

    private static void renderArrayList(DrawContext context, HUD hud) {
        float delta = MC.getRenderTickCounter().getLastFrameDuration() * 0.2f;
        var font = FontUtils.durman[13];

        List<Module> visibleModules = DeccaLWare.INSTANCE.moduleManager.getModules().stream()
                .filter(m -> m.isVisible() && (m.isEnabled() || animations.getOrDefault(m, 0f) > 0.01f))
                .sorted(Comparator.<Module>comparingDouble(m -> font.getWidth(m.getName())).reversed())
                .collect(Collectors.toList());

        // 1) Satirlari (animasyon degerleriyle) hazirla, en genis hucreyi bul.
        int rowCount = 0;
        float maxCellWidth = 0;
        for (Module module : visibleModules) {
            float currentAnim = MathHelper.lerp(delta, animations.getOrDefault(module, 0f), module.isEnabled() ? 1f : 0f);
            animations.put(module, currentAnim);

            if (currentAnim <= 0.01f && !module.isEnabled()) {
                continue;
            }

            String name = module.getName();
            if (hud.showKeybinds.getValue() && module.getKey() > 0) {
                String bindKey = ClientManager.getKey(module.getKey());
                if (bindKey != null && !bindKey.isEmpty()) {
                    name += " [" + bindKey + "]";
                }
            }

            maxCellWidth = Math.max(maxCellWidth, font.getWidth(name));
            rowCount++;
        }

        // 2) Kutuyu ONCE olce: hit-test, snap ve cizim hep ayni kutuya baksin.
        //    Satirlar capitin en fazla 16px icine kadar uzanir ("Akis"/"Gradyan"
        //    stilleri en genis dolgu), 4px dikey bosluk birakilir.
        float boxWidth = maxCellWidth + 16;
        float boxHeight = rowCount * (float) AL_ROW_HEIGHT + 4;
        arrayListDrag.setWidth(boxWidth);
        arrayListDrag.setHeight(boxHeight);

        // 3) Kutu ekranin hangi yariminda: sag yarimda saga yapisik, alt yarimda
        //    asagi yapisik (liste yukaridan asagiya degil, asagidan yukariya uzar).
        float boxCenterX = arrayListDrag.getBoxX() + boxWidth / 2f;
        float boxCenterY = arrayListDrag.getBoxY() + boxHeight / 2f;
        boolean newRight = boxCenterX > MC.getWindow().getScaledWidth() / 2f;
        boolean newBottom = boxCenterY > MC.getWindow().getScaledHeight() / 2f;

        float keepBoxX = arrayListDrag.getBoxX();
        float keepBoxY = arrayListDrag.getBoxY();
        arrayListDrag.rightAnchored = newRight;
        arrayListDrag.bottomAnchored = newBottom;
        arrayListDrag.setBoxPosition(keepBoxX, keepBoxY);
        arrayListDrag.clampToScreen();

        boolean rightSide = arrayListDrag.rightAnchored;
        boolean growUp = arrayListDrag.bottomAnchored;
        float anchorX = arrayListDrag.getX();
        float anchorY = arrayListDrag.getY();

        // 4) Satirlari bas
        int index = 0;
        for (Module module : visibleModules) {
            float currentAnim = animations.getOrDefault(module, 0f);
            if (currentAnim <= 0.01f && !module.isEnabled()) {
                continue;
            }

            String name = module.getName();
            if (hud.showKeybinds.getValue() && module.getKey() > 0) {
                String bindKey = ClientManager.getKey(module.getKey());
                if (bindKey != null && !bindKey.isEmpty()) {
                    name += " [" + bindKey + "]";
                }
            }

            float cellWidth = font.getWidth(name);

            // growUp: en uzun ad (index 0) kutunun EN ALTINDA kalir, digerleri yukarida.
            float targetY = growUp
                    ? anchorY - (index + 1) * (float) AL_ROW_HEIGHT
                    : anchorY + index * (float) AL_ROW_HEIGHT;

            // Yon degisince (asagi<->yukari) satirlari ekran boyunca kaydirmak yerine
            // aninda yerine oturt: lerp sadece kucuk kaymalarda gecerli.
            float prevAnimY = yAnimations.getOrDefault(module, targetY);
            float animY = Math.abs(prevAnimY - targetY) > 40f
                    ? targetY
                    : MathHelper.lerp(delta, prevAnimY, targetY);
            yAnimations.put(module, animY);

            int alpha = (int) (MathHelper.clamp(currentAnim, 0, 1) * 255);
            int color1 = ColorUtil.getColorStyle(30 + index * 8, alpha);
            int color2 = ColorUtil.getColorStyle(120 + index * 8, alpha);

            switch (hud.arraylistStyle.getValue()) {
                case "Sade" -> segmentSade(context, font, anchorX, cellWidth, currentAnim, animY, name, color1, color2, rightSide);
                case "Gradyan" -> segmentGradyan(context, font, anchorX, cellWidth, currentAnim, animY, name, color1, color2, rightSide);
                case "Neon" -> segmentNeon(context, font, anchorX, cellWidth, currentAnim, animY, name, color1, color2, rightSide);
                case "Akis" -> segmentAkis(context, font, anchorX, cellWidth, currentAnim, animY, name, color1, color2, rightSide);
                case "Terminal" -> segmentTerminal(context, font, anchorX, cellWidth, currentAnim, animY, name, color1, color2, rightSide);
                default -> segmentKlasik(context, font, anchorX, cellWidth, currentAnim, animY, name, color1, color2, rightSide);
            }

            index++;
        }
    }

    private static void segmentKlasik(DrawContext context, BuiltinFont font, float anchorX, float cellWidth, float anim, float y, String name, int color1, int color2, boolean rightSide) {
        int rowHeight = 12;
        int bgColor = new java.awt.Color(22, 22, 22, (int) (MathHelper.clamp(anim, 0, 1) * 255 * 0.85f)).getRGB();
        if (rightSide) {
            float xPos = anchorX - (cellWidth + 10) * anim;
            RenderUtil.drawRoundedRect(context.getMatrices(), xPos - 6, y, cellWidth + 10, rowHeight, new Vector4f(0, 0, 3, 3), bgColor);
            int accentBarWidth = Math.max(3, (int) (cellWidth * 0.06f) + 2);
            RenderUtil.drawRoundedRect(context.getMatrices(), anchorX - accentBarWidth, y, accentBarWidth, rowHeight, new Vector4f(3, 3, 0, 0), color1);
            font.renderGradientText(context.getMatrices(), name, xPos, y + 2, color1, color2);
        } else {
            float width = (cellWidth + 10) * anim;
            RenderUtil.drawRoundedRect(context.getMatrices(), anchorX, y, width, rowHeight, new Vector4f(3, 0, 0, 3), bgColor);
            RenderUtil.drawRoundedRect(context.getMatrices(), anchorX, y, 3, rowHeight, new Vector4f(3, 0, 0, 3), color1);
            font.renderGradientText(context.getMatrices(), name, anchorX + 8, y + 2, color1, color2);
        }
    }

    private static void segmentSade(DrawContext context, BuiltinFont font, float anchorX, float cellWidth, float anim, float y, String name, int color1, int color2, boolean rightSide) {
        int shadow = new java.awt.Color(0, 0, 0, 130).getRGB();
        float xPos = rightSide ? anchorX - (cellWidth + 2) * anim : anchorX + 4;
        font.drawLeftAligned(context.getMatrices(), name, xPos + 1, y + 3, shadow);
        font.renderGradientText(context.getMatrices(), name, xPos, y + 2, color1, color2);
    }

    private static void segmentGradyan(DrawContext context, BuiltinFont font, float anchorX, float cellWidth, float anim, float y, String name, int color1, int color2, boolean rightSide) {
        int rowHeight = 12;
        float width = (cellWidth + 14) * anim;
        float xStart = rightSide ? anchorX - width : anchorX;
        RenderUtil.rectRGB(context.getMatrices(), xStart, y, width, rowHeight, 2,
                ColorUtil.withAlpha(color2, 0.60f), ColorUtil.withAlpha(color1, 0.60f),
                ColorUtil.withAlpha(color1, 0.18f), ColorUtil.withAlpha(color2, 0.18f));
        RenderUtil.drawRoundedRect(context.getMatrices(), xStart, y, 3, rowHeight, 2, color1);
        font.drawLeftAligned(context.getMatrices(), name, xStart + 7, y + 2, java.awt.Color.WHITE.getRGB());
    }

    private static void segmentNeon(DrawContext context, BuiltinFont font, float anchorX, float cellWidth, float anim, float y, String name, int color1, int color2, boolean rightSide) {
        float xPos = rightSide ? anchorX - (cellWidth + 4) * anim : anchorX + 4;
        float pulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() / 200.0 + y * 0.3);
        int glow = ColorUtil.applyAlpha(color1, 0.30f * pulse);
        int shadow = new java.awt.Color(0, 0, 0, 90).getRGB();

        font.drawLeftAligned(context.getMatrices(), name, xPos + 1, y + 3, shadow);
        font.drawLeftAligned(context.getMatrices(), name, xPos - 1, y + 2, glow);
        font.drawLeftAligned(context.getMatrices(), name, xPos + 1, y + 2, glow);
        font.drawLeftAligned(context.getMatrices(), name, xPos, y + 1, glow);
        font.drawLeftAligned(context.getMatrices(), name, xPos, y + 3, glow);
        font.renderGradientText(context.getMatrices(), name, xPos, y + 2, color1, color2);

        if (rightSide) {
            RenderUtil.drawCircle(context.getMatrices(), anchorX - 3, y + 6, 1.4f, color2);
        } else {
            RenderUtil.drawCircle(context.getMatrices(), anchorX + 3, y + 6, 1.4f, color2);
        }
    }

    private static void segmentAkis(DrawContext context, BuiltinFont font, float anchorX, float cellWidth, float anim, float y, String name, int color1, int color2, boolean rightSide) {
        int rowHeight = 12;
        float ease = easeOutBack(MathHelper.clamp(anim, 0f, 1f));
        float width = (cellWidth + 16) * ease;
        float xStart = rightSide ? anchorX - width : anchorX;

        RenderUtil.drawRoundedRect(context.getMatrices(), xStart, y, width, rowHeight, 6, new java.awt.Color(16, 16, 16, 210).getRGB());
        RenderUtil.drawRoundedRect(context.getMatrices(), xStart, y, 3, rowHeight, new Vector4f(6, 0, 0, 6), color1);
        font.drawLeftAligned(context.getMatrices(), name, xStart + 8, y + 2, java.awt.Color.WHITE.getRGB());
        RenderUtil.drawRoundedRect(context.getMatrices(), xStart, y + rowHeight - 2, width, 2, 1, ColorUtil.withAlpha(color2, MathHelper.clamp(anim, 0f, 1f)));
        font.renderGradientText(context.getMatrices(), name, xStart + 8, y + 2, color1, color2);
    }

    private static void segmentTerminal(DrawContext context, BuiltinFont font, float anchorX, float cellWidth, float anim, float y, String name, int color1, int color2, boolean rightSide) {
        int rowHeight = 12;
        float ease = MathHelper.clamp(anim, 0f, 1f);
        if (rightSide) {
            float markX = anchorX - (cellWidth + 12) * ease;
            float nameX = anchorX - cellWidth * ease;

            RenderUtil.drawRoundedRect(context.getMatrices(), markX, y + rowHeight - 2, Math.max(1, anchorX - markX), 1, 1, ColorUtil.applyAlpha(color2, ease * 0.6f));
            font.drawLeftAligned(context.getMatrices(), ">", markX, y + 2, color1);
            font.drawLeftAligned(context.getMatrices(), name, nameX, y + 2, java.awt.Color.WHITE.getRGB());
        } else {
            float markX = anchorX + 4;
            float nameX = anchorX + 12;
            RenderUtil.drawRoundedRect(context.getMatrices(), markX, y + rowHeight - 2, Math.max(1, (cellWidth + 8) * ease), 1, 1, ColorUtil.applyAlpha(color2, ease * 0.6f));
            font.drawLeftAligned(context.getMatrices(), ">", markX, y + 2, color1);
            font.drawLeftAligned(context.getMatrices(), name, nameX, y + 2, java.awt.Color.WHITE.getRGB());
        }
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (t - 1) * (t - 1) * (t - 1) + c1 * (t - 1) * (t - 1);
    }
}
