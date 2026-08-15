package cc.ecisr.jyutdict.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import cc.ecisr.jyutdict.utils.HttpUtil;

/** Owns the active request and its UI state across Activity recreation. */
public final class SearchViewModel extends ViewModel {
    public enum Status { IDLE, LOADING, SUCCESS, ERROR }

    public static final class Request {
        @NonNull public final String url;
        public final int responseMode;

        public Request(@NonNull String url, int responseMode) {
            if (url.isEmpty()) throw new IllegalArgumentException("url must not be empty");
            this.url = url;
            this.responseMode = responseMode;
        }
    }

    public static final class State {
        @NonNull public final Status status;
        @Nullable public final Request request;
        @Nullable public final String responseBody;
        @Nullable public final HttpUtil.RequestError error;

        private State(@NonNull Status status, @Nullable Request request,
                      @Nullable String responseBody, @Nullable HttpUtil.RequestError error) {
            this.status = status;
            this.request = request;
            this.responseBody = responseBody;
            this.error = error;
        }
    }

    interface Client {
        void search(@NonNull String url, @NonNull HttpUtil.Callback callback);
        void cancel();
    }

    private final Client client;
    private final MutableLiveData<State> uiState =
            new MutableLiveData<>(state(Status.IDLE, null, null, null));

    public SearchViewModel() {
        this(new Client() {
            private final HttpUtil request = new HttpUtil(HttpUtil.GET);

            @Override
            public void search(@NonNull String url, @NonNull HttpUtil.Callback callback) {
                request.enqueue(url, callback);
            }

            @Override
            public void cancel() {
                request.cancel();
            }
        });
    }

    SearchViewModel(Client client) {
        this.client = client;
    }

    public LiveData<State> getUiState() {
        return uiState;
    }

    public void search(@NonNull Request request) {
        uiState.setValue(state(Status.LOADING, request, null, null));
        client.search(request.url, new HttpUtil.Callback() {
            @Override
            public void onSuccess(@NonNull String responseBody) {
                uiState.setValue(state(Status.SUCCESS, request, responseBody, null));
            }

            @Override
            public void onFailure(@NonNull HttpUtil.RequestError error) {
                uiState.setValue(state(Status.ERROR, request, null, error));
            }
        });
    }

    private static State state(Status status, Request request, String body,
                               HttpUtil.RequestError error) {
        return new State(status, request, body, error);
    }

    @Override
    protected void onCleared() {
        client.cancel();
    }
}
