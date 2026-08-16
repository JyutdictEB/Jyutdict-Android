package cc.ecisr.jyutdict;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayoutMediator;

import cc.ecisr.jyutdict.adapter.ArticlePagerAdapter;
import cc.ecisr.jyutdict.databinding.ActivityInfoBinding;
import cc.ecisr.jyutdict.utils.ImmersiveBarUtil;
import cc.ecisr.jyutdict.utils.ThemeUtil;

/** 伺服器三篇說明文章的只讀分頁；正文由 ArticleFragment 半持久化快取。 */
public class InfoActivity extends AppCompatActivity {
    private String[] articleTitles;
    private TabLayoutMediator tabMediator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeUtil.isNightMode(this) ? R.style.DarkTheme : R.style.AppTheme);
        super.onCreate(savedInstanceState);
        ActivityInfoBinding binding = ActivityInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ImmersiveBarUtil.setImmersiveBar(this, false, !ThemeUtil.isNightMode(this));
        ImmersiveBarUtil.applyToolbarInsets(binding.toolbar);
        ImmersiveBarUtil.applyBottomInsets(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        articleTitles = new String[]{getString(R.string.info_bundled_title),
                "泛粵典", "擴展粵拼", "聲調總表"};
        ArticlePagerAdapter pagerAdapter = new ArticlePagerAdapter(this,
                new String[]{ArticleFragment.BUNDLED_INFO_ID, "intro", "jpp", "tone"});
        binding.viewPager.setAdapter(pagerAdapter);
        binding.viewPager.setUserInputEnabled(true);
        binding.progressBar.setVisibility(View.GONE);
        binding.errorText.setVisibility(View.GONE);
        binding.viewPager.setVisibility(View.VISIBLE);
        binding.tabLayout.setVisibility(View.VISIBLE);

        tabMediator = new TabLayoutMediator(
                binding.tabLayout,
                binding.viewPager,
                (tab, position) -> tab.setText(articleTitles[position])
        );
        tabMediator.attach();
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
