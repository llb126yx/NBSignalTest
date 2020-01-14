package com.lb.nbtest;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;

public class UsbSerialControl {
    private static final String TAG = UsbSerialControl.class.getSimpleName();
    private Context mContext;
    private UsbManager mUsbManager; //USB管理器
    private UsbSerialPort mPort = null;
    private int mBaudRate = 9600;
    private int mDataBits = 8;
    private int mStopBits = UsbSerialPort.STOPBITS_1;
    private int mParity = UsbSerialPort.PARITY_NONE;
    public boolean isOpen = false;
    private Handler mHandler;
    private SerialInputOutputManager mSerialIoManager;  //输入输出管理器（本质是一个Runnable）
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();  //用于不断从端口读取数据
    //数据输入输出监听器
    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    Log.d(TAG, "new data.");
                    ((Activity)mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //更新UI
                        }
                    });
                }
            };

    public UsbSerialControl(Context context) {
        mContext = context;
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
    }

    public boolean  openPort() {
        isOpen = false;
        mPort = null;
        stopIoManager();
        //mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        //全部设备
        List<UsbSerialDriver> usbSerialDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
        //全部端口
        List<UsbSerialPort> usbSerialPorts = new ArrayList<UsbSerialPort>();
        for (UsbSerialDriver driver : usbSerialDrivers) {
            List<UsbSerialPort> ports = driver.getPorts();
            Log.d(TAG, String.format("+ %s: %s port%s",
                    driver, Integer.valueOf(ports.size()), ports.size() == 1 ? "" : "s"));
            usbSerialPorts.addAll(ports);
        }
        if(usbSerialPorts.size() > 0) {
            mPort = usbSerialPorts.get(0); //取第一个设备
        }
        if (mPort != null) {
            //成功获取端口，打开连接
            UsbDeviceConnection connection = mUsbManager.openDevice(mPort.getDriver().getDevice());
            if (connection == null) {
                Log.e(TAG, "Opening device failed");
                return false;
            }
            try {
                mPort.open(connection);
                //设置波特率
                mPort.setParameters(mBaudRate, mDataBits, mStopBits, mParity);
                isOpen = true;
                return true;
            } catch (IOException e) {
                //打开端口失败，关闭！
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                try {
                    mPort.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                mPort = null;
                return false;
            }
        } else {
            return false;
        }
    }

//    public void onDisConnect(){
//        stopIoManager();
//        if (mPort != null) {
//            try {
//                mPort.close();
//            } catch (IOException e) {
//                // Ignore.
//            }
//            mPort = null;
//        }
//    }

    public void onDeviceStateChange() {
        //重新开启USB管理器
        stopIoManager();
        startIoManager();
    }

    public void startIoManager() {
        if (mPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(mPort, mListener);
            mExecutor.submit(mSerialIoManager);  //实质是用一个线程不断读取USB端口
        }
    }

    public void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    public void onPause() {
        stopIoManager();
        if (mPort != null) {
            try {
                mPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            mPort = null;
        }
        isOpen = false;
    }
    public void write(String str,boolean isHex)
    {
        if(mPort != null){
            byte[] tx;
            if(isHex) {
                tx = HexDump.hexStringToByteArray(str);
            }
            else {
                tx = str.getBytes();
            }
            try{
                mPort.write(tx,1000);
            }catch (IOException e){

            }
        }
    }
    public byte[] read(){
        if(mPort != null){
            int len;
            final ByteBuffer mReadBuffer = ByteBuffer.allocate(500);
            try{
                len = mPort.read(mReadBuffer.array(), 500);
            }catch (IOException e){
                len = 0;
            }
            if(len > 0){
                byte[] rx = new byte[len];
                mReadBuffer.get(rx,0,len);
                return rx;
            }
        }
        return null;
    }

    public void setHandler(Handler hand)
    {
        mHandler = hand;
    }

}
