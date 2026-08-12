package cc.ecisr.jyutdict.struct;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import cc.ecisr.jyutdict.utils.ColorUtil;

/**
 * FjbHeaderInfo 類，用於儲存泛粵字表的表頭
 * 同時以此確定泛粵表各地輸出的順序
 */
public final class FjbHeaderInfo {
	public static boolean isLoaded = false;
	
	public static final String COLUMN_NAME_CHARACTER = "繁";
	public static final String COLUMN_NAME_PRONUNCIATION = "綜";
	public static final String COLUMN_NAME_RETRIEVAL = "檢";
	public static final String COLUMN_NAME_MEANING = "釋義";
	public static final String COLUMN_NAME_CONVENTIONAL = "俗/常";
	public static final String COLUMN_NAME_NOTE = "註";
	public static final String COLUMN_NAME_CLASS_MAJOR = "大類";
	public static final String COLUMN_NAME_CLASS_SECONDARY = "中類";
	public static final String COLUMN_NAME_CLASS_MINOR = "小類";
	public static final String COLUMN_NAME_EXAMPLE = "例";
	public static final String COLUMN_NAME_IDS = "IDS";
	public static final String COLUMN_NAME_GRAMMAR_MARKER = "語法";
	public static final String COLUMN_NAME_BOOKS_CHARA = "錔";
	public static final String COLUMN_NAME_BOOKS_PRON = "音";
	public static final String COLUMN_NAME_BOOKS_MEANING = "義";
	public static final String COLUMN_NAME_CELL_NOTE = "附";
	
	private static int infoLength = 0; // 表头总列数
	private static final Vector<String> cityList = new Vector<>();  // 地方點列表，cityList[0]=>"穗" etc
	private static final Vector<String> foreignList = new Vector<>();  // 域外音列表，foreignList[0]=>"官" etc
	private static final Vector<String> fullList = new Vector<>();  // 所有列匯總，fullList[0]=>"繁" etc
	
	private static final Map<String, Boolean> isCity = new HashMap<>();
	private static final Map<String, Integer> colNumber = new HashMap<>();  // colNumber.get("穗")=>0 etc
	private static final Map<String, ArrayList<String>> cityColors = new HashMap<>();
	private static final Map<String, ArrayList<String>> columnColors = new HashMap<>();
	private static final Map<String, String[]> fullName = new HashMap<>(); // fullName.get("穗")=>["广州",""] etc
	
	private static final Map<String, ArrayList<String>> foreignColors = new HashMap<>();
	
	private static int meaningsColNum = 0; // 釋義所在列序號
	private static final int[] classificationColNum = new int[3]; // 詞場所在列序號
	private static int commonlyUsedCharaColNum = 0; // 俗字所在列序號
	private static int noteColNum = 0; // 註所在列序號
	private static int authorizedCharaColNum = 0; // 錔字所在列序號
	private static int authorizedPronColNum = 0; // 綜合音所在列序號
	private static int exampleColNum = 0; // 例詞所在列序號
	private static int idsColNum = 0; // IDS所在列序號
	private static int grammarMarkerColNum = 0; // 語法標記所在列序號
	private static int cellNoteColNum = 0; // 單元格備註所在列序號
	
	
	/**
	 * 構造函數
	 * 在服務器返回表頭到手機時調用
	 *
	 * @param headerInfo JSONArray 類，儲存的是整個表頭及與之相關的詳細信息
	 *                   v1.0 格式：[{"index":1,"col":"綜","kind":0,"fullname":"綜合音","color":"#F1C232"},
	 *                   {"index":7,"col":"台大江","kind":1,"fullname":"台山","sub":"大江","color":"#873279"}, ...]
	 */
	public static void load(JSONArray headerInfo) {
		reset();
		infoLength = headerInfo.length();
		for (int i = 0; i < infoLength; i++) {
			JSONObject headerEntry = headerInfo.optJSONObject(i);
			if (headerEntry == null) continue;

			int id = headerEntry.optInt("index", -1);
			int cityKind = headerEntry.optInt("kind", 0);
			String colName = headerEntry.optString("col", "");
			String resolvedFullName = headerEntry.optString("fullname", colName);
			String subCity = headerEntry.optString("sub", "");
			if (id < 0 || colName.isEmpty()) continue;

			ArrayList<String> colors = ColorUtil.parseLocationColors(
					headerEntry.opt("colors"),
					headerEntry.opt("color")
			);
			if (colors.isEmpty()) colors.add(ColorUtil.DEFAULT_LOCATION_COLOR);

			// 完成單條資料的容錯解析後才寫入靜態表，避免半初始化。
			colNumber.put(colName, id);
			fullList.add(colName);
			columnColors.put(colName, colors);
			switch (cityKind) {
				case 2: // 域外音
					foreignList.add(colName);
					fullName.put(colName, new String[]{resolvedFullName, ""});
					foreignColors.put(colName, colors);
					break;
				case 1: // 地方音
					cityList.add(colName);
					isCity.put(colName, true);
					fullName.put(colName, new String[]{resolvedFullName, subCity});
					cityColors.put(colName, colors);
					break;
				case 0: // 其它表頭信息
				default:
					isCity.put(colName, false);
					fullName.put(colName, new String[]{resolvedFullName, ""});
					break;
			}

			switch (colName) {
				case COLUMN_NAME_CHARACTER:
					authorizedCharaColNum = id; break;
				case COLUMN_NAME_PRONUNCIATION:
					authorizedPronColNum = id; break;
				case COLUMN_NAME_MEANING:
					meaningsColNum = id; break;
				case COLUMN_NAME_CLASS_MAJOR:
					classificationColNum[0] = id; break;
				case COLUMN_NAME_CLASS_SECONDARY:
					classificationColNum[1] = id; break;
				case COLUMN_NAME_CLASS_MINOR:
					classificationColNum[2] = id; break;
				case COLUMN_NAME_CONVENTIONAL:
					commonlyUsedCharaColNum = id; break;
				case COLUMN_NAME_NOTE:
					noteColNum = id; break;
				case COLUMN_NAME_EXAMPLE:
					exampleColNum = id; break;
				case COLUMN_NAME_IDS:
					idsColNum = id; break;
				case COLUMN_NAME_GRAMMAR_MARKER:
					grammarMarkerColNum = id; break;
				case COLUMN_NAME_CELL_NOTE:
					cellNoteColNum = id; break;
				default: break;
			}
		}
		isLoaded = true;
	}

