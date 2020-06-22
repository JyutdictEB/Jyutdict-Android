package cc.ecisr.jyutdict.struct;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cc.ecisr.jyutdict.utils.ColorUtil;
import cc.ecisr.jyutdict.utils.StringUtil;

public class Character {
	private static final String TAG = "`Character";
	private static final String ENTER = "<br>";
	
	private Map<String, String> key2val;
	private int type;
	
	public Character(JSONObject charaEntry, int type) {
		this.type = type;
		key2val = new HashMap<>(HeaderInfo.getInfoLength());
		Iterator<String> keysIterator = charaEntry.keys(); // 獲取地名鍵
		while (keysIterator.hasNext()) {
			String key = keysIterator.next();
			try {
				key2val.put(key, charaEntry.getString(key));
			} catch (JSONException ignored) {} // 理应不进入此处
		}
	}
	
	public Spanned printMeanings() {
		StringBuilder sb = new StringBuilder();
		String oriString = key2val.get(HeaderInfo.COLUMN_NAME_MEANING)
				.replaceAll("：?需要例句", "")
				.replaceAll("<", "&lt;")
				.replaceAll("(?=[^\"“])&lt;", "；&lt;");
		if (oriString.startsWith("[粵]") && oriString.contains("①")) {
			oriString = oriString.replaceFirst("\\[粵]", "[粵]；");
		}
		String[] meanings = oriString.split("[；。？！] *?((?=&lt;)|(?=[①-⑩]))"); // 根据分号换行
		for (String meaning: meanings) {
			if ("".equals(meaning)) continue;
			if (meaning.contains("[粵]")) {
				sb.append("<b>").append(meaning).append("</b>");
			} else {
				sb.append(meaning);
			}
			sb.append(ENTER);
		}
		sb.delete(sb.length()-ENTER.length(), sb.length());
		return Html.fromHtml(sb.toString());
	}
	
	public Spanned printLocations() {
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		int presentStringBeginPosition, presentStringEndPosition; // ssb拼接某地的String前后的位置，用于指定某地的Span
		StringBuilder sb = new StringBuilder(); // 用于合成各点自身的字符串
		
		String[] cityList = HeaderInfo.getCityListInShort();
		String value;
		for (String key: cityList) {
			value = key2val.get(key);
			if (value==null || "".equals(value) || !HeaderInfo.isNameACity(key)) continue;
			
			String[] fullName = HeaderInfo.getFullName(key);
			sb.delete(0, sb.length());
			sb.append(fullName[0]).append(fullName[1]).append(": ");
			presentStringBeginPosition = ssb.length();
			ssb.append(sb);
			
			if ((type & 0b0100) != 0) { // 啟用地區著色
				presentStringEndPosition = ssb.length();
				int textColor = ColorUtil.darken(HeaderInfo.getCityColor(key), 0.92); // 將顏色調暗
				ssb.setSpan(new ForegroundColorSpan(textColor),
						presentStringBeginPosition, presentStringEndPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				presentStringBeginPosition = ssb.length();
			}
			
			ssb.append(value).append(" \t");
			presentStringEndPosition = ssb.length();
			
			if ("_".equals(value)) {
				ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#89BAA3")),
						presentStringBeginPosition, presentStringEndPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			
			if (value.contains("?")) {
				ssb.setSpan(new StyleSpan(Typeface.ITALIC),
						presentStringBeginPosition, presentStringEndPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
		
		String note = key2val.get(HeaderInfo.COLUMN_NAME_NOTE);
		String classified = key2val.get(HeaderInfo.COLUMN_NAME_CLASS_MAJOR);
		if (!"".equals(note) || !"".equals(classified)) {
			ssb.append("\n");
			if (!"".equals(note)) {  // "備註"列
				ssb.append("\n");
				presentStringBeginPosition = ssb.length();
				ssb.append("註 ").append(key2val.get(HeaderInfo.COLUMN_NAME_NOTE));
				presentStringEndPosition = ssb.length();
				ssb.setSpan(new StyleSpan(Typeface.ITALIC),
						presentStringBeginPosition, presentStringEndPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			
			if (!"".equals(classified)) { // "大類"列
				ssb.append("\n");
				sb.delete(0, sb.length()).append(classified).append("\n");
				sb.append(key2val.get(HeaderInfo.COLUMN_NAME_CLASS_SECONDARY)).append("\n");
				sb.append(key2val.get(HeaderInfo.COLUMN_NAME_CLASS_MINOR));
				presentStringBeginPosition = ssb.length();
				ssb.append(sb);
				presentStringEndPosition = ssb.length();
				
				ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#89BAA3")),
						presentStringBeginPosition, presentStringEndPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
		return ssb;
	}
	
	public Spanned printCharacter() {
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		ForegroundColorSpan questionableColorSpan = new ForegroundColorSpan(Color.parseColor("#B9BAA3"));
		ForegroundColorSpan duplicateColorSpan = new ForegroundColorSpan(Color.parseColor("#3D3B4F"));
		String chara = key2val.get(HeaderInfo.COLUMN_NAME_CHARACTER);
		if (chara==null) return ssb;
		if ("".equals(chara) || "？".equals(chara)) ssb.append("？");
		else ssb.append(chara.replaceAll("[?/!！？見歸 ]", ""));
		if (chara.contains("?") || chara.contains("？")) {
			ssb.setSpan(questionableColorSpan, 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		if (chara.contains("見") || chara.contains("歸")) {
			ssb.setSpan(duplicateColorSpan, 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		
		return ssb;
	}
	
	public Spanned printUnicode() {
		String chara = key2val.get(HeaderInfo.COLUMN_NAME_CHARACTER).replaceAll("[?/!？！見歸 ]", "");
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		String unicode = StringUtil.charaToUnicode(chara);
		ssb.append(unicode);
//		if (unicode.startsWith("U+E") || (unicode.startsWith("U+F")&&unicode.charAt(3)<'9')) { // 這些都是萬國碼保留區
//			ssb.setSpan(new StrikethroughSpan(),
//					0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//		}
		
		String adaptedChara = key2val.get(HeaderInfo.COLUMN_NAME_CONVENTIONAL); // 俗字
		if (!"".equals(adaptedChara)) {
			ssb.append("\n(").append(adaptedChara).append(")");
		}
		return ssb;
	}
	
	public Spanned printPronunciation() {
		String pron = key2val.get(HeaderInfo.COLUMN_NAME_PRONUNCIATION).replace("!", "");
		SpannableStringBuilder ssb = new SpannableStringBuilder(pron);
		if (pron.contains("?")) {
			ssb.setSpan(new StyleSpan(Typeface.ITALIC),
					0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		
		return ssb;
	}
}
