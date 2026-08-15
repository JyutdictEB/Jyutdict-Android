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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cc.ecisr.jyutdict.utils.ColorUtil;
import cc.ecisr.jyutdict.utils.StringUtil;
import cc.ecisr.jyutdict.widget.AnnotationClickSpan;
import cc.ecisr.jyutdict.widget.DashedUnderlineSpan;
import cc.ecisr.jyutdict.widget.GradientTextSpan;
import cc.ecisr.jyutdict.widget.LocationClickSpan;

/** One sheet row and its five rendered result fields. */
public class FjbCharacter {
    private static final String ENTER = "<br>";
    private final Map<String, String> values = new HashMap<>();
    private final EntrySetting settings;
    private JSONObject cellNotes;

    public FjbCharacter(JSONObject entry, EntrySetting settings) {
        this.settings = settings;
        Iterator<String> keys = entry.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            values.put(key, entry.optString(key, ""));
        }
        try {
            cellNotes = new JSONObject(value(FjbHeaderInfo.COLUMN_NAME_CELL_NOTE));
        } catch (JSONException ignored) {}
    }

    public Spanned printMeanings() {
        StringBuilder html = new StringBuilder();
        String booksChara = value(FjbHeaderInfo.COLUMN_NAME_BOOKS_CHARA);
        String booksPron = value(FjbHeaderInfo.COLUMN_NAME_BOOKS_PRON);
        String booksMeaning = value(FjbHeaderInfo.COLUMN_NAME_BOOKS_MEANING);
        if (!booksChara.isEmpty() || !booksPron.isEmpty() || !booksMeaning.isEmpty()) {
            html.append("—— <i>");
            if (!booksChara.isEmpty()) {
                html.append(booksChara);
                if (!booksPron.isEmpty() || !booksMeaning.isEmpty()) html.append(": ");
            }
            html.append(booksPron);
            if (!booksPron.isEmpty() && !booksMeaning.isEmpty()) html.append(" | ");
            if (!booksMeaning.isEmpty()) html.append("「").append(booksMeaning).append("」");
            html.append("</i>").append(ENTER);
        }

        String original = value(FjbHeaderInfo.COLUMN_NAME_MEANING)
                .replace("<", "&lt;")
                .replaceAll("(?<=[^}\"“])&lt;", "；&lt;");
        if (original.startsWith("[粵]") && original.contains("{1}")) {
            original = original.replaceFirst("\\[粵]", "[粵]；");
        }
        original = original.replace("}", "} ");
        String[] meanings = original.split("[；。？！] *?((?=&lt;)|(?=[{]))");
        String[] markers = value(FjbHeaderInfo.COLUMN_NAME_GRAMMAR_MARKER).split("[;；] ?");
        boolean hasMarkers = markers.length == original.split("；").length;
        int markerIndex = 0;
        for (String meaning : meanings) {
            if (meaning.isEmpty()) continue;
            if (hasMarkers && !markers[markerIndex].isEmpty()) {
                String marker = markers[markerIndex].replace("？", "?");
                if (meanings.length == 1) html.append("‹").append(marker).append("›");
                else {
                    meaning = meaning.replaceFirst("(?<=[}])", "‹" + marker + "›");
                    markerIndex++;
                }
            }
            if (meaning.contains("[粵]") && meanings.length > 1) {
                html.append("<b>").append(meaning).append("</b>");
            } else {
                html.append(meaning);
            }
            html.append(ENTER);
        }
        if (!original.isEmpty()) html.delete(html.length() - ENTER.length(), html.length());
        return Html.fromHtml(html.toString());
    }

    public Spanned printLocations() {
        SpannableStringBuilder text = new SpannableStringBuilder();
        for (String key : FjbHeaderInfo.getCityListInShort()) appendCity(text, key);
        if (text.length() >= 2) text.delete(text.length() - 2, text.length());

        boolean hasForeign = false;
        for (String key : FjbHeaderInfo.getForeignListInShort()) {
            String pronunciation = value(key);
            if (pronunciation.isEmpty()) continue;
            if (hasForeign) text.append(" \t");
            else {
                text.append("\n\n");
                span(text, new RelativeSizeSpan(0.5f), text.length() - 1, text.length());
                hasForeign = true;
            }
            appendForeign(text, key, pronunciation);
        }

        String major = value(FjbHeaderInfo.COLUMN_NAME_CLASS_MAJOR);
        if (!major.isEmpty() && settings.isMeaningDomainPresence) {
            StringBuilder classes = new StringBuilder(major);
            String secondary = value(FjbHeaderInfo.COLUMN_NAME_CLASS_SECONDARY);
            String minor = value(FjbHeaderInfo.COLUMN_NAME_CLASS_MINOR);
            if (!secondary.isEmpty()) classes.append("\n").append(secondary);
            if (!minor.isEmpty()) classes.append("\n").append(minor);
            int start = text.length();
            text.append("\n\n").append(classes);
            span(text, new ForegroundColorSpan(Color.parseColor("#BBBBBB")),
                    start + 2, text.length());
        }
        return text;
    }

    private void appendCity(SpannableStringBuilder text, String key) {
        String pronunciation = value(key);
        if (pronunciation.isEmpty()) return;
        String[] fullName = FjbHeaderInfo.getFullName(key);
        String displayName = fullName[0] + fullName[1];
        int itemStart = text.length();
        text.append(displayName).append(": ");
        int nameEnd = itemStart + displayName.length();
        span(text, new LocationClickSpan(displayName), itemStart, text.length());
        int styleStart = itemStart;
        if (settings.isAreaColoring) {
            span(text, new GradientTextSpan(tinted(FjbHeaderInfo.getCityColors(key))),
                    itemStart, nameEnd);
            styleStart = text.length();
        }
        int pronunciationStart = text.length();
        appendCityPronunciation(text, pronunciation);
        int pronunciationEnd = text.length();
        text.append(" \t");
        if ("_".equals(pronunciation)) {
            span(text, new ForegroundColorSpan(Color.parseColor("#BBBBBB")),
                    styleStart, text.length());
        }
        if (pronunciation.contains("?")) {
            span(text, new StyleSpan(Typeface.ITALIC), styleStart, text.length());
        }
        applyNote(text, key, pronunciation, pronunciationStart, pronunciationEnd,
                FjbHeaderInfo.getCityColors(key));
    }

    private void appendForeign(SpannableStringBuilder text, String key, String pronunciation) {
        int itemStart = text.length();
        text.append(key).append(": ");
        int nameEnd = itemStart + key.length();
        int styleStart = itemStart;
        if (settings.isAreaColoring) {
            span(text, new GradientTextSpan(tinted(FjbHeaderInfo.getForeignColors(key))),
                    itemStart, nameEnd);
            styleStart = text.length();
        }
        int pronunciationStart = text.length();
        text.append(pronunciation.replace('\n', ','));
        int pronunciationEnd = text.length();
        if (pronunciation.contains("?")) {
            span(text, new StyleSpan(Typeface.ITALIC), styleStart, pronunciationEnd);
        }
        applyNote(text, key, pronunciation, pronunciationStart, pronunciationEnd,
                FjbHeaderInfo.getForeignColors(key));
    }

    private void appendCityPronunciation(SpannableStringBuilder text, String pronunciation) {
        if (!pronunciation.contains("^")) {
            text.append(pronunciation);
            return;
        }
        for (String part : pronunciation.split("\\^")) {
            text.append(part);
            if (part.charAt(0) > 'z') {
                span(text, new StrikethroughSpan(),
                        text.length() - part.length(), text.length() - part.length() + 1);
            }
        }
    }

    private void applyNote(SpannableStringBuilder text, String key, String pronunciation,
                           int start, int end, ArrayList<String> colors) {
        String note = cellNotes == null ? "" : cellNotes.optString(key);
        if (note.isEmpty()) return;
        String message = ">「" + value(FjbHeaderInfo.COLUMN_NAME_CHARACTER) + "」("
                + value(FjbHeaderInfo.COLUMN_NAME_PRONUNCIATION) + ")   [" + key + "] "
                + pronunciation + ", \n" + note.replaceAll("\n\t-.+", "\t\t- by Anonymous")
                .replaceAll("\n-{10,}", "");
        span(text, new AnnotationClickSpan(message), start, end);
        int[] underline = settings.isAreaColoring
                ? tinted(colors) : new int[]{Color.parseColor("#999999")};
        span(text, new DashedUnderlineSpan(underline), start, end);
    }

    private int[] tinted(ArrayList<String> colors) {
        double ratio = settings.isUsingNightMode
                ? 2 - settings.areaColoringDarkenRatio : settings.areaColoringDarkenRatio;
        return ColorUtil.locationColorInts(colors, ratio);
    }

    public Spanned printCharacter() {
        String character = value(FjbHeaderInfo.COLUMN_NAME_CHARACTER);
        String display = character.isEmpty() || "？".equals(character)
                ? "□" : character.replaceAll("[?/!！？ ]", "");
        if (!display.equals("見")) display = display.replace("見", "");
        if (!display.equals("歸")) display = display.replace("歸", "");
        SpannableStringBuilder text = new SpannableStringBuilder(display);
        if (character.contains("？") || character.contains("?")) {
            span(text, new ForegroundColorSpan(Color.parseColor("#B9BAA3")), 0, text.length());
        }
        if (character.contains("見 ") || character.contains("歸")) {
            span(text, new ForegroundColorSpan(Color.parseColor("#3D3B4F")), 0, text.length());
        }
        return text;
    }

    public Spanned printUnicode() {
        String character = value(FjbHeaderInfo.COLUMN_NAME_CHARACTER)
                .replaceAll("[?/!？！見歸 ]", "");
        SpannableStringBuilder text = new SpannableStringBuilder();
        String unicode = StringUtil.countCharaLength(character) < 2
                ? StringUtil.charaToUnicode(character) : "";
        text.append(unicode);
        String ids = value(FjbHeaderInfo.COLUMN_NAME_IDS);
        if (!ids.isEmpty()) {
            if (!unicode.isEmpty()) text.append("\n");
            text.append("[").append(ids).append("]");
        }
        return text;
    }

    public Spanned printPronunciation() {
        String[] pronunciations = value(FjbHeaderInfo.COLUMN_NAME_PRONUNCIATION)
                .replaceAll("[!！]", "").split("/");
        SpannableStringBuilder text = new SpannableStringBuilder();
        for (int index = 0; index < pronunciations.length; index++) {
            if (index > 0) text.append(index % 2 == 0 ? "/\n" : "/");
            text.append(pronunciations[index]);
        }
        if (text.toString().contains("?")) {
            span(text, new StyleSpan(Typeface.ITALIC), 0, text.length());
        }
        String conventional = value(FjbHeaderInfo.COLUMN_NAME_CONVENTIONAL);
        if (!conventional.isEmpty()) {
            if (!text.isEmpty()) text.append("\n");
            text.append("(").append(conventional).append(")");
        }
        return text;
    }

    private String value(String key) {
        String result = values.get(key);
        return result == null ? "" : result.trim();
    }

    private static void span(SpannableStringBuilder text, Object span, int start, int end) {
        text.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
