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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import cc.ecisr.jyutdict.utils.ApiUrlBuilder;
import cc.ecisr.jyutdict.utils.HttpUtil;
import cc.ecisr.jyutdict.utils.MarkdownUtil;
import cc.ecisr.jyutdict.utils.ThemeUtil;

/**
 * 文章展示 Fragment
 * 每个 Fragment 独立管理自己的加载状态
 */
public class ArticleFragment extends Fragment {
    public static final String BUNDLED_INFO_ID = "__bundled_info__";
    private static final String ARG_ARTICLE_ID = "article_id";
    private static final String URL_API_BASE = "https://jyutdict.org/api/v1.0/";

    private WebView webView;
    private ProgressBar progressBar;
    private TextView errorText;
    private String articleId;
    private HttpUtil contentRequest;
    private Handler contentHandler;
    private int viewGeneration = 0;

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
        viewGeneration++;
        errorText.setOnClickListener(v -> loadContent());

        configureWebView();

        if (BUNDLED_INFO_ID.equals(articleId)) {
            loadBundledInfo();
            return view;
        }

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
        settings.setAllowFileAccess(BUNDLED_INFO_ID.equals(articleId));
        settings.setAllowContentAccess(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                String scheme = uri.getScheme();
                if (getActivity() != null
                        && ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        });
    }

    private void loadBundledInfo() {
        if (getActivity() == null || webView == null) return;
        try (InputStream input = getActivity().getAssets().open("info/info.html");
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(input, StandardCharsets.UTF_16BE))) {
            StringBuilder html = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                html.append(line).append('\n');
            }
            if (ThemeUtil.isNightMode(getActivity())) {
                html.insert(
                        html.indexOf("</style>"),
                        "body{background:#202020;color:#E9E7EF}" +
                                "h2,h3,h4{color:#E9E7EF}.cite{color:#ABAAB1}"
                );
            }
            webView.loadDataWithBaseURL(
                    "file:///android_asset/info/",
                    html.toString(),
                    "text/html",
                    "UTF-8",
                    null
            );
            progressBar.setVisibility(View.GONE);
            errorText.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
        } catch (IOException | RuntimeException e) {
            showError();
        }
    }

    private void loadContent() {
        if (isLoading || articleId == null || articleId.isEmpty()) return;

        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        errorText.setVisibility(View.GONE);

        final int requestViewGeneration = viewGeneration;
        if (contentRequest != null) contentRequest.cancel();
        if (contentHandler != null) contentHandler.removeCallbacksAndMessages(null);
        contentRequest = new HttpUtil(HttpUtil.GET);
        contentHandler = new Handler(Looper.getMainLooper(), msg -> {
                    isLoading = false;

                    if (requestViewGeneration != viewGeneration
                            || !isAdded()
                            || webView == null
                            || progressBar == null
                            || errorText == null) {
                        return true;
                    }

                    if (msg.what == HttpUtil.REQUEST_CONTENT_SUCCESSFULLY) {
                        try {
                            JSONObject response = new JSONObject(msg.obj.toString());
                            loadedContent = response.optString("content", "");
                            loadedImagesBase = response.optString("images_base", "");
                            if (loadedContent.isEmpty()) {
                                showError();
                                return true;
                            }
                            isLoaded = true;
                            showContent(loadedContent, loadedImagesBase);
                        } catch (JSONException e) {
                            showError();
                        }
                    } else {
                        showError();
                    }
                    return true;
                });
        contentRequest.setUrl(ApiUrlBuilder.from(URL_API_BASE, "articles/about")
                        .add("id", articleId)
                        .build())
                .setHandler(contentHandler)
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
        errorText.setText(R.string.info_load_error_retry);
    }

    @Override
    public void onDestroyView() {
        viewGeneration++;
        isLoading = false;
        if (contentRequest != null) {
            contentRequest.cancel();
            contentRequest = null;
        }
        if (contentHandler != null) {
            contentHandler.removeCallbacksAndMessages(null);
            contentHandler = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.setWebViewClient(null);
            webView.destroy();
        }
        webView = null;
        progressBar = null;
        errorText = null;
        super.onDestroyView();
    }
}
