package com.example.MediaCodecDemo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.widget.Button;

import android.database.Cursor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.widget.Toast;

import android.os.HandlerThread;
import android.os.Handler;
import android.os.Message;

import android.media.MediaCodecInfo;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import android.os.EnvironmentEx;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.nio.ByteBuffer;

import java.util.Arrays;

/* just test avc encoder */
public class MainActivity extends Activity {
    private static final String TAG = "MediaCodecDemo";
    private Button button0; // state: "prepare"/"start"/"finished"
    private Button button1; // "quit Activity"
    private Button button2; // show encoded frame counter
    private Button button3; // select yuv file
    private EditText etWidth;
    private EditText etHeight;

    private int mCurState = 0; // 0-uninitialized; 1-prepared; 2-running; 3-finished
    private int mEncFrmCnt = 0;

    private Handler mMainHandler;

    private String mYuvPath;    // input yuv path
    private String mBsPath;     // output bitstream path

    // feed yuv to MC's inport-buffer in this loop
    private HandlerThread mCodecThread; // queue MC input-buffer.
    private Handler mCodecHandler;

    private HandlerThread mMuxerThread; // dequeue MC output-buffer, and muxer.
    private Handler mMuxerHandler;

    InputStream mYuvStream;
    OutputStream mAvcStream;

    // for statatistic encoding speed
    private int mTotalFrameCnt;
    private int mEncodingFps;
    private int mEncodingDuraMilliSec;
    private long mStartTime;
    private long mStopTime;

    private int mWidth = 1280;
    private int mHeight = 720;
    private final String kMimeTypeAvc = "video/avc";

    private ByteBuffer[] mInputBuffers;
    private ByteBuffer[] mOutputBuffers;
    private byte[] mByteArray;
    private int mPicSz;

    private MediaFormat mMediaFormat;
    private MediaCodec mMediaCodec;
    private MediaMuxer mMuxer;

    private final int MSG_CODEC_START = 0;
    private final int MSG_CODEC_QUEUE_BUF  = 1;
    private final int MSG_CODEC_STOP = 2;

    private final int MSG_MUXER_START = 0;
    private final int MSG_MUXER_READ_SPSPPS = 1;
    private final int MSG_MUXER_LOOP_READ_BS = 2;
    private final int MSG_MUXER_STOP_READ_BS = 3;

