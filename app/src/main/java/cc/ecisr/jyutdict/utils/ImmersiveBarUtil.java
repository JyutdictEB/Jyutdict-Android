package cc.ecisr.jyutdict.utils;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(statusDark);
            controller.setAppearanceLightNavigationBars(navigationDark);
        }
    }

    /**
     * 為頂部 Toolbar 應用狀態欄沉浸式高度 Padding
     */
    public static void applyToolbarInsets(@NonNull View toolbar) {
        final int originalPaddingLeft = toolbar.getPaddingLeft();
        final int originalPaddingTop = toolbar.getPaddingTop();
        final int originalPaddingRight = toolbar.getPaddingRight();
        final int originalPaddingBottom = toolbar.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets barInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(originalPaddingLeft, originalPaddingTop + barInsets.top,
                    originalPaddingRight, originalPaddingBottom);
            return insets;
        });
        requestApplyInsetsWhenAttached(toolbar);
    }

    /**
     * 為底部 View（如滾動列表、底部按鈕欄或頁面根佈局）應用導航欄高度 Padding
     */
    public static void applyBottomInsets(@NonNull View view) {
        final int originalPaddingLeft = view.getPaddingLeft();
        final int originalPaddingTop = view.getPaddingTop();
        final int originalPaddingRight = view.getPaddingRight();
        final int originalPaddingBottom = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets barInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(originalPaddingLeft, originalPaddingTop, originalPaddingRight,
                    originalPaddingBottom + barInsets.bottom);
            return insets;
        });
        requestApplyInsetsWhenAttached(view);
    }

    private static void requestApplyInsetsWhenAttached(@NonNull View view) {
        if (ViewCompat.isAttachedToWindow(view)) {
            ViewCompat.requestApplyInsets(view);
        } else {
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    v.removeOnAttachStateChangeListener(this);
                    ViewCompat.requestApplyInsets(v);
                }

                @Override
                public void onViewDetachedFromWindow(View v) {}
            });
        }
    }
}
