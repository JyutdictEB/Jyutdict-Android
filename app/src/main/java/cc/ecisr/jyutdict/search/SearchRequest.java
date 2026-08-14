package cc.ecisr.jyutdict.search;

import androidx.annotation.NonNull;

/** Immutable description of one API search. */
public final class SearchRequest {
    @NonNull
    public final String url;
    public final int responseMode;

    public SearchRequest(@NonNull String url, int responseMode) {
        if (url.isEmpty()) throw new IllegalArgumentException("url must not be empty");
        this.url = url;
        this.responseMode = responseMode;
    }
}
