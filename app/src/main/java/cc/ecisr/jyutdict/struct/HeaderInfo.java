package cc.ecisr.jyutdict.struct;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * HeaderInfo 類，用於儲存泛粵字表的表頭
 * 同時以此確定泛粵表各地輸出的順序
 */
public class HeaderInfo {
	
	public static final String COLUMN_NAME_CHARACTER = "繁";
	public static final String COLUMN_NAME_PRONUNCIATION = "綜";
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
	
	private static int infoLength = 0; // 表头总列数
	private static Vector<String> cityList = new Vector<>();  // 地方點列表，cityList[0]=>"穗" etc
	private static Vector<String> foreignList = new Vector<>();  // 域外音列表，foreignList[0]=>"官" etc
	private static Vector<String> fullList = new Vector<>();  // 所有列匯總，fullList[0]=>"繁" etc
	
	private static Map<String, Boolean> isCity = new HashMap<>();
	private static Map<String, Integer> colNumber = new HashMap<>();  // colNumber.get("穗")=>0 etc
	private static Map<String, String> cityColor = new HashMap<>(); // cityColor.get("穗")=>"#FFFFFF" etc
	private static Map<String, String[]> fullName = new HashMap<>(); // fullName.get("穗")=>["广州",""] etc
	
	private static Map<String, String> foreignColor = new HashMap<>(); // foreignColor.get("官")=>"#FFFFFF" etc
	
	private static int meaningsColNum = 0; // 釋義所在列序號
	private static int[] classificationColNum = new int[3]; // 詞場所在列序號
	private static int commonlyUsedCharaColNum = 0; // 俗字所在列序號
	private static int noteColNum = 0; // 註所在列序號
	private static int authorizedCharaColNum = 0; // 錔字所在列序號
	private static int authorizedPronColNum = 0; // 綜合音所在列序號
	private static int exampleColNum = 0; // 例詞所在列序號
	private static int idsColNum = 0; // IDS所在列序號
	private static int grammarMarkerColNum = 0; // 語法標記所在列序號
	
	
	/**
	 * 構造函數
	 * 在服務器返回表頭到手機時調用
	 *
	 * @param headerInfo JSONArray 類，儲存的是整個表頭及與之相關的詳細信息
	 *                   如：[{"id":0,"col":"繁","is_city":0,"fullname":"錔字"},
	 *                   {"id":1,"col":"穗","is_city":1,"city":"廣州","sub":"","color":"#FD9521"},
	 *                   {"id":2,"col":"客","is_city":2,"fullname":"客家話","color":"#79BFE4"} ...]
	 */
	public HeaderInfo(JSONArray headerInfo) {
		infoLength = headerInfo.length();
		for (int i=0; i<infoLength; i++) {
			try {
				JSONObject headerEntry = headerInfo.getJSONObject(i);
				int id = headerEntry.getInt("id");
				int isCity = headerEntry.getInt("is_city");
				String colName = headerEntry.getString("col");
				String color;
				colNumber.put(colName, id);
				fullList.add(colName);
				switch (isCity) {
					case 2: // 域外音
						foreignList.add(colName);
						String foreignName = headerEntry.getString("fullname");
						HeaderInfo.fullName.put(colName, new String[]{foreignName, ""});
						color = headerEntry.getString("color");
						foreignColor.put(colName, color);
						break;
					case 1: // 地方音
						cityList.add(colName);
						HeaderInfo.isCity.put(colName, true);
						String city = headerEntry.getString("city");
						String subCity = headerEntry.getString("sub");
						fullName.put(colName, new String[]{city, subCity});
						color = headerEntry.getString("color");
						cityColor.put(colName, color);
						break;
					case 0: // 其它表頭信息
					default:
						HeaderInfo.isCity.put(colName, false);
						String fullname = headerEntry.getString("fullname");
						HeaderInfo.fullName.put(colName, new String[]{fullname, ""});
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
					default:
						break;
				}
			} catch (JSONException e) {
				e.printStackTrace(); // 理应不会进入此处
			}
		}
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
	static String getCityColor(String colName) {
		return cityColor.get(colName);
	}
	static String getForeignColor(String colName) {
		return foreignColor.get(colName);
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
}
