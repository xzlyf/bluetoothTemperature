package com.xz.bluetoothtemperature;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.orhanobut.logger.Logger;

import java.util.List;

/**
 * @author czr
 * @date 2020/7/21
 */
public class DeviceAdapter extends ArrayAdapter<BluetoothDevice> {
    private final int resourceId;

    public DeviceAdapter(Context context, int resourceId, List<BluetoothDevice> list) {
        super(context, resourceId, list);
        this.resourceId = resourceId;
    }

    /**
     * 刷新数据
     *
     * @param list
     */
    public void refreh(List<BluetoothDevice> list) {
        clear();
        addAll(list);
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
        TextView t1 = view.findViewById(R.id.tv1);
        TextView t2 = view.findViewById(R.id.tv2);
        BluetoothDevice device = getItem(position);
        if (device != null) {
            t1.setText(device.getName());
            t2.setText(device.getAddress());
        }
        return view;
    }

}
