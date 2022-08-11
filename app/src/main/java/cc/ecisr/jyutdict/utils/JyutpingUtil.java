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

        //Matcher c = reCoda.matcher(jyutping);
        //if (c.find()) { cod = c.group(); }
        //jyutping = jyutping.substring(0, jyutping.length()-cod.length());

        //Matcher d = reFinal.matcher(jyutping);
        //if (d.find()) { fin = d.group(); }
        //if (fin.length() < jyutping.length()) return new String[]{"", fin, "", ""};
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
}