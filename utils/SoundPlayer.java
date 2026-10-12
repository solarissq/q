package com.deccalware.utils;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class SoundPlayer {
    private static Clip cachedClip = null;

    public static void playCustomSound() {
        new Thread(() -> {
            try {
                if (cachedClip == null) {
                    InputStream is = SoundPlayer.class.getResourceAsStream("/assets/deccalware/totem_sesi.wav");
                    if (is != null) {
                        InputStream bufferedIn = new BufferedInputStream(is);
                        AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
                        cachedClip = AudioSystem.getClip();
                        cachedClip.open(audioStream);
                    }
                }
                if (cachedClip != null) {
                    cachedClip.setFramePosition(0);
                    cachedClip.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}