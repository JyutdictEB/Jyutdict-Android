package cc.ecisr.jyutdict.utils;

import android.graphics.Color;

/**
 * ColorUtil 類，用於存放處理顏色相關的函數
 */
public class ColorUtil {
	
	/**
	 * 獲取顏色亮度
	 * @param colorString 表示RGB顏色的十六進制字符串，如"#FFFFFF"
	 * @return double 格式，表示顏色的亮度，範圍從 0~252.705
	 */
	public static double getLightness(String colorString) {
		int color = Color.parseColor(colorString);
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
		return darken(Color.parseColor(colorString), ratio);
	}
	public static int darken(int color, double ratio) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		hsv[2] *= ratio;
		hsv[1] /= ratio*ratio;
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
		int color = Color.parseColor(colorString);
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		double ratio = hsv[2] * (b - a) + a;
		
		hsv[2] *= ratio;
		hsv[1] /= ratio*ratio;
		return Color.HSVToColor(hsv);
	}

	/**
	 * 將色相空間分為 max 份，返回第 i 份顏色，i 從 1 開始計
	 */
	public static int ithColorInHsv(int i, int max) {
		float[] hsv = new float[3];
		hsv[0] = (float)((int)(i/2f+1) + ((i%2==0)?(int)(max/2f-0.5):0) - 1) * 360 / max;
		hsv[1] = 0.4f;
		hsv[2] = 0.8f;
		return Color.HSVToColor(hsv);
	}
}
