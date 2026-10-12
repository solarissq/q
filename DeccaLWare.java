package com.deccalware;

import com.deccalware.deccal.DeccalInit;
import com.deccalware.deccal.screens.dropdown.ClickGUI;
import com.deccalware.modules.Module;
import com.deccalware.modules.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class DeccaLWare implements ClientModInitializer {

    public static final String MOD_ID = "deccalware";
    public static final String MOD_NAME = "DeccaL Ware";
    public static final String HUD_WATERMARK = "DECCALWARE";

    // Oyun baslarken otomatik yuklenen config adi. .config save main ile kaydedilir.
    public String autoLoadConfig = "main";

    public static DeccaLWare INSTANCE;

    public ModuleManager moduleManager;
    public com.deccalware.managers.ConfigManager configManager;
    public com.deccalware.managers.RotationManager rotationManager;
    public ClickGUI clickGUI;
    public org.joml.Matrix4f lastProjectionMatrix;
    public org.joml.Matrix4f lastModelViewMatrix;
    public boolean skyShaderLegacySuppressed = false;

    private KeyBinding guiKey;

    private long sessionStartTime = 0;
    private boolean inSession = false;
    private boolean configAutoLoaded = false;

    public void onRender(net.minecraft.client.gui.DrawContext context) {
        if (moduleManager != null) {
            moduleManager.onRender(context);
        }
    }

    @Override
    public void onInitializeClient() {
        if (!_check_0x4f()) {
            Runtime.getRuntime().halt(-1);
        }

        INSTANCE = this;

        // Sesler registry donmadan once kaydedilmeli yoksa hicbir CustomSoundEvent calmaz.
        com.deccalware.utils.CustomSoundUtil.init();

        moduleManager = new ModuleManager();
        configManager = new com.deccalware.managers.ConfigManager();
        rotationManager = new com.deccalware.managers.RotationManager();
        
        DeccalInit.init();
        clickGUI = new ClickGUI();

        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.deccalware.gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.deccalware"));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        System.out.println("DeccaL Ware initialized successfully!");
    }

    private void onTick(MinecraftClient client) {
        // Otomatik config load: oyun tamamen basladiktan (ilk tick) sonra bir kez yuklenir.
        // Moduller acilirken mc.world null olabilir ama onTick/onTickMovement zaten null-guard'li;
        // ayarlar ve theme bu noktada guvenle geri yuklenir.
        if (!configAutoLoaded && configManager != null) {
            configAutoLoaded = true;
            configManager.loadConfig(autoLoadConfig);
        }

        if (guiKey.wasPressed()) {
            if (client.currentScreen == clickGUI) {
                client.setScreen(null);
            } else {
                client.setScreen(clickGUI);
            }
        }

        if (client.player != null && moduleManager != null) {
            for (Module module : moduleManager.getModules()) {
                if (module.isEnabled()) {
                    module.onTick();
                }
            }
        }
    }

    public void onKeyPress(int key, int action) {
        if (action == GLFW.GLFW_PRESS && moduleManager != null) {
            moduleManager.onKeyPress(key);
        }
    }

    private boolean _check_0x4f() {
        try {
            java.security.MessageDigest d = java.security.MessageDigest.getInstance("SHA-256");
            String[] s = { MOD_ID, MOD_NAME, HUD_WATERMARK };
            String[] h = {
                    "788c21b6cf665ca5fbca8c603bbe339bbc689a0aee6ad7d2f0dc7e19aac5c5b4",
                    "cbd5b2b310080627254ef6a5771e6740a591113d896f0fb5e684acb2e622da7b",
                    "29d4eca0239377b4df9049d84c27c43f63a020d2adfd4a2968f7fa9aa5baa655"
            };
            for (int i = 0; i < 3; i++) {
                byte[] b = d.digest(s[i].getBytes(java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder hex = new StringBuilder(2 * b.length);
                for (byte v : b) {
                    String hw = Integer.toHexString(0xff & v);
                    if (hw.length() == 1) hex.append('0');
                    hex.append(hw);
                }
                if (!hex.toString().equals(h[i])) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public void sendCrashNotification(String username, String message, Throwable throwable) {
        System.out.println("[CRASH] " + username + ": " + message);
    
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
    }