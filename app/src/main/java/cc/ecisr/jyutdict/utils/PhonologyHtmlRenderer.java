package cc.ecisr.jyutdict.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 將公開的程序音系 JSON 轉為四頁可切換的只讀 HTML 表。 */
public final class PhonologyHtmlRenderer {
    private PhonologyHtmlRenderer() {
    }

    public static String render(JSONObject report, boolean darkMode) {
        JSONArray sections = report.optJSONArray("sections");
        StringBuilder html = new StringBuilder(192 * 1024);
        html.append("<!doctype html><html><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=5\">")
                .append("<style>").append(css(darkMode)).append("</style></head><body>")
                .append("<h1>").append(escape(report.optString("locationName", "音系表"))).append("</h1>")
                .append("<p class=\"notice\">本表爲程序生成，音之分合均爲程序依條件熵所定。</p>");

        if (sections == null || sections.length() == 0) {
            return html.append("<p>音系表沒有可顯示的分區。</p></body></html>").toString();
        }

        for (int i = 0; i < sections.length(); i++) {
            html.append("<input class=\"tab-radio\" type=\"radio\" name=\"phonology-tab\" id=\"tab-")
                    .append(i).append("\"").append(i == 0 ? " checked" : "").append(">");
        }
        html.append("<nav class=\"tabs\" role=\"tablist\">");
        for (int i = 0; i < sections.length(); i++) {
            JSONObject section = sections.optJSONObject(i);
            if (section == null) continue;
            html.append("<label role=\"tab\" for=\"tab-").append(i).append("\">")
                    .append(escape(section.optString("label", "音系"))).append("</label>");
        }
        html.append("</nav><div class=\"section-stack\">");

        for (int i = 0; i < sections.length(); i++) {
            JSONObject section = sections.optJSONObject(i);
            if (section == null) continue;
            html.append("<section class=\"phonology-section section-").append(i).append("\">")
                    .append("<h2>").append(escape(section.optString("label", "音系"))).append("</h2>")
                    .append(renderSection(section, darkMode))
                    .append("</section>");
        }
        html.append("</div><script>").append(filterScript()).append("</script></body></html>");
        return html.toString();
    }

    private static String renderSection(JSONObject section, boolean darkMode) {
        JSONArray rules = section.optJSONArray("rules");
        int conditionDepth = 1;
        if (rules != null) {
            for (int i = 0; i < rules.length(); i++) {
                JSONObject rule = rules.optJSONObject(i);
                JSONArray conditions = rule == null ? null : rule.optJSONArray("conditions");
                conditionDepth = Math.max(conditionDepth, conditions == null ? 0 : conditions.length());
            }
        }

        List<RowData> rows = flattenRows(rules, conditionDepth);
        calculateMerges(rows, conditionDepth);
        StringBuilder result = new StringBuilder();
        result.append("<div class=\"section-tools\"><input class=\"table-filter\" type=\"search\" ")
                .append("placeholder=\"篩選中古音、現音、條件或例字\" ")
                .append("aria-label=\"篩選中古音、現音、條件或例字\"></div>")
                .append("<div class=\"table-wrap\"><table data-merge-columns=\"")
                .append(conditionDepth + 1)
                .append("\"><thead><tr><th class=\"position-head base-head\">")
                .append(escape(section.optString("baseLabel", "中古音"))).append("</th>");
        for (int condition = 0; condition < conditionDepth; condition++) {
            result.append("<th class=\"position-head condition-head\">條件 ")
                    .append(condition + 1).append("</th>");
        }
        result.append("<th class=\"target-heading\">")
                .append(escape(section.optString("outcomeLabel", "現代音")))
                .append("</th><th>轄字</th><th>例字</th></tr></thead><tbody>");

        for (RowData row : rows) {
            result.append("<tr class=\"data-row\" data-search=\"")
                    .append(escapeAttribute(searchText(row.rule, row.outcome)))
                    .append("\">");
            for (int column = 0; column <= conditionDepth; column++) {
                boolean baseColumn = column == 0;
                String tag = baseColumn ? "th" : "td";
                result.append("<").append(tag)
                        .append(" class=\"mergeable position-cell ")
                        .append(baseColumn ? "base" : "condition")
                        .append("\" data-merge-column=\"").append(column)
                        .append("\" data-merge-key=\"")
                        .append(escapeAttribute(mergeKey(row, column)))
                        .append("\"");
                if (!row.mergedCellVisible[column]) {
                    result.append(" style=\"display:none\"");
                } else if (row.rowSpans[column] > 1) {
                    result.append(" rowspan=\"").append(row.rowSpans[column]).append("\"");
                }
                result.append(">");
                if (baseColumn) {
                    result.append(escape(emptyAsNull(row.rule.optString("base", ""))));
                } else {
                    JSONArray conditions = row.rule.optJSONArray("conditions");
                    JSONArray condition = conditions == null
                            ? null
                            : conditions.optJSONArray(column - 1);
                    if (condition == null) {
                        result.append("—");
                    } else {
                        result.append("<small>")
                                .append(escape(condition.optString(0, "")))
                                .append("</small> <strong>")
                                .append(escape(condition.optString(1, "")))
                                .append("</strong>");
                    }
                }
                result.append("</").append(tag).append(">");
            }
            result.append(renderOutcomeCell(
                            row.outcome,
                            row.relativeToMode,
                            row.prominence,
                            darkMode
                    ))
                    .append("<td class=\"count\" style=\"opacity:")
                    .append(decimal(0.72 + row.prominence * 0.28)).append("\">")
                    .append(row.outcome.optInt("charCount", 0));
            if (!row.outcome.isNull("checkedCharCount")) {
                result.append("+").append(row.outcome.optInt("checkedCharCount", 0));
            }
            result.append("</td><td class=\"examples\" style=\"opacity:")
                    .append(decimal(0.72 + row.prominence * 0.28)).append("\">")
                    .append(renderExamples(
                            row.outcome.optJSONArray("examples"),
                            "reverse-finals".equals(section.optString("id", "")),
                            darkMode
                    ))
                    .append("</td></tr>");
        }
        result.append("<tr class=\"no-results\"")
                .append(rows.isEmpty() ? "" : " style=\"display:none\"")
                .append("><td colspan=\"").append(conditionDepth + 4)
                .append("\">沒有符合條件的行</td></tr>");
        return result.append("</tbody></table></div>").toString();
    }

