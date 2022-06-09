package cc.ecisr.jyutdict.struct;

import static cc.ecisr.jyutdict.utils.EnumConst.*;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import cc.ecisr.jyutdict.utils.ColorUtil;

public class GeneralCharacterManager {
    public enum ColoringMode { NoColoring, InnerColoring, InterColoring}
    ArrayList<GeneralCharacter> charas = new ArrayList<>();
    ArrayList<String> charaHead = new ArrayList<>();
    HashMap<String, ArrayList<String>> div2city = new HashMap<>();
    HashMap<String, String> city2color = new HashMap<>();
    public static ArrayList<String> cityList = new ArrayList<>(Arrays.asList("尺牘分韻", "英華"));
    public static HashSet<String> cityFilter = new HashSet<>(); // static 是因為 MainActivity 要調用
    EntrySetting settings;
    int colorCount = 0;

    public void parse(String raw, EntrySetting settings) {
        JSONArray charasJSON;
        try {
            charasJSON = new JSONArray(raw);
        } catch (JSONException e) { return; }
        //Gson gson = new Gson();
        //Type type = new TypeToken<GeneralCharacterBean[]>(){}.getType();
        //GeneralCharacterBean[] charasDuplicated = gson.fromJson(raw, type);
        for (int i=0; i<charasJSON.length(); i++) {
            JSONObject chara = charasJSON.optJSONObject(i);
            if (charaHead.contains(chara.optString("字"))) { continue; }
            charas.add(new GeneralCharacter(chara));
            charaHead.add(chara.optString("字"));
        }
        this.settings = settings;
    }

    public void retrieveInfo() {
        for (int index=0; index<charas.size(); index++) {
            GeneralCharacter chara = charas.get(index);
            for (GeneralCharacter.SingleLoc i: chara.areas) {
                if (!city2color.containsKey(i.city)) { city2color.put(i.city, i.color); }
                if (!cityList.contains(i.city)) {
                    cityList.add(i.city);
                }

                ArrayList<String> cities = div2city.get(i.division);
                if (cities == null) {
                    div2city.put(i.division, new ArrayList<>(Collections.singletonList(i.city)));
                } else if (!cities.contains(i.city)) {
                    cities.add(i.city);
                }
            }
        }
    }

    public void coloring(int displayMode) {
        ColoringMode coloringMode;
        boolean ini = (displayMode & DISPLAY_CHECKING_INI) != 0;
        boolean fin = (displayMode & DISPLAY_CHECKING_FIN) != 0;
        boolean ton = (displayMode & DISPLAY_CHECKING_TON) != 0;
        if (!ini && !fin && !ton) {
            coloringMode = ColoringMode.NoColoring;
        } else {
            coloringMode = (displayMode & DISPLAY_CHECKING_IS_INNER)!=0 ? ColoringMode.InnerColoring : ColoringMode.InterColoring;
        }
        colorCount = 0;
        int presentColorAssigning;
        HashMap<String, Integer> coloringMarker;
        ArrayList<ArrayList<GeneralCharacter.SingleLoc.SinglePron>> prons, prons_;
        switch (coloringMode) {
            case InterColoring: // 不知到怎麼合併
                for (String city: cityList) {
                    if (cityFilter.contains(city)) { continue; }
                    presentColorAssigning = 0;
                    coloringMarker = new HashMap<>();
                    for (int i=0; i<charas.size()-1; i++) {
                        prons = charas.get(i).area(city).prons;
                        for (ArrayList<GeneralCharacter.SingleLoc.SinglePron> ii: prons) for (GeneralCharacter.SingleLoc.SinglePron ij: ii) {
                            String ijPron = ij.jpp(ini, fin, ton);
                            for (int j=i+1; j<charas.size(); j++) {
                                prons_ = charas.get(j).area(city).prons;
                                for (ArrayList<GeneralCharacter.SingleLoc.SinglePron> ji: prons_) for (GeneralCharacter.SingleLoc.SinglePron jj: ji) {
                                    if (ijPron.equals(jj.jpp(ini, fin, ton))) {
                                        if (!coloringMarker.containsKey(ijPron)) {
                                            presentColorAssigning ++;
                                            coloringMarker.put(ijPron, presentColorAssigning);
                                        }
                                        ij.coloring = jj.coloring = coloringMarker.get(ijPron);
                                    }
                                }
                            }
                        }
                    }
                    if (presentColorAssigning>colorCount) colorCount = presentColorAssigning;
                }
                break;
            case InnerColoring:
                for (GeneralCharacter chara: charas) {
                    presentColorAssigning = 0;
                    coloringMarker = new HashMap<>();
                    for (int i=0; i<chara.areas.size()-1; i++) {
                        prons = chara.areas.get(i).prons;
                        if (cityFilter.contains(chara.areas.get(i).city)) { continue; }
                        for (ArrayList<GeneralCharacter.SingleLoc.SinglePron> ii: prons) for (GeneralCharacter.SingleLoc.SinglePron ij: ii) {
                            String ijPron = ij.jpp(ini, fin, ton);
                            //if (coloringMarker.containsKey(ijPron)) { ij.coloring = coloringMarker.get(ijPron);continue; }
                            for (int j=i+1; j<chara.areas.size(); j++) {
                                prons_ = chara.areas.get(j).prons;
                                if (cityFilter.contains(chara.areas.get(j).city)) { continue; }
                                for (ArrayList<GeneralCharacter.SingleLoc.SinglePron> ji: prons_) for (GeneralCharacter.SingleLoc.SinglePron jj: ji) {
                                    if (ijPron.equals(jj.jpp(ini, fin, ton))) {
                                        if (!coloringMarker.containsKey(ijPron)) {
                                            presentColorAssigning ++;
                                            coloringMarker.put(ijPron, presentColorAssigning);
                                        }
                                        ij.coloring = jj.coloring = coloringMarker.get(ijPron);
                                    }
                                }
                            }
                        }
                    }
                    if (presentColorAssigning>colorCount) colorCount = presentColorAssigning;
                }
                break;
            case NoColoring:
            default:
                break;
        }
    }


