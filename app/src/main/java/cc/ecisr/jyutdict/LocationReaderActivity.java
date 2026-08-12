package cc.ecisr.jyutdict;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONException;
import org.json.JSONObject;

import cc.ecisr.jyutdict.utils.ApiUrlBuilder;
import cc.ecisr.jyutdict.utils.DiskTextCache;
import cc.ecisr.jyutdict.utils.HttpUtil;
import cc.ecisr.jyutdict.utils.ImmersiveBarUtil;
import cc.ecisr.jyutdict.utils.MarkdownUtil;
import cc.ecisr.jyutdict.utils.PhonologyHtmlRenderer;
import cc.ecisr.jyutdict.utils.ThemeUtil;

/** 地點介紹與程序音系表共用的只讀頁。 */
public class LocationReaderActivity extends AppCompatActivity {
    private static final String URL_API_BASE = "https://jyutdict.org/api/v1.0/";
    private static final String EXTRA_MODE = "mode";
    private static final String EXTRA_LOCATION_ID = "location_id";
    private static final String EXTRA_LOCATION_NAME = "location_name";
    private static final String EXTRA_SHEET_STATISTIC = "sheet_statistic";
    private static final String MODE_ARTICLE = "article";
    private static final String MODE_PHONOLOGY = "phonology";
    private static final int LOAD_SUCCESS = 7401;
    private static final int LOAD_FAILURE = 7402;
    private static final long ARTICLE_CACHE_MAX_AGE = 24L * 60L * 60L * 1000L;
    private static final long PHONOLOGY_CACHE_FALLBACK_MAX_AGE =
            7L * 24L * 60L * 60L * 1000L;

    private Toolbar toolbar;
    private WebView webView;
    private ProgressBar progressBar;
    private TextView errorText;
    private final HttpUtil request = new HttpUtil(HttpUtil.GET);
    private Handler handler;
    private String mode;
    private String locationName;
    private String sheetStatistic;
    private int locationId;

    public static Intent articleIntent(Context context, String locationName) {
        return new Intent(context, LocationReaderActivity.class)
                .putExtra(EXTRA_MODE, MODE_ARTICLE)
                .putExtra(EXTRA_LOCATION_NAME, locationName);
    }

    public static Intent phonologyIntent(Context context, int locationId, String locationName,
                                         String sheetStatistic) {
        return new Intent(context, LocationReaderActivity.class)
                .putExtra(EXTRA_MODE, MODE_PHONOLOGY)
                .putExtra(EXTRA_LOCATION_ID, locationId)
                .putExtra(EXTRA_LOCATION_NAME, locationName)
                .putExtra(EXTRA_SHEET_STATISTIC, sheetStatistic);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtil.isNightMode(this) ? R.style.DarkTheme : R.style.AppTheme);
        setContentView(R.layout.activity_location_reader);
        ImmersiveBarUtil.setImmersiveBar(this, true, false);

        mode = getIntent().getStringExtra(EXTRA_MODE);
        locationName = getIntent().getStringExtra(EXTRA_LOCATION_NAME);
        sheetStatistic = getIntent().getStringExtra(EXTRA_SHEET_STATISTIC);
        locationId = getIntent().getIntExtra(EXTRA_LOCATION_ID, -1);
        if (locationName == null) locationName = "";
        if (sheetStatistic == null) sheetStatistic = "";

        toolbar = findViewById(R.id.toolbar);
        webView = findViewById(R.id.reader_web_view);
        progressBar = findViewById(R.id.reader_progress);
        errorText = findViewById(R.id.reader_error);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(
                    MODE_PHONOLOGY.equals(mode)
                            ? getString(R.string.location_phonology_title, locationName)
                            : getString(R.string.location_article_title, locationName)
            );
        }

        configureWebView();
        errorText.setOnClickListener(v -> load(true));
        handler = new Handler(Looper.getMainLooper(), msg -> {
            if (isFinishing() || isDestroyed()) return true;
            if (msg.what == LOAD_SUCCESS) {
                String raw = String.valueOf(msg.obj);
                DiskTextCache.write(this, cacheKey(), raw);
                showResponse(raw);
            } else {
                String stale = DiskTextCache.readAny(this, cacheKey());
                if (stale == null) {
                    showError(R.string.info_load_error_retry);
                } else {
                    showResponse(stale);
                }
            }
            return true;
        });
        load(false);
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(MODE_PHONOLOGY.equals(mode));
        settings.setDomStorageEnabled(false);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setUseWideViewPort(false);
        settings.setLoadWithOverviewMode(false);
        settings.setTextZoom(105);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    return true;
                }
                return false;
            }
        });
    }

    private void load(boolean forceNetwork) {
        progressBar.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);

        if (!forceNetwork) {
            long maxAge = MODE_PHONOLOGY.equals(mode)
                    ? (sheetStatistic.isEmpty()
                            ? PHONOLOGY_CACHE_FALLBACK_MAX_AGE
                            : Long.MAX_VALUE)
                    : ARTICLE_CACHE_MAX_AGE;
            String cached = DiskTextCache.readFresh(this, cacheKey(), maxAge);
            if (cached != null) {
                showResponse(cached);
                return;
            }
        }

        ApiUrlBuilder url;
        if (MODE_PHONOLOGY.equals(mode)) {
            if (locationId < 1) {
                showError(R.string.location_phonology_unavailable);
                return;
            }
            url = ApiUrlBuilder.from(URL_API_BASE, "phonology")
                    .add("area_id", locationId);
        } else {
            url = ApiUrlBuilder.from(URL_API_BASE, "articles/")
                    .add("location_name", locationName)
                    .add("type", "location");
        }
        request.setUrl(url.build())
                .setHandler(handler, LOAD_SUCCESS, LOAD_FAILURE)
                .start();
    }

    private String cacheKey() {
        return MODE_PHONOLOGY.equals(mode)
                ? "location_phonology_" + locationId + "_" + sheetStatistic
                : "location_article_" + locationName;
    }

    private void showResponse(String raw) {
        try {
            JSONObject response = new JSONObject(raw);
            String html;
            if (MODE_PHONOLOGY.equals(mode)) {
                html = PhonologyHtmlRenderer.render(response, ThemeUtil.isNightMode(this));
            } else {
                JSONObject article = response.optJSONObject("article");
                if (article == null || article.optString("content", "").isEmpty()) {
                    showError(R.string.location_article_unavailable);
                    return;
                }
                html = MarkdownUtil.toHtml(
                        article.optString("content", ""),
                        "https://jyutdict.org/",
                        ThemeUtil.isNightMode(this)
                );
            }
            webView.loadDataWithBaseURL(
                    "https://jyutdict.org/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
            );
            progressBar.setVisibility(View.GONE);
            errorText.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
        } catch (JSONException e) {
            showError(R.string.error_tips_data);
        }
    }

    private void showError(int message) {
        progressBar.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        request.cancel();
        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.stopLoading();
            webView.setWebViewClient(null);
            webView.destroy();
        }
        super.onDestroy();
    }
}
