package cc.ecisr.jyutdict;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import cc.ecisr.jyutdict.widget.SelectableTextView;
import cc.ecisr.jyutdict.utils.MotionUtil;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ResultItemAdapter extends RecyclerView.Adapter<ResultItemAdapter.LinearViewHolder> {
    private final Context mContext;
    private final iOnItemClickListener mListener;
    private final ArrayList<ResultInfo> items = new ArrayList<>();

    ResultItemAdapter(Context context, iOnItemClickListener listener) {
        this.mContext = context; // 主activity
        this.mListener = listener; // 提供給fragment的監聯器
    }

    @NonNull
    @Override
    public ResultItemAdapter.LinearViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == ResultInfo.TYPE_SHEET
                ? R.layout.layout_result_list_item_sheet
                : R.layout.layout_result_list_item;
        return new LinearViewHolder(LayoutInflater.from(mContext).inflate(layout, parent, false));
    }


    @Override
    public void onBindViewHolder(@NonNull ResultItemAdapter.LinearViewHolder holder, final int position) {
        ResultInfo item = items.get(position);
        Spanned header = item.chara;
        Spanned info = item.leftMiddle;
        Spanned extra = item.leftBottom;
        Spanned wanshyu = item.rightTop;
        Spanned location = item.rightBottom;

        holder.tvCharaHeader.setSelectableText(header);
        holder.tvCharaInfo.setSelectableText(info);
        boolean isSheetEntry = getItemViewType(position) == ResultInfo.TYPE_SHEET;
        holder.tvCharaExtra.setSelectableText(extra);
        holder.tvRightTop.setSelectableText(wanshyu);
        holder.tvRightBottom.setSelectableText(isSheetEntry
                ? joinTextSections(wanshyu, location)
                : location);
        holder.collapseAnnotation(false);
        holder.tvRightBottom.setOnAnnotationClickListener(holder::toggleAnnotation);
        int lyCharaVisibility = (!header.isEmpty() || !info.isEmpty()) ? View.VISIBLE : View.GONE;
        int tvContentInfoVisibility = (!info.isEmpty()) ? View.VISIBLE : View.GONE;
        int tvContentExtraVisibility = (!extra.isEmpty()) ? View.VISIBLE : View.GONE;
        int tvContentWanshyuVisibility = (!wanshyu.isEmpty()) ? View.VISIBLE : View.GONE;
        int tvContentLocationVisibility = (!location.isEmpty()) ? View.VISIBLE : View.GONE;
        boolean hasTopContent = !wanshyu.isEmpty()
                || (getItemViewType(position) == ResultInfo.TYPE_GENERAL && !extra.isEmpty());
        int dividerVisibility = (hasTopContent && !location.isEmpty())
                ? View.VISIBLE : View.GONE;
        holder.lyChara.setVisibility(lyCharaVisibility);
        holder.tvCharaInfo.setVisibility(tvContentInfoVisibility);
        holder.tvCharaExtra.setVisibility(tvContentExtraVisibility);
        holder.tvRightTop.setVisibility(isSheetEntry ? View.GONE : tvContentWanshyuVisibility);
        holder.tvRightBottom.setVisibility(isSheetEntry
                ? (wanshyu.isEmpty() && location.isEmpty() ? View.GONE : View.VISIBLE)
                : tvContentLocationVisibility);
        holder.contentDivider.setVisibility(isSheetEntry ? View.GONE : dividerVisibility);
        holder.contentMerged = isSheetEntry;

        holder.commentTarget = item.commentTarget;

        // 短按彈出操作菜單
        View.OnClickListener showItemMenu = v -> mListener.onClick(holder);
        holder.itemView.setOnClickListener(showItemMenu);
        holder.tvCharaHeader.setOnNonLinkClickListener(showItemMenu);
        holder.tvCharaInfo.setOnNonLinkClickListener(showItemMenu);
        holder.tvCharaExtra.setOnNonLinkClickListener(showItemMenu);
        holder.tvRightTop.setOnNonLinkClickListener(showItemMenu);
        holder.tvRightBottom.setOnNonLinkClickListener(showItemMenu);

        ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
        layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    void replaceItems(List<ResultInfo> newItems) {
        items.clear();
        items.addAll(newItems);
    }

    ResultInfo getItem(int position) {
        return items.get(position);
    }

    private static Spanned joinTextSections(Spanned first, Spanned second) {
        SpannableStringBuilder combined = new SpannableStringBuilder();
        if (first != null && !first.isEmpty()) {
            combined.append(first);
        }
        if (first != null && !first.isEmpty() && second != null && !second.isEmpty()) {
            combined.append("\n\n");
        }
        if (second != null && !second.isEmpty()) {
            combined.append(second);
        }
        return combined;
    }

    public static class LinearViewHolder extends RecyclerView.ViewHolder {
        LinearLayout lyChara;
        View contentDivider;
        View annotationContainer;
        SelectableTextView tvCharaHeader, tvCharaInfo, tvCharaExtra,
                tvRightTop, tvRightBottom, tvAnnotation;
        ResultInfo.CommentTarget commentTarget;
        String expandedAnnotation;
        boolean contentMerged;

        LinearViewHolder(@NonNull View itemView) {
            super(itemView);
            lyChara = itemView.findViewById(R.id.item_chara);

            tvCharaHeader = itemView.findViewById(R.id.chara_header);
            tvCharaInfo = itemView.findViewById(R.id.chara_info);
            tvCharaExtra = itemView.findViewById(R.id.chara_extra);
            tvRightTop = itemView.findViewById(R.id.content_wanshyu);
            tvRightBottom = itemView.findViewById(R.id.content_location);
            contentDivider = itemView.findViewById(R.id.content_divider);
            annotationContainer = itemView.findViewById(R.id.sheet_annotation_container);
            tvAnnotation = itemView.findViewById(R.id.sheet_annotation_text);
            if (annotationContainer != null) {
                annotationContainer.setOnClickListener(view -> collapseAnnotation(true));
            }
            if (tvAnnotation != null) {
                tvAnnotation.setOnClickListener(view -> collapseAnnotation(true));
            }
        }

        ResultInfo.CommentTarget getCommentTarget() {
            return commentTarget;
        }

        void toggleAnnotation(String annotation) {
            if (annotationContainer == null || tvAnnotation == null) return;
            if (annotationContainer.getVisibility() == View.VISIBLE
                    && annotation.equals(expandedAnnotation)) {
                collapseAnnotation(true);
                return;
            }
            expandedAnnotation = annotation;
            tvAnnotation.setSelectableText(annotation);
            beginAnnotationTransition();
            annotationContainer.setVisibility(View.VISIBLE);
        }

        void collapseAnnotation(boolean animate) {
            expandedAnnotation = null;
            if (animate) beginAnnotationTransition();
            if (annotationContainer != null) annotationContainer.setVisibility(View.GONE);
            if (tvAnnotation != null) tvAnnotation.setSelectableText("");
        }

        private void beginAnnotationTransition() {
            if (itemView.getParent() instanceof ViewGroup) {
                MotionUtil.beginLayoutTransition((ViewGroup) itemView.getParent());
            } else if (itemView instanceof ViewGroup) {
                MotionUtil.beginLayoutTransition((ViewGroup) itemView);
            }
        }

        String getChara() {
            return tvCharaHeader.getSelectablePlainText();
        }
        String printContent() {
            return tvCharaHeader.getSelectablePlainText() + "\n" +
                    tvCharaInfo.getSelectablePlainText() + "\n" +
                    tvCharaExtra.getSelectablePlainText() + "\n" +
                    (contentMerged ? "" : tvRightTop.getSelectablePlainText() + "\n") +
                    tvRightBottom.getSelectablePlainText() + "\n";
        }
    }

    public interface iOnItemClickListener {
        void onClick(@NonNull ResultItemAdapter.LinearViewHolder holder);
        void onComments(@NonNull ResultItemAdapter.LinearViewHolder holder,
                        String type, String target);
    }

    static class ResultInfo {
        static final int TYPE_GENERAL = 0;
        static final int TYPE_SHEET = 1;
        final Spanned chara;
        final Spanned leftMiddle;
        final Spanned leftBottom;
        final Spanned rightTop;
        final Spanned rightBottom;
        final int type;
        final CommentTarget commentTarget;

        ResultInfo(Spanned chara, Spanned leftMiddle, Spanned leftBottom,
                   Spanned rightTop, Spanned rightBottom, int type,
                   String commentType, String commentTarget) {
            this.chara = chara;
            this.leftMiddle = leftMiddle;
            this.leftBottom = leftBottom;
            this.rightTop = rightTop;
            this.rightBottom = rightBottom;
            this.type = type;
            this.commentTarget = commentType == null || commentTarget == null
                    ? null
                    : new CommentTarget(commentType, commentTarget);
        }

        static final class CommentTarget {
            final String type;
            final String target;
            int count;
            boolean countLoaded;

            CommentTarget(String type, String target) {
                this.type = type;
                this.target = target;
            }
        }
    }
}
