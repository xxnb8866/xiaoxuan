package com.wechat.ai.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.wechat.ai.App;

/**
 * 轻量偏好存储：AI 配置、个人信息、界面开关。
 */
public class Prefs {

    private static final String SP = "wechat_ai_prefs";
    private static SharedPreferences sp;

    public static final String KEY_PROVIDER = "provider_id";
    public static final String KEY_API_KEY = "api_key";
    public static final String KEY_MODEL = "model";
    public static final String KEY_BASE_URL = "base_url";
    public static final String KEY_TEMPERATURE = "temperature";
    public static final String KEY_SYSTEM_PROMPT = "system_prompt";
    public static final String KEY_STREAM = "stream";
    public static final String KEY_EMOTION = "emotion_enabled";
    public static final String KEY_EMOTION_FREQ = "emotion_freq";

    public static final String KEY_NICKNAME = "nickname";
    public static final String KEY_WXID = "wxid";
    public static final String KEY_SIGNATURE = "signature";
    public static final String KEY_AVATAR_PATH = "avatar_path";
    public static final String KEY_AVATAR_ICON = "avatar_icon";
    public static final String KEY_BALANCE = "balance";

    private static SharedPreferences sp() {
        if (sp == null) {
            sp = App.get().getSharedPreferences(SP, Context.MODE_PRIVATE);
        }
        return sp;
    }

    public static String getString(String key, String def) {
        return sp().getString(key, def);
    }

    public static void putString(String key, String value) {
        sp().edit().putString(key, value).apply();
    }

    public static boolean getBoolean(String key, boolean def) {
        return sp().getBoolean(key, def);
    }

    public static void putBoolean(String key, boolean value) {
        sp().edit().putBoolean(key, value).apply();
    }

    public static float getFloat(String key, float def) {
        return sp().getFloat(key, def);
    }

    public static void putFloat(String key, float value) {
        sp().edit().putFloat(key, value).apply();
    }

    // ---------- AI ----------
    public static String getApiKey() {
        return getString(KEY_API_KEY, "");
    }

    public static String getProvider() {
        return getString(KEY_PROVIDER, "deepseek");
    }

    public static String getModel() {
        return getString(KEY_MODEL, "deepseek-chat");
    }

    public static String getBaseUrl() {
        return getString(KEY_BASE_URL, "https://api.deepseek.com/v1/chat/completions");
    }

    public static float getTemperature() {
        return getFloat(KEY_TEMPERATURE, 0.7f);
    }

    public static String getSystemPrompt() {
        return getString(KEY_SYSTEM_PROMPT,
                "你是微信里的真人好友，不是助手。短句口语，一次一到三句，不用Markdown。");
    }

    public static boolean isStream() {
        return getBoolean(KEY_STREAM, true);
    }

    /** AI 相册开关，默认开 */
    public static boolean isPhotoAlbumEnabled() {
        return getBoolean("photo_album", true);
    }

    public static void setPhotoAlbumEnabled(boolean v) {
        putBoolean("photo_album", v);
    }

    public static boolean isEmotionEnabled() {
        return getBoolean(KEY_EMOTION, true);
    }

    public static int getEmotionFreq() {
        return sp().getInt(KEY_EMOTION_FREQ, 2);
    }

    public static void setEmotionFreq(int v) {
        sp().edit().putInt(KEY_EMOTION_FREQ, v).apply();
    }

    /** 表情包计数：按会话持久化，避免退出聊天页后计数归零导致频率看起来不对 */
    public static int getEmojiCounter(String convId) {
        return sp().getInt("emoji_counter_" + convId, 0);
    }

    public static void setEmojiCounter(String convId, int v) {
        sp().edit().putInt("emoji_counter_" + convId, v).apply();
    }

    /** 朋友圈封面图路径，为空则用默认渐变 */
    public static String getMomentsCover() {
        return getString("moments_cover", "");
    }

    public static void setMomentsCover(String v) {
        putString("moments_cover", v);
    }

    // ---------- AI 主动发消息 ----------
    public static final String KEY_PROACTIVE = "proactive";
    public static final String KEY_PRO_START = "pro_start";
    public static final String KEY_PRO_END = "pro_end";
    public static final String KEY_PRO_CONVS = "pro_convs";
    public static final String KEY_PRO_PROMPT = "pro_prompt";
    public static final String KEY_PRO_REPEAT = "pro_repeat";
    public static final String KEY_PRO_WEEKDAYS = "pro_weekdays";
    public static final String KEY_PRO_RAND = "pro_rand";
    public static final String KEY_PRO_RAND_DAY = "pro_rand_day";
    public static final String KEY_PRO_FIRED_DAY = "pro_fired_day";

    public static final int REPEAT_ONCE = 0;
    public static final int REPEAT_DAILY = 1;
    public static final int REPEAT_WEEKLY = 2;

