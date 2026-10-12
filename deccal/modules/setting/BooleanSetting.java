package com.deccalware.deccal.modules.setting;


import java.util.function.Supplier;

public class BooleanSetting extends Setting {

    private String name;
    private String desc;
    private boolean status;
    private Supplier<Boolean> visible;
    private com.deccalware.settings.BooleanSetting originalSetting;

    public BooleanSetting(String name, boolean status) {
        this.name = name;
        this.status = status;
        setVisible(() -> true);
    }

    public BooleanSetting(com.deccalware.settings.BooleanSetting originalSetting) {
        this(originalSetting.getName(), originalSetting.getValue());
        this.originalSetting = originalSetting;
    }

    public BooleanSetting(String name, boolean status, String desc) {
        this.name = name;
        this.status = status;
        this.desc = desc;
        setVisible(() -> true);
    }

    public BooleanSetting(String name, boolean status, Supplier<Boolean> visible) {
        this.name = name;
        this.status = status;
        setVisible(visible);
    }

    public BooleanSetting(String name, boolean status, String desc, Supplier<Boolean> visible) {
        this.name = name;
        this.status = status;
        this.desc = desc;
        setVisible(visible);
    }

    public boolean isVisible() {
        return visible.get();
    }

    public void setVisible(Supplier<Boolean> visible) {
        this.visible = visible;
    }


    public String getName() {
        return this.name;
    }

    public String getDesc() {
        return this.desc;
    }
    public boolean get() {
        if (originalSetting != null) {
            return originalSetting.getValue();
        }
        return this.status;
    }

    public void set(boolean value) {
        this.status = value;
        if (originalSetting != null) {
            originalSetting.setValue(value);
        }
    }
}