package cc.ecisr.jyutdict.struct;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cc.ecisr.jyutdict.utils.ColorUtil;
import cc.ecisr.jyutdict.utils.StringUtil;

/**
 * Character 類，用於儲存一條字頭項，與輸出顯示內容
 */
public class Character {
	private static final String ENTER = "<br>";
	
	// 一項字所有信息，包含地區音等，以 HashMap 鍵值對保存
	// 如泛粵字表的一項有格式：
	// { "繁"=>"一", "綜"=>"jat1", "穗"=>"jat", ..., "釋義"=>"…", ... }
	// 目前（v0.2.5/200719）僅用在泛粵字表上，怎麼應用到通語字表上還需要再考慮一下
	private Map<String, String> key2val;
	
	// 輸出到屏幕上時的設置
	// 包含如“是否對地方名著色”等顯示設置
	private EntrySetting settings;
	
	/**
	 * 構造函數
	 *
	 * @param charaEntry 一個 JSONObject 類對象，以初始化 {@code this.key2val}
	 *                   其原始字符串爲：{"繁":"一","綜":"jat1",...,"釋義":"…",...}
	 *
	 * @param settings 一個 EntrySettings 類對象，以初始化 this.settings
	 */
	public Character(JSONObject charaEntry, final EntrySetting settings) {
		this.settings = settings;
		key2val = new HashMap<>(HeaderInfo.getInfoLength());
		Iterator<String> keysIterator = charaEntry.keys(); // 獲取地名鍵
		while (keysIterator.hasNext()) {
			String key = keysIterator.next();
			try {
				key2val.put(key, charaEntry.getString(key));
			} catch (JSONException ignored) {} // 理应不进入此处
		}
	}
	
	/**
	 * 向屏幕打印釋義
	 * 對應字項 Layout 右上部分
	 *
	 * 僅於查詢泛粵字表時調用
	 *
	 * @return spanned 格式的富文本，可直接調用 setText() 顯示
	 */
	public Spanned printMeanings() {
		StringBuilder sb = new StringBuilder();
		
		// 默认不会发生空指针异常，因为服务器返回的json必定存在键COLUMN_NAME_MEANING
		String oriString = key2val.get(HeaderInfo.COLUMN_NAME_MEANING)
				.replaceAll("：?需要例句", "")
				.replaceAll("<", "&lt;")
				.replaceAll("(?<=[^①-⑩\"“])&lt;", "；&lt;");
		
		// 釋義以「[粵]①」开头则换行，变为「[粵]<br>①」
		if (oriString.startsWith("[粵]") && oriString.contains("①")) {
			oriString = oriString.replaceFirst("\\[粵]", "[粵]；");
		}
		
		// 根据分号换行：在「；[①-⑩]」條件下
		String[] meanings = oriString.split("[；。？！] *?((?=&lt;)|(?=[①-⑩]))");
		
		for (String meaning: meanings) {
			if ("".equals(meaning)) continue;
			if (meaning.contains("[粵]")) {
				sb.append("<b>").append(meaning).append("</b>");
			} else {
				sb.append(meaning);
			}
			sb.append(ENTER);
		}
		
		if (oriString.length()!=0) sb.delete(sb.length()-ENTER.length(), sb.length());
		
		return Html.fromHtml(sb.toString());
	}
	
