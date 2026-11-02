package com.wechat.ai.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatTextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

/**
 * 微信风格头像：圆角方形底色 + 昵称首字 / 矢量图标 / 相册图片。
 * 三种形态互斥，优先级：图片 > 图标 > 文字。
 */
public class AvatarView extends AppCompatTextView {

    private int bgColor = 0xFF5B8FF9;
    private String text = "";
    private Drawable icon;
    private Bitmap bitmap;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path clipPath = new Path();
    private float radius = 8f;

    public AvatarView(Context c) {
        super(c);
        init();
    }

    public AvatarView(Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    public AvatarView(Context c, AttributeSet a, int s) {
        super(c, a, s);
        init();
    }

    private void init() {
        radius = 6 * getResources().getDisplayMetrics().density;
        setTextColor(0xFFFFFFFF);
        setTextSize(16);
        setGravity(android.view.Gravity.CENTER);
        setIncludeFontPadding(false);
    }

    /** 文字头像 */
    public void setAvatar(String text, int color) {
        this.text = text == null ? "" : text;
        this.bgColor = color;
        this.icon = null;
        this.bitmap = null;
        setText(this.text);
        invalidate();
    }

    /** 矢量图标头像 */
    public void setIcon(int resId, int color) {
        this.icon = AppCompatResources.getDrawable(getContext(), resId);
        this.bgColor = color;
        this.text = "";
        this.bitmap = null;
        setText("");
        invalidate();
    }

    /** 相册图片头像 */
    public void setImage(String path) {
        if (path == null || path.isEmpty()) {
            this.bitmap = null;
            invalidate();
            return;
        }
        Glide.with(getContext()).asBitmap().load(path).into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                bitmap = resource;
                invalidate();
            }
        });
    }

    /** 统一入口：按 图片 > 图标 > 文字 的优先级设置 */
    public void bind(String path, int iconRes, String text, int color) {
        if (path != null && !path.isEmpty()) {
            this.bgColor = color;
            this.text = "";
            setText("");
            this.icon = null;
            Glide.with(getContext()).asBitmap().load(path).into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                    bitmap = resource;
                    invalidate();
                }
            });
        } else if (iconRes != 0) {
            setIcon(iconRes, color);
        } else {
            setAvatar(text, color);
        }
    }

    public static int iconResByName(String name) {
        if (name == null || name.isEmpty()) return 0;
        return AvatarIcons.resOf(name);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 固定偏小的圆角：更接近微信头像的方形圆角，避免大头像圆得过头
        radius = Math.min(getWidth(), getHeight()) * 0.12f;
        rect.set(0, 0, getWidth(), getHeight());

        if (bitmap != null) {
            // 图片头像：圆角裁剪后填满
            clipPath.reset();
            clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clipPath);
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, getWidth(), getHeight(), true);
            canvas.drawBitmap(scaled, 0, 0, bitmapPaint);
            canvas.restore();
            return;
        }

        bgPaint.setColor(bgColor);
        canvas.drawRoundRect(rect, radius, radius, bgPaint);

        if (icon != null) {
            int size = (int) (Math.min(getWidth(), getHeight()) * 0.62f);
            int left = (getWidth() - size) / 2;
            int top = (getHeight() - size) / 2;
            icon.setBounds(left, top, left + size, top + size);
            icon.draw(canvas);
        } else {
            super.onDraw(canvas);
        }
    }
}
