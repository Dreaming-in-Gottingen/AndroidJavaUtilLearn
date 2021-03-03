package com.example.service_client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import android.support.v7.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;

import android.util.Log;

import com.example.service_server.AIDL_Service;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "ServiceClientDemo";

    private Button bindServiceButton;
    private boolean mbBindState = false;

    // define aidl var
    private AIDL_Service mAIDL_Service;

    // create anonymous class of ServiceConnection
    private ServiceConnection mConnection = new ServiceConnection() {

        //重写onServiceConnected()方法和onServiceDisconnected()方法
        //在Activity与Service建立关联和解除关联的时候调用
        @Override
        public void onServiceDisconnected(ComponentName name) {
            //只有在远程服务崩溃时，才会被调用到，正常情况下不会调用
            //例如kill -9 service_pid
            Log.d(TAG, "onServiceDisconnected for " + name);
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected for " + name);
            //使用AIDLService1.Stub.asInterface()方法将传入的IBinder对象传换成了mAIDL_Service对象
            mAIDL_Service = AIDL_Service.Stub.asInterface(service);
            mbBindState = true;
        }
    };

    @Override
    protected void onCreate(Bundle savedState) {
        Log.d(TAG, "onCreate begin!");    
        super.onCreate(savedState);
        setContentView(R.layout.main_activity);

        bindServiceButton = (Button) findViewById(R.id.bind_service);
        bindServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mbBindState == false) {
                    Log.d(TAG, "ready to bindService!");    

                    //通过Intent指定服务端的服务名称和所在包，与远程Service进行绑定
                    //参数与服务器端的action要一致，即"服务器包名.aidl接口文件名"
                    Intent intent = new Intent("com.example.service_server.AIDL_Service");

                    //Android5.0后无法只通过隐式Intent绑定远程Service
                    //需要通过setPackage()方法指定包名
                    intent.setPackage("com.example.service_server");

                    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                    bindServiceButton.setText("unbind service");
                } else {
                    Log.d(TAG, "ready to unbindService!");    
                    unbindService(mConnection);
                    mbBindState = false;
                    bindServiceButton.setText("bind service");
                }
            }
        });

        Button btn1 = (Button) findViewById(R.id.button1);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    //通过该对象调用在MyAIDLService.aidl文件中定义的接口方法,从而实现跨进程通信
                    //mAIDL_Service.aidl_service();
                    mAIDL_Service.aidl_setter(123456);
                    btn1.setText("setter:123456");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        Button btn2 = (Button) findViewById(R.id.button2);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int val = mAIDL_Service.aidl_getter();
                    btn2.setText("getter:" + val);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        Log.d(TAG, "onCreate begin!");    
    }
}
