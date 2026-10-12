package com.deccalware.deccal.modules;

import com.deccalware.DeccaLWare;
import com.deccalware.deccal.modules.setting.*;
import com.deccalware.deccal.modules.visual.DeccalClickGuiModule;
import com.deccalware.modules.Module;
import com.deccalware.settings.BooleanSetting;
import com.deccalware.settings.KeybindSetting;
import com.deccalware.settings.ModeSetting;
import com.deccalware.settings.NumberSetting;
import com.deccalware.settings.Setting;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class DeccalFunctionManager {
    public final DeccalClickGuiModule clickGUI = new DeccalClickGuiModule();
    private final List<DeccalFunction> functions = new ArrayList<>();
    private final Map<Type, List<DeccalFunction>> byCategory = new EnumMap<>(Type.class);

    public DeccalFunctionManager() {
        for (Type type : Type.values()) {
            byCategory.put(type, new ArrayList<>());
        }
        for (Module module : DeccaLWare.INSTANCE.moduleManager.getModules()) {
            if ("ClickGUI".equalsIgnoreCase(module.getName())) {
                continue;
            }
            Type type = mapCategory(module.getCategory());
            DeccalFunction fn = new DeccalFunction(module, type);
            convertSettings(module, fn);
            functions.add(fn);
            byCategory.get(type).add(fn);
        }
    }

    public List<DeccalFunction> getFunctions() {
        return functions;
    }

    public List<DeccalFunction> getFunctions(Type category) {
        return byCategory.getOrDefault(category, List.of());
    }

    private static Type mapCategory(Module.Category category) {
        return switch (category) {
            case COMBAT, CRYSTAL, HVH -> Type.Combat;
            case MOVEMENT -> Type.Move;
            case VISUAL -> Type.Render;
            case PLAYER -> Type.Player;
            default -> Type.Misc;
        };
    }

    private static void convertSettings(Module module, DeccalFunction fn) {
        for (com.deccalware.settings.Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting bs) {
                fn.addSettings(new com.deccalware.deccal.modules.setting.BindBooleanSetting(bs));
            } else if (setting instanceof KeybindSetting ks) {
                fn.addSettings(new KeyBindSetting(ks));
            } else if (setting instanceof NumberSetting ns) {
                fn.addSettings(new SliderSetting(ns));
            } else if (setting instanceof ModeSetting ms) {
                fn.addSettings(new com.deccalware.deccal.modules.setting.ModeSetting(ms));
            }
        }
    }
}
