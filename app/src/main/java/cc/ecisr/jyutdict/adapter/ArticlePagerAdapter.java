package cc.ecisr.jyutdict.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import cc.ecisr.jyutdict.ArticleFragment;

/** Stable-ID pager for the fixed information article list. */
public class ArticlePagerAdapter extends FragmentStateAdapter {
    private final String[] articleIds;

    public ArticlePagerAdapter(@NonNull FragmentActivity activity, String[] articleIds) {
        super(activity);
        this.articleIds = articleIds.clone();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return ArticleFragment.newInstance(articleIds[position]);
    }

    @Override
    public int getItemCount() {
        return articleIds.length;
    }

}