    private void prepare() {

        // check input yuv dimension and path
        String width = etWidth.getText().toString();
        String height = etHeight.getText().toString();
        if (width.equals("") || height.equals("")) {
            Toast.makeText(MainActivity.this, "must specify width & height before start!", Toast.LENGTH_LONG).show();
            return;
        } else {
            Toast.makeText(MainActivity.this, "yuv dimension: " + width + " x " + height, Toast.LENGTH_LONG).show();
            mWidth = Integer.parseInt(width);
            mHeight = Integer.parseInt(height);
            Log.d(TAG, "yuv size = " + mWidth + " x " + mHeight);
        }

        if (mYuvPath==null || mYuvPath.equals("")) {
            Toast.makeText(MainActivity.this, "must specify input yuv file!", Toast.LENGTH_LONG).show();
            return;
        } else {
            Log.d(TAG, "yuv path = " + mYuvPath);
            Toast.makeText(MainActivity.this, "yuv path: " + mYuvPath, Toast.LENGTH_LONG).show();
        }

        // prepare Codec handler
        mCodecThread = new HandlerThread("CodecThread");
        mCodecThread.start();
        mCodecHandler = new Handler(mCodecThread.getLooper()) {
            // read yuv from file and send to MediaCodec's inport.
            public void handleMessage(Message msg) {
                switch(msg.what) {
                    case MSG_CODEC_START:
                        Log.d(TAG, "MSG_CODEC_START begin");
                        mMediaCodec.start();
                        mInputBuffers = mMediaCodec.getInputBuffers();
                        mOutputBuffers = mMediaCodec.getOutputBuffers();
                        Log.d(TAG, "mInputBuffers.len=" + mInputBuffers.length + ", mOutputBuffers.len=" + mOutputBuffers.length);
                        mCodecHandler.sendEmptyMessage(MSG_CODEC_QUEUE_BUF);
                        Log.d(TAG, "MSG_CODEC_START end");
                        mCurState = 2;
                        mStartTime = System.nanoTime();
                        break;
                    case MSG_CODEC_QUEUE_BUF:
                        //Log.d(TAG, "MSG_CODEC_QUEUE_BUF");
                        int idx = mMediaCodec.dequeueInputBuffer(-1);
                        //Log.d(TAG, "[input-buffer] dequeueInputBuffer idx=" + idx);
                        if (idx < 0) {
                            mCodecHandler.sendEmptyMessageDelayed(MSG_CODEC_QUEUE_BUF, 30);
                            return;
                        }
                        ByteBuffer buf = mInputBuffers[idx];
                        buf.clear();

                        try {
                            int rd_len = mYuvStream.read(mByteArray);
                            if (rd_len != mPicSz) {
                                Log.w(TAG, "found EOF in yuv_file! rd_len=" + rd_len);
                                mMediaCodec.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                //mCodecHandler.sendEmptyMessage(MSG_CODEC_STOP);
                                return;
                            }
                            //Log.d(TAG, "read " + rd_len + " Bytes from yuv_file!");
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                        buf.put(mByteArray);
                        mMediaCodec.queueInputBuffer(idx, 0, mByteArray.length, 0, 0);
                        mCodecHandler.sendEmptyMessage(MSG_CODEC_QUEUE_BUF);
                        break;
                    case MSG_CODEC_STOP:
                        Log.d(TAG, "MSG_CODEC_STOP");
                        mMediaCodec.stop();
                        mMediaCodec.release();
                        mCurState = 3;
                        mStopTime = System.nanoTime();
                        mEncodingDuraMilliSec = (int)((mStopTime - mStartTime)/1000000L);
                        mEncodingFps = mTotalFrameCnt*1000/mEncodingDuraMilliSec;
                        Log.d(TAG, "encoding speed=" + mEncodingFps + "fps, total frm_cnt=" + mTotalFrameCnt + ", dura=" + mEncodingDuraMilliSec + " ms");
                        button2.setText("encoding speed=" + mEncodingFps + "fps, total frm_cnt=" + mTotalFrameCnt + ", dura=" + mEncodingDuraMilliSec + "ms");
                        break;
                    default:
                        Log.d(TAG, "unknown case! should never happen!");
                        break;
                }
            }
        };

        // prepare Muxer handler
        mMuxerThread = new HandlerThread("MuxerThread");
        mMuxerThread.start();
        mMuxerHandler = new Handler(mMuxerThread.getLooper()) {
            // read bs from MediaCodec's outport, and write to muxer.
            public void handleMessage(Message msg) {
                switch(msg.what) {
                    case MSG_MUXER_START:
                        Log.d(TAG, "MSG_MUXER_START");
                        button2 = (Button) findViewById(R.id.button2);
                        button2.setText("encode couter");
                        mMuxerHandler.sendEmptyMessage(MSG_MUXER_READ_SPSPPS);
                        break;
                    case MSG_MUXER_STOP_READ_BS:
                        Log.d(TAG, "MSG_MUXER_STOP_READ_BS");
                        // clear MediaCodec in mCodecHandler
                        mCodecHandler.sendEmptyMessage(MSG_CODEC_STOP);
                        // update UI disp, should avoid below:
                        // AndroidRuntime: android.view.ViewRootImpl$CalledFromWrongThreadException: Only the original thread that created a view hierarchy can touch its views
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                button0.setText("finished!");
                            }
                        });
                        // close file handle
                        try {
                            mYuvStream.close();
                            mAvcStream.close();
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case MSG_MUXER_READ_SPSPPS:
                        Log.d(TAG, "MSG_MUXER_READ_SPSPPS");
                        MediaCodec.BufferInfo buf_info = new MediaCodec.BufferInfo();
                        int idx = mMediaCodec.dequeueOutputBuffer(buf_info, -1);
                        //Log.d(TAG, "[output-buffer] idx=" + idx + ", sz=" + buf_info.size);
                        if ((idx>=0) && (buf_info.flags==MediaCodec.BUFFER_FLAG_CODEC_CONFIG)) {
                            int csd_sz = buf_info.size;
                            byte[] csd_data = new byte[csd_sz];
                            mOutputBuffers[idx].get(csd_data);
                            String strHex = "";
                            StringBuilder sb = new StringBuilder("");
                            for (int i=0; i<csd_sz; i++) {
                                strHex = Integer.toHexString(csd_data[i] & 0xFF);
                                sb.append(" " + ((strHex.length()==1)?"0"+strHex:strHex));
                            }
                            //Log.d(TAG, "get CSD! csd_sz=" + csd_sz + ", csd_data=" + Arrays.toHexString(csd_data));
                            Log.d(TAG, "get CSD! csd_sz=" + csd_sz + ", csd_data=" + sb.toString().trim());
                            try {
                                mAvcStream.write(csd_data, 0, csd_sz);
                            } catch(IOException e) {
                                e.printStackTrace();
                            }
                            mMediaCodec.releaseOutputBuffer(idx, false);

                            button2 = (Button) findViewById(R.id.button2);
                            button2.setText("encode couter");
                        } else {
                            //Log.e(TAG, "fatal error! no csd? never happen!!! we cotinue pull it!");
                            Log.w(TAG, "no csd? never happen!!! we cotinue pull it!");
                            mMuxerHandler.sendEmptyMessage(MSG_MUXER_READ_SPSPPS);
                            break;
                        }
                        mMuxerHandler.sendEmptyMessage(MSG_MUXER_LOOP_READ_BS);
                        break;
                    case MSG_MUXER_LOOP_READ_BS:
                        //Log.d(TAG, "MSG_MUXER_READ_BS");
                        buf_info = new MediaCodec.BufferInfo();
                        idx = mMediaCodec.dequeueOutputBuffer(buf_info, -1);
                        if (idx >= 0) {
                            if (buf_info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                Log.w(TAG, "find EOS in output-buffer! need quit!");
                                mMediaCodec.releaseOutputBuffer(idx, false);
                                mMuxerHandler.sendEmptyMessage(MSG_MUXER_STOP_READ_BS);
                                break;
                            }
                            int bs_sz = buf_info.size;
                            byte[] bs_data = new byte[bs_sz];
                            mOutputBuffers[idx].get(bs_data);
                            try {
                                mAvcStream.write(bs_data, 0, bs_sz);
                            } catch(IOException e) {
                                e.printStackTrace();
                            }
                            mMediaCodec.releaseOutputBuffer(idx, false);
                            mMuxerHandler.sendEmptyMessage(MSG_MUXER_LOOP_READ_BS);
                            mEncFrmCnt++;
                            button2.setText("cnt:" + mEncFrmCnt);
                            Log.d(TAG, "[output-buffer] idx=" + idx + ", sz=" + buf_info.size + ", flag=" + buf_info.flags + ", cnt=" + mEncFrmCnt);
                        } else {
                            mMuxerHandler.sendEmptyMessageDelayed(MSG_MUXER_LOOP_READ_BS, 20);
                        }
                        break;
                    default:
                        Log.d(TAG, "unknown case! should never happen!");
                        break;
                }
            }
        };

