package cc.ecisr.jyutdict.utils;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JyutpingUtil {
    final static private String standardJppForm =
            "((mb?|n[jrd]?|ngg?|[bdg]{1,2}|g[hn]?|r[bdgzscrh]|[zcs][hrjl]?|[ptkvw]h?|[hqfjlr])" +
                    "([jwv]?))?(ng?|m|((i[rwi]?|u[rwu]?|[aeo][aeo]?|y)+" +
                    "(n[ng]?|[mptkh])?))" +
                    "([0-9]?[0-9*][0-9']?)?"; // What the ?!
    final static private String rePronStr  = "^" + standardJppForm + "$";
    final static private Pattern rePron    = Pattern.compile("\\b" + standardJppForm + "\\b");
    final static private Pattern reInitial = Pattern.compile("^(mb?|n[jrd]?|ngg?|[bdg]{1,2}|g[hn]?|r[bdgzscrh]|[zcs][hrjl]?|[ptkvw]h?|[hqfjlr])([jwv]?)(?=[aeoiuymn])");
    final static private Pattern reCoda    = Pattern.compile("(n[ng]?|[mptkh])?$");
    final static private Pattern reTone    = Pattern.compile("[0-9]?[0-9*][0-9']?$");
    final static private Pattern reFinal   = Pattern.compile("(^ng?$|^m$)|(i[rwi]?|u[rwu]?|[aeo][aeo]?|yu$|y)+");

    static public boolean isValidJpp(String jyutping)  {
        return Pattern.matches(rePronStr, jyutping);
    }

    static public String[] splitJyutping(String jyutping) {
        if (!isValidJpp(jyutping)) return new String[]{"", jyutping, ""};
        String ini="", ton="", cod="", fin="";
        Matcher a = reInitial.matcher(jyutping);
        if (a.find()) { ini = a.group(); }
        jyutping = jyutping.substring(ini.length());

        Matcher b = reTone.matcher(jyutping);
        if (b.find()) { ton = b.group(); }
        jyutping = jyutping.substring(0, jyutping.length()-ton.length());

        return new String[]{ini, jyutping, ton};
    }

    static public String[] retrieveJyutping(String rawString) {
        Matcher prons = rePron.matcher(rawString);
        Vector<String> result = new Vector<>();
        while (prons.find()) {
            result.add(prons.group());
        }
        return result.toArray(new String[0]);
    }

    /**
     * 將粵拼輸入解析為 API 查詢參數 [聲母, 韻核, 韻尾, 聲調]
     * 空格表示模糊位置，轉為 "%"
     *
     * 例：
     *   "ji3"  → ["j", "i", "", "3"]
     *   "j 3"  → ["j", "%", "", "3"]   (任意韻核)
     *   " aa3" → ["%", "aa", "", "3"]   (任意聲母)
     *   "aa3"  → ["", "aa", "", "3"]    (無聲母，精確匹配)
     *   "ji"   → ["j", "i", "", "%"]    (任意聲調)
     *
     * @param input 用戶輸入的粵拼字符串
     * @return 長度為 4 的字符串數組 [initial, nucleus, coda, tone]，
     *         如果輸入無法解析則返回 null
     */
    public static String[] parseJyutpingQuery(String input) {
        if (input == null || input.isEmpty()) return null;
        input = input.toLowerCase().trim();

        // 正則定義
        String initialFormat = "^( ?)(mb?|n[jrd]?|ngg?|[bdg]{1,2}|g[hn]?|r[bdgzscrh]|[zcs][hrjl]?|[ptkvw]h?|[hqfjlr])([jwv]?)([ aeiouymn])?";
        String codaFormat = "(?<=[aeiouymn])([ngmptkh]?)([ \\d]*)$";
        String toneFormat = "[ \\d]*([1-6]?[\\*']?)$";

        String initial = "", nucleus = "", coda = "", tone = "";

        // Step 1: 提取聲調（末尾數字部分）
        java.util.regex.Matcher toneMatcher = Pattern.compile(toneFormat).matcher(input);
        if (toneMatcher.find()) {
            tone = toneMatcher.group(1);
            input = input.substring(0, toneMatcher.start());
        }

        // Step 2: 提取聲母（開頭輔音部分）
        java.util.regex.Matcher initialMatcher = Pattern.compile(initialFormat).matcher(input);
        if (initialMatcher.find()) {
            String leadingSpace = initialMatcher.group(1);
            initial = leadingSpace + initialMatcher.group(2) + initialMatcher.group(3);
            input = input.substring(initialMatcher.end());
        }

        // Step 3: 提取韻尾（元音後的輔音部分）
        java.util.regex.Matcher codaMatcher = Pattern.compile(codaFormat).matcher(input);
        if (codaMatcher.find()) {
            coda = codaMatcher.group(1);
            input = input.substring(0, codaMatcher.start());
        }

        // Step 4: 剩餘部分為韻核
        nucleus = input;

        // Step 5: 空格轉為 "%" 作為模糊匹配
        initial = initial.contains(" ") ? "%" : initial;
        nucleus = nucleus.contains(" ") ? "%" : nucleus;
        coda = coda.contains(" ") ? "%" : coda;
        tone = tone.contains(" ") ? "%" : tone;

        // 如果沒有聲調，默認模糊匹配所有聲調
        if (tone.isEmpty()) {
            tone = "%";
        }

        return new String[]{initial, nucleus, coda, tone};
    }
}