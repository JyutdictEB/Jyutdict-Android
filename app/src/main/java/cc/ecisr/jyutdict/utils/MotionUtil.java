package cc.ecisr.jyutdict.utils;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

/** Shared, deliberately brisk motion used by the View-based UI. */
public final class MotionUtil {
    public static final long DURATION_SHORT = 120L;
    public static final long DURATION_MEDIUM = 180L;

    private MotionUtil() {
    }

    /** Animates visibility and the layout reflow caused by GONE views. */
    public static void beginLayoutTransition(ViewGroup root) {
        if (root == null || !root.isLaidOut() || root.getWindowToken() == null) return;

        TransitionSet transition = new TransitionSet()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .addTransition(new Fade(Fade.IN | Fade.OUT))
                .addTransition(new ChangeBounds())
                .setDuration(DURATION_MEDIUM)
                .setInterpolator(AnimationUtils.loadInterpolator(
                        root.getContext(), android.R.interpolator.fast_out_slow_in));
        TransitionManager.beginDelayedTransition(root, transition);
    }

    /** Shows one state view and hides the others with a single coordinated transition. */
    public static void showOnly(ViewGroup root, View shown, View... states) {
        boolean changed = shown.getVisibility() != View.VISIBLE;
        for (View state : states) {
            int target = state == shown ? View.VISIBLE : View.GONE;
            changed |= state.getVisibility() != target;
        }
        if (!changed) return;

        beginLayoutTransition(root);
        for (View state : states) {
            state.setVisibility(state == shown ? View.VISIBLE : View.GONE);
        }
    }

    public static void animateTextColor(TextView view, @ColorInt int from, @ColorInt int to) {
        if (!view.isLaidOut()) {
            view.setTextColor(to);
            return;
        }
        ObjectAnimator animator = ObjectAnimator.ofInt(view, "textColor", from, to);
        animator.setEvaluator(new ArgbEvaluator());
        animator.setDuration(DURATION_MEDIUM);
        animator.setInterpolator(AnimationUtils.loadInterpolator(
                view.getContext(), android.R.interpolator.fast_out_slow_in));
        animator.start();
    }

    /** Quickly fades changed text out and back in without retaining stale end actions. */
    public static void setText(TextView view, CharSequence text) {
        CharSequence current = view.getText();
        if (String.valueOf(current).contentEquals(text) || !view.isLaidOut()) {
            view.setText(text);
            return;
        }
        view.animate().cancel();
        view.animate()
                .alpha(0f)
                .setDuration(DURATION_SHORT / 2)
                .setInterpolator(AnimationUtils.loadInterpolator(
                        view.getContext(), android.R.interpolator.fast_out_linear_in))
                .withEndAction(() -> {
                    view.setText(text);
                    view.animate()
                            .alpha(1f)
                            .setDuration(DURATION_SHORT)
                            .setInterpolator(AnimationUtils.loadInterpolator(
                                    view.getContext(), android.R.interpolator.linear_out_slow_in))
                            .start();
                })
                .start();
    }

    /** Fades and lifts freshly replaced content into place. */
    public static void reveal(View view) {
        if (!view.isLaidOut()) {
            view.setAlpha(1f);
            view.setTranslationY(0f);
            return;
        }
        float offset = 6f * view.getResources().getDisplayMetrics().density;
        view.animate().cancel();
        view.setAlpha(0f);
        view.setTranslationY(offset);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(DURATION_MEDIUM)
                .setInterpolator(AnimationUtils.loadInterpolator(
                        view.getContext(), android.R.interpolator.linear_out_slow_in))
                .start();
    }

    public static void fadeTo(View view, float alpha) {
        view.animate().cancel();
        view.animate()
                .alpha(alpha)
                .setDuration(DURATION_SHORT)
                .setInterpolator(AnimationUtils.loadInterpolator(
                        view.getContext(), android.R.interpolator.fast_out_slow_in))
                .start();
    }

    public static void fadeThroughColor(ViewGroup root, @ColorInt int color, Runnable action) {
        if (!root.isLaidOut()) {
            action.run();
            return;
        }
        View scrim = new View(root.getContext());
        scrim.setBackgroundColor(color);
        scrim.setAlpha(0f);
        root.addView(scrim, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        scrim.animate()
                .alpha(1f)
                .setDuration(DURATION_MEDIUM)
                .setInterpolator(AnimationUtils.loadInterpolator(
                        root.getContext(), android.R.interpolator.fast_out_slow_in))
                .withEndAction(action)
                .start();
    }

    public static void fadeIn(View view) {
        if (!view.isLaidOut()) {
            view.post(() -> fadeIn(view));
            return;
        }
        view.animate().cancel();
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .setDuration(DURATION_MEDIUM)
                .setInterpolator(AnimationUtils.loadInterpolator(
                        view.getContext(), android.R.interpolator.linear_out_slow_in))
                .start();
    }

    /** Keeps RecyclerView changes noticeable without the stock 250 ms change pause. */
    public static void configureItemAnimator(RecyclerView recyclerView) {
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(DURATION_MEDIUM);
        animator.setRemoveDuration(DURATION_SHORT);
        animator.setMoveDuration(DURATION_MEDIUM);
        animator.setChangeDuration(DURATION_SHORT);
        recyclerView.setItemAnimator(animator);
    }
}
