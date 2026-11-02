package com.wechat.ai.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.appcompat.widget.AppCompatButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 确认按钮：点击后
 *   ① 收缩成圆形
 *   ② 绘制对勾
 *   ③ 迸发一圈彩色小粒子
 * 每一步都有清晰的视觉反馈。
 */
public class ConfirmButton extends AppCompatButton {

    private static final int[] PARTICLE_COLORS = {
            0xFFFF5A5F, 0xFFFFC107, 0xFF4CAF50, 0xFF2196F3,
            0xFF9C27B0, 0xFFFF9800, 0xFF00BCD4, 0xFFE91E63
    };

    private int baseColor = 0xFF07C160;
    private int baseTextColor = 0xFFFFFFFF;

    private float shrink = 0f;      // 0=原矩形 1=完全圆形
    private float check = 0f;       // 对勾绘制进度 0..1
    private float burst = 0f;       // 粒子迸发进度 0..1
    private boolean animating = false;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path checkPath = new Path();
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    private OnConfirmedListener listener;

    public interface OnConfirmedListener {
        void onConfirmed();
    }

    public ConfirmButton(Context c) {
        super(c);
        init();
    }

    public ConfirmButton(Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    public ConfirmButton(Context c, AttributeSet a, int s) {
        super(c, a, s);
        init();
    }

    private void init() {
        setAllCaps(false);
        setBackgroundColor(Color.TRANSPARENT);
    }

    public void setBaseColor(int color) {
        baseColor = color;
        invalidate();
    }

    public void setOnConfirmedListener(OnConfirmedListener l) {
        listener = l;
    }

    /** 触发完整动画；动画结束后回调 onConfirmed */
    public void confirm() {
        if (animating) return;
        animating = true;
        shrink = 0f;
        check = 0f;
        burst = 0f;
        particles.clear();
        setText("");

        // ① 收缩成圆形
        ValueAnimator a1 = ValueAnimator.ofFloat(0f, 1f);
        a1.setDuration(260);
        a1.setInterpolator(new DecelerateInterpolator());
        a1.addUpdateListener(an -> {
            shrink = (float) an.getAnimatedValue();
            invalidate();
        });

        // ② 绘制对勾
        ValueAnimator a2 = ValueAnimator.ofFloat(0f, 1f);
        a2.setDuration(280);
        a2.setInterpolator(new DecelerateInterpolator());
        a2.addUpdateListener(an -> {
            check = (float) an.getAnimatedValue();
            invalidate();
        });

        // ③ 彩色小粒子迸发
        ValueAnimator a3 = ValueAnimator.ofFloat(0f, 1f);
        a3.setDuration(600);
        a3.setInterpolator(new LinearInterpolator());
        a3.addUpdateListener(an -> {
            burst = (float) an.getAnimatedValue();
            invalidate();
        });

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(a1, a2, a3);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                buildParticles();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animating = false;
                shrink = 0f;
                check = 0f;
                burst = 0f;
                setText(getText());
                invalidate();
                if (listener != null) listener.onConfirmed();
            }
        });
        set.start();
    }

    private void buildParticles() {
        particles.clear();
        for (int i = 0; i < 14; i++) {
            Particle p = new Particle();
            p.angle = (float) (i * 2 * Math.PI / 14 + random.nextFloat() * 0.3);
            p.dist = 0.6f + random.nextFloat() * 0.5f;
            p.size = 3f + random.nextFloat() * 4f;
            p.color = PARTICLE_COLORS[i % PARTICLE_COLORS.length];
            particles.add(p);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!animating) {
            super.onDraw(canvas);
            return;
        }

        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;

        // 背景：从圆角矩形收缩成圆形
        float fullR = h / 2f;
        float curR = h / 2f + (h / 2f - h / 2f) * 0f;  // 高度不变
        float left = cx - (w / 2f) + (w / 2f - h / 2f) * shrink;
        float right = cx + (w / 2f) - (w / 2f - h / 2f) * shrink;
        float top = 0f;
        float bottom = (float) h;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(baseColor);
        android.graphics.RectF rect = new android.graphics.RectF(left, top, right, bottom);
        canvas.drawRoundRect(rect, fullR, fullR, paint);

        // ② 对勾
        if (check > 0f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0xFFFFFFFF);
            paint.setStrokeWidth(Math.max(3f, h * 0.07f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            checkPath.reset();
            float x1 = cx - h * 0.20f;
            float y1 = cy + h * 0.02f;
            float x2 = cx - h * 0.06f;
            float y2 = cy + h * 0.16f;
            float x3 = cx + h * 0.22f;
            float y3 = cy - h * 0.15f;
            float seg = check * 2f;
            checkPath.moveTo(x1, y1);
            if (seg <= 1f) {
                checkPath.lineTo(x1 + (x2 - x1) * seg, y1 + (y2 - y1) * seg);
            } else {
                checkPath.lineTo(x2, y2);
                float s2 = seg - 1f;
                checkPath.lineTo(x2 + (x3 - x2) * s2, y2 + (y3 - y2) * s2);
            }
            canvas.drawPath(checkPath, paint);
        }

        // ③ 彩色小粒子
        if (burst > 0f && burst < 1f) {
            paint.setStyle(Paint.Style.FILL);
            float maxR = Math.min(w, h) * 0.9f;
            for (Particle p : particles) {
                float d = maxR * p.dist * burst;
                float px = cx + (float) Math.cos(p.angle) * d;
                float py = cy + (float) Math.sin(p.angle) * d;
                float alpha = 1f - burst;
                paint.setColor(p.color);
                paint.setAlpha((int) (255 * alpha));
                paint.setStrokeWidth(1f);
                canvas.drawCircle(px, py, p.size * (1f - burst * 0.5f), paint);
            }
            paint.setAlpha(255);
        }
    }

    private static class Particle {
        float angle;
        float dist;
        float size;
        int color;
    }
}
