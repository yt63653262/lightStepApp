package com.imooc.step.beans;

import android.os.Parcel;
import android.os.Parcelable;

public class PedometerChartBean implements Parcelable {
    private int[] arrayData;
    private int index;

    public PedometerChartBean() {
        index = 0;
        arrayData = new int[1441];
    }

    public void reset() {
        index = 0;
        for (int i = 0; i < arrayData.length; i++) {
            arrayData[i] = 0;
        }
    }

    protected PedometerChartBean(Parcel in) {
        arrayData = in.createIntArray();
        index = in.readInt();
    }

    public static final Creator<PedometerChartBean> CREATOR = new Creator<PedometerChartBean>() {
        @Override
        public PedometerChartBean createFromParcel(Parcel in) {
            return new PedometerChartBean(in);
        }

        @Override
        public PedometerChartBean[] newArray(int size) {
            return new PedometerChartBean[size];
        }
    };

    public void setArrayData(int[] arrayData) {
        this.arrayData = arrayData;
    }

    public int[] getArrayData() {
        return arrayData;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) { //写入和读取的顺序需对应，即先写入的先读取
        dest.writeIntArray(arrayData);
        dest.writeInt(index);
    }
}
