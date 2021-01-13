package cc.ecisr.jyutdict.utils;

import java.util.regex.Pattern;

/**
 * StringUtil 類，用於存放處理字符串相關的函數
 */
public class StringUtil {
	
	/**
	 * 判斷字符串是否由純廿六英字字母([a-zA-Z])及數字([0-9])組成
	 * @param s 被檢驗的字符串
	 * @return {@code true}，若 {@code s} 由純字母及數字組成
	 */
	public static boolean isAlphaString(final String s) {
		Pattern pattern = Pattern.compile("^[0-9a-zA-Z]+$");
		return pattern.matcher(s).matches();
	}
	
	/**
	 * 生成漢字的統一碼
	 * 支持擴展B區及以後的漢字
	 * 允許以多個漢字作輸入
	 * @param gbString 包含一個或多個字符的字符串
	 * @return 以"U+XXXX"或"U+XXXXX"爲格式的統一碼字符串，X是0~F的十六進制數碼，字母使用大寫，各碼位之間用空格分隔
	 */
	public static String charaToUnicode(final String gbString) {
		char[] utfBytes = gbString.toCharArray();
		StringBuilder unicodeBytes = new StringBuilder();
		for (int i = 0; i<utfBytes.length; i++ ) {
			if (utfBytes[i]==' ') continue;
			String hexB;
			if (Character.isHighSurrogate(utfBytes[i])) {
				hexB = Integer.toHexString(Character.toCodePoint(utfBytes[i], utfBytes[i+1]));
				i++;
			} else {
				hexB = Integer.toHexString(utfBytes[i]);
				if (hexB.length() <= 2) {
					hexB = "00" + hexB;
				}
			}
			if (i!=0) unicodeBytes.append(" ");
			unicodeBytes.append("U+").append(hexB.toUpperCase());
		}
		return unicodeBytes.toString();
	}
}