    public Spanned[] printChara(int index) {
        if (index >= charas.size()) return new Spanned[5];
        GeneralCharacter chara = charas.get(index);
        String charaHead = chara.head;

        SpannableStringBuilder contentCharaInfo = new SpannableStringBuilder("");
        for (int i = 0; i < chara.books.kwangun.size(); i++) {
            if (i!=0) contentCharaInfo.append("\n");
            contentCharaInfo.append(chara.books.kwangun.get(i));
        }
        contentCharaInfo.setSpan(new RelativeSizeSpan(0.8f),
                0, contentCharaInfo.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannableStringBuilder contentWanshyu = new SpannableStringBuilder("");
        if (!cityFilter.contains("尺牘分韻")) { /// Why hardcode?! never mind
            for (int i = 0; i<chara.books.fanwan.size(); i++) {
                if (i==0) { contentWanshyu.append("[尺牘分韻] "); }
                if (i>0) { contentWanshyu.append(" | "); }
                contentWanshyu.append(chara.books.fanwan.get(i));
            }
        }
        if (!cityFilter.contains("英華")) {
            if (contentWanshyu.length()>0) { contentWanshyu.append("\n"); }
            for (int i = 0; i<chara.books.jingwaa.size(); i++) {
                if (i==0) { contentWanshyu.append("[英華] "); }
                if (i>0) { contentWanshyu.append(" | "); }
                contentWanshyu.append(chara.books.jingwaa.get(i));
            }
        }

        SpannableStringBuilder contentLoc = new SpannableStringBuilder();
        int presentBeginPosition, presentEndPosition, textColor;
        double areaColoringDarkenRatio = settings.isUsingNightMode ?
                2 - settings.areaColoringDarkenRatio : // 將顏色調亮
                settings.areaColoringDarkenRatio;  // 將顏色調暗;
        for (GeneralCharacter.SingleLoc loc: chara.areas) {
            if (cityFilter.contains(loc.city)) continue;
            if (contentLoc.length()>0) contentLoc.append("\n");

            presentBeginPosition = contentLoc.length();
            contentLoc.append(loc.city.replace("'", "")).append("　");
            presentEndPosition = contentLoc.length();
            if (settings.isAreaColoring) {
                textColor = ColorUtil.darken(loc.color, areaColoringDarkenRatio);
                contentLoc.setSpan(new ForegroundColorSpan(textColor),
                        presentBeginPosition, presentEndPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            for (int i=0; i<loc.prons.size(); i++) {
                if (i>0) {
                    if (!"".equals(loc.notes.get(i-1))) {
                        contentLoc.append("\n　　　　　");
                    } else {
                        contentLoc.append(" · ");
                    }
                } else if (loc.city.length()==2) {
                    contentLoc.append("　　");
                }

                ArrayList<GeneralCharacter.SingleLoc.SinglePron> singleLoc = loc.prons.get(i);
                for (int j=0; j<loc.prons.get(i).size(); j++) {
                    contentLoc.append(j > 0 ? "=" : "");
                    presentBeginPosition = contentLoc.length();
                    contentLoc.append(singleLoc.get(j).syllable());
                    presentEndPosition = contentLoc.length();
                    if (singleLoc.get(j).coloring!=0) {
                        textColor = ColorUtil.darken(ColorUtil.ithColorInHsv(singleLoc.get(j).coloring, colorCount),
                                areaColoringDarkenRatio
                        );
                        contentLoc.setSpan(new ForegroundColorSpan(textColor),
                                presentBeginPosition, presentEndPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                }
                if (settings.isPresentIpa && !"".equals(loc.prons.get(i).get(0).ipa) && !loc.prons.get(i).get(0).ipa.contains("(")) {
                    contentLoc.append(" /");
                    for (int j = 0; j < loc.prons.get(i).size(); j++) {
                        contentLoc.append(j > 0 ? "=" : "").append(singleLoc.get(j).ipa);
                    }
                    contentLoc.append("/ ");
                }

                if (!"".equals(loc.notes.get(i))) {
                    presentBeginPosition = contentLoc.length();
                    contentLoc.append(" ").append(loc.notes.get(i));
                    presentEndPosition = contentLoc.length();
                    contentLoc.setSpan(new RelativeSizeSpan(0.75f),
                            presentBeginPosition, presentEndPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        return new Spanned[]{
                new SpannableString(charaHead),
                new SpannableString(""),
                contentCharaInfo, contentWanshyu, contentLoc
        };
    }


    public int length() {
        return charas.size();
    }


}
