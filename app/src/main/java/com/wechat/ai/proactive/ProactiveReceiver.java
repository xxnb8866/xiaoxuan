package com.wechat.ai.proactive;

import android.content.BroadcastReceiver;
import android.content.BroadcastReceiver.PendingResult;
import android.content.Context;
import android.content.Intent;

import com.wechat.ai.App;
import com.wechat.ai.ai.AiEngine;
import com.wechat.ai.ai.ChatMsg;
import com.wechat.ai.model.Conversation;
import com.wechat.ai.model.Message;
import com.wechat.ai.model.ProactiveTask;
import com.wechat.ai.util.NotifyHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 多任务主动消息：
 *   KIND_WINDOW —— 到窗口时刻，为每个任务排好 sendCount 个随机时刻
 *   KIND_FIRE   —— 到某个具体时刻，真正让 AI 开口并发通知
 */
public class ProactiveReceiver extends BroadcastReceiver {

    public static final String ACTION = "com.wechat.ai.ACTION_PROACTIVE";
    public static final String EXTRA_KIND = "kind";
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_INDEX = "index";
    public static final int KIND_WINDOW = 0;
    public static final int KIND_FIRE = 1;

    private static final Random RANDOM = new Random();

    @Override
    public void onReceive(Context context, Intent intent) {
        int kind = intent.getIntExtra(EXTRA_KIND, KIND_WINDOW);
        if (kind == KIND_WINDOW) {
            planAll(context);
            return;
        }
        long taskId = intent.getLongExtra(EXTRA_TASK_ID, -1);
        int index = intent.getIntExtra(EXTRA_INDEX, 0);
        if (taskId < 0) return;
        fire(context, taskId, index);
    }

    /** 为每个启用的任务排今天的发送时刻 */
    private void planAll(Context context) {
        Calendar now = Calendar.getInstance();
        final int day = now.get(Calendar.DAY_OF_YEAR);
        try {
            for (ProactiveTask t : App.dbHelper().allTasks()) {
                if (!t.enabled) continue;
                if (!shouldFireToday(t, now)) continue;
                if (t.plannedDay == day && t.firedCount >= t.sendCount) continue;
                // 今天还没排过 -> 生成随机时刻
                if (t.plannedDay != day) {
                    t.plannedTimes = makeRandomTimes(t);
                    t.firedCount = 0;
                    t.plannedDay = day;
                    App.dbHelper().updateTask(t);
                }
                List<Integer> times = parseTimes(t.plannedTimes);
                if (t.firedCount >= times.size()) continue;
                int at = times.get(t.firedCount);
                Calendar target = (Calendar) now.clone();
                target.set(Calendar.HOUR_OF_DAY, at / 60);
                target.set(Calendar.MINUTE, at % 60);
                target.set(Calendar.SECOND, 0);
                target.set(Calendar.MILLISECOND, 0);
                if (target.getTimeInMillis() <= System.currentTimeMillis()) {
                    fire(context, t.id, t.firedCount);   // 已过，立刻发
                } else {
                    ProactiveManager.scheduleFireAt(context, t.id, t.firedCount,
                            target.getTimeInMillis());
                }
            }
        } catch (Throwable ignored) {
        }
        ProactiveManager.scheduleWindow(context);
    }

