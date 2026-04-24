package cc.ecisr.jyutdict;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cc.ecisr.jyutdict.adapter.ArticlePagerAdapter;
import cc.ecisr.jyutdict.struct.ArticleInfo;
import cc.ecisr.jyutdict.utils.HttpUtil;
import cc.ecisr.jyutdict.utils.ImmersiveBarUtil;
import cc.ecisr.jyutdict.utils.ThemeUtil;
import cc.ecisr.jyutdict.utils.ToastUtil;

/**
 * 信息页面 Activity
 * 通过 API 获取文章列表，使用 TabLayout + ViewPager2 展示
 */
public class InfoActivity extends AppCompatActivity {
    private static final String TAG = "`InfoActivity";
    private static final String URL_API_BASE = "https://jyutdict.org/api/v1.0/";

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ProgressBar progressBar;

    private List<ArticleInfo> articleList = new ArrayList<>();
    private ArticlePagerAdapter pagerAdapter;
    private InfoHandler handler;
    private HttpUtil httpUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyLightDarkTheme();
        setContentView(R.layout.activity_info);
        ImmersiveBarUtil.setImmersiveBar(this, true, false);

        initView();
        initHandler();
        fetchArticleList();
    }

    private void applyLightDarkTheme() {
        if (ThemeUtil.isNightMode(this)) {
            setTheme(R.style.DarkTheme);
        } else {
            setTheme(R.style.AppTheme);
        }
    }

    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        progressBar = findViewById(R.id.progress_bar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        pagerAdapter = new ArticlePagerAdapter(this, articleList);
        viewPager.setAdapter(pagerAdapter);
        // 禁用滑动手势翻页
        viewPager.setUserInputEnabled(false);
    }

    private void initHandler() {
        handler = new InfoHandler(Looper.getMainLooper(), msg -> {
            progressBar.setVisibility(View.GONE);
            if (msg.what == FETCH_ARTICLE_LIST) {
                parseArticleList(msg.obj.toString());
            } else if (msg.what == HttpUtil.REQUEST_CONTENT_FAIL) {
                String errorMsg = "0".equals(msg.obj.toString()) ?
                        getString(R.string.error_tips_network_out_of_time) :
                        getString(R.string.error_tips_network, msg.obj.toString());
                ToastUtil.msg(this, errorMsg);
            }
        });
        httpUtil = new HttpUtil(HttpUtil.GET);
    }

    private static final int FETCH_ARTICLE_LIST = 3601;

    private void fetchArticleList() {
        progressBar.setVisibility(View.VISIBLE);
        httpUtil.setUrl(URL_API_BASE + "articles/about")
                .setHandler(handler, FETCH_ARTICLE_LIST)
                .start();
    }

    private void parseArticleList(String json) {
        try {
            JSONObject response = new JSONObject(json);
            JSONArray pages = response.getJSONArray("pages");
            articleList.clear();
            for (int i = 0; i < pages.length(); i++) {
                articleList.add(ArticleInfo.fromJson(pages.getJSONObject(i)));
            }
            pagerAdapter.notifyDataSetChanged();

            new TabLayoutMediator(tabLayout, viewPager,
                    (tab, position) -> tab.setText(articleList.get(position).title)
            ).attach();

        } catch (JSONException e) {
            e.printStackTrace();
            ToastUtil.msg(this, getString(R.string.info_load_error));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private interface IHandleMessageProcessor {
        void handleMessage(Message msg);
    }

    private static class InfoHandler extends Handler {
        private final IHandleMessageProcessor processor;

        public InfoHandler(@NonNull Looper looper, IHandleMessageProcessor processor) {
            super(looper);
            this.processor = processor;
        }

        @Override
        public void handleMessage(@Nullable Message msg) {
            if (processor != null && msg != null) {
                processor.handleMessage(msg);
            }
        }
    }
}
