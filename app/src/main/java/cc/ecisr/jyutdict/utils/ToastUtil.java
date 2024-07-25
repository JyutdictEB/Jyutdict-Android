package cc.ecisr.jyutdict.utils;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
		
		// TextView snackTextView = mSnackbar.getView().findViewById(R.id.snackbar_text);
		// snackTextView.setMaxLines(5);
		// snackTextView.setPadding(50,50,50,50);
		TextView snackButtonView = mSnackbar.getView().findViewById(R.id.snackbar_action);
		snackButtonView.setTextSize(18);
		snackButtonView.setTextColor(view.getResources().getColor(R.color.colorPrimary));
		
		
		mSnackbar.show();
	}
	
}