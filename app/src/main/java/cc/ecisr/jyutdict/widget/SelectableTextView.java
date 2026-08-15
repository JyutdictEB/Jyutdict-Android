package cc.ecisr.jyutdict.widget;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * 支持 ClickableSpan 点击和文本选择的 TextView
 *
 * 短按链接区域：触发链接点击
 * 长按任意区域：启动文本选择
 */
public class SelectableTextView extends AppCompatTextView {
    public interface OnAnnotationClickListener {
        void onAnnotationClick(String annotation);
    }

    private static final String WORD_JOINER = "\u2060";

    private boolean mIsPressedOnLink;
    private boolean mHasPerformedLongPress;
    private ClickableSpan mPressedSpan;
    private CharSequence mSourceText;
    private int mRenderedContentWidth = -1;
    private OnAnnotationClickListener mAnnotationClickListener;
    private View.OnClickListener mOnNonLinkClickListener;

    public SelectableTextView(Context context) {
        super(context);
        init();
    }

    public SelectableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SelectableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setTextIsSelectable(true);
    }

    /**
     * RecyclerView 在兩種條目佈局間切換時，TextView 內部的選區 ActionMode
     * 可能仍綁在上一份文字上。重新建立 selectable 狀態可清掉該隱藏狀態。
     */
    public void setSelectableText(CharSequence text) {
        mIsPressedOnLink = false;
        mHasPerformedLongPress = false;
        mPressedSpan = null;
        cancelLongPress();
        clearFocus();
        clearSelection();
        mSourceText = text instanceof Spanned
                ? new SpannedString(text)
                : text == null ? "" : text.toString();
        mRenderedContentWidth = -1;
        renderSelectableText();
        clearSelection();
    }

    public String getSelectablePlainText() {
        CharSequence text = getText();
        return formatSelectedTextForClipboard(text, 0, text.length());
    }

    public void setOnAnnotationClickListener(OnAnnotationClickListener listener) {
        mAnnotationClickListener = listener;
    }

    public void setOnNonLinkClickListener(View.OnClickListener listener) {
        mOnNonLinkClickListener = listener;
    }

    void dispatchAnnotationClick(String annotation) {
        if (mAnnotationClickListener != null) {
            mAnnotationClickListener.onAnnotationClick(annotation);
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (width != oldWidth) renderSelectableText();
    }

    private void renderSelectableText() {
        if (mSourceText == null) return;
        int contentWidth = getWidth() - getTotalPaddingLeft() - getTotalPaddingRight();
        if (contentWidth <= 0) {
            setText(null, TextView.BufferType.SPANNABLE);
            setText(mSourceText, TextView.BufferType.SPANNABLE);
            return;
        }
        if (contentWidth == mRenderedContentWidth && getText().length() > 0) return;
        mRenderedContentWidth = contentWidth;

        CharSequence rendered = applyConditionalNoBreaks(mSourceText, contentWidth);
        setText(null, TextView.BufferType.SPANNABLE);
        setText(rendered, TextView.BufferType.SPANNABLE);
    }

    private CharSequence applyConditionalNoBreaks(CharSequence source, int contentWidth) {
        if (!(source instanceof Spanned)) return source;
        Spanned spanned = (Spanned) source;
        NoBreakCandidateSpan[] candidates = spanned.getSpans(
                0, spanned.length(), NoBreakCandidateSpan.class);
        if (candidates.length == 0) return source;

        SpannableStringBuilder result = new SpannableStringBuilder(source);
        java.util.Arrays.sort(candidates, (left, right) -> Integer.compare(
                result.getSpanStart(right), result.getSpanStart(left)));
        for (NoBreakCandidateSpan candidate : candidates) {
            int start = result.getSpanStart(candidate);
            int end = result.getSpanEnd(candidate);
            result.removeSpan(candidate);
            if (start < 0 || end <= start) continue;

            float availableWidth = contentWidth
                    - candidate.continuationIndentPx(getPaint().getTextSize());
            float desiredWidth = Layout.getDesiredWidth(result, start, end, getPaint());
            if (availableWidth <= 0f || desiredWidth > availableWidth) continue;

            int cursor = end;
            while (cursor > start) {
                int previous = Character.offsetByCodePoints(result, cursor, -1);
                if (previous > start) result.insert(previous, WORD_JOINER);
                cursor = previous;
            }
        }
        return result;
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        if (id == android.R.id.copy) {
            CharSequence text = getText();
            int start = Math.max(0, Math.min(Selection.getSelectionStart(text),
                    Selection.getSelectionEnd(text)));
            int end = Math.max(Selection.getSelectionStart(text),
                    Selection.getSelectionEnd(text));
            if (start >= 0 && end > start) {
                ClipboardManager clipboard = (ClipboardManager)
                        getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText(
                            null, formatSelectedTextForClipboard(text, start, end)));
                    clearSelection();
                    return true;
                }
            }
        }
        return super.onTextContextMenuItem(id);
    }

    private static String formatSelectedTextForClipboard(CharSequence fullText, int start, int end) {
        if (fullText == null || start >= end) return "";
        CharSequence sub = fullText.subSequence(start, end);
        if (fullText instanceof Spanned) {
            Spanned spanned = (Spanned) fullText;
            CharacterHeaderSpan[] headerSpans = spanned.getSpans(start, end, CharacterHeaderSpan.class);
            if (headerSpans.length > 0) {
                SpannableStringBuilder ssb = new SpannableStringBuilder(sub);
                for (CharacterHeaderSpan headerSpan : headerSpans) {
                    int spanEnd = spanned.getSpanEnd(headerSpan);
                    int relEnd = spanEnd - start;
                    if (relEnd > 0 && relEnd < ssb.length()) {
                        if (ssb.charAt(relEnd) != '\n') {
                            ssb.insert(relEnd, "\n");
                        }
                    }
                }
                return stripLayoutCharacters(ssb);
            }
        }
        return stripLayoutCharacters(sub);
    }

    private static String stripLayoutCharacters(CharSequence text) {
        return text == null ? "" : text.toString()
                .replace(WORD_JOINER, "")
                .replace("\u200B", "")
                .replace("\uFEFF", "");
    }

    @Override
    protected void onDetachedFromWindow() {
        mIsPressedOnLink = false;
        mHasPerformedLongPress = false;
        mPressedSpan = null;
        cancelLongPress();
        clearSelection();
        super.onDetachedFromWindow();
    }

    private void clearSelection() {
        CharSequence text = getText();
        if (text instanceof Spannable) {
            Selection.removeSelection((Spannable) text);
        }
    }

    private boolean isLongPressLocationCard() {
        Context context = getContext();
        if (context == null) return true;
        return context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getBoolean("long_press_location_card", true);
    }

    @Override
    public boolean performLongClick() {
        mHasPerformedLongPress = true;
        if (mPressedSpan instanceof LocationClickSpan && isLongPressLocationCard()) {
            mPressedSpan.onClick(this);
            clearSelection();
            return true;
        }
        return super.performLongClick();
    }

    @Override
    public boolean performLongClick(float x, float y) {
        mHasPerformedLongPress = true;
        if (mPressedSpan instanceof LocationClickSpan && isLongPressLocationCard()) {
            mPressedSpan.onClick(this);
            clearSelection();
            return true;
        }
        return super.performLongClick(x, y);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mHasPerformedLongPress = false;
                mPressedSpan = getSpanAtPosition(event);
                mIsPressedOnLink = (mPressedSpan != null);
                break;

            case MotionEvent.ACTION_MOVE:
                // Let super handle MOVE for long press detection
                break;

            case MotionEvent.ACTION_UP:
                if (mHasPerformedLongPress) {
                    mIsPressedOnLink = false;
                    mPressedSpan = null;
                    break;
                }

                ClickableSpan clickedSpan = mPressedSpan;
                mIsPressedOnLink = false;
                mPressedSpan = null;

                if (clickedSpan instanceof AnnotationClickSpan) {
                    clickedSpan.onClick(this);
                    MotionEvent cancelEvent = MotionEvent.obtain(event);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                    super.onTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                    return true;
                } else if (clickedSpan instanceof LocationClickSpan) {
                    if (!isLongPressLocationCard()) {
                        clickedSpan.onClick(this);
                        MotionEvent cancelEvent = MotionEvent.obtain(event);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                        super.onTouchEvent(cancelEvent);
                        cancelEvent.recycle();
                        return true;
                    } else {
                        if (mOnNonLinkClickListener != null) {
                            mOnNonLinkClickListener.onClick(this);
                        }
                    }
                } else if (clickedSpan != null) {
                    clickedSpan.onClick(this);
                    MotionEvent cancelEvent = MotionEvent.obtain(event);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                    super.onTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                    return true;
                } else {
                    if (mOnNonLinkClickListener != null) {
                        mOnNonLinkClickListener.onClick(this);
                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                mIsPressedOnLink = false;
                mPressedSpan = null;
                break;
        }

        return super.onTouchEvent(event);
    }

    private ClickableSpan getSpanAtPosition(MotionEvent event) {
        CharSequence text = getText();
        if (!(text instanceof Spannable)) {
            return null;
        }

        Spannable buffer = (Spannable) text;

        int x = (int) event.getX();
        int y = (int) event.getY();

        x -= getTotalPaddingLeft();
        y -= getTotalPaddingTop();

        x += getScrollX();
        y += getScrollY();

        Layout layout = getLayout();
        if (layout == null) {
            return null;
        }

        int line = layout.getLineForVertical(y);
        int off = layout.getOffsetForHorizontal(line, x);

        ClickableSpan[] spans = buffer.getSpans(off, off, ClickableSpan.class);
        if (spans.length > 0) {
            return spans[0];
        }

        return null;
    }
}
