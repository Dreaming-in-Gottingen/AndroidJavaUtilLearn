package com.example.HotplugTest;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.view.View;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import android.os.HandlerThread;
import android.os.Handler;
import android.os.Message;


import android.content.BroadcastReceiver;


public class MainActivity extends Activity {
    private static final String TAG = "HotplugTest";
    private Button button0;
    private Button button1;

    private IntentFilter mMountFilter;
    private BroadcastReceiver mReceiver;

    private AlertDialog.Builder mBuilder;

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(saveInstanceState);
        setContentView(R.layout.main_activity);

        mMountFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);   //监听卡插入事件
        mMountFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);          //监听卡拔出事件
        mMountFilter.addDataScheme("file");

        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context ctx, Intent intent) {
                broadcastReceiverHandler(intent);
            }
        };

        registerReceiver(mReceiver, mMountFilter);

        mBuilder = new AlertDialog.Builder(this);
        mBuilder.setTitle("Warnning!");

        // init button
        button0 = (Button) findViewById(R.id.button0);
        button0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button0.setText("fake init button");
            }
        });

        // quit button
        button1 = (Button) findViewById(R.id.button1);
        button1.setText("quit Activity");
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, "quit Activity!");
                finish();
            }
        });
    }

    private void broadcastReceiverHandler(Intent intent) {
        Log.w(TAG, "broadcastReceiverHandler:"+intent);
        String action = intent.getAction();
        if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
            mBuilder.setMessage("TF card mounted!");
        } else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {
            mBuilder.setMessage("TF card unmounted!");
        } else {
            mBuilder.setMessage("what happend?!!!");
        }
        mBuilder.create().show();
    }

    protected void onDestroy() {
        Log.w(TAG, "onDestroy!");
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        super.onDestroy();
    }
}
