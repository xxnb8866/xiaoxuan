package com.wechat.ai.model;

/**
 * 通讯录联系人。
 */
public class Contact {
    public String id;
    public String name;
    public String remark;
    public String signature;
    public String region;
    /** 分组标题（为空表示是成员） */
    public String groupTitle;
    public int avatarColor;

    public Contact(String id, String name, String signature, String region, int avatarColor) {
        this.id = id;
        this.name = name;
        this.signature = signature;
        this.region = region;
        this.avatarColor = avatarColor;
    }

    public static Contact group(String title) {
        Contact c = new Contact("", "", "", "", 0);
        c.groupTitle = title;
        return c;
    }

    public boolean isGroup() {
        return groupTitle != null;
    }
}