        // prepare src yuv file and dst h264 file
        String state = EnvironmentEx.getExternalStoragePathState();
        boolean ext_mounted = Environment.MEDIA_MOUNTED.equals(state);
        Log.d(TAG, "state:" + state + ", ext_mounted:" + ext_mounted);
        String dir_path;
        if (ext_mounted == true) {
            dir_path = EnvironmentEx.getExternalStoragePath().toString() + "/MediaCodecDemo";
        } else {
            dir_path = EnvironmentEx.getInternalStoragePath().toString() + "/MediaCodecDemo";
        }
        Log.d(TAG, "dir_path:" + dir_path);

        File yuv_dir = new File(dir_path);
        if (yuv_dir.isDirectory()) {
            File lists[] = yuv_dir.listFiles();
            Log.d(TAG, "total file_cnt=" + lists.length + " in " + yuv_dir);
            for (int i=0; i<lists.length; i++)
                Log.d(TAG, "    [" + i + "]: " + lists[i].getName());

            try {
                //String src_yuv = lists[0].getPath();
                //Log.d(TAG, "read from file(" + src_yuv + ") to feed MediaCodec!");
                //File f_in = new File(src_yuv);
                Log.d(TAG, "read from file(" + mYuvPath + ") to feed MediaCodec!");
                File f_in = new File(mYuvPath);
                mYuvStream = new FileInputStream(f_in);


                String dst_avc = dir_path + "/output.h264";
                Log.d(TAG, "encode by h.264, and write bistream to file(" + dst_avc + ")!");
                File f_out = new File(dst_avc);
                mAvcStream = new FileOutputStream(f_out);

                mPicSz = mWidth * mHeight * 3 >> 1;
                mByteArray = new byte[mPicSz];

                long file_len = f_in.length();
                mTotalFrameCnt = (int)(file_len/mPicSz);
                Log.d(TAG, "input yuv length=" + file_len + ", total frame cnt=" + mTotalFrameCnt);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "we create yuv_dir!");
            yuv_dir.mkdirs();
        }

