package cc.ecisr.jyutdict.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ReplacementSpan;
import android.util.TypedValue;

import androidx.annotation.NonNull;

import cc.ecisr.jyutdict.R;
import cc.ecisr.jyutdict.utils.ColorUtil;

/**
 * 通用字表字頭 Span，使用 34sp 繪製左側大字字頭，並提供固定的欄寬。
 */
public final class CharacterHeaderSpan extends ReplacementSpan {
    private static final float CHARA_TEXT_SIZE_SP = 34f;
    private static final float HORIZONTAL_PADDING_DP = 10f;
    private static final float MARGIN_END_DP = 8f;

    private final float charaTextSizePx;
    private final float paddingHorizontalPx;
    private final float marginEndPx;
    private final int textColor;
    private final boolean singleLine;
    private final Paint charaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CharacterHeaderSpan(Context context, boolean singleLine) {
        float density = context.getResources().getDisplayMetrics().density;
        this.charaTextSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                CHARA_TEXT_SIZE_SP,
                context.getResources().getDisplayMetrics()
        );
        this.paddingHorizontalPx = HORIZONTAL_PADDING_DP * density;
        this.marginEndPx = MARGIN_END_DP * density;
        this.textColor = ColorUtil.resolveThemeColor(context, R.attr.clockTextColor);
        this.singleLine = singleLine;
    }

    public int getTotalWidth(Paint paint, CharSequence text, int start, int end) {
        charaPaint.set(paint);
        charaPaint.setTextSize(charaTextSizePx);
        charaPaint.setColor(textColor);
        float charaWidth = charaPaint.measureText(text, start, end);
        return Math.round(paddingHorizontalPx * 2 + charaWidth + marginEndPx);
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
                       Paint.FontMetricsInt fm) {
        charaPaint.set(paint);
        charaPaint.setTextSize(charaTextSizePx);
        charaPaint.setColor(textColor);
        float charaWidth = charaPaint.measureText(text, start, end);
        int totalWidth = Math.round(paddingHorizontalPx * 2 + charaWidth + marginEndPx);

        if (fm != null && singleLine) {
            Paint.FontMetricsInt charaFm = charaPaint.getFontMetricsInt();
            fm.ascent = Math.min(fm.ascent, charaFm.ascent);
            fm.descent = Math.max(fm.descent, charaFm.descent);
            fm.top = Math.min(fm.top, charaFm.top);
            fm.bottom = Math.max(fm.bottom, charaFm.bottom);
        }
        return totalWidth;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end,
                     float x, int top, int baseline, int bottom, @NonNull Paint paint) {
        charaPaint.set(paint);
        charaPaint.setTextSize(charaTextSizePx);
        charaPaint.setColor(textColor);

        float drawX = x + paddingHorizontalPx;
        Paint.FontMetrics charaFm = charaPaint.getFontMetrics();
        float drawY = top - charaFm.ascent;

        canvas.drawText(text, start, end, drawX, drawY, charaPaint);
    }
}
