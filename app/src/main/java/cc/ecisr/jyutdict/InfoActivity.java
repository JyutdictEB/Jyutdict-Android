package cc.ecisr.jyutdict;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

import cc.ecisr.jyutdict.adapter.ArticlePagerAdapter;
import cc.ecisr.jyutdict.databinding.ActivityInfoBinding;
import cc.ecisr.jyutdict.struct.ArticleInfo;
import cc.ecisr.jyutdict.utils.ImmersiveBarUtil;
import cc.ecisr.jyutdict.utils.ThemeUtil;

/** 伺服器三篇說明文章的只讀分頁；正文由 ArticleFragment 半持久化快取。 */
public class InfoActivity extends AppCompatActivity {
    private final List<ArticleInfo> articleList = new ArrayList<>();
    private TabLayoutMediator tabMediator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeUtil.isNightMode(this) ? R.style.DarkTheme : R.style.AppTheme);
        super.onCreate(savedInstanceState);
        ActivityInfoBinding binding = ActivityInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        boolean lightSystemBars = !ThemeUtil.isNightMode(this);
        ImmersiveBarUtil.setImmersiveBar(this, lightSystemBars, lightSystemBars);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        articleList.add(article(
                ArticleFragment.BUNDLED_INFO_ID,
                getString(R.string.info_bundled_title)
        ));
        articleList.add(article("intro", "泛粵典"));
        articleList.add(article("jpp", "擴展粵拼"));
        articleList.add(article("tone", "聲調總表"));

        ArticlePagerAdapter pagerAdapter = new ArticlePagerAdapter(this, articleList);
        binding.viewPager.setAdapter(pagerAdapter);
        binding.viewPager.setUserInputEnabled(false);
        binding.viewPager.setPageTransformer((page, position) -> {
            float distance = Math.min(1f, Math.abs(position));
            page.setAlpha(1f - 0.18f * distance);
            page.setScaleX(1f - 0.015f * distance);
            page.setScaleY(1f - 0.015f * distance);
            page.setTranslationX(-position * page.getWidth() * 0.06f);
        });
        binding.progressBar.setVisibility(View.GONE);
        binding.errorText.setVisibility(View.GONE);
        binding.viewPager.setVisibility(View.VISIBLE);
        binding.tabLayout.setVisibility(View.VISIBLE);

        tabMediator = new TabLayoutMediator(
                binding.tabLayout,
                binding.viewPager,
                (tab, position) -> tab.setText(articleList.get(position).title)
        );
        tabMediator.attach();
    }

    private static ArticleInfo article(String id, String title) {
        ArticleInfo info = new ArticleInfo();
        info.id = id;
        info.title = title;
        return info;
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        if (tabMediator != null) tabMediator.detach();
        super.onDestroy();
    }
}
