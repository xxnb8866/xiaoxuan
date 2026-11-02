package com.wechat.ai.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.wechat.ai.model.Conversation;
import com.wechat.ai.model.EmojiItem;
import com.wechat.ai.model.MemoryItem;
import com.wechat.ai.model.Message;
import com.wechat.ai.model.Moment;
import com.wechat.ai.model.ProactiveTask;
import com.wechat.ai.model.PromptTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地数据库：聊天记录、AI 会话、提示词模板、表情包、朋友圈。
 */
public class DbHelper extends SQLiteOpenHelper {

    private static final String DB = "wechat_ai.db";
    private static final int VERSION = 6;

    public DbHelper(Context ctx) {
        super(ctx, DB, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS messages(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "conv_id TEXT," +
                "mfrom INTEGER," +
                "type INTEGER," +
                "content TEXT," +
                "extra TEXT," +
                "ts INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS conversations(" +
                "id TEXT PRIMARY KEY," +
                "name TEXT," +
                "avatar_text TEXT," +
                "avatar_color INTEGER," +
                "avatar_path TEXT," +
                "avatar_icon TEXT," +
                "last_msg TEXT," +
                "last_ts INTEGER," +
                "unread INTEGER," +
                "is_ai INTEGER," +
                "sticky INTEGER," +
                "mute INTEGER," +
                "system_prompt TEXT," +
                "temperature REAL," +
                "model TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS prompts(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "content TEXT," +
                "ts INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS memories(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "category TEXT," +
                "content TEXT," +
                "enabled INTEGER," +
                "ts INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS proactive_tasks(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "start_min INTEGER," +
                "end_min INTEGER," +
                "convs TEXT," +
                "prompt TEXT," +
                "send_count INTEGER," +
                "repeat_mode INTEGER," +
                "weekdays TEXT," +
                "enabled INTEGER," +
                "planned_times TEXT," +
                "fired_count INTEGER," +
                "planned_day INTEGER," +
                "ts INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS emojis(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "path TEXT," +
                "tags TEXT," +
                "hits INTEGER," +
                "ts INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS photos(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "path TEXT," +
                "tags TEXT," +
                "hits INTEGER," +
                "ts INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS moments(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "author TEXT," +
                "avatar_color INTEGER," +
                "text TEXT," +
                "images TEXT," +
                "ts INTEGER," +
                "liked INTEGER," +
                "likes TEXT," +
                "comments TEXT)");

        seedPrompts(db);
    }

    /** 内置人设模板：每套性格、说话节奏、用词习惯都刻意错开，避免同质化 */
    private void seedPrompts(SQLiteDatabase db) {
        addPrompt(db, "嘴硬心软",
                "你跟对方是十几年的发小，见面就互损，但真出事第一个到场。"
                        + "说话全是大白话，句子短，爱用“得了吧”“就你？”“行行行”。"
                        + "夸人从来不直说，非要绕一句损的，比如“哟，太阳打西边出来了”。"
                        + "对方真难受时你立刻收起玩笑，只问一句“在哪，我过去”，然后陪着。"
                        + "不总结，不鼓励，不说“加油”，一次就回一两句。");
        addPrompt(db, "话少的室友",
                "你是个话很少但特别靠谱的人，跟对方合租。回复通常就几个字：“嗯”“行”“放那吧”“我来”。"
                        + "不爱发表情，不爱解释，别人问你为什么，你就说“没为啥”。"
                        + "但对方遇到困难时你会直接给方案，一句话说到点上，不多说一个字。"
                        + "从不主动找话题，对方不问你就不说。");
        addPrompt(db, "爱操心的人",
                "你是那种天生操心命的人，把对方当自己家孩子疼。开口就问“吃了吗”“几点睡的”“穿够没”，"
                        + "一件事能念叨三遍。爱用“哎呀”“你说你”“听话”。"
                        + "对方一说不舒服你立刻紧张，追着问细节，然后开始安排：喝水、早点睡、别扛着。"
                        + "语气热乎，话可以稍长一点，但别变成说教。");
        addPrompt(db, "毒舌同事",
                "你是对方工位旁边那个嘴特别毒的同事。看什么都不顺眼，吐槽精准又刻薄，"
                        + "爱用“这也叫方案？”“你认真的？”“我服了”。"
                        + "但吐槽完你会偷偷把活干了，还不承认是自己帮的。"
                        + "你从不夸人，最多说一句“还行吧”，那就是最高评价。");
        addPrompt(db, "深夜电台",
                "你是深夜陪人说话的人。语速慢，句子轻，像怕吵醒谁。"
                        + "不急着给建议，先接住对方的情绪，常说“我听着”“嗯，然后呢”“那确实挺难的”。"
                        + "偶尔说一句很短的体己话，不煽情，不灌鸡汤，不说“一切都会好的”。"
                        + "对方不想说的时候你就陪着安静，不追问。");
        addPrompt(db, "中二同桌",
                "你是对方初中同桌，脑回路清奇，满脑子热血漫台词。"
                        + "动不动“这股力量……”“我的战斗才刚开始”“你竟然能接住我这招”。"
                        + "把日常小事说成决战，买个煎饼都能讲成试炼。"
                        + "但对方真低落时你会突然正经，说一句很笨拙但真诚的鼓励。");
        addPrompt(db, "知心姐姐",
                "你是对方特别信任的年长朋友。温柔但不含糊，会听，也会直接点出问题在哪，"
                        + "不说“你没错”这种哄人的话。习惯先复述一遍对方的处境确认自己听懂了，再开口。"
                        + "用词温和，句子完整，偶尔用“我理解”“换个角度想”。"
                        + "该泼冷水时会泼，但会先肯定对方已经尽力了。");
        addPrompt(db, "暴躁真诚",
                "你脾气直，说话冲，爱用“哈？”“你再说一遍”“我不是说过吗”。"
                        + "看不惯就当场说，不装，不绕弯子。但你对谁都这样，不是针对对方。"
                        + "说完该帮还是帮，而且帮得比谁都快。"
                        + "别道歉，别解释自己脾气不好，就这样。");
        addPrompt(db, "文艺网友",
                "你是对方在网上认识多年的网友，喜欢把感受说得很细。"
                        + "爱用具体的画面代替形容词，比如不说“开心”，说“像放学路上买到最后一根冰棍”。"
                        + "句子偏长，有停顿，偶尔跑题去看窗外的云。"
                        + "不评判，只描述自己的感受，然后问对方有没有过类似的瞬间。");
    }

    private void addPrompt(SQLiteDatabase db, String name, String content) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("content", content);
        v.put("ts", System.currentTimeMillis());
        db.insert("prompts", null, v);
    }

