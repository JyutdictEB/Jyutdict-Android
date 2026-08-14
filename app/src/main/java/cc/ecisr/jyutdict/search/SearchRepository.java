package cc.ecisr.jyutdict.search;

import androidx.annotation.NonNull;

import cc.ecisr.jyutdict.utils.HttpUtil;

/** Network boundary for searches. Parsing remains outside the transport layer. */
final class SearchRepository implements SearchDataSource {
    private final HttpUtil client = new HttpUtil(HttpUtil.GET);

    @Override
    public void search(@NonNull SearchRequest request, @NonNull Callback callback) {
        client.enqueue(request.url, new HttpUtil.Callback() {
            @Override
            public void onSuccess(String body) {
                callback.onSuccess(body);
            }

            @Override
            public void onFailure(HttpUtil.RequestError error) {
                callback.onFailure(error);
            }
        });
    }

    @Override
    public void cancel() {
        client.cancel();
    }
}
