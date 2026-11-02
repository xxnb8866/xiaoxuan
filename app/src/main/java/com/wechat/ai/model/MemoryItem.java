package com.wechat.ai.model;

/**
 * 人物经历条目：让 AI 了解"一个真人会经历什么、怎么做事"，从而避免 AI 感。
 * category 决定这段经历如何被描述给 AI。
 */
public class MemoryItem {

    /** 经历分类，对应注入时的分组标题 */
    public static final String[] CATEGORIES = {
            "日常习惯", "过往经历", "人际关系", "喜好厌恶",
            "说话习惯", "工作学习", "情绪反应", "生活细节"
    };

    public long id;
    public String category;
    public String content;
    public boolean enabled;
    public long ts;

    public MemoryItem() {
        enabled = true;
        ts = System.currentTimeMillis();
    }
}
