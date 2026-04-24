package cc.ecisr.jyutdict.utils;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

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
    public static void setImmersiveBar(@NonNull Activity activity, boolean statusDark, boolean navigationDark) {
        Window window = activity.getWindow();
        if (window == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

            int visibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                visibility |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            }

            window.getDecorView().setSystemUiVisibility(visibility);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setStatusBarDarkMode(window, statusDark);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setNavigationBarDarkMode(window, navigationDark);
            }

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
    }

    private static void setStatusBarDarkMode(Window window, boolean dark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = window.getDecorView();
            int systemUiVisibility = decorView.getSystemUiVisibility();
            if (dark) {
                systemUiVisibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                systemUiVisibility &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decorView.setSystemUiVisibility(systemUiVisibility);
        }
    }

    private static void setNavigationBarDarkMode(Window window, boolean dark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decorView = window.getDecorView();
            int systemUiVisibility = decorView.getSystemUiVisibility();
            if (dark) {
                systemUiVisibility |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                systemUiVisibility &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decorView.setSystemUiVisibility(systemUiVisibility);
        }
    }
}
