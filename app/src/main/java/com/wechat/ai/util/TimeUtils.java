package com.wechat.ai.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 微信风格的时间格式化。
 */
public class TimeUtils {

    public static String chatTime(long ts) {
        Calendar now = Calendar.getInstance();
        Calendar t = Calendar.getInstance();
        t.setTimeInMillis(ts);

        if (isSameDay(now, t)) {
            SimpleDateFormat f = new SimpleDateFormat("HH:mm", Locale.CHINA);
            return f.format(new Date(ts));
        }
        if (isYesterday(now, t)) {
            SimpleDateFormat f = new SimpleDateFormat("昨天 HH:mm", Locale.CHINA);
            return f.format(new Date(ts));
        }
        if (isSameYear(now, t)) {
            SimpleDateFormat f = new SimpleDateFormat("M月d日 HH:mm", Locale.CHINA);
            return f.format(new Date(ts));
        }
        SimpleDateFormat f = new SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA);
        return f.format(new Date(ts));
    }

    /** 会话列表右上角时间 */
    public static String listTime(long ts) {
        Calendar now = Calendar.getInstance();
        Calendar t = Calendar.getInstance();
        t.setTimeInMillis(ts);
        if (isSameDay(now, t)) {
            return new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(ts));
        }
        if (isYesterday(now, t)) {
            return "昨天";
        }
        if (isSameYear(now, t)) {
            return new SimpleDateFormat("M月d日", Locale.CHINA).format(new Date(ts));
        }
        return new SimpleDateFormat("yyyy/M/d", Locale.CHINA).format(new Date(ts));
    }

    /** 朋友圈时间：xx分钟前 / 昨天 / x月x日 */
    public static String momentTime(long ts) {
        long diff = System.currentTimeMillis() - ts;
        if (diff < 60_000) return "刚刚";
        if (diff < 3_600_000) return (diff / 60_000) + "分钟前";
        Calendar now = Calendar.getInstance();
        Calendar t = Calendar.getInstance();
        t.setTimeInMillis(ts);
        if (isSameDay(now, t)) return (diff / 3_600_000) + "小时前";
        if (isYesterday(now, t)) return "昨天";
        if (isSameYear(now, t)) return new SimpleDateFormat("M月d日", Locale.CHINA).format(new Date(ts));
        return new SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(new Date(ts));
    }

    /** 是否需要插入时间分隔（与上条间隔超过 5 分钟） */
    public static boolean needTimeDivider(long prev, long cur) {
        return cur - prev > 5 * 60_000;
    }

    private static boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private static boolean isYesterday(Calendar now, Calendar t) {
        Calendar y = (Calendar) now.clone();
        y.add(Calendar.DAY_OF_YEAR, -1);
        return isSameDay(y, t);
    }

    private static boolean isSameYear(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR);
    }
}
