package com.example.cameratest;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import android.app.AlertDialog;
//import android.support.v4.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;

import android.view.View;
import android.view.TextureView;
import android.view.SurfaceView;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import android.os.EnvironmentEx;
import android.os.Environment;


public class MainActivity extends Activity {
    private static final String TAG = "CameraTest";

    Context mContext;

    CameraHelper mCamHelper;
    int mCurCamState = 0; // 1-on; 0-off
    SurfaceView mSurfaceView;

    private Button button1;     //start
    private Button button2;     //stop
    private Button button3;     //current camera state
    private Button button4;     //take photo
    private Button button6;     //quit

    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.main_activity);
        Log.d(TAG, "onCreate");

        mContext = this;

        mSurfaceView = (SurfaceView) findViewById(R.id.surview);

        // start preview
        button1 = (Button) findViewById(R.id.button1);
        button1.setText("start-preview");
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurCamState == 0) {
                    if (mCamHelper == null) {
                        Log.d(TAG, "CameraHelper ctor! start preview!");
                        mCamHelper = new CameraHelper(mContext, mSurfaceView);
                    }
                    mCamHelper.openCamera();
                    mCurCamState = 1;
                    button3.setText("state:" + mCurCamState);
                }
            }
        });

        // stop preview
        button2 = (Button) findViewById(R.id.button2);
        button2.setText("stop-preview");
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurCamState == 1) {
                    Log.d(TAG, "stop camera device!");
                    mCamHelper.closeCameraDevice();
                    mCurCamState = 0;
                    button3.setText("state:" + mCurCamState);
                }
            }
        });

        // camera state
        button3 = (Button) findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String state = EnvironmentEx.getExternalStoragePathState();
                boolean mount = Environment.MEDIA_MOUNTED.equals(state);
                Log.d(TAG, "state=" + state + ", mount=" + mount);
            }
        });

        // take photo
        button4 = (Button) findViewById(R.id.button4);
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamHelper != null)
                    mCamHelper.takePhoto();
            }
        });

        // quit Activity
        button6 = (Button) findViewById(R.id.button6);
        button6.setText("quit Activity");
        button6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, "quit Activity!");
                if (mCurCamState == 1) {
                    Log.d(TAG, "stop camera device!");
                    mCamHelper.closeCameraDevice();
                    mCurCamState = 0;
                }
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
