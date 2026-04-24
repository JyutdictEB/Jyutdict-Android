package cc.ecisr.jyutdict.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

/**
 * 主題工具類
 */
public class ThemeUtil {

    /**
     * 判斷當前是否為夜間模式
     * 根據用戶設置決定：跟隨系統、白天、夜間
     */
    public static boolean isNightMode(Context context) {
        SharedPreferences sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String themeMode = sp.getString("theme_mode", "follow_system");

        if ("follow_system".equals(themeMode)) {
            int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        } else if ("dark".equals(themeMode)) {
            return true;
        } else {
            return false;
        }
    }
}
