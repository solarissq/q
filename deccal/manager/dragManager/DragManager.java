package com.deccalware.deccal.manager.dragManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import com.deccalware.deccal.manager.IMinecraft;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;

/**
 * Tum HUD widget'larinin konum kayitlarini tutar.
 *
 * Widget'lar oyun acilirken (JSON okunmadan ONCE) register edilir; boylece
 * kayitli konumlar var olan widget'lara uygulanir ve widget'in kendisi
 * ilk render'da "getOrCreate" ile yeni bir nesne yaratmaz.
 */
public class DragManager implements IMinecraft {
    public static LinkedHashMap<String, Dragging> draggables = new LinkedHashMap<>();

    public static final String WATERMARK = "WaterMark";
    public static final String ARRAYLIST = "ArrayList";
    public static final String SPOTIFY = "SpotifyHUD";
    public static final String KEYSTROKES = "KeystrokesHUD";
    public static final String TARGETHUD = "TargetHUD";
    public static final String TOTEMHUD = "TotemHUD";

    public final File DRAG_DATA = new File(MinecraftClient.getInstance().runDirectory, "\\files\\drag.ew");
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

    /** Widget'i kayitliyse getirir, degilse default konumda olusturup kaydeder. */
    public static Dragging getOrCreate(String name, float defaultX, float defaultY) {
        Dragging existing = draggables.get(name);
        if (existing != null) return existing;
        return new Dragging(null, name, defaultX, defaultY);
    }

    /** Pencere hazir degilse (cok erken init) guvenli varsayilan olcu. */
    public static int screenWidth() {
        try {
            return mc.getWindow().getScaledWidth();
        } catch (Throwable ignored) {
            return 854;
        }
    }

    public static int screenHeight() {
        try {
            return mc.getWindow().getScaledHeight();
        } catch (Throwable ignored) {
            return 480;
        }
    }

    private static int lastScreenW = 0;
    private static int lastScreenH = 0;

    /**
     * Cozunurluk degisimini yakalar ve tum widget'lari ORANLA olarak yeni
     * cozunurluge tasir. F11 (tam ekran) iki kez basildiginda widget'lar
     * eski yerine doner; hicbir widget ekran disinakirilmaz/kirpilmaz.
     *
     * Gecersiz araliklar (0 veya aniden 4x buyuk/kucuk) yok sayilir: bunlar
     * pencere gecisinin 1-2 bozuk karesidir ve konumlari bozmasina izin
     * verilmez.
     */
    public static void handleScreenResize() {
        int w = screenWidth();
        int h = screenHeight();

        if (w <= 0 || h <= 0) return;

        if (lastScreenW <= 0 || lastScreenH <= 0) {
            lastScreenW = w;
            lastScreenH = h;
            return;
        }

        if (w == lastScreenW && h == lastScreenH) return;

        // Bozuk gecis karesi: 0'a cok yakin veya absurd oran.
        if (w < lastScreenW / 4 || h < lastScreenH / 4 || w > lastScreenW * 4 || h > lastScreenH * 4) {
            return;
        }

        for (Dragging d : draggables.values()) {
            d.rescaleToScreen(lastScreenW, lastScreenH, w, h);
        }
        lastScreenW = w;
        lastScreenH = h;
    }

    private void registerDefaults() {
        int w = screenWidth();
        int h = screenHeight();
        lastScreenW = w;
        lastScreenH = h;

        getOrCreate(WATERMARK, 10, 10);
        getOrCreate(ARRAYLIST, w - 8, 5);
        getOrCreate(SPOTIFY, 10, 46);
        getOrCreate(KEYSTROKES, 6, 120);
        getOrCreate(TARGETHUD, w / 2f + 10, h / 2f + 10);
        getOrCreate(TOTEMHUD, w / 2f - 8, h - 60);
    }

    public void init() {
        registerDefaults();

        if (!DRAG_DATA.exists()) {
            System.out.println("[DragManager] Konum dosyasi bulunamadi, varsayilan konumlar kullanilacak: " + DRAG_DATA.getAbsolutePath());
            return;
        }

        try {
            Dragging[] loadedDrags = GSON.fromJson(Files.readString(DRAG_DATA.toPath()), Dragging[].class);

            if (loadedDrags == null) {
                System.out.println("[DragManager] Konum dosyasi okunamadi, varsayilan konumlar kullanilacak.");
                return;
            }

            for (Dragging dragging : loadedDrags) {
                if (dragging == null) continue;

                Dragging currentDrag = draggables.get(dragging.getName());
                if (currentDrag == null) {
                    // Eski surumden kalma, artik kullanilmayan widget: yoksay.
                    continue;
                }
                currentDrag.setX(dragging.getX());
                currentDrag.setY(dragging.getY());
            }
            System.out.println("[DragManager] Widget konumlari yuklendi.");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void save() {
        if (!DRAG_DATA.exists()) {
            DRAG_DATA.getParentFile().mkdirs();
        }

        try (FileWriter writer = new FileWriter(DRAG_DATA)) {
            writer.write(GSON.toJson(draggables.values()));
            System.out.println("[DragManager] Widget konumlari kaydedildi: " + DRAG_DATA.getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void reset() {
        for (Dragging dragging : draggables.values()) {
            dragging.setX(dragging.getDefaultX());
            dragging.setY(dragging.getDefaultY());
            dragging.targetX = dragging.getDefaultX();
            dragging.targetY = dragging.getDefaultY();
            dragging.clampToScreen();
        }
        save();
    }
}