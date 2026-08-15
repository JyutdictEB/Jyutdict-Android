package cc.ecisr.jyutdict.struct;

import static cc.ecisr.jyutdict.utils.EnumConst.*;

import android.content.res.Resources;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import cc.ecisr.jyutdict.utils.ColorUtil;
import cc.ecisr.jyutdict.widget.FixedWidthSpan;
import cc.ecisr.jyutdict.widget.BookLabelSpan;
import cc.ecisr.jyutdict.widget.LocationClickSpan;
import cc.ecisr.jyutdict.widget.LocationLabelSpan;
import cc.ecisr.jyutdict.widget.NoBreakCandidateSpan;

public class GeneralCharacterManager {
    public static final String FILTER_BOOK_FANWAN = "韻書 · 分韻";
    public static final String FILTER_BOOK_JINGWAA = "韻書 · 英華";
    ArrayList<GeneralCharacter> charas = new ArrayList<>();
    HashSet<String> characterHeads = new HashSet<>();
    public static ArrayList<String> cityList = new ArrayList<>();
    public static HashSet<String> cityFilter = new HashSet<>(); // static 是因為 MainActivity 要調用
    EntrySetting settings;
    int colorCount = 0;

    public void parse(String raw, EntrySetting settings) throws JSONException {
        JSONArray charasJSON = new JSONArray(raw);
        for (int i=0; i<charasJSON.length(); i++) {
            JSONObject chara = charasJSON.optJSONObject(i);
            if (!characterHeads.add(chara.optString("字"))) continue;
            charas.add(new GeneralCharacter(chara));
        }
        this.settings = settings;
    }

    public void retrieveInfo() {
        for (GeneralCharacter character : charas) {
            for (GeneralCharacter.SingleLoc location : character.areas) {
                if (!cityList.contains(location.city)) cityList.add(location.city);
            }
        }
    }

    public void coloring(int displayMode) {
        boolean ini = (displayMode & DISPLAY_CHECKING_INI) != 0;
        boolean fin = (displayMode & DISPLAY_CHECKING_FIN) != 0;
        boolean ton = (displayMode & DISPLAY_CHECKING_TON) != 0;
        colorCount = 0;
        if (!ini && !fin && !ton) return;
        boolean inner = (displayMode & DISPLAY_CHECKING_IS_INNER) != 0;
        if (inner) {
            for (GeneralCharacter character : charas) {
                ArrayList<ArrayList<ArrayList<GeneralCharacter.SingleLoc.SinglePron>>> groups =
                        new ArrayList<>();
                for (GeneralCharacter.SingleLoc location : character.areas) {
                    if (!cityFilter.contains(location.city)) groups.add(location.prons);
                }
                colorCount = Math.max(colorCount, colorMatches(groups, ini, fin, ton));
            }
        } else {
            for (String city : cityList) {
                if (cityFilter.contains(city)) continue;
                ArrayList<ArrayList<ArrayList<GeneralCharacter.SingleLoc.SinglePron>>> groups =
                        new ArrayList<>();
                for (GeneralCharacter character : charas) groups.add(character.area(city).prons);
                colorCount = Math.max(colorCount, colorMatches(groups, ini, fin, ton));
            }
        }
    }

    private int colorMatches(
            ArrayList<ArrayList<ArrayList<GeneralCharacter.SingleLoc.SinglePron>>> groups,
            boolean initial, boolean fin, boolean tone) {
        int nextColor = 0;
        HashMap<String, Integer> colors = new HashMap<>();
        for (int left = 0; left < groups.size() - 1; left++) {
            for (ArrayList<GeneralCharacter.SingleLoc.SinglePron> alternatives : groups.get(left)) {
                for (GeneralCharacter.SingleLoc.SinglePron pronunciation : alternatives) {
                    String key = pronunciation.jpp(initial, fin, tone);
                    for (int right = left + 1; right < groups.size(); right++) {
                        for (ArrayList<GeneralCharacter.SingleLoc.SinglePron> rightAlternatives
                                : groups.get(right)) {
                            for (GeneralCharacter.SingleLoc.SinglePron candidate : rightAlternatives) {
                                if (!key.equals(candidate.jpp(initial, fin, tone))) continue;
                                if (!colors.containsKey(key)) colors.put(key, ++nextColor);
                                pronunciation.coloring = candidate.coloring = colors.get(key);
                            }
                        }
                    }
                }
            }
        }
        return nextColor;
    }


