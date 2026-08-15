package cc.ecisr.jyutdict.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;

import cc.ecisr.jyutdict.R;
import cc.ecisr.jyutdict.utils.ColorUtil;

/**
 * 通用字表行內分割線 Span，繪製 1dp 的水平分割線並提供上下間距。
 */
public final class HorizontalDividerSpan extends ReplacementSpan {
    private static final float TOP_MARGIN_DP = 4f;
    private static final float THICKNESS_DP = 1f;
    private static final float BOTTOM_MARGIN_DP = 4f;

    private final float topMarginPx;
    private final float thicknessPx;
    private final float bottomMarginPx;
    private final int color;

    public HorizontalDividerSpan(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        this.topMarginPx = TOP_MARGIN_DP * density;
        this.thicknessPx = Math.max(1f, THICKNESS_DP * density);
        this.bottomMarginPx = BOTTOM_MARGIN_DP * density;
        this.color = ColorUtil.resolveThemeColor(context, R.attr.clockHover);
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
                       Paint.FontMetricsInt fm) {
        if (fm != null) {
            fm.ascent = -Math.round(topMarginPx + thicknessPx);
            fm.top = fm.ascent;
            fm.descent = Math.round(bottomMarginPx);
            fm.bottom = fm.descent;
        }
        return 0;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end,
                     float x, int top, int baseline, int bottom, @NonNull Paint paint) {
        int originalColor = paint.getColor();
        Paint.Style originalStyle = paint.getStyle();

        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);

        float lineY = top + topMarginPx;
        canvas.drawRect(0, lineY, canvas.getWidth(), lineY + thicknessPx, paint);

        paint.setColor(originalColor);
        paint.setStyle(originalStyle);
    }
}
