package com.deccalware.deccal.modules;

import com.deccalware.deccal.modules.setting.Setting;
import com.deccalware.modules.Module;

import java.util.ArrayList;
import java.util.List;

public class DeccalFunction {
    public final Module module;
    public final String name;
    public String desc;
    public String keywords = "";
    public final Type category;
    public int bind;
    public boolean expanded;

    private final List<Setting> settings = new ArrayList<>();

    public DeccalFunction(Module module, Type category) {
        this.module = module;
        this.name = module.getName();
        this.desc = module.getDescription() != null ? module.getDescription() : "";
        this.category = category;
        this.bind = module.getKey() > 0 ? module.getKey() : 0;
    }

    public boolean isState() {
        return module.isEnabled();
    }

    public void setState(boolean state) {
        module.setEnabled(state);
    }

    public void toggle() {
        module.toggle();
    }

    public int getBindCode() {
        return bind;
    }

    public void setBindCode(int code) {
        bind = code;
        module.setKey(code);
    }

    public List<Setting> getSettings() {
        return settings;
    }

    public void addSettings(Setting... options) {
        for (Setting s : options) {
            settings.add(s);
        }
    }
}
