package cc.ecisr.jyutdict.widget;

import android.graphics.Color;
import androidx.annotation.ColorInt;

import cc.ecisr.jyutdict.utils.ColorUtil;

/**
 * 地點名稱描邊工具類。
 *
 * 當地點顏色在白天模式下過淺（接近白色背景）或在夜間模式下過深（接近深色背景）時，
 * 為文字添加外輪廓描邊以保證足夠的對比度與辨識度。
 *
 * 預設行為：根據地點文字原色動態生成同色系描邊色：
 * - 白天模式：將原色調成更暗、更深的版本（提高淺色文字在亮背景上的對比度）
 * - 夜間模式：將原色調成更淡、更亮、更柔和的版本（提高深色文字在暗背景上的對比度）
 *
 * ==================== 【參數調配指南】 ====================
 * 您可以在下方「描邊調配參數」區塊直接修改各項常數：
 *
 * 1. ALWAYS_APPLY_STROKE：是否對所有地點名稱強制啟用描邊（true: 全部描邊方便測試; false: 僅對過淺/過深顏色描邊）。
 * 2. LIGHT_LUMINANCE_THRESHOLD：白天模式觸發描邊的明度下限 (0.0 ~ 1.0)，顏色明度 >= 此值時觸發深色描邊。
 * 3. DARK_LUMINANCE_THRESHOLD：夜間模式觸發描邊的明度上限 (0.0 ~ 1.0)，顏色明度 <= 此值時觸發淺色描邊。
 * 4. STROKE_MODE：
 *      - MODE_STROKE (1): 銳利外輪廓描邊（預設）
 *      - MODE_GLOW   (2): 柔和外發光/光暈（ARGB 的 A 值能非常細膩地控制光暈強弱）
 *      - MODE_BOTH   (3): 描邊 + 外發光同時啟用
 * 5. STROKE_WIDTH_RATIO：描邊寬度係數（相對於字體大小 textSize 的比例）。
 * 6. MIN_STROKE_WIDTH_PX：最小描邊像素寬度。
 * 7. GLOW_RADIUS_PX：外發光模糊半徑 (px)。
 * 8. USE_DYNAMIC_COLOR_STROKE：是否啟用同色系動態調色（true: 根據文字原色自動變暗/變淡; false: 使用固定的黑白描邊色）。
 * 9. 白天模式動態調色參數：
 *      - LIGHT_MODE_DARKEN_RATIO：明度衰減係數 (0.0 ~ 1.0，數值越小描邊越深/越暗)。
 *      - LIGHT_MODE_SATURATION_RATIO：飽和度調整係數 (通常略 >= 1.0 保持色調濃郁)。
 *      - LIGHT_MODE_STROKE_ALPHA：描邊不透明度 (0 ~ 255)。
 * 10. 夜間模式動態調色參數：
 *      - DARK_MODE_LIGHTEN_RATIO：明度提升係數 (>= 1.0，數值越大描邊越明亮)。
 *      - DARK_MODE_MIN_VALUE：夜間描邊明度保底下限 (0.0 ~ 1.0，確保即使極深色也能生成足夠亮的描邊)。
 *      - DARK_MODE_SATURATION_RATIO：飽和度調整係數 (通常 <= 1.0，數值越小越淡雅)。
 *      - DARK_MODE_STROKE_ALPHA：描邊不透明度 (0 ~ 255)。
 * 11. 靜態固定描邊顏色備用（當 USE_DYNAMIC_COLOR_STROKE = false 時生效）：
 *      - LIGHT_MODE_STATIC_STROKE_COLOR
 *      - DARK_MODE_STATIC_STROKE_COLOR
 * ==========================================================
 */
public final class LocationStrokeHelper {

    // ==========================================================
    // >>>>>>>>>>>>>>>>>>>> 【描邊調配參數】 <<<<<<<<<<<<<<<<<<<<
    // ==========================================================

