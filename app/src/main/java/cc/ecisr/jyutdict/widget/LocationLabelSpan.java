package cc.ecisr.jyutdict.widget;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;

import java.util.Arrays;

/**
 * 以固定欄寬繪製地名。
 *
 * 普通地名固定佔四個全角字寬；年份地名另加固定年份徽章欄。
 * 實際文字過長時只縮放繪製，不影響 span 的固定 advance，因此後方讀音可以精確對齊。
 */
public final class LocationLabelSpan extends ReplacementSpan {
    private static final float NAME_EM = 4.0f;
    private static final float YEAR_EM = 2.35f;
    private static final float COLUMN_GAP_EM = 0.34f;
    private static final float BADGE_TEXT_SCALE = 0.58f;

    private final String fullName;
    private final String year;
    private final String placeName;
    private final int[] colors;

    public LocationLabelSpan(String fullName, int[] colors) {
        this.fullName = fullName == null ? "" : fullName;
        if (hasFourDigitYear(this.fullName)) {
            year = this.fullName.substring(0, 4);
            placeName = this.fullName.substring(4);
        } else {
            year = "";
            placeName = this.fullName;
        }
        this.colors = colors == null ? new int[0] : Arrays.copyOf(colors, colors.length);
    }

    public static boolean hasFourDigitYear(String value) {
        if (value == null || value.length() < 4) return false;
        for (int i = 0; i < 4; i++) {
            if (!Character.isDigit(value.charAt(i))) return false;
        }
        return true;
    }

    public static float widthEm(String value) {
        return NAME_EM + COLUMN_GAP_EM
                + (hasFourDigitYear(value) ? YEAR_EM + COLUMN_GAP_EM : 0f);
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
                       Paint.FontMetricsInt fm) {
        return Math.round(paint.getTextSize() * widthEm(fullName));
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end,
                     float x, int top, int y, int bottom, @NonNull Paint paint) {
        float originalTextSize = paint.getTextSize();
        float originalScaleX = paint.getTextScaleX();
        int originalColor = paint.getColor();
        Paint.Style originalStyle = paint.getStyle();
        Shader originalShader = paint.getShader();

        float nameX = x;
        int colorOffset = 0;
        if (!year.isEmpty()) {
            float badgeWidth = originalTextSize * YEAR_EM;
            float badgeHeight = originalTextSize * 1.18f;
            float badgeTop = y - originalTextSize * 0.92f;
            RectF badge = new RectF(x, badgeTop, x + badgeWidth, badgeTop + badgeHeight);
            int badgeColor = colors.length > 0 ? colors[0] : originalColor;

            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(badgeColor);
            canvas.drawRoundRect(
                    badge,
                    originalTextSize * 0.18f,
                    originalTextSize * 0.18f,
                    paint
            );

            paint.setTextSize(originalTextSize * BADGE_TEXT_SCALE);
            paint.setTextScaleX(1f);
            paint.setColor(readableForeground(badgeColor));
            float yearWidth = paint.measureText(year);
            Paint.FontMetrics badgeMetrics = paint.getFontMetrics();
            float yearBaseline = badge.centerY()
                    - (badgeMetrics.ascent + badgeMetrics.descent) / 2f;
            canvas.drawText(year, badge.centerX() - yearWidth / 2f, yearBaseline, paint);

            nameX += badgeWidth + originalTextSize * COLUMN_GAP_EM;
            colorOffset = Math.min(1, colors.length);
        }

        paint.setTextSize(originalTextSize);
        paint.setStyle(Paint.Style.FILL);
        float nameWidth = originalTextSize * NAME_EM;
        int[] nameColors;
        if (colors.length == 0) {
            nameColors = new int[]{originalColor};
        } else if (colorOffset >= colors.length) {
            nameColors = new int[]{colors[0]};
        } else {
            nameColors = Arrays.copyOfRange(colors, colorOffset, colors.length);
        }

        if (nameColors.length > 1) {
            paint.setShader(new LinearGradient(
                    nameX,
                    0,
                    nameX + nameWidth,
                    0,
                    nameColors,
                    null,
                    Shader.TileMode.CLAMP
            ));
            paint.setColor(Color.WHITE);
        } else {
            paint.setShader(null);
            paint.setColor(nameColors[0]);
        }

        paint.setTextScaleX(1f);
        float measured = paint.measureText(placeName);
        float scale = measured > nameWidth && measured > 0f ? nameWidth / measured : 1f;
        paint.setTextScaleX(scale);
        float drawnWidth = measured * scale;
        canvas.drawText(placeName, nameX + Math.max(0f, (nameWidth - drawnWidth) / 2f), y, paint);

        paint.setTextSize(originalTextSize);
        paint.setTextScaleX(originalScaleX);
        paint.setColor(originalColor);
        paint.setStyle(originalStyle);
        paint.setShader(originalShader);
    }

    private static int readableForeground(int background) {
        double lightness = Color.red(background) * 0.299
                + Color.green(background) * 0.587
                + Color.blue(background) * 0.114;
        return lightness > 165 ? Color.BLACK : Color.WHITE;
    }
}
