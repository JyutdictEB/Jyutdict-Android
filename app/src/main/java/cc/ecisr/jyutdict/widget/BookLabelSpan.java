package cc.ecisr.jyutdict.widget;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;

/** Compact inline badge for a rhyme-book name. The underlying text remains selectable. */
public final class BookLabelSpan extends ReplacementSpan {
    private static final float TEXT_SCALE = 0.78f;
    private static final float HORIZONTAL_PADDING_EM = 0.42f;
    private static final float TRAILING_GAP_EM = 0.22f;

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
                       Paint.FontMetricsInt fontMetrics) {
        String label = text.subSequence(start, end).toString();
        float originalSize = paint.getTextSize();
        paint.setTextSize(originalSize * TEXT_SCALE);
        float textWidth = paint.measureText(label);
        paint.setTextSize(originalSize);
        return Math.round(textWidth
                + originalSize * (HORIZONTAL_PADDING_EM * 2 + TRAILING_GAP_EM));
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end,
                     float x, int top, int baseline, int bottom, @NonNull Paint paint) {
        String label = text.subSequence(start, end).toString();
        float originalSize = paint.getTextSize();
        int originalColor = paint.getColor();
        Paint.Style originalStyle = paint.getStyle();

        paint.setTextSize(originalSize * TEXT_SCALE);
        float textWidth = paint.measureText(label);
        float horizontalPadding = originalSize * HORIZONTAL_PADDING_EM;
        float badgeWidth = textWidth + horizontalPadding * 2;
        float badgeHeight = originalSize * 1.16f;
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float centerY = baseline - (metrics.ascent + metrics.descent) / 2f;
        RectF badge = new RectF(
                x,
                centerY - badgeHeight / 2f,
                x + badgeWidth,
                centerY + badgeHeight / 2f
        );

        paint.setStyle(Paint.Style.FILL);
        paint.setColor((originalColor & 0x00FFFFFF) | 0x26000000);
        canvas.drawRoundRect(badge, originalSize * 0.24f, originalSize * 0.24f, paint);

        paint.setColor(originalColor);
        canvas.drawText(label, x + horizontalPadding,
                centerY - (metrics.ascent + metrics.descent) / 2f, paint);

        paint.setTextSize(originalSize);
        paint.setColor(originalColor);
        paint.setStyle(originalStyle);
    }
}
