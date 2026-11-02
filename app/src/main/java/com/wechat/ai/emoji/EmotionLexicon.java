package com.wechat.ai.emoji;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 情绪词典：把用户随手写的中文标签归一化到标准情绪，
 * 并给出「相近情绪」关系，用于表情包的模糊命中（开心 ≈ 欢乐 ≈ 高兴）。
 */
public class EmotionLexicon {

    /** 标准情绪标签（英文 key） */
    public static final String[] TAGS = {
            "happy", "excited", "funny", "love", "thanks", "greet", "bye", "ok", "calm",
            "sad", "cry", "angry", "fear", "disgust", "surprised", "tired", "shy",
            "sorry", "think", "question"
    };

    /** 标准标签的中文显示名 */
    public static final Map<String, String> TAG_CN = new HashMap<>();

    /** 同义词：标准标签 -> 一组近义词（中文/英文/口语） */
    public static final Map<String, Set<String>> SYNONYMS = new HashMap<>();

    /** 情绪大类：同族之间可以互相命中（分值略低） */
    public static final Map<String, String> FAMILY = new HashMap<>();

    public static final String FAMILY_POSITIVE = "positive";
    public static final String FAMILY_NEGATIVE = "negative";
    public static final String FAMILY_INTENSE = "intense";
    public static final String FAMILY_REACTIVE = "reactive";
    public static final String FAMILY_SOCIAL = "social";
    public static final String FAMILY_NEUTRAL = "neutral";

    static {
        TAG_CN.put("happy", "开心");
        TAG_CN.put("excited", "兴奋");
        TAG_CN.put("funny", "搞笑");
        TAG_CN.put("love", "喜欢");
        TAG_CN.put("thanks", "感谢");
        TAG_CN.put("greet", "打招呼");
        TAG_CN.put("bye", "再见");
        TAG_CN.put("ok", "同意");
        TAG_CN.put("calm", "平静");
        TAG_CN.put("sad", "伤心");
        TAG_CN.put("cry", "哭");
        TAG_CN.put("angry", "生气");
        TAG_CN.put("fear", "害怕");
        TAG_CN.put("disgust", "嫌弃");
        TAG_CN.put("surprised", "惊讶");
        TAG_CN.put("tired", "累");
        TAG_CN.put("shy", "害羞");
        TAG_CN.put("sorry", "抱歉");
        TAG_CN.put("think", "思考");
        TAG_CN.put("question", "疑问");

        put("happy", "开心 高兴 快乐 欢乐 愉快 乐呵 喜悦 爽 美滋滋 大笑 笑 嗨 喜悦 高兴 happy joy glad cheerful delight 喜 乐 爽 棒 好耶 太好了 不错 满意");
        put("excited", "兴奋 激动 亢奋 期待 冲 燃 炸裂 惊喜 兴奋不已 excited thrilled hype 激动 冲鸭 好耶 开心到飞起 乐疯了");
        put("funny", "搞笑 逗 幽默 沙雕 好笑 笑死 哈哈哈 哈哈 逗比 滑稽 funny lol haha joke 有趣的 搞笑的 乐 笑喷");
        put("love", "爱 喜欢 心动 爱心 亲亲 抱抱 爱你 喜欢你 么么哒 love like crush 心动 甜 撒娇 亲 抱");
        put("thanks", "谢谢 感谢 多谢 感恩 thanks thank you thx 感谢你 辛苦了 谢谢你 受用");
        put("greet", "你好 嗨 hello hi 早上好 晚上好 在吗 在么 打招呼 早 来了 报到 问候");
        put("bye", "再见 拜拜 晚安 bye goodbye 走了 溜了 下次聊 回头见 拜 撤了 睡了");
        put("ok", "好的 可以 行 没问题 同意 收到 ok okay 嗯嗯 好 成了 包在我身上 没问题 答应 办妥");
        put("calm", "平静 淡定 安慰 稳 安心 别急 安慰你 平静 calm relax 放心 没事的 缓缓 冷静 佛系");
        put("sad", "伤心 难过 失落 沮丧 郁闷 难受 低落 心碎 悲伤 sad upset 不开心 低落 委屈 丧 心塞");
        put("cry", "哭 大哭 流泪 呜呜 哭唧唧 泪目 呜呜呜 哭哭 cry tears 流泪 眼泪 破防 呜呜呜呜");
        put("angry", "生气 愤怒 火大 气死 炸了 烦 愤怒 angry mad 气 火 恼 怒 生气了 窝火 气炸");
        put("fear", "害怕 恐惧 慌 怕 惊恐 紧张 怂 fear scared 慌张 怕怕 吓人 恐怖 惊慌");
        put("disgust", "嫌弃 无语 恶心 服了 离谱 吐了 白眼 鄙视 吐槽 disgust 无语子 服 醉了 够了 嫌");
        put("surprised", "惊讶 震惊 意外 居然 竟然 哇 哇塞 惊 surprise wow 没想到 出乎意料 惊呆 真的假的");
        put("tired", "累 困 疲惫 乏 睡 熬夜 困死 好累 tired sleepy 疲惫 精疲力尽 歇会儿 想睡 乏了");
        put("shy", "害羞 脸红 尴尬 不好意思 社死 羞 shy 羞涩 尴尬 难为情 捂脸 羞耻");
        put("sorry", "抱歉 对不起 不好意思 道歉 sorry apologize 我的错 对不住 惭愧 认错");
        put("think", "思考 想 琢磨 考虑 沉思 让我想想 think hmm 想想 琢磨 研究 分析 思考中");
        put("question", "疑问 问号 不懂 为什么 真的吗 疑惑 question 啊 啥 什么 咋 困惑 纳闷 求问");

        // 情绪大类
        family("happy", FAMILY_POSITIVE);
        family("excited", FAMILY_POSITIVE);
        family("funny", FAMILY_POSITIVE);
        family("love", FAMILY_POSITIVE);
        family("thanks", FAMILY_SOCIAL);
        family("greet", FAMILY_SOCIAL);
        family("bye", FAMILY_SOCIAL);
        family("ok", FAMILY_SOCIAL);
        family("calm", FAMILY_NEUTRAL);
        family("sad", FAMILY_NEGATIVE);
        family("cry", FAMILY_NEGATIVE);
        family("angry", FAMILY_INTENSE);
        family("fear", FAMILY_INTENSE);
        family("disgust", FAMILY_INTENSE);
        family("surprised", FAMILY_REACTIVE);
        family("tired", FAMILY_NEGATIVE);
        family("shy", FAMILY_REACTIVE);
        family("sorry", FAMILY_NEGATIVE);
        family("think", FAMILY_NEUTRAL);
        family("question", FAMILY_REACTIVE);
    }

