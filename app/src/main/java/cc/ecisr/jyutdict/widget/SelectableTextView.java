package cc.ecisr.jyutdict.widget;

import android.content.Context;
import android.text.Layout;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * 支持 ClickableSpan 点击和文本选择的 TextView
 *
 * 短按链接区域：触发链接点击
 * 长按任意区域：启动文本选择
 */
public class SelectableTextView extends AppCompatTextView {

    private boolean mIsPressedOnLink;
    private boolean mHasPerformedLongPress;
    private ClickableSpan mPressedSpan;

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

    @Override
    public boolean performLongClick() {
        mHasPerformedLongPress = true;
        return super.performLongClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mHasPerformedLongPress = false;
                mPressedSpan = getSpanAtPosition(event);
                if (mPressedSpan != null) {
                    mIsPressedOnLink = true;
                } else {
                    mIsPressedOnLink = false;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                // Let super handle MOVE for long press detection
                break;

            case MotionEvent.ACTION_UP:
                if (mIsPressedOnLink) {
                    if (!mHasPerformedLongPress && mPressedSpan != null) {
                        mPressedSpan.onClick(this);
                        mIsPressedOnLink = false;
                        mPressedSpan = null;
                        
                        // Cancel the event for super to avoid default behavior
                        MotionEvent cancelEvent = MotionEvent.obtain(event);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                        super.onTouchEvent(cancelEvent);
                        cancelEvent.recycle();
                        return true;
                    }
                    mIsPressedOnLink = false;
                    mPressedSpan = null;
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
