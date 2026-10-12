package com.deccalware.deccal.modules.setting;

/**
 * ClickGUI icin kaynak KeybindSetting'i BindSetting olarak saran render-side
 * sarmalayici. BindSettingRenderer/ClickGUI'e degisiklik gerekmeden
 * "tusa tikla -> tus atanir" akisini calistirir; tus degisimleri dogrudan
 * kaynak ayara (ConfigManager + module) isler.
 */
public class KeyBindSetting extends BindSetting {

    private final com.deccalware.settings.KeybindSetting original;

    public KeyBindSetting(com.deccalware.settings.KeybindSetting original) {
        super(original.getName(), original.getInt());
        this.original = original;
    }

    @Override
    public int getKey() {
        return original.getInt();
    }

    @Override
    public void setKey(int key) {
        original.setInt(key);
    }

    @Override
    public boolean isVisible() {
        return original.isVisible();
    }
}