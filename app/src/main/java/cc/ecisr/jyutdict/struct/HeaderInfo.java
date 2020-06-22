package cc.ecisr.jyutdict.struct;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

public class HeaderInfo {
	
	public static final String COLUMN_NAME_CHARACTER = "繁";
	public static final String COLUMN_NAME_PRONUNCIATION = "綜";
	public static final String COLUMN_NAME_MEANING = "釋義";
	public static final String COLUMN_NAME_CONVENTIONAL = "俗/常";
	public static final String COLUMN_NAME_NOTE = "註";
	public static final String COLUMN_NAME_CLASS_MAJOR = "大類";
	public static final String COLUMN_NAME_CLASS_SECONDARY = "中類";
	public static final String COLUMN_NAME_CLASS_MINOR = "小類";
	
	
	
	private static int infoLength = 0; // 表头总列数
	private static Vector<String> cityList = new Vector<>();  // cityList[0]=>"穗" etc
	private static Vector<String> fullList = new Vector<>();  // cityList[0]=>"穗" etc
	private static Map<String, Boolean> isCity = new HashMap<>();
	private static Map<String, Integer> colNumber = new HashMap<>();  // colNumber.get("穗")=>0 etc
	private static Map<String, String> cityColor = new HashMap<>(); // cityColor.get("穗")=>"#FFFFFF" etc
	private static Map<String, String[]> fullName = new HashMap<>(); // fullName.get("穗")=>["广州",""] etc
	
	private static int meaningsColNum = 0;
	private static int[] classificationColNum = new int[3];
	private static int commonlyUsedCharaColNum = 0;
	private static int noteColNum = 0;
	private static int authorizedCharaColNum = 0;
	private static int authorizedPronColNum = 0;
	
	public HeaderInfo(JSONArray headerInfo) {
		infoLength = headerInfo.length();
		JSONObject headerEntry;
		for (int i=0; i<infoLength; i++) {
			try {
				headerEntry = headerInfo.getJSONObject(i);
				int id = headerEntry.getInt("id");
				int isCity = headerEntry.getInt("is_city");
				String colName = headerEntry.getString("col");
				
				colNumber.put(colName, id);
				fullList.add(colName);
				if (isCity==1) {
					cityList.add(colName);
					
					HeaderInfo.isCity.put(colName, true);
					String city = headerEntry.getString("city");
					String subCity = headerEntry.getString("sub");
					fullName.put(colName, new String[]{city, subCity});
					String color = headerEntry.getString("color");
					cityColor.put(colName, color);
				} else {
					HeaderInfo.isCity.put(colName, false);
					String fullname = headerEntry.getString("fullname");
					HeaderInfo.fullName.put(colName, new String[]{fullname, ""});
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
//	static Integer getColNumber(String colName) {
//		return colNumber.get(colName);
//	}
	static String[] getFullName(String colName) {
		return fullName.get(colName);
	}
	
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
	static public String[] getCityListInShort() {
		return cityList.toArray(new String[]{});
	}
	
	static public Boolean isNameACity(String colName) {
		return isCity.get(colName);
	}
}
