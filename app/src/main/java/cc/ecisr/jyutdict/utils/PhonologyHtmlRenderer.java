package cc.ecisr.jyutdict.utils;

import org.json.JSONArray;
import org.json.JSONObject;

/** 將公開的程序音系 JSON 轉為可縮放、可橫向滾動的只讀 HTML 表。 */
public final class PhonologyHtmlRenderer {
    private PhonologyHtmlRenderer() {
    }

    public static String render(JSONObject report, boolean darkMode) {
        String background = darkMode ? "#202020" : "#FAFAFA";
        String foreground = darkMode ? "#E9E7EF" : "#0F110C";
        String surface = darkMode ? "#292929" : "#FFFFFF";
        String header = darkMode ? "#151515" : "#222222";
        String border = darkMode ? "#4A4A4A" : "#D8D8D8";
        String muted = darkMode ? "#ABAAB1" : "#666666";

        StringBuilder html = new StringBuilder(64 * 1024);
        html.append("<!doctype html><html><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<style>")
                .append("body{font-family:sans-serif;margin:0;padding:16px;line-height:1.45;background:")
                .append(background).append(";color:").append(foreground).append("}")
                .append("h1{font-size:22px;margin:0 0 8px}h2{font-size:18px;margin:24px 0 8px;border-left:4px solid #D32913;padding-left:8px}")
                .append(".notice{color:").append(muted).append(";font-size:13px;margin-bottom:14px}")
                .append(".tabs{display:flex;gap:6px;flex-wrap:wrap;margin:12px 0}.tabs a{border:1px solid #D32913;color:#D32913;padding:5px 8px;text-decoration:none;font-weight:bold}")
                .append(".table-wrap{overflow-x:auto;background:").append(surface).append("}")
                .append("table{border-collapse:collapse;min-width:720px;width:100%;font-size:13px}")
                .append("th,td{border:1px solid ").append(border).append(";padding:6px;vertical-align:top}")
                .append("thead th{background:").append(header).append(";color:#fff;text-align:left}")
                .append("tbody th{white-space:nowrap}.condition{color:").append(muted).append("}")
                .append(".outcome{font-weight:bold;color:#D32913}.count{white-space:nowrap;color:")
                .append(muted).append("}.examples{min-width:180px}")
                .append("</style></head><body>");

        html.append("<h1>").append(escape(report.optString("locationName", "音系表"))).append("</h1>")
                .append("<div class=\"notice\">本表爲程序生成，音之分合均由程序依條件熵所定。</div>");

        JSONArray sections = report.optJSONArray("sections");
        if (sections == null || sections.length() == 0) {
            html.append("<p>音系表沒有可顯示的分區。</p></body></html>");
            return html.toString();
        }

        html.append("<nav class=\"tabs\">");
        for (int i = 0; i < sections.length(); i++) {
            JSONObject section = sections.optJSONObject(i);
            if (section == null) continue;
            html.append("<a href=\"#section-").append(i).append("\">")
                    .append(escape(section.optString("label", "音系")))
                    .append("</a>");
        }
        html.append("</nav>");

        for (int i = 0; i < sections.length(); i++) {
            JSONObject section = sections.optJSONObject(i);
            if (section == null) continue;
            String label = section.optString("label", "音系");
            html.append("<section id=\"section-").append(i).append("\"><h2>")
                    .append(escape(label)).append("</h2><div class=\"table-wrap\"><table><thead><tr>")
                    .append("<th>").append(escape(section.optString("baseLabel", "中古音"))).append("</th>")
                    .append("<th>條件</th><th>")
                    .append(escape(section.optString("outcomeLabel", "現代音"))).append("</th>")
                    .append("<th>轄字</th><th>例字</th></tr></thead><tbody>");

            JSONArray rules = section.optJSONArray("rules");
            if (rules != null) {
                for (int ruleIndex = 0; ruleIndex < rules.length(); ruleIndex++) {
                    JSONObject rule = rules.optJSONObject(ruleIndex);
                    if (rule == null) continue;
                    JSONArray outcomes = rule.optJSONArray("outcomes");
                    if (outcomes == null || outcomes.length() == 0) continue;
                    String conditions = renderConditions(rule.optJSONArray("conditions"));

                    for (int outcomeIndex = 0; outcomeIndex < outcomes.length(); outcomeIndex++) {
                        JSONObject outcome = outcomes.optJSONObject(outcomeIndex);
                        if (outcome == null) continue;
                        html.append("<tr>");
                        if (outcomeIndex == 0) {
                            html.append("<th rowspan=\"").append(outcomes.length()).append("\">")
                                    .append(escape(rule.optString("base", "—"))).append("</th>")
                                    .append("<td class=\"condition\" rowspan=\"")
                                    .append(outcomes.length()).append("\">")
                                    .append(conditions).append("</td>");
                        }
                        html.append("<td class=\"outcome\">")
                                .append(escape(outcome.optString("value", "∅")))
                                .append("</td><td class=\"count\">")
                                .append(outcome.optInt("charCount", 0));
                        if (!outcome.isNull("checkedCharCount")) {
                            html.append("+").append(outcome.optInt("checkedCharCount", 0));
                        }
                        html.append("</td><td class=\"examples\">")
                                .append(renderExamples(outcome.optJSONArray("examples")))
                                .append("</td></tr>");
                    }
                }
            }
            html.append("</tbody></table></div></section>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    private static String renderConditions(JSONArray conditions) {
        if (conditions == null || conditions.length() == 0) return "—";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < conditions.length(); i++) {
            JSONArray condition = conditions.optJSONArray(i);
            if (condition == null) continue;
            if (result.length() > 0) result.append("<br>");
            result.append(escape(condition.optString(0, "")))
                    .append("：<strong>")
                    .append(escape(condition.optString(1, "")))
                    .append("</strong>");
        }
        return result.length() == 0 ? "—" : result.toString();
    }

    private static String renderExamples(JSONArray examples) {
        if (examples == null || examples.length() == 0) return "—";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < examples.length(); i++) {
            JSONObject example = examples.optJSONObject(i);
            if (example == null) continue;
            if (result.length() > 0) result.append("　");
            result.append("<span title=\"")
                    .append(escapeAttribute(pronunciations(example)))
                    .append("\">")
                    .append(escape(example.optString("char", "")));
            String note = example.optString("note", "");
            if (!note.isEmpty()) {
                result.append("<small>(").append(escape(note)).append(")</small>");
            }
            result.append("</span>");
        }
        return result.length() == 0 ? "—" : result.toString();
    }

    private static String pronunciations(JSONObject example) {
        JSONArray values = example.optJSONArray("pronunciations");
        if (values == null) return example.optString("pronunciations", "");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length(); i++) {
            if (i > 0) result.append("、");
            result.append(values.optString(i, ""));
        }
        return result.toString();
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String escapeAttribute(String value) {
        return escape(value).replace("'", "&#39;");
    }
}