    /** 在一个时间段内随机分散出 count 个时刻 */
    private static String makeRandomTimes(ProactiveTask t) {
        int span = t.endMin - t.startMin;
        if (span < 0) span += 24 * 60;
        int count = t.sendCount < 1 ? 1 : Math.min(t.sendCount, 10);
        if (span <= count) span = count * 5;
        List<Integer> list = new ArrayList<>();
        // 均分成 count 段，每段内随机取一点，避免挤在一起
        int seg = span / count;
        for (int i = 0; i < count; i++) {
            int base = t.startMin + i * seg;
            int pick = base + (seg > 1 ? RANDOM.nextInt(seg) : 0);
            if (pick >= 24 * 60) pick -= 24 * 60;
            list.add(pick);
        }
        Collections.sort(list);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    private static List<Integer> parseTimes(String raw) {
        List<Integer> out = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return out;
        for (String p : raw.split(",")) {
            try {
                out.add(Integer.parseInt(p.trim()));
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    private static boolean shouldFireToday(ProactiveTask t, Calendar now) {
        if (t.repeatMode == ProactiveTask.REPEAT_DAILY) return true;
        if (t.repeatMode == ProactiveTask.REPEAT_WEEKLY) {
            return t.isWeekdayOn(now.get(Calendar.DAY_OF_WEEK));
        }
        return t.plannedDay != now.get(Calendar.DAY_OF_YEAR);   // 仅一次
    }

    private void fire(Context context, long taskId, int index) {
        ProactiveTask t;
        try {
            t = App.dbHelper().getTask(taskId);
        } catch (Throwable e) {
            return;
        }
        if (t == null || !t.enabled) return;

        // 推进计数并安排下一次
        t.firedCount = index + 1;
        try {
            App.dbHelper().updateTask(t);
        } catch (Throwable ignored) {
        }
        try {
            List<Integer> times = parseTimes(t.plannedTimes);
            if (t.firedCount < times.size()) {
                Calendar c = Calendar.getInstance();
                int at = times.get(t.firedCount);
                Calendar target = (Calendar) c.clone();
                target.set(Calendar.HOUR_OF_DAY, at / 60);
                target.set(Calendar.MINUTE, at % 60);
                target.set(Calendar.SECOND, 0);
                target.set(Calendar.MILLISECOND, 0);
                ProactiveManager.scheduleFireAt(context, t.id, t.firedCount,
                        target.getTimeInMillis());
            }
        } catch (Throwable ignored) {
        }
        if (t.repeatMode == ProactiveTask.REPEAT_ONCE && t.firedCount >= t.sendCount) {
            t.enabled = false;              // 仅一次，发完就停
            try {
                App.dbHelper().updateTask(t);
            } catch (Throwable ignored) {
            }
        }

        List<String> ids = new ArrayList<>();
        for (String p : t.convs.split("\\|")) {
            if (!p.trim().isEmpty()) ids.add(p.trim());
        }
        if (ids.isEmpty()) return;

        final PendingResult result = goAsync();
        final int[] left = {ids.size()};
        for (String convId : ids) {
            Conversation conv;
            try {
                conv = App.dbHelper().getConversation(convId);
            } catch (Throwable e) {
                doneOne(result, left);
                continue;
            }
            if (conv == null) {
                doneOne(result, left);
                continue;
            }
            List<ChatMsg> msgs = new ArrayList<>();
            msgs.add(ChatMsg.system(t.prompt
                    + "\n现在是主动找对方说话，只发一条，一句话，不要解释。"));
            Float temp = conv.temperature >= 0f ? conv.temperature : null;
            String model = (conv.model != null && !conv.model.trim().isEmpty())
                    ? conv.model.trim() : null;
            final String cid = convId;
            final String cname = conv.name;
            AiEngine.chat(msgs, temp, model, new AiEngine.Callback() {
                @Override
                public void onDelta(String text) {
                }

                @Override
                public void onDone(String full, String emotion) {
                    String content = full == null ? "" : full.trim();
                    if (!content.isEmpty()) {
                        try {
                            App.dbHelper().addMessage(
                                    Message.text(cid, Message.FROM_PEER, content));
                            NotifyHelper.show(context, cid, cname, content);
                        } catch (Throwable ignored) {
                        }
                    }
                    doneOne(result, left);
                }

                @Override
                public void onError(String message) {
                    doneOne(result, left);
                }
            });
        }
    }

    private static void doneOne(PendingResult r, int[] left) {
        left[0]--;
        if (left[0] <= 0) {
            try {
                r.finish();
            } catch (Throwable ignored) {
            }
        }
    }
}
