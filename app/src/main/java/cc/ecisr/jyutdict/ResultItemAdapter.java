package cc.ecisr.jyutdict;

import android.content.Context;
import android.graphics.Paint;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cc.ecisr.jyutdict.utils.MotionUtil;
import cc.ecisr.jyutdict.widget.CharacterHeaderSpan;
import cc.ecisr.jyutdict.widget.HorizontalDividerSpan;
import cc.ecisr.jyutdict.widget.SelectableTextView;

public class ResultItemAdapter extends RecyclerView.Adapter<ResultItemAdapter.LinearViewHolder> {
    private final OnItemClickListener listener;
    private final ArrayList<ResultInfo> items = new ArrayList<>();

    ResultItemAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public LinearViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == ResultInfo.TYPE_SHEET
                ? R.layout.layout_result_list_item_sheet : R.layout.layout_result_list_item;
        return new LinearViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull LinearViewHolder holder, int position) {
        ResultInfo item = items.get(position);
        holder.currentItem = item;
        holder.collapseAnnotation(false);

        if (item.type == ResultInfo.TYPE_SHEET) {
            holder.tvCharaHeader.setSelectableText(item.chara);
            holder.tvCharaInfo.setSelectableText(item.leftMiddle);
            holder.tvCharaExtra.setSelectableText(item.leftBottom);
            holder.tvRightTop.setSelectableText(item.rightTop);
            holder.tvRightTop.setVisibility(View.GONE);
            holder.tvRightBottom.setSelectableText(joinTextSections(item.rightTop, item.rightBottom));
            holder.tvRightBottom.setVisibility(empty(item.rightTop) && empty(item.rightBottom)
                    ? View.GONE : View.VISIBLE);
            holder.lyChara.setVisibility(!empty(item.chara) || !empty(item.leftMiddle)
                    ? View.VISIBLE : View.GONE);
            holder.tvCharaInfo.setVisibility(empty(item.leftMiddle) ? View.GONE : View.VISIBLE);
            holder.tvCharaExtra.setVisibility(empty(item.leftBottom) ? View.GONE : View.VISIBLE);
            holder.contentDivider.setVisibility(View.GONE);
        } else {
            holder.tvRightBottom.setSelectableText(formatGeneralEntry(holder.itemView.getContext(),
                    item.chara, item.leftBottom, item.rightTop, item.rightBottom));
            holder.tvRightBottom.setVisibility(empty(item.chara) && empty(item.leftBottom)
                    && empty(item.rightTop) && empty(item.rightBottom)
                    ? View.GONE : View.VISIBLE);
        }
        holder.tvRightBottom.setOnAnnotationClickListener(holder::toggleAnnotation);

        View.OnClickListener showMenu = view -> listener.onClick(holder);
        holder.itemView.setOnClickListener(showMenu);
        holder.setOnNonLinkClickListener(showMenu);
        holder.itemView.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
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
        SpannableStringBuilder result = new SpannableStringBuilder();
        if (!empty(first)) result.append(first);
        if (!empty(first) && !empty(second)) result.append("\n\n");
        if (!empty(second)) result.append(second);
        return result;
    }

    private static Spanned formatGeneralEntry(Context context, Spanned header, Spanned extra,
                                              Spanned wanshyu, Spanned location) {
        SpannableStringBuilder result = new SpannableStringBuilder();
        boolean hasHeader = !empty(header), hasExtra = !empty(extra);
        boolean hasWanshyu = !empty(wanshyu), hasLocation = !empty(location);
        boolean hasTop = hasHeader || hasExtra || hasWanshyu;

        if (hasHeader) {
            CharacterHeaderSpan headerSpan = new CharacterHeaderSpan(
                    context, !hasExtra && !hasWanshyu);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13,
                    context.getResources().getDisplayMetrics()));
            int headerWidth = headerSpan.getTotalWidth(
                    paint, header.toString(), 0, header.length());
            int headerStart = result.length();
            result.append(header);
            span(result, headerSpan, headerStart, result.length());

            if (hasExtra) {
                int extraStart = result.length();
                result.append(extra);
                span(result, new ForegroundColorSpan(ContextCompat.getColor(
                        context, R.color.colorPrimary)), extraStart, result.length());
                span(result, new LeadingMarginSpan.Standard(0, headerWidth),
                        headerStart, result.length());
                if (hasWanshyu) {
                    appendWanshyuParagraphs(result, wanshyu, headerStart, headerWidth, false);
                }
            } else if (hasWanshyu) {
                appendWanshyuParagraphs(result, wanshyu, headerStart, headerWidth, true);
            }
        } else {
            if (hasExtra) {
                int start = result.length();
                result.append(extra);
                span(result, new ForegroundColorSpan(ContextCompat.getColor(
                        context, R.color.colorPrimary)), start, result.length());
            }
            if (hasWanshyu) {
                if (result.length() > 0) result.append("\n");
                result.append(wanshyu);
            }
        }

        if (hasTop && hasLocation) {
            result.append("\n\u200B\n");
            span(result, new HorizontalDividerSpan(context),
                    result.length() - 2, result.length() - 1);
        }
        if (hasLocation) result.append(location);
        return result;
    }

    private static void appendWanshyuParagraphs(SpannableStringBuilder result, Spanned source,
                                                int headerStart, int headerWidth,
                                                boolean firstFollowsHeader) {
        String plain = source.toString();
        int cursor = 0;
        boolean first = true;
        while (cursor < source.length()) {
            int newline = plain.indexOf('\n', cursor);
            int end = newline < 0 ? source.length() : newline;
            CharSequence paragraph = source.subSequence(cursor, end);
            if (first && firstFollowsHeader) {
                result.append(paragraph);
                span(result, new LeadingMarginSpan.Standard(0, headerWidth),
                        headerStart, result.length());
                first = false;
            } else {
                result.append("\n");
                int start = result.length();
                result.append(paragraph);
                span(result, new LeadingMarginSpan.Standard(headerWidth, headerWidth),
                        start, result.length());
            }
            cursor = end + 1;
        }
    }

    private static void span(SpannableStringBuilder text, Object span, int start, int end) {
        text.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static boolean empty(CharSequence text) {
        return text.length() == 0;
    }

    public static class LinearViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout lyChara;
        final View contentDivider, annotationContainer;
        final SelectableTextView tvCharaHeader, tvCharaInfo, tvCharaExtra,
                tvRightTop, tvRightBottom, tvAnnotation;
        ResultInfo currentItem;
        String expandedAnnotation;

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

        void setOnNonLinkClickListener(View.OnClickListener listener) {
            SelectableTextView[] views = {tvCharaHeader, tvCharaInfo, tvCharaExtra,
                    tvRightTop, tvRightBottom};
            for (SelectableTextView view : views) {
                if (view != null) view.setOnNonLinkClickListener(listener);
            }
        }

        ResultInfo.CommentTarget getCommentTarget() {
            return currentItem.commentTarget;
        }

        CharSequence getWanshyuText() {
            return currentItem.rightTop.toString();
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
            return empty(currentItem.chara) ? "" : currentItem.chara.toString();
        }

        String printContent() {
            StringBuilder result = new StringBuilder();
            appendLine(result, currentItem.chara);
            appendLine(result, currentItem.leftMiddle);
            appendLine(result, currentItem.leftBottom);
            appendLine(result, currentItem.rightTop);
            if (!empty(currentItem.rightBottom)) {
                if (result.length() > 0) result.append("\n");
                appendLine(result, currentItem.rightBottom);
            }
            return result.toString();
        }

        private static void appendLine(StringBuilder output, CharSequence text) {
            if (text != null && text.length() > 0) output.append(text).append("\n");
        }
    }

    public interface OnItemClickListener {
        void onClick(@NonNull LinearViewHolder holder);
    }

    static class ResultInfo {
        static final int TYPE_GENERAL = 0, TYPE_SHEET = 1;
        final Spanned chara, leftMiddle, leftBottom, rightTop, rightBottom;
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
                    ? null : new CommentTarget(commentType, commentTarget);
        }

        static final class CommentTarget {
            final String type, target;
            int count;
            boolean countLoaded;

            CommentTarget(String type, String target) {
                this.type = type;
                this.target = target;
            }
        }
    }
}
