package cc.ecisr.jyutdict;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONException;
import org.json.JSONObject;

import cc.ecisr.jyutdict.utils.HttpUtil;
import cc.ecisr.jyutdict.utils.MarkdownUtil;
import cc.ecisr.jyutdict.utils.ThemeUtil;

/**
 * 文章展示 Fragment
 * 每个 Fragment 独立管理自己的加载状态
 */
public class ArticleFragment extends Fragment {
    private static final String ARG_ARTICLE_ID = "article_id";
    private static final String URL_API_BASE = "https://jyutdict.org/api/v1.0/";

    private WebView webView;
    private ProgressBar progressBar;
    private TextView errorText;
    private String articleId;

    // 每个 Fragment 自己的加载状态
    private boolean isLoading = false;
    private boolean isLoaded = false;
    private String loadedContent = "";
    private String loadedImagesBase = "";

    public static ArticleFragment newInstance(String articleId) {
        ArticleFragment fragment = new ArticleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ARTICLE_ID, articleId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        articleId = getArguments() != null ? getArguments().getString(ARG_ARTICLE_ID) : "";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_article, container, false);
        webView = view.findViewById(R.id.web_view);
        progressBar = view.findViewById(R.id.progress_bar);
        errorText = view.findViewById(R.id.error_text);

        configureWebView();

        // 如果已加载过，直接显示
        if (isLoaded) {
            showContent(loadedContent, loadedImagesBase);
        } else {
            loadContent();
        }
        return view;
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setDomStorageEnabled(false);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (getActivity() != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                }
                return true;
            }
        });
    }

    private void loadContent() {
        if (isLoading || articleId == null || articleId.isEmpty()) return;

        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        errorText.setVisibility(View.GONE);

        // 创建新的 HttpUtil 实例，避免复用
        HttpUtil httpUtil = new HttpUtil(HttpUtil.GET);
        httpUtil.setUrl(URL_API_BASE + "articles/about?id=" + articleId)
                .setHandler(new Handler(Looper.getMainLooper(), msg -> {
                    isLoading = false;

                    if (getContext() == null) return true;  // Fragment 已销毁

                    if (msg.what == HttpUtil.REQUEST_CONTENT_SUCCESSFULLY) {
                        try {
                            JSONObject response = new JSONObject(msg.obj.toString());
                            loadedContent = response.optString("content", "");
                            loadedImagesBase = response.optString("images_base", "");
                            isLoaded = true;
                            showContent(loadedContent, loadedImagesBase);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            showError();
                        }
                    } else {
                        showError();
                    }
                    return true;
                }))
                .start();
    }

    private void showContent(String markdown, String imagesBase) {
        if (getActivity() == null || webView == null) return;

        boolean isDarkMode = ThemeUtil.isNightMode(getActivity());
        String html = MarkdownUtil.toHtml(markdown, imagesBase, isDarkMode);
        webView.loadDataWithBaseURL(imagesBase, html, "text/html", "UTF-8", null);

        progressBar.setVisibility(View.GONE);
        errorText.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
    }

    private void showError() {
        if (progressBar == null) return;
        progressBar.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        errorText.setVisibility(View.VISIBLE);
        errorText.setText(R.string.info_load_error);
    }

    @Override
    public void onDestroyView() {
        isLoading = false;  // 取消加载
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
