package com.example.LockTest;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import android.view.View;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import android.os.HandlerThread;
import android.os.Handler;
import android.os.Message;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;


public class MainActivity extends Activity {
    private static final String TAG = "LockTest";
    private Button button1;
    private Button button2;
    private Button button3;
    private Button button4;
    private Button button5; //quit button

    private final int MSG_INIT_LISTENER = 0;
    private final int MSG_TRIGER_TEST = 1;

    private Handler mWorkHandler;
    private HandlerThread mWorkThread;

    private ReentrantLock mLock;
    private Condition mCond;

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(saveInstanceState);
        setContentView(R.layout.main_activity);

        mLock = new ReentrantLock();
        mCond = mLock.newCondition();

        mWorkThread = new HandlerThread("work_thread");
        mWorkThread.start();
        mWorkHandler = new Handler(mWorkThread.getLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                   case MSG_INIT_LISTENER:
                        Log.d(TAG, "fake message!");
                        break;
                   case MSG_TRIGER_TEST:
                        Log.i(TAG, "wait for signal from mainhandler...");

                        mLock.lock();
                        try {
                            try {
                                Log.i(TAG, "waited begin!");
                                mCond.await();
                                Log.i(TAG, "waited end!");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } finally {
                            mLock.unlock();
                        }

                        break;
                }
            }
        };

        // init button
        button1 = (Button) findViewById(R.id.button1);
        button1.setText("fake init button");
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button1.setText("click wait-signal button");
                Message msg = Message.obtain();
                msg.what = MSG_INIT_LISTENER;
                mWorkHandler.sendMessage(msg);
            }
        });

        // wait button
        button2 = (Button) findViewById(R.id.button2);
        button2.setText("wait button");
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message msg = Message.obtain();
                msg.what = MSG_TRIGER_TEST;
                mWorkHandler.sendMessage(msg);
                button2.setText("waiting signal...");
                button3.setText("click here!");
            }
        });

        // signal button
        button3 = (Button) findViewById(R.id.button3);
        button3.setText("signal button");
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "signal!");
                mLock.lock();
                try {
                    mCond.signal();
                    button2.setText("finish block!");
                    button3.setText("have signaled!");
                } finally {
                    mLock.unlock();
                }
            }
        });

        // quit Activity
        button4 = (Button) findViewById(R.id.button4);
        button4.setText("quit Activity");
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, "workThread quit and quit Activity!");
                mWorkThread.quit();
                finish();
            }
        });
    }

    protected void onDestroy() {
        Log.w(TAG, "onDestroy!");
        super.onDestroy();
    }
}
