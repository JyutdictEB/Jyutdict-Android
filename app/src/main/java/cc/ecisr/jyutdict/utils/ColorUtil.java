package cc.ecisr.jyutdict.utils;

import android.graphics.Color;
import android.util.Log;

public class ColorUtil {
	private static final String TAG = "`ColorUtil";
	public static double getLightness(String colorString) {
		int color = Color.parseColor(colorString);
		double r = Color.red(color);
		double g = Color.green(color);
		double b = Color.blue(color);
		return r*0.299 + g*0.578 + b*0.114;
	}
	
	public static int darken(String colorString, double ratio) {
		int color = Color.parseColor(colorString);
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		hsv[2] *= ratio;
		hsv[1] /= ratio*ratio;
		return Color.HSVToColor(hsv);
	}
}