        // prepare MediaFormat and MediaCodec
        mMediaFormat = MediaFormat.createVideoFormat(kMimeTypeAvc, mWidth, mHeight);
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mWidth*mHeight*5);
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        try {
            mMediaCodec = MediaCodec.createEncoderByType(kMimeTypeAvc);
        } catch(IOException e) {
            e.printStackTrace();
        }
        mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        // prepare MediaMuxer -> not use currently, we keep naked h264 stream.
        mCurState = 1;
        mEncFrmCnt = 0;
    }

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
                mYuvPath = getPath(this, uri);
            } else {//4.4以下下系统调用方法
                mYuvPath = getRealPathFromURI(uri);
            }
            Log.d(TAG, "after parse uri, video absolute path=" + mYuvPath);
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
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
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


    @Override
    protected void onCreate(Bundle saveInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(saveInstanceState);
        setContentView(R.layout.main_activity);

        mMainHandler = new Handler();

        button3 = (Button) findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                //intent.setType("image/*");        //选择图片
                //intent.setType("audio/*");        //选择音频
                //intent.setType("video/*");        //选择视频
                //intent.setType("video/*; image/*"); //同时选择视频和图片
                intent.setType("*/*");              //无类型限制
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 1);
            }
        });

        etWidth = (EditText)findViewById(R.id.edt_width);
        etWidth.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    String width = etWidth.getText().toString();
                    Toast.makeText(MainActivity.this, "yuv width=" + width, Toast.LENGTH_LONG).show();
                    //handled = true;
                }
                return handled;
            }
        });

        etHeight = (EditText)findViewById(R.id.edt_height);
        etHeight.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String height = etHeight.getText().toString();
                    Toast.makeText(MainActivity.this, "yuv height=" + height, Toast.LENGTH_LONG).show();
                    //handled = true;
                }
                return handled;
            }
        });

        // init button
        button0 = (Button) findViewById(R.id.button0);
        button0.setText("prepare");
        button0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurState == 0) {
                    prepare();
                    if (mCurState == 1) {
                        button0.setText("start");
                    }
                } else if (mCurState == 1) {
                    Log.d(TAG, "start MediaCode and MediaMuxer!");
                    mCodecHandler.sendEmptyMessage(MSG_CODEC_START);
                    mMuxerHandler.sendEmptyMessageDelayed(MSG_MUXER_START, 50);// 50ms for wait MediaCodec.start() finish, maybe longer...
                    button0.setText("running...");
                } else if (mCurState == 2) {
                    Log.w(TAG, "no response because encoding...");
                } else if (mCurState == 3) {
                    Log.w(TAG, "usr want to start again!");
                    prepare();
                    if (mCurState == 1) {
                        button0.setText("start again!");
                    }
                }
            }
        });

        // quit button
        button1 = (Button) findViewById(R.id.button1);
        button1.setText("quit Activity");
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, "quit Activity!");
                mCodecThread.quit();
                finish();
            }
        });
    }

    protected void onDestroy() {
        Log.w(TAG, "onDestroy!");
        super.onDestroy();
    }
}
