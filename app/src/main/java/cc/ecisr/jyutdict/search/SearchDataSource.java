package cc.ecisr.jyutdict.search;

import androidx.annotation.NonNull;

import cc.ecisr.jyutdict.utils.HttpUtil;

interface SearchDataSource {
    interface Callback {
        void onSuccess(@NonNull String responseBody);

        void onFailure(@NonNull HttpUtil.RequestError error);
    }

    void search(@NonNull SearchRequest request, @NonNull Callback callback);

    void cancel();
}
