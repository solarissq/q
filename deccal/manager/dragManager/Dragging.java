package com.deccalware.deccal.manager.dragManager;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import com.deccalware.deccal.manager.fontManager.FontUtils;
import com.deccalware.deccal.modules.DeccalFunction;
import com.deccalware.deccal.util.render.RenderUtil;

import static com.deccalware.deccal.manager.IMinecraft.mc;

/**
 * HUD widget'larinin ekran konumu + surukleme (drag) davranisi.
 *
 * Konum (xPos/yPos) widget'in "capa" (anchor) noktasidir:
 *  - rightAnchored  : xPos sag kenar, kutu sola dogru acilir.
 *  - bottomAnchored : yPos alt kenar, kutu yukari dogru acilir.
 * Her widget'in gercek bant kutusu {@link #getBoxX()} / {@link #getBoxY()} ile
 * bulunur; boylece hit-test, drag siniri, hizalama (snap) ve cizim dort
 * noktada da AYNI kutuyu kullanir (onceki kodda hit-test daima xPos/yPos'ten
 * asagi/saga iniyordu, bu yuzden asagi tasiya widget'in gorsel kutusu ile
 * tiklanabilir kutu uyusmuyordu).
 */
public class Dragging {
    private static final int SNAP_DISTANCE = 10;
    private static final float FADE_SPEED = 0.01f;
    private static final float DRAG_SMOOTHNESS = 0.1f;

    @Expose
    @SerializedName("x")
    private float xPos;
    @Expose
    @SerializedName("y")
    private float yPos;

    public float initialXVal;
    public float initialYVal;

    private float startX, startY;
    private boolean dragging;

    private float width, height;
    private int defaultX;
    private int defaultY;

    public boolean rightAnchored;
    public boolean bottomAnchored;

    /**
     * Widget bu cerceve cizildi mi? Yalnizca ekranda gorunen widget'lar
     * suruklenebilir; aksi halde kapatilan bir modulun hayalet kutusu
     * tiklanabilir kalirdi.
     */
    public boolean rendered;

    @Expose
    @SerializedName("name")
    private final String name;
    private final DeccalFunction function;

    private float borderAlpha = 0f;
    private float borderScale = 0f;
    private float guideAlpha = 0f;
    private float tipAlpha = 0f;
    public float targetX, targetY;

    private long lastTime = System.currentTimeMillis();

    public Dragging(DeccalFunction DeccalFunction, String name, float initialXVal, float initialYVal) {
        this.function = DeccalFunction;
        this.name = name;
        this.xPos = initialXVal;
        this.yPos = initialYVal;
        this.initialXVal = initialXVal;
        this.initialYVal = initialYVal;

        this.targetX = initialXVal;
        this.targetY = initialYVal;

        this.defaultX = (int) initialXVal;
        this.defaultY = (int) initialYVal;

        DragManager.draggables.put(name, this);
    }

    public int getDefaultX() {
        return defaultX;
    }

    public int getDefaultY() {
        return defaultY;
    }

    public float getX() {
        return xPos;
    }

    public void setX(float x) {
        this.xPos = x;
        this.targetX = x;
    }

    public float getY() {
        return yPos;
    }

