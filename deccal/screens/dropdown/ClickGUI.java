package com.deccalware.deccal.screens.dropdown;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4i;
import org.lwjgl.glfw.GLFW;
import com.deccalware.deccal.manager.ClientManager;
import com.deccalware.deccal.manager.DeccalManager;
import com.deccalware.deccal.manager.IMinecraft;
import com.deccalware.deccal.manager.themeManager.Style;
import com.deccalware.deccal.modules.DeccalFunction;
import com.deccalware.deccal.modules.Type;
import com.deccalware.deccal.modules.setting.*;
// import com.deccalware.deccal.protect.AES; // removed – not used in current version
import com.deccalware.deccal.screens.dropdown.impl.*;
import com.deccalware.deccal.screens.dropdown.impl.*;
import com.deccalware.deccal.screens.dropdown.search.SearchState;
import com.deccalware.deccal.util.animations.impl.EaseInOutQuad;
import com.deccalware.deccal.util.color.ColorUtil;
import com.deccalware.deccal.manager.fontManager.FontUtils;
import com.deccalware.deccal.util.math.MathUtil;
import com.deccalware.deccal.util.render.RenderUtil;
import com.deccalware.deccal.util.render.Scissor;
import com.deccalware.deccal.util.render.providers.ResourceProvider;
import java.awt.*;
import java.util.*;

import static com.deccalware.deccal.util.render.RenderUtil.drawBlur;

public class ClickGUI extends Screen implements IMinecraft {
    private boolean isClose;

    private final int PANEL_WIDTH = 135;
    private final int PANEL_HEIGHT = 280;
    private final int PANEL_MARGIN = 8;

    private Color GUI_COLOR = DeccalManager.FUNCTION_MANAGER.clickGUI.getGuiColor();

    private final int TITLE_MARGIN_TOP = 5;
    private final int TITLE_HEIGHT = 20;

    private final int FUNCTION_HEIGHT = 20;

    private final int SCROLL_AREA_Y_OFFSET = TITLE_MARGIN_TOP + TITLE_HEIGHT;
    private final int SCROLL_AREA_HEIGHT = PANEL_HEIGHT - SCROLL_AREA_Y_OFFSET - 5;

    private final Set<Type> renderCategories = EnumSet.of(Type.Combat, Type.Move, Type.Render, Type.Player, Type.Misc);
    private static final Map<Type, Float> scrollOffsets = new HashMap<>();
    private static final Map<Type, Float> scrollTargets = new HashMap<>();
    private final Map<DeccalFunction, Float> arrowRotationProgress = new HashMap<>();
    private final EaseInOutQuad animationOpen = new EaseInOutQuad(250, 1);
    private double animation;

    private final Map<DeccalFunction, Float> expandProgress = new HashMap<>();
    private final Map<DeccalFunction, Float> hoverProgress = new HashMap<>();

    private final SearchState searchState;
    private boolean functionBinding = false;
    private DeccalFunction functions = null;

    private SliderSetting draggingSlider = null;
    private int draggingSliderX = 0;
    private int draggingSliderWidth = 0;

    private final BooleanSettingRenderer booleanSettingRenderer = new BooleanSettingRenderer();
    private final BindBooleanSettingRenderer bindbooleanSettingRenderer = new BindBooleanSettingRenderer();
    private final BindSettingRenderer bindSettingRenderer = new BindSettingRenderer();
    private final ModeSettingRenderer modeSettingRenderer = new ModeSettingRenderer();
    private final MultiSettingRenderer multiSettingRenderer = new MultiSettingRenderer();
    private final SliderSettingRenderer sliderSettingRenderer = new SliderSettingRenderer();
    private final TextSettingRenderer textSettingRenderer = new TextSettingRenderer();

    private final int SEARCH_HEIGHT = 20;
    private final int SEARCH_MARGIN_BOTTOM = 10;
    private final int SEARCH_MAX_WIDTH = 180;

    private final int THEME_HEIGHT = 16;
    private final int THEME_MARGIN_BOTTOM = 40;
    private final int THEME_MAX_WIDTH = 180;
    private static float themeScrollOffset = 0;
    private static float themeScrollTarget = 0;
    private final int VISIBLE_THEMES = 11;

    private float themeMenuAnim = 0f;
    private float themeMenuTarget = 0f;
    private float themeAlphaAnim = 0f;
    private static final float THEME_ANIM_SPEED = 0.2f;
    private static boolean themeMenu;
    private float themeNameAnim = 0f;

    private final float SCROLL_SPEED = 12f;
    private final float SCROLL_LERP_FACTOR = 20f;
    private final float SCROLL_SMOOTH_FACTOR = 12f;

    private final float THEME_SCROLL_SPEED = 15f;
    private final float THEME_SCROLL_LERP_FACTOR = 15f;

    private static boolean colorPickerOpen = false;
    private static int selectedColor1 = Color.WHITE.getRGB();
    private static int selectedColor2 = Color.WHITE.getRGB();

    private float picker1CursorX = 0.5f;
    private float picker1CursorY = 0.5f;
    private float picker2CursorX = 0.5f;
    private float picker2CursorY = 0.5f;
    private boolean draggingPicker1 = false;
    private boolean draggingPicker2 = false;
    private float colorPickerAnim = 0f;

    private static final String[] GUI_STYLES = {"Standard", "Minimal", "Outline", "Neon"};

    public ClickGUI() {
        super(Text.literal("ClickGUI"));
        this.searchState = new SearchState();
        if (scrollOffsets.isEmpty() && scrollTargets.isEmpty()) {
            renderCategories.forEach(cat -> {
                scrollOffsets.put(cat, 0f);
                scrollTargets.put(cat, 0f);
            });
        }
    }

    private String guiStyle() {
        return DeccalManager.FUNCTION_MANAGER.clickGUI.guiStyle.get();
    }

    private static float guiShift() {
        return (System.currentTimeMillis() / 50f) % 360f;
    }

