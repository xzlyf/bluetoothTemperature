
package com.xz.bluetoothtemperature.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BlueToothUtils {
    private static final String TAG = "BlueToothUtils";
    private Context mContext;
    public static BlueToothUtils sInstance;
    private BluetoothAdapter mBA;
    private SharedPreferences sp;
    // UUID.randomUUID()随机获取UUID
    private final UUID MY_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    // 连接对象的名称
    private final String NAME = "zn_device";

    // 这里本身即是服务端也是客户端，需要如下类
    private BluetoothSocket mSocket;
    private BluetoothDevice mOldDevice;
    private BluetoothDevice mCurDevice;
    // 输出流_客户端需要往服务端输出
    private OutputStream os;
    private InputStream inputStream;
    //线程类的实例
    private AcceptThread ac;
    private Handler mhandler;
    private Handler handler2;

    private BlueToothUtils.Readtask readtask;

    public static synchronized BlueToothUtils getInstance() {
        if (sInstance == null) {
            sInstance = new BlueToothUtils();
        }
        return sInstance;
    }

    public BlueToothUtils() {
        mBA = BluetoothAdapter.getDefaultAdapter();
        ac = new AcceptThread();
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public void setHandler(Handler handler){
        this.mhandler=handler;
    }
    public void setHandler2(Handler handler2){
        this.handler2=handler2;
    }
    public BluetoothAdapter getBA() {
        return mBA;
    }

    public AcceptThread getAc() {
        return ac;
    }

    public BluetoothDevice getCurDevice() {
        return mCurDevice;
    }

    /**
     * 判断是否打开蓝牙
     *
     * @return
     */
    public boolean isEnabled() {
        if (mBA.isEnabled()) {
            return true;
        }
        return false;
    }

    /**
     * 搜索设备
     */
    public void searchDevices() {
        // 判断是否在搜索,如果在搜索，就取消搜索
        if (mBA.isDiscovering()) {
            mBA.cancelDiscovery();
        }
        // 开始搜索
        mBA.startDiscovery();
        Log.e(TAG, "正在搜索...");
    }

    /**
     * 获取已经配对的设备
     *
     * @return
     */
    public List<BluetoothDevice> getBondedDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();
        Set<BluetoothDevice> pairedDevices = mBA.getBondedDevices();
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
     *
     * @param device
     * @param str  设置PIN码
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
        mBA.cancelDiscovery();

        if (!mBA.isEnabled()) {
            mBA.enable();
        }

        if (!BluetoothAdapter.checkBluetoothAddress(strAddr)) { // 检查蓝牙地址是否有效
            Log.d("mylog", "devAdd un effient!");
        }

        BluetoothDevice device = mBA.getRemoteDevice(strAddr);
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
        if (!mBA.isEnabled()) {
            // 弹出对话框提示用户是后打开
            /*Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 1);*/
            // 不做提示，强行打开
            mBA.enable();
            showToast("打开蓝牙");
        } else {
            showToast("蓝牙已打开");
        }
    }

    /**
     * 关闭蓝牙
     */
    public void closeBlueTooth() {
        mBA.disable();
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

    /**
     * 主动连接蓝牙
     *
     * @param device
     */
    public void connectDevice(BluetoothDevice device) {
        // 判断是否在搜索,如果在搜索，就取消搜索
        if (mBA.isDiscovering()) {
            mBA.cancelDiscovery();
        }
        try {
            // 获得远程设备
            if (mCurDevice == null || mCurDevice != mOldDevice) {
                mCurDevice = mBA.getRemoteDevice(device.getAddress());
                mOldDevice = mCurDevice;
                Log.e(TAG, "device:" + mCurDevice);
//                sp.edit().putString("device","device:"+mCurDevice).commit();
                mSocket = mCurDevice.createRfcommSocketToServiceRecord(MY_UUID);
                // 连接
//                mSocket.connect();
//                boolean connected = mSocket.isConnected();
//                if (connected){
//
//                    // 获得输出流
//                    os = mSocket.getOutputStream();
//                    inputStream=mSocket.getInputStream();
//                    while (true){
//                        readDataFromServer();
//                    }
//                }


                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mSocket.connect();
                            os = mSocket.getOutputStream();
                            mhandler.sendEmptyMessage(10);
                            readtask = new BlueToothUtils.Readtask();  //连接成功后开启读取数据的线程
                            readtask.start();
                            mContext.getSharedPreferences("demo",Context.MODE_PRIVATE).edit().putString("device","已连接").commit();
                        } catch (IOException e) {
                            mhandler.sendEmptyMessage(11);
                            e.printStackTrace();
                        }
                    }
                }).start();

            }
            // 如果成功获得输出流

        } catch (IOException e) {
            e.printStackTrace();
            mhandler.sendEmptyMessage(11);

        }





    }


    public static final int CONNECT_FAILED=1;
    public static final int CONNECT_SUCCESS=5;
    public  static final int READ_FAILED=2;
    public static final int WRITE_FAILED=3;
    public  static final int DATA=4;
    private boolean isConnect=false;

    /**
     *开辟连接线程任务
     */
    public void connectTow(final BluetoothDevice device){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                BluetoothSocket tmp = null;
                Method method;
                try {
                    method = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                    tmp = (BluetoothSocket) method.invoke(device, 1);
                } catch (Exception e) {
//                    setState(CONNECT_FAILED);
                    Log.e("TAG", e.toString());
                }
                mSocket = tmp;
                try {
                    mSocket.connect();
                    isConnect = true;
//                    setState(CONNECT_SUCCESS);
                   readtask = new BlueToothUtils.Readtask();  //连接成功后开启读取数据的线程
                    readtask.start();
                } catch (Exception e) {

                    Log.e("TAG", e.toString());
                }
            }
        });
        new Thread(thread).start();
    }
    /**
     *开辟线程读任务
     */
    public class Readtask extends Thread{
        @Override
        public void run(){
//            byte[] buffer = new byte[9999];
//            int bytes;
//            InputStream inputStream ;   //建立输入流读取数据
//            while (true) {
//                try {
//                    inputStream = mSocket.getInputStream();
//                    if ((bytes = inputStream.read(buffer)) > 0) {
//                        byte[] buf_data= new byte[bytes];
//                        for (int i = 0; i < bytes; i++) {
//                            buf_data[i] = buffer[i];
//                        }
//                        String s = new String(buf_data);
//
//                        setState(1,s);
//
//                    }
//                } catch (IOException e) {
//
//                    Log.e("TAG", e.toString());
//                    break;
//                }
//            }



            byte[] tt = new byte[100];
            try {
                int ch;
                inputStream = mSocket.getInputStream();

                int i;
                while (true) {
                    synchronized (this) {
                     i=0;
                        while ((ch=inputStream.read())!='#'){
                            if (ch!=-1){
                                tt[i]=(byte)ch;
                                i++;
                            }


                        }

                        if (tt.length > 0) {
                            String s = new String(tt, "GBK");
                        System.out.println(s);
                            setState(1,s);
                        }else {
                            System.out.println(111);
                        }
                    }
                }


            } catch (IOException e) {
                e.printStackTrace();
                Message message=new Message();
                message.obj="连接已断开，请重新连接";
                handler.sendMessage(message);
            }



            if (mSocket != null) {
                try {
                    mSocket.close();
                } catch (IOException e) {
                    Log.e("TAG", e.toString());
                }
            }
        }
        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
    /**
     *开辟线程写任务
     */
    public  class WriteTask extends Thread{
        private String srt;
        public  WriteTask(String str){
            this.srt=str;
        }
        @Override
        public void run(){
            OutputStream outputStream=null;
            byte[] st=srt.getBytes();
            try{
                outputStream=mSocket.getOutputStream();
                outputStream.write(st);
            }catch (Exception e){

                e.printStackTrace();
            }
        }

    }


    private void setState(int mes,String str){
        Message message=new Message();
        message.what=mes;
        message.obj=str;
        handler2.sendMessage(message);

    }







    /**
     * 传输数据
     *
     * @param message
     */
    public void write(String message) {
        try {
            if (os != null) {
                os.write(message.getBytes("GBK"));
            }
            Log.e(TAG, "write:" + message);
            handler2.sendEmptyMessage(3);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Stop all threads
     */
    public synchronized void stops() {

        if (readtask != null) {readtask.cancel(); readtask = null;}
            Message message=new Message();
        message.obj="关闭所有线程";
        handler.sendMessage(message);
    }


    public void initSP(){
        sp=mContext.getSharedPreferences("demo",Context.MODE_PRIVATE);
    }

    // 服务端，需要监听客户端的线程类
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            showToast(String.valueOf(msg.obj));
            Log.e(TAG, "服务端:" + msg.obj);
            super.handleMessage(msg);
        }
    };


    // 线程服务类
    public class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;
        private BluetoothSocket socket;
        // 输入 输出流
        private OutputStream os;
        private InputStream is;

        public AcceptThread() {
            try {
                serverSocket = mBA.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            // 截获客户端的蓝牙消息
            try {
                socket = serverSocket.accept(); // 如果阻塞了，就会一直停留在这里
                is = socket.getInputStream();
                os = socket.getOutputStream();
                while (true) {
                    synchronized (this) {
                        byte[] tt = new byte[is.available()];
                        if (tt.length > 0) {
                            is.read(tt, 0, tt.length);
                            Message msg = new Message();
                            msg.obj = new String(tt, "GBK");
                            Log.e(TAG, "客户端:" + msg.obj);
                            handler.sendMessage(msg);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}