    /**
     * 是否對所有地點強制啟用描邊（無需達到閾值）。
     * 調配或測試時可設為 true；正式使用建議保持 false。
     */
    public static final boolean ALWAYS_APPLY_STROKE = false;

    /** 白天模式觸發描邊的明度下限 (0.0 ~ 1.0)。明度 >= 此值的顏色會加上深色描邊。默認 0.84f */
    public static final float LIGHT_LUMINANCE_THRESHOLD = 0.84f;

    /** 夜間模式觸發描邊的明度上限 (0.0 ~ 1.0)。明度 <= 此值的顏色會加上淺色描邊。默認 0.38f */
    public static final float DARK_LUMINANCE_THRESHOLD = 0.20f;

    /** 描邊模式枚舉 */
    public static final int MODE_STROKE = 1; // 實線輪廓描邊
    public static final int MODE_GLOW = 2;   // 柔和外發光/光暈
    public static final int MODE_BOTH = 3;   // 實線描邊 + 外發光

    /** 當前啟用的描邊模式（可設為 MODE_STROKE、MODE_GLOW 或 MODE_BOTH）。默認 MODE_STROKE */
    public static final int STROKE_MODE = MODE_STROKE;

    /** 描邊寬度係數（相對於當前字體大小 textSize 的比例）。默認 0.08f */
    public static final float STROKE_WIDTH_RATIO = 0.08f;

    /** 最小描邊寬度（像素 px），確保小字體或高分率屏幕上依然有清晰輪廓。默認 1.2f */
    public static final float MIN_STROKE_WIDTH_PX = 1.2f;

    /** 外發光/光暈模式下的模糊半徑 (px)。默認 1.0f */
    public static final float GLOW_RADIUS_PX = 1.0f;

    // ----------------------------------------------------------
    // >>> 動態原色調配參數（白天調暗、夜間調淡） <<<
    // ----------------------------------------------------------

    /**
     * 是否根據文字原色動態生成同色系描邊色。
     * true: 白天按原色變暗、夜間按原色變淡；
     * false: 直接使用下方的 LIGHT_MODE_STATIC_STROKE_COLOR / DARK_MODE_STATIC_STROKE_COLOR。
     */
    public static final boolean USE_DYNAMIC_COLOR_STROKE = true;

    /**
     * 【白天模式】明度衰減係數 (0.0 ~ 1.0)。
     * 數值越小，描邊顏色越暗/深。例如 0.45f 代表將原色明度降至 45%。
     */
    public static final float LIGHT_MODE_DARKEN_RATIO = 1.15f;

    /**
     * 【白天模式】飽和度調整係數。
     * 略大於 1.0 (如 1.10f) 能讓變暗後的描邊保持鮮明濃郁，不至於灰暗發濁。
     */
    public static final float LIGHT_MODE_SATURATION_RATIO = 0.88f;

    /**
     * 【白天模式】描邊不透明度 (0 ~ 255)。
     * 255 為完全不透明，數值越小越半透明。
     */
    public static final int LIGHT_MODE_STROKE_ALPHA = 172;

    /**
     * 【夜間模式】明度提升係數 (>= 1.0)。
     * 數值越大，描邊顏色越明亮。例如 1.80f 代表將原色明度放大 1.8 倍。
     */
    public static final float DARK_MODE_LIGHTEN_RATIO = 1.80f;

    /**
     * 【夜間模式】明度保底下限 (0.0 ~ 1.0)。
     * 當原色極深（如深藏青、深紫）時，確保描邊明度至少達到此值（如 0.70f），保證在黑底下依然清晰可見。
     */
    public static final float DARK_MODE_MIN_VALUE = 0.70f;

    /**
     * 【夜間模式】飽和度調整係數 (0.0 ~ 1.0)。
     * 數值越小，描邊顏色越偏向淡雅柔和（粉/淡色調）。例如 0.75f 代表降低 25% 飽和度。
     */
    public static final float DARK_MODE_SATURATION_RATIO = 0.5f;

