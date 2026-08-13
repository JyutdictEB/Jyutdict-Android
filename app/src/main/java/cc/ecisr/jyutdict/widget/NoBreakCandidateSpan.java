package cc.ecisr.jyutdict.widget;

import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;

import androidx.annotation.NonNull;

/**
 * Marks a piece of text that should stay on one line when it fits in the
 * continuation-line width. The actual decision is made by SelectableTextView
 * after its width is known.
 */
public final class NoBreakCandidateSpan extends CharacterStyle implements UpdateAppearance {
    private final float continuationIndentEm;

    public NoBreakCandidateSpan(float continuationIndentEm) {
        this.continuationIndentEm = Math.max(0f, continuationIndentEm);
    }

    float continuationIndentPx(float textSize) {
        return continuationIndentEm * textSize;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint textPaint) {
        // Marker only.
    }
}
