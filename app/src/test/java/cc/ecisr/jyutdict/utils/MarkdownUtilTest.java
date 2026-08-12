package cc.ecisr.jyutdict.utils;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MarkdownUtilTest {
    @Test
    public void rendersGfmTablesAndResponsiveViewport() {
        String html = MarkdownUtil.toHtml(
                "| 調類 | 調值 |\n| --- | --- |\n| 陰平 | 55 |",
                "https://jyutdict.org/img/",
                true
        );

        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("<th>調類</th>"));
        assertTrue(html.contains("<td>55</td>"));
        assertTrue(html.contains("width=device-width"));
    }
}
