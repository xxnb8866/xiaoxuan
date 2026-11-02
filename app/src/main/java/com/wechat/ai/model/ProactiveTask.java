package com.wechat.ai.model;

/**
 * AI 主动消息任务：可以创建多个，每个有自己的时间段、目标 AI 和发送条数。
 */
public class ProactiveTask {

    public static final int REPEAT_ONCE = 0;
    public static final int REPEAT_DAILY = 1;
    public static final int REPEAT_WEEKLY = 2;

    /** Calendar 顺序：日 一 二 三 四 五 六 */
    public static final int[] WEEK_DAYS = {1, 2, 3, 4, 5, 6, 7};
    public static final String[] WEEK_CN = {"日", "一", "二", "三", "四", "五", "六"};

    public long id;
    public int startMin;      // 起始时间（当日分钟数）
    public int endMin;        // 结束时间
    public String convs;      // 多个会话 id，| 分隔
    public String prompt;     // 给 AI 的提示词
    public int sendCount;     // 一个时间段内发几条
    public int repeatMode;    // 仅一次 / 每天 / 每周
    public String weekdays;   // 每周哪几天，逗号分隔
    public boolean enabled;

    /** 当天已排好的发送时刻（当日分钟数，逗号分隔），用于按条数分散发送 */
    public String plannedTimes;
    public int firedCount;
    public int plannedDay;    // 计划属于哪一天（DAY_OF_YEAR）
    public long ts;

    public ProactiveTask() {
        startMin = 12 * 60;
        endMin = 14 * 60;
        convs = "";
        prompt = "";
        sendCount = 1;
        repeatMode = REPEAT_DAILY;
        weekdays = "";
        enabled = true;
        plannedTimes = "";
        firedCount = 0;
        plannedDay = -1;
        ts = System.currentTimeMillis();
    }

    /** 时间段文字，如 12:00 - 14:00 */
    public String timeText() {
        return fmt(startMin) + " - " + fmt(endMin);
    }

    public static String fmt(int min) {
        return String.format("%02d:%02d", min / 60, min % 60);
    }

    public String repeatText() {
        if (repeatMode == REPEAT_ONCE) return "仅一次";
        if (repeatMode == REPEAT_DAILY) return "每天";
        StringBuilder sb = new StringBuilder("每周");
        if (weekdays != null && !weekdays.isEmpty()) {
            sb.append(" ");
            String[] parts = weekdays.split(",");
            for (int i = 0; i < parts.length; i++) {
                int d = parseIntSafe(parts[i].trim(), -1);
                for (int k = 0; k < WEEK_DAYS.length; k++) {
                    if (WEEK_DAYS[k] == d) sb.append(WEEK_CN[k]);
                }
                if (i < parts.length - 1) sb.append("、");
            }
        }
        return sb.toString();
    }

    private static int parseIntSafe(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    public boolean isWeekdayOn(int calendarDay) {
        if (weekdays == null || weekdays.isEmpty()) return false;
        for (String p : weekdays.split(",")) {
            if (p.trim().equals(String.valueOf(calendarDay))) return true;
        }
        return false;
    }
}
