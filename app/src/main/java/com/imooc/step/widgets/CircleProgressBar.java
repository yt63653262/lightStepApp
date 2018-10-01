package com.imooc.step.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

public class CircleProgressBar extends View {
    private int progress = 0;
    private int maxProgress = 100;
    //绘图的Paint
    private Paint pathPaint;
    private Paint fillPaint;
    //绘制的矩形区域
    private RectF oval;

    private int[] arcColors = {0xFF02C016,0xFF3DF346,0xFF40F1D5,0xFF02C016};
    //背景灰色
    private int pathColor = 0xFFF0EEDF;
    //边框灰色
    private int borderColor = 0xFFD2D1C4;
    private int pathWidth = 35;
    private int width;
    private int height;
    //圆半径
    private int radius = 120;
    //梯度渲染
    private SweepGradient sweepGradient;
    private boolean reset = false;

    public CircleProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        //初始化绘制
        pathPaint = new Paint();
        pathPaint.setAntiAlias(true);//抗锯齿
        pathPaint.setFlags(Paint.ANTI_ALIAS_FLAG);//抗锯齿标志
        pathPaint.setStyle(Paint.Style.STROKE);//设置画笔样式，描边不填充
        pathPaint.setDither(true);//设定是否使用图像抖动处理，会使绘制出来的图片颜色更加平滑和饱满，图像更加清晰
        pathPaint.setStrokeJoin(Paint.Join.ROUND);//设置结合处的样式，此处为圆弧。当设置setStyle是Stroke或StrokeAndFill，设置绘制时各图形的结合方式，如影响矩形角的外轮廓

        fillPaint = new Paint();
        fillPaint.setAntiAlias(true);
        fillPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.STROKE);
        fillPaint.setDither(true);
        fillPaint.setStrokeJoin(Paint.Join.ROUND);
        oval = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (reset) {
            canvas.drawColor(0xFFFFFFFF);
            reset = false;
        }
        width = getMeasuredWidth();
        height = getMeasuredHeight();
        radius = getMeasuredWidth() / 2 - pathWidth;//半径这块其实只需要减去1/2画笔宽度圆就能完全显示，目前这样写会导致边界还多留出1/2画笔宽度的空白，为下边的画线留有一定的空间
        //设置背景颜色
        pathPaint.setColor(pathColor);
        //设置画笔宽度
        pathPaint.setStrokeWidth(pathWidth);
        //绘制背景
        canvas.drawCircle(width / 2,height / 2,radius,pathPaint);
        pathPaint.setStrokeWidth(0.5f);
        pathPaint.setColor(borderColor);
        canvas.drawCircle(width / 2,height / 2,(float) (radius + pathWidth / 2) + 0.5f,pathPaint);
        canvas.drawCircle(width / 2,height / 2,(float) (radius - pathWidth / 2) - 0.5f,pathPaint);
        sweepGradient = new SweepGradient((float)(width / 2),(float)(height / 2),arcColors,null);
        fillPaint.setShader(sweepGradient);//设置着色器
        fillPaint.setStrokeCap(Paint.Cap.ROUND);//设置线帽
        fillPaint.setStrokeWidth(pathWidth);
        oval.set(width / 2 - radius,height / 2 - radius,width / 2 + radius,height / 2 + radius);
        canvas.drawArc(oval,-90.0F,(float) progress / (float) maxProgress * 360.0F,false,fillPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width,height);
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        invalidate();
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public void setMaxProgress(int maxProgress) {
        this.maxProgress = maxProgress;
        invalidate();
    }

    public int getPathColor() {
        return pathColor;
    }

    public void setPathColor(int pathColor) {
        this.pathColor = pathColor;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }

    public int getPathWidth() {
        return pathWidth;
    }

    public void setPathWidth(int pathWidth) {
        this.pathWidth = pathWidth;
    }

    public void setReset(boolean reset) {
        this.reset = reset;
        if (reset) {
            progress = 0;
            invalidate();
        }
    }
}
