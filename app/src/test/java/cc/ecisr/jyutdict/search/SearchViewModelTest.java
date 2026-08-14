package cc.ecisr.jyutdict.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Rule;
import org.junit.Test;

import cc.ecisr.jyutdict.utils.HttpUtil;

public class SearchViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule =
            new InstantTaskExecutorRule();

    @Test
    public void searchPublishesLoadingThenSuccess() {
        FakeSearchDataSource dataSource = new FakeSearchDataSource();
        SearchViewModel viewModel = new SearchViewModel(dataSource);
        SearchRequest request = new SearchRequest("https://example.test/search", 17);

        viewModel.search(request);

        SearchUiState loading = viewModel.getUiState().getValue();
        assertNotNull(loading);
        assertEquals(SearchUiState.Status.LOADING, loading.status);
        assertSame(request, loading.request);

        dataSource.succeed("[]");

        SearchUiState success = viewModel.getUiState().getValue();
        assertNotNull(success);
        assertEquals(SearchUiState.Status.SUCCESS, success.status);
        assertSame(request, success.request);
        assertEquals("[]", success.responseBody);
    }

    @Test
    public void failureIsRepresentedInUiState() {
        FakeSearchDataSource dataSource = new FakeSearchDataSource();
        SearchViewModel viewModel = new SearchViewModel(dataSource);
        SearchRequest request = new SearchRequest("https://example.test/search", 23);

        viewModel.search(request);
        HttpUtil.RequestError error = new HttpUtil.RequestError(
                HttpUtil.ErrorKind.TIMEOUT, 0, "timeout");
        dataSource.fail(error);

        SearchUiState failure = viewModel.getUiState().getValue();
        assertNotNull(failure);
        assertEquals(SearchUiState.Status.ERROR, failure.status);
        assertSame(request, failure.request);
        assertSame(error, failure.error);
    }

    private static final class FakeSearchDataSource implements SearchDataSource {
        private Callback callback;

        @Override
        public void search(SearchRequest request, Callback callback) {
            this.callback = callback;
        }

        @Override
        public void cancel() {
        }

        void succeed(String responseBody) {
            callback.onSuccess(responseBody);
        }

        void fail(HttpUtil.RequestError error) {
            callback.onFailure(error);
        }
    }
}
