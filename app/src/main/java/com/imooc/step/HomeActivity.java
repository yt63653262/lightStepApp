package com.imooc.step;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Transition;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.imooc.step.beans.PedometerChartBean;
import com.imooc.step.frame.BaseActivity;
import com.imooc.step.frame.LogWriter;
import com.imooc.step.service.IPedometerService;
import com.imooc.step.service.PedometerService;
import com.imooc.step.utils.Utils;
import com.imooc.step.widgets.CircleProgressBar;

import java.util.ArrayList;

public class HomeActivity extends BaseActivity {
    private CircleProgressBar progressBar;
    private TextView textCalorie;
    private TextView time;
    private TextView distance;
    private TextView stepCount;
    private Button reset;
    private Button btnStart;
    private BarChart dataChart;
    private IPedometerService remoteService;
    private ImageView setting;
    private int status = -1;
    private static final int STATUS_NOT_RUNNING = 0;
    private static final int STATUS_RUNNING = 1;
    private boolean isRunning = false;
    private boolean isChartUpdate = false;

    private static final int MESSAGE_UPDATE_STEP_COUNT = 1000;
    private static final int MESSAGE_UPDATE_CHART_DATA = 2000;
    private static final int GET_DATA_TIME = 200;
    private static final long GET_CHART_DATA_TIME = 60000L;
    private PedometerChartBean chartBean;

    private boolean bindService = false;

    private XAxis xAxis;
    private YAxis leftAxis;
    private YAxis rightAxis;
    private Legend legend;
    @TargetApi(21)
    @Override
    protected void onInitVariable() {
        Transition transition = new Explode();
        getWindow().setEnterTransition(transition);
        getWindow().setExitTransition(transition);
    }

