package com.wechat.ai.ai;

/**
 * 发给大模型的单条消息。
 */
public class ChatMsg {
    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    public String role;
    public String content;

    public ChatMsg(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public static ChatMsg system(String c) {
        return new ChatMsg(ROLE_SYSTEM, c);
    }

    public static ChatMsg user(String c) {
        return new ChatMsg(ROLE_USER, c);
    }

    public static ChatMsg assistant(String c) {
        return new ChatMsg(ROLE_ASSISTANT, c);
    }
}
