package cc.ecisr.jyutdict.struct;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cc.ecisr.jyutdict.utils.ColorUtil;

/** Parsed sheet columns and their display metadata. */
public final class FjbHeaderInfo {
    public static boolean isLoaded;

    public static final String COLUMN_NAME_CHARACTER = "繁";
    public static final String COLUMN_NAME_PRONUNCIATION = "綜";
    public static final String COLUMN_NAME_RETRIEVAL = "檢";
    public static final String COLUMN_NAME_MEANING = "釋義";
    public static final String COLUMN_NAME_CONVENTIONAL = "俗/常";
    public static final String COLUMN_NAME_CLASS_MAJOR = "大類";
    public static final String COLUMN_NAME_CLASS_SECONDARY = "中類";
    public static final String COLUMN_NAME_CLASS_MINOR = "小類";
    public static final String COLUMN_NAME_IDS = "IDS";
    public static final String COLUMN_NAME_GRAMMAR_MARKER = "語法";
    public static final String COLUMN_NAME_BOOKS_CHARA = "錔";
    public static final String COLUMN_NAME_BOOKS_PRON = "音";
    public static final String COLUMN_NAME_BOOKS_MEANING = "義";
    public static final String COLUMN_NAME_CELL_NOTE = "附";

    private static final ArrayList<String> cityList = new ArrayList<>();
    private static final ArrayList<String> foreignList = new ArrayList<>();
    private static final Map<String, ArrayList<String>> cityColors = new HashMap<>();
    private static final Map<String, ArrayList<String>> foreignColors = new HashMap<>();
    private static final Map<String, ArrayList<String>> columnColors = new HashMap<>();
    private static final Map<String, String[]> fullNames = new HashMap<>();

    private FjbHeaderInfo() {}

    public static void load(JSONArray header) {
        reset();
        for (int index = 0; index < header.length(); index++) {
            JSONObject item = header.optJSONObject(index);
            if (item == null || item.optInt("index", -1) < 0) continue;
            String column = item.optString("col", "");
            if (column.isEmpty()) continue;
            String name = item.optString("fullname", column);
            String subName = item.optString("sub", "");
            ArrayList<String> colors = ColorUtil.parseLocationColors(
                    item.opt("colors"), item.opt("color"));
            if (colors.isEmpty()) colors.add(ColorUtil.DEFAULT_LOCATION_COLOR);
            columnColors.put(column, colors);

            switch (item.optInt("kind", 0)) {
                case 2:
                    foreignList.add(column);
                    foreignColors.put(column, colors);
                    fullNames.put(column, new String[]{name, ""});
                    break;
                case 1:
                    cityList.add(column);
                    cityColors.put(column, colors);
                    fullNames.put(column, new String[]{name, subName});
                    break;
                default:
                    fullNames.put(column, new String[]{name, ""});
                    break;
            }
        }
        isLoaded = true;
    }

    public static void reset() {
        isLoaded = false;
        cityList.clear();
        foreignList.clear();
        cityColors.clear();
        foreignColors.clear();
        columnColors.clear();
        fullNames.clear();
    }

    public static String getCityNameByNumber(int index) {
        return cityList.get(index);
    }

    static ArrayList<String> getCityColors(String column) {
        return copyColors(cityColors.get(column));
    }

    static ArrayList<String> getForeignColors(String column) {
        return copyColors(foreignColors.get(column));
    }

    public static ArrayList<String> getColumnColors(String column) {
        return copyColors(columnColors.get(column));
    }

    private static ArrayList<String> copyColors(ArrayList<String> colors) {
        return colors == null || colors.isEmpty()
                ? new ArrayList<>(Collections.singletonList(ColorUtil.DEFAULT_LOCATION_COLOR))
                : new ArrayList<>(colors);
    }

    static String[] getFullName(String column) {
        return fullNames.get(column);
    }

    public static String[] getCityList() {
        String[] result = new String[cityList.size()];
        for (int index = 0; index < result.length; index++) {
            String[] name = fullNames.get(cityList.get(index));
            result[index] = name == null ? "" : name[0] + name[1];
        }
        return result;
    }

    static String[] getCityListInShort() {
        return cityList.toArray(new String[0]);
    }

    static String[] getForeignListInShort() {
        return foreignList.toArray(new String[0]);
    }

    /** Same location-pronunciation coverage score used by the web client. */
    public static int densityScore(JSONObject row) {
        int score = 0;
        for (String key : cityList) {
            String value = row.isNull(key) ? "" : row.optString(key, "").trim();
            if (!value.isEmpty() && !"_".equals(value) && !"?".equals(value)) score++;
        }
        return score;
    }
}
