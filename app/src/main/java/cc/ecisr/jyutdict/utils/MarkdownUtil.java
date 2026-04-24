package cc.ecisr.jyutdict.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 转 HTML 工具类
 * 处理图片 URL 替换和主题适配
 */
public class MarkdownUtil {

    private static final String CSS_LIGHT =
            "<style>" +
            "body { font-family: sans-serif; padding: 16px; line-height: 1.6; color: #0F110C; background: #FAFAFA; }" +
            "h1, h2, h3, h4 { margin-top: 16px; margin-bottom: 8px; }" +
            "h2, h3 { line-height: 45px; color: rgb(48,48,48); }" +
            "h4 { line-height: 20px; color: rgb(48,48,48); }" +
            "p { text-indent: 2em; margin: 0 auto 5px auto; }" +
            "b { color: rgb(211, 41, 19); }" +
            "i { font-style: italic; }" +
            "hr { margin: 15px 0; border: none; border-top: 1px solid #ddd; }" +
            "a { color: rgb(0, 0, 255); }" +
            "a:visited { color: rgb(0, 0, 255); }" +
            ".cite { border-left: 5px solid rgb(211, 41, 19); padding: 0 10px 5px 15px; color: rgb(96,96,96); background: rgba(0,0,0,0.02); margin: 10px 0; }" +
            ".del { text-decoration: line-through; }" +
            ".copyright { text-align: center; }" +
            ".img { text-align: center; margin: 10px 0; }" +
            "img { max-width: 100%; height: auto; }" +
            "ul, ol { margin: 8px 0; padding-left: 1.5em; }" +
            "ul ul, ol ol, ul ol, ol ul { margin: 4px 0; }" +
            "li { margin: 4px 0; }" +
            "table { border-collapse: collapse; width: 100%; margin: 10px 0; }" +
            "th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }" +
            "th { background: #f5f5f5; }" +
            "ruby { ruby-align: center; }" +
            "rp { font-size: 0.8em; }" +
            "code { background: #f0f0f0; padding: 2px 6px; border-radius: 3px; font-family: monospace; color: #333; }" +
            "pre { background: #f5f5f5; padding: 12px; border-radius: 4px; overflow-x: auto; margin: 10px 0; }" +
            "pre code { background: none; padding: 0; }" +
            "</style>";

    private static final String CSS_DARK =
            "<style>" +
            "body { font-family: sans-serif; padding: 16px; line-height: 1.6; color: #E9E7EF; background: #202020; }" +
            "h1, h2, h3, h4 { margin-top: 16px; margin-bottom: 8px; }" +
            "h2, h3 { line-height: 45px; color: #E9E7EF; }" +
            "h4 { line-height: 20px; color: #E9E7EF; }" +
            "p { text-indent: 2em; margin: 0 auto 5px auto; }" +
            "b { color: rgb(211, 41, 19); }" +
            "i { font-style: italic; }" +
            "hr { margin: 15px 0; border: none; border-top: 1px solid #444; }" +
            "a { color: #6CB4EE; }" +
            "a:visited { color: #6CB4EE; }" +
            ".cite { border-left: 5px solid rgb(211, 41, 19); padding: 0 10px 5px 15px; color: #ABAAB1; background: rgba(255,255,255,0.05); margin: 10px 0; }" +
            ".del { text-decoration: line-through; }" +
            ".copyright { text-align: center; }" +
            ".img { text-align: center; margin: 10px 0; }" +
            "img { max-width: 100%; height: auto; }" +
            "ul, ol { margin: 8px 0; padding-left: 1.5em; }" +
            "ul ul, ol ol, ul ol, ol ul { margin: 4px 0; }" +
            "li { margin: 4px 0; }" +
            "table { border-collapse: collapse; width: 100%; margin: 10px 0; }" +
            "th, td { border: 1px solid #444; padding: 8px; text-align: left; }" +
            "th { background: #333; }" +
            "ruby { ruby-align: center; }" +
            "rp { font-size: 0.8em; }" +
            "code { background: #333; padding: 2px 6px; border-radius: 3px; font-family: monospace; color: #e0e0e0; }" +
            "pre { background: #2a2a2a; padding: 12px; border-radius: 4px; overflow-x: auto; margin: 10px 0; }" +
            "pre code { background: none; padding: 0; }" +
            "</style>";