	/**
	 * 向屏幕打印地方音、註、詞場
	 * 對應字項 Layout 右下部分
	 *
	 * 僅於查詢泛粵字表時調用
	 *
	 * @return Spanned 格式的富文本，可直接調用 setText() 顯示
	 */
	public Spanned printLocations() {
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		
		// ssb拼接某地的String前后的位置，用于指定某地的Span
		// 如初始狀態 ssb.length() == 5
		// presentStringBeginPosition = 5
		// ssb.append("1")
		// presentStringEndPosition = 6
		int presentStringBeginPosition, presentStringEndPosition;
		
		// 獲取簡稱作爲鍵，從而在 key2val 獲取值
		// 如   [ ..., "港", "穗", "澳", ... ]
		// 和   [ ..., "官", "客", "吳", ... ]
		String[] cityList = HeaderInfo.getCityListInShort();
		String[] foreignList = HeaderInfo.getForeignListInShort();
		
		// 記錄鍵對應的值
		String value;
		
		// 記錄各市自身部分
		// 如 "廣州: jat1"
		// 在下方循環體內生成並合併進 ssb
		StringBuilder sb = new StringBuilder();
		for (String key: cityList) {
			value = key2val.get(key);
			if (value==null || "".equals(value) || !HeaderInfo.isNameACity(key)) continue; // isNameACity(key)有甚麼用？我也忘了
			
			String[] fullName = HeaderInfo.getFullName(key);
			sb.delete(0, sb.length());
			sb.append(fullName[0]).append(fullName[1]).append(": ");
			presentStringBeginPosition = ssb.length();
			ssb.append(sb);
			
			// 對地區名著色
			if (settings.isAreaColoring) {
				presentStringEndPosition = ssb.length();
				int textColor = ColorUtil.darken(HeaderInfo.getCityColor(key), settings.areaColoringDarkenRatio); // 將顏色調暗
				ssb.setSpan(new ForegroundColorSpan(textColor),
						presentStringBeginPosition, presentStringEndPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				presentStringBeginPosition = ssb.length();
			}
			
			// 加上刪除線
			if (!value.contains("^")) {
				ssb.append(value);
			} else {
				String[] splitedPron = value.split("\\^");
				for (String subStr: splitedPron) {
					if (subStr.charAt(0) <= 'z') {
						ssb.append(subStr);
					} else {
						ssb.append(subStr);
						ssb.setSpan(new StrikethroughSpan(),
								ssb.length()-subStr.length(), ssb.length()-subStr.length()+1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
				}
			}
			
			ssb.append(" \t");
			presentStringEndPosition = ssb.length();
			
			// 若爲「_」，字體著淺灰色
			if ("_".equals(value)) {
				ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#BBBBBB")),
						presentStringBeginPosition, presentStringEndPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			
			// 若讀音存在「?」，使用斜體標註
			if (value.contains("?")) {
				ssb.setSpan(new StyleSpan(Typeface.ITALIC),
						presentStringBeginPosition, presentStringEndPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
		
		if (settings.isDisplayEcdemic) {
			boolean isForeignPronEnterExist = false;
			
			for (String key : foreignList) {
				value = key2val.get(key);
				if (value == null || "".equals(value)) continue;
				if (!isForeignPronEnterExist) {
					ssb.append("\n\n");
					ssb.setSpan(new RelativeSizeSpan(0.5f), ssb.length()-1, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					isForeignPronEnterExist = true;
				}
				
				sb.delete(0, sb.length());
				sb.append(key).append(": ");
				presentStringBeginPosition = ssb.length();
				ssb.append(sb);
				
				// 對地區名著色
				if (settings.isAreaColoring) {
					presentStringEndPosition = ssb.length();
					int textColor = ColorUtil.darken(HeaderInfo.getForeignColor(key), settings.areaColoringDarkenRatio); // 將顏色調暗
					ssb.setSpan(new ForegroundColorSpan(textColor),
							presentStringBeginPosition, presentStringEndPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					presentStringBeginPosition = ssb.length();
				}
				
				ssb.append(value.replace('\n', ',')).append(" \t");
				presentStringEndPosition = ssb.length();
				
				// 若讀音存在「?」，使用斜體標註
				if (value.contains("?")) {
					ssb.setSpan(new StyleSpan(Typeface.ITALIC),
							presentStringBeginPosition, presentStringEndPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}
		}
		
		
		// note 爲「備註」一列的值
		String note = key2val.get(HeaderInfo.COLUMN_NAME_NOTE);
		// classified 爲「大類」一列的值
		String classified = key2val.get(HeaderInfo.COLUMN_NAME_CLASS_MAJOR);
		if (!"".equals(note) || !"".equals(classified)) {
			ssb.append("\n");
			
			// 打印備註
			if (!"".equals(note)) {  // "備註"列
				ssb.append("\n");
				presentStringBeginPosition = ssb.length();
				ssb.append("註 ").append(key2val.get(HeaderInfo.COLUMN_NAME_NOTE));
				presentStringEndPosition = ssb.length();
				ssb.setSpan(new StyleSpan(Typeface.ITALIC),
						presentStringBeginPosition, presentStringEndPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			
			// 打印詞場
			if (!"".equals(classified)) { // "大類"列
				ssb.append("\n");
				sb.delete(0, sb.length()).append(classified);
				String class_secondary = key2val.get(HeaderInfo.COLUMN_NAME_CLASS_SECONDARY);
				String class_minor = key2val.get(HeaderInfo.COLUMN_NAME_CLASS_MINOR);
				if (!"".equals(class_secondary)) sb.append("\n").append(class_secondary);
				if (!"".equals(class_minor)) sb.append("\n").append(class_minor);
				presentStringBeginPosition = ssb.length();
				ssb.append(sb);
				presentStringEndPosition = ssb.length();
				
				ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#BBBBBB")),
						presentStringBeginPosition, presentStringEndPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
		return ssb;
	}
	
	/**
	 * 向屏幕打印字頭
	 * 對應字項 Layout 左上部分
	 *
	 * 原字頭存在各種標記，因此預處理需要刪除這些標記
	 *
	 * @return Spanned 格式的富文本，可直接調用 setText() 顯示
	 */
	public Spanned printCharacter() {
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		
		// chara 爲「繁」一列的值，即錔字
		String chara = key2val.get(HeaderInfo.COLUMN_NAME_CHARACTER);
		if (chara==null) return ssb;
		
		// 沒有錔字的一列以「□」作顯示，使用問號會導致字頭排版不居中
		if ("".equals(chara) || "？".equals(chara)) ssb.append("□");
		else ssb.append(chara.replaceAll("[?/!！？見歸 ]", ""));
		
		// 錔字未確認，著灰色
		if (chara.contains("？") || chara.contains("?")) { // 該狀態下 大多以全角問號標記
			ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#B9BAA3")),
					0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		
		// 錔字被併到其它字，著灰色
		if (chara.contains("見") || chara.contains("歸")) {
			ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#3D3B4F")),
					0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		
		return ssb;
	}
	
	/**
	 * 向屏幕打印統一碼、俗字
	 * 對應字項 Layout 左中部分
	 *
	 * 俗字的顯示位置可能需要調整
	 *
	 * @return Spanned 格式的富文本，可直接調用 setText() 顯示
	 */
	public Spanned printUnicode() {
		String chara = key2val.get(HeaderInfo.COLUMN_NAME_CHARACTER)
				.replaceAll("[?/!？！見歸 ]", "");
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		String unicode = StringUtil.charaToUnicode(chara);
		ssb.append(unicode);
		
//      獲取到的萬國碼碼位在保留區
//		if (unicode.startsWith("U+E") || (unicode.startsWith("U+F")&&unicode.charAt(3)<'9')) {
//			ssb.setSpan(new StrikethroughSpan(),
//					0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//		}
		
		// 打印俗字
		String adaptedChara = key2val.get(HeaderInfo.COLUMN_NAME_CONVENTIONAL);
		if (!"".equals(adaptedChara)) {
			if (!"".equals(unicode)) ssb.append("\n");
			ssb.append("(").append(adaptedChara).append(")");
		}
		return ssb;
	}
	
	/**
	 * 向屏幕打印讀音（綜合音）
	 * 對應字項 Layout 左下部分
	 *
	 * @return Spanned 格式的富文本，可直接調用 setText() 顯示
	 */
	public Spanned printPronunciation() {
		String pron = key2val.get(HeaderInfo.COLUMN_NAME_PRONUNCIATION).replaceAll("[!！]", "");
		SpannableStringBuilder ssb = new SpannableStringBuilder(pron);
		if (pron.contains("?")) {
			ssb.setSpan(new StyleSpan(Typeface.ITALIC),
					0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		
		return ssb;
	}
}
