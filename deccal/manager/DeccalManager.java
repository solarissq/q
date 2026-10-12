package com.deccalware.deccal.manager;

import com.deccalware.deccal.manager.dragManager.DragManager;
import com.deccalware.deccal.manager.fontManager.FontUtils;
import com.deccalware.deccal.manager.themeManager.StyleManager;
import com.deccalware.deccal.modules.DeccalFunctionManager;

public final class DeccalManager {
    public static DeccalFunctionManager FUNCTION_MANAGER;
    public static StyleManager STYLE_MANAGER;
    public static FontUtils FONT_MANAGER;
    public static DragManager DRAG_MANAGER;
    public static SyncManager SYNC_MANAGER;
    public static final DeccalUserProfile USER_PROFILE = new DeccalUserProfile();

    private DeccalManager() {
    }

    public static void init() {
        SYNC_MANAGER = new SyncManager();
        STYLE_MANAGER = new StyleManager();
        STYLE_MANAGER.init();
        FONT_MANAGER = new FontUtils();
        FONT_MANAGER.init();
        DRAG_MANAGER = new DragManager();
        DRAG_MANAGER.init();
        FUNCTION_MANAGER = new DeccalFunctionManager();
    }

    public static final class DeccalUserProfile {
        public String getName() {
            if (IMinecraft.mc != null && IMinecraft.mc.getSession() != null) {
                return IMinecraft.mc.getSession().getUsername();
            }
            return "Player";
        }

        public String getExpiry() {
            return "";
        }
    }
}
