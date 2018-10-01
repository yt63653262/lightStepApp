package com.imooc.step;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.imooc.step.frame.BaseActivity;
import com.imooc.step.frame.LogWriter;
import com.imooc.step.service.IPedometerService;
import com.imooc.step.service.PedometerService;
import com.imooc.step.utils.Settings;
import com.imooc.step.utils.Utils;

public class SettingActivity extends BaseActivity {

    private ListView settingListView;
    private ImageView back;

    static class ViewHolder {
        TextView title;
        TextView desc;
    }
    public class SettingListAdapter extends BaseAdapter {
        private Settings settings = null;

        public SettingListAdapter() {
            settings = new Settings(SettingActivity.this);
        }
        private String[] listTitle = {"设置步长","设置体重","传感器灵敏度","传感器采样时间"};
        @Override
        public int getCount() {
            return listTitle.length;
        }

        @Override
        public Object getItem(int position) {
            if (listTitle != null && position < listTitle.length) {
                return listTitle[position];
            }
            return 0;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private void stepClick(float stepLen) {
            AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
            builder.setTitle("设置步长");
            View view = View.inflate(SettingActivity.this,R.layout.view_dlg_input,null);
            final EditText input = view.findViewById(R.id.input);
            input.setText(String.valueOf(stepLen));
            builder.setView(view);
            builder.setNegativeButton("取消",null);
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String val = input.getText().toString();
                    if (val != null && val.length() > 0) {
                        float len = Float.parseFloat(val);
                        settings.setStepLength(len);
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        Toast.makeText(SettingActivity.this,"请输入正确的参数！",Toast.LENGTH_LONG).show();
                    }
                }
            });
            builder.create().show();
        }

        private void weightClick(float weight) {
            AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
            builder.setTitle("设置体重");
            View view = View.inflate(SettingActivity.this,R.layout.view_dlg_input,null);
            final EditText input = view.findViewById(R.id.input);
            input.setText(String.valueOf(weight));
            builder.setView(view);
            builder.setNegativeButton("取消",null);
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String val = input.getText().toString();
                    if (val != null && val.length() > 0) {
                        float bodyWeight = Float.parseFloat(val);
                        settings.setBodyWeight(bodyWeight);
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        Toast.makeText(SettingActivity.this,"请输入正确的参数！",Toast.LENGTH_LONG).show();
                    }
                }
            });
            builder.create().show();
        }

        private void sensitiveClick() {
            AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
            builder.setItems(R.array.sensitive_array, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //调用服务，设置灵敏度
                    if (remoteService != null) {
                        try {
                            remoteService.setSensitivity(Settings.SENSITIVE_ARRAY[which]);
                        } catch (RemoteException e) {
                            LogWriter.d(e.toString());
                        }
                    }
                    settings.setSensitivity(Settings.SENSITIVE_ARRAY[which]);
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                }
            });
            builder.setTitle("传感器灵敏度");
            builder.create().show();
        }

        private void setInterval() {
            AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
            builder.setItems(R.array.interval_array, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //调用服务，设置时间间隔
                    if (remoteService != null) {
                        try {
                            remoteService.setInterval(Settings.INTERVAL_ARRAY[which]);
                        } catch (RemoteException e) {
                            LogWriter.d(e.toString());
                        }
                    }
                    settings.setInterval(Settings.INTERVAL_ARRAY[which]);
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                }
            });
            builder.setTitle("设置传感器采样时间间隔");
            builder.create().show();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = null;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = View.inflate(SettingActivity.this,R.layout.item_setting,null);
                viewHolder.title = (TextView) convertView.findViewById(R.id.title);
                viewHolder.desc = (TextView) convertView.findViewById(R.id.desc);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.title.setText(listTitle[position]);
            switch (position) {
                case 0: {
                    final float stepLen = settings.getStepLength();
                    viewHolder.desc.setText(String.format("计算距离和消耗的热量：%s CM",stepLen));
                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //设置步长
                            stepClick(stepLen);
                        }
                    });
                }
                break;
                case 1: {
                    final float bodyWeight = settings.getBodyWeight();
                    viewHolder.desc.setText(String.format("通过体重计算消耗的热量：%s Kg",bodyWeight));
                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //设置体重
                            weightClick(bodyWeight);
                        }
                    });
                }
                break;
                case 2: {
                    double sensitivity = settings.getSensitivity();
                    viewHolder.desc.setText(String.format("传感器的敏感程度：%s",Utils.getFormatVal(sensitivity,"#.00")));//(0.1)->(.10)
                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //传感器敏感度
                            sensitiveClick();
                        }
                    });
                }
                break;
                case 3: {
                    int interval = settings.getInterval();
                    viewHolder.desc.setText(String.format("每隔：%s毫秒进行一次数据采集", Utils.getFormatVal(interval,"#.00")));
                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //传感器采样时间间隔
                            setInterval();
                        }
                    });
                }
                break;
                default: {
                    LogWriter.d("Position = " + position);
                }
            }
            return convertView;
        }
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        super.onDestroy();
    }

    @Override
    protected void onInitVariable() {

    }

    @Override
    protected void onInitView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_setting);
        settingListView = (ListView) findViewById(R.id.listView);
        back = (ImageView) findViewById(R.id.imageView);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        settingListView.setAdapter(adapter);
    }
    private SettingListAdapter adapter = new SettingListAdapter();

    private IPedometerService remoteService;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            remoteService = IPedometerService.Stub.asInterface(service);
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
            serviceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        bindService(serviceIntent,serviceConnection,BIND_AUTO_CREATE);
    }
}
