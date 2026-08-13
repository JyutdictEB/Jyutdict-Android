package cc.ecisr.jyutdict.utils;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * 沉浸式狀態欄/導航欄工具類
 * 兼容 Android 4.4+
 */
public class ImmersiveBarUtil {

    /**
     * 設置沉浸式狀態欄和導航欄
     *
     * @param activity        當前 Activity
     * @param statusDark      狀態欄圖標是否為深色（適合淺色背景）
     * @param navigationDark  導航欄圖標是否為深色（適合淺色背景）
     */
    @SuppressWarnings("deprecation")
    public static void setImmersiveBar(@NonNull Activity activity, boolean statusDark,
                                       boolean navigationDark) {
        Window window = activity.getWindow();
        if (window == null) return;

        WindowCompat.setDecorFitsSystemWindows(window, false);

        // Android 15+ enforces transparent, edge-to-edge system bars for targetSdk 35+.
        // Keep the explicit colors only where they are still honored for compatibility.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                window, window.getDecorView());
        controller.setAppearanceLightStatusBars(statusDark);
        controller.setAppearanceLightNavigationBars(navigationDark);
    }
}
