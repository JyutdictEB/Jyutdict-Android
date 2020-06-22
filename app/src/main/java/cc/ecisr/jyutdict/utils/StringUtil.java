package cc.ecisr.jyutdict.utils;

import java.util.regex.Pattern;

public class StringUtil {
	public static boolean isAlphaString(String s) {
		Pattern pattern = Pattern.compile("^[0-9a-zA-Z]+$");
		return pattern.matcher(s).matches();
	}
	
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
			unicodeBytes.append("u").append(hexB.toUpperCase());
		}
		return unicodeBytes.toString();
	}
}