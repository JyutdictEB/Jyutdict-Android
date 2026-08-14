package cc.ecisr.jyutdict.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cc.ecisr.jyutdict.utils.HttpUtil;

/** Complete screen state for the lifecycle-aware search request. */
public final class SearchUiState {
    public enum Status {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    @NonNull
    public final Status status;
    @Nullable
    public final SearchRequest request;
    @Nullable
    public final String responseBody;
    @Nullable
    public final HttpUtil.RequestError error;

    private SearchUiState(
            @NonNull Status status,
            @Nullable SearchRequest request,
            @Nullable String responseBody,
            @Nullable HttpUtil.RequestError error
    ) {
        this.status = status;
        this.request = request;
        this.responseBody = responseBody;
        this.error = error;
    }

    public static SearchUiState idle() {
        return new SearchUiState(Status.IDLE, null, null, null);
    }

    public static SearchUiState loading(@NonNull SearchRequest request) {
        return new SearchUiState(Status.LOADING, request, null, null);
    }

    public static SearchUiState success(
            @NonNull SearchRequest request,
            @NonNull String responseBody
    ) {
        return new SearchUiState(Status.SUCCESS, request, responseBody, null);
    }

    public static SearchUiState error(
            @NonNull SearchRequest request,
            @NonNull HttpUtil.RequestError error
    ) {
        return new SearchUiState(Status.ERROR, request, null, error);
    }
}
