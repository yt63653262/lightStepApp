package com.imooc.step.frame;

import android.content.Context;

public class ErrorHandler implements Thread.UncaughtExceptionHandler {

    /**
     * 当UncaughtException发生时会转入该函数来处理
     * @param thread
     * @param ex
     */
    @Override
    public void uncaughtException(final Thread thread,final Throwable ex) {
        // 处理异常
        LogWriter.LogToFile("崩溃信息:" + ex.getMessage());
        LogWriter.LogToFile("崩溃简短信息:" + ex.toString());
        LogWriter.LogToFile("崩溃线程名称:" + thread.getName() + "崩溃线程ID:" + thread.getId());

        final StackTraceElement[] trace = ex.getStackTrace();
        for (final StackTraceElement element : trace) {
            LogWriter.LogToFile("Lines : " + element.getLineNumber() + " : " + element.getMethodName());
        }
        ex.printStackTrace();
        FrameApplication.exitApp();
    }

    /**
     * CrashHandler实例
     */
    private static ErrorHandler INSTANCE;
    private static volatile boolean onError = false;

    /**
     * 获取CrashHandler实例，单例模式
     * @return
     */
    public static ErrorHandler getInstance() {
        if (ErrorHandler.INSTANCE == null) {
            ErrorHandler.INSTANCE = new ErrorHandler();
        }
        return ErrorHandler.INSTANCE;
    }

    /**
     * 保证只有一个CrashHandler实例
     */
    private ErrorHandler() {}

    /**
     * 初始化，注册Context对象，获取系统默认的UncaughtException处理器，设置该CrashHandler为程序的默认处理器
     * @param ctx
     */
    public void setErrorHandler(final Context ctx) {
        Thread.setDefaultUncaughtExceptionHandler(this);
    }
}
