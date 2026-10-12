package com.deccalware.deccal.manager.fontManager;

@SuppressWarnings("All")
public class FontUtils {

    public static volatile BuiltinFont[] comfortaa = new BuiltinFont[256];
    public static volatile BuiltinFont[] durman = new BuiltinFont[256];
    public static volatile BuiltinFont[] glitched = new BuiltinFont[256];
    public static volatile BuiltinFont[] icons = new BuiltinFont[256];
    public static volatile BuiltinFont[] monsterrat = new BuiltinFont[256];
    public static volatile BuiltinFont[] profont = new BuiltinFont[256];
    public static volatile BuiltinFont[] sf_bold = new BuiltinFont[256];
    public static volatile BuiltinFont[] sf_medium = new BuiltinFont[256];
    public static volatile BuiltinFont[] iconsWex = new BuiltinFont[256];
    public static volatile BuiltinFont[] gilroy = new BuiltinFont[256];
    public static volatile BuiltinFont[] gilroy_bold = new BuiltinFont[256];
    public static volatile BuiltinFont[] hud = new BuiltinFont[256];
    public static volatile BuiltinFont[] icomoon = new BuiltinFont[256];

    private boolean initialized = false;

    public synchronized void init() {
        if (initialized) {
            return;
        }
        for (int i = 1; i < 256; i++) {
            BuiltinFont font = new BuiltinFont(i);
            comfortaa[i] = font;
            durman[i] = font;
            glitched[i] = font;
            icons[i] = font;
            monsterrat[i] = font;
            profont[i] = font;
            sf_bold[i] = font;
            sf_medium[i] = font;
            iconsWex[i] = font;
            gilroy[i] = font;
            gilroy_bold[i] = font;
            hud[i] = font;
            icomoon[i] = font;
        }
        initialized = true;
    }
}
