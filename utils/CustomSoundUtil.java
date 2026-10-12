package com.deccalware.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * These sound events are registered in assets/deccalware/sounds.json and
 * played through vanilla's sound manager (purely client-side audio).
 */
public class CustomSoundUtil {
    public static final SoundEvent CLICK_LEFT = register("gui_click_left");
    public static final SoundEvent CLICK_RIGHT = register("gui_click_right");
    public static final SoundEvent GUI_CLOSE = register("gui_close");
    public static final SoundEvent GUI_OPEN = register("gui_open");
    public static final SoundEvent MODULE_DISABLE = register("module_disable");
    public static final SoundEvent MODULE_ENABLE = register("module_enable");
    public static final SoundEvent SLIDER_STEP = register("slider_step");

    private static final Map<String, SoundEvent> BY_NAME = new HashMap<>();
    static {
        for (String name : SoundBank.SOUNDS) {
            if (!name.equals("None")) {
                BY_NAME.put(name, register(name));
            }
        }
    }

    public static SoundEvent byName(String name) {
        return BY_NAME.get(name);
    }

    private static boolean started;

    /** Registry freeze'den ÖNCE (oyun baslangicinda) tum ses event'lerini kaydettirir. */
    public static void init() {
        if (started)
            return;
        started = true;
    }

    private static SoundEvent register(String name) {
        Identifier id = Identifier.of("deccalware", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void play(SoundEvent event) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getSoundManager() != null) {
                mc.getSoundManager().play(PositionedSoundInstance.master(event, 1.0F));
            }
        } catch (Exception ignored) {}
    }
}