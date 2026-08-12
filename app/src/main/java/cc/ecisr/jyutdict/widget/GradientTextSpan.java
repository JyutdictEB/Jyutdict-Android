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

/** 以自然文字寬度繪製單色或多色漸變文字。 */
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
        Shader originalShader = paint.getShader();
        float width = paint.measureText(text, start, end);

        if (colors.length > 1 && width > 0f) {
            paint.setShader(new LinearGradient(
                    x, 0f, x + width, 0f, colors, null, Shader.TileMode.CLAMP));
            paint.setColor(Color.WHITE);
        } else if (colors.length == 1) {
            paint.setShader(null);
            paint.setColor(colors[0]);
        }

        canvas.drawText(text.subSequence(start, end).toString(), x, y, paint);
        paint.setShader(originalShader);
        paint.setColor(originalColor);
    }
}
