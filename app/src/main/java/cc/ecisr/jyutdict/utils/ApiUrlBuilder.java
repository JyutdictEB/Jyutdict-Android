package cc.ecisr.jyutdict.utils;

import android.net.Uri;

/**
 * 統一構造 API URL，避免查詢內容中的空格、&、+、# 或正則字符破壞參數。
 */
public final class ApiUrlBuilder {
    private final Uri.Builder builder;

    private ApiUrlBuilder(String baseUrl, String endpoint) {
        builder = Uri.parse(baseUrl).buildUpon();
        if (endpoint != null) {
            for (String segment : endpoint.split("/")) {
                if (!segment.isEmpty()) builder.appendPath(segment);
            }
        }
    }

    public static ApiUrlBuilder from(String baseUrl, String endpoint) {
        return new ApiUrlBuilder(baseUrl, endpoint);
    }

    public ApiUrlBuilder add(String name, Object value) {
        if (name == null || name.isEmpty() || value == null) return this;
        builder.appendQueryParameter(name, String.valueOf(value));
        return this;
    }

    public String build() {
        return builder.build().toString();
    }
}
