package com.example.jingzhou_zhang.handler_learning;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.util.Log;

public class MyHandlerActivity extends AppCompatActivity {

    private static final String TAG = "zjz-MyHandlerActivity";
    public TextView mTextView;
    public Button mButton;
    public Handler mHandler;

    // 步骤1：（自定义）新创建Handler子类(继承Handler类) & 复写handleMessage（）方法
    class Mhandler extends Handler {

        // 通过复写handlerMessage() 从而确定更新UI的操作
        @Override
        public void handleMessage(Message msg) {
            // 根据不同线程发送过来的消息，执行不同的UI操作
            switch (msg.what) {
                case 1:
                    mTextView.setText("执行了线程1的UI操作");
                    break;
                case 2:
                    mTextView.setText("执行了线程2的UI操作");
                    break;
            }
        }
    }

    protected void goBackMainActivity() {
        Intent new_activity = new Intent(this, MainActivity.class);
        startActivity(new_activity);
        finish();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myhandler);

        Log.d(TAG, "onCreate begin!");

        mTextView = (TextView) findViewById(R.id.show);
        mTextView.setText("MyHandlerActivity demo"); 

        mButton = (Button) findViewById(R.id.goback);
        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "onClick, goback to MainActivity!");
                goBackMainActivity();
            }
        });

        // 步骤2：创建Handler实例
        mHandler = new Mhandler();

        Log.d(TAG, "new Thread1");
        // 步骤3：在工作线程中 发送消息到消息队列中
        new Thread() {
            @Override
            public void run() {
                //try {
                //    Thread.sleep(5000);
                //} catch (InterruptedException e) {
                //    e.printStackTrace();
                //}

                // 通过sendMessage（）发送
                // a. 定义要发送的消息
                Message msg = Message.obtain();
                msg.what = 1; //消息的标识
                msg.obj = "A"; // 消息的存放
                // b. 通过Handler发送消息到其绑定的消息队列
                //mHandler.sendMessage(msg);
                mHandler.sendMessageDelayed(msg, 5000);
            }
        }.start();
        // 步骤3：开启工作线程（同时启动了Handler）

        Log.d(TAG, "new Thread2");
        // 此处用2个工作线程展示
        new Thread() {
            @Override
            public void run() {
                //try {
                //    Thread.sleep(10000);
                //} catch (InterruptedException e) {
                //    e.printStackTrace();
                //}

                // 通过sendMessage（）发送
                // a. 定义要发送的消息
                Message msg = Message.obtain();
                msg.what = 2; //消息的标识
                msg.obj = "B"; // 消息的存放
                // b. 通过Handler发送消息到其绑定的消息队列
                //mHandler.sendMessage(msg);
                mHandler.sendMessageDelayed(msg, 10000);
            }
        }.start();
        Log.d(TAG, "onCreate end!");
    }
}
