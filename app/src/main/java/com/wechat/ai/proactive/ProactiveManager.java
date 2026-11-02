package com.wechat.ai.proactive;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

import com.wechat.ai.App;
import com.wechat.ai.model.ProactiveTask;

/**
 * AI 主动消息调度器（支持多个任务）。
 *
 * 策略：不设固定间隔。每个任务到窗口开始时刻后，在其时间段内
 * 随机分散出 sendCount 个时刻依次发送，每天的时刻都不一样。
 */
public final class ProactiveManager {

    private static final int REQ_WINDOW = 4096;
    private static final int REQ_FIRE_BASE = 5000;   // + taskId*10 + index

    private ProactiveManager() {
    }

    private static PendingIntent windowPi(Context c) {
        Intent i = new Intent(c, ProactiveReceiver.class);
        i.setAction(ProactiveReceiver.ACTION);
        i.putExtra(ProactiveReceiver.EXTRA_KIND, ProactiveReceiver.KIND_WINDOW);
        return build(c, REQ_WINDOW, i);
    }

    public static PendingIntent firePi(Context c, long taskId, int index) {
        Intent i = new Intent(c, ProactiveReceiver.class);
        i.setAction(ProactiveReceiver.ACTION);
        i.putExtra(ProactiveReceiver.EXTRA_KIND, ProactiveReceiver.KIND_FIRE);
        i.putExtra(ProactiveReceiver.EXTRA_TASK_ID, taskId);
        i.putExtra(ProactiveReceiver.EXTRA_INDEX, index);
        int req = REQ_FIRE_BASE + (int) (taskId * 10) + index;
        return build(c, req, i);
    }

    private static PendingIntent build(Context c, int req, Intent i) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(c, req, i, flags);
    }

    /** 重新安排所有任务 */
    public static void apply(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        cancelAll(c);
        try {
            boolean any = false;
            for (ProactiveTask t : App.dbHelper().allTasks()) {
                if (t.enabled) any = true;
            }
            if (!any) return;
            // 统一在「最早的那个窗口开始时刻」唤醒一次，再由 Receiver 分发各任务
            int earliest = Integer.MAX_VALUE;
            for (ProactiveTask t : App.dbHelper().allTasks()) {
                if (t.enabled && t.startMin < earliest) earliest = t.startMin;
            }
            if (earliest == Integer.MAX_VALUE) earliest = 12 * 60;
            set(am, nextWindowStartMillis(earliest), windowPi(c));
        } catch (Throwable ignored) {
        }
    }

    public static void cancelAll(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.cancel(windowPi(c));
        try {
            for (ProactiveTask t : App.dbHelper().allTasks()) {
                for (int i = 0; i < 10; i++) am.cancel(firePi(c, t.id, i));
            }
        } catch (Throwable ignored) {
        }
    }

    /** 下一次窗口开始时间（今天已过则顺延到明天） */
    public static long nextWindowStartMillis(int startMin) {
        Calendar cal = Calendar.getInstance();
        int cur = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        Calendar target = (Calendar) cal.clone();
        target.set(Calendar.HOUR_OF_DAY, startMin / 60);
        target.set(Calendar.MINUTE, startMin % 60);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        if (cur >= startMin) target.add(Calendar.DAY_OF_YEAR, 1);
        return target.getTimeInMillis();
    }

    public static void scheduleWindow(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        try {
            int earliest = Integer.MAX_VALUE;
            for (ProactiveTask t : App.dbHelper().allTasks()) {
                if (t.enabled && t.startMin < earliest) earliest = t.startMin;
            }
            if (earliest == Integer.MAX_VALUE) return;
            set(am, nextWindowStartMillis(earliest), windowPi(c));
        } catch (Throwable ignored) {
        }
    }

    public static void scheduleFireAt(Context c, long taskId, int index, long atMillis) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        set(am, atMillis, firePi(c, taskId, index));
    }

    private static void set(AlarmManager am, long atMillis, PendingIntent pi) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, atMillis, pi);
            }
        } catch (SecurityException e) {
            try {
                am.set(AlarmManager.RTC_WAKEUP, atMillis, pi);
            } catch (Throwable ignored) {
            }
        }
    }
}
