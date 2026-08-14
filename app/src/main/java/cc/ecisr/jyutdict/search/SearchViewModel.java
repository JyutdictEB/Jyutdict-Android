package cc.ecisr.jyutdict.search;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/** Owns the active request and its UI state across Activity recreation. */
public final class SearchViewModel extends ViewModel {
    private final SearchDataSource dataSource;
    private final MutableLiveData<SearchUiState> uiState =
            new MutableLiveData<>(SearchUiState.idle());

    public SearchViewModel() {
        this(new SearchRepository());
    }

    SearchViewModel(SearchDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public LiveData<SearchUiState> getUiState() {
        return uiState;
    }

    public void search(@NonNull SearchRequest request) {
        uiState.setValue(SearchUiState.loading(request));
        dataSource.search(request, new SearchDataSource.Callback() {
            @Override
            public void onSuccess(@NonNull String responseBody) {
                uiState.setValue(SearchUiState.success(request, responseBody));
            }

            @Override
            public void onFailure(@NonNull cc.ecisr.jyutdict.utils.HttpUtil.RequestError error) {
                uiState.setValue(SearchUiState.error(request, error));
            }
        });
    }

    @Override
    protected void onCleared() {
        dataSource.cancel();
    }
}