    public static String toHtml(String markdown, String imagesBase, boolean isDarkMode) {
        if (markdown == null) markdown = "";
        String processed = markdown.replace("_IMG_BASE_", imagesBase);
        String htmlBody = convertMarkdownToHtml(processed);
        String css = isDarkMode ? CSS_DARK : CSS_LIGHT;
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">" +
               "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
               css + "</head><body>" + htmlBody + "</body></html>";
    }

    private static String convertMarkdownToHtml(String markdown) {
        String html = markdown;

        // 1. 保护大代码块 ```code```
        List<String> fencedCodeBlocks = new ArrayList<>();
        Pattern fencedPattern = Pattern.compile("```(.*?)```", Pattern.DOTALL);
        Matcher fencedMatcher = fencedPattern.matcher(html);
        StringBuffer sb1 = new StringBuffer();
        while (fencedMatcher.find()) {
            String code = fencedMatcher.group(1);
            if (code.startsWith("\r\n")) code = code.substring(2);
            else if (code.startsWith("\n")) code = code.substring(1);
            if (code.endsWith("\r\n")) code = code.substring(0, code.length() - 2);
            else if (code.endsWith("\n")) code = code.substring(0, code.length() - 1);
            fencedCodeBlocks.add(escapeHtml(code));
            fencedMatcher.appendReplacement(sb1, "\u0000FENCED" + (fencedCodeBlocks.size() - 1) + "\u0000");
        }
        fencedMatcher.appendTail(sb1);
        html = sb1.toString();

        // 2. 保护行内代码块 `code`
        List<String> inlineCodeBlocks = new ArrayList<>();
        Pattern inlinePattern = Pattern.compile("`([^`]+)`");
        Matcher inlineMatcher = inlinePattern.matcher(html);
        StringBuffer sb2 = new StringBuffer();
        while (inlineMatcher.find()) {
            String code = inlineMatcher.group(1);
            inlineCodeBlocks.add(escapeHtml(code));
            inlineMatcher.appendReplacement(sb2, "\u0000INLINE" + (inlineCodeBlocks.size() - 1) + "\u0000");
        }
        inlineMatcher.appendTail(sb2);
        html = sb2.toString();

        // 3. 处理标题（从高级别到低级别处理）
        html = html.replaceAll("(?m)^#### (.+)$", "<h4>$1</h4>");
        html = html.replaceAll("(?m)^### (.+)$", "<h3>$1</h3>");
        html = html.replaceAll("(?m)^## (.+)$", "<h2>$1</h2>");
        html = html.replaceAll("(?m)^# (.+)$", "<h1>$1</h1>");

        // 4. 处理粗体 **text**
        html = html.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");

        // 5. 处理斜体 *text*
        html = html.replaceAll("\\*([^*\\n]+?)\\*", "<i>$1</i>");

        // 6. 处理删除线 ~~text~~
        html = html.replaceAll("~~(.+?)~~", "<del>$1</del>");

        // 7. 处理水平线
        html = html.replaceAll("(?m)^---$", "<hr>");
        html = html.replaceAll("(?m)^\\*\\*\\*$", "<hr>");

        // 8. 处理链接 [text](url)
        html = html.replaceAll("\\[(.+?)\\]\\((.+?)\\)", "<a href=\"$2\">$1</a>");

        // 9. 处理带缩进的列表（最多支持3级缩进）
        // 格式：2/4空格 + */-/+ + 空格 + 内容
        html = html.replaceAll("(?m)^            [*+-] (.+)$", "<li indent=\"3\">$1</li>");
        html = html.replaceAll("(?m)^        [*+-] (.+)$", "<li indent=\"2\">$1</li>");
        html = html.replaceAll("(?m)^    [*+-] (.+)$", "<li indent=\"1\">$1</li>");
        html = html.replaceAll("(?m)^  [*+-] (.+)$", "<li indent=\"1\">$1</li>");

        // 10. 处理无缩进的列表
        html = html.replaceAll("(?m)^[*+-] (.+)$", "<li>$1</li>");

        // 11. 处理有序列表（支持缩进）
        html = html.replaceAll("(?m)^            \\d+\\. (.+)$", "<li indent=\"3\">$1</li>");
        html = html.replaceAll("(?m)^        \\d+\\. (.+)$", "<li indent=\"2\">$1</li>");
        html = html.replaceAll("(?m)^    \\d+\\. (.+)$", "<li indent=\"1\">$1</li>");
        html = html.replaceAll("(?m)^  \\d+\\. (.+)$", "<li indent=\"1\">$1</li>");
        html = html.replaceAll("(?m)^\\d+\\. (.+)$", "<li>$1</li>");

        // 12. 包装列表项为 <ul>
        html = wrapListItems(html);

        // 13. 处理换行
        html = html.replace("\r\n", "\n");
        html = html.replaceAll("\n{3,}", "\n\n");
        html = html.replaceAll("\n\n", "<br>");
        html = html.replace("\n", "<br>");

        // 14. 清理多余的 <br>
        html = html.replaceAll("<br><br>", "<br>");
        html = html.replaceAll("</h1><br>", "</h1>");
        html = html.replaceAll("</h2><br>", "</h2>");
        html = html.replaceAll("</h3><br>", "</h3>");
        html = html.replaceAll("</h4><br>", "</h4>");
        html = html.replaceAll("</ul><br>", "</ul>");
        html = html.replaceAll("<ul><br>", "<ul>");
        html = html.replaceAll("<hr><br>", "<hr>");
        html = html.replaceAll("^<br>", "");
        html = html.replaceAll("<br>$", "");

        // 15. 恢复行内代码块
        for (int j = 0; j < inlineCodeBlocks.size(); j++) {
            html = html.replace("\u0000INLINE" + j + "\u0000", "<code>" + inlineCodeBlocks.get(j) + "</code>");
        }

        // 16. 恢复大代码块
        for (int j = 0; j < fencedCodeBlocks.size(); j++) {
            String code = fencedCodeBlocks.get(j).replace("\n", "<br>");
            html = html.replace("\u0000FENCED" + j + "\u0000", "<pre><code>" + code + "</code></pre>");
        }

        return html;
    }