	public static void reset() {
		isLoaded = false;
		infoLength = 0;
		cityList.clear();
		foreignList.clear();
		fullList.clear();
		isCity.clear();
		colNumber.clear();
		cityColors.clear();
		columnColors.clear();
		foreignColors.clear();
		fullName.clear();
		meaningsColNum = 0;
		classificationColNum[0] = 0;
		classificationColNum[1] = 0;
		classificationColNum[2] = 0;
		commonlyUsedCharaColNum = 0;
		noteColNum = 0;
		authorizedCharaColNum = 0;
		authorizedPronColNum = 0;
		exampleColNum = 0;
		idsColNum = 0;
		grammarMarkerColNum = 0;
		cellNoteColNum = 0;
	}
	
	static int getInfoLength() {
		return infoLength;
	}
	
	static int getMeaningsColNum() {
		return meaningsColNum;
	}
	static int getCommonlyUsedCharaColNum() {
		return commonlyUsedCharaColNum;
	}
	static int getNoteColNum() {
		return noteColNum;
	}
	static int getAuthorizedCharaColNum() {
		return authorizedCharaColNum;
	}
	static int getAuthorizedPronColNum() {
		return authorizedPronColNum;
	}
	static int[] getClassificationColNum() {
		return classificationColNum;
	}
	static int getCityCount() {
		return cityList.size();
	}
	
	static String getColNameByNumber(int col) {
		return fullList.elementAt(col);
	}
	static public String getCityNameByNumber(int index) {
		return cityList.get(index);
	}
	static ArrayList<String> getCityColors(String colName) {
		return copyColors(cityColors.get(colName));
	}
	static ArrayList<String> getForeignColors(String colName) {
		return copyColors(foreignColors.get(colName));
	}
	public static ArrayList<String> getColumnColors(String colName) {
		return copyColors(columnColors.get(colName));
	}
	private static ArrayList<String> copyColors(ArrayList<String> colors) {
		if (colors == null || colors.isEmpty()) {
			return new ArrayList<>(java.util.Collections.singletonList(
					ColorUtil.DEFAULT_LOCATION_COLOR));
		}
		return new ArrayList<>(colors);
	}
	static String[] getFullName(String colName) {
		return fullName.get(colName);
	}
	
	/**
	 * @return 地方音城市全名列表
	 * 如：[..., "香港", "廣州" , ...]
	 */
	static public String[] getCityList() {
		String[] cities = new String[cityList.size()];
		int order = 0;
		for (String s : cityList) {
			String[] cityFullName = fullName.get(s);
			cities[order] = (cityFullName!=null ? cityFullName[0]+cityFullName[1] : "");
			order++;
		}
		
		return cities;
	}
	
	/**
	 * @return 地方音城市縮寫列表
	 * 如：[..., "港", "穗" , ...]
 	 */
	static String[] getCityListInShort() {
		return cityList.toArray(new String[]{});
	}
	
	/**
	 * @return 域外音城市縮寫列表
	 * 如：[..., "官", "吳" , ...]
	 */
	static String[] getForeignListInShort() {
		return foreignList.toArray(new String[]{});
	}
	
	/**
	 * 查詢某個縮寫是否一個代表地方音的城市名
	 *
	 * @param colName 表頭中某一列的縮寫名稱，如"綜"
	 * @return {@code true} 當 {@code colName} 是一個城市名縮寫時
	 */
	static Boolean isNameACity(String colName) {
		return isCity.get(colName);
	}

	/** 與網頁端泛粵字表相同的地方讀音覆蓋度排序分數。 */
	public static int densityScore(JSONObject row) {
		int score = 0;
		for (String key : cityList) {
			String value = row.isNull(key) ? "" : row.optString(key, "").trim();
			if (!value.isEmpty() && !"_".equals(value) && !"?".equals(value)) {
				score++;
			}
		}
		return score;
	}
}
