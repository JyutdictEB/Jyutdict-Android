package cc.ecisr.jyutdict.utils;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * ColorUtil 類，用於存放處理顏色相關的函數
 */
public class ColorUtil {
    public static final String DEFAULT_LOCATION_COLOR = "#888888";

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 解析 API 中的地點顏色。兼容：
     * 1. 單個顏色字符串
     * 2. color / colors 字段中的顏色數組
     * 3. 以逗號、分號或豎線分隔的兼容字符串
     *
     * 非法值會被忽略；歷史音使用的純黑色會轉為中性灰色。
     */
    public static ArrayList<String> parseLocationColors(Object... rawValues) {
        ArrayList<String> colors = new ArrayList<>();
        if (rawValues == null) return colors;

        for (Object rawValue : rawValues) {
            appendLocationColors(colors, rawValue);
        }
        return colors;
    }

    private static void appendLocationColors(ArrayList<String> colors, Object rawValue) {
        if (rawValue == null || rawValue == JSONObject.NULL) return;

        if (rawValue instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) {
                appendLocationColors(colors, array.opt(i));
            }
            return;
        }

        String value = String.valueOf(rawValue).trim();
        if (value.isEmpty()) return;

        String[] candidates = value.split("[,;|]");
        for (String candidate : candidates) {
            String normalized = normalizeLocationColor(candidate);
            if (normalized != null && !colors.contains(normalized)) {
                colors.add(normalized);
            }
        }
    }

    public static String primaryLocationColor(List<String> colors) {
        if (colors != null) {
            for (String color : colors) {
                String normalized = normalizeLocationColor(color);
                if (normalized != null) return normalized;
            }
        }
        return DEFAULT_LOCATION_COLOR;
    }

    public static String normalizeLocationColor(String colorString) {
        if (colorString == null) return null;
        String candidate = colorString.trim();
        if (candidate.isEmpty()) return null;

        try {
            int parsed = Color.parseColor(candidate);
            return parsed == Color.BLACK ? DEFAULT_LOCATION_COLOR : candidate;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * 解析十六進制顏色字符串為色值，解析失敗時返回默認的 fallbackColor。
     */
    public static int parseColor(String colorString, int fallbackColor) {
        if (colorString == null) return fallbackColor;
        String candidate = colorString.trim();
        if (candidate.isEmpty()) return fallbackColor;
        try {
            return Color.parseColor(candidate);
        } catch (IllegalArgumentException ignored) {
            return fallbackColor;
        }
    }

    /**
     * 解析地點顏色字符串為色值，無效時回退為 DEFAULT_LOCATION_COLOR (#888888)。
     */
    public static int parseColorOrDefault(String colorString) {
        String normalized = normalizeLocationColor(colorString);
        int fallback = Color.parseColor(DEFAULT_LOCATION_COLOR);
        return normalized != null ? parseColor(normalized, fallback) : fallback;
    }

    /** 將一個地點的全部顏色轉為可直接繪製的色值，無有效值時回退為中性灰。 */
    public static int[] locationColorInts(List<String> colors) {
        return locationColorInts(colors, 1.0);
    }

    /** 與 {@link #locationColorInts(List)} 相同，並按顯示主題調整每種顏色的明度。 */
    public static int[] locationColorInts(List<String> colors, double darkenRatio) {
        ArrayList<Integer> parsed = new ArrayList<>();
        if (colors != null) {
            for (String color : colors) {
                String normalized = normalizeLocationColor(color);
                if (normalized != null) {
                    parsed.add(darken(normalized, darkenRatio));
                }
            }
        }
        if (parsed.isEmpty()) {
            parsed.add(darken(DEFAULT_LOCATION_COLOR, darkenRatio));
        }

        int[] result = new int[parsed.size()];
        for (int i = 0; i < parsed.size(); i++) {
            result[i] = parsed.get(i);
        }
        return result;
    }

    /** 單色時返回純色 drawable，多色時返回由左至右的漸變。 */
    public static GradientDrawable locationColorDrawable(List<String> colors) {
        return locationColorDrawable(colors, GradientDrawable.Orientation.LEFT_RIGHT);
    }

    /** 單色時返回純色 drawable，多色時按指定方向漸變。 */
    public static GradientDrawable locationColorDrawable(
            List<String> colors,
            GradientDrawable.Orientation orientation
    ) {
        int[] parsed = locationColorInts(colors);
        if (parsed.length == 1) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(parsed[0]);
            return drawable;
        }
        return new GradientDrawable(
                orientation,
                parsed
        );
    }

    /**
     * 獲取顏色相對明度 (0.0 ~ 1.0)
     * @param color 整形色值 (ARGB/RGB)
     * @return 範圍從 0.0 ~ 1.0 的明度
     */
    public static double getLightness(int color) {
        return (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
    }

    /**
     * 獲取顏色相對明度 (0.0 ~ 1.0)
     * @param colorString 表示RGB顏色的十六進制字符串，如"#FFFFFF"
     * @return 範圍從 0.0 ~ 1.0 的明度
     */
    public static double getLightness(String colorString) {
        return getLightness(parseColorOrDefault(colorString));
    }

    /**
     * 計算單個顏色的相對明度 (0.0 ~ 1.0)，為 {@link #getLightness(int)} 的別名。
     */
    public static double calculateLuminance(int color) {
        return getLightness(color);
    }

    /**
     * 計算一組顏色的平均相對明度 (0.0 ~ 1.0)。
     */
    public static double calculateAverageLuminance(int[] colors) {
        if (colors == null || colors.length == 0) return 0.5;
        double sum = 0;
        for (int c : colors) {
            sum += getLightness(c);
        }
        return sum / colors.length;
    }

    /**
     * 計算一組顏色（如漸變色）的平均 RGB 顏色。
     */
    public static int getAverageColor(int[] colors) {
        if (colors == null || colors.length == 0) return Color.GRAY;
        int r = 0, g = 0, b = 0, a = 0;
        for (int c : colors) {
            a += Color.alpha(c);
            r += Color.red(c);
            g += Color.green(c);
            b += Color.blue(c);
        }
        int len = colors.length;
        return Color.argb(a / len, r / len, g / len, b / len);
    }

    /**
     * 將顏色調暗（白天模式描邊或文字加深）。
     *
     * @param color 原色
     * @param darkenRatio 明度係數 (0.0 ~ 1.0，越小越暗)
     * @param saturationRatio 飽和度係數 (通常 >= 1.0 保持鮮明)
     * @param alpha 不透明度 (0 ~ 255)
     * @return 調暗後的顏色
     */
    public static int adjustColorForLightMode(int color, float darkenRatio, float saturationRatio, int alpha) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = clamp(hsv[1] * saturationRatio, 0f, 1f);
        hsv[2] = clamp(hsv[2] * darkenRatio, 0f, 1f);
        int rgb = Color.HSVToColor(hsv);
        return Color.argb(alpha, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
    }

    /**
     * 將顏色調淡/調亮（夜間模式描邊或文字提亮）。
     *
     * @param color 原色
     * @param lightenRatio 明度提升係數 (>= 1.0)
     * @param minValue 明度保底下限 (0.0 ~ 1.0)
     * @param saturationRatio 飽和度係數 (通常 <= 1.0 偏向柔和)
     * @param alpha 不透明度 (0 ~ 255)
     * @return 調淡/調亮後的顏色
     */
    public static int adjustColorForDarkMode(int color, float lightenRatio, float minValue, float saturationRatio, int alpha) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = clamp(hsv[1] * saturationRatio, 0f, 1f);
        hsv[2] = clamp(Math.max(hsv[2] * lightenRatio, minValue), 0f, 1f);
        int rgb = Color.HSVToColor(hsv);
        return Color.argb(alpha, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
    }

    /**
     * 將輸入顏色的明度乘以一個係數再返回
     * 以調節該顏色明度
     * 爲了保持顏色的飽和度，明度在乘以係數的同時，飽和度除以該係數的平方
     * 如，係數爲 0.5 時，明度降爲一半，飽和度昇爲四倍
     *
     * @param colorString 表示顏色的十六進制字符串，如"#FFFFFFF"
     * @param ratio 明度係數
     * @return 以整形數字表示的顏色代碼
     */
    public static int darken(String colorString, double ratio) {
        return darken(parseColorOrDefault(colorString), ratio);
    }
    public static int darken(int color, double ratio) {
        float safeRatio = clamp((float) ratio, 0.2f, 2.0f);
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = clamp(hsv[2] * safeRatio, 0f, 1f);
        hsv[1] = clamp(hsv[1] / (safeRatio * safeRatio), 0f, 1f);
        return Color.HSVToColor(hsv);
    }

    /**
     * 將輸入顏色的明度 ∈[0, 1] 映射到區間 [a, b]
     * 以調節該顏色明度
     * 爲了保持顏色的飽和度，明度在乘以係數的同時，飽和度除以該係數的平方
     * 如，a=0.5, b=1.0，原顏色明度 =0.5 時，輸出明度 =0.75
     *
     * @param colorString 表示顏色的十六進制字符串，如"#FFFFFFF"
     * @param a 映射左區間
     * @param b 映射右區間
     * @return 以整形數字表示的顏色代碼
     */
    public static int remapValue(String colorString, double a, double b) {
        int color = parseColorOrDefault(colorString);
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        double ratio = hsv[2] * (b - a) + a;

        float safeRatio = clamp((float) ratio, 0.2f, 2.0f);
        hsv[2] = clamp(hsv[2] * safeRatio, 0f, 1f);
        hsv[1] = clamp(hsv[1] / (safeRatio * safeRatio), 0f, 1f);
        return Color.HSVToColor(hsv);
    }

    /**
     * 將色相空間分為 max 份，返回第 i 份顏色，i 從 1 開始計
     */
    public static int ithColorInHsv(int i, int max) {
        if (max <= 0) {
            return Color.parseColor(DEFAULT_LOCATION_COLOR);
        }
        float[] hsv = new float[3];
        hsv[0] = (float)((int)(i/2f+1) + ((i%2==0)?(int)(max/2f-0.5):0) - 1) * 360 / max;
        hsv[1] = 0.4f;
        hsv[2] = 0.8f;
        return Color.HSVToColor(hsv);
    }

    /**
     * 解析當前主題下的顏色屬性，如 R.attr.clockTextColor、R.attr.clockHover 等
     */
    public static int resolveThemeColor(android.content.Context context, int attrResId) {
        if (context == null) return Color.BLACK;
        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (context.getTheme().resolveAttribute(attrResId, typedValue, true)) {
            if (typedValue.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT
                    && typedValue.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT) {
                return typedValue.data;
            } else if (typedValue.resourceId != 0) {
                return androidx.core.content.ContextCompat.getColor(context, typedValue.resourceId);
            }
        }
        return Color.BLACK;
    }
}
