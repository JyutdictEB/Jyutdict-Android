package cc.ecisr.jyutdict.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

import cc.ecisr.jyutdict.ArticleFragment;
import cc.ecisr.jyutdict.struct.ArticleInfo;

/**
 * ViewPager2 适配器
 * 使用文章 ID 作为 Fragment 标识
 */
public class ArticlePagerAdapter extends FragmentStateAdapter {
    private List<ArticleInfo> articleList;

    public ArticlePagerAdapter(@NonNull FragmentActivity activity, List<ArticleInfo> articleList) {
        super(activity);
        this.articleList = articleList;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        String articleId = articleList.get(position).id;
        return ArticleFragment.newInstance(articleId);
    }

    @Override
    public int getItemCount() {
        return articleList != null ? articleList.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        return articleList.get(position).id.hashCode();
    }

    @Override
    public boolean containsItem(long itemId) {
        for (ArticleInfo info : articleList) {
            if (info.id.hashCode() == itemId) {
                return true;
            }
        }
        return false;
    }
}
