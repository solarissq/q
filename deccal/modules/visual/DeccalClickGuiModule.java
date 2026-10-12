package com.deccalware.deccal.modules.visual;

import com.deccalware.deccal.modules.setting.BooleanSetting;
import com.deccalware.deccal.modules.setting.ModeSetting;
import com.deccalware.deccal.modules.setting.MultiSetting;
import com.deccalware.deccal.modules.setting.SliderSetting;

import java.awt.Color;
import java.util.Arrays;

public class DeccalClickGuiModule {
    public final ModeSetting guiStyle = new ModeSetting("GUI Style", "Standard", "Standard", "Minimal", "Outline", "Neon");
    public final ModeSetting colorGUI = new ModeSetting("Тема", "Светло-чёрная", "Светло-чёрная", "Тёмная");
    public final SliderSetting alpha = new SliderSetting("Прозрачность", 200f, 120f, 255f, 1f);
    public final BooleanSetting blur = new BooleanSetting("Размытие", true);
    public final MultiSetting blurSetting = new MultiSetting(
            () -> blur.get(),
            "Элементы",
            Arrays.asList("Поиск", "Панели"),
            new String[] { "Поиск", "Темы", "Панели", "Описание", "Создание темы" });
    public final BooleanSetting strike = new BooleanSetting("Обводка для модулей", true);
    public final BooleanSetting filling = new BooleanSetting("Заливка для модулей", true);
    public final SliderSetting rounding = new SliderSetting("Закругление", 4, 0, 6, 1, () -> strike.get() || filling.get());
    public final SliderSetting alphaModules = new SliderSetting("Прозрачность модулей", 20f, 10f, 40f, 1f,
            () -> strike.get() || filling.get());

    public DeccalClickGuiModule() {
    }

    public Color getGuiColor() {
        return switch (colorGUI.get()) {
            case "Тёмная" -> new Color(0, 0, 0, alpha.get().intValue());
            default -> new Color(17, 15, 28, alpha.get().intValue());
        };
    }
}
