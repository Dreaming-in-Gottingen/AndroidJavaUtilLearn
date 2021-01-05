package com.example.IntentTest;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import android.content.Intent;

import android.view.View;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import android.net.Uri;
import java.io.File;


public class MainActivity extends Activity {
    private static final String TAG = "IntentTest";

    private Context mContext;

    private Button button0; //photo intent
    private Button button1; //record intent
    private Button button2; //quit button

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;

    private int mPicCnt = 0;
    private int mVideoCnt = 0;

    private String mPicDir = "/mnt/sdcard/";
    private String mVideoDir = "/mnt/sdcard/";

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(saveInstanceState);
        setContentView(R.layout.main_activity);
        mContext = this;

        // photo intent button
        button0 = (Button) findViewById(R.id.button0);
        button0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick begin");
                //button0.setText("photo intent...");
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File file = new File(mPicDir);
                if (!file.exists()) {
                    Log.w(TAG, "mPicDir(" + mPicDir + ") not exist!");
                    file.mkdirs();
                } else {
                    Log.v(TAG, "mPicDir(" + mPicDir + ") exist!");
                }
                String picPath = mPicDir + "intent_photo" + mPicCnt++ + ".jpg";
                intent.putExtra(MediaStore.EXTRA_OUTPUT, picPath);
                //intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(picPath)));

                //File camPhoto = new File(picPath);
                //Uri photoUri = FileProvider.getUriForFile(
                //                    mContext,
                //                    picPath + ".fileprovider",
                //                    camPhoto);
                //intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

                //Intent intent = new Intent(Intent.ACTION_DIAL);
                //intent.setData("10086");

                startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
                Log.d(TAG, "onClick end");
            }
        });

        // record intent button
        button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick begin");
                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                File file = new File(mVideoDir);
                if (!file.exists()) {
                    Log.w(TAG, "mVideoDir(" + mVideoDir + ") not exist!");
                    file.mkdirs();
                } else {
                    Log.v(TAG, "mVideoDir(" + mVideoDir + ") exist!");
                }
                String vidPath = mVideoDir + "intent_video" + mVideoCnt++ + ".mp4";
                intent.putExtra(MediaStore.EXTRA_OUTPUT, vidPath);
                //intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(vidPath)));

                startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
                Log.d(TAG, "onClick end");
            }
        });

        // quit Activity
        button2 = (Button) findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, "quit Activity!");
                finish();
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "Image save to: " + data.getData());
                } else if (resultCode == RESULT_CANCELED) {
                    Log.w(TAG, "usr cancel!");
                } else {
                    Log.e(TAG, "unknown resultCode:" + resultCode);
                }
                break;
            case CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "Video save to: " + data.getData());
                } else if (resultCode == RESULT_CANCELED) {
                    Log.w(TAG, "usr cancel!");
                } else {
                    Log.e(TAG, "unknown resultCode:" + resultCode);
                }
                break;
            default:
                Log.e(TAG, "unknown requestCode:" + requestCode);
                break;
        }
    }

    protected void onDestroy() {
        Log.w(TAG, "onDestroy!");
        super.onDestroy();
    }
}
