package com.wechat.ai.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.wechat.ai.R;

/**
 * 手机系统级通知：AI 发来消息时弹出（不是软件内的提示条）。
 */
public final class NotifyHelper {

    public static final String CHANNEL_MSG = "wx_ai_msg";
    private static final int ID_BASE = 9000;

    private NotifyHelper() {
    }

    /** 创建通知渠道（Android 8+ 必须，重复调用无副作用） */
    public static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null || nm.getNotificationChannel(CHANNEL_MSG) != null) return;
        NotificationChannel ch = new NotificationChannel(
                CHANNEL_MSG, "新消息", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("AI 好友发来的消息");
        ch.enableVibration(true);
        ch.setShowBadge(true);
        nm.createNotificationChannel(ch);
    }

    /**
     * 弹出一条新消息通知。
     *
     * @param convId 会话 id，点击通知会跳进对应聊天页
     * @param title  标题（好友名）
     * @param text   内容预览
     */
    public static void show(Context c, String convId, String title, String text) {
        ensureChannel(c);

        Intent open = new Intent(c, com.wechat.ai.ui.chat.ChatActivity.class);
        open.putExtra(com.wechat.ai.ui.chat.ChatActivity.EXTRA_CONV_ID, convId);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getActivity(c, convId.hashCode(), open, flags);

        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL_MSG)
                .setSmallIcon(R.drawable.ic_notify_msg)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pi);

        // 合并会话内未读，避免刷屏
        NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle(
                c.getString(R.string.app_name));
        style.setConversationTitle(title);
        style.addMessage(text, System.currentTimeMillis(), title);
        b.setStyle(style);

        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(ID_BASE + Math.abs(convId.hashCode() % 1000), b.build());
        }
    }
}
