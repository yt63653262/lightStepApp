package com.imooc.step.frame;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Window;
import android.view.WindowManager;

public abstract class BaseActivity extends Activity {
    //是否显示程序标题
    protected boolean isHideAppTitle = true;
    //是否显示系统标题（状态栏）
    protected boolean isHideSysTitle = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        this.onInitVariable();
        if (this.isHideAppTitle) { //在setcontentview()之前设置
            this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        super.onCreate(savedInstanceState);
        if (this.isHideSysTitle) {
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        //构造View,绑定事件
        this.onInitView(savedInstanceState);
        //请求数据
        this.onRequestData();
        FrameApplication.addToActivityList(this);
    }

    @Override
    protected void onDestroy() {
        FrameApplication.removeFromActivityList(this);
        super.onDestroy();
    }

    /**
     * 1) 初始化变量 最先被调用 用于初始化一些变量，创建一些对象
     */
    protected abstract void onInitVariable();

    /**
     * 2) 初始化UI 布局载入操作
     * @param savedInstanceState
     */
    protected abstract void onInitView(final Bundle savedInstanceState);

    /**
     * 3) 请求数据
     */
    protected abstract void onRequestData();
}