    /**
     * 将 <li> 标签包装为嵌套的 <ul>
     */
    private static String wrapListItems(String html) {
        StringBuilder result = new StringBuilder();
        String[] lines = html.split("\n");
        int currentIndent = -1;

        for (String line : lines) {
            if (line.contains("<li") && line.contains("</li>")) {
                int indent = 0;
                if (line.contains("indent=\"1\"")) indent = 1;
                else if (line.contains("indent=\"2\"")) indent = 2;
                else if (line.contains("indent=\"3\"")) indent = 3;

                // 移除 indent 属性
                line = line.replaceAll(" indent=\"\\d+\"", "");

                if (indent > currentIndent) {
                    // 需要开启新的一级
                    for (int i = currentIndent + 1; i <= indent; i++) {
                        result.append("<ul>");
                    }
                } else if (indent < currentIndent) {
                    // 需要关闭一些级别
                    for (int i = currentIndent; i > indent; i--) {
                        result.append("</ul>");
                    }
                }
                result.append(line);
                currentIndent = indent;
            } else {
                // 非列表行，关闭所有打开的列表
                while (currentIndent >= 0) {
                    result.append("</ul>");
                    currentIndent--;
                }
                result.append(line);
            }
        }

        // 结束时关闭所有打开的列表
        while (currentIndent >= 0) {
            result.append("</ul>");
            currentIndent--;
        }

        return result.toString();
    }

    private static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