    @Override
    protected void onInitView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_home);
        progressBar = (CircleProgressBar) findViewById(R.id.progressBar);
        progressBar.setProgress(5000);
        progressBar.setMaxProgress(10000);
        setting = findViewById(R.id.imageView);
        textCalorie = (TextView) findViewById(R.id.textCalorie);
        time = (TextView) findViewById(R.id.time);
        distance = (TextView) findViewById(R.id.distance);
        stepCount = (TextView) findViewById(R.id.stepCount);
        reset = (Button) findViewById(R.id.reset);
        btnStart = (Button) findViewById(R.id.btnStart);
        dataChart = (BarChart) findViewById(R.id.chart1);
        dataChart.getDescription().setText("");//图表的描述
        dataChart.setPinchZoom(true);//设置按比例缩放柱状图

        xAxis = dataChart.getXAxis();
        leftAxis = dataChart.getAxisLeft();
        rightAxis = dataChart.getAxisRight();
        legend = dataChart.getLegend();
        //图例的显示位置
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(HomeActivity.this,SettingActivity.class);
                startActivity(intent);
            }
        });
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                builder.setTitle("确认重置");
                builder.setMessage("您的记录将要被清除,确定吗?");
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (remoteService != null) {
                            try {
                                isRunning = false;
                                isChartUpdate = false;
                                remoteService.stopCount();
                                remoteService.resetCount();
                                chartBean = remoteService.getChartData();
                                updateChart(chartBean);
                                status = remoteService.getServiceRunningStatus();
                                if (status == STATUS_RUNNING) {
                                    btnStart.setText("停止");
                                } else if (status == STATUS_NOT_RUNNING) {
                                    btnStart.setText("启动");
                                }
                            } catch (RemoteException e) {
                                LogWriter.e(e.toString());
                            }
                        }
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton("取消",null);
                builder.show();
            }
        });
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(HomeActivity.this, PedometerService.class);
                    startService(intent);
                    bindService = bindService(intent,serviceConnection,BIND_AUTO_CREATE);
                    if (remoteService != null) {
                        status = remoteService.getServiceRunningStatus();
                    }
                } catch (RemoteException e) {
                    LogWriter.d(e.toString());
                }

                if (status == STATUS_RUNNING && remoteService != null) {
                    try {
                        remoteService.stopCount();
                        btnStart.setText("启动");
                        isRunning = false;
                        isChartUpdate = false;
                    } catch (RemoteException e) {
                        LogWriter.d(e.toString());
                    }
                } else if (status == STATUS_NOT_RUNNING && remoteService != null) {
                    try {
                        remoteService.startCount();
                        btnStart.setText("停止");
                        isChartUpdate = true;
                        isRunning = true;
                        chartBean = remoteService.getChartData();
                        updateChart(chartBean);
                        //启动两个线程，定时获取数据，刷新UI
                        new Thread(new StepRunnable()).start();
                        new Thread(new ChartRunnable()).start();
                    } catch (RemoteException e) {
                        LogWriter.d(e.toString());
                    }
                }
            }
        });
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            remoteService = IPedometerService.Stub.asInterface(service);
            try {
                status = remoteService.getServiceRunningStatus();
                if (status == STATUS_RUNNING) {
                    btnStart.setText("停止");
                    isChartUpdate = true;
                    isRunning = true;
                    chartBean = remoteService.getChartData();
                    updateChart(chartBean);
                    //启动两个线程，定时获取数据，刷新UI
                    new Thread(new StepRunnable()).start();
                    new Thread(new ChartRunnable()).start();
                } else {
                    btnStart.setText("启动");
                }
            } catch (RemoteException e) {
                LogWriter.d(e.toString());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            remoteService = null;
        }
    };

    @Override
    protected void onRequestData() {
        //检查服务是否运行
        //服务没有运行，启动服务，如果服务已经运行，直接绑定服务
        Intent serviceIntent = new Intent(this, PedometerService.class);
        if (!Utils.isServiceRunning(this,PedometerService.class.getName())) {
            //服务没有运行，启动服务
            startService(serviceIntent);
        } else { //服务运行
            serviceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//创建一个新的service任务栈，并将其压入
        }
        //绑定服务操作
        bindService = bindService(serviceIntent,serviceConnection,BIND_AUTO_CREATE);
        //初始化一些对应状态，按钮文字等
        if (bindService && remoteService != null) {
            try {
                status = remoteService.getServiceRunningStatus();
                if (status == PedometerService.STATUS_NOT_RUN) {
                    btnStart.setText("启动");
                } else if (status == PedometerService.STATUS_RUNNING) {
                    btnStart.setText("停止");
                    isRunning = true;
                    isChartUpdate = true;
                    //启动两个线程，定时获取数据，刷新UI
                    new Thread(new StepRunnable()).start();
                    new Thread(new ChartRunnable()).start();
                }
            } catch (RemoteException e) {
                LogWriter.e(e.toString());
            }
        } else {
            btnStart.setText("启动");
        }
    }

    private class StepRunnable implements Runnable {
        @Override
        public void run() {
            while (isRunning) {
                try {
                    status = remoteService.getServiceRunningStatus();
                    if (status == STATUS_RUNNING) {
                        handler.removeMessages(MESSAGE_UPDATE_STEP_COUNT);
                        //发送消息，让Handler去更新数据
                        handler.sendEmptyMessage(MESSAGE_UPDATE_STEP_COUNT);
                        Thread.sleep(GET_DATA_TIME);
                    }
                } catch (RemoteException e) {
                    LogWriter.d(e.toString());
                } catch (InterruptedException e) {
                    LogWriter.d(e.toString());
                }
            }
        }
    }

    private class ChartRunnable implements Runnable {
        @Override
        public void run() {
            while (isChartUpdate) {
                try {
                    chartBean = remoteService.getChartData();
                    handler.removeMessages(MESSAGE_UPDATE_CHART_DATA);
                    handler.sendEmptyMessage(MESSAGE_UPDATE_CHART_DATA);
                    Thread.sleep(GET_CHART_DATA_TIME);
                } catch (RemoteException e) {
                    LogWriter.d(e.toString());
                } catch (InterruptedException e) {
                    LogWriter.d(e.toString());
                }
            }
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_UPDATE_STEP_COUNT: {
                    //更新计步数据
                    updateStepCount();
                }
                break;
                case MESSAGE_UPDATE_CHART_DATA: {
                    if (chartBean != null) {
                        updateChart(chartBean);
                    }
                }
                break;
                default:
                    LogWriter.d("Default = " + msg.what);
            }
            super.handleMessage(msg);
        }
    };

    public void updateChart(PedometerChartBean bean) {
        final ArrayList<String> xVals = new ArrayList<String>();
        final ArrayList<BarEntry> yVals = new ArrayList<BarEntry>();
        if (bean != null) {
            for (int i = 0; i <= bean.getIndex(); i++) {
                xVals.add(String.valueOf(i) + "分");
                int valY = bean.getArrayData()[i];
                yVals.add(new BarEntry(i,valY));
            }
            time.setText(String.valueOf(bean.getIndex()) + "分");
            BarDataSet set1 = new BarDataSet(yVals,"所走的步数");
            //设置柱子上文字的格式
            set1.setValueFormatter(new IValueFormatter() {
                @Override
                public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                    int v = (int) value;
                    return String.valueOf(v);
                }
            });
            xAxis.setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    if (xVals.size() > value) {
                        return String.valueOf(xVals.get((int) value));
                    }
                    return "";
                }
            });
            xAxis.setLabelCount(xVals.size());
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);//X轴设置显示位置在底部
            xAxis.setGranularity(1f);//设置最小的区间，避免标签的迅速增多
            xAxis.setDrawGridLines(false);//设置竖状的线是否显示
            xAxis.setAxisMinimum(0.55f);//设置x轴显示的起始位置，隐藏掉第0分为0的数据
            //保证Y轴从0开始，不然会上移一点
            leftAxis.setAxisMinimum(0f);
            rightAxis.setAxisMinimum(0f);
            rightAxis.setEnabled(false);//设置右侧y轴关闭
            ArrayList<IBarDataSet> dataSets = new ArrayList<IBarDataSet>();
            dataSets.add(set1);
            BarData data = new BarData(dataSets);
            data.setBarWidth(0.9f);
            data.setValueTextSize(10f);
            dataChart.setData(data);
            dataChart.invalidate();
        }
    }

    public void updateStepCount() {
        if (remoteService != null) {
            int stepCountVal = 0;
            double calorieVal = 0;
            double distanceVal = 0;
            try {
                stepCountVal = remoteService.getStepsCount();
                calorieVal = remoteService.getCalorie();
                distanceVal = remoteService.getDistance();
            } catch (RemoteException e) {
                LogWriter.d(e.toString());
            }
            //更新数据到UI
            stepCount.setText(String.valueOf(stepCountVal) + "步");
            textCalorie.setText(Utils.getFormatVal(calorieVal) + "大卡");
            distance.setText(Utils.getFormatVal(distanceVal) + "公里");
            progressBar.setProgress(stepCountVal);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bindService) {
            bindService = false;
            isRunning = false;
            isChartUpdate = false;
            unbindService(serviceConnection);
        }
    }
}
