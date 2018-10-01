package com.imooc.step;

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Transition;

import com.imooc.step.frame.BaseActivity;

public class WelcomeActivity extends BaseActivity {

    public static final int DELAY_MILLIS = 3000;

    private Handler handler;
    private Runnable jumpRunnable;
    @TargetApi(21)
    @Override
    protected void onInitVariable() {
        handler = new Handler();
        jumpRunnable = new Runnable() {
            @Override
            public void run() {
                // 跳转到Home
                Transition transition = new Explode();
                getWindow().setExitTransition(transition);
                getWindow().setEnterTransition(transition);
                ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(WelcomeActivity.this);
                Intent intent = new Intent();
                intent.setClass(WelcomeActivity.this,HomeActivity.class);
                startActivity(intent,options.toBundle());
                WelcomeActivity.this.finish();
            }
        };
    }

    @Override
    protected void onInitView(Bundle savedInstanceState) {
        setContentView(R.layout.act_welcome);
    }

    @Override
    protected void onRequestData() {
        handler.postDelayed(jumpRunnable, DELAY_MILLIS);
    }
}
