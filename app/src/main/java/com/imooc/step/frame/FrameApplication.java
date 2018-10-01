package com.imooc.step.frame;

import android.app.Activity;
import android.app.Application;

import java.util.LinkedList;

/**
 * 需要在AndroidManifest中去指定android:name为该类
 */
public class FrameApplication extends Application {
    private static LinkedList<Activity> actList = new LinkedList<Activity>();

    public LinkedList<Activity> getActivityList() {
        return actList;
    }

    /**
     * 添加到列表
     * @param act
     */
    public static void addToActivityList(final Activity act) {
        if (act != null) {
            actList.add(act);
        }
    }

    /**
     * 删除act
     * @param act
     */
    public static void removeFromActivityList(final Activity act) {
        if (actList != null && actList.size() > 0 && actList.indexOf(act) != -1) {
            actList.remove(act);
        }
    }

    /**
     * 清理所有activity
     */
    public static void clearActivityList() {
        for (int i = actList.size() - 1; i >= 0; i--) {
            final Activity act = actList.get(i);
            if (act != null) {
                act.finish();
            }
        }
    }

    /**
     * 退出app
     */
    public static void exitApp() {
        try {
            clearActivityList();
        } catch (final Exception e) {

        } finally {
            System.exit(0);
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }
    private PrefsManager prefsManager;
    private static FrameApplication instance;

    public static FrameApplication getInstance() {
        return instance;
    }

    public PrefsManager getPrefsManager() {
        return prefsManager;
    }

    private ErrorHandler errorHandler;
    public void onCreate() {
        super.onCreate();
        instance = this;
        prefsManager = new PrefsManager(this);
        errorHandler = ErrorHandler.getInstance();
    }
}
