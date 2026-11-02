package com.wechat.ai.model;

/**
 * 一条聊天消息。type 决定气泡内容。
 */
public class Message {

    public static final int FROM_ME = 0;
    public static final int FROM_PEER = 1;
    public static final int FROM_SYSTEM = 2;

    public static final int TYPE_TEXT = 0;
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_REDPACKET = 2;
    public static final int TYPE_TRANSFER = 3;
    public static final int TYPE_VOICE = 4;
    public static final int TYPE_TIME = 5;
    public static final int TYPE_EMOJI = 6;

    public long id;
    public String convId;
    public int from;
    public int type;
    public String content;
    /** 附加数据 JSON：图片路径 / 红包金额主题标语 / 转账金额说明 */
    public String extra;
    public long ts;

    public Message() {
        ts = System.currentTimeMillis();
    }

    public static Message text(String convId, int from, String content) {
        Message m = new Message();
        m.convId = convId;
        m.from = from;
        m.type = TYPE_TEXT;
        m.content = content;
        return m;
    }

    public static Message time(String convId, String content) {
        Message m = new Message();
        m.convId = convId;
        m.from = FROM_SYSTEM;
        m.type = TYPE_TIME;
        m.content = content;
        return m;
    }

    public static Message image(String convId, int from, String path) {
        Message m = new Message();
        m.convId = convId;
        m.from = from;
        m.type = TYPE_IMAGE;
        m.content = "[图片]";
        m.extra = path;
        return m;
    }

    public static Message emoji(String convId, int from, String path) {
        Message m = new Message();
        m.convId = convId;
        m.from = from;
        m.type = TYPE_EMOJI;
        m.content = "[表情]";
        m.extra = path;
        return m;
    }

    public static Message voice(String convId, int from, int seconds) {
        Message m = new Message();
        m.convId = convId;
        m.from = from;
        m.type = TYPE_VOICE;
        m.content = "[语音]";
        m.extra = String.valueOf(seconds);
        return m;
    }
}
