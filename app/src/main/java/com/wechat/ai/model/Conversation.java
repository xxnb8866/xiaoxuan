package com.wechat.ai.model;

/**
 * 会话（聊天列表里的一行）。用户可自行添加任意数量的 AI 好友。
 */
public class Conversation {
    public String id;
    public String name;
    /** 头像文字（取昵称首字） */
    public String avatarText;
    /** 头像底色 */
    public int avatarColor;
    /** 头像：相册选择的图片路径（为空则用文字/图标头像） */
    public String avatarPath;
    /** 头像：内置矢量图标资源名（为空则用文字头像） */
    public String avatarIcon;
    public String lastMsg;
    public long lastTs;
    public int unread;
    /** 是否为 AI 会话（本版本全部为 AI） */
    public boolean isAi;
    /** 是否置顶 */
    public boolean sticky;
    /** 免打扰 */
    public boolean mute;

    /** 该 AI 专属人设提示词；为空则回退到全局提示词 */
    public String systemPrompt;
    /** 该 AI 的温度（0~2）；小于 0 表示跟随全局设置 */
    public float temperature = -1f;
    /** 该 AI 单独指定的模型；为空则跟随全局 */
    public String model;

    public Conversation() {
    }

    public Conversation(String id, String name, boolean isAi) {
        this.id = id;
        this.name = name;
        this.isAi = isAi;
        this.avatarText = name == null || name.isEmpty() ? "A" : name.substring(0, 1);
    }

    /** 头像显示用的首字，兼容空名称 */
    public String firstChar() {
        if (name == null || name.isEmpty()) return "A";
        return name.substring(0, 1);
    }
}
