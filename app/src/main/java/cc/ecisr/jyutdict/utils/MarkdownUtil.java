package cc.ecisr.jyutdict.utils;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.Collections;
import java.util.List;

/** 使用 CommonMark + GFM 表格渲染只讀文章。 */
public final class MarkdownUtil {
    private static final List<Extension> EXTENSIONS =
            Collections.singletonList(TablesExtension.create());
    private static final Parser PARSER = Parser.builder()
            .extensions(EXTENSIONS)
            .build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder()
            .extensions(EXTENSIONS)
            .softbreak("<br>\n")
            .build();

    private MarkdownUtil() {
    }

    public static String toHtml(String markdown, String imagesBase, boolean darkMode) {
        String base = imagesBase == null ? "" : imagesBase;
        String slashBase = base.endsWith("/") ? base : base + "/";
        String processed = (markdown == null ? "" : markdown)
                .replace("_IMG_BASE_/", slashBase)
                .replace("_IMG_BASE_", slashBase);
        Node document = PARSER.parse(processed);
        String body = RENDERER.render(document);
        return "<!doctype html><html><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=5\">"
                + style(darkMode)
                + "</head><body><article>" + body + "</article></body></html>";
    }

    private static String style(boolean dark) {
        String background = dark ? "#1A1110" : "#FFF8F6";
        String foreground = dark ? "#F1DEDB" : "#231918";
        String muted = dark ? "#D8C2BE" : "#534341";
        String border = dark ? "#534341" : "#D8C2BE";
        String surface = dark ? "#271D1C" : "#FFF1EE";
        String header = dark ? "#3D3230" : "#F1DEDB";
        String link = dark ? "#FFB4A8" : "#9B2F24";
        String code = dark ? "#322826" : "#FCEAE7";
        return "<style>"
                + "*{box-sizing:border-box}html{-webkit-text-size-adjust:100%;text-size-adjust:100%}"
                + "body{width:auto;max-width:100%;margin:0;padding:20px 20px 40px;"
                + "font-family:system-ui,sans-serif;font-size:16px;line-height:1.7;overflow-wrap:anywhere;"
                + "color:" + foreground + ";background:" + background + "}"
                + "article{width:100%;max-width:760px;margin:0 auto}"
                + "h1,h2,h3,h4{font-family:serif;line-height:1.35;margin:1.5em 0 .6em}"
                + "h1{font-size:1.7em}h2{font-size:1.4em;border-bottom:1px solid " + border
                + ";padding-bottom:.3em}h3{font-size:1.2em}h4{font-size:1.08em}"
                + "p{margin:.5em 0 1em;text-indent:2em}li p,blockquote p,td p,th p{"
                + "text-indent:0;margin:.25em 0}"
                + "strong{color:#D32913}em{color:" + muted + "}"
                + "a{color:" + link + ";text-decoration:underline}"
                + "blockquote{margin:1em 0;border-left:4px solid #9B2F24;padding:.7em 1em;"
                + "border-radius:0 12px 12px 0;color:" + muted + ";background:" + surface + "}"
                + "img{display:block;max-width:100%;height:auto;margin:1em auto}"
                + "ul,ol{padding-left:1.6em}li{margin:.25em 0}"
                + "table{display:block;width:max-content;min-width:100%;max-width:100%;"
                + "overflow-x:auto;border-collapse:collapse;margin:1em 0;background:" + surface + "}"
                + "th,td{min-width:5em;border:1px solid " + border
                + ";padding:.45em .6em;text-align:left;vertical-align:top;white-space:normal}"
                + "th{background:" + header + ";font-weight:bold}"
                + "code{padding:.12em .3em;background:" + code + ";font-family:monospace}"
                + "pre{max-width:100%;overflow-x:auto;padding:12px;background:" + code + "}"
                + "pre code{padding:0;background:transparent}"
                + "hr{border:0;border-top:1px solid " + border + ";margin:1.4em 0}"
                + ".cite{border-left:5px solid #D32913;padding:.25em 1em;color:" + muted + "}"
                + "</style>";
    }
}
