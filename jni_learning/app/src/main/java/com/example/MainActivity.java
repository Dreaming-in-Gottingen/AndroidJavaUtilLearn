package com.example;

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

public class MainActivity extends Activity {
    private static final String TAG = "JniTest";
    private Button button1;
    private Button button2;
    private Button button3;
    private Button button4;
    private Button button5; //quit button

    static {
        Log.d(TAG, "load jni lib...");
        System.loadLibrary("hellojni");
    }

    public native String stringFromJNI();
    public native int addFromJNI(int a, int b);
    public native int subFromJNI(int a, int b);

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(saveInstanceState);
        setContentView(R.layout.main_activity);

        // jni getString
        button1 = (Button) findViewById(R.id.button1);
        button1.setText("jni-getString-test");
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = stringFromJNI();
                Log.i(TAG, "get string: [" + str + "]"); 
                button1.setText("jni-getString-test: [" + str + "]");
            }
        });

        // jni add
        button2 = (Button) findViewById(R.id.button2);
        button2.setText("jni-add-test");
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int ret = addFromJNI(123, 456);
                Log.i(TAG, "jni add result: [" + ret + "]"); 
                button2.setText("jniAdd:" + ret); 
            }
        });

        // jni sub
        button3 = (Button) findViewById(R.id.button3);
        button3.setText("jni-sub-test");
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int ret = subFromJNI(456, 123);
                Log.i(TAG, "jni sub result: [" + ret + "]"); 
                button3.setText("jniSub:" + ret);
            }
        });

        // quit Activity
        button4 = (Button) findViewById(R.id.button4);
        button4.setText("quit Activity");
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, "quit Activity!");
                finish();
            }
        });
    }
}
