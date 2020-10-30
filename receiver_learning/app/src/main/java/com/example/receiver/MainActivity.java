package com.example.receiver;

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
    private static final String TAG = "ReceiverTest";
    private Button button1;
    private Button button2;
    private Button button3;
    private Button button4;
    private Button button5; //quit button
    private int add = 0;
    private int sub = 10000;

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(saveInstanceState);
        setContentView(R.layout.main_activity);

        // show string
        button1 = (Button) findViewById(R.id.button1);
        button1.setText("show-string-test");
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button1.setText("show-string-test...");
            }
        });

        // add
        button2 = (Button) findViewById(R.id.button2);
        button2.setText("inc-test");
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button2.setText("inc-test:" + add++);
            }
        });

        // sub
        button3 = (Button) findViewById(R.id.button3);
        button3.setText("dec-test");
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button3.setText("dec-test:" + sub--);
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

    @Override
    public void finish() {
        super.finish();
        Log.w(TAG, "finish Activity!");
    }
}
