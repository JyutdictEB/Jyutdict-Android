package cc.ecisr.jyutdict;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

import cc.ecisr.jyutdict.adapter.ArticlePagerAdapter;
import cc.ecisr.jyutdict.struct.ArticleInfo;
import cc.ecisr.jyutdict.utils.ImmersiveBarUtil;
import cc.ecisr.jyutdict.utils.ThemeUtil;

/**
 * 說明頁使用隨 APK 發佈的可信內容，避免後端 about 配置缺失導致整頁不可用。
 */
public class InfoActivity extends AppCompatActivity {
    private final List<ArticleInfo> articleList = new ArrayList<>();
    private TabLayoutMediator tabMediator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtil.isNightMode(this) ? R.style.DarkTheme : R.style.AppTheme);
        setContentView(R.layout.activity_info);
        ImmersiveBarUtil.setImmersiveBar(this, true, false);

        Toolbar toolbar = findViewById(R.id.toolbar);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        ProgressBar progressBar = findViewById(R.id.progress_bar);
        TextView errorText = findViewById(R.id.error_text);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ArticleInfo bundledInfo = new ArticleInfo();
        bundledInfo.id = ArticleFragment.BUNDLED_INFO_ID;
        bundledInfo.title = getString(R.string.info_bundled_title);
        articleList.add(bundledInfo);

        ArticlePagerAdapter pagerAdapter = new ArticlePagerAdapter(this, articleList);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setUserInputEnabled(false);
        progressBar.setVisibility(View.GONE);
        errorText.setVisibility(View.GONE);
        viewPager.setVisibility(View.VISIBLE);
        tabLayout.setVisibility(View.VISIBLE);

        tabMediator = new TabLayoutMediator(
                tabLayout,
                viewPager,
                (tab, position) -> tab.setText(articleList.get(position).title)
        );
        tabMediator.attach();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        if (tabMediator != null) tabMediator.detach();
        super.onDestroy();
    }
}
