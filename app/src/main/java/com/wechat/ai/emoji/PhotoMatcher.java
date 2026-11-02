package com.wechat.ai.emoji;

import com.wechat.ai.db.DbHelper;
import com.wechat.ai.model.EmojiItem;

import java.util.List;

/**
 * AI 相册检索：逻辑和表情包一致（标签不必全命中，同义/近义/字面重合都算分），
 * 但触发源不同 —— 表情包由 AI 判定的"情绪"触发，
 * 相册由"对方这句话在聊什么"触发（比如问"去哪旅游了给我看看"→ 命中"风景"）。
 */
public class PhotoMatcher {

    /** 低于该分数就不发图，避免乱发 */
    private static final int THRESHOLD = 45;

    public static EmojiItem pick(DbHelper db, String topic) {
        if (topic == null || topic.trim().isEmpty()) return null;
        List<EmojiItem> all = db.allPhotos();
        if (all == null || all.isEmpty()) return null;
        EmojiMatcher.Result best = EmojiMatcher.match(all, topic);
        if (best == null || best.score < THRESHOLD) return null;
        db.hitPhoto(best.emoji.id);
        return best.emoji;
    }

    /** 兜底：不挑主题，从相册里取一张（命中次数最少的优先） */
    public static EmojiItem pickAny(DbHelper db) {
        List<EmojiItem> all = db.allPhotos();
        if (all == null || all.isEmpty()) return null;
        EmojiItem best = all.get(0);
        for (EmojiItem e : all) {
            if (e.hits < best.hits) best = e;
        }
        db.hitPhoto(best.id);
        return best;
    }

    public static boolean hasAny(DbHelper db) {
        List<EmojiItem> all = db.allPhotos();
        return all != null && !all.isEmpty();
    }
}
