// IPedometerService.aidl
package com.imooc.step.service;
import com.imooc.step.beans.PedometerChartBean;

interface IPedometerService {
    //开始计步
    void startCount();
    //结束计步
    void stopCount();
    //重置计步器步数
    void resetCount();
    //获取计步器步数
    int getStepsCount();
    //获取消耗的卡路里
    double getCalorie();
    //获取走路的距离
    double getDistance();
    //保存数据
    void saveData();
    //设置传感器灵敏度
    void setSensitivity(double sensitivity);
    //获取传感器灵敏度
    double getSensitivity();
    //设置采样时间
    void setInterval(int interval);
    //获取采样时间
    int getInterval();
    //获取开始时间戳
    long getStartTimeStamp();
    //获取服务运行状态
    int getServiceRunningStatus();
    //获取运动图表数据
    PedometerChartBean getChartData();
}