    /**
     * 【夜間模式】描邊不透明度 (0 ~ 255)。
     * 255 為完全不透明。
     */
    public static final int DARK_MODE_STROKE_ALPHA = 112;

    // ----------------------------------------------------------
    // >>> 靜態固定描邊顏色（僅當 USE_DYNAMIC_COLOR_STROKE = false 時使用） <<<
    // ----------------------------------------------------------

    /** 白天模式下的靜態描邊顏色 */
    @ColorInt
    public static final int LIGHT_MODE_STATIC_STROKE_COLOR = Color.argb(255, 0, 0, 0);

    /** 夜間模式下的靜態描邊顏色 */
    @ColorInt
    public static final int DARK_MODE_STATIC_STROKE_COLOR = Color.argb(255, 255, 255, 255);

    // ==========================================================

    private LocationStrokeHelper() {
    }

    /**
     * 計算單個顏色的相對明度 (0.0 ~ 1.0)。
     */
    public static double calculateLuminance(int color) {
        return ColorUtil.calculateLuminance(color);
    }

    /**
     * 計算一組顏色（如漸變色）的平均明度 (0.0 ~ 1.0)。
     */
    public static double calculateAverageLuminance(int[] colors) {
        return ColorUtil.calculateAverageLuminance(colors);
    }

    /**
     * 判斷當前顏色是否需要描邊，若需要則返回相應的描邊顏色；若不需要則返回 Color.TRANSPARENT。
     *
     * @param colors 地點文字顏色（單色或漸變色數組）
     * @param defaultTextColor 當前 TextView 默認文本顏色（用於自動識別白天/夜間模式）
     * @return 描邊顏色（包含 Alpha 通道），或 Color.TRANSPARENT（無需描邊）
     */
    @ColorInt
    public static int getStrokeColor(int[] colors, int defaultTextColor) {
        if (colors == null || colors.length == 0) return Color.TRANSPARENT;
        double colorLuminance = ColorUtil.calculateAverageLuminance(colors);
        double bgIndicatorLuminance = ColorUtil.calculateLuminance(defaultTextColor);

        // 在白天模式下，默認文字為深色（bgIndicatorLuminance < 0.5），背景為淺色
        // 在夜間模式下，默認文字為淺色（bgIndicatorLuminance >= 0.5），背景為深色
        boolean isNightMode = bgIndicatorLuminance >= 0.5;

        boolean needStroke = ALWAYS_APPLY_STROKE ||
                (!isNightMode && colorLuminance >= LIGHT_LUMINANCE_THRESHOLD) ||
                (isNightMode && colorLuminance <= DARK_LUMINANCE_THRESHOLD);

        if (!needStroke) {
            return Color.TRANSPARENT;
        }

        if (!USE_DYNAMIC_COLOR_STROKE) {
            return isNightMode ? DARK_MODE_STATIC_STROKE_COLOR : LIGHT_MODE_STATIC_STROKE_COLOR;
        }

        int baseColor = ColorUtil.getAverageColor(colors);
        return isNightMode
                ? ColorUtil.adjustColorForDarkMode(baseColor, DARK_MODE_LIGHTEN_RATIO, DARK_MODE_MIN_VALUE, DARK_MODE_SATURATION_RATIO, DARK_MODE_STROKE_ALPHA)
                : ColorUtil.adjustColorForLightMode(baseColor, LIGHT_MODE_DARKEN_RATIO, LIGHT_MODE_SATURATION_RATIO, LIGHT_MODE_STROKE_ALPHA);
    }

    /**
     * 根據字體大小計算合適的描邊寬度 (px)。
     */
    public static float getStrokeWidth(float textSize) {
        return Math.max(MIN_STROKE_WIDTH_PX, textSize * STROKE_WIDTH_RATIO);
    }
}
