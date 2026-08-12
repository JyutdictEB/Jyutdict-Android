package cc.ecisr.jyutdict.widget;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

public class DashedUnderlineSpan extends ReplacementSpan {
    private final Paint mLinePaint;
    private final float mStrokeWidth;
    private final int[] colors;

    public DashedUnderlineSpan(int color) {
        this(new int[]{color});
    }

    public DashedUnderlineSpan(int[] colors) {
        float density = Resources.getSystem().getDisplayMetrics().density;
        mStrokeWidth = 1.0f * density;
        this.colors = colors == null ? new int[0] : Arrays.copyOf(colors, colors.length);
        
        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setStyle(Paint.Style.STROKE);
        if (this.colors.length > 0) mLinePaint.setColor(this.colors[0]);
        mLinePaint.setStrokeWidth(mStrokeWidth);
        mLinePaint.setPathEffect(new DashPathEffect(new float[]{3f * density, 3f * density}, 0));
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
        if (text instanceof Spanned) {
            Spanned spanned = (Spanned) text;
            int next;
            float width = 0;
            for (int i = start; i < end; i = next) {
                next = spanned.nextSpanTransition(i, end, CharacterStyle.class);
                TextPaint tp = new TextPaint();
                if (paint instanceof TextPaint) {
                    tp.set((TextPaint) paint);
                } else {
                    tp.set(paint);
                }
                CharacterStyle[] spans = spanned.getSpans(i, next, CharacterStyle.class);
                for (CharacterStyle span : spans) {
                    if (span != this) span.updateDrawState(tp);
                }
                width += tp.measureText(text, i, next);
            }
            return Math.round(width);
        }
        return Math.round(paint.measureText(text, start, end));
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        float currentX = x;
        if (text instanceof Spanned) {
            Spanned spanned = (Spanned) text;
            int next;
            for (int i = start; i < end; i = next) {
                next = spanned.nextSpanTransition(i, end, CharacterStyle.class);
                TextPaint tp = new TextPaint();
                if (paint instanceof TextPaint) {
                    tp.set((TextPaint) paint);
                } else {
                    tp.set(paint);
                }
                CharacterStyle[] spans = spanned.getSpans(i, next, CharacterStyle.class);
                for (CharacterStyle span : spans) {
                    if (span != this) span.updateDrawState(tp);
                }
                canvas.drawText(text, i, next, currentX, y, tp);
                currentX += tp.measureText(text, i, next);
            }
        } else {
            canvas.drawText(text, start, end, x, y, paint);
            currentX += paint.measureText(text, start, end);
        }
        
        // Draw the dashed underline
        Path path = new Path();
        float lineY = y + paint.getFontMetrics().descent / 1.5f + mStrokeWidth / 2f; 
        path.moveTo(x, lineY);
        path.lineTo(currentX, lineY);
        if (colors.length > 1 && currentX > x) {
            mLinePaint.setShader(new LinearGradient(
                    x, lineY, currentX, lineY, colors, null, Shader.TileMode.CLAMP));
        } else {
            mLinePaint.setShader(null);
        }
        canvas.drawPath(path, mLinePaint);
        mLinePaint.setShader(null);
    }
}
