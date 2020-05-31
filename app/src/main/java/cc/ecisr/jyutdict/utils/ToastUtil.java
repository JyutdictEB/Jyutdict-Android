package cc.ecisr.jyutdict.utils;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

public class ToastUtil {
	private static Toast mToast;
	public static void msg(Context context, String msg) {
		if (mToast != null) {
			mToast.cancel();
		}
		mToast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
		mToast.show();
	}
	
	public static void tips(View view, String msg, String button) {
		Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
				.setAction(button, new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						//showMsg(view.getContext(),"你点了");
					}
				})
				.show();
	}
	
}