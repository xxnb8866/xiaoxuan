package com.wechat.ai.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

/**
 * 通用入场动画：容器内多个子组件「从上方堆叠 → 依次向下展开」。
 * 灵感来自微信列表/表单进场时的层次感。
 */
public final class AnimHelper {

    private AnimHelper() {
    }

    /**
     * 让容器内所有直接子 View 依次展开。
     *
     * @param container 容器
     * @param staggerMs 每个子项之间的间隔
     */
    public static void expandChildren(ViewGroup container, long staggerMs) {
        if (container == null) return;
        AnimatorSet set = new AnimatorSet();
        java.util.List<Animator> list = new java.util.ArrayList<>();
        long delay = 0;
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) continue;
            child.setAlpha(0f);
            child.setTranslationY(-dp(container, 26));
            child.setScaleX(0.96f);
            child.setScaleY(0.96f);

            ObjectAnimator a = ObjectAnimator.ofFloat(child, View.ALPHA, 0f, 1f);
            ObjectAnimator t = ObjectAnimator.ofFloat(child, View.TRANSLATION_Y,
                    -dp(container, 26), 0f);
            ObjectAnimator sx = ObjectAnimator.ofFloat(child, View.SCALE_X, 0.96f, 1f);
            ObjectAnimator sy = ObjectAnimator.ofFloat(child, View.SCALE_Y, 0.96f, 1f);
            AnimatorSet item = new AnimatorSet();
            item.playTogether(a, t, sx, sy);
            item.setStartDelay(delay);
            item.setDuration(320);
            item.setInterpolator(new DecelerateInterpolator());
            list.add(item);
            delay += staggerMs;
        }
        if (list.isEmpty()) return;
        set.playTogether(list);
        set.start();
    }

    /** 单个 View 的展开动画 */
    public static void expand(View v, long delay) {
        if (v == null) return;
        v.setAlpha(0f);
        v.setTranslationY(-dp(v, 22));
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(v, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(v, View.TRANSLATION_Y, -dp(v, 22), 0f));
        set.setStartDelay(delay);
        set.setDuration(300);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }

    /** 轻点按压反馈 */
    public static void press(View v) {
        if (v == null) return;
        v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90).start();
        v.postDelayed(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start(), 110);
    }

    private static float dp(View v, float value) {
        return value * v.getResources().getDisplayMetrics().density;
    }
}
