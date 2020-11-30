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

import android.app.AlertDialog;
//import android.content.DialogInterface;


public class MainActivity extends Activity {
    private static final String TAG = "CameraTest";

    Context mContext;

    CameraHelper mCamHelper;
    int mCamDevCnt = 0;
    int mCurCamId = 0;
    int mCurCamState = 0; // 1-on; 0-off
    int mCurRecState = 0; // 1-on; 0-off
    SurfaceView mSurfaceView;

    private Button button00;    //CamDev count
    private Button button01;    //CamId select
    private Button button1;     //start
    private Button button2;     //stop
    private Button button3;     //current camera state
    private Button button4;     //take photo
    private Button button5;     //start/stop record
    private Button button6;     //quit

    // use which camId for preview
    public void selectCameraDeviceId(View v) {
        Log.d(TAG, "select which CamId for preview");
        if (mCamDevCnt == 0) {
            if (mCamHelper == null) {
                mCamHelper = new CameraHelper(mContext, mSurfaceView);
            }
            mCamDevCnt = mCamHelper.getCameraDeviceCount();
            Log.d(TAG, "CamDevCnt=" + mCamDevCnt);
            button00.setText("CamDevCnt="+mCamDevCnt);
        }

        String[] items = new String[mCamDevCnt];
        int idx = 0;
        for (; idx<mCamDevCnt; idx++) {
            items[idx] = "camId" + idx;
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("select camId");
        alert.setSingleChoiceItems(items, mCurCamId, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(TAG, "showSingleAlertDialog : " + i);
                mCurCamId = i;
            }
        });
        alert.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                Log.d(TAG, "current use CamId=" + mCurCamId);
                button01.setText("CurCamId=" + mCurCamId);
            }
        });
        AlertDialog dialog = alert.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.main_activity);
        Log.d(TAG, "onCreate");

        mContext = this;

        mSurfaceView = (SurfaceView) findViewById(R.id.surview);

        // get camera device count
        button00 = (Button) findViewById(R.id.button00);
        button00.setText("camera cnt...");
        button00.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamHelper == null) {
                    mCamHelper = new CameraHelper(mContext, mSurfaceView);
                }
                mCamDevCnt = mCamHelper.getCameraDeviceCount();
                Log.d(TAG, "CamDevCnt=" + mCamDevCnt);
                button00.setText("CamDevCnt="+mCamDevCnt);
            }
        });

        button01 = (Button) findViewById(R.id.button01);
        button01.setText("CurCamId...");

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
                    mCamHelper.openCamera(mCurCamId);
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

        // start/stop record
        button5 = (Button) findViewById(R.id.button5);
        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurCamState == 1) {
                    if (mCurRecState==0 && mCamHelper!=null) {
                        mCamHelper.startRecorder();
                        mCurRecState = 1;
                        button5.setText("stop record");
                    } else if (mCurRecState==1 && mCamHelper!=null) {
                        mCamHelper.stopRecorder();
                        mCurRecState = 0;
                        button5.setText("start record");
                    }
                } else {
                    Log.e(TAG, "can not control recorder when CameraDevice is not working!!!");
                }
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
