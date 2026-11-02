package com.wechat.ai;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import com.wechat.ai.db.DbHelper;

/**
 * 全局 Application：持有数据库、初始化默认数据。
 */
public class App extends Application {

    private static App sInstance;
    private DbHelper mDbHelper;

    /** 当前正处于前台的会话 id（用于判断系统通知是否需要弹出） */
    private static String sForegroundConv;

    public static String getForegroundConv() {
        return sForegroundConv;
    }

    public static void setForegroundConv(String convId) {
        sForegroundConv = convId;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mDbHelper = new DbHelper(this);
        // 提前打开一次，触发建表与默认数据
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.close();
        // 开机/启动后恢复 AI 主动消息的定时（设置里开启过才生效）
        try {
            com.wechat.ai.proactive.ProactiveManager.apply(this);
        } catch (Throwable ignored) {
        }
    }

    public static App get() {
        return sInstance;
    }

    public static SQLiteDatabase db() {
        return get().mDbHelper.getWritableDatabase();
    }

    public static DbHelper dbHelper() {
        return get().mDbHelper;
    }
}
