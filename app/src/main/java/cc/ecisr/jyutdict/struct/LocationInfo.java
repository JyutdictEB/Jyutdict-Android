package cc.ecisr.jyutdict.struct;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
        public String color;   // 顏色，如 "#e57d99"
        public double longitude;
        public double latitude;

        /** 返回用於顯示的城市名（市+管區），如 "鶴山沙坪" */
        public String displayName() {
            return second + third;
        }
    }

    /**
     * 從 API 返回的 JSON 數組加載地點列表
     *
     * @param jsonArray 格式：[{"id":1,"longitude":0,"latitude":0,
     *                  "first":"歷史音","second":"1884新甯","third":"甌城","color":"#000000"}, ...]
     */
    public static void load(JSONArray jsonArray) {
        if (isLoaded) return;
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
                loc.color = obj.optString("color", "#888888");
                loc.longitude = obj.optDouble("longitude", 0);
                loc.latitude = obj.optDouble("latitude", 0);
                // 歷史音（色="#000000"）用灰色替代
                if ("#000000".equals(loc.color)) {
                    loc.color = "#888888";
                }
                locationMap.put(loc.id, loc);
                locationList.add(loc);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        isLoaded = true;
    }

    public static Location get(int id) {
        return locationMap.get(id);
    }

    public static ArrayList<Location> getAll() {
        return locationList;
    }

    /** 返回所有地點的顯示名稱列表（用於篩選對話框） */
    public static ArrayList<String> getDisplayNames() {
        ArrayList<String> names = new ArrayList<>();
        for (Location loc : locationList) {
            names.add(loc.displayName());
        }
        return names;
    }

    public static void reset() {
        isLoaded = false;
        locationMap.clear();
        locationList.clear();
    }
}
