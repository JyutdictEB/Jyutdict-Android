package cc.ecisr.jyutdict.widget;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;

/** 顯式換行後使用的精確空白欄。 */
public final class FixedWidthSpan extends ReplacementSpan {
    private final float widthEm;

    public FixedWidthSpan(float widthEm) {
        this.widthEm = widthEm;
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
                       Paint.FontMetricsInt fm) {
        return Math.round(paint.getTextSize() * widthEm);
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end,
                     float x, int top, int y, int bottom, @NonNull Paint paint) {
        // 僅佔位。
    }
}
