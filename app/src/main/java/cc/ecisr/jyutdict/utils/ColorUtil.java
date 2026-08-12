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

        if (rawValue instanceof JSONArray) {
            JSONArray array = (JSONArray) rawValue;
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

    public static int parseColorOrDefault(String colorString) {
        String normalized = normalizeLocationColor(colorString);
        return Color.parseColor(normalized != null ? normalized : DEFAULT_LOCATION_COLOR);
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
        int[] parsed = locationColorInts(colors);
        if (parsed.length == 1) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(parsed[0]);
            return drawable;
        }
        return new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                parsed
        );
    }

    /**
     * 獲取顏色亮度
     * @param colorString 表示RGB顏色的十六進制字符串，如"#FFFFFF"
     * @return double 格式，表示顏色的亮度，範圍從 0~252.705
     */
    public static double getLightness(String colorString) {
        int color = parseColorOrDefault(colorString);
        double r = Color.red(color);
        double g = Color.green(color);
        double b = Color.blue(color);
        return r*0.299 + g*0.578 + b*0.114;
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
}