    private static void put(String tag, String words) {
        Set<String> set = new HashSet<>();
        for (String w : words.split("\\s+")) {
            if (!w.isEmpty()) set.add(w.toLowerCase(Locale.US));
        }
        SYNONYMS.put(tag, set);
    }

    private static void family(String tag, String f) {
        FAMILY.put(tag, f);
    }

    /**
     * 把用户写的中文标签（如「欢乐」）归一化到标准情绪（happy）。
     * 匹配不到时原样返回。
     */
    public static String normalize(String raw) {
        if (raw == null) return "";
        String t = raw.trim().toLowerCase(Locale.US);
        if (t.isEmpty()) return "";
        // 标准 key / 中文名直接命中
        for (String tag : TAGS) {
            if (tag.equals(t)) return tag;
            String cn = TAG_CN.get(tag);
            if (cn != null && cn.equals(t)) return tag;
        }
        // 同义词命中：包含关系双向判断
        for (String tag : TAGS) {
            Set<String> set = SYNONYMS.get(tag);
            if (set == null) continue;
            if (set.contains(t)) return tag;
            for (String w : set) {
                if (w.length() >= 2 && (t.contains(w) || w.contains(t))) return tag;
            }
        }
        return t;
    }

    public static String familyOf(String tag) {
        String f = FAMILY.get(normalize(tag));
        return f == null ? "" : f;
    }

    public static String cnName(String tag) {
        String t = normalize(tag);
        String cn = TAG_CN.get(t);
        return cn == null ? tag : cn;
    }

    /**
     * 从一段聊天文本里本地检测情绪（不依赖大模型），返回标准标签，检测不到返回 ""。
     */
    public static String detectEmotion(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String t = text.toLowerCase(Locale.US);
        String best = "";
        int bestScore = 0;
        for (String tag : TAGS) {
            Set<String> set = SYNONYMS.get(tag);
            if (set == null) continue;
            int score = 0;
            for (String w : set) {
                if (w.length() < 2) continue;
                if (t.contains(w)) {
                    // 越长的词命中越可信
                    score += w.length() * 2;
                    if (t.contains("不" + w) || t.contains("没" + w)) score -= w.length() * 3;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = tag;
            }
        }
        return bestScore >= 4 ? best : "";
    }

    /** 两个标签是否同义 / 近义 */
    public static boolean isSynonym(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if (na.isEmpty() || nb.isEmpty()) return false;
        if (na.equals(nb)) return true;
        Set<String> set = SYNONYMS.get(na);
        return set != null && set.contains(nb);
    }
}