    public static boolean isProactiveEnabled() {
        return getBoolean(KEY_PROACTIVE, false);
    }

    public static void setProactiveEnabled(boolean v) {
        sp().edit().putBoolean(KEY_PROACTIVE, v).apply();
    }

    /** 起始时间（当日分钟数，12:00 -> 720） */
    public static int getProStart() {
        return sp().getInt(KEY_PRO_START, 12 * 60);
    }

    public static void setProStart(int v) {
        sp().edit().putInt(KEY_PRO_START, v).apply();
    }

    /** 结束时间（当日分钟数，14:00 -> 840） */
    public static int getProEnd() {
        return sp().getInt(KEY_PRO_END, 14 * 60);
    }

    public static void setProEnd(int v) {
        sp().edit().putInt(KEY_PRO_END, v).apply();
    }

    /** 多个会话 id，用 | 分隔 */
    public static String getProConvs() {
        return getString(KEY_PRO_CONVS, "");
    }

    public static void setProConvs(String v) {
        putString(KEY_PRO_CONVS, v);
    }

    public static java.util.List<String> getProConvList() {
        java.util.List<String> out = new java.util.ArrayList<>();
        String raw = getProConvs();
        if (raw == null || raw.isEmpty()) return out;
        for (String part : raw.split("\\|")) {
            if (!part.trim().isEmpty()) out.add(part.trim());
        }
        return out;
    }

    public static int getProRepeat() {
        return sp().getInt(KEY_PRO_REPEAT, REPEAT_DAILY);
    }

    public static void setProRepeat(int v) {
        sp().edit().putInt(KEY_PRO_REPEAT, v).apply();
    }

    /** 每周的哪几天，1=周日 2=周一 ... 7=周六（Calendar 规范），用逗号分隔 */
    public static String getProWeekdays() {
        return getString(KEY_PRO_WEEKDAYS, "");
    }

    public static void setProWeekdays(String v) {
        putString(KEY_PRO_WEEKDAYS, v);
    }

    public static boolean isProWeekdayOn(int calendarDay) {
        String raw = getProWeekdays();
        if (raw == null || raw.isEmpty()) return false;
        for (String part : raw.split(",")) {
            if (part.trim().equals(String.valueOf(calendarDay))) return true;
        }
        return false;
    }

    /** 给 AI 的提示词：让它在时间窗内自己决定说什么 */
    public static String getProPrompt() {
        return getString(KEY_PRO_PROMPT,
                "现在是空闲时间，你主动找我聊一句。"
                        + "像朋友随口开口那样，别汇报、别问「在吗」，一句话就好。");
    }

    public static void setProPrompt(String v) {
        putString(KEY_PRO_PROMPT, v);
    }

    /** 今日随机时刻（当日分钟数），保证同一天只随机一次 */
    public static int getProRand(int dayOfYear) {
        if (sp().getInt(KEY_PRO_RAND_DAY, -1) != dayOfYear) return -1;
        return sp().getInt(KEY_PRO_RAND, -1);
    }

    public static void setProRand(int dayOfYear, int minute) {
        sp().edit().putInt(KEY_PRO_RAND_DAY, dayOfYear).putInt(KEY_PRO_RAND, minute).apply();
    }

    /** 今日是否已发过 */
    public static boolean isProFiredToday(int dayOfYear) {
        return sp().getInt(KEY_PRO_FIRED_DAY, -1) == dayOfYear;
    }

    public static void markProFired(int dayOfYear) {
        sp().edit().putInt(KEY_PRO_FIRED_DAY, dayOfYear).apply();
    }

    // ---------- 个人信息 ----------
    public static String getNickname() {
        return getString(KEY_NICKNAME, "我");
    }

    public static String getWxid() {
        return getString(KEY_WXID, "wxid_ai8888");
    }

    /** 我的头像：相册图片路径，为空则用文字/图标头像 */
    public static String getAvatarPath() {
        return getString(KEY_AVATAR_PATH, "");
    }

    /** 我的头像：内置图标名，为空则用文字头像 */
    public static String getAvatarIcon() {
        return getString(KEY_AVATAR_ICON, "");
    }

    public static int getAvatarColor() {
        try {
            return Integer.parseInt(getString("avatar_color", "-10872839"));
        } catch (Exception e) {
            return 0xFF5B8FF9;
        }
    }

    public static String getSignature() {
        return getString(KEY_SIGNATURE, "这个人很懒，什么都没留下");
    }

    public static double getBalance() {
        String v = getString(KEY_BALANCE, "1000");
        try {
            return Double.parseDouble(v);
        } catch (Exception e) {
            return 1000d;
        }
    }

    public static void setBalance(double v) {
        putString(KEY_BALANCE, String.valueOf(v));
    }
}