    /** 列已存在时安全跳过，避免升级因 duplicate column 直接崩掉导致数据不可用 */
    private static void safeAlter(SQLiteDatabase db, String table, String col, String type) {
        try {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + col + " " + type);
        } catch (Throwable ignored) {
        }
    }

    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        // 老版本可能存进了 NULL，先统一洗成空串（否则上下文里会出现 "null"）
        try {
            db.execSQL("UPDATE messages SET content='' WHERE content IS NULL");
            db.execSQL("UPDATE messages SET extra='' WHERE extra IS NULL");
        } catch (Throwable ignored) {
        }
        // 升级一律走增量 ALTER，绝不 DROP —— 保证本地数据永不丢失
        if (oldV < 6) {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS photos(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "path TEXT," +
                        "tags TEXT," +
                        "hits INTEGER," +
                        "ts INTEGER)");
            } catch (Throwable ignored) {
            }
        }
        if (oldV < 5) {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS proactive_tasks(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "start_min INTEGER," +
                        "end_min INTEGER," +
                        "convs TEXT," +
                        "prompt TEXT," +
                        "send_count INTEGER," +
                        "repeat_mode INTEGER," +
                        "weekdays TEXT," +
                        "enabled INTEGER," +
                        "planned_times TEXT," +
                        "fired_count INTEGER," +
                        "planned_day INTEGER," +
                        "ts INTEGER)");
            } catch (Throwable ignored) {
            }
        }
        if (oldV < 4) {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS memories(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "category TEXT," +
                        "content TEXT," +
                        "enabled INTEGER," +
                        "ts INTEGER)");
            } catch (Throwable ignored) {
            }
        }
        if (oldV < 3) {
            safeAlter(db, "conversations", "avatar_path", "TEXT");
            safeAlter(db, "conversations", "avatar_icon", "TEXT");
            safeAlter(db, "conversations", "system_prompt", "TEXT");
            safeAlter(db, "conversations", "temperature", "REAL DEFAULT -1");
            safeAlter(db, "conversations", "model", "TEXT");
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS prompts(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name TEXT," +
                        "content TEXT," +
                        "ts INTEGER)");
            } catch (Throwable ignored) {
            }
        }
        if (oldV < 2) {
            db.execSQL("ALTER TABLE conversations ADD COLUMN avatar_path TEXT");
            db.execSQL("ALTER TABLE conversations ADD COLUMN avatar_icon TEXT");
            db.execSQL("ALTER TABLE conversations ADD COLUMN system_prompt TEXT");
            db.execSQL("ALTER TABLE conversations ADD COLUMN temperature REAL DEFAULT -1");
            db.execSQL("ALTER TABLE conversations ADD COLUMN model TEXT");
            db.execSQL("CREATE TABLE IF NOT EXISTS prompts(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT," +
                    "content TEXT," +
                    "ts INTEGER)");
            seedPrompts(db);
        }
    }

    // ---------------- 消息 ----------------

    public long addMessage(Message m) {
        ContentValues v = new ContentValues();
        v.put("conv_id", m.convId);
        v.put("mfrom", m.from);
        v.put("type", m.type);
        v.put("content", nz(m.content));
        v.put("extra", nz(m.extra));
        v.put("ts", m.ts);
        long id = getWritableDatabase().insert("messages", null, v);
        updateConversationLast(m);
        return id;
    }

    public void updateMessage(Message m) {
        ContentValues v = new ContentValues();
        v.put("content", m.content);
        v.put("extra", m.extra);
        v.put("type", m.type);
        getWritableDatabase().update("messages", v, "id=?", new String[]{String.valueOf(m.id)});
    }

    public void deleteMessage(long id) {
        getWritableDatabase().delete("messages", "id=?", new String[]{String.valueOf(id)});
    }

    /** null -> 空串，杜绝 "null" 字面量进入对话上下文 */
    public static String nz(String v) {
        return v == null ? "" : v;
    }

    public List<Message> getMessages(String convId) {
        List<Message> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query("messages", null, "conv_id=?",
                new String[]{convId}, null, null, "ts ASC");
        while (c.moveToNext()) {
            Message m = new Message();
            m.id = c.getLong(c.getColumnIndexOrThrow("id"));
            m.convId = c.getString(c.getColumnIndexOrThrow("conv_id"));
            m.from = c.getInt(c.getColumnIndexOrThrow("mfrom"));
            m.type = c.getInt(c.getColumnIndexOrThrow("type"));
            // 关键：数据库里的 NULL 必须转成空串。
            // 否则 Java 字符串拼接会得到字面量 "null"，混进发给 AI 的上下文里，
            // AI 会学舌输出 "nullnullnull..."。
            m.content = nz(c.getString(c.getColumnIndexOrThrow("content")));
            m.extra = nz(c.getString(c.getColumnIndexOrThrow("extra")));
            m.ts = c.getLong(c.getColumnIndexOrThrow("ts"));
            list.add(m);
        }
        c.close();
        return list;
    }

    // ---------------- 会话 / AI 好友 ----------------

    private void updateConversationLast(Message m) {
        String preview = m.content;
        if (m.type == Message.TYPE_IMAGE || m.type == Message.TYPE_EMOJI) preview = "[图片]";
        else if (m.type == Message.TYPE_REDPACKET) preview = "[微信红包]";
        else if (m.type == Message.TYPE_TRANSFER) preview = "[转账]";
        else if (m.type == Message.TYPE_VOICE) preview = "[语音]";
        ContentValues v = new ContentValues();
        v.put("last_msg", preview);
        v.put("last_ts", m.ts);
        getWritableDatabase().update("conversations", v, "id=?", new String[]{m.convId});
    }

    /** 新增一个 AI 好友 */
    // ---------------- 人物经历（让 AI 像个真人的记忆库） ----------------

    public long addMemory(String category, String content) {
        ContentValues v = new ContentValues();
        v.put("category", nz(category));
        v.put("content", nz(content));
        v.put("enabled", 1);
        v.put("ts", System.currentTimeMillis());
        return getWritableDatabase().insert("memories", null, v);
    }

    public void updateMemory(long id, String category, String content, boolean enabled) {
        ContentValues v = new ContentValues();
        v.put("category", nz(category));
        v.put("content", nz(content));
        v.put("enabled", enabled ? 1 : 0);
        getWritableDatabase().update("memories", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteMemory(long id) {
        getWritableDatabase().delete("memories", "id=?", new String[]{String.valueOf(id)});
    }

    public List<MemoryItem> allMemories() {
        List<MemoryItem> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query("memories", null, null, null, null, null, "ts DESC");
        while (c.moveToNext()) {
            MemoryItem m = new MemoryItem();
            m.id = c.getLong(c.getColumnIndexOrThrow("id"));
            m.category = nz(c.getString(c.getColumnIndexOrThrow("category")));
            m.content = nz(c.getString(c.getColumnIndexOrThrow("content")));
            m.enabled = c.getInt(c.getColumnIndexOrThrow("enabled")) == 1;
            m.ts = c.getLong(c.getColumnIndexOrThrow("ts"));
            list.add(m);
        }
        c.close();
        return list;
    }

    /** 只取启用的经历，按分类拼成一段给 AI 看的"人生背景" */
    public String buildMemoryBrief() {
        List<MemoryItem> all = allMemories();
        if (all.isEmpty()) return "";
        java.util.LinkedHashMap<String, StringBuilder> byCat = new java.util.LinkedHashMap<>();
        for (MemoryItem m : all) {
            if (!m.enabled || m.content.trim().isEmpty()) continue;
            StringBuilder sb = byCat.get(m.category);
            if (sb == null) {
                sb = new StringBuilder();
                byCat.put(m.category, sb);
            }
            if (sb.length() > 0) sb.append("；");
            sb.append(m.content.trim());
        }
        if (byCat.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (java.util.Map.Entry<String, StringBuilder> e : byCat.entrySet()) {
            out.append("\n").append(e.getKey()).append("：").append(e.getValue());
        }
        return out.toString();
    }

    // ---------------- AI 主动消息任务（支持多个） ----------------

    public long addTask(ProactiveTask t) {
        ContentValues v = new ContentValues();
        v.put("start_min", t.startMin);
        v.put("end_min", t.endMin);
        v.put("convs", nz(t.convs));
        v.put("prompt", nz(t.prompt));
        v.put("send_count", t.sendCount < 1 ? 1 : t.sendCount);
        v.put("repeat_mode", t.repeatMode);
        v.put("weekdays", nz(t.weekdays));
        v.put("enabled", t.enabled ? 1 : 0);
        v.put("planned_times", nz(t.plannedTimes));
        v.put("fired_count", t.firedCount);
        v.put("planned_day", t.plannedDay);
        v.put("ts", System.currentTimeMillis());
        return getWritableDatabase().insert("proactive_tasks", null, v);
    }

    public void updateTask(ProactiveTask t) {
        ContentValues v = new ContentValues();
        v.put("start_min", t.startMin);
        v.put("end_min", t.endMin);
        v.put("convs", nz(t.convs));
        v.put("prompt", nz(t.prompt));
        v.put("send_count", t.sendCount < 1 ? 1 : t.sendCount);
        v.put("repeat_mode", t.repeatMode);
        v.put("weekdays", nz(t.weekdays));
        v.put("enabled", t.enabled ? 1 : 0);
        v.put("planned_times", nz(t.plannedTimes));
        v.put("fired_count", t.firedCount);
        v.put("planned_day", t.plannedDay);
        getWritableDatabase().update("proactive_tasks", v, "id=?",
                new String[]{String.valueOf(t.id)});
    }

    public void deleteTask(long id) {
        getWritableDatabase().delete("proactive_tasks", "id=?", new String[]{String.valueOf(id)});
    }

    public List<ProactiveTask> allTasks() {
        List<ProactiveTask> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query("proactive_tasks", null, null, null,
                null, null, "ts DESC");
        while (c.moveToNext()) {
            ProactiveTask t = new ProactiveTask();
            t.id = c.getLong(c.getColumnIndexOrThrow("id"));
            t.startMin = c.getInt(c.getColumnIndexOrThrow("start_min"));
            t.endMin = c.getInt(c.getColumnIndexOrThrow("end_min"));
            t.convs = nz(c.getString(c.getColumnIndexOrThrow("convs")));
            t.prompt = nz(c.getString(c.getColumnIndexOrThrow("prompt")));
            t.sendCount = c.getInt(c.getColumnIndexOrThrow("send_count"));
            t.repeatMode = c.getInt(c.getColumnIndexOrThrow("repeat_mode"));
            t.weekdays = nz(c.getString(c.getColumnIndexOrThrow("weekdays")));
            t.enabled = c.getInt(c.getColumnIndexOrThrow("enabled")) == 1;
            t.plannedTimes = nz(c.getString(c.getColumnIndexOrThrow("planned_times")));
            t.firedCount = c.getInt(c.getColumnIndexOrThrow("fired_count"));
            t.plannedDay = c.getInt(c.getColumnIndexOrThrow("planned_day"));
            t.ts = c.getLong(c.getColumnIndexOrThrow("ts"));
            list.add(t);
        }
        c.close();
        return list;
    }

    public ProactiveTask getTask(long id) {
        Cursor c = getReadableDatabase().query("proactive_tasks", null, "id=?",
                new String[]{String.valueOf(id)}, null, null, null);
        if (c.moveToFirst()) {
            ProactiveTask t = new ProactiveTask();
            t.id = c.getLong(c.getColumnIndexOrThrow("id"));
            t.startMin = c.getInt(c.getColumnIndexOrThrow("start_min"));
            t.endMin = c.getInt(c.getColumnIndexOrThrow("end_min"));
            t.convs = nz(c.getString(c.getColumnIndexOrThrow("convs")));
            t.prompt = nz(c.getString(c.getColumnIndexOrThrow("prompt")));
            t.sendCount = c.getInt(c.getColumnIndexOrThrow("send_count"));
            t.repeatMode = c.getInt(c.getColumnIndexOrThrow("repeat_mode"));
            t.weekdays = nz(c.getString(c.getColumnIndexOrThrow("weekdays")));
            t.enabled = c.getInt(c.getColumnIndexOrThrow("enabled")) == 1;
            t.plannedTimes = nz(c.getString(c.getColumnIndexOrThrow("planned_times")));
            t.firedCount = c.getInt(c.getColumnIndexOrThrow("fired_count"));
            t.plannedDay = c.getInt(c.getColumnIndexOrThrow("planned_day"));
            t.ts = c.getLong(c.getColumnIndexOrThrow("ts"));
            c.close();
            return t;
        }
        c.close();
        return null;
    }

    public long addConversation(Conversation c) {
        ContentValues v = new ContentValues();
        v.put("id", c.id);
        v.put("name", c.name);
        v.put("avatar_text", c.firstChar());
        v.put("avatar_color", c.avatarColor);
        v.put("avatar_path", c.avatarPath);
        v.put("avatar_icon", c.avatarIcon);
        v.put("last_msg", "");
        v.put("last_ts", System.currentTimeMillis());
        v.put("unread", 0);
        v.put("is_ai", c.isAi ? 1 : 0);
        v.put("sticky", c.sticky ? 1 : 0);
        v.put("mute", c.mute ? 1 : 0);
        v.put("system_prompt", c.systemPrompt);
        v.put("temperature", c.temperature);
        v.put("model", c.model);
        return getWritableDatabase().insert("conversations", null, v);
    }

    /** 更新 AI 好友资料（名称 / 头像 / 人设 / 温度 / 模型） */
    public void updateConversation(Conversation c) {
        ContentValues v = new ContentValues();
        v.put("name", c.name);
        v.put("avatar_text", c.firstChar());
        v.put("avatar_color", c.avatarColor);
        v.put("avatar_path", c.avatarPath);
        v.put("avatar_icon", c.avatarIcon);
        v.put("is_ai", c.isAi ? 1 : 0);
        v.put("sticky", c.sticky ? 1 : 0);
        v.put("mute", c.mute ? 1 : 0);
        v.put("system_prompt", c.systemPrompt);
        v.put("temperature", c.temperature);
        v.put("model", c.model);
        getWritableDatabase().update("conversations", v, "id=?", new String[]{c.id});
    }

    public void deleteConversation(String id) {
        getWritableDatabase().delete("conversations", "id=?", new String[]{id});
        getWritableDatabase().delete("messages", "conv_id=?", new String[]{id});
    }

    public void setSticky(String id, boolean sticky) {
        ContentValues v = new ContentValues();
        v.put("sticky", sticky ? 1 : 0);
        getWritableDatabase().update("conversations", v, "id=?", new String[]{id});
    }

    public void setMute(String id, boolean mute) {
        ContentValues v = new ContentValues();
        v.put("mute", mute ? 1 : 0);
        getWritableDatabase().update("conversations", v, "id=?", new String[]{id});
    }

    public void markRead(String id) {
        ContentValues v = new ContentValues();
        v.put("unread", 0);
        getWritableDatabase().update("conversations", v, "id=?", new String[]{id});
    }

    private Conversation readConv(Cursor c) {
        Conversation conv = new Conversation();
        conv.id = c.getString(c.getColumnIndexOrThrow("id"));
        conv.name = c.getString(c.getColumnIndexOrThrow("name"));
        conv.avatarText = c.getString(c.getColumnIndexOrThrow("avatar_text"));
        conv.avatarColor = c.getInt(c.getColumnIndexOrThrow("avatar_color"));
        int pIdx = c.getColumnIndex("avatar_path");
        if (pIdx >= 0) conv.avatarPath = c.getString(pIdx);
        int iIdx = c.getColumnIndex("avatar_icon");
        if (iIdx >= 0) conv.avatarIcon = c.getString(iIdx);
        conv.lastMsg = c.getString(c.getColumnIndexOrThrow("last_msg"));
        conv.lastTs = c.getLong(c.getColumnIndexOrThrow("last_ts"));
        conv.unread = c.getInt(c.getColumnIndexOrThrow("unread"));
        conv.isAi = c.getInt(c.getColumnIndexOrThrow("is_ai")) == 1;
        conv.sticky = c.getInt(c.getColumnIndexOrThrow("sticky")) == 1;
        conv.mute = c.getInt(c.getColumnIndexOrThrow("mute")) == 1;
        int spIdx = c.getColumnIndex("system_prompt");
        if (spIdx >= 0) conv.systemPrompt = c.getString(spIdx);
        int tIdx = c.getColumnIndex("temperature");
        conv.temperature = tIdx >= 0 ? c.getFloat(tIdx) : -1f;
        int mIdx = c.getColumnIndex("model");
        if (mIdx >= 0) conv.model = c.getString(mIdx);
        return conv;
    }

    public List<Conversation> getConversations() {
        List<Conversation> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query("conversations", null, null, null, null, null,
                "sticky DESC, last_ts DESC");
        while (c.moveToNext()) list.add(readConv(c));
        c.close();
        return list;
    }

    public Conversation getConversation(String id) {
        Cursor c = getReadableDatabase().query("conversations", null, "id=?", new String[]{id},
                null, null, null);
        if (c.moveToFirst()) {
            Conversation conv = readConv(c);
            c.close();
            return conv;
        }
        c.close();
        return new Conversation(id, "会话", true);
    }

    public void clearConversation(String convId) {
        getWritableDatabase().delete("messages", "conv_id=?", new String[]{convId});
        ContentValues v = new ContentValues();
        v.put("last_msg", "");
        getWritableDatabase().update("conversations", v, "id=?", new String[]{convId});
    }

    // ---------------- 提示词模板 ----------------

    public long addPrompt(PromptTemplate p) {
        ContentValues v = new ContentValues();
        v.put("name", p.name);
        v.put("content", p.content);
        v.put("ts", p.ts == 0 ? System.currentTimeMillis() : p.ts);
        return getWritableDatabase().insert("prompts", null, v);
    }

    public void updatePrompt(PromptTemplate p) {
        ContentValues v = new ContentValues();
        v.put("name", p.name);
        v.put("content", p.content);
        getWritableDatabase().update("prompts", v, "id=?", new String[]{String.valueOf(p.id)});
    }

    public void deletePrompt(long id) {
        getWritableDatabase().delete("prompts", "id=?", new String[]{String.valueOf(id)});
    }

    public List<PromptTemplate> allPrompts() {
        List<PromptTemplate> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query("prompts", null, null, null, null, null, "ts DESC");
        while (c.moveToNext()) {
            PromptTemplate p = new PromptTemplate();
            p.id = c.getLong(c.getColumnIndexOrThrow("id"));
            p.name = c.getString(c.getColumnIndexOrThrow("name"));
            p.content = c.getString(c.getColumnIndexOrThrow("content"));
            p.ts = c.getLong(c.getColumnIndexOrThrow("ts"));
            list.add(p);
        }
        c.close();
        return list;
    }

    // ---------------- 表情包 ----------------

    public long addEmoji(EmojiItem e) {
        ContentValues v = new ContentValues();
        v.put("path", e.path);
        v.put("tags", e.tags);
        v.put("hits", 0);
        v.put("ts", e.ts);
        return getWritableDatabase().insert("emojis", null, v);
    }

    public List<EmojiItem> allEmojis() {
        List<EmojiItem> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query("emojis", null, null, null, null, null, "id DESC");
        while (c.moveToNext()) {
            EmojiItem e = new EmojiItem();
            e.id = c.getLong(c.getColumnIndexOrThrow("id"));
            e.path = c.getString(c.getColumnIndexOrThrow("path"));
            e.tags = c.getString(c.getColumnIndexOrThrow("tags"));
            e.hits = c.getInt(c.getColumnIndexOrThrow("hits"));
            e.ts = c.getLong(c.getColumnIndexOrThrow("ts"));
            list.add(e);
        }
        c.close();
        return list;
    }

    public void updateEmoji(EmojiItem e) {
        ContentValues v = new ContentValues();
        v.put("tags", e.tags);
        v.put("hits", e.hits);
        getWritableDatabase().update("emojis", v, "id=?", new String[]{String.valueOf(e.id)});
    }

    public void deleteEmoji(long id) {
        getWritableDatabase().delete("emojis", "id=?", new String[]{String.valueOf(id)});
    }

    public void hitEmoji(long id) {
        getWritableDatabase().execSQL("UPDATE emojis SET hits=hits+1 WHERE id=" + id);
    }

    // ---------------- AI 相册 ----------------

    public long addPhoto(EmojiItem e) {
        ContentValues v = new ContentValues();
        v.put("path", e.path);
        v.put("tags", e.tags);
        v.put("hits", 0);
        v.put("ts", e.ts);
        return getWritableDatabase().insert("photos", null, v);
    }

    public List<EmojiItem> allPhotos() {
        List<EmojiItem> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query("photos", null, null, null, null, null, "id DESC");
        while (c.moveToNext()) {
            EmojiItem e = new EmojiItem();
            e.id = c.getLong(c.getColumnIndexOrThrow("id"));
            e.path = c.getString(c.getColumnIndexOrThrow("path"));
            e.tags = c.getString(c.getColumnIndexOrThrow("tags"));
            e.hits = c.getInt(c.getColumnIndexOrThrow("hits"));
            e.ts = c.getLong(c.getColumnIndexOrThrow("ts"));
            list.add(e);
        }
        c.close();
        return list;
    }

    public void deletePhoto(long id) {
        getWritableDatabase().delete("photos", "id=?", new String[]{String.valueOf(id)});
    }

    public void hitPhoto(long id) {
        getWritableDatabase().execSQL("UPDATE photos SET hits=hits+1 WHERE id=" + id);
    }

    public void updatePhoto(EmojiItem e) {
        ContentValues v = new ContentValues();
        v.put("tags", e.tags);
        v.put("hits", e.hits);
        getWritableDatabase().update("photos", v, "id=?", new String[]{String.valueOf(e.id)});
    }

    // ---------------- 朋友圈 ----------------

    public long addMoment(Moment m) {
        ContentValues v = new ContentValues();
        v.put("author", m.author);
        v.put("avatar_color", m.avatarColor);
        v.put("text", m.text);
        v.put("images", m.images);
        v.put("ts", m.ts);
        v.put("liked", m.liked);
        v.put("likes", m.likes);
        v.put("comments", m.comments);
        return getWritableDatabase().insert("moments", null, v);
    }

    public List<Moment> allMoments() {
        List<Moment> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query("moments", null, null, null, null, null, "ts DESC");
        while (c.moveToNext()) {
            Moment m = new Moment();
            m.id = c.getLong(c.getColumnIndexOrThrow("id"));
            m.author = c.getString(c.getColumnIndexOrThrow("author"));
            m.avatarColor = c.getInt(c.getColumnIndexOrThrow("avatar_color"));
            m.text = c.getString(c.getColumnIndexOrThrow("text"));
            m.images = c.getString(c.getColumnIndexOrThrow("images"));
            m.ts = c.getLong(c.getColumnIndexOrThrow("ts"));
            m.liked = c.getInt(c.getColumnIndexOrThrow("liked"));
            m.likes = c.getString(c.getColumnIndexOrThrow("likes"));
            m.comments = c.getString(c.getColumnIndexOrThrow("comments"));
            list.add(m);
        }
        c.close();
        return list;
    }

    public void updateMoment(Moment m) {
        ContentValues v = new ContentValues();
        v.put("liked", m.liked);
        v.put("likes", m.likes);
        v.put("comments", m.comments);
        getWritableDatabase().update("moments", v, "id=?", new String[]{String.valueOf(m.id)});
    }
}
