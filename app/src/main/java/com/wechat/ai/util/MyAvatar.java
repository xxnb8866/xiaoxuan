package com.wechat.ai.util;

import com.wechat.ai.widget.AvatarIcons;

/**
 * 我的头像：统一读取入口，供各页面复用。
 */
public final class MyAvatar {

    private MyAvatar() {
    }

    /** 头像图片路径 */
    public static String path() {
        return Prefs.getAvatarPath();
    }

    /** 内置图标资源，无图标返回 0 */
    public static int iconRes() {
        return AvatarIcons.resOf(Prefs.getAvatarIcon());
    }

    /** 文字头像首字 */
    public static String text() {
        String n = Prefs.getNickname();
        return n == null || n.isEmpty() ? "我" : n.substring(0, 1);
    }

    public static int color() {
        return Prefs.getAvatarColor();
    }
}
