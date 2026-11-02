package com.wechat.ai.emoji;

import com.wechat.ai.db.DbHelper;
import com.wechat.ai.model.EmojiItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * 表情包检索：根据 AI 判定的情绪，从用户上传的表情里挑最合适的一张。
 * 标签不需要全命中：同义、同情绪大类、字面重合都能得分。
 */
public class EmojiMatcher {

    /** 低于该分数就不发表情，避免乱发 */
    private static final int THRESHOLD = 45;
    private static final Random RANDOM = new Random();

    public static class Result {
        public EmojiItem emoji;
        public int score;
        public String matchedTag;

        public Result(EmojiItem emoji, int score, String matchedTag) {
            this.emoji = emoji;
            this.score = score;
            this.matchedTag = matchedTag;
        }
    }

    /** 主入口：给出情绪标签，返回最佳表情（可能为 null） */
    /** 兜底：不挑情绪，直接从表情库里取一张（命中次数最少的优先，避免老发同一张） */
    public static EmojiItem pickAny(DbHelper db) {
        java.util.List<EmojiItem> all = db.allEmojis();
        if (all.isEmpty()) return null;
        EmojiItem best = all.get(0);
        for (EmojiItem e : all) {
            if (e.hits < best.hits) best = e;
        }
        db.hitEmoji(best.id);
        return best;
    }

    public static EmojiItem pick(DbHelper db, String emotionTag) {
        if (emotionTag == null || emotionTag.trim().isEmpty()) return null;
        List<EmojiItem> all = db.allEmojis();
        if (all.isEmpty()) return null;
        Result best = match(all, emotionTag);
        if (best == null || best.score < THRESHOLD) return null;
        db.hitEmoji(best.emoji.id);
        return best.emoji;
    }

    /** 返回详细匹配结果（用于调试/展示命中标签） */
    public static Result match(List<EmojiItem> list, String emotionTag) {
        String target = EmotionLexicon.normalize(emotionTag);
        String targetFamily = EmotionLexicon.familyOf(target);
        Result best = null;
        List<Result> top = new ArrayList<>();

        for (EmojiItem e : list) {
            int score = 0;
            String hitTag = "";
            for (String raw : e.tagArray()) {
                String t = EmotionLexicon.normalize(raw);
                if (t.isEmpty()) continue;
                int s = scoreOf(t, target, targetFamily, raw, emotionTag);
                if (s > score) {
                    score = s;
                    hitTag = t;
                }
            }
            if (score <= 0) continue;
            // 用过太多次的稍微降权，让表情轮换起来
            score -= Math.min(12, e.hits);
            top.add(new Result(e, score, hitTag));
        }

        if (top.isEmpty()) return null;
        Collections.sort(top, (a, b) -> b.score - a.score);
        int max = top.get(0).score;
        // 同分随机，避免永远同一张
        List<Result> ties = new ArrayList<>();
        for (Result r : top) {
            if (r.score >= max - 3) ties.add(r);
        }
        best = ties.get(RANDOM.nextInt(ties.size()));
        return best;
    }

    private static int scoreOf(String tag, String target, String targetFamily,
                               String raw, String rawTarget) {
        // 完全命中
        if (tag.equals(target)) return 100;
        // 同义词（开心 ≈ 欢乐）
        if (EmotionLexicon.isSynonym(tag, target)) return 88;
        // 同一情绪大类
        String f1 = EmotionLexicon.familyOf(tag);
        if (!f1.isEmpty() && f1.equals(targetFamily)) return 70;
        // 字面互相包含（用户自定义词，如「无语子」命中「无语」）
        String a = raw.toLowerCase(Locale.US).trim();
        String b = rawTarget == null ? "" : rawTarget.toLowerCase(Locale.US).trim();
        if (!a.isEmpty() && !b.isEmpty() && (a.contains(b) || b.contains(a))) return 60;
        // 字面相似（共同字符比例）
        int sim = charSimilarity(a, b);
        if (sim > 0) return 40 + sim * 15;
        return 0;
    }

    /** 两个字符串共同字符占比 0~2 */
    private static int charSimilarity(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        int common = 0;
        String longer = a.length() >= b.length() ? a : b;
        String shorter = a.length() >= b.length() ? b : a;
        for (char c : shorter.toCharArray()) {
            if (longer.indexOf(c) >= 0) common++;
        }
        if (common == 0) return 0;
        float ratio = (float) common / shorter.length();
        if (ratio >= 0.99f) return 2;
        if (ratio >= 0.5f) return 1;
        return 0;
    }
}
