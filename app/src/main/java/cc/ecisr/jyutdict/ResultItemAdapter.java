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

import cc.ecisr.jyutdict.utils.ColorUtil;
import cc.ecisr.jyutdict.utils.MotionUtil;
import cc.ecisr.jyutdict.widget.CharacterHeaderSpan;
import cc.ecisr.jyutdict.widget.HorizontalDividerSpan;
import cc.ecisr.jyutdict.widget.SelectableTextView;

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
        boolean isSheetEntry = getItemViewType(position) == ResultInfo.TYPE_SHEET;

        holder.currentItem = item;
        holder.contentMerged = true;
        holder.commentTarget = item.commentTarget;
        holder.collapseAnnotation(false);

        if (isSheetEntry) {
            if (holder.tvCharaHeader != null) holder.tvCharaHeader.setSelectableText(header);
            if (holder.tvCharaInfo != null) holder.tvCharaInfo.setSelectableText(info);
            if (holder.tvCharaExtra != null) holder.tvCharaExtra.setSelectableText(extra);
            if (holder.tvRightTop != null) {
                holder.tvRightTop.setSelectableText(wanshyu);
                holder.tvRightTop.setVisibility(View.GONE);
            }
            if (holder.tvRightBottom != null) {
                holder.tvRightBottom.setSelectableText(joinTextSections(wanshyu, location));
                holder.tvRightBottom.setVisibility(
                        wanshyu.isEmpty() && location.isEmpty() ? View.GONE : View.VISIBLE);
                holder.tvRightBottom.setOnAnnotationClickListener(holder::toggleAnnotation);
            }
            if (holder.lyChara != null) {
                int lyCharaVisibility = (!header.isEmpty() || !info.isEmpty()) ? View.VISIBLE : View.GONE;
                holder.lyChara.setVisibility(lyCharaVisibility);
            }
            if (holder.tvCharaInfo != null) {
                holder.tvCharaInfo.setVisibility(!info.isEmpty() ? View.VISIBLE : View.GONE);
            }
            if (holder.tvCharaExtra != null) {
                holder.tvCharaExtra.setVisibility(!extra.isEmpty() ? View.VISIBLE : View.GONE);
            }
            if (holder.contentDivider != null) {
                holder.contentDivider.setVisibility(View.GONE);
            }
        } else {
            if (holder.tvCharaHeader != null) holder.tvCharaHeader.setSelectableText("");
            if (holder.tvCharaInfo != null) holder.tvCharaInfo.setSelectableText("");
            if (holder.tvCharaExtra != null) holder.tvCharaExtra.setSelectableText("");
            if (holder.tvRightTop != null) holder.tvRightTop.setSelectableText("");
            if (holder.lyChara != null) holder.lyChara.setVisibility(View.GONE);
            if (holder.contentDivider != null) holder.contentDivider.setVisibility(View.GONE);
            if (holder.tvRightBottom != null) {
                holder.tvRightBottom.setSelectableText(
                        formatGeneralEntry(mContext, header, extra, wanshyu, location));
                holder.tvRightBottom.setVisibility(
                        header.isEmpty() && extra.isEmpty() && wanshyu.isEmpty() && location.isEmpty()
                                ? View.GONE
                                : View.VISIBLE);
                holder.tvRightBottom.setOnAnnotationClickListener(holder::toggleAnnotation);
            }
        }

        // 短按彈出操作菜單
        View.OnClickListener showItemMenu = v -> mListener.onClick(holder);
        holder.itemView.setOnClickListener(showItemMenu);
        if (holder.tvCharaHeader != null) holder.tvCharaHeader.setOnNonLinkClickListener(showItemMenu);
        if (holder.tvCharaInfo != null) holder.tvCharaInfo.setOnNonLinkClickListener(showItemMenu);
        if (holder.tvCharaExtra != null) holder.tvCharaExtra.setOnNonLinkClickListener(showItemMenu);
        if (holder.tvRightTop != null) holder.tvRightTop.setOnNonLinkClickListener(showItemMenu);
        if (holder.tvRightBottom != null) holder.tvRightBottom.setOnNonLinkClickListener(showItemMenu);

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

    private static Spanned formatGeneralEntry(Context context, Spanned header, Spanned extra,
                                             Spanned wanshyu, Spanned location) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        boolean hasHeader = header != null && !header.isEmpty();
        boolean hasExtra = extra != null && !extra.isEmpty();
        boolean hasWanshyu = wanshyu != null && !wanshyu.isEmpty();
        boolean hasLocation = location != null && !location.isEmpty();

        boolean hasTop = hasHeader || hasExtra || hasWanshyu;

        if (hasHeader) {
            boolean topIsSingleLine = !hasExtra && !hasWanshyu;
            CharacterHeaderSpan headerSpan = new CharacterHeaderSpan(context, topIsSingleLine);

            Paint measurePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            measurePaint.setTextSize(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP, 13, context.getResources().getDisplayMetrics()));
            int totalHeaderWidth = headerSpan.getTotalWidth(
                    measurePaint, header.toString(), 0, header.length());

            int headerStart = builder.length();
            builder.append(header);
            int headerEnd = builder.length();
            builder.setSpan(headerSpan, headerStart, headerEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (hasExtra) {
                int extraStart = builder.length();
                builder.append(extra);
                int extraEnd = builder.length();

                int primaryColor = androidx.core.content.ContextCompat.getColor(context, R.color.colorPrimary);
                builder.setSpan(new ForegroundColorSpan(primaryColor), extraStart, extraEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                builder.setSpan(new LeadingMarginSpan.Standard(0, totalHeaderWidth),
                        headerStart, extraEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                if (hasWanshyu) {
                    appendWanshyuParagraphs(builder, wanshyu, headerStart, totalHeaderWidth, false);
                }
            } else if (hasWanshyu) {
                // 沒有廣韻時（如“氈”）：第一段韻書緊接字頭右側排版，後續韻書各段均縮進 totalHeaderWidth
                appendWanshyuParagraphs(builder, wanshyu, headerStart, totalHeaderWidth, true);
            }
        } else if (hasExtra || hasWanshyu) {
            if (hasExtra) {
                int extraStart = builder.length();
                builder.append(extra);
                int extraEnd = builder.length();
                int primaryColor = androidx.core.content.ContextCompat.getColor(context, R.color.colorPrimary);
                builder.setSpan(new ForegroundColorSpan(primaryColor), extraStart, extraEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (hasWanshyu) {
                if (!builder.isEmpty()) builder.append("\n");
                builder.append(wanshyu);
            }
        }

        if (hasTop && hasLocation) {
            builder.append("\n\u200B\n");
            int dividerStart = builder.length() - 2;
            int dividerEnd = builder.length() - 1;
            builder.setSpan(new HorizontalDividerSpan(context), dividerStart, dividerEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (hasLocation) {
            builder.append(location);
        }

        return builder;
    }

    private static void appendWanshyuParagraphs(SpannableStringBuilder builder, Spanned wanshyu,
                                                int headerStart, int totalHeaderWidth,
                                                boolean firstFollowsHeader) {
        int length = wanshyu.length();
        int cursor = 0;
        boolean isFirst = true;

        while (cursor < length) {
            int nextNewline = -1;
            for (int i = cursor; i < length; i++) {
                if (wanshyu.charAt(i) == '\n') {
                    nextNewline = i;
                    break;
                }
            }
            int end = (nextNewline == -1) ? length : nextNewline;
            CharSequence paragraph = wanshyu.subSequence(cursor, end);

            if (isFirst && firstFollowsHeader) {
                int pStart = builder.length();
                builder.append(paragraph);
                int pEnd = builder.length();
                builder.setSpan(new LeadingMarginSpan.Standard(0, totalHeaderWidth),
                        headerStart, pEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                isFirst = false;
            } else {
                builder.append("\n");
                int pStart = builder.length();
                builder.append(paragraph);
                int pEnd = builder.length();
                builder.setSpan(new LeadingMarginSpan.Standard(totalHeaderWidth, totalHeaderWidth),
                        pStart, pEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            cursor = end + 1;
        }
    }

    public static class LinearViewHolder extends RecyclerView.ViewHolder {
        LinearLayout lyChara;
        View contentDivider;
        View annotationContainer;
        SelectableTextView tvCharaHeader, tvCharaInfo, tvCharaExtra,
                tvRightTop, tvRightBottom, tvAnnotation;
        ResultInfo.CommentTarget commentTarget;
        ResultInfo currentItem;
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

        CharSequence getWanshyuText() {
            if (currentItem != null && currentItem.rightTop != null) {
                return currentItem.rightTop.toString();
            }
            return tvRightTop != null ? tvRightTop.getText() : "";
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
            if (currentItem != null && currentItem.chara != null && !currentItem.chara.isEmpty()) {
                return currentItem.chara.toString();
            }
            return tvCharaHeader != null ? tvCharaHeader.getSelectablePlainText() : "";
        }

        String printContent() {
            if (currentItem != null) {
                StringBuilder sb = new StringBuilder();
                if (currentItem.chara != null && !currentItem.chara.isEmpty()) {
                    sb.append(currentItem.chara).append("\n");
                }
                if (currentItem.leftMiddle != null && !currentItem.leftMiddle.isEmpty()) {
                    sb.append(currentItem.leftMiddle).append("\n");
                }
                if (currentItem.leftBottom != null && !currentItem.leftBottom.isEmpty()) {
                    sb.append(currentItem.leftBottom).append("\n");
                }
                if (currentItem.rightTop != null && !currentItem.rightTop.isEmpty()) {
                    sb.append(currentItem.rightTop).append("\n");
                }
                boolean hasTop = sb.length() > 0;
                if (currentItem.rightBottom != null && !currentItem.rightBottom.isEmpty()) {
                    if (hasTop) sb.append("\n");
                    sb.append(currentItem.rightBottom).append("\n");
                }
                return sb.toString();
            }
            if (tvRightBottom != null && tvCharaHeader == null) {
                return tvRightBottom.getSelectablePlainText();
            }
            return (tvCharaHeader != null ? tvCharaHeader.getSelectablePlainText() + "\n" : "") +
                    (tvCharaInfo != null ? tvCharaInfo.getSelectablePlainText() + "\n" : "") +
                    (tvCharaExtra != null ? tvCharaExtra.getSelectablePlainText() + "\n" : "") +
                    (contentMerged ? "" : (tvRightTop != null ? tvRightTop.getSelectablePlainText() + "\n" : "")) +
                    (tvRightBottom != null ? tvRightBottom.getSelectablePlainText() + "\n" : "");
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