    public Spanned[] printChara(int index) {
        if (index >= charas.size()) return new Spanned[5];
        GeneralCharacter chara = charas.get(index);
        String charaHead = chara.head;

        SpannableStringBuilder contentCharaInfo = new SpannableStringBuilder("");
        for (int i = 0; i < chara.books.kwangun.size(); i++) {
            if (i!=0) contentCharaInfo.append(" · ");
            contentCharaInfo.append(chara.books.kwangun.get(i));
        }
        if (contentCharaInfo.length() > 0) {
            contentCharaInfo.setSpan(new RelativeSizeSpan(0.8f),
                    0, contentCharaInfo.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        SpannableStringBuilder contentWanshyu = new SpannableStringBuilder("");
        if (!isBookFiltered(FILTER_BOOK_FANWAN) && !chara.books.fanwan.isEmpty()) {
            appendBook(contentWanshyu, "分韻", chara.books.fanwan);
        }
        if (!isBookFiltered(FILTER_BOOK_JINGWAA) && !chara.books.jingwaa.isEmpty()) {
            if (contentWanshyu.length() > 0) contentWanshyu.append("\n");
            appendBook(contentWanshyu, "英華", chara.books.jingwaa);
        }

        SpannableStringBuilder contentLoc = new SpannableStringBuilder();
        ArrayList<Integer> locationParagraphStarts = new ArrayList<>();
        ArrayList<Integer> locationParagraphEnds = new ArrayList<>();
        ArrayList<Integer> locationParagraphIndents = new ArrayList<>();
        int presentBeginPosition, presentEndPosition, textColor;
        double areaColoringDarkenRatio = settings.isUsingNightMode ?
                2 - settings.areaColoringDarkenRatio : // 將顏色調亮
                settings.areaColoringDarkenRatio;  // 將顏色調暗;
        for (GeneralCharacter.SingleLoc loc: chara.areas) {
            if (cityFilter.contains(loc.city)) continue;
            if (contentLoc.length() > 0) contentLoc.append("\n");

            String displayName = loc.city.replace("'", "");
            int paragraphStart = contentLoc.length();
            presentBeginPosition = contentLoc.length();
            contentLoc.append(displayName).append("\t");
            presentEndPosition = contentLoc.length();

            int[] labelColors = new int[settings.isAreaColoring ? loc.colors.size() : 0];
            for (int colorIndex = 0; colorIndex < labelColors.length; colorIndex++) {
                labelColors[colorIndex] = ColorUtil.darken(
                        loc.colors.get(colorIndex),
                        areaColoringDarkenRatio
                );
            }
            contentLoc.setSpan(
                    new LocationLabelSpan(displayName, labelColors),
                    presentBeginPosition,
                    presentEndPosition,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            if (loc.id >= 0) {
                contentLoc.setSpan(
                        new LocationClickSpan(loc.id),
                        presentBeginPosition,
                        presentEndPosition,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }

            for (int i=0; i<loc.prons.size(); i++) {
                if (i>0) {
                    if (!"".equals(loc.notes.get(i-1))) {
                        contentLoc.append("\n");
                        int indentStart = contentLoc.length();
                        contentLoc.append("\t");
                        contentLoc.setSpan(
                                new FixedWidthSpan(LocationLabelSpan.widthEm(displayName)),
                                indentStart,
                                contentLoc.length(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                    } else {
                        contentLoc.append(" · ");
                    }
                }

                int pronunciationUnitStart = contentLoc.length();

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

                String ipaSample = loc.prons.get(i).get(0).ipa;
                if (settings.isPresentIpa && !"".equals(ipaSample)) {
                    presentBeginPosition = contentLoc.length();
                    boolean isMarkNeeded =  !(ipaSample.startsWith("[") || ipaSample.startsWith("("));
                    contentLoc.append(" ");
                    if (isMarkNeeded) contentLoc.append("/");
                    for (int j = 0; j < loc.prons.get(i).size(); j++) {
                        contentLoc.append(j > 0 ? "=" : "").append(singleLoc.get(j).ipa);
                    }
                    if (isMarkNeeded) contentLoc.append("/");
                    presentEndPosition = contentLoc.length();

                    if (settings.isAreaColoring) {
                        contentLoc.setSpan(new ForegroundColorSpan(Color.parseColor("#777777")),
                                presentBeginPosition, presentEndPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }

                if (!"".equals(loc.notes.get(i))) {
                    presentBeginPosition = contentLoc.length();
                    contentLoc.append(" ").append(loc.notes.get(i));
                    presentEndPosition = contentLoc.length();
                    contentLoc.setSpan(new RelativeSizeSpan(0.75f),
                            presentBeginPosition, presentEndPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                contentLoc.setSpan(
                        new NoBreakCandidateSpan(LocationLabelSpan.widthEm(displayName)),
                        pronunciationUnitStart,
                        contentLoc.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }

            float locationTextSize = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP,
                    14,
                    Resources.getSystem().getDisplayMetrics()
            );
            locationParagraphStarts.add(paragraphStart);
            locationParagraphEnds.add(contentLoc.length());
            locationParagraphIndents.add(Math.round(
                    locationTextSize * LocationLabelSpan.widthEm(displayName)
            ));
        }

        for (int i = 0; i < locationParagraphStarts.size(); i++) {
            int end = locationParagraphEnds.get(i);
            if (end < contentLoc.length() && contentLoc.charAt(end) == '\n') {
                end++;
            }
            contentLoc.setSpan(
                    new LeadingMarginSpan.Standard(0, locationParagraphIndents.get(i)),
                    locationParagraphStarts.get(i),
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        return new Spanned[]{
                new SpannableString(charaHead),
                new SpannableString(""),
                contentCharaInfo, contentWanshyu, contentLoc
        };
    }

    private static boolean isBookFiltered(String bookFilter) {
        return cityFilter.contains("韻書") || cityFilter.contains(bookFilter);
    }

    private static void appendBook(SpannableStringBuilder output, String name,
                                   ArrayList<String> entries) {
        int labelStart = output.length();
        output.append(name);
        output.setSpan(new BookLabelSpan(), labelStart, output.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        output.append(" ");
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) output.append(" | ");
            output.append(entries.get(index));
        }
    }


    public int length() {
        return charas.size();
    }


}
