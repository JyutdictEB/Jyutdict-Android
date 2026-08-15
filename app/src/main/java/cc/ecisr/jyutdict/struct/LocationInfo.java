package cc.ecisr.jyutdict.struct;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cc.ecisr.jyutdict.utils.ColorUtil;

/**
 * 管理通用字表的地點列表（從 /api/v1.0/detail?chara= 獲取）
 * 用於：
 * 1. 在通用字表查字結果中，將 id 映射為地點名、顏色等
 * 2. 在檢音結果中，將 __id 映射為地點名
 * 3. 構建地點篩選對話框
 */
public class LocationInfo {
    public static boolean isLoaded = false;

    // id -> Location 的映射
    private static final Map<Integer, Location> locationMap = new HashMap<>();
    // 按順序排列的地點列表（用於篩選對話框）
    private static final ArrayList<Location> locationList = new ArrayList<>();

    public static class Location {
        public int id;
        public String first;   // 片區，如 "四邑片"
        public String second;  // 市，如 "鶴山"
        public String third;   // 管區，如 "沙坪"
        public String detailedName;
        public String sheetAuthor;
        public String sheetStatistic;
        public String sheetInfo;
        public boolean hasPhonology;
        public final ArrayList<String> colors = new ArrayList<>();
        public double longitude;
        public double latitude;

        /** 返回用於顯示的城市名（市+管區），如 "鶴山沙坪" */
        public String displayName() {
            return second + third;
        }

        public String displayTitle() {
            return detailedName == null || detailedName.isEmpty()
                    ? displayName()
                    : detailedName;
        }

        public String hierarchy() {
            ArrayList<String> levels = new ArrayList<>();
            if (second != null && !second.isEmpty()) levels.add(second);
            if (third != null && !third.isEmpty()) levels.add(third);
            return android.text.TextUtils.join(" · ", levels);
        }

        public String primaryColor() {
            return ColorUtil.primaryLocationColor(colors);
        }
    }

    /**
     * 從 API 返回的 JSON 數組加載地點列表
     *
     * @param jsonArray 格式：[{"id":1,"longitude":0,"latitude":0,
     *                  "first":"歷史音","second":"1884新甯","third":"甌城","color":"#000000"}, ...]
     */
    public static void load(JSONArray jsonArray) {
        isLoaded = false;
        locationMap.clear();
        locationList.clear();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject obj = jsonArray.getJSONObject(i);
                Location loc = new Location();
                loc.id = obj.getInt("id");
                loc.first = obj.optString("first", "");
                loc.second = obj.optString("second", "");
                loc.third = obj.optString("third", "");
                loc.detailedName = nullableString(obj, "detailed_name");
                loc.sheetAuthor = nullableString(obj, "sheet_author");
                loc.sheetStatistic = nullableString(obj, "sheet_statistic");
                loc.sheetInfo = nullableString(obj, "sheet_info");
                loc.hasPhonology = obj.optInt("has_phonology", 0) != 0
                        || obj.optBoolean("has_phonology", false);
                loc.colors.addAll(ColorUtil.parseLocationColors(
                        obj.opt("colors"),
                        obj.opt("color")
                ));
                if (loc.colors.isEmpty()) {
                    loc.colors.add(ColorUtil.DEFAULT_LOCATION_COLOR);
                }
                loc.longitude = obj.optDouble("longitude", 0);
                loc.latitude = obj.optDouble("latitude", 0);
                locationMap.put(loc.id, loc);
                locationList.add(loc);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        isLoaded = true;
    }

    private static String nullableString(JSONObject object, String key) {
        return object.isNull(key) ? "" : object.optString(key, "");
    }

    public static Location get(int id) {
        return locationMap.get(id);
    }

    /** 兼容通用字表名稱、泛粵字表名稱經伺服器別名解析後的抽象地名。 */
    public static Location findByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String target = name.trim();
        for (Location location : locationList) {
            if (target.equals(location.displayName())
                    || target.equals(location.displayTitle())
                    || target.equals(location.detailedName)) {
                return location;
            }
        }
        return null;
    }

    public static ArrayList<Location> getAll() {
        return locationList;
    }

}
