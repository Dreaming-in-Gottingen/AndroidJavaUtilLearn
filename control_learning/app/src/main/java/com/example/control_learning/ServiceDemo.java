package com.example.control_learning;

import android.os.IBinder;

import android.app.Service;
import android.content.Intent;

import android.util.Log;


public class ServiceDemo extends Service {
    private final static String TAG = "ServiceDemo";

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int id) {
        Log.d(TAG, "onStartCommand, id=" + id);
        return super.onStartCommand(intent, flags, id);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }
}
