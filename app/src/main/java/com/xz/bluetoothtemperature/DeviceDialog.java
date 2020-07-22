package com.xz.bluetoothtemperature;

import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.xz.bluetoothtemperature.bluetooth.BlueToothUtils;

/**
 * @author czr
 * @date 2020/7/22
 */
public class DeviceDialog extends Dialog {
    private Context mContext;
    private BluetoothDevice mDevice;
    private TextView title;
    private TextView tv1;
    private TextView tv2;
    private TextView tv3;
    private TextView tv4;

    private BlueToothUtils blueToothUtils;

    public DeviceDialog(Context context) {
        this(context, 0);
    }

    public DeviceDialog(Context context, int themeResId) {
        super(context, themeResId);
        this.mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_device);
        Window window = getWindow();
        assert window != null;
        window.setBackgroundDrawableResource(R.drawable.bg_cicle_common);
        WindowManager.LayoutParams lp = window.getAttributes();
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        lp.width = (int) (dm.widthPixels * 0.3);
        lp.dimAmount = 0.2f;
        window.setAttributes(lp);
        blueToothUtils = BlueToothUtils.getInstance();
        initView();
    }

    public void setDevice(BluetoothDevice device) {
        this.mDevice = device;
    }

    @Override
    public void show() {
        super.show();
        if (mDevice != null) {

            int state = mDevice.getBondState();
            title.setText(mDevice.getName() == null ? mDevice.getAddress() : mDevice.getName() + ":" + getBoudSt(state));

            if (state == BluetoothDevice.BOND_BONDED) {
                tv2.setVisibility(View.VISIBLE);
                tv1.setVisibility(View.GONE);
            } else if (state == BluetoothDevice.BOND_NONE) {
                tv1.setVisibility(View.VISIBLE);
                tv2.setVisibility(View.GONE);
            } else {
                tv1.setVisibility(View.GONE);
                tv2.setVisibility(View.GONE);
            }


        }

    }

    private void initView() {
        title = findViewById(R.id.title);
        tv1 = findViewById(R.id.tv1);
        tv2 = findViewById(R.id.tv2);
        tv3 = findViewById(R.id.tv3);
        tv4 = findViewById(R.id.tv4);
        tv1.setOnClickListener(clickListener);
        tv2.setOnClickListener(clickListener);
        tv3.setOnClickListener(clickListener);
        tv4.setOnClickListener(clickListener);
    }

    private String getBoudSt(int b) {
        switch (b) {
            case BluetoothDevice.BOND_BONDED:
                return "已配对";
            case BluetoothDevice.BOND_BONDING:
                return "正在配对";
            case BluetoothDevice.BOND_NONE:
                return "未配对";
            default:
                return b + "";
        }
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.tv1:
                    blueToothUtils.createBond(mDevice);
                    break;
                case R.id.tv2:
                    blueToothUtils.removeBond(mDevice);
                    break;
            }
            dismiss();
        }
    };
}
