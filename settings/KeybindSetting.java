package com.deccalware.settings;

/**
 * Degistirilebilir bir klavye tusu saklayan ayar (GLFW keycode veya -1 = yok).
 * ClickGUI'de BindSetting gibi render edilir; tus once pressed oldugunda
 * kayitli action tetiklenir (ModuleManager.onKeyPress).
 */
public class KeybindSetting extends Setting<Integer> {

    private Runnable action;

    public KeybindSetting(String name, int defaultKey) {
        super(name, defaultKey);
    }

    public int getInt() {
        Integer v = getValue();
        return v != null ? v : -1;
    }

    public void setInt(int key) {
        setValue(key);
    }

    public Runnable getAction() {
        return action;
    }

    public void setAction(Runnable action) {
        this.action = action;
    }
}