package com.example.control_learning;

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

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

public class MainActivity extends Activity {
    private static final String TAG = "ControlTest";
    private int mBtn1ClickCnt = 0;
    private int mImgBtn1ClickCnt = 0;
    private TextView tv;
    private Button button1;
    private ImageButton img_button;
    private Button button3;
    private Button button4;
    private Button button5;
    private Button button6;
    private Button button7; //quit button

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(saveInstanceState);
        setContentView(R.layout.main_activity);

        //tv = (TextView) findViewById(R.id.textview);
        //tv.setGravity(Gravity.CENTER);
        //tv.setGravity(Gravity.CENTER_VERTICAL);

        button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "button1 click_cnt = " + mBtn1ClickCnt);
                button1.setText("button1 click_cnt=" + mBtn1ClickCnt);
                mBtn1ClickCnt++;
            }
        });

        img_button = (ImageButton) findViewById(R.id.image_button);
        img_button.getBackground().setAlpha(0);
        img_button.setImageResource(R.mipmap.lock_off);
        img_button.setScaleType(ImageView.ScaleType.FIT_XY);
        img_button.setVisibility(View.VISIBLE);
        img_button.setOnClickListener (new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "img_button click_cnt=" + mImgBtn1ClickCnt++);
                if (mImgBtn1ClickCnt%2 == 0) {
                    Log.d(TAG, "show gray disp for lock_off");
                    ColorMatrix cm = new ColorMatrix();
                    cm.setSaturation(0); // 设置饱和度
                    ColorMatrixColorFilter grayColorFilter = new ColorMatrixColorFilter(cm);
                    img_button.setColorFilter(grayColorFilter); // 黑白显示
                    img_button.setImageResource(R.mipmap.lock_off);
                } else {
                    Log.d(TAG, "show color disp for lock_off");
                    img_button.setColorFilter(null); // 如果想恢复彩色显示，设置为null即可
                    img_button.setImageResource(R.mipmap.lock_off);
                }
                //if (mImgBtn1ClickCnt%2 == 0)
                //    img_button.setImageResource(R.mipmap.lock_on);
                //else
                //    img_button.setImageResource(R.mipmap.lock_off);
            }
        });

        // disable button
        button3 = (Button) findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "button3 clicked! disable button1 & img_button!");
                button1.setEnabled(false);
                img_button.setEnabled(false);
            }
        });

        // enable button
        button4 = (Button) findViewById(R.id.button4);
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "button3 clicked! enable button1 & img_button!");
                button1.setEnabled(true);
                img_button.setEnabled(true);
            }
        });

        // start Service
        button5 = (Button) findViewById(R.id.button5);
        button5.setText("start Service");
        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "start Service!");
                //Intent intent = new Intent(MainActivity.this, ServiceDemo.class);
                //startService(intent);
                Intent intent = new Intent(getBaseContext(), MyService.class);
                startService(intent);
                //Intent intent = new Intent(MainActivity.this, MainActivity.class);
                //startActivity(intent);
                //finish();
            }
        });

        // stop Service
        button6 = (Button) findViewById(R.id.button6);
        button6.setText("stop Service");
        button6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "stop Service!");
                //Intent intent = new Intent(getBaseContext(), ServiceDemo.class);
                //stopService(intent);
                Intent intent = new Intent(getBaseContext(), MyService.class);
                stopService(intent);
                //finish();
            }
        });

        // quit Activity
        button7 = (Button) findViewById(R.id.button7);
        button7.setText("quit Activity");
        button7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "quit Activity!");
                finish();
            }
        });
    }

    public void showWarningInfo(View view) {
        Log.d(TAG, "showWarningInfo");
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Warning!");
        alert.setMessage("学习AlertDialog的使用");
        alert.setPositiveButton("确定", new DialogInterface.OnClickListener () {
            @Override
            public void onClick (DialogInterface dialog, int i) {
                Log.d(TAG, "ImageButton-PositiveButton onClick! i=" + i);
            }
        });
        alert.create().show();
    }
}
