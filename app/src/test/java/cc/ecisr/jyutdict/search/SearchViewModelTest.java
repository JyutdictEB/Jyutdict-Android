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
        FakeClient dataSource = new FakeClient();
        SearchViewModel viewModel = new SearchViewModel(dataSource);
        SearchViewModel.Request request =
                new SearchViewModel.Request("https://example.test/search", 17);

        viewModel.search(request);

        SearchViewModel.State loading = viewModel.getUiState().getValue();
        assertNotNull(loading);
        assertEquals(SearchViewModel.Status.LOADING, loading.status);
        assertSame(request, loading.request);

        dataSource.succeed("[]");

        SearchViewModel.State success = viewModel.getUiState().getValue();
        assertNotNull(success);
        assertEquals(SearchViewModel.Status.SUCCESS, success.status);
        assertSame(request, success.request);
        assertEquals("[]", success.responseBody);
    }

    @Test
    public void failureIsRepresentedInUiState() {
        FakeClient dataSource = new FakeClient();
        SearchViewModel viewModel = new SearchViewModel(dataSource);
        SearchViewModel.Request request =
                new SearchViewModel.Request("https://example.test/search", 23);

        viewModel.search(request);
        HttpUtil.RequestError error = new HttpUtil.RequestError(
                HttpUtil.ErrorKind.TIMEOUT, 0, "timeout");
        dataSource.fail(error);

        SearchViewModel.State failure = viewModel.getUiState().getValue();
        assertNotNull(failure);
        assertEquals(SearchViewModel.Status.ERROR, failure.status);
        assertSame(request, failure.request);
        assertSame(error, failure.error);
    }

    private static final class FakeClient implements SearchViewModel.Client {
        private HttpUtil.Callback callback;

        @Override
        public void search(String url, HttpUtil.Callback callback) {
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
