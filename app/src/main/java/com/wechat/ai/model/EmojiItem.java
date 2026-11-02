package com.wechat.ai.model;

/**
 * 用户自己上传的表情包条目。
 */
public class EmojiItem {
    public long id;
    /** 应用私有目录下的图片路径 */
    public String path;
    /** 逗号分隔的标签（用户自己写的中文或英文） */
    public String tags;
    /** 命中次数，用于轮转 */
    public int hits;
    public long ts;

    public EmojiItem() {
    }

    public EmojiItem(String path, String tags) {
        this.path = path;
        this.tags = tags;
        this.ts = System.currentTimeMillis();
    }

    public String[] tagArray() {
        if (tags == null || tags.trim().isEmpty()) return new String[0];
        return tags.replace("，", ",").replace("、", ",").replace(" ", ",").split(",");
    }
}
