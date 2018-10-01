package com.imooc.step.service;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import com.imooc.step.beans.PedometerBean;
import com.imooc.step.beans.PedometerChartBean;
import com.imooc.step.db.DBHelper;
import com.imooc.step.frame.FrameApplication;
import com.imooc.step.utils.ACache;
import com.imooc.step.utils.Settings;
import com.imooc.step.utils.Utils;

public class PedometerService extends Service {

    private SensorManager sensorManager;
    private PedometerBean pedometerBean;
    private PedometerListener pedometerListener;
    public static final int STATUS_NOT_RUN = 0;
    public static final int STATUS_RUNNING = 1;
    private int runStatus = STATUS_NOT_RUN;
    private static final long SAVE_CHART_TIME = 60000L;
    private Settings settings;
    private PedometerChartBean pedometerChartBean;

    private static Handler handler = new Handler();

    private Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            if (runStatus == STATUS_RUNNING) {
                if (handler != null && pedometerChartBean != null) {
                    handler.removeCallbacks(timeRunnable);
                    updateChartData();//更新数据
                    handler.postDelayed(timeRunnable,SAVE_CHART_TIME);
                }
            }
        }
    };

    public double getCalorieBySteps(int stepCount) {
        //步长
        float stepLen = settings.getStepLength();
        //体重
        float bodyWeight = settings.getBodyWeight();
        double METRIC_WALKING_FACTOR = 0.708;//走路
        double METRIC_RUNNING_FACTOR = 1.02784823;//跑步
        // 跑步热量（kcal）=体重（kg）*距离（公里）*1.02784823
        // 走路热量（kcal）=体重（kg）*距离（公里）*0.708
        double calories = (bodyWeight * METRIC_WALKING_FACTOR) * stepLen * stepCount / 100000.0;
        return calories;
    }

    public double getStepDistance(int stepCount) {
        //步长
        float stepLen = settings.getStepLength();
        double distance = (stepCount * (long)(stepLen)) / 100000.0f;
        return distance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        pedometerBean = new PedometerBean();
        pedometerListener = new PedometerListener(pedometerBean);
        pedometerChartBean = new PedometerChartBean();
        settings = new Settings(this);
    }

    //更新了计步器的图表数据
    private void updateChartData() {
        if (pedometerChartBean.getIndex() < 1441 - 1) {
            pedometerChartBean.setIndex(pedometerChartBean.getIndex() + 1);
            pedometerChartBean.getArrayData()[pedometerChartBean.getIndex()] = pedometerBean.getStepCount();
        }
    }

    /**
     *  将对象保存
     */
    private void saveChartData() {
        String jsonStr = Utils.objToJson(pedometerChartBean);
        ACache.get(FrameApplication.getInstance()).put("JsonChartData",jsonStr);
    }

    private IPedometerService.Stub iPedometerService = new IPedometerService.Stub() {
        @Override
        public void startCount() throws RemoteException {
            if (sensorManager != null && pedometerListener != null) {
                Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                sensorManager.registerListener(pedometerListener,sensor,SensorManager.SENSOR_DELAY_NORMAL);
                pedometerBean.setStartTime(System.currentTimeMillis());
                pedometerBean.setDay(Utils.getTimestampByDay()); //记录的是哪一天的数据
                runStatus = STATUS_RUNNING;
                handler.postDelayed(timeRunnable,SAVE_CHART_TIME);//开始触发数据刷新
            }
        }

        @Override
        public void stopCount() throws RemoteException {
            if (sensorManager != null && pedometerListener != null) {
                Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                sensorManager.unregisterListener(pedometerListener,sensor);
                runStatus = STATUS_NOT_RUN;
                handler.removeCallbacks(timeRunnable);
            }
        }

        @Override
        public void resetCount() throws RemoteException {
            if (pedometerBean != null) {
                pedometerBean.reset();
                saveData();
            }
            if (pedometerChartBean != null) {
                pedometerChartBean.reset();
                saveChartData(); //清零之后的数据也保存一下，防止清零之后下次再进页面还是清零之前的缓存
            }
            if (pedometerListener != null) {
                pedometerListener.setCurrentSteps(0);
            }
        }

        @Override
        public int getStepsCount() throws RemoteException {
            if (pedometerBean != null) {
                return pedometerBean.getStepCount();
            }
            return 0;
        }

        @Override
        public double getCalorie() throws RemoteException {
            if (pedometerBean != null) {
                return getCalorieBySteps(pedometerBean.getStepCount());
            }
            return 0;
        }

        @Override
        public double getDistance() throws RemoteException {
            return getDistanceVal();
        }

        private double getDistanceVal() {
            if (pedometerBean != null) {
                return getStepDistance(pedometerBean.getStepCount());
            }
            return 0;
        }

        @Override
        public void saveData() throws RemoteException {
            if (pedometerBean != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        DBHelper dbHelper = new DBHelper(PedometerService.this,DBHelper.DB_NAME);
                        //设置距离
                        pedometerBean.setDistance(getDistanceVal());
                        //设置热量消耗
                        pedometerBean.setCalorie(getCalorieBySteps(pedometerBean.getStepCount()));
                        long time = (pedometerBean.getLastStepTime() - pedometerBean.getStartTime()) / 1000;
                        if (time == 0) {
                            pedometerBean.setPace(0);//设置多少步/分钟
                            pedometerBean.setSpeed(0);
                        } else {
                            int pace = Math.round(60 * pedometerBean.getStepCount() / time);
                            pedometerBean.setPace(pace);
                            long speed = Math.round(pedometerBean.getDistance() / (time / (60 * 60))); //km/h
                            pedometerBean.setSpeed(speed);
                        }
                        dbHelper.writeToDatabase(pedometerBean);
                    }
                }).start();
            }
        }

        @Override
        public void setSensitivity(double sensitivity) throws RemoteException {
//            if (settings != null) {
//                settings.setSensitivity((float) sensitivity);
//            }
            if (pedometerListener != null) {
                pedometerListener.setSensitivity((float) sensitivity);
            }
        }

        @Override
        public double getSensitivity() throws RemoteException {
            if (settings != null) {
                return settings.getSensitivity();
            }
            return 0;
        }

        @Override
        public void setInterval(int interval) throws RemoteException {
//            if (settings != null) {
//                settings.setInterval(interval);
//            }
            if (pedometerListener != null) {
                pedometerListener.setLimit(interval);
            }
        }

        @Override
        public int getInterval() throws RemoteException {
            if (settings != null) {
                return settings.getInterval();
            }
            return 0;
        }

        @Override
        public long getStartTimeStamp() throws RemoteException {
            if (pedometerBean != null) {
                return pedometerBean.getStartTime();
            }
            return 0L;
        }

        @Override
        public int getServiceRunningStatus() throws RemoteException {
            return runStatus;
        }

        @Override
        public PedometerChartBean getChartData() throws RemoteException {
            return pedometerChartBean;
        }
    };
    @Override
    public IBinder onBind(Intent intent) {
        return iPedometerService;
    }
}
