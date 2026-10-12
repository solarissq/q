package com.deccalware.settings;

public class EnumSetting<T extends Enum<T>> extends Setting<T> {
    private final T[] modes;
    private int index;

    @SuppressWarnings("unchecked")
    public EnumSetting(String name, T defaultValue) {
        super(name, defaultValue);
        this.modes = (T[]) defaultValue.getClass().getEnumConstants();
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] == defaultValue) {
                this.index = i;
                break;
            }
        }
    }

    public void cycle() {
        index = (index + 1) % modes.length;
        setValue(modes[index]);
    }

    public T[] getModes() {
        return modes;
    }
}
