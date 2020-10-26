package com.example.control_learning;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import android.util.Log;

public class MyService extends Service {
    private final static String TAG = "MyService";

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        Log.d(TAG, "onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand! flags=" + flags + ", startId=" + startId);
        // Let it continue running until it is stopped.
        Toast.makeText(this, "服务已经启动", Toast.LENGTH_LONG).show();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        Toast.makeText(this, "服务已经停止", Toast.LENGTH_LONG).show();
    }
}