    @Override
    public void init() {
        super.init();

        isClose = false;
        animationOpen.setDirection(Direction.AxisDirection.POSITIVE);
        animationOpen.reset();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        GUI_COLOR = DeccalManager.FUNCTION_MANAGER.clickGUI.getGuiColor();
        animation = animationOpen.getOutput();
        if (isClose && animationOpen.finished(Direction.AxisDirection.NEGATIVE)) {
            super.close();
            return;
        }
        if (animation <= 0.01) return;

        super.render(ctx, mouseX, mouseY, delta);
        ctx.getMatrices().push();

        for (Type category : renderCategories) {
            float target = scrollTargets.get(category);
            float current = scrollOffsets.get(category);
            float newOffset = MathUtil.lerp(current, target, SCROLL_LERP_FACTOR);
            scrollOffsets.put(category, newOffset);
        }

        int totalWidth = renderCategories.size() * (PANEL_WIDTH + PANEL_MARGIN) - PANEL_MARGIN;
        int startX = (width - totalWidth) / 2;
        int startY = (height - PANEL_HEIGHT) / 2;

        // Inline sizeAnimation: scale from center
        ctx.getMatrices().translate(width / 2f, height / 2f, 0);
        ctx.getMatrices().scale((float) animation, (float) animation, 1f);
        ctx.getMatrices().translate(-width / 2f, -height / 2f, 0);

        int idx = 0;
        for (Type category : renderCategories) {
            renderPanel(ctx, startX + idx++ * (PANEL_WIDTH + PANEL_MARGIN), startY, category, mouseX, mouseY);
        }

        DeccalFunction hoveredFunction = getHoveredFunction(mouseX, mouseY, startX, startY);
        if (hoveredFunction != null && hoveredFunction.desc != null && !hoveredFunction.desc.isEmpty()) {
            drawDescription(ctx, hoveredFunction.desc, startY);
        }
        DescriptionRenderQueue.renderAll(ctx);

        renderSearchField(ctx);
        renderButtomTheme(ctx, mouseX, mouseY);
        renderTheme(ctx,mouseX,mouseY);
        ctx.getMatrices().pop();
    }
    private DeccalFunction getHoveredFunction(int mouseX, int mouseY, int startX, int startY) {
        int idx = 0;
        for (Type category : renderCategories) {
            int panelX = startX + idx++ * (PANEL_WIDTH + PANEL_MARGIN);

            if (mouseX < panelX || mouseX > panelX + PANEL_WIDTH || mouseY < startY || mouseY > startY + PANEL_HEIGHT) {
                continue;
            }

            float offset = scrollOffsets.get(category);

            float currentY = startY + SCROLL_AREA_Y_OFFSET - offset;

            for (DeccalFunction DeccalFunction : DeccalManager.FUNCTION_MANAGER.getFunctions(category)) {
                if (!isFunctionVisible(DeccalFunction)) continue;

                int functionHeight = FUNCTION_HEIGHT;

                int fullSettingsHeight = computeSettingsHeight(DeccalFunction);
                float progress = expandProgress.getOrDefault(DeccalFunction, DeccalFunction.expanded ? 1f : 0f);
                int animatedSettingsHeight = (int) Math.round(fullSettingsHeight * progress);

                int totalHeight = functionHeight + animatedSettingsHeight;

                if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH && mouseY >= currentY && mouseY <= currentY + functionHeight && mouseY >= startY + SCROLL_AREA_Y_OFFSET &&
                        mouseY <= startY + SCROLL_AREA_Y_OFFSET + SCROLL_AREA_HEIGHT) {
                    return DeccalFunction;
                }

                if (animatedSettingsHeight > 0) {
                    float settingY = currentY + functionHeight;
                    int remaining = animatedSettingsHeight;

                    for (Setting setting : DeccalFunction.getSettings()) {
                        if (!setting.isVisible()) continue;

                        int settingHeight = getSettingRendererHeight(setting, PANEL_WIDTH - 20);
                        if (settingHeight <= 0) continue;

                        int visible = Math.max(0, Math.min(settingHeight, remaining));
                        if (visible <= 0) break;

                        if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH &&
                                mouseY >= settingY && mouseY <= settingY + visible &&
                                mouseY >= startY + SCROLL_AREA_Y_OFFSET &&
                                mouseY <= startY + SCROLL_AREA_Y_OFFSET + SCROLL_AREA_HEIGHT) {
                            return DeccalFunction;
                        }

                        settingY += settingHeight + 1;
                        remaining -= (settingHeight + 1);
                        if (remaining <= 0) break;
                    }
                }

                currentY += totalHeight;
            }
        }
        return null;
    }

    private void drawDescription(DrawContext ctx, String desc, int startY) {
        int descWidth = (int) FontUtils.durman[19].getWidth(desc);
        int descHeight = 20;

        int descX = (width - descWidth) / 2;
        int descY = startY - descHeight - 10;


        if (DeccalManager.FUNCTION_MANAGER.clickGUI.blur.get() && DeccalManager.FUNCTION_MANAGER.clickGUI.blurSetting.get("Описание")) {
            drawBlur(ctx.getMatrices(), descX - 6, descY - 3.5f, descWidth + 12, descHeight, 12, 8, -1);
        }

        RenderUtil.drawRoundedRect(ctx.getMatrices(), descX - 6, descY - 3.5f, descWidth + 12, descHeight, 6, GUI_COLOR.getRGB());
        FontUtils.durman[19].drawLeftAligned(ctx.getMatrices(), desc, descX, descY, Color.WHITE.getRGB());
    }

    private float updateExpandAnimation(DeccalFunction f) {
        float target = f.expanded ? 1f : 0f;
        float prog = expandProgress.getOrDefault(f, target);

        prog = MathUtil.lerp(prog, target, 15f);

        if (Math.abs(target - prog) < 0.001f) prog = target;
        expandProgress.put(f, prog);

        return prog;
    }

    private void renderPanel(DrawContext ctx, int x, int y, Type category, int mouseX, int mouseY) {
        if (DeccalManager.FUNCTION_MANAGER.clickGUI.blur.get() && DeccalManager.FUNCTION_MANAGER.clickGUI.blurSetting.get("Панели")) {
            drawBlur(ctx.getMatrices(), x, y, PANEL_WIDTH, PANEL_HEIGHT, 12, 8, -1);
        }

        String title = category.name();
        String icon = category.icon;
        int titleColor = Color.WHITE.getRGB();

        switch (guiStyle()) {
            case "Minimal" -> {
                RenderUtil.drawRoundedRect(ctx.getMatrices(), x, y, PANEL_WIDTH, PANEL_HEIGHT, 4, new Color(17, 15, 28, 140).getRGB());
                FontUtils.sf_medium[15].centeredDraw(ctx.getMatrices(), title, x + PANEL_WIDTH / 2f, y + TITLE_MARGIN_TOP + 2, titleColor);
                RenderUtil.drawRoundedRect(ctx.getMatrices(), x + 6, y + TITLE_MARGIN_TOP + 17, PANEL_WIDTH - 12, 1, 1, ColorUtil.getColorStyle(30));
            }
            case "Outline" -> {
                RenderUtil.drawRoundedRect(ctx.getMatrices(), x, y, PANEL_WIDTH, PANEL_HEIGHT, 0, new Color(12, 12, 12, 150).getRGB());
                RenderUtil.drawRoundedBorder(ctx.getMatrices(), x, y, PANEL_WIDTH, PANEL_HEIGHT, 0, 1.5f, ColorUtil.getColorStyle(30));
                int titleW = (int) FontUtils.sf_bold[18].getWidth(title);
                FontUtils.sf_bold[18].drawLeftAligned(ctx.getMatrices(), title, x + 8, y + TITLE_MARGIN_TOP + 1, titleColor);
                RenderUtil.rectRGB(ctx.getMatrices(), x + 8, y + TITLE_MARGIN_TOP + 17, titleW, 2, 1,
                        ColorUtil.getColorStyle(30), ColorUtil.getColorStyle(120), ColorUtil.getColorStyle(200), ColorUtil.getColorStyle(330));
            }
            case "Neon" -> {
                float s = guiShift();
                RenderUtil.rectRGB(ctx.getMatrices(), x, y, PANEL_WIDTH, PANEL_HEIGHT, 10,
                        ColorUtil.getColorStyle(200 + s, 110), ColorUtil.getColorStyle(120 + s, 110),
                        ColorUtil.getColorStyle(40 + s, 110), ColorUtil.getColorStyle(280 + s, 110));
                RenderUtil.drawRoundedBorder(ctx.getMatrices(), x, y, PANEL_WIDTH, PANEL_HEIGHT, 10, 1f, ColorUtil.applyAlpha(ColorUtil.getColorStyle(300 + s), 0.6f));
                int titleW = (int) FontUtils.sf_bold[20].getWidth(title);
                int titleX = x + (PANEL_WIDTH - titleW) / 2;
                FontUtils.sf_bold[20].renderGradientText(ctx.getMatrices(), title, titleX, y + TITLE_MARGIN_TOP + 2, ColorUtil.getColorStyle(200 + s), ColorUtil.getColorStyle(40 + s));
                FontUtils.icomoon[20].drawLeftAligned(ctx.getMatrices(), icon, titleX - 27, y + TITLE_MARGIN_TOP + 2, ColorUtil.getColorStyle(200 + s));
                float pulseW = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 200.0);
                int underlineX = x + (PANEL_WIDTH - Math.max(20, (int) ((PANEL_WIDTH - 20) * 0.7f))) / 2;
                RenderUtil.drawRoundedRect(ctx.getMatrices(), underlineX, y + TITLE_MARGIN_TOP + 17, Math.max(20, (int) ((PANEL_WIDTH - 20) * 0.7f * MathHelper.clamp(pulseW, 0f, 1f))), 2, 1, ColorUtil.getColorStyle(120 + s));
            }
            default -> {
                RenderUtil.drawRoundedRect(ctx.getMatrices(), x, y, PANEL_WIDTH, PANEL_HEIGHT, 12, GUI_COLOR.getRGB());
                RenderUtil.drawRoundedBorder(ctx.getMatrices(), x + 0.5f, y + 0.5f, PANEL_WIDTH - 1, PANEL_HEIGHT - 1, 12, 1f,
                        ColorUtil.applyAlpha(Color.WHITE.getRGB(), 0.06f));
                FontUtils.sf_bold[20].drawLeftAligned(ctx.getMatrices(), title, x + (PANEL_WIDTH - (int) FontUtils.sf_bold[20].getWidth(title)) / 2, y + TITLE_MARGIN_TOP + 2, titleColor);
                FontUtils.icomoon[20].drawLeftAligned(ctx.getMatrices(), icon, x + (PANEL_WIDTH - (int) FontUtils.sf_bold[20].getWidth(icon)) / 2 - 50, y + TITLE_MARGIN_TOP + 2, titleColor);
            }
        }

        {
            int maxBefore = calculateMaxScroll(category);
            float clampedTarget = MathHelper.clamp(scrollTargets.get(category), 0f, (float) maxBefore);
            float clampedOffset = MathHelper.clamp(scrollOffsets.get(category), 0f, (float) maxBefore);
            scrollTargets.put(category, clampedTarget);
            scrollOffsets.put(category, clampedOffset);
        }

        float offset = scrollOffsets.compute(category, (k, v) -> {
            float target = scrollTargets.get(k);
            float current = v;
            float lerped = MathUtil.lerp(current, target, SCROLL_LERP_FACTOR);
            float smoothed = MathUtil.lerp(current, lerped, SCROLL_SMOOTH_FACTOR);

            return smoothed;
        });

        renderScrollbar(ctx, x, y, category, offset);
        ctx.getMatrices().push();
        Scissor.push();
        Scissor.setFromComponentCoordinates(x, y + SCROLL_AREA_Y_OFFSET, PANEL_WIDTH, SCROLL_AREA_HEIGHT);

        float currentY = y + SCROLL_AREA_Y_OFFSET - offset;

        for (DeccalFunction f : DeccalManager.FUNCTION_MANAGER.getFunctions(category)) {
            if (!isFunctionVisible(f)) continue;

            int functionHeight = FUNCTION_HEIGHT;
            float prog = updateExpandAnimation(f);

            int fullSettingsHeight = computeSettingsHeight(f);
            int animatedSettingsHeight = (int) (fullSettingsHeight * prog);
            int totalHeight = functionHeight + animatedSettingsHeight;

            if (currentY + totalHeight < y + SCROLL_AREA_Y_OFFSET || currentY > y + PANEL_HEIGHT) {
                currentY += totalHeight;
                continue;
            }

            boolean rowHovered = mouseX >= x && mouseX <= x + PANEL_WIDTH && mouseY >= currentY && mouseY <= currentY + functionHeight;
            float hoverP = hoverProgress.getOrDefault(f, rowHovered ? 1f : 0f);
            hoverP = MathUtil.lerp(hoverP, rowHovered ? 1f : 0f, 15f);
            if (Math.abs((rowHovered ? 1f : 0f) - hoverP) < 0.001f) hoverP = rowHovered ? 1f : 0f;
            hoverProgress.put(f, hoverP);
            if (hoverP > 0.01f) {
                RenderUtil.drawRoundedRect(ctx.getMatrices(), x + 3, currentY + 1, PANEL_WIDTH - 6, FUNCTION_HEIGHT - 2, 6,
                        ColorUtil.applyAlpha(Color.WHITE.getRGB(), 0.10f * hoverP));
            }

            com.deccalware.deccal.modules.visual.DeccalClickGuiModule clickGUI = DeccalManager.FUNCTION_MANAGER.clickGUI;
            int col1 = f.isState() ? ColorUtil.getColorStyle(30) : new Color(198, 198, 198).getRGB();
            int col2 = f.isState() ? ColorUtil.getColorStyle(120) : col1;

            String guiStyle = guiStyle();
            if ("Minimal".equals(guiStyle)) {
                RenderUtil.drawCircle(ctx.getMatrices(), x + 13, currentY + FUNCTION_HEIGHT / 2f, 2.5f,
                        f.isState() ? col1 : new Color(70, 70, 70).getRGB());
            } else if ("Outline".equals(guiStyle)) {
                if (f.isState()) {
                    RenderUtil.drawRoundedRect(ctx.getMatrices(), x + 3, currentY + 3, 2, FUNCTION_HEIGHT - 6, 1, col1);
                }
            } else if ("Neon".equals(guiStyle) && f.isState()) {
                RenderUtil.rectRGB(ctx.getMatrices(), x + 3, currentY + 2, PANEL_WIDTH - 6, FUNCTION_HEIGHT - 4, 4,
                        ColorUtil.withAlpha(col1, 0.22f), ColorUtil.withAlpha(col1, 0.10f),
                        ColorUtil.withAlpha(col1, 0.10f), ColorUtil.withAlpha(col2, 0.22f));
            }

            int colorModule = f.isState() ? ColorUtil.getColorStyle(30, clickGUI.alphaModules.get().intValue()) : new Color(25, 25, 25, clickGUI.alphaModules.get().intValue()).getRGB();
            int colorModule2 = f.isState() ? ColorUtil.getColorStyle(120, clickGUI.alphaModules.get().intValue()) : colorModule;

            if (false) {
                RenderUtil.rectRGB(ctx.getMatrices(), x + 4, currentY - 1, PANEL_WIDTH - 8, totalHeight - 1, clickGUI.rounding.get().intValue(), colorModule2, colorModule2, colorModule2, colorModule2);
            }
            if (false) {
                RenderUtil.drawRoundedBorder(ctx.getMatrices(), x + 4, currentY - 1, PANEL_WIDTH - 8, totalHeight - 1, clickGUI.rounding.get().intValue(), 0f, colorModule2);
            }

            String textToRender;
            if (functionBinding && functions == f) {
                int bindCode = f.getBindCode();
                String keyName = bindCode != 0 ? ClientManager.getKey(bindCode) : "";
                textToRender = bindCode == 0 ? "Binding..." : "Binding... [" + keyName + "]";
            } else {
                textToRender = f.name;
            }

            if ("Minimal".equals(guiStyle)) {
                FontUtils.sf_medium[15].drawLeftAligned(ctx.getMatrices(), textToRender, x + 20, currentY + 3,
                        f.isState() ? col1 : new Color(190, 190, 190).getRGB());
            } else if ("Outline".equals(guiStyle)) {
                FontUtils.sf_medium[16].drawLeftAligned(ctx.getMatrices(), textToRender, x + 10, currentY + 3,
                        f.isState() ? Color.WHITE.getRGB() : new Color(160, 160, 160).getRGB());
            } else {
                FontUtils.sf_medium[16].renderGradientText(ctx.getMatrices(), textToRender, x + 10, currentY + 3, col1, col2);
            }

            if (animatedSettingsHeight > 0) {
                float settingY = currentY + functionHeight;
                ctx.getMatrices().push();
                Scissor.push();
                Scissor.setFromComponentCoordinates(x + 1, (int) settingY, PANEL_WIDTH - 2, animatedSettingsHeight);

                for (Setting setting : f.getSettings()) {
                    if (!setting.isVisible()) continue;
                    int settingHeight = 0;

                    if (setting instanceof BooleanSetting booleanSetting) {
                        settingHeight = booleanSettingRenderer.getHeight();
                        booleanSettingRenderer.render(ctx, booleanSetting, x + 10, (int) settingY, PANEL_WIDTH - 20, settingHeight);
                    } else if (setting instanceof BindBooleanSetting bindBooleanSetting) {
                        settingHeight = bindbooleanSettingRenderer.getHeight();
                        bindbooleanSettingRenderer.render(ctx, bindBooleanSetting, x + 10, (int) settingY, PANEL_WIDTH - 20, settingHeight);
                    } else if (setting instanceof BindSetting bindSetting) {
                        settingHeight = bindSettingRenderer.getHeight();
                        bindSettingRenderer.render(ctx, bindSetting, x + 10, (int) settingY - 2, PANEL_WIDTH - 20, settingHeight);
                    } else if (setting instanceof ModeSetting modeSetting) {
                        settingHeight = modeSettingRenderer.getHeight(modeSetting, PANEL_WIDTH - 20);
                        modeSettingRenderer.render(ctx, modeSetting, x + 10, (int) settingY, PANEL_WIDTH - 20, settingHeight);
                    } else if (setting instanceof MultiSetting multiSetting) {
                        settingHeight = multiSettingRenderer.getHeight(multiSetting, PANEL_WIDTH - 20);
                        multiSettingRenderer.render(ctx, multiSetting, x + 10, (int) settingY, PANEL_WIDTH - 20, settingHeight);
                    } else if (setting instanceof SliderSetting sliderSetting) {
                        settingHeight = sliderSettingRenderer.getHeight();
                        sliderSettingRenderer.render(ctx, sliderSetting, x + 10, (int) settingY - 2, PANEL_WIDTH - 20, settingHeight);
                    } else if (setting instanceof TextSetting textSetting) {
                        settingHeight = textSettingRenderer.getHeight();
                        textSettingRenderer.render(ctx, textSetting, x + 10, (int) settingY, PANEL_WIDTH - 20, settingHeight);
                    }

                    settingY += settingHeight + 1;
                }

                Scissor.pop();
                ctx.getMatrices().pop();
            }

            boolean hasSettings = !f.getSettings().isEmpty();
            if (hasSettings) {
                float currentProgress = arrowRotationProgress.getOrDefault(f, f.expanded ? 1f : 0f);
                currentProgress = MathUtil.lerp(currentProgress, f.expanded ? 1f : 0f, 15f);
                if (Math.abs((f.expanded ? 1f : 0f) - currentProgress) < 0.001f) currentProgress = f.expanded ? 1f : 0f;
                arrowRotationProgress.put(f, currentProgress);

                int arrowX = x + PANEL_WIDTH - 15;
                int arrowY = (int) (currentY + FUNCTION_HEIGHT / 2 - 2);

                ctx.getMatrices().push();
                ctx.getMatrices().translate(arrowX, arrowY, 0);
                float angleRad = (float) Math.toRadians(90.0f * currentProgress);
                Quaternionf rotation = new Quaternionf().fromAxisAngleRad(new Vector3f(0, 0, 1), angleRad);
                ctx.getMatrices().multiply(rotation);
                FontUtils.sf_medium[16].drawLeftAligned(ctx.getMatrices(), "→", -4, -FontUtils.sf_medium[16].getHeight() / 2, col1);
                ctx.getMatrices().pop();
            }

            currentY += totalHeight;
        }

        clampScrollForCategory(category);
        Scissor.pop();
        ctx.getMatrices().pop();
    }

    private void renderScrollbar(DrawContext ctx, int x, int y, Type category, float offset) {
        int maxScroll = calculateMaxScroll(category);
        if (maxScroll <= 0) return;

        int scrollbarWidth = 3;
        int scrollbarX = x + PANEL_WIDTH - scrollbarWidth - 1;

        int scrollbarHeight = SCROLL_AREA_HEIGHT - 30;
        int scrollbarY = y + SCROLL_AREA_Y_OFFSET + 15;

        int scrollbarBgColor = new Color(0, 0, 0, 50).getRGB();
        RenderUtil.drawRoundedRect(ctx.getMatrices(), scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight, 1, scrollbarBgColor);

        float scrollProgress = offset / maxScroll;
        int thumbHeight = Math.max(6, (int) (scrollbarHeight * (SCROLL_AREA_HEIGHT / (float) (SCROLL_AREA_HEIGHT + maxScroll))));
        int thumbY = scrollbarY + (int) (scrollProgress * (scrollbarHeight - thumbHeight));

        String style = guiStyle();
        if ("Minimal".equals(style)) {
            RenderUtil.drawRoundedRect(ctx.getMatrices(), scrollbarX, thumbY, scrollbarWidth, thumbHeight, 1, new Color(255, 255, 255, 70).getRGB());
        } else if ("Outline".equals(style)) {
            RenderUtil.drawRoundedBorder(ctx.getMatrices(), scrollbarX - 1, thumbY - 1, scrollbarWidth + 2, thumbHeight + 2, 1, 1f, ColorUtil.getColorStyle(30));
            RenderUtil.drawRoundedRect(ctx.getMatrices(), scrollbarX, thumbY, scrollbarWidth, thumbHeight, 1, new Color(20, 20, 20, 160).getRGB());
        } else if ("Neon".equals(style)) {
            float s = guiShift();
            RenderUtil.rectRGB(ctx.getMatrices(), scrollbarX, thumbY, scrollbarWidth, thumbHeight, 1,
                    ColorUtil.getColorStyle(120 + s), ColorUtil.getColorStyle(40 + s),
                    ColorUtil.getColorStyle(220 + s), ColorUtil.getColorStyle(320 + s));
        } else {
            int thumbColor = new Color(255, 255, 255, 150).getRGB();
            RenderUtil.drawRoundedRect(ctx.getMatrices(), scrollbarX, thumbY, scrollbarWidth, thumbHeight, 1, thumbColor);
        }
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int themeWidth = getThemeWidth();
        int themeX = getThemeX(themeWidth);
        int themeY = getThemeY();

        if (mouseX >= themeX && mouseX <= themeX + themeWidth && mouseY >= themeY && mouseY <= themeY + THEME_HEIGHT) {
            themeScrollTarget += (float) (-verticalAmount * THEME_SCROLL_SPEED);
            return true;
        }

        int totalWidth = renderCategories.size() * (PANEL_WIDTH + PANEL_MARGIN) - PANEL_MARGIN;
        int startX = (width - totalWidth) / 2;
        int startY = (height - PANEL_HEIGHT) / 2;

        int i = 0;
        for (Type category : renderCategories) {
            int px = startX + i++ * (PANEL_WIDTH + PANEL_MARGIN);
            if (mouseX >= px && mouseX <= px + PANEL_WIDTH && mouseY >= startY + SCROLL_AREA_Y_OFFSET && mouseY <= startY + SCROLL_AREA_Y_OFFSET + SCROLL_AREA_HEIGHT) {
                int maxScroll = calculateMaxScroll(category);
                if (maxScroll > 0) {
                    scrollTargets.compute(category, (k, v) -> Math.max(0, Math.min(v - (float) verticalAmount * SCROLL_SPEED, maxScroll)));
                    return true;
                }
                return false;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private int calculateMaxScroll(Type category) {
        int totalHeight = 0;
        for (DeccalFunction f : DeccalManager.FUNCTION_MANAGER.getFunctions(category)) {
            if (!isFunctionVisible(f)) continue;
            int functionHeight = FUNCTION_HEIGHT;

            int fullSettingsHeight = computeSettingsHeight(f);
            float prog = expandProgress.getOrDefault(f, f.expanded ? 1f : 0f);
            int animated = (int) Math.round(fullSettingsHeight * prog);

            totalHeight += functionHeight + animated;
        }
        int maxScroll = totalHeight - SCROLL_AREA_HEIGHT;
        return Math.max(0, maxScroll);
    }

    @Override
    public boolean charTyped(char c, int keyCode) {
        if (searchState.focused) {
            String prevText = searchState.text;
            if (searchState.text.length() < 30) {
                String before = searchState.text.substring(0, searchState.cursorPosition);
                String after = searchState.text.substring(searchState.cursorPosition);
                searchState.text = before + c + after;
                searchState.cursorPosition++;
            }
            if (!prevText.equals(searchState.text)) {
                resetScrollForAllCategories();
            }
            return true;
        }

        for (Type category : renderCategories) {
            for (DeccalFunction DeccalFunction : DeccalManager.FUNCTION_MANAGER.getFunctions(category)) {
                if (!DeccalFunction.expanded) continue;
                for (Setting setting : DeccalFunction.getSettings()) {
                    if (setting instanceof TextSetting textSetting && textSetting.isFocused()) {
                        if (textSettingRenderer.charTyped(textSetting, c, keyCode)) {
                            return true;
                        }
                    }
                }
            }
        }

        return super.charTyped(c, keyCode);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isClose) {
            return true;
        }
        for (Type category : renderCategories) {
            for (DeccalFunction DeccalFunction : DeccalManager.FUNCTION_MANAGER.getFunctions(category)) {
                if (!isFunctionVisible(DeccalFunction)) continue;
                if (DeccalFunction.expanded) {
                    for (Setting setting : DeccalFunction.getSettings()) {
                        if (!setting.isVisible()) continue;
                        if (setting instanceof BindSetting bindSetting) {
                            if (bindSetting.isBinding()) {
                                if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                                    bindSetting.setKey(-1);
                                } else {
                                    bindSetting.setKey(keyCode);
                                }
                                bindSetting.setBinding(false);
                                return true;
                            }
                        }
                        if (setting instanceof TextSetting textSetting) {
                            if (textSetting.isFocused()) {
                                if (textSettingRenderer.keyPressed(textSetting, keyCode, scanCode, modifiers)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (functionBinding && functions != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                functions.setBindCode(0);
            } else {
                functions.setBindCode(keyCode);
            }
            functionBinding = false;
            functions = null;
            return true;
        }

        if (searchState.focused) {
            String prevText = searchState.text;
            switch (keyCode) {
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (searchState.cursorPosition > 0 && !searchState.text.isEmpty()) {
                        searchState.text = searchState.text.substring(0, searchState.cursorPosition - 1) + searchState.text.substring(searchState.cursorPosition);
                        searchState.cursorPosition--;
                    }
                    if (!prevText.equals(searchState.text)) {
                        resetScrollForAllCategories();
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_DELETE -> {
                    if (searchState.cursorPosition < searchState.text.length()) {
                        searchState.text = searchState.text.substring(0, searchState.cursorPosition) + searchState.text.substring(searchState.cursorPosition + 1);
                    }
                    if (!prevText.equals(searchState.text)) {
                        resetScrollForAllCategories();
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_LEFT -> {
                    if (searchState.cursorPosition > 0) searchState.cursorPosition--;
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    if (searchState.cursorPosition < searchState.text.length()) searchState.cursorPosition++;
                    return true;
                }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_ESCAPE -> {
                    searchState.focused = false;
                    return true;
                }
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void resetScrollForAllCategories() {
        for (Type category : renderCategories) {
            scrollTargets.put(category, 0f);
            scrollOffsets.put(category, 0f);
        }
    }

    @Override
    public void tick() {
        super.tick();
        long currentTime = System.currentTimeMillis();
        if (currentTime - searchState.lastCursorBlink >= 500) {
            searchState.cursorVisible = !searchState.cursorVisible;
            searchState.lastCursorBlink = currentTime;
        }
    }

    @Override
    public void close() {
        if (isClose) {
            return;
        }
        draggingPicker1 = false;
        draggingPicker2 = false;
        isClose = true;
        animationOpen.setDirection(Direction.AxisDirection.NEGATIVE);
        animationOpen.reset();
    }

    private int getSearchWidth() {
        return SEARCH_MAX_WIDTH;
    }

    private int getSearchX(int searchWidth) {
        return (width - searchWidth) / 2;
    }

    private int getSearchY() {
        return (height + PANEL_HEIGHT) / 2 + SEARCH_MARGIN_BOTTOM;
    }

    private void renderSearchField(DrawContext ctx) {
        int searchWidth = getSearchWidth();
        int searchX = getSearchX(searchWidth);
        int searchY = getSearchY();
        com.deccalware.deccal.modules.visual.DeccalClickGuiModule clickGUI = DeccalManager.FUNCTION_MANAGER.clickGUI;
        if (clickGUI.blur.get() && clickGUI.blurSetting.get("Поиск")) {
            drawBlur(ctx.getMatrices(), searchX, searchY, searchWidth, SEARCH_HEIGHT, 6, 12, -1);
        }
        String style = guiStyle();
        if ("Minimal".equals(style)) {
            RenderUtil.drawRoundedRect(ctx.getMatrices(), searchX, searchY, searchWidth, SEARCH_HEIGHT, 4, new Color(17, 15, 28, 150).getRGB());
            RenderUtil.drawRoundedRect(ctx.getMatrices(), searchX, searchY + SEARCH_HEIGHT - 2, searchWidth, 1, 1, ColorUtil.getColorStyle(30));
        } else if ("Outline".equals(style)) {
            RenderUtil.drawRoundedRect(ctx.getMatrices(), searchX, searchY, searchWidth, SEARCH_HEIGHT, 0, new Color(12, 12, 12, 150).getRGB());
            RenderUtil.drawRoundedBorder(ctx.getMatrices(), searchX, searchY, searchWidth, SEARCH_HEIGHT, 0, 1.2f, ColorUtil.getColorStyle(30));
        } else if ("Neon".equals(style)) {
            float s = guiShift();
            RenderUtil.drawRoundedRect(ctx.getMatrices(), searchX, searchY, searchWidth, SEARCH_HEIGHT, 6, GUI_COLOR.getRGB());
            RenderUtil.drawRoundedRect(ctx.getMatrices(), searchX, searchY + SEARCH_HEIGHT - 2, searchWidth, 2, 1, ColorUtil.getColorStyle(120 + s));
        } else {
            RenderUtil.drawRoundedRect(ctx.getMatrices(), searchX, searchY, searchWidth, SEARCH_HEIGHT, 6, GUI_COLOR.getRGB());
        }

        String displayText;
        int textColor;
        int textX;

        if (searchState.text.isEmpty() && !searchState.focused) {
            displayText = "Search...";
            textColor = new Color(255, 255, 255, 120).getRGB();
            int textWidth = (int) FontUtils.sf_medium[18].getWidth(displayText);
            textX = searchX + (searchWidth - textWidth) / 2;
        } else {
            String text = searchState.text;
            if (searchState.focused && searchState.cursorVisible) {
                int pos = Math.min(searchState.cursorPosition, text.length());
                text = text.substring(0, pos) + "|" + text.substring(pos);
            }
            displayText = text;
            textColor = Color.WHITE.getRGB();
            textX = searchX + 6;
        }
        int textY = (int) (searchY + (SEARCH_HEIGHT - FontUtils.sf_medium[18].getHeight()) / 2);
        FontUtils.sf_medium[18].drawLeftAligned(ctx.getMatrices(), displayText, textX, textY, textColor);
    }

    private int getThemeWidth() {
        return THEME_MAX_WIDTH;
    }

    private int getThemeX(int searchWidth) {
        return (width - searchWidth) / 2;
    }

    private int getThemeY() {
        return (height + PANEL_HEIGHT) / 2 + THEME_MARGIN_BOTTOM;
    }
    private void renderButtomTheme(DrawContext ctx, double mouseX, double mouseY) {
        int searchWidth = getSearchWidth();
        int searchX = getSearchX(searchWidth);
        int searchY = getSearchY();

        int buttonX = searchX + 185;
        int buttonY = searchY + 2;
        int buttonWidth = 16;
        int buttonHeight = 16;
        boolean hovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
        int color = hovered ? GUI_COLOR.brighter().getRGB() : GUI_COLOR.getRGB();

        RenderUtil.drawRoundedRect(ctx.getMatrices(), buttonX, buttonY, buttonWidth, buttonHeight, 2, color);
        RenderUtil.drawTexture(ctx.getMatrices(), "images/gui/colors2.png", buttonX + 3, buttonY + 2.5f, 10, 10, 0, Color.white.getRGB());

        int chipSize = 10;
        String hoveredStyle = null;
        for (int i = 0; i < GUI_STYLES.length; i++) {
            int chipX = buttonX + buttonWidth + 9 + i * (chipSize + 5);
            int chipY = buttonY + 3;
            int chipColor = "Standard".equals(GUI_STYLES[i]) ? new Color(210, 210, 210).getRGB() : ColorUtil.getColorStyle(50 + i * 80);
            boolean active = guiStyle().equals(GUI_STYLES[i]);
            RenderUtil.drawRoundedRect(ctx.getMatrices(), chipX, chipY, chipSize, chipSize, 2, chipColor);
            if (active) {
                RenderUtil.drawRoundedBorder(ctx.getMatrices(), chipX - 1, chipY - 1, chipSize + 2, chipSize + 2, 3, 1f, Color.WHITE.getRGB());
            }
            if (mouseX >= chipX && mouseX <= chipX + chipSize && mouseY >= chipY && mouseY <= chipY + chipSize) {
                hoveredStyle = GUI_STYLES[i];
            }
        }

        if (hoveredStyle != null) {
            int tipWidth = (int) FontUtils.sf_medium[14].getWidth("GUI: " + hoveredStyle);
            FontUtils.sf_medium[14].drawLeftAligned(ctx.getMatrices(), "GUI: " + hoveredStyle, searchX + searchWidth / 2 - tipWidth / 2, searchY - 12, Color.WHITE.getRGB());
        }
    }

    private void renderTheme(DrawContext ctx, int mouseX, int mouseY) {
        float targetAlpha = themeMenuTarget > 0.01f ? 1f : 0f;
        themeAlphaAnim += (targetAlpha - themeAlphaAnim) * 0.15f;
        if (themeAlphaAnim < 0.01f) return;

        themeMenuAnim += (themeMenuTarget - themeMenuAnim) * THEME_ANIM_SPEED;
        if (themeMenuAnim < 0.01f) return;

        int themeWidth = getThemeWidth();
        int themeX = getThemeX(themeWidth);
        int themeY = getThemeY();
        float offsetY = (1f - themeMenuAnim) * 10f;
        themeScrollOffset = MathUtil.lerp(themeScrollOffset, themeScrollTarget, THEME_SCROLL_LERP_FACTOR);

        int panelColor = ColorUtil.applyAlpha(GUI_COLOR.getRGB(), themeAlphaAnim);

        if (DeccalManager.FUNCTION_MANAGER.clickGUI.blur.get() && DeccalManager.FUNCTION_MANAGER.clickGUI.blurSetting.get("Темы")) {
            drawBlur(ctx.getMatrices(), themeX, themeY + offsetY, themeWidth, THEME_HEIGHT, 3, 12, -1);
        }
        RenderUtil.drawRoundedRect(ctx.getMatrices(), themeX, themeY + offsetY, themeWidth, THEME_HEIGHT, 3, panelColor);

        int circleSize = THEME_HEIGHT - 5;
        int padding = 5;
        int totalThemes = DeccalManager.STYLE_MANAGER.getStyles().size() + 1;

        float maxScroll = Math.max(0, (totalThemes - VISIBLE_THEMES) * (circleSize + padding));
        themeScrollTarget = MathHelper.clamp(themeScrollTarget, 0, maxScroll);
        themeScrollOffset = MathHelper.clamp(themeScrollOffset, 0, maxScroll);

        if (totalThemes > VISIBLE_THEMES) {
            int arrowColor = ColorUtil.applyAlpha(Color.WHITE.getRGB(), themeAlphaAnim * 0.6f);

            if (themeScrollTarget > 0) {
                int arrowY = themeY + THEME_HEIGHT / 2;
                FontUtils.sf_medium[16].drawLeftAligned(ctx.getMatrices(), "←", themeX - 10, arrowY - 5, arrowColor);
            }
            if (themeScrollTarget < maxScroll) {
                int arrowY = themeY + THEME_HEIGHT / 2;
                int arrowX = themeX + themeWidth + 4;
                FontUtils.sf_medium[16].drawLeftAligned(ctx.getMatrices(), "→", arrowX, arrowY - 5, arrowColor);
            }
        }

        ctx.getMatrices().push();
        Scissor.push();
        Scissor.setFromComponentCoordinates(themeX + 1, themeY + offsetY, themeWidth - 2, THEME_HEIGHT);

        float startX = themeX + padding - themeScrollOffset;
        int centerY = (int) (themeY + (THEME_HEIGHT - circleSize) / 2 + 0.9f + offsetY);
        String hoveredTheme = null;
        int x = (int) startX;
        int y = centerY;

        int pickerSize = circleSize;

        if (selectedColor1 != -1 && selectedColor2 != -1) {
            final Vector4i vec = new Vector4i(
                    ColorUtil.gradient(5, 0, selectedColor1, selectedColor2),
                    ColorUtil.gradient(5, 180, selectedColor1, selectedColor2),
                    ColorUtil.gradient(5, 90, selectedColor1, selectedColor2),
                    ColorUtil.gradient(5, 360, selectedColor1, selectedColor2)
            );
            RenderUtil.rectRGB(ctx.getMatrices(), x, y + 0.5f, circleSize, circleSize, 5,
                    ColorUtil.applyAlpha(vec.w, themeAlphaAnim),
                    ColorUtil.applyAlpha(vec.x, themeAlphaAnim),
                    ColorUtil.applyAlpha(vec.y, themeAlphaAnim),
                    ColorUtil.applyAlpha(vec.z, themeAlphaAnim)
            );
        } else {
            RenderUtil.drawTexture(ctx.getMatrices(), "images/gui/pips.png", x, y + 0.5f, pickerSize, pickerSize, 5,
                    ColorUtil.applyAlpha(Color.WHITE.getRGB(), themeAlphaAnim));
        }

        if (RenderUtil.isHovered(mouseX, mouseY, x, y, circleSize, circleSize)) {
            RenderUtil.drawRoundedBorder(ctx.getMatrices(), x - 1, y - 0.5f, circleSize + 2, circleSize + 2, 5, 0.1f,
                    ColorUtil.applyAlpha(Color.WHITE.getRGB(), themeAlphaAnim));
            hoveredTheme = "LMB - Create Custom Theme";
        }
        startX += circleSize + padding;

        for (Style style : DeccalManager.STYLE_MANAGER.getStyles()) {
            int[] colors = style.colors;
            int c1 = colors[0];
            int c2 = colors.length > 1 ? colors[1] : colors[0];
            final Vector4i vec = new Vector4i(
                    ColorUtil.gradient(5, 0, c1, c2),
                    ColorUtil.gradient(5, 180, c1, c2),
                    ColorUtil.gradient(5, 90, c1, c2),
                    ColorUtil.gradient(5, 360, c1, c2)
            );
            x = (int) startX;
            y = centerY;

            RenderUtil.rectRGB(ctx.getMatrices(), x, y + 0.5f, circleSize, circleSize, 5,
                    ColorUtil.applyAlpha(vec.w, themeAlphaAnim),
                    ColorUtil.applyAlpha(vec.x, themeAlphaAnim),
                    ColorUtil.applyAlpha(vec.y, themeAlphaAnim),
                    ColorUtil.applyAlpha(vec.z, themeAlphaAnim)
            );

            if (RenderUtil.isHovered(mouseX, mouseY, x, y, circleSize, circleSize)) {
                hoveredTheme = style.name;
            }
            startX += circleSize + padding;
        }

        Scissor.pop();
        ctx.getMatrices().pop();

        float animSpeed = 0.2f;
        float targetAnim = colorPickerOpen ? 1f : 0f;
        colorPickerAnim += (targetAnim - colorPickerAnim) * animSpeed;

        if (colorPickerAnim > 0.01f) {
            int fixedX = themeX + padding;
            int fixedY = centerY;
            renderColorPickers(ctx, fixedX, fixedY, mouseX, mouseY);
        }

        if (hoveredTheme != null) {
            themeNameAnim += (1f - themeNameAnim) * 0.2f;
        } else {
            themeNameAnim += (0f - themeNameAnim) * 0.2f;
        }

        if (themeNameAnim > 0.01f && hoveredTheme != null) {
            int screenWidth = ctx.getScaledWindowWidth();
            int textWidth = (int) FontUtils.sf_medium[18].getWidth(hoveredTheme);
            int textX = (screenWidth - textWidth) / 2;

            int textColor = ColorUtil.applyAlpha(Color.WHITE.getRGB(), themeNameAnim * themeAlphaAnim);
            FontUtils.sf_medium[18].drawLeftAligned(ctx.getMatrices(), hoveredTheme, textX, centerY + 18, textColor);

            if (hoveredTheme.toLowerCase().contains("custom")) {
                int tipColor = ColorUtil.applyAlpha(Color.WHITE.getRGB(), themeNameAnim * themeAlphaAnim * 0.7f);
                String tipText = "RMB - Delete";
                int tipWidth = (int) FontUtils.sf_medium[14].getWidth(tipText);
                int tipX = (screenWidth - tipWidth) / 2;
                FontUtils.sf_medium[14].drawLeftAligned(ctx.getMatrices(), tipText, tipX, centerY + 32, tipColor);
            }
        }
    }

    private void renderColorPickers(DrawContext ctx, int x, int y, int mouseX, int mouseY) {
        int panelWidth = 85;
        int panelHeight = 51;
        float animOffsetX = (1f - colorPickerAnim) * 30f;
        float animScale = 0.95f + 0.05f * colorPickerAnim;
        float alphaMult = colorPickerAnim;

        int panelX = (int) (x - panelWidth - 20 + animOffsetX);
        int panelY = y - panelHeight / 2 - 12;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(panelX + panelWidth / 2f, panelY + panelHeight / 2f, 0);
        ctx.getMatrices().scale(animScale, animScale, 1f);
        ctx.getMatrices().translate(-panelWidth / 2f, -panelHeight / 2f, 0);

        int baseColor = GUI_COLOR.getRGB();
        if (DeccalManager.FUNCTION_MANAGER.clickGUI.blur.get() && DeccalManager.FUNCTION_MANAGER.clickGUI.blurSetting.get("Создание темы")) {
            RenderUtil.drawBlur(ctx.getMatrices(), 0, 0, panelWidth, panelHeight, 4,12,-1);
        }
        RenderUtil.drawRoundedRect(ctx.getMatrices(), 0, 0, panelWidth, panelHeight, 4, ColorUtil.applyAlpha(baseColor, alphaMult));

        int picker1Size = 30;
        int picker1X = 5;
        int picker1Y = 5;
        RenderUtil.drawTexture(ctx.getMatrices(), "images/gui/pick.png", picker1X, picker1Y, picker1Size, picker1Size, 14, ColorUtil.applyAlpha(Color.WHITE.getRGB(), alphaMult));
        RenderUtil.drawRoundedBorder(ctx.getMatrices(), picker1X, picker1Y, picker1Size, picker1Size, 14, 0.1f, ColorUtil.applyAlpha(Color.WHITE.getRGB(), alphaMult));

        int dotX1 = (int) (picker1X + picker1CursorX * picker1Size);
        int dotY1 = (int) (picker1Y + picker1CursorY * picker1Size);
        RenderUtil.drawCircle(ctx.getMatrices(), dotX1, dotY1, 4f, ColorUtil.applyAlpha(Color.BLACK.getRGB(), alphaMult));

        int picker2Size = 30;
        int picker2X = 50;
        int picker2Y = 5;
        RenderUtil.drawTexture(ctx.getMatrices(), "images/gui/pick.png", picker2X, picker2Y, picker2Size, picker2Size, 14, ColorUtil.applyAlpha(Color.WHITE.getRGB(), alphaMult));
        RenderUtil.drawRoundedBorder(ctx.getMatrices(), picker2X, picker2Y, picker2Size, picker2Size, 14, 0.1f, ColorUtil.applyAlpha(Color.WHITE.getRGB(), alphaMult));

        int dotX2 = (int) (picker2X + picker2CursorX * picker2Size);
        int dotY2 = (int) (picker2Y + picker2CursorY * picker2Size);
        RenderUtil.drawCircle(ctx.getMatrices(), dotX2, dotY2, 4f, ColorUtil.applyAlpha(Color.BLACK.getRGB(), alphaMult));

        int closeButtonSize = 10;
        int closeButtonX = panelWidth - closeButtonSize;
        int closeButtonY = 0;
        RenderUtil.drawRoundedRect(ctx.getMatrices(), closeButtonX, closeButtonY, closeButtonSize, closeButtonSize, new Vector4f(0, 4, 0, 4), ColorUtil.applyAlpha(Color.WHITE.getRGB(), alphaMult));
        FontUtils.sf_medium[20].drawLeftAligned(ctx.getMatrices(), "×", closeButtonX + 2, closeButtonY - 1.5f, ColorUtil.applyAlpha(Color.RED.getRGB(), alphaMult));

        RenderUtil.drawRoundedRect(ctx.getMatrices(), 14, 39, 56, 8, new Vector4f(1, 1, 1, 1), ColorUtil.applyAlpha(Color.WHITE.getRGB(), alphaMult));
        FontUtils.durman[12].drawLeftAligned(ctx.getMatrices(), "Add Theme", 18, 39, ColorUtil.applyAlpha(Color.BLACK.getRGB(), alphaMult));

        ctx.getMatrices().pop();
    }


    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && colorPickerOpen) {
            int panelWidth = 85;
            int panelHeight = 51;
            int panelX = getThemeX(getThemeWidth()) - panelWidth - 20;
            int panelY = getThemeY() - panelHeight / 2 - 12;

            int picker1Size = 30;
            int picker1X = panelX + 5;
            int picker1Y = panelY + 5;

            int picker2Size = 30;
            int picker2X = panelX + 50;
            int picker2Y = panelY + 5;
            if (draggingPicker1) {
                float nx = (float) (mouseX - picker1X) / picker1Size;
                float ny = (float) (mouseY - picker1Y) / picker1Size;
                picker1CursorX = MathHelper.clamp(nx, 0f, 1f);
                picker1CursorY = MathHelper.clamp(ny, 0f, 1f);
                selectedColor1 = ColorUtil.getPixelColor(ResourceProvider.color_image, picker1CursorX, picker1CursorY);
                return true;
            }

            if (draggingPicker2) {
                float nx = (float) (mouseX - picker2X) / picker2Size;
                float ny = (float) (mouseY - picker2Y) / picker2Size;
                picker2CursorX = MathHelper.clamp(nx, 0f, 1f);
                picker2CursorY = MathHelper.clamp(ny, 0f, 1f);
                selectedColor2 = ColorUtil.getPixelColor(ResourceProvider.color_image, picker2CursorX, picker2CursorY);
                return true;
            }
        }

        if (draggingSlider != null && button == 0) {
            sliderSettingRenderer.mouseDragged(draggingSlider, mouseX, draggingSliderX, draggingSliderWidth);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingPicker1 = false;
        draggingPicker2 = false;
        if (draggingSlider != null) {
            sliderSettingRenderer.mouseReleased(draggingSlider);
            draggingSlider = null;
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }



    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int totalWidth = renderCategories.size() * (PANEL_WIDTH + PANEL_MARGIN) - PANEL_MARGIN;
        int startX = (width - totalWidth) / 2;
        int startY = (height - PANEL_HEIGHT) / 2;
        int searchWidth = getSearchWidth();
        int searchX = getSearchX(searchWidth);
        int searchY = getSearchY();

        if (colorPickerOpen) {
            int themeWidth = getThemeWidth();
            int themeX = getThemeX(themeWidth);
            int themeY = getThemeY();
            int circleSize = THEME_HEIGHT - 5;
            int padding = 5;

            float offsetY = (1f - themeMenuAnim) * 10f;
            int centerY = (int) (themeY + (THEME_HEIGHT - circleSize) / 2 + 0.9f + offsetY);

            int fixedX = themeX + padding;
            int fixedY = centerY;

            int panelWidth = 85;
            int panelHeight = 51;
            int panelX = fixedX - panelWidth - 20;
            int panelY = fixedY - panelHeight / 2 - 12;

            int picker1Size = 30;
            int picker1X = panelX + 5;
            int picker1Y = panelY + 5;

            int picker2Size = 30;
            int picker2X = panelX + 50;
            int picker2Y = panelY + 5;

            int closeButtonSize = 10;
            int closeButtonX = panelX + panelWidth - closeButtonSize;
            int closeButtonY = panelY;
            if (mouseX >= closeButtonX && mouseX <= closeButtonX + closeButtonSize && mouseY >= closeButtonY && mouseY <= closeButtonY + closeButtonSize) {
                colorPickerOpen = false;
                return true;
            }

            if (mouseX >= picker1X && mouseX <= picker1X + picker1Size && mouseY >= picker1Y && mouseY <= picker1Y + picker1Size) {
                float nx = (float) (mouseX - picker1X) / picker1Size;
                float ny = (float) (mouseY - picker1Y) / picker1Size;
                draggingPicker1 = true;
                picker1CursorX = Math.max(0, Math.min(1, nx));
                picker1CursorY = Math.max(0, Math.min(1, ny));
                selectedColor1 = ColorUtil.getPixelColor(ResourceProvider.color_image, picker1CursorX, picker1CursorY);
                return true;
            }
            if (mouseX >= picker2X && mouseX <= picker2X + picker2Size && mouseY >= picker2Y && mouseY <= picker2Y + picker2Size) {
                float nx = (float) (mouseX - picker2X) / picker2Size;
                float ny = (float) (mouseY - picker2Y) / picker2Size;
                draggingPicker2 = true;
                picker2CursorX = Math.max(0, Math.min(1, nx));
                picker2CursorY = Math.max(0, Math.min(1, ny));
                selectedColor2 = ColorUtil.getPixelColor(ResourceProvider.color_image, picker2CursorX, picker2CursorY);
                return true;
            }

            int addButtonX = panelX + 14;
            int addButtonY = panelY + 39;
            int addButtonWidth = 56;
            int addButtonHeight = 8;

            if (mouseX >= addButtonX && mouseX <= addButtonX + addButtonWidth && mouseY >= addButtonY && mouseY <= addButtonY + addButtonHeight) {
                String baseName = "Custom";
                String candidate = baseName;
                int index = 2;

                boolean exists;
                do {
                    exists = false;
                    for (Style s : DeccalManager.STYLE_MANAGER.getStyles()) {
                        if (s.name.equals(candidate)) {
                            exists = true;
                            candidate = baseName + "-" + index++;
                            break;
                        }
                    }
                } while (exists);

                int[] colors = {selectedColor1, selectedColor2};
                Style newStyle = new Style(candidate, colors);
                DeccalManager.STYLE_MANAGER.addCustomTheme(candidate, selectedColor1, selectedColor2);
                DeccalManager.STYLE_MANAGER.setTheme(newStyle);
                colorPickerOpen = false;
                return true;
            }
        }

        if (mouseX >= searchX && mouseX <= searchX + searchWidth && mouseY >= searchY && mouseY <= searchY + SEARCH_HEIGHT) {
            searchState.focused = true;
            searchState.cursorPosition = searchState.text.length();
            return true;
        } else {
            searchState.focused = false;
        }
        int buttonX = searchX + 185;
        int buttonY = searchY + 2;
        int buttonWidth = 16;
        int buttonHeight = 16;
        if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
            themeMenu = !themeMenu;
            themeMenuTarget = themeMenu ? 1f : 0f;
        }

        int chipSize = 10;
        for (int i = 0; i < GUI_STYLES.length; i++) {
            int chipX = buttonX + buttonWidth + 9 + i * (chipSize + 5);
            if (mouseX >= chipX && mouseX <= chipX + chipSize && mouseY >= buttonY + 3 && mouseY <= buttonY + 3 + chipSize) {
                DeccalManager.FUNCTION_MANAGER.clickGUI.guiStyle.set(GUI_STYLES[i]);
                return true;
            }
        }

        if (themeMenu) {
            int themeWidth = getThemeWidth();
            int themeX = getThemeX(themeWidth);
            int themeY = getThemeY();
            int circleSize = THEME_HEIGHT - 5;
            int padding = 5;
            int totalThemes = DeccalManager.STYLE_MANAGER.getStyles().size() + 1;
            float maxScroll = Math.max(0, (totalThemes - VISIBLE_THEMES) * (circleSize + padding));
            if (themeScrollTarget > 0 && mouseX >= themeX - 15 && mouseX <= themeX - 5 && mouseY >= themeY && mouseY <= themeY + THEME_HEIGHT) {
                themeScrollTarget -= (circleSize + padding) * 3;
                return true;
            }
            if (themeScrollTarget < maxScroll && mouseX >= themeX + themeWidth + 5 && mouseX <= themeX + themeWidth + 15 && mouseY >= themeY && mouseY <= themeY + THEME_HEIGHT) {
                themeScrollTarget += (circleSize + padding) * 3;
                return true;
            }

            float currentX = themeX + padding - themeScrollTarget;
            int centerY = themeY + (THEME_HEIGHT - circleSize) / 2;
            int visibleAreaEndX = themeX + themeWidth;


            if (currentX + circleSize >= themeX && currentX <= visibleAreaEndX) {
                if (mouseX >= currentX && mouseX <= currentX + circleSize && mouseY >= centerY && mouseY <= centerY + circleSize) {
                    colorPickerOpen = !colorPickerOpen;
                    return true;
                }
            }
            currentX += circleSize + padding;

            if (button == 0) {
                for (Style style : DeccalManager.STYLE_MANAGER.getStyles()) {
                    if (currentX + circleSize < themeX || currentX > visibleAreaEndX) {
                        currentX += circleSize + padding;
                        continue;
                    }
                    if (mouseX >= currentX && mouseX <= currentX + circleSize && mouseY >= centerY && mouseY <= centerY + circleSize) {
                        DeccalManager.STYLE_MANAGER.setTheme(style);
                        return true;
                    }
                    currentX += circleSize + padding;
                }
            }

            if (button == 1) {
                currentX = themeX + padding + circleSize + padding - themeScrollTarget;
                for (Style style : new ArrayList<>(DeccalManager.STYLE_MANAGER.getStyles())) {
                    if (currentX + circleSize < themeX || currentX > visibleAreaEndX) {
                        currentX += circleSize + padding;
                        continue;
                    }

                    if (mouseX >= currentX && mouseX <= currentX + circleSize && mouseY >= centerY && mouseY <= centerY + circleSize) {
                        if (style.name.startsWith("Custom")) {
                            DeccalManager.STYLE_MANAGER.removeStyle(style);
                            return true;
                        } else {
                            return true;
                        }
                    }
                    currentX += circleSize + padding;
                }
            }
        }

        if (functionBinding && functions != null) {
            int code = -(button + 2);
            functions.setBindCode(code);
            functionBinding = false;
            functions = null;
            return true;
        }

        for (Type category : renderCategories) {
            for (DeccalFunction DeccalFunction : DeccalManager.FUNCTION_MANAGER.getFunctions(category)) {
                if (!DeccalFunction.expanded) continue;
                for (Setting setting : DeccalFunction.getSettings()) {
                    if (!setting.isVisible()) continue;
                    if (setting instanceof BindSetting bindSetting) {
                        if (bindSetting.isBinding()) {
                            int code = -(button + 2);
                            bindSetting.setKey(code);
                            bindSetting.setBinding(false);
                            return true;
                        }
                    }
                }
            }
        }

        int idx = 0;
        for (Type category : renderCategories) {
            int panelX = startX + idx++ * (PANEL_WIDTH + PANEL_MARGIN);
            float offset = scrollOffsets.get(category);

            float currentY = startY + SCROLL_AREA_Y_OFFSET - offset;

            for (DeccalFunction DeccalFunction : DeccalManager.FUNCTION_MANAGER.getFunctions(category)) {
                if (!isFunctionVisible(DeccalFunction)) continue;

                int functionHeight = FUNCTION_HEIGHT;

                int fullSettingsHeight = computeSettingsHeight(DeccalFunction);
                float prog = expandProgress.getOrDefault(DeccalFunction, DeccalFunction.expanded ? 1f : 0f);
                int animatedSettingsHeight = (int) Math.round(fullSettingsHeight * prog);

                int totalHeight = functionHeight + animatedSettingsHeight;

                if (currentY + totalHeight < startY + SCROLL_AREA_Y_OFFSET) {
                    currentY += totalHeight;
                    continue;
                }
                if (currentY > startY + SCROLL_AREA_Y_OFFSET + SCROLL_AREA_HEIGHT) {
                    break;
                }

                if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH && mouseY >= currentY && mouseY <= currentY + functionHeight && mouseY >= startY + SCROLL_AREA_Y_OFFSET && mouseY <= startY + SCROLL_AREA_Y_OFFSET + SCROLL_AREA_HEIGHT) {
                    if (button == 0) {
                        DeccalFunction.toggle();
                        return true;
                    } else if (button == 1) {
                        DeccalFunction.expanded = !DeccalFunction.expanded;
                        clampScrollForCategory(category);
                        return true;
                    } else if (button == 2) {
                        functionBinding = true;
                        functions = DeccalFunction;
                        return true;
                    }
                }

                if (animatedSettingsHeight > 0) {
                    float settingY = currentY + functionHeight;
                    int remaining = animatedSettingsHeight;

                    for (Setting setting : DeccalFunction.getSettings()) {
                        if (!setting.isVisible()) continue;

                        int settingHeight = getSettingRendererHeight(setting, PANEL_WIDTH - 20);
                        if (settingHeight <= 0) continue;

                        int visible = Math.max(0, Math.min(settingHeight, remaining));
                        if (visible <= 0) break;

                        if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH && mouseY >= settingY && mouseY <= settingY + visible && mouseY >= startY + SCROLL_AREA_Y_OFFSET && mouseY <= startY + SCROLL_AREA_Y_OFFSET + SCROLL_AREA_HEIGHT) {

                            int settingX = panelX + 10;
                            int settingWidth = PANEL_WIDTH - 20;

                            if (setting instanceof BooleanSetting booleanSetting) {
                                if (booleanSettingRenderer.mouseClicked(booleanSetting, mouseX, mouseY, button, settingX, (int) settingY, settingWidth, visible)) {
                                    return true;
                                }
                            } else if (setting instanceof BindBooleanSetting bindBooleanSetting) {
                                if (bindbooleanSettingRenderer.mouseClicked(bindBooleanSetting, mouseX, mouseY, button, settingX, (int) settingY, settingWidth, visible)) {
                                    return true;
                                }
                            } else if (setting instanceof BindSetting bindSetting) {
                                if (bindSettingRenderer.mouseClicked(bindSetting, mouseX, mouseY, button, settingX, (int) settingY  - 2, settingWidth, visible)) {
                                    return true;
                                }
                            } else if (setting instanceof ModeSetting modeSetting) {
                                if (modeSettingRenderer.mouseClicked(modeSetting, mouseX, mouseY, button, settingX, (int) settingY, settingWidth, visible)) {
                                    return true;
                                }
                            } else if (setting instanceof MultiSetting multiSetting) {
                                if (multiSettingRenderer.mouseClicked(multiSetting, mouseX, mouseY, button, settingX, (int) settingY, settingWidth, visible)) {
                                    return true;
                                }
                            } else if (setting instanceof SliderSetting sliderSetting) {
                                if (sliderSettingRenderer.mouseClicked(sliderSetting, mouseX, mouseY, button, settingX, (int) settingY - 2, settingWidth, visible)) {
                                    draggingSlider = sliderSetting;
                                    draggingSliderX = settingX;
                                    draggingSliderWidth = settingWidth;
                                    return true;
                                }
                            } else if (setting instanceof TextSetting textSetting) {
                                if (textSettingRenderer.mouseClicked(textSetting, mouseX, mouseY, button, settingX, (int) settingY, settingWidth, visible)) {
                                    return true;
                                }
                            }
                        }

                        settingY += settingHeight + 1;
                        remaining -= (settingHeight + 1);
                        if (remaining <= 0) break;
                    }
                }

                currentY += totalHeight;
            }
        }

        for (Type category : renderCategories) {
            for (DeccalFunction DeccalFunction : DeccalManager.FUNCTION_MANAGER.getFunctions(category)) {
                if (!DeccalFunction.expanded) continue;
                for (Setting setting : DeccalFunction.getSettings()) {
                    if (setting instanceof TextSetting textSetting) {
                        textSetting.setFocused(false);
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isFunctionVisible(DeccalFunction DeccalFunction) {
        String searchTextLower = searchState.text.toLowerCase();
        return searchTextLower.isEmpty() || DeccalFunction.name.toLowerCase().contains(searchTextLower) || DeccalFunction.keywords.toLowerCase().contains(searchTextLower);
    }

    @Override
    public void renderBackground(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        int dim = (int) (MathHelper.clamp((float) animation, 0f, 1f) * 0.60f * 255f);
        drawContext.fill(0, 0, drawContext.getScaledWindowWidth(), drawContext.getScaledWindowHeight(),
                new Color(0, 0, 0, dim).getRGB());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private int computeSettingsHeight(DeccalFunction f) {
        int settingsHeight = 0;
        for (Setting setting : f.getSettings()) {
            if (!setting.isVisible()) continue;
            if (setting instanceof BooleanSetting) {
                settingsHeight += booleanSettingRenderer.getHeight() + 1;
            } else if (setting instanceof BindBooleanSetting) {
                settingsHeight += bindbooleanSettingRenderer.getHeight() + 1;
            } else if (setting instanceof BindSetting) {
                settingsHeight += bindSettingRenderer.getHeight() + 1;
            } else if (setting instanceof ModeSetting) {
                settingsHeight += modeSettingRenderer.getHeight((ModeSetting) setting, PANEL_WIDTH - 20) + 2;
            } else if (setting instanceof MultiSetting) {
                settingsHeight += multiSettingRenderer.getHeight((MultiSetting) setting, PANEL_WIDTH - 20) + 2;
            } else if (setting instanceof SliderSetting) {
                settingsHeight += sliderSettingRenderer.getHeight() + 1;
            } else if (setting instanceof TextSetting) {
                settingsHeight += textSettingRenderer.getHeight() + 1;
            }
        }
        return Math.max(0, settingsHeight);
    }

    private int getSettingRendererHeight(Setting setting, int width) {
        if (!setting.isVisible()) return 0;
        if (setting instanceof BooleanSetting) {
            return booleanSettingRenderer.getHeight();
        } else if (setting instanceof BindBooleanSetting) {
            return bindbooleanSettingRenderer.getHeight();
        } else if (setting instanceof BindSetting) {
            return bindSettingRenderer.getHeight();
        } else if (setting instanceof ModeSetting modeSetting) {
            return modeSettingRenderer.getHeight(modeSetting, width);
        } else if (setting instanceof MultiSetting multiSetting) {
            return multiSettingRenderer.getHeight(multiSetting, width);
        } else if (setting instanceof SliderSetting) {
            return sliderSettingRenderer.getHeight();
        } else if (setting instanceof TextSetting) {
            return textSettingRenderer.getHeight();
        }
        return 0;
    }

    private void clampScrollForCategory(Type category) {
        int maxScroll = calculateMaxScroll(category);
        float clampedTarget = MathHelper.clamp(scrollTargets.get(category), 0f, (float) maxScroll);
        float clampedOffset = MathHelper.clamp(scrollOffsets.get(category), 0f, (float) maxScroll);
        scrollTargets.put(category, clampedTarget);
        scrollOffsets.put(category, clampedOffset);
    }
}
