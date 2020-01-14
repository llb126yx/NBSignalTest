package com.lb.nbtest;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.util.HashMap;

public class NBModule {
    public static final int MSG_NB_NET_OK = 0;
    public static final int MSG_NB_NET_NO = 1;
    public static final int MSG_NB_NET_ING = 2;
    public Bundle mSignalData = new Bundle();
    private UsbSerialControl mPort;
    private Handler mHandler; //infrom main therd to updata ui.
    private Thread mThread;
    public NBModule(UsbSerialControl port)
    {
        mPort = port;
    }
    public void setHandler(Handler hand){
        mHandler = hand;
    }
    public void checkNet()
    {
        mPort.stopIoManager();
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int loopNum = 0;
                while(true) {
                    mPort.write("AT+CGATT?\r\n",false);
                    byte[] rxbyte = mPort.read();
                    if(rxbyte != null){
                        String rxstr = new String(rxbyte);
                        int index = rxstr.indexOf(":1");
                        if(index > 0){
                            if(mHandler!=null){
                                mHandler.sendEmptyMessage(MSG_NB_NET_OK);
                                break;
                            }
                        }
                    }
                    loopNum++;
                    if(loopNum > 5){
                        mHandler.sendEmptyMessage(MSG_NB_NET_NO);
                        break;
                    }
                    if(mHandler!=null) {
                        mHandler.sendEmptyMessage(MSG_NB_NET_ING);
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mThread.start();
    }
    private Float toFloat(String src)
    {
        if(src == null){
            return null;
        }
        int sign = 0;
        int val = 1;
        boolean findNum = false;
        int divider = 0;
        for(int i = 0; i<src.length();i++){
            char ch = src.charAt(i);
            if(ch>='0' && ch <= '9'){
                findNum = true;
                if(sign == 0){
                    if(i > 0) {
                        ch = src.charAt(i - 1);
                        if (ch == '-') {
                            sign = -1;
                        } else {
                            sign = 1;
                        }
                    }else {
                        sign = 1;
                    }
                }
                int tmp = ch - '0';
                val *= tmp;
                if(divider > 0){
                    divider *= 10;
                }
            }else if(findNum){
                if(ch == '.' && divider == 0) {
                    divider = 1;
                }else{
                    break;
                }
            }
        }
        if(!findNum){
            return null;
        }
        if(divider == 0){
            divider = 1;
        }
        return  new Float((float) val/divider);
    }
    private boolean parseSignal(String str)
    {
        if(str == null){
            return false;
        }
        int index = -1;
        String pattern;
        String strdig;
        Float fval;
        pattern = "Signal power:";
        index = str.indexOf(pattern);
        if(index > -1){
            index += pattern.length();
            strdig = str.substring(index);
            fval = toFloat(strdig);
            if(fval != null){

            }
        }
        return true;
    }
    public void getSignal()
    {
        mPort.stopIoManager();
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int loopNum = 0;
                while(true) {
                    mPort.write("AT+NUESTATS\r\n",false);
                    byte[] rxbyte = mPort.read();
                    if(rxbyte != null){
                        String rxstr = new String(rxbyte);
                        if(parseSignal(rxstr))
                        {
                            if(mHandler!=null){
                                mHandler.sendEmptyMessage(MSG_NB_NET_OK);
                                break;
                            }
                        }
                    }
                    loopNum++;
                    if(loopNum > 5){
                        mHandler.sendEmptyMessage(MSG_NB_NET_NO);
                        break;
                    }
                    if(mHandler!=null) {
                        mHandler.sendEmptyMessage(MSG_NB_NET_ING);
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mThread.start();
    }
}