    private static List<RowData> flattenRows(JSONArray rules, int conditionDepth) {
        ArrayList<RowData> rows = new ArrayList<>();
        if (rules == null) return rows;
        for (int ruleIndex = 0; ruleIndex < rules.length(); ruleIndex++) {
            JSONObject rule = rules.optJSONObject(ruleIndex);
            if (rule == null) continue;
            JSONArray outcomes = rule.optJSONArray("outcomes");
            if (outcomes == null || outcomes.length() == 0) continue;
            int total = 0;
            int modeCount = 0;
            for (int outcomeIndex = 0; outcomeIndex < outcomes.length(); outcomeIndex++) {
                int count = outcomeCount(outcomes.optJSONObject(outcomeIndex));
                total += count;
                modeCount = Math.max(modeCount, count);
            }
            for (int outcomeIndex = 0; outcomeIndex < outcomes.length(); outcomeIndex++) {
                JSONObject outcome = outcomes.optJSONObject(outcomeIndex);
                if (outcome == null) continue;
                int count = outcomeCount(outcome);
                double share = total == 0 ? 0 : (double) count / total;
                double relative = modeCount == 0 ? 0 : (double) count / modeCount;
                double prominence = share >= 0.4
                        ? 1
                        : 0.35 + 0.65 * share / 0.4;
                rows.add(new RowData(
                        rule,
                        outcome,
                        relative,
                        prominence,
                        conditionDepth + 1
                ));
            }
        }
        return rows;
    }

    private static void calculateMerges(List<RowData> rows, int conditionDepth) {
        for (int column = 0; column <= conditionDepth; column++) {
            int index = 0;
            while (index < rows.size()) {
                String key = mergeKey(rows.get(index), column);
                int end = index + 1;
                while (end < rows.size() && key.equals(mergeKey(rows.get(end), column))) {
                    end++;
                }
                rows.get(index).mergedCellVisible[column] = true;
                rows.get(index).rowSpans[column] = end - index;
                index = end;
            }
        }
    }

