package com.xz.bluetoothtemperature;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.orhanobut.logger.Logger;
import com.xz.bluetoothtemperature.bluetooth.BlueToothUtils;
import com.xz.bluetoothtemperature.entitiy.TempValueEntity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Switch switchBluetooth;
    private Button searchButton;
    private ListView bondedList;
    private ListView newDeviceList;
    private DeviceAdapter bondedAdapter;
    private DeviceAdapter newbondedAdapter;

    private BlueToothUtils blueToothUtils;
    private BluetoothBroadcastReceiver mBluetoothReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPermission();
        EventBus.getDefault().register(this);
        blueToothUtils = BlueToothUtils.getInstance();
        registerReceiver();
        initView();

    }

    private void initPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("警告")
                    .setMessage("连接蓝牙设备需要开启以下权限\n拒绝将无法正常开启该功能")
                    .setPositiveButton("继续", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                                            Manifest.permission.ACCESS_FINE_LOCATION},
                                    12345);
                            dialog.dismiss();
                            dialog.cancel();

                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            dialog.cancel();
                        }
                    })
                    .create();

            dialog.show();

        }
    }

    private void initView() {
        switchBluetooth = findViewById(R.id.switchBluetooth);
        searchButton = findViewById(R.id.searchButton);
        bondedList = findViewById(R.id.bondedDeviceList);
        newDeviceList = findViewById(R.id.newDeviceList);
        newbondedAdapter = new DeviceAdapter(MainActivity.this, R.layout.item_device, new ArrayList<BluetoothDevice>());
        newDeviceList.setAdapter(newbondedAdapter);

        switchBluetooth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!blueToothUtils.isEnabled()) {
                        blueToothUtils.openBlueTooth();
                    }

                } else {
                    if (blueToothUtils.isEnabled())
                        blueToothUtils.closeBlueTooth();
                }
            }
        });
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!blueToothUtils.isEnabled()) {
                    Toast.makeText(MainActivity.this, "请先打开蓝牙!", Toast.LENGTH_SHORT).show();
                    return;
                }

                clearList();
                //开始搜索蓝牙
                blueToothUtils.searchDevices();
                //刷新已配对列表
                refreshBondedList(blueToothUtils.getBondedDevices());

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (blueToothUtils.getBA() == null) {
            switchBluetooth.setChecked(false);
            switchBluetooth.setEnabled(false);
            switchBluetooth.setText("未找到蓝牙模块");
            searchButton.setEnabled(false);
        }
        switchBluetooth.setChecked(blueToothUtils.isEnabled());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        unregisterReceiver();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateView(TempValueEntity entity) {

    }

    private void clearList() {
        if (bondedAdapter != null) {
            bondedAdapter.clear();
        }
        if (newbondedAdapter != null) {
            newbondedAdapter.clear();
        }
    }

    /**
     * 刷新已绑定列表
     *
     * @param list
     */
    private void refreshBondedList(List<BluetoothDevice> list) {
        if (bondedAdapter == null) {
            bondedAdapter = new DeviceAdapter(MainActivity.this, R.layout.item_device, list);
            bondedList.setAdapter(bondedAdapter);
        } else {
            bondedAdapter.refreh(list);
        }
    }


    /**
     * 一个个的添加未绑定列表
     *
     * @param device
     */
    private void addNewBondedDevice(BluetoothDevice device) {
        newbondedAdapter.add(device);
    }

    /**
     * 改变蓝牙开关状态
     *
     * @param open
     */
    private void changeSwitchState(boolean open) {
        switchBluetooth.setChecked(open);
    }

    /**
     * 改变搜索按钮的状态
     *
     * @param text
     * @param isEnable
     */
    private void setSearchButtonState(String text, boolean isEnable) {
        searchButton.setText(text);
        searchButton.setEnabled(isEnable);
    }


    /**
     * 注册接收广播
     */
    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);//配对状态广播
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);//状态改变广播
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);//连接状态广播
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);//发现设备广播
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//扫描完成广播
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);//扫描开始广播
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);//配对请求广播
        mBluetoothReceiver = new BluetoothBroadcastReceiver();
        registerReceiver(mBluetoothReceiver, intentFilter);
    }

    /**
     * 注销接收广播
     */
    private void unregisterReceiver() {
        if (mBluetoothReceiver != null) {
            unregisterReceiver(mBluetoothReceiver);
            mBluetoothReceiver = null;
        }
    }


    /**
     * 蓝牙接收广播处理
     */
    class BluetoothBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            handleReceive(intent);
        }
    }

    /**
     * 蓝牙广播处理
     *
     * @param intent
     */
    private void handleReceive(Intent intent) {
        String action = intent.getAction();
        if (action == null)
            return;
        Logger.i(action);
        switch (action) {
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                //蓝牙状态改变
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if (state == BluetoothAdapter.STATE_TURNING_ON) {
                    //由关转向开
                    changeSwitchState(true);
                    Logger.d("蓝牙打开");
                } else if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                    //由开转向关
                    changeSwitchState(false);
                    clearList();
                    Logger.d("蓝牙关闭");
                }

                break;
            case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                //开始扫描
                setSearchButtonState("正在搜索...", false);
                break;
            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                //结束扫描
                setSearchButtonState("搜索设备", true);
                break;
            case BluetoothDevice.ACTION_FOUND: {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) return;
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    //已经配对
                } else {
                    //没有配对
                    addNewBondedDevice(device);
                }
            }
            break;

        }
    }


}
