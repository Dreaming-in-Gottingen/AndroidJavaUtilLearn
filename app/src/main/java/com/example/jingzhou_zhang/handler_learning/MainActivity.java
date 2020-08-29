package com.example.jingzhou_zhang.handler_learning;

import android.os.Process;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "zjz-MainActivity";
    Handler mainHandler,workHandler;
    HandlerThread mHandlerThread;
    TextView text;
    Button button1,button2,button3,button4;
    int mUpdateCnt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate begin!");

        Log.d(TAG, "1. Process.myPid:" + Process.myPid());
        Log.d(TAG, "2. Process.myTid:" + Process.myTid());
        Log.d(TAG, "3. Process.myUid:" + Process.myUid());

        Log.d(TAG, "4. Thread.currentThread().getId():" + Thread.currentThread().getId());
        Log.d(TAG, "5. getMainLooper().getThread().getId():" + getMainLooper().getThread().getId());
        Log.d(TAG, "6. getTaskId():" + getTaskId());

        Log.d(TAG, "7. AppInfo.uid:" + getApplicationInfo().uid);
        Log.d(TAG, "8. AppInfo.processName:" + getApplicationInfo().processName);

        // 显示文本
        text = (TextView) findViewById(R.id.text1);
        text.setText("author: jingzhou.zhang");

        // 创建与主线程关联的Handler
        mainHandler = new Handler();

        /**
          * 步骤1：创建HandlerThread实例对象
          * 传入参数 = 线程名字，作用 = 标记该线程
          */
        mHandlerThread = new HandlerThread("handlerThread");

        /**
         * 步骤2：启动线程
         */
        mHandlerThread.start();

        /**
         * 步骤3：创建工作线程Handler & 复写handleMessage（）
         * 作用：关联HandlerThread的Looper对象、实现消息处理操作 & 与其他线程进行通信
         * 注：消息处理操作（HandlerMessage（））的执行线程 = mHandlerThread所创建的工作线程中执行
         * 工作线程的消息处理操作(每个线程可以有一个对应的消息处理函数，类似于主函数的maiHandler)
         * 该函数消息处理使用的looper由mHandlerThread提供，而mainHandler使用默认的主线程
         */
        workHandler = new Handler(mHandlerThread.getLooper()){

            @Override
            // 消息处理的操作
            public void handleMessage(Message msg)
            {
                Log.d(TAG, "[work thread] thread_id:" + mHandlerThread.getThreadId());
                Log.d(TAG, "[work thread] handleMessage:" + msg);
                //设置了两种消息处理操作,通过msg来进行识别
                switch(msg.what){
                    // 消息1
                    case 1:
                        try {
                            //延时操作
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // 通过主线程Handler.post方法进行在主线程的UI更新操作
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run () {
                                text.setText("hello! my friend!");
                            }
                        });
                        break;

                    // 消息2
                    case 2:
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run () {
                                text.setText("goodbye! my friend!");
                            }
                        });
                        break;

                    case 3:
                        mainHandler.post(new Runnable() {
                            public void run() {
                                button3.setText("内容已被更新,mUpdateCnt:" + mUpdateCnt++);
                            }
                        });
                        break;

                    case 4:
                        mainHandler.post(new Runnable() {
                            public void run() {
                                Log.d(TAG, "tell main_activity to switch to another activity");
                                runAnotherActivity();
                            }
                        });
                        break;

                    default:
                        break;
                }
            }
        };

        /**
         * 步骤4：使用工作线程Handler向工作线程的消息队列发送消息
         * 在工作线程中，当消息循环时取出对应消息 & 在工作线程执行相关操作
         */
        // 点击Button1
        button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "[main thread] button1 onClick");

                // 通过sendMessage（）发送
                // a. 定义要发送的消息
                Message msg = Message.obtain();
                msg.what = 1; //消息的标识
                msg.obj = "A"; // 消息的存放
                // b. 通过Handler发送消息到其绑定的消息队列
                workHandler.sendMessage(msg);
            }
        });

        // 点击Button2
        button2 = (Button) findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "[main thread] button2 onClick");

                // 通过sendMessage（）发送
                // a. 定义要发送的消息
                Message msg = Message.obtain();
                msg.what = 2; //消息的标识
                msg.obj = "B"; // 消息的存放
                // b. 通过Handler发送消息到其绑定的消息队列
                workHandler.sendMessage(msg);
            }
        });

        // 点击Button3
        button3 = (Button) findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "[main thread] button3 onClick");
                Message msg = Message.obtain();
                msg.what = 3;
                msg.obj = "C";
                workHandler.sendMessage(msg);
            }
        });

        // 点击Button4
        // 作用：退出消息循环
        // 作用：进入下个界面
        button4 = (Button) findViewById(R.id.button4);
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "[main thread] button4 onClick and go to another activity");
                Message msg = Message.obtain();
                msg.what = 4;
                msg.obj = "D";
                workHandler.sendMessage(msg);

                //mHandlerThread.quitSafely();
            }
        });
        Log.d(TAG, "onCreate end!");
    }

    public void runAnotherActivity() {
        // run into another activity
        Log.d(TAG, "ready go into MyHandlerActivity");
        Intent new_activity = new Intent(this, MyHandlerActivity.class);
        startActivity(new_activity);
        finish();
    }
}
