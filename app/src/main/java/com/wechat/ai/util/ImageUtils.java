package com.wechat.ai.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * 图片工具：把相册里的图片复制到应用私有目录，供表情包使用。
 */
public class ImageUtils {

    public static File emojiDir(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "emojis");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /** 头像存放目录 */
    public static File avatarDir(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "avatars");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /** 复制头像到私有目录，返回目标文件路径 */
    public static String copyAvatar(Context ctx, Uri uri) {
        try {
            File out = new File(avatarDir(ctx), "avatar_" + System.currentTimeMillis() + ".jpg");
            Bitmap bmp = decodeSampled(ctx, uri, 400, 400);
            if (bmp == null) return null;
            FileOutputStream fos = new FileOutputStream(out);
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
            bmp.recycle();
            return out.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** 复制并压缩到私有目录，返回目标文件路径 */
    public static String copyToPrivate(Context ctx, Uri uri) {
        try {
            File dir = emojiDir(ctx);
            File out = new File(dir, "emoji_" + System.currentTimeMillis() + ".jpg");
            Bitmap bmp = decodeSampled(ctx, uri, 512, 512);
            if (bmp == null) return null;
            FileOutputStream fos = new FileOutputStream(out);
            bmp.compress(Bitmap.CompressFormat.JPEG, 88, fos);
            fos.flush();
            fos.close();
            bmp.recycle();
            return out.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap decodeSampled(Context ctx, Uri uri, int reqW, int reqH) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            InputStream in = ctx.getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(in, null, o);
            if (in != null) in.close();

            int w = o.outWidth;
            int h = o.outHeight;
            int scale = 1;
            while (w / scale > reqW && h / scale > reqH) scale *= 2;

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            InputStream in2 = ctx.getContentResolver().openInputStream(uri);
            Bitmap raw = BitmapFactory.decodeStream(in2, null, o2);
            if (in2 != null) in2.close();
            if (raw == null) return null;

            int rotate = rotation(ctx, uri);
            if (rotate != 0) {
                Matrix m = new Matrix();
                m.postRotate(rotate);
                Bitmap r = Bitmap.createBitmap(raw, 0, 0, raw.getWidth(), raw.getHeight(), m, true);
                raw.recycle();
                return r;
            }
            return raw;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static int rotation(Context ctx, Uri uri) {
        try {
            if (!"content".equals(uri.getScheme())) return 0;
            InputStream in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) return 0;
            ExifInterface exif = new ExifInterface(in);
            in.close();
            int o = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (o) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /** 生成纯色位图，用作默认头像/占位 */
    public static Bitmap colorBitmap(int color, int size) {
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(color);
        return bmp;
    }
}
