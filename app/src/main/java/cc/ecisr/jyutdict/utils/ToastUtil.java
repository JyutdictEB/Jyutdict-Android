package cc.ecisr.jyutdict.utils;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import cc.ecisr.jyutdict.R;

public class ToastUtil {
    private static Toast mToast;
    public static void msg(Context context, String msg) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
        mToast.show();
    }

    private static Snackbar mSnackbar;
    public static void tips(View view, String msg, String button) {
        if (view == null) { return; }
        if (mSnackbar != null) {
            mSnackbar.dismiss();
        }
        mSnackbar = Snackbar.make(view, msg, Snackbar.LENGTH_INDEFINITE)
                .setAction(button, view1 -> {
                    //
                });

        TextView snackButtonView = mSnackbar.getView().findViewById(
                com.google.android.material.R.id.snackbar_action
        );
        if (snackButtonView != null) {
            snackButtonView.setTextSize(14);
            snackButtonView.setTextColor(ContextCompat.getColor(
                    snackButtonView.getContext(),
                    R.color.colorPrimary
            ));
        }

        mSnackbar.show();
    }
}
