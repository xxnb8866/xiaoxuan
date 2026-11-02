package com.wechat.ai.widget;

import com.wechat.ai.R;

/**
 * 内置头像图标库：用户创建 AI 时可以直接挑一个，不必上传图片。
 * 全部为矢量 path 绘制，不使用 emoji。
 */
public final class AvatarIcons {

    /** 图标 key -> drawable 资源 */
    private static final int[] RES = {
            R.drawable.ic_avatar_ai,
            R.drawable.ic_avatar_robot,
            R.drawable.ic_avatar_cat,
            R.drawable.ic_avatar_star,
            R.drawable.ic_avatar_heart,
            R.drawable.ic_avatar_flower,
            R.drawable.ic_avatar_moon,
            R.drawable.ic_avatar_leaf,
    };

    private static final String[] NAMES = {
            "ai", "robot", "cat", "star", "heart", "flower", "moon", "leaf"
    };

    private AvatarIcons() {
    }

    /** 图标数量 */
    public static int count() {
        return RES.length;
    }

    /** 按序号取资源 */
    public static int resAt(int index) {
        if (index < 0 || index >= RES.length) return 0;
        return RES[index];
    }

    /** 按名称取资源；未知返回 0 */
    public static int resOf(String name) {
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(name)) return RES[i];
        }
        return 0;
    }

    /** 按资源反查名称 */
    public static String nameOf(int res) {
        for (int i = 0; i < RES.length; i++) {
            if (RES[i] == res) return NAMES[i];
        }
        return "";
    }

    /** 按名称反查序号 */
    public static int indexOf(String name) {
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(name)) return i;
        }
        return -1;
    }

    /** 按序号取名称 */
    public static String nameAt(int index) {
        if (index < 0 || index >= NAMES.length) return "";
        return NAMES[index];
    }
}
