package cc.ecisr.jyutdict.widget;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

/** 以自然文字寬度繪製單色或多色漸變文字，必要時自動附加外邊輪廓以保證對比度。 */
public final class GradientTextSpan extends ReplacementSpan {
    private final int[] colors;

    public GradientTextSpan(int[] colors) {
        this.colors = colors == null ? new int[0] : Arrays.copyOf(colors, colors.length);
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
                       @Nullable Paint.FontMetricsInt fm) {
        return Math.round(paint.measureText(text, start, end));
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end,
                     float x, int top, int y, int bottom, @NonNull Paint paint) {
        int originalColor = paint.getColor();
        int originalAlpha = paint.getAlpha();
        Paint.Style originalStyle = paint.getStyle();
        float originalStrokeWidth = paint.getStrokeWidth();
        Paint.Join originalStrokeJoin = paint.getStrokeJoin();
        Paint.Cap originalStrokeCap = paint.getStrokeCap();
        Shader originalShader = paint.getShader();
        float width = paint.measureText(text, start, end);
        String textString = text.subSequence(start, end).toString();

        int strokeColor = LocationStrokeHelper.getStrokeColor(colors, originalColor);

        // 1. 若需要描邊，先繪製實線描邊 (STROKE)
        if (strokeColor != Color.TRANSPARENT &&
                (LocationStrokeHelper.STROKE_MODE == LocationStrokeHelper.MODE_STROKE ||
                 LocationStrokeHelper.STROKE_MODE == LocationStrokeHelper.MODE_BOTH)) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(LocationStrokeHelper.getStrokeWidth(paint.getTextSize()));
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(strokeColor);
            paint.setAlpha(Color.alpha(strokeColor));
            paint.setShader(null);
            canvas.drawText(textString, x, y, paint);
        }

        // 2. 繪製文字填充 (FILL)，若啟用外發光模式則附帶 ShadowLayer
        paint.setStyle(Paint.Style.FILL);
        if (strokeColor != Color.TRANSPARENT &&
                (LocationStrokeHelper.STROKE_MODE == LocationStrokeHelper.MODE_GLOW ||
                 LocationStrokeHelper.STROKE_MODE == LocationStrokeHelper.MODE_BOTH)) {
            paint.setShadowLayer(LocationStrokeHelper.GLOW_RADIUS_PX, 0f, 0f, strokeColor);
        } else {
            paint.clearShadowLayer();
        }

        if (colors.length > 1 && width > 0f) {
            paint.setShader(new LinearGradient(
                    x, 0f, x + width, 0f, colors, null, Shader.TileMode.CLAMP));
            paint.setColor(Color.WHITE);
            paint.setAlpha(255);
        } else if (colors.length == 1) {
            paint.setShader(null);
            paint.setColor(colors[0]);
            paint.setAlpha(Color.alpha(colors[0]));
        }

        canvas.drawText(textString, x, y, paint);

        // 3. 還原 Paint 狀態
        paint.clearShadowLayer();
        paint.setShader(originalShader);
        paint.setColor(originalColor);
        paint.setAlpha(originalAlpha);
        paint.setStyle(originalStyle);
        paint.setStrokeWidth(originalStrokeWidth);
        paint.setStrokeJoin(originalStrokeJoin);
        paint.setStrokeCap(originalStrokeCap);
    }
}