    public void setY(float y) {
        this.yPos = y;
        this.targetY = y;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public String getName() {
        return name;
    }

    public DeccalFunction getModule() {
        return function;
    }

    /** Gorsel kutunun sol ust kosesi (hit-test / clamp / cizim buradan). */
    public float getBoxX() {
        return rightAnchored ? xPos - width : xPos;
    }

    /** Gorsel kutunun sol ust kosesi. */
    public float getBoxY() {
        return bottomAnchored ? yPos - height : yPos;
    }

    /** Kutunun sol-ust kosesinden koyarak konum ayarla (cap noktasi otomatik). */
    public void setBoxPosition(float boxX, float boxY) {
        setX(rightAnchored ? boxX + width : boxX);
        setY(bottomAnchored ? boxY + height : boxY);
    }

    /**
     * Kutu tamamen ekran icinde kalacak sekilde konumu kisitlar.
     * Hedef (targetX/targetY) kisitlanir; mevcut konum yalnizca ekran disinda
     * kaldiginda (orn. pencere yeniden boyutlandiginda) sert olarak atilir —
     * boylece asagi/yukari kayma animasyonu (lerp) bozulmaz.
     */
    public void clampToScreen() {
        float sw = DragManager.screenWidth();
        float sh = DragManager.screenHeight();

        // Pencere olculeri gecersizse (F11 tam ekran gecisinde scaledWidth 0'a
        // duser veya Initialization kareleri) HICBIR sey kirpilmaz: aksi halde
        // tum widget'lar 0'a cakilir ve cozunurluk geri geldiginde yerleri
        // kalici olarak bozulmus olur.
        if (sw <= 0 || sh <= 0) return;

        float w = Math.max(0, width);
        float h = Math.max(0, height);
        float maxBoxX = Math.max(0, sw - w);
        float maxBoxY = Math.max(0, sh - h);

        if (Float.isNaN(xPos) || Float.isNaN(yPos) || Float.isInfinite(xPos) || Float.isInfinite(yPos)) {
            xPos = getDefaultX();
            yPos = getDefaultY();
            targetX = xPos;
            targetY = yPos;
        }

        float clampedTargetX = rightAnchored
                ? MathHelper.clamp(targetX - w, 0, maxBoxX) + w
                : MathHelper.clamp(targetX, 0, maxBoxX);
        float clampedTargetY = bottomAnchored
                ? MathHelper.clamp(targetY - h, 0, maxBoxY) + h
                : MathHelper.clamp(targetY, 0, maxBoxY);

        // Hedef yalnizca gecici olarak kirpilir; asil kaydedilen deger (xPos)
        // yalnizca cok uzaklastiginda sert atilir. Boylece lerp animasyonu
        // bozulmaz, cozunurluk dondugunde widget eski yerine doner.
        targetX = clampedTargetX;
        targetY = clampedTargetY;

        float curBoxX = rightAnchored ? xPos - w : xPos;
        float curBoxY = bottomAnchored ? yPos - h : yPos;
        if (curBoxX < -8 || curBoxX > maxBoxX + 8 || curBoxY < -8 || curBoxY > maxBoxY + 8) {
            xPos = targetX;
            yPos = targetY;
        }
    }

    /**
     * Cozunurluk degistiginde (F11 tam ekran, pencere boyutu) konumlari
     * ORANLA olarak yeniden olceklendirir. Kirpma yerine olcekleme kullanilir:
     * sag/sag ve alt/sag capit widget'lar ekranin ayni oraninda kalir ve F11
     * iki kez basildiginda yerleri degismez.
     */
    public void rescaleToScreen(int oldW, int oldH, int newW, int newH) {
        if (oldW <= 0 || oldH <= 0 || newW <= 0 || newH <= 0) return;

        float fx = (float) newW / (float) oldW;
        float fy = (float) newH / (float) oldH;

        xPos *= fx;
        yPos *= fy;
        targetX *= fx;
        targetY *= fy;
        initialXVal *= fx;
        initialYVal *= fy;
        defaultX = Math.round(defaultX * fx);
        defaultY = Math.round(defaultY * fy);

        clampToScreen();
    }

    private float centerX() {
        return getBoxX() + width / 2f;
    }

    private float centerY() {
        return getBoxY() + height / 2f;
    }

    public void onDraw(double mouseX, double mouseY, Window window) {
        long windowHandle = mc.getWindow().getHandle();
        boolean isShiftDown = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;

        if (dragging) {
            float screenWidth = window.getScaledWidth();
            float screenHeight = window.getScaledHeight();
            float screenCenterX = screenWidth / 2f;
            float screenCenterY = screenHeight / 2f;

            float boxX = (float) (mouseX - startX);
            float boxY = (float) (mouseY - startY);
            if (rightAnchored) boxX -= width;
            if (bottomAnchored) boxY -= height;

            float boxCenterX = boxX + width / 2f;
            float boxCenterY = boxY + height / 2f;

            if (isShiftDown) {
                if (Math.abs(boxCenterX - screenCenterX) <= SNAP_DISTANCE) boxX = screenCenterX - width / 2f;
                if (Math.abs(boxCenterY - screenCenterY) <= SNAP_DISTANCE) boxY = screenCenterY - height / 2f;

                for (Dragging other : DragManager.draggables.values()) {
                    if (other == this) continue;
                    float otherCenterX = other.getBoxX() + other.width / 2f;
                    float otherCenterY = other.getBoxY() + other.height / 2f;

                    if (Math.abs(boxCenterX - otherCenterX) <= SNAP_DISTANCE) boxX = otherCenterX - width / 2f;
                    if (Math.abs(boxCenterY - otherCenterY) <= SNAP_DISTANCE) boxY = otherCenterY - height / 2f;
                }
            }

            boxX = MathHelper.clamp(boxX, 0, Math.max(0, screenWidth - width));
            boxY = MathHelper.clamp(boxY, 0, Math.max(0, screenHeight - height));

            targetX = rightAnchored ? boxX + width : boxX;
            targetY = bottomAnchored ? boxY + height : boxY;
        }

        xPos += (targetX - xPos) * DRAG_SMOOTHNESS;
        yPos += (targetY - yPos) * DRAG_SMOOTHNESS;
        clampToScreen();

        borderAlpha = dragging ? Math.min(borderAlpha + FADE_SPEED, 1f) : Math.max(borderAlpha - FADE_SPEED, 0f);

        float targetExpand = dragging ? 4f : 0f;
        borderScale += (targetExpand - borderScale) * 0.1f;
        if (borderAlpha > 0f) {
            int alphaColor = ((int) (borderAlpha * 255) << 24) | 0xFFFFFF;

            float drawX = getBoxX() - borderScale / 2f;
            float drawY = getBoxY() - borderScale / 2f;
            float drawWidth = width + borderScale;
            float drawHeight = height + borderScale;

            RenderUtil.drawRoundedBorder(new MatrixStack(), drawX, drawY, drawWidth, drawHeight, 3f, 0.01f, alphaColor);
        }

        float targetTipAlpha = dragging ? 1f : 0f;
        tipAlpha += (targetTipAlpha - tipAlpha) * 0.1f;
        tipAlpha = MathHelper.clamp(tipAlpha, 0f, 1f);
        if (tipAlpha > 0f) {
            int color = ((int) (tipAlpha * 255) << 24) | 0xFFFFFF;
            MatrixStack matrix = new MatrixStack();
            FontUtils.durman[12].centeredDraw(matrix, "Shift - Ortala / Hizala", centerX(), getBoxY() + height + 4f, color);
        }
    }

    public void renderGuides(Window window) {
        long now = System.currentTimeMillis();
        float delta = (now - lastTime) / 16.666f;
        lastTime = now;
        float targetAlpha = dragging ? 1f : 0f;
        guideAlpha += (targetAlpha - guideAlpha) * 1.2f * delta;
        guideAlpha = MathHelper.clamp(guideAlpha, 0f, 1f);
        if (guideAlpha <= 0f) return;

        float screenWidth = window.getScaledWidth();
        float screenHeight = window.getScaledHeight();
        float screenCenterX = screenWidth / 2f;
        float screenCenterY = screenHeight / 2f;
        int alphaColor = ((int) (guideAlpha * 255) << 24) | 0xFFFFFF;

        RenderUtil.drawLine(screenCenterX, 0, screenCenterX, screenHeight, alphaColor);
        RenderUtil.drawLine(0, screenCenterY, screenWidth, screenCenterY, alphaColor);

        float myCenterX = centerX();
        float myCenterY = centerY();

        for (Dragging other : DragManager.draggables.values()) {
            if (other == this) continue;
            float otherCenterX = other.getBoxX() + other.width / 2f;
            float otherCenterY = other.getBoxY() + other.height / 2f;
            if (Math.abs(myCenterX - otherCenterX) < SNAP_DISTANCE) {
                RenderUtil.drawLine(otherCenterX, 0, otherCenterX, screenHeight, alphaColor);
            }
            if (Math.abs(myCenterY - otherCenterY) < SNAP_DISTANCE) {
                RenderUtil.drawLine(0, otherCenterY, screenWidth, otherCenterY, alphaColor);
            }
        }
    }

    public boolean onClick(double mouseX, double mouseY, int button) {
        if (!(mc.currentScreen instanceof ChatScreen)) return false;
        if (button != 0) return false;
        if (width <= 0 || height <= 0) return false;
        if (!rendered) return false;

        if (RenderUtil.isInRegion((int) mouseX, (int) mouseY, getBoxX(), getBoxY(), width, height)) {
            dragging = true;
            startX = (float) (mouseX - xPos);
            startY = (float) (mouseY - yPos);
            return true;
        }
        return false;
    }

    public void onRelease(int button) {
        if (button == 0) dragging = false;
    }

    public boolean isDragging() {
        return dragging;
    }
}