package cc.ecisr.jyutdict.widget;

import android.graphics.Color;
import androidx.annotation.ColorInt;

/**
 * 地點名稱描邊工具類。
 *
 * 當地點顏色在白天模式下過淺（接近白色背景）或在夜間模式下過深（接近深色背景）時，
 * 為文字添加外輪廓描邊以保證足夠的對比度與辨識度。
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
 * 8. LIGHT_MODE_STROKE_COLOR：白天模式下的描邊顏色（ARGB，A 為 0~255）。
 * 9. DARK_MODE_STROKE_COLOR：夜間模式下的描邊顏色（ARGB，A 為 0~255）。
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

    /** 白天模式觸發描邊的明度下限 (0.0 ~ 1.0)。明度 >= 此值的顏色會加上深色描邊。默認 0.68f */
    public static final float LIGHT_LUMINANCE_THRESHOLD = 0.84f;

    /** 夜間模式觸發描邊的明度上限 (0.0 ~ 1.0)。明度 <= 此值的顏色會加上淺色描邊。默認 0.38f */
    public static final float DARK_LUMINANCE_THRESHOLD = 0.38f;

    /** 描邊模式枚舉 */
    public static final int MODE_STROKE = 1; // 實線輪廓描邊
    public static final int MODE_GLOW = 2;   // 柔和外發光/光暈
    public static final int MODE_BOTH = 3;   // 實線描邊 + 外發光

    /** 當前啟用的描邊模式（可設為 MODE_STROKE、MODE_GLOW 或 MODE_BOTH）。默認 MODE_STROKE */
    public static final int STROKE_MODE = MODE_STROKE;

    /** 描邊寬度係數（相對於當前字體大小 textSize 的比例）。默認 0.12f */
    public static final float STROKE_WIDTH_RATIO = 0.08f;

    /** 最小描邊寬度（像素 px），確保小字體或高分率屏幕上依然有清晰輪廓。默認 2.0f */
    public static final float MIN_STROKE_WIDTH_PX = 1.2f;

    /** 外發光/光暈模式下的模糊半徑 (px)。默認 4.0f */
    public static final float GLOW_RADIUS_PX = 1.0f;

    /** 白天模式下的描邊顏色（ARGB 格式，A 為 0~255 控制不透明度）。默認 Color.argb(180, 20, 20, 20) */
    @ColorInt
    public static final int LIGHT_MODE_STROKE_COLOR = Color.argb(45, 120, 120, 120);

    /** 夜間模式下的描邊顏色（ARGB 格式，A 為 0~255 控制不透明度）。默認 Color.argb(180, 240, 240, 240) */
    @ColorInt
    public static final int DARK_MODE_STROKE_COLOR = Color.argb(45, 0, 0, 0); // 特意用的黑色。用白色會反而更不易看

    // ==========================================================

    private LocationStrokeHelper() {
    }

    /**
     * 計算單個顏色的相對明度 (0.0 ~ 1.0)。
     */
    public static double calculateLuminance(int color) {
        return (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
    }

    /**
     * 計算一組顏色（如漸變色）的平均明度 (0.0 ~ 1.0)。
     */
    public static double calculateAverageLuminance(int[] colors) {
        if (colors == null || colors.length == 0) return 0.5;
        double sum = 0;
        for (int c : colors) {
            sum += calculateLuminance(c);
        }
        return sum / colors.length;
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
        double colorLuminance = calculateAverageLuminance(colors);
        double bgIndicatorLuminance = calculateLuminance(defaultTextColor);

        // 在白天模式下，默認文字為深色（bgIndicatorLuminance < 0.5），背景為淺色
        // 在夜間模式下，默認文字為淺色（bgIndicatorLuminance >= 0.5），背景為深色
        boolean isNightMode = bgIndicatorLuminance >= 0.5;

        if (ALWAYS_APPLY_STROKE) {
            return isNightMode ? DARK_MODE_STROKE_COLOR : LIGHT_MODE_STROKE_COLOR;
        }

        if (!isNightMode) {
            // 白天模式：如果地點顏色過淺，添加深色描邊
            if (colorLuminance >= LIGHT_LUMINANCE_THRESHOLD) {
                return LIGHT_MODE_STROKE_COLOR;
            }
        } else {
            // 夜間模式：如果地點顏色過深，添加淺色描邊
            if (colorLuminance <= DARK_LUMINANCE_THRESHOLD) {
                return DARK_MODE_STROKE_COLOR;
            }
        }
        return Color.TRANSPARENT;
    }

    /**
     * 根據字體大小計算合適的描邊寬度 (px)。
     */
    public static float getStrokeWidth(float textSize) {
        return Math.max(MIN_STROKE_WIDTH_PX, textSize * STROKE_WIDTH_RATIO);
    }
}
