package com.xz.bluetoothtemperature.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.xz.bluetoothtemperature.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 建立和管理与其他设备的BT连接。
 * <p>
 * 它有一个线程来侦听传入的连接，一个线程
 * <p>
 * 用于与设备连接，以及用于执行数据的线程
 * <p>
 * 连接时的传输。
 */
public class BTService {

    // 表示当前连接状态的常量
    public static final int STATE_NONE = 0;         // 我们什么也不做
    public static final int STATE_LISTEN = 1;       // 现在监听传入的连接
    public static final int STATE_CONNECTING = 2;   // 现在启动输出连接
    public static final int STATE_CONNECTED = 3;    // 现在连接到远程设备
    private static final String TAG = "BTConnection";
    // 创建服务器套接字时SDP记录的名称
    private static final String MY_NAME = "BTConnection";

//引用：[来自Android SDK文档]

//如果您正在连接蓝牙串行板，则尝试使用众所周知的

//SPP UUID 000 000 110～00 0 0 0 0 0 0 0 0 0 0 0 5 5 F9B34 FB。

    //    但是，如果您正在连接Android对等体，那么请生成您自己的
//
///独特的UUID。
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // 成员字段
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private final Context mContext;
    public static BTService sInstance;

    /**
     * Constructor. Prepares a new BT session.
     *
     * @param handler A Handler to send message back to the UI Activity.
     *                构造函数。准备新的BT会话。 * * @param handler一个Handler，用于将消息发送回UI Activity。
     */
    public BTService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
        mContext = context;
    }


    /**
     * 判断是否打开蓝牙
     *
     * @return
     */
    public boolean isEnabled() {
        if (mAdapter.isEnabled()) {
            return true;
        }
        return false;
    }

    /**
     * 搜索设备
     */
    public void searchDevices() {
        // 判断是否在搜索,如果在搜索，就取消搜索
        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }
        // 开始搜索
        mAdapter.startDiscovery();
        Log.e(TAG, "正在搜索...");
    }

    /**
     * 获取已经配对的设备
     *
     * @return
     */
    public List<BluetoothDevice> getBondedDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();
        Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();
        // 判断是否有配对过的设备
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                devices.add(device);
                Log.e(TAG, "BondedDevice:" + device.getName());
            }
        }
        return devices;
    }

    /**
     * 与设备配对
     *
     * @param device
     */
    public void createBond(BluetoothDevice device) {
        try {
            Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
            createBondMethod.invoke(device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 与设备解除配对
     *
     * @param device
     */
    public void removeBond(BluetoothDevice device) {
        try {
            Method removeBondMethod = device.getClass().getMethod("removeBond");
            removeBondMethod.invoke(device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param device
     * @param str    设置PIN码
     * @return
     */
    public boolean setPin(BluetoothDevice device, String str) {
        try {
            Method removeBondMethod = device.getClass().getDeclaredMethod("setPin",
                    new Class[]{byte[].class});
            Boolean returnValue = (Boolean) removeBondMethod.invoke(device,
                    new Object[]{str.getBytes()});
            Log.e("returnValue", "" + returnValue);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 取消用户输入
     */
    public boolean cancelPairingUserInput(BluetoothDevice device) {
        Boolean returnValue = false;
        try {
            Method createBondMethod = device.getClass().getMethod("cancelPairingUserInput");
            returnValue = (Boolean) createBondMethod.invoke(device);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        // cancelBondProcess()
        return returnValue.booleanValue();
    }

    /**
     * 取消配对
     */
    public boolean cancelBondProcess(BluetoothDevice device) {
        Boolean returnValue = null;
        try {
            Method createBondMethod = device.getClass().getMethod("cancelBondProcess");
            returnValue = (Boolean) createBondMethod.invoke(device);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return returnValue.booleanValue();
    }

    /**
     * @param strAddr
     * @param strPsw
     * @return
     */
    public boolean pair(String strAddr, String strPsw) {
        boolean result = false;
        mAdapter.cancelDiscovery();

        if (!mAdapter.isEnabled()) {
            mAdapter.enable();
        }

        if (!BluetoothAdapter.checkBluetoothAddress(strAddr)) { // 检查蓝牙地址是否有效
            Log.d("mylog", "devAdd un effient!");
        }

        BluetoothDevice device = mAdapter.getRemoteDevice(strAddr);
        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
            Log.d("mylog", "NOT BOND_BONDED");
            try {
                setPin(device, strPsw); // 手机和蓝牙采集器配对
                createBond(device);
                result = true;
            } catch (Exception e) {
                Log.d("mylog", "setPiN failed!");
                e.printStackTrace();
            } //

        } else {
            Log.d("mylog", "HAS BOND_BONDED");
            try {
                createBond(device);
                setPin(device, strPsw); // 手机和蓝牙采集器配对
                createBond(device);
                result = true;
            } catch (Exception e) {
                Log.d("mylog", "setPiN failed!");
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 获取device.getClass()这个类中的所有Method
     *
     * @param clsShow
     */
    public void printAllInform(Class clsShow) {
        try {
            // 取得所有方法
            Method[] hideMethod = clsShow.getMethods();
            int i = 0;
            for (; i < hideMethod.length; i++) {
                Log.e("method name", hideMethod[i].getName() + ";and the i is:" + i);
            }
            // 取得所有常量
            Field[] allFields = clsShow.getFields();
            for (i = 0; i < allFields.length; i++) {
                Log.e("Field name", allFields[i].getName());
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开蓝牙
     */
    public void openBlueTooth() {
        if (!mAdapter.isEnabled()) {
            // 弹出对话框提示用户是后打开
            /*Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 1);*/
            // 不做提示，强行打开
            mAdapter.enable();
            showToast("打开蓝牙");
        } else {
            showToast("蓝牙已打开");
        }
    }

    /**
     * 关闭蓝牙
     */
    public void closeBlueTooth() {
        mAdapter.disable();
        showToast("关闭蓝牙");
    }

    /**
     * 弹出Toast窗口
     *
     * @param message
     */
    private void showToast(String message) {
        if (mContext != null) {
            Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
        } else {
            Log.e(TAG, "message:" + message);
        }
    }


    public static synchronized BTService getInstance(Context context, Handler handler) {
        if (sInstance == null) {
            sInstance = new BTService(context, handler);
        }
        return sInstance;
    }


    public BluetoothAdapter getBA() {
        return mAdapter;
    }

    /**
     * Return the current connection state.
     *
     * @return mState current connection state
     * 返回当前连接状态。 * * @return mState当前连接状态
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Set the current state of BT connection.
     *
     * @param state An integer defining the current connection state
     *              <p>
     *              *设置BT连接的当前状态。 * * @param state定义当前连接状态的整数
     */
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + "-> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        //将新状态提供给处理程序，以便UI活动可以更新
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Start the BT service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     * *启动BT服务。具体来说，启动AcceptThread以在侦听（服务器）模式下开始*会话。由Activity onResume（）调用
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        //取消任何尝试建立连接的线程
        if (mConnThread != null) {
            mConnThread.cancel();
            mConnThread = null;
        }

        // Cancel any thread currently running a connection
        //取消当前正在运行连接的任何线程
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        //启动线程以侦听BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }

    /**
     * Start the ConnThread to initiate a connection to a remote device.
     * 启动ConnThread以启动与远程设备的连接。
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "Connect to: " + device);

        // Cancel any thread attempting to make a connection
        //取消任何尝试建立连接的线程
        if (mState == STATE_CONNECTING) {
            if (mConnThread != null) {
                mConnThread.cancel();
                mConnThread = null;
            }
        }
        // Cancel any thread currently running a connection
        //取消当前正在运行连接的任何线程
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        // Start the thread to connect with the given device
        //启动线程以连接给定设备
        mConnThread = new ConnectThread(device);
        mConnThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * 启动ConnectedThread以开始管理蓝牙连接。
     *
     * @param socket 连接所在的BluetoothSocket
     * @param device 已连接的BluetoothDevice
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "Connected");

        // 取消完成连接的线程
        if (mConnThread != null) {
            mConnThread.cancel();
            mConnThread = null;
        }
        //取消当前正在运行连接的任何线程
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        //取消接受线程，因为我们只想连接到一个设备
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        //启动线程来管理连接并执行传输
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // 将已连接设备的名称发送回UI活动
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }


    /**
     * 停止所有线程。
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnThread != null) {
            mConnThread.cancel();
            mConnThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * 以不同步的方式写入ConnectedThread。
     *
     * @param content 要写入的字节
     * @see
     */
    public void write(String content) {
        //创建临时对象
        ConnectedThread r;
        //同步ConnectedThread的副本
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                Log.d(TAG, "State: " + getState());
                return;
            }
            r = mConnectedThread;
        }
        //执行写入不同步
        r.write(content);
    }

    /**
     * 指示连接尝试失败并通知UI活动。
     */
    private void connectionError(String errMsg) {
        Log.e(TAG, "Connection Error:" + errMsg);
        // 将失败消息发送回活动
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, errMsg);
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // 启动服务以重新启动侦听模式
        BTService.this.start();
    }


    /**
     * 此线程在侦听传入连接时运行。它表现得很好
     * 像服务器端客户端。它会一直运行，直到接受连接
     * （或直到取消）。
     */
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            //使用稍后分配给mmServerSocket的临时对象
            //因为mmServerSocket是最终的
            BluetoothServerSocket tmp = null;

            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(MY_NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "套接字听不通", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "套接字开始");

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    //这是一个阻止调用，只会返回
                    //成功连接或异常
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "套接字accept（）失败", e);
                    break;
                }

                //如果接受了连接
                if (socket != null) {
                    synchronized (BTService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                //情况正常启动连接的线程。
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                //未准备好或已连接。终止新套接字。
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "无法关闭不需要的套接字", e);
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            Log.d(TAG, "套接字取消");
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "服务器的套接字close（）失败", e);
            }
        }
    }

    /***此线程在尝试进行传出连接时运行
     *带有设备。它直接通过;连接
     *成功或失败。
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        private ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            //获取与之连接的BluetoothSocket
            //给出了BluetoothDevice
            try {
//                device.fetchUuidsWithSdp();
//                ParcelUuid[] uuids = device.getUuids();
//                for (ParcelUuid u : uuids) {
//                    Log.d(TAG, u.getUuid().toString());
//                }
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            try {
                mmSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    mmSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                connectionError("连接失败");
                return;
            }

            //重置ConnectThread，因为我们已经完成了
            synchronized (BTService.this) {
                mConnThread = null;
            }

            //启动连接的线程
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            Log.d(TAG, "Socket cancel");
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket close() of server failed", e);
            }
        }
    }

    /**
     * 此线程在与远程设备连接期间运行。
     * 它处理所有传入和传出传输。
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        private ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //获取BluetoothSocket输入和输出流
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.d(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[512];
            byte[] ret = null;

            //连接时继续收听inputStream
            while (true) {
                try {
                    //从InputStream中读取
                    int bytes = mmInStream.read(buffer);
                    Log.e(TAG, "length: " + bytes);

                    for (int i = 0; i < bytes; i++) {
                        if ((buffer[i] & 0xFF) == 255) {
                            ret = Arrays.copyOfRange(buffer, 0, i + 1);
                            break;
                        }
                    }
                    Log.e(TAG, "data: " + ret);
                    //将获取的字节发送到UI活动
                    mHandler.obtainMessage(Constants.MESSAGE_READ, -1, -1, ret).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionError("连接失败");
                    //启动服务以重新启动侦听模式
                    BTService.this.start();
                    break;
                }
            }
        }

        /**
         * 写入已连接的OutStream。
         *
         * @param content The bytes to write
         */
        public void write(String content) {
            try {
                mmOutStream.write(content.getBytes());
                // 将发送的消息共享回UI活动
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, content).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "E写入期间的xception", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "connect（）的连接套接字失败", e);
            }
        }
    }
}