    private static String mergeKey(RowData row, int column) {
        StringBuilder key = new StringBuilder(row.rule.optString("base", ""));
        JSONArray conditions = row.rule.optJSONArray("conditions");
        for (int index = 0; index < column; index++) {
            key.append('\u241f');
            JSONArray condition = conditions == null ? null : conditions.optJSONArray(index);
            key.append(condition == null ? "null" : condition.toString());
        }
        return key.toString();
    }

    private static String searchText(JSONObject rule, JSONObject outcome) {
        StringBuilder text = new StringBuilder(rule.optString("base", ""));
        JSONArray conditions = rule.optJSONArray("conditions");
        if (conditions != null) {
            for (int index = 0; index < conditions.length(); index++) {
                JSONArray condition = conditions.optJSONArray(index);
                if (condition == null) continue;
                for (int value = 0; value < condition.length(); value++) {
                    text.append(' ').append(condition.optString(value, ""));
                }
            }
        }
        text.append(' ').append(outcome.optString("value", ""));
        JSONArray examples = outcome.optJSONArray("examples");
        if (examples != null) {
            for (int index = 0; index < examples.length(); index++) {
                JSONObject example = examples.optJSONObject(index);
                if (example == null) continue;
                text.append(' ').append(example.optString("char", ""))
                        .append(' ').append(example.optString("note", ""))
                        .append(' ').append(pronunciations(example));
            }
        }
        return text.toString().toLowerCase(Locale.ROOT);
    }

    private static final class RowData {
        final JSONObject rule;
        final JSONObject outcome;
        final double relativeToMode;
        final double prominence;
        final boolean[] mergedCellVisible;
        final int[] rowSpans;

        RowData(JSONObject rule, JSONObject outcome, double relativeToMode,
                double prominence, int positionColumns) {
            this.rule = rule;
            this.outcome = outcome;
            this.relativeToMode = relativeToMode;
            this.prominence = prominence;
            this.mergedCellVisible = new boolean[positionColumns];
            this.rowSpans = new int[positionColumns];
        }
    }

    private static String renderOutcomeCell(JSONObject outcome, double relative,
                                            double prominence, boolean darkMode) {
        String value = outcome.optString("value", "");
        PhonologyColorUtil.Colors colors = PhonologyColorUtil.forValue(value);
        String accent = darkMode ? colors.accentDark : colors.accent;
        String surface = darkMode ? colors.surfaceDark : colors.surface;
        String stripe = darkMode ? colors.stripeDark : colors.stripe;
        boolean isShe = "she".equals(outcome.optString("level", ""));
        return "<td class=\"outcome\" style=\"--accent:" + accent
                + ";--surface:" + surface
                + ";--stripe:" + stripe
                + ";--bar-opacity:" + decimal(0.72 + prominence * 0.28)
                + ";--content-opacity:" + decimal(0.55 + prominence * 0.45)
                + ";--relative:" + decimal(relative * 100) + "%\">"
                + "<span class=\"outcome-bar" + (isShe ? " she" : "") + "\"></span>"
                + "<span class=\"outcome-content\"><strong>" + escape(emptyAsNull(value)) + "</strong>"
                + (isShe ? "<span class=\"she-badge\">攝</span>" : "")
                + "</span></td>";
    }

