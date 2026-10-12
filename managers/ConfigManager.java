package com.deccalware.managers;

import com.deccalware.DeccaLWare;
import com.deccalware.modules.Module;
import com.deccalware.settings.BooleanSetting;
import com.deccalware.settings.KeybindSetting;
import com.deccalware.settings.ModeSetting;
import com.deccalware.settings.NumberSetting;
import com.deccalware.settings.Setting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class ConfigManager {
    private final File configDir;
    private final Gson gson;

    public ConfigManager() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("DeccaLWare");
        this.configDir = configPath.toFile();
        if (!this.configDir.exists()) {
            this.configDir.mkdirs();
        }
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void saveConfig(String name) {
        File file = new File(configDir, name + ".deccalware");
        JsonObject json = new JsonObject();

        // GUI / tema rengi (StyleManager) config'e kaydedilip load'da geri yuklenir.
        com.deccalware.deccal.manager.themeManager.Style theme =
                com.deccalware.deccal.manager.DeccalManager.STYLE_MANAGER.getTheme();
        if (theme != null) {
            json.addProperty("theme", theme.name);
        }

        json.addProperty("guiStyle",
                com.deccalware.deccal.manager.DeccalManager.FUNCTION_MANAGER.clickGUI.guiStyle.get());

        for (Module module : DeccaLWare.INSTANCE.moduleManager.getModules()) {
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("enabled", module.isEnabled());
            moduleJson.addProperty("key", module.getKey());

            JsonObject settingsJson = new JsonObject();
            for (Setting<?> setting : module.getSettings()) {
                if (setting instanceof BooleanSetting booleanSetting) {
                    settingsJson.addProperty(setting.getName(), booleanSetting.getValue());
                    if (booleanSetting.getKey() != -1) {
                        settingsJson.addProperty(setting.getName() + "-bind", booleanSetting.getKey());
                    }
                } else if (setting instanceof NumberSetting) {
                    settingsJson.addProperty(setting.getName(), ((NumberSetting) setting).getValue());
                } else if (setting instanceof KeybindSetting keybindSetting) {
                    settingsJson.addProperty(setting.getName(), keybindSetting.getInt());
                } else if (setting instanceof ModeSetting) {
                    settingsJson.addProperty(setting.getName(), ((ModeSetting) setting).getValue());
                }
            }
            moduleJson.add("settings", settingsJson);
            json.add(module.getName(), moduleJson);
        }

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadConfig(String name) {
        File file = new File(configDir, name + ".deccalware");
        if (!file.exists())
            return;

        try (FileReader reader = new FileReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            // GUI / tema rengi: kayitli tema adini bul ve uygula (yoksa/degismisse atla)
            if (json.has("theme")) {
                String themeName = json.get("theme").getAsString();
                for (com.deccalware.deccal.manager.themeManager.Style s
                        : com.deccalware.deccal.manager.DeccalManager.STYLE_MANAGER.getStyles()) {
                    if (s.name.equals(themeName)) {
                        com.deccalware.deccal.manager.DeccalManager.STYLE_MANAGER.setTheme(s);
                        break;
                    }
                }
            }

if (json.has("guiStyle")) {
                String guiStyle = json.get("guiStyle").getAsString();
                com.deccalware.deccal.manager.DeccalManager.FUNCTION_MANAGER.clickGUI.guiStyle.set(guiStyle);
            }

            for (Module module : DeccaLWare.INSTANCE.moduleManager.getModules()) {
                if (!json.has(module.getName()))
                    continue;

                JsonObject moduleJson = json.get(module.getName()).getAsJsonObject();
                if (moduleJson.has("enabled")) {
                    boolean enabled = moduleJson.get("enabled").getAsBoolean();
                    if (enabled != module.isEnabled())
                        module.toggle();
                }
                if (moduleJson.has("key")) {
                    module.setKey(moduleJson.get("key").getAsInt());
                }

                if (moduleJson.has("settings")) {
                    JsonObject settingsJson = moduleJson.get("settings").getAsJsonObject();
                    for (Setting<?> setting : module.getSettings()) {
                        if (!settingsJson.has(setting.getName()))
                            continue;
                        JsonElement element = settingsJson.get(setting.getName());

                        if (setting instanceof BooleanSetting booleanSetting) {
                            booleanSetting.setValue(element.getAsBoolean());
                            if (settingsJson.has(setting.getName() + "-bind")) {
                                booleanSetting.setKey(settingsJson.get(setting.getName() + "-bind").getAsInt());
                            }
                        } else if (setting instanceof NumberSetting) {
                            ((NumberSetting) setting).setValue(element.getAsDouble());
                        } else if (setting instanceof KeybindSetting keybindSetting) {
                            keybindSetting.setInt(element.getAsInt());
                        } else if (setting instanceof ModeSetting) {
                            ((ModeSetting) setting).setValue(element.getAsString());
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
