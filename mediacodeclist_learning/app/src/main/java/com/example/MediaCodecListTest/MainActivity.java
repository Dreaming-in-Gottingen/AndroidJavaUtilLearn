package com.example.MediaCodecListTest;

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

import android.media.MediaCodecList;
import android.media.MediaCodecInfo;


public class MainActivity extends Activity {
    private static final String TAG = "MediaCodecListTest";
    private Button button0;
    private Button button1;

    private MediaCodecList mCodecList;

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(saveInstanceState);
        setContentView(R.layout.main_activity);

        // init button
        button0 = (Button) findViewById(R.id.button0);
        button0.setText("get MediaCodecList");
        button0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button0.setText("dump in logcat");
                mCodecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
                MediaCodecInfo[] infoList = mCodecList.getCodecInfos();
                int idx;
                for (idx=0; idx<infoList.length; idx++)
                    Log.d(TAG, "["+idx+"] name="+infoList[idx].getName()+", canonicalName="+infoList[idx].getCanonicalName()+", isAlias="+infoList[idx].isAlias()+", isVendor="+infoList[idx].isVendor());
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

    protected void onDestroy() {
        Log.w(TAG, "onDestroy!");
        super.onDestroy();
    }
}
