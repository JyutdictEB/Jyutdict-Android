package cc.ecisr.jyutdict.struct;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cc.ecisr.jyutdict.utils.JyutpingUtil;

public class GeneralCharacter {
    private static final String TAG = "`GeneralCharacter";

    public String head;
    public ArrayList<SingleLoc> areas = new ArrayList<>();
    public HashMap<String, Integer> city2index = new HashMap<>();
    public Books books = new Books();

    public static class SingleLoc {
        public String division = "";
        public String city = "";

        public String color = "";
        public ArrayList<ArrayList<SinglePron>> prons = new ArrayList<>();
        public ArrayList<String> notes = new ArrayList<>();

        public static class SinglePron {
            public String[] jpp;
            public String ipa;
            public int coloring = 0;
            public SinglePron(String[] jpp, String ipa) { this.jpp=jpp; this.ipa=ipa; }
            public String syllable() { return jpp[0] + jpp[1] + jpp[2]; }
            public String jpp(boolean ini, boolean fin, boolean ton) {
                return "" + (ini?jpp[0]:"") + (fin?jpp[1]:"") + (ton?jpp[2]:"");
            }
        }
    }
    public static class Books {
        public ArrayList<String> kwangun = new ArrayList<>(1);
        public ArrayList<String> fanwan = new ArrayList<>(1);
        public ArrayList<String> jingwaa = new ArrayList<>(1);
    }


//    public GeneralCharacter(GeneralCharacterBean gcg) {
//        head = gcg.字;
//        for (List<GeneralCharacterBean.各地DTO> i: gcg.各地) {
//            SingleLoc loc = new SingleLoc();
//            loc.division = i.get(0).片區;
//            loc.city = i.get(0).市 + "'" + i.get(0).管區;
//            loc.color = i.get(0).色;
//            for (GeneralCharacterBean.各地DTO j: i) {
//                String[] jpps = (j.聲母+j.韻核+j.韻尾+j.聲調).split("=");
//                String[] ipas = j.ipa.split("=");
//                ArrayList<SingleLoc.SinglePron> prons = new ArrayList<>(jpps.length);
//                for (int k=0; k<jpps.length; k++ ) {
//                    prons.add(new SingleLoc.SinglePron(JyutpingUtil.splitJyutping(jpps[k]), ipas[k]));
//                }
//                loc.prons.add(prons);
//                loc.notes.add(j.註);
//            }
//            areas.add(loc);
//        }
//
//        for (List<GeneralCharacterBean.韻書DTO> i: gcg.韻書) { // 甚麼垃圾 API
//            for (GeneralCharacterBean.韻書DTO j: i) {
//                switch (j.書名) {
//                    case "廣韻":
//                        books.kwangun.add(j.聲母+j.攝+j.韻+j.等+j.呼+j.聲調+j.轉寫);
//                        break;
//                    case "分韻":
//                    case "英華":
//                    default:
//                        break;
//                }
//            }
//        }
//    }

    public GeneralCharacter(JSONObject charaJson) {
        head = charaJson.optString("字");
        JSONArray areasJson;
        if ((areasJson = charaJson.optJSONArray("各地"))==null) return;
        for (int ii=0; ii<areasJson.length(); ii++) {
            JSONArray i =  areasJson.optJSONArray(ii);
            SingleLoc loc = new SingleLoc();
            JSONObject j = i.optJSONObject(0);
            loc.division = j.optString("片區");
            loc.city = j.optString("市") + j.optString("管區");
            String rawColor = j.optString("色", "#888888");
            loc.color = !"#000000".equals(rawColor) ? rawColor : "#888888";

            for (int jj=0; jj<i.length(); jj++) {
                j = i.optJSONObject(jj);
                String[] jpps = (j.optString("聲母") + j.optString("韻核") +
                        j.optString("韻尾") + j.optString("聲調")
                ).split("=");
                String[] ipas = j.optString("IPA").split("=");
                ArrayList<SingleLoc.SinglePron> prons = new ArrayList<>(jpps.length);
                for (int k=0; k<jpps.length; k++ ) {
                    prons.add(new SingleLoc.SinglePron(JyutpingUtil.splitJyutping(jpps[k]), ipas[k]));
                }
                loc.prons.add(prons);
                loc.notes.add(j.optString("註"));
            }
            areas.add(loc);
            city2index.put(loc.city, ii);
        }

        if ((areasJson = charaJson.optJSONArray("韻書"))==null) return;
        for (int ii=0; ii<areasJson.length(); ii++) { // 甚麼垃圾 API
            JSONArray i =  areasJson.optJSONArray(ii);
            for (int jj=0; jj<i.length(); jj++) {
                JSONObject j = i.optJSONObject(jj);
                switch (j.optString("書名")) {
                    case "廣韻":
                        books.kwangun.add(
                                j.optString("聲母")+j.optString("攝")
                                +j.optString("韻")+j.optString("等")
                                +j.optString("呼")+j.optString("聲調")+j.optString("轉寫"));
                        break;
                    case "分韻":
                        books.fanwan.add(j.optString("韻部") + "-" + j.optString("小韻") + ", "
                                + j.optString("聲字") + j.optString("韻字") + j.optString("調類") + "("
                                + j.optString("聲母") + j.optString("韻核") + j.optString("韻尾")
                                + j.optString("聲調") + "), " + j.optString("義"));
                        break;
                    case "英華":
                        books.jingwaa.add(j.optString("音") + "(" + j.optString("聲母")
                                + j.optString("韻核") + j.optString("韻尾") + j.optString("聲調") + ")");
                        break;
                    default:
                        Log.w(TAG, "未知韻書");
                        break;
                }
            }
        }
    }

    public SingleLoc area(String name) {
        if (!city2index.containsKey(name)) return new SingleLoc();
        Integer index = city2index.get(name);
        return areas.get(index==null ? 0 : index);
    }
}
