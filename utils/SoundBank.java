package com.deccalware.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class SoundBank {
    public static final String[] SOUNDS = {"None", "anime", "hitmaker", "loud", "bell", "bonk", "bubble", "metallic"};

    private SoundBank() {
    }

    public static void play(String name) {
        play(name, 1.0F);
    }

    public static void play(String name, float volume) {
        if (name == null || name.equals("None"))
            return;
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getSoundManager() != null) {
                SoundEvent ev = CustomSoundUtil.byName(name);
                if (ev != null) {
                    // Gain asla 1.0'i asmaz -> kırpma (clip) olmaz, ses temiz kalir.
                    float gain = Math.max(0.05F, Math.min(1.0F, volume));
                    // Ayni ses zaten caliyorsa once kes -> hizli vuruslarda ust uste binmez.
                    mc.getSoundManager().stopSounds(ev.id(), SoundCategory.MASTER);
                    mc.getSoundManager().play(PositionedSoundInstance.master(ev, gain, 1.0F));
                }
            }
        } catch (Exception ignored) {
        }
    }
}