    private static String renderExamples(JSONArray examples, boolean colourFinals,
                                         boolean darkMode) {
        if (examples == null || examples.length() == 0) return "—";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < examples.length(); i++) {
            JSONObject example = examples.optJSONObject(i);
            if (example == null) continue;
            if (result.length() > 0) result.append(" ");
            String style = colourFinals ? exampleStyle(example, darkMode) : "";
            result.append("<span class=\"example")
                    .append(colourFinals ? " coloured" : "")
                    .append("\"").append(style)
                    .append(" title=\"").append(escapeAttribute(pronunciations(example))).append("\">")
                    .append("<span class=\"example-char\">")
                    .append(escape(example.optString("char", ""))).append("</span>");
            String note = example.optString("note", "");
            if (!note.isEmpty()) {
                result.append("<small>(").append(escape(note)).append(")</small>");
            }
            result.append("</span>");
        }
        return result.length() == 0 ? "—" : result.toString();
    }

    private static String exampleStyle(JSONObject example, boolean darkMode) {
        Set<String> finals = new LinkedHashSet<>();
        JSONArray values = example.optJSONArray("pronunciations");
        if (values != null) {
            for (int i = 0; i < values.length(); i++) {
                String[] parts = JyutpingUtil.splitJyutping(values.optString(i, ""));
                if (parts[1].isEmpty()) continue;
                finals.add(PhonologyColorUtil.normaliseCheckedFinal(parts[1]));
            }
        }
        if (finals.isEmpty()) return "";
        List<String> surfaces = new ArrayList<>();
        List<String> accents = new ArrayList<>();
        for (String value : finals) {
            PhonologyColorUtil.Colors colors = PhonologyColorUtil.forValue(value);
            surfaces.add(darkMode ? colors.surfaceDark : colors.surface);
            accents.add(darkMode ? colors.accentDark : colors.accent);
        }
        return " style=\"--example-surface:" + segmented(surfaces)
                + ";--example-accent:" + segmented(accents) + "\"";
    }

    private static String segmented(List<String> colors) {
        if (colors.size() == 1) return colors.get(0);
        StringBuilder result = new StringBuilder("linear-gradient(90deg,");
        double width = 100.0 / colors.size();
        for (int i = 0; i < colors.size(); i++) {
            if (i > 0) result.append(",");
            result.append(colors.get(i)).append(" ").append(decimal(i * width)).append("%,")
                    .append(colors.get(i)).append(" ").append(decimal((i + 1) * width)).append("%");
        }
        return result.append(")").toString();
    }

    private static int outcomeCount(JSONObject outcome) {
        if (outcome == null) return 0;
        return outcome.optInt("charCount", 0) + outcome.optInt("checkedCharCount", 0);
    }

    private static String filterScript() {
        return "(function(){"
                + "function remerge(table){"
                + "var all=Array.prototype.slice.call(table.querySelectorAll('tr.data-row'));"
                + "var visible=all.filter(function(row){return row.style.display!=='none';});"
                + "all.forEach(function(row){"
                + "Array.prototype.forEach.call(row.querySelectorAll('.mergeable'),function(cell){"
                + "cell.style.display='';cell.rowSpan=1;});});"
                + "var columns=parseInt(table.getAttribute('data-merge-columns')||'0',10);"
                + "for(var column=0;column<columns;column++){"
                + "var index=0;"
                + "while(index<visible.length){"
                + "var cell=visible[index].querySelector('[data-merge-column=\"'+column+'\"]');"
                + "var key=cell?cell.getAttribute('data-merge-key'):'';"
                + "var end=index+1;"
                + "while(end<visible.length){"
                + "var next=visible[end].querySelector('[data-merge-column=\"'+column+'\"]');"
                + "if(!next||next.getAttribute('data-merge-key')!==key)break;end++;}"
                + "if(cell)cell.rowSpan=end-index;"
                + "for(var hidden=index+1;hidden<end;hidden++){"
                + "var duplicate=visible[hidden].querySelector('[data-merge-column=\"'+column+'\"]');"
                + "if(duplicate)duplicate.style.display='none';}"
                + "index=end;}}"
                + "var empty=table.querySelector('tr.no-results');"
                + "if(empty)empty.style.display=visible.length?'none':'';"
                + "}"
                + "Array.prototype.forEach.call(document.querySelectorAll('.table-filter'),function(input){"
                + "input.addEventListener('input',function(){"
                + "var section=input.closest('.phonology-section');"
                + "var table=section.querySelector('table');"
                + "var needle=input.value.trim().toLocaleLowerCase();"
                + "Array.prototype.forEach.call(table.querySelectorAll('tr.data-row'),function(row){"
                + "var haystack=row.getAttribute('data-search')||'';"
                + "row.style.display=!needle||haystack.indexOf(needle)!==-1?'':'none';});"
                + "remerge(table);});});"
                + "})();";
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

    private static String css(boolean dark) {
        String background = dark ? "#202020" : "#FAFAFA";
        String foreground = dark ? "#E9E7EF" : "#0F110C";
        String surface = dark ? "#292929" : "#FFFFFF";
        String border = dark ? "#4A4A4A" : "#D8D8D8";
        String muted = dark ? "#ABAAB1" : "#666666";
        String base = dark ? "#232323" : "#F2F2F2";
        String header = dark ? "#101010" : "#222222";
        StringBuilder selectors = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            selectors.append("#tab-").append(i)
                    .append(":checked~.tabs label[for=tab-").append(i)
                    .append("]{background:#D32913;color:white;border-color:#D32913}")
                    .append("#tab-").append(i)
                    .append(":checked~.section-stack .section-").append(i)
                    .append("{display:block}");
        }
        return "*{box-sizing:border-box}html{-webkit-text-size-adjust:100%}"
                + "body{margin:0;padding:16px 14px 30px;font-family:sans-serif;font-size:14px;"
                + "line-height:1.5;background:" + background + ";color:" + foreground + "}"
                + "h1{font-size:22px;margin:0 0 6px}h2{font-size:17px;margin:0;padding:10px 12px;"
                + "border-bottom:1px solid " + border + "}.notice{color:" + muted + ";margin:0 0 12px}"
                + ".tab-radio{position:absolute;opacity:0;pointer-events:none}"
                + ".tabs{display:flex;gap:5px;overflow-x:auto;padding:2px 1px 12px}"
                + ".tabs label{flex:0 0 auto;border:2px solid " + border + ";padding:8px 11px;"
                + "font-weight:bold;color:" + foreground + ";background:" + surface + "}"
                + selectors
                + ".phonology-section{display:none;border:1px solid " + border + ";background:" + surface + "}"
                + ".section-tools{display:flex;justify-content:flex-end;padding:9px 10px;"
                + "border-bottom:1px solid " + border + "}"
                + ".table-filter{width:100%;max-width:22rem;border:2px solid " + border + ";"
                + "border-radius:0;padding:7px 8px;background:" + surface + ";color:" + foreground
                + ";font-size:13px;outline:none}.table-filter:focus{border-color:#D32913}"
                + ".table-wrap{max-width:100%;overflow-x:auto}"
                + "table{border-collapse:collapse;table-layout:auto;min-width:720px;width:100%;font-size:13px}"
                + "th,td{border:1px solid " + border + ";padding:4px 5px;vertical-align:middle}"
                + "thead th{background:" + header + ";color:white;text-align:left;white-space:nowrap}"
                + ".position-head,.position-cell{width:1%;overflow-wrap:anywhere;word-break:break-word}"
                + ".base-head,th.base{min-width:3.5rem;max-width:6.25rem}"
                + ".condition-head,td.condition{min-width:3rem;max-width:7.5rem}"
                + "th.base{background:" + base + ";font-size:14px;text-align:center}"
                + "td.condition{text-align:center}.condition small{display:block;color:" + muted + "}"
                + ".target-heading{border-bottom:3px solid #D32913}"
                + "td.outcome{position:relative;width:8.5rem;min-width:8.5rem;padding:0;overflow:hidden;"
                + "white-space:nowrap;background:" + surface + "}"
                + ".outcome-bar{position:absolute;inset-block:0;right:0;width:max(4rem,var(--relative));"
                + "border-left:4px solid var(--accent);background:var(--surface);opacity:var(--bar-opacity)}"
                + ".outcome-bar.she{background:repeating-linear-gradient(-45deg,transparent 0 6px,"
                + "var(--stripe) 6px 10px),var(--surface)}"
                + ".outcome-content{position:relative;z-index:1;display:flex;min-height:1.72rem;"
                + "align-items:center;justify-content:flex-end;gap:.25rem;padding:.18rem .65rem;"
                + "opacity:var(--content-opacity)}"
                + ".she-badge{border:1px solid var(--accent);padding:0 .2rem;color:" + muted
                + ";font-size:9px}.count{color:" + muted + ";white-space:nowrap}"
                + ".examples{min-width:160px}.example{display:inline-flex;align-items:baseline;"
                + "margin:.05rem .25rem .05rem 0;line-height:1.35}.example-char{font-size:16px}"
                + ".example small{margin-left:2px;color:" + muted + "}"
                + ".example.coloured{position:relative;min-height:1.4rem;border:1px solid " + border
                + ";padding:0 .32rem .12rem;overflow:hidden;background:var(--example-surface)}"
                + ".example.coloured:after{position:absolute;right:0;bottom:0;left:0;height:2px;"
                + "background:var(--example-accent);content:''}"
                + ".no-results td{padding:32px;text-align:left;color:" + muted + "}";
    }

    private static String emptyAsNull(String value) {
        return value == null || value.isEmpty() ? "∅" : value;
    }

    private static String decimal(double value) {
        return String.format(Locale.US, "%.3f", value);
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
