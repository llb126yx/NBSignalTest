package com.lb.nbtest.ui.home;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.lb.nbtest.MainActivity;
import com.lb.nbtest.NBModule;
import com.lb.nbtest.R;
import com.lb.nbtest.UsbSerialControl;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private ImageButton btnTest;
    private UsbSerialControl mUsbSerialPort;
    private NBModule mNBModule;
    private TextView textLog;
    private TextView textRes;

    private final Handler mHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            //super.handleMessage(msg);
            switch (msg.what)
            {
                case NBModule.MSG_NB_NET_OK:
                    textRes.setText("已入网...");
                    break;
                case NBModule.MSG_NB_NET_ING:
                    textRes.setText("正在入网...");
                    break;
                case NBModule.MSG_NB_NET_NO:
                    textRes.setText("未入网");
                    break;
                default:
                    textRes.setText("");
                    break;
            }
        }
    };

    @SuppressLint("HandlerLeak")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
//        final TextView textView = root.findViewById(R.id.text_home);
//        homeViewModel.getText().observe(this, new Observer<String>() {
//            @Override
//            public void onChanged(@Nullable String s) {
//                textView.setText(s);
//            }
//        });
        textLog = root.findViewById(R.id.textLog);
        textRes = root.findViewById(R.id.textRes);
        mUsbSerialPort = ((MainActivity)getActivity()).getUsbPort();
        mNBModule = new NBModule(mUsbSerialPort);
        mNBModule.setHandler(mHandler);

        btnTest = root.findViewById(R.id.btnTest);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mUsbSerialPort.isOpen) {
                    mNBModule.checkNet();
                }else{
                    Toast.makeText(getContext(),"请插入设备！",Toast.LENGTH_SHORT).show();
                    mUsbSerialPort.openPort();
                }
            }
        });

        return root;
    }
}