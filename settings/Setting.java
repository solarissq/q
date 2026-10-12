package com.deccalware.settings;

import java.util.function.Predicate;

public class Setting<T> {
    private String name;
    private T value;
    private T defaultValue;
    private Predicate<T> visibility;

    public Setting(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.visibility = v -> true;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public boolean isVisible() {
        return visibility.test(value);
    }

    public void setVisibility(Predicate<T> visibility) {
        this.visibility = visibility;
    }
}
