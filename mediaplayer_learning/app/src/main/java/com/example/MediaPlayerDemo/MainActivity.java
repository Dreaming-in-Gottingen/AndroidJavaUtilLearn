package com.example.MediaPlayerDemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ProgressBar;

import android.os.HandlerThread;
import android.os.Handler;
import android.os.Message;

import android.media.MediaPlayer;

import android.os.Environment;
import android.os.Build;

import android.database.Cursor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.content.ContentUris;

import android.net.Uri;

import android.view.Surface;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

public class MainActivity extends Activity
    implements MediaPlayer.OnCompletionListener, 
               MediaPlayer.OnSeekCompleteListener,
               MediaPlayer.OnPreparedListener,
               MediaPlayer.OnInfoListener,
               MediaPlayer.OnErrorListener
{
    private static final String TAG = "MediaPlayerDemo";
    private Button mButton0; // reset button, select which video-file to play
    private Button mButton1; // play
    private Button mButton2; // pause
    private Button mButton3; // stop
    private Button mButton4; // reset
    private Button mButton5; // exit

    private String mVideoPath;

    private MediaPlayer mMediaPlayer;

    private Handler mMainHandler;   // main handler update progress bar

    private final int MSG_UPDATE_PB = 0;

    private Surface mPlaySurface = null;
    private SurfaceHolder mSurfaceHolder = null;

    private ProgressBar mBar;
    private int mDurationMs;

    private int mCurrentState = 0; // 0-unintialized; 1-prepared; 2-playing;


///////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "result data: " + data);
            Uri uri = data.getData();
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                String path = uri.getPath();
            }
            Log.d(TAG, "Scheme:" + uri.getScheme());
            Log.d(TAG, "Path:" + uri.getPath());
            Log.d(TAG, "Authority:" + uri.getAuthority());

            String path;
            //get real absolute path
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {//4.4以后
                mVideoPath = getPath(this, uri);
            } else {//4.4以下下系统调用方法
                mVideoPath = getRealPathFromURI(uri);
            }
            Log.d(TAG, "after parse uri, video absolute path=" + mVideoPath);
        }
    }

    public String getRealPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if(null!=cursor&&cursor.moveToFirst()){;
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
            cursor.close();
        }
        return res;
    }

    public String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

     /**
      * Get the value of the data column for this Uri. This is useful for
      * MediaStore Uris, and other file-based ContentProviders.
      *
      * @param context       The context.
      * @param uri           The Uri to query.
      * @param selection     (Optional) Filter used in the query.
      * @param selectionArgs (Optional) Selection arguments used in the query.
      * @return The value of the _data column, which is typically a file path.
      */
    public String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
///////////////////////////////////////////////////////////////////////////////////


    private SurfaceHolder.Callback mHolderCB = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated! this=" + this + ", holder=" + holder);
        }
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "surfaceChanged! this=" + this + ", holder=" + holder);
            Log.d(TAG, "format=["+format+"], width/height=["+width+"/"+height+"]");
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed! this=" + this + ", holder=" + holder);
        }
    };

    private void initPlayer() {
        try {
            mMediaPlayer.setDataSource(mVideoPath);
            Log.d(TAG, "ready to play file=" + mVideoPath);
            mMediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //mMediaPlayer.setSurface(mPlaySurface);  // will screen off
        mMediaPlayer.setDisplay(mSurfaceHolder);
        mMediaPlayer.setScreenOnWhilePlaying(true);
        mMediaPlayer.setOnPreparedListener(MainActivity.this);
        mMediaPlayer.setOnSeekCompleteListener(MainActivity.this);
        mMediaPlayer.setOnCompletionListener(MainActivity.this);
        mMediaPlayer.setOnInfoListener(MainActivity.this);
        mMediaPlayer.setOnErrorListener(MainActivity.this);
        mDurationMs = mMediaPlayer.getDuration();
        Log.d(TAG, "duration=" + mDurationMs/1000 + "s, w/h=(" + mMediaPlayer.getVideoWidth() + "," + mMediaPlayer.getVideoHeight() + ")");
    }

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        Log.d(TAG, "onCreate");
        Log.d(TAG, "sdk_version=" + Build.VERSION.SDK_INT);

        super.onCreate(saveInstanceState);
        setContentView(R.layout.main_activity);

        SurfaceView sv = (SurfaceView) findViewById(R.id.surview);
        mSurfaceHolder = sv.getHolder();
        mSurfaceHolder.setFixedSize(1280, 720);
        mSurfaceHolder.addCallback(mHolderCB);
        mPlaySurface = mSurfaceHolder.getSurface();

        mBar = findViewById(R.id.bar);

        mMediaPlayer = new MediaPlayer();

        mButton0 = (Button) findViewById(R.id.button0);
        mButton0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                //intent.setType("image/*");        //选择图片
                //intent.setType("audio/*");        //选择音频
                intent.setType("video/*");        //选择视频
                //intent.setType("video/*; image/*"); //同时选择视频和图片
                //intent.setType("*/*");              //无类型限制
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 1);
            }
        });

        mButton1 = (Button) findViewById(R.id.button1);
        mButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVideoPath == null) {
                    Toast.makeText(MainActivity.this, "select video file firstly!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (mCurrentState == 0) {
                    initPlayer();
                    mMediaPlayer.start();
                    mCurrentState = 2;
                    mMainHandler.sendEmptyMessageDelayed(MSG_UPDATE_PB, 1000);
                } else if (mCurrentState == 1) {
                    mMediaPlayer.start();
                    mCurrentState = 2;
                } else if (mCurrentState == 2) {
                    Log.w(TAG, "already in playing!");
                }
            }
        });

        mButton2 = (Button) findViewById(R.id.button2);
        mButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, "pause player!");
                mMediaPlayer.pause();
                mCurrentState = 1;
            }
        });

        mButton3 = (Button) findViewById(R.id.button3);
        mButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, "stop player!");
                mMediaPlayer.stop();
                mCurrentState = 1;
            }
        });

        mButton4 = (Button) findViewById(R.id.button4);
        mButton4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, "reset player!");
                mMediaPlayer.reset();
                initPlayer();
                mCurrentState = 1;
            }
        });

        mButton5 = (Button) findViewById(R.id.button5);
        mButton5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, "quit Activity!");
                mMediaPlayer.stop();
                mMediaPlayer.release();
                finish();
            }
        });

        mMainHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch(msg.what) {
                    case MSG_UPDATE_PB:
                        //Log.d(TAG, "update ProgressBar");
                        if (mCurrentState == 2) {
                            int cur_postion = mMediaPlayer.getCurrentPosition();
                            int percentage = 100 * cur_postion / mDurationMs;
                            mBar.setProgress(percentage);
                            mMainHandler.sendEmptyMessageDelayed(MSG_UPDATE_PB, 1000);
                        }
                        break;
                    default:
                        break;
                } 
            }
        };
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "play prepared!");
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.d(TAG, "seek completed!");
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "play completed!");
        mCurrentState = 0;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "onInfo! what=" + what);
        return false;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "onError! what=" + what);
        return false;
    }

    protected void onDestroy() {
        Log.w(TAG, "onDestroy!");
        super.onDestroy();
    }
}
