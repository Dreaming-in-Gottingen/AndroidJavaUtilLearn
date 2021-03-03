package com.example.service_server;

import android.app.Service;
import android.content.Intent;

import android.util.Log;

import android.os.IBinder;
import android.os.RemoteException;

import android.support.annotation.Nullable;

public class MyService extends Service {
    private static final String TAG = "MyService";

    private int mValue = 0;

    // 实例化AIDL的Stub类(Binder的子类)
    AIDL_Service.Stub mBinder = new AIDL_Service.Stub() {
        //重写接口里定义的方法
        @Override
        public void aidl_service() throws RemoteException {
            Log.d(TAG, "success to communication with remote server from client!");
        }

        @Override
        public void aidl_setter(int val) throws RemoteException {
            mValue = val;
            Log.d(TAG, "set mValue to [" + val + "] by client!");
        }

        @Override
        public int aidl_getter() throws RemoteException {
            Log.d(TAG, "get mValue from client!");
            return mValue;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate!");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onCommand!");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy!");
    }

    //在onBind()返回Stub类实例
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind!");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind!");
        return super.onUnbind(intent);
    }
}
