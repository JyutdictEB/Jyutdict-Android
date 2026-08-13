package cc.ecisr.jyutdict.widget;

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;

/** Clickable pronunciation span that asks its host row to show an inline note. */
public final class AnnotationClickSpan extends ClickableSpan {
    private final String annotation;

    public AnnotationClickSpan(String annotation) {
        this.annotation = annotation;
    }

    @Override
    public void onClick(@NonNull View widget) {
        if (widget instanceof SelectableTextView) {
            ((SelectableTextView) widget).dispatchAnnotationClick(annotation);
        }
    }

    @Override
    public void updateDrawState(@NonNull TextPaint paint) {
        paint.setUnderlineText(false);
    }
}
