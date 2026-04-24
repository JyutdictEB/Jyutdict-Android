package cc.ecisr.jyutdict.struct;

import org.json.JSONObject;

/**
 * 文章信息数据模型
 * 对应 API /api/v1.0/articles/about 返回的文章列表项
 */
public class ArticleInfo {
    public String id;           // 文章标识，如 "intro", "jpp", "tone"
    public String title;        // 文章标题，如 "本站", "擴展粵拼"
    public String file;         // 文件名，如 "about.md"
    public String content;      // Markdown 内容（延迟加载）
    public boolean isLoaded;    // 内容是否已加载

    public ArticleInfo() {
        this.id = "";
        this.title = "";
        this.file = "";
        this.content = "";
        this.isLoaded = false;
    }

    /**
     * 从 JSON 对象解析文章信息
     * API 返回格式: {"id": "intro", "title": "本站", "file": "about.md"}
     */
    public static ArticleInfo fromJson(JSONObject json) {
        ArticleInfo info = new ArticleInfo();
        info.id = json.optString("id", "");
        info.title = json.optString("title", "");
        info.file = json.optString("file", "");
        info.isLoaded = false;
        return info;
    }
}
