package com.example.Extractor2Codec2RenderDemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import android.os.HandlerThread;
import android.os.Handler;
import android.os.Message;

import android.media.MediaFormat;
import android.media.MediaExtractor;
import android.media.MediaCodec;

import android.os.Environment;
import android.os.Build;

import android.database.Cursor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.content.ContentUris;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.nio.ByteBuffer;

import android.net.Uri;

import android.view.Surface;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

public class MainActivity extends Activity {
    private static final String TAG = "Extractor2Codec2RenderDemo";
    private Button mButton0; // reset button, select which video-file to extract
    private Button mButton1; // extract video track
    private Button mButton2; // extract audio track
    private Button mButton3; // exit

    final private int MAX_BS_LEN = 1024*1024;

    private final String kMimeTypeAvc = "video/avc";
    private final String kMimeTypeAac = "audio/mp4a-latm";

    private int mTrackType; // 0-video; 1-audio
    volatile private int mTrackCnt;  // 1 -- file without audio

    private MediaFormat mMediaFormat;

    private MediaCodec mMediaCodec;

    private MediaCodec.BufferInfo mBufferInfo;
    private ByteBuffer[] mInputBuffers;
    private ByteBuffer[] mOutputBuffers;

    private String mVideoPath;

    private int mExtractFrameCnt = 0; // from extractor
    private int mDecFrmCnt = 0;       // from codec

    private MediaExtractor mMediaExtractor;

    // feed bitstream to MC's inport-buffer in this loop
    private HandlerThread mCodecThread; // dequeue/queue MC input-buffer.
    private Handler mCodecHandler;

    private HandlerThread mRenderThread; // queue/release MC output-buffer, and render(Surface/AudioTrack).
    private Handler mRenderHandler;

    private Handler mMainHandler;   // main handler update decoded frame count

    private boolean mbDumpSupportedInputFormat = true;
    private boolean mbDumpCurrentOutputFormat = true;

    private final int MSG_CODEC_START = 0;
    private final int MSG_CODEC_STOP = 1;
    private final int MSG_CODEC_DEQUEUE_IN_BUF = 2;
    private final int MSG_CODEC_DEQUEUE_OUT_BUF = 3;

    private final int MSG_RENDER_START = 0;
    private final int MSG_RENDER_STOP = 1;

    private Surface mPlaySurface = null;


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

            if (mMediaExtractor != null) {
                mMediaExtractor.release();
                mMediaExtractor = null;
            }
            mMediaExtractor = new MediaExtractor();
            try {
                mMediaExtractor.setDataSource(mVideoPath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            int mTrackCnt = mMediaExtractor.getTrackCount();
            Log.d(TAG, "track_cnt=" + mTrackCnt);

            for (int i=0; i<mTrackCnt; i++) {
                MediaFormat media_format = mMediaExtractor.getTrackFormat(i);
                // careful: i may be not equal to track-id, which is contained in MediaFormat
                String mime = media_format.getString(MediaFormat.KEY_MIME);
                Log.d(TAG, "track_idx=" + i + ", mime=" + mime);
                Log.d(TAG, "          " + media_format);
            }
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

    private void prepare(int type) {
        // prepare Codec handler
        mCodecThread = new HandlerThread("CodecThread");
        mCodecThread.start();
        mCodecHandler = new Handler(mCodecThread.getLooper()) {
            public void handleMessage(Message msg) {
                // workflow: dequeue buf -> extract into buf -> queue buf
                switch(msg.what) {
                    case MSG_CODEC_START:
                        Log.d(TAG, "MSG_CODEC_START");
                        mMediaCodec.start();
                        mInputBuffers = mMediaCodec.getInputBuffers();
                        mOutputBuffers = mMediaCodec.getOutputBuffers();
                        Log.d(TAG, "mInputBuffers.len=" + mInputBuffers.length + ", mOutputBuffers.len=" + mOutputBuffers.length);
                        mBufferInfo = new MediaCodec.BufferInfo();
                        mCodecHandler.sendEmptyMessage(MSG_CODEC_DEQUEUE_IN_BUF);
                        mbDumpCurrentOutputFormat = true;
                        break;
                    case MSG_CODEC_STOP:
                        Log.d(TAG, "MSG_CODEC_STOP");
                        mMediaCodec.stop();
                        mMediaCodec.release();
                        mMediaCodec = null;
                        break;
                    case MSG_CODEC_DEQUEUE_IN_BUF:
                        int idx = mMediaCodec.dequeueInputBuffer(-1);
                        if (idx < 0) {
                            mCodecHandler.sendEmptyMessageDelayed(MSG_CODEC_DEQUEUE_IN_BUF, 30); // 30ms
                            return;
                        }
                        ByteBuffer buf = mInputBuffers[idx];
                        buf.clear();

                        if (mMediaExtractor.readSampleData(buf, 0) != -1) {
                            int track_id = mMediaExtractor.getSampleTrackIndex();
                            long pts = mMediaExtractor.getSampleTime();
                            int size = (int)mMediaExtractor.getSampleSize();
                            Log.d(TAG, "[input-buffer] buf_idx=" + idx + ", mExtractFrameCnt=" + mExtractFrameCnt++ + ", pts=" + pts/1000 + ", size=" + size);

                            boolean eos = mMediaExtractor.advance();
                            if (eos == false) {
                                Log.w(TAG, "warning: end of stream found from file!");
                                mMediaCodec.queueInputBuffer(idx, 0, size, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                //mCodecHandler.sendEmptyMessage(MSG_CODEC_STOP); // move to MuxerThread to send this message!
                                break;
                            }
                            mMediaCodec.queueInputBuffer(idx, 0, size, pts, 0);
                            mCodecHandler.sendEmptyMessage(MSG_CODEC_DEQUEUE_IN_BUF);
                        } else {
                            Log.w(TAG, "unknown error: no more data available!");
                            Log.d(TAG, "seekTo start of file!");
                            //mMediaExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                            //mCodecHandler.sendEmptyMessage(MSG_CODEC_DEQUEUE_IN_BUF);
                            mCodecHandler.sendEmptyMessage(MSG_CODEC_STOP);
                        }
                        break;
                        //mButton1.setText("dec_frm_cnt:" + frm_idx);
                }
            }
        };

        // prepare Muxer handler
        mRenderThread = new HandlerThread("MuxerThread");
        mRenderThread.start();
        mRenderHandler = new Handler(mRenderThread.getLooper()) {
            public void handleMessage(Message msg) {
                switch(msg.what) {
                    case MSG_RENDER_START:
                        Log.w(TAG, "MSG_RENDER_START");
                        mRenderHandler.sendEmptyMessageDelayed(MSG_CODEC_DEQUEUE_OUT_BUF, 200);
                        break;
                    case MSG_RENDER_STOP:
                        Log.w(TAG, "MSG_RENDER_STOP");
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run () {
                                // reset everything
                                mButton1.setText("extract video");
                                mButton2.setText("extract audio");
                                mRenderThread.quit();
                                mCodecThread.quit();
                            }
                        });
                        break;
                    case MSG_CODEC_DEQUEUE_OUT_BUF:
                        int idx = mMediaCodec.dequeueOutputBuffer(mBufferInfo, -1);
                        if (idx >= 0) {
                            if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                Log.w(TAG, "find EOS in output-buffer! need quit!");
                                mMediaCodec.releaseOutputBuffer(idx, false);
                                mRenderHandler.sendEmptyMessage(MSG_RENDER_STOP);
                                mCodecHandler.sendEmptyMessage(MSG_CODEC_STOP);
                                break;
                            }
                            if (mbDumpCurrentOutputFormat == true) {
                                Log.d(TAG, "output_format=" + mMediaCodec.getOutputFormat());
                                mbDumpCurrentOutputFormat = false;
                            }
                            int out_sz = mBufferInfo.size;
                            long pts = mBufferInfo.presentationTimeUs;

                            if (mTrackType == 0) {
                                mMainHandler.post(new Runnable() {
                                    @Override
                                    public void run () {
                                        mButton1.setText("video_dec_frm_cnt=" + mDecFrmCnt++);
                                    }
                                });
                            } else {
                                mMainHandler.post(new Runnable() {
                                    @Override
                                    public void run () {
                                        mButton2.setText("audio_dec_frm_cnt=" + mDecFrmCnt++);
                                    }
                                });
                            }
                            Log.d(TAG, "[output-buffer] buf_idx=" + idx + ", sz=" + out_sz + ", cnt=" + mDecFrmCnt++ + ", pts=" + pts/1000);
                            // not sleep before render, I have not found an elegant method except sleep.
                            // so it will play as quickly as decode speed.
                            mMediaCodec.releaseOutputBuffer(idx, (mTrackType==0)? true:false);
                            mRenderHandler.sendEmptyMessage(MSG_CODEC_DEQUEUE_OUT_BUF);
                        } else if (idx == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) { // for BUG2
                            Log.w(TAG, "INFO_OUTPUT_BUFFERS_CHANGED happen! must getOutputBuffers again!");
                            mOutputBuffers = mMediaCodec.getOutputBuffers();
                            mRenderHandler.sendEmptyMessage(MSG_CODEC_DEQUEUE_OUT_BUF);
                        } else if (idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            Log.w(TAG, "INFO_OUTPUT_FORMAT_CHANGED happen!");
                            Log.w(TAG, "new_format=" + mMediaCodec.getOutputFormat());
                            mRenderHandler.sendEmptyMessage(MSG_CODEC_DEQUEUE_OUT_BUF);
                        } else {
                            Log.d(TAG, "no decoded frame now...");
                            mRenderHandler.sendEmptyMessageDelayed(MSG_CODEC_DEQUEUE_OUT_BUF, 100);
                        }
                        break;
                }
            }
        };

        // prepare render
        mTrackType = type;
        if (mTrackType == 0) {  // to SurfaceView
            SurfaceView sv = (SurfaceView) findViewById(R.id.surview);
            SurfaceHolder surface_holder = sv.getHolder();
            surface_holder.setFixedSize(1280, 720);
            surface_holder.addCallback(mHolderCB);
            mPlaySurface = surface_holder.getSurface();
        } else {                // to AudioTrack
        
        }

        // prepare extractor
        mMediaFormat = mMediaExtractor.getTrackFormat(mTrackType);
        String mime = mMediaFormat.getString(MediaFormat.KEY_MIME);
        try {
            mMediaCodec = MediaCodec.createDecoderByType(mime);
            Log.d(TAG, "codec_name=" + mMediaCodec.getName());
            // may crash when out of range for hw decoder.
            // because omx-hw-decoder will check capability.
            mMediaCodec.configure(mMediaFormat, (mTrackType==0)?mPlaySurface:null, null, 0);
            if (mbDumpSupportedInputFormat == true) {
                Log.d(TAG, "supported_input_format=" + mMediaCodec.getInputFormat());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMediaExtractor.selectTrack(mTrackType);

        mExtractFrameCnt = 0; // from extractor
        mDecFrmCnt = 0;       // from codec
    }

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

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        Log.d(TAG, "onCreate");
        Log.d(TAG, "sdk_version=" + Build.VERSION.SDK_INT);

        super.onCreate(saveInstanceState);
        setContentView(R.layout.main_activity);

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
                mButton1.setText("extract video and decoding...");
                prepare(0);
                mCodecHandler.sendEmptyMessage(MSG_CODEC_START);
                mRenderHandler.sendEmptyMessage(MSG_RENDER_START);
                //Toast.makeText(MainActivity.this, "extract video bitstream finished!", Toast.LENGTH_SHORT).show();
            }
        });

        mButton2 = (Button) findViewById(R.id.button2);
        mButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // may equal to 0 if no volatile -> no useful, BUG1
                //if (mTrackCnt < 2) {
                //    Log.w(TAG, "only have one track, no audio track! track_cnt=" + mTrackCnt);
                //    Toast.makeText(MainActivity.this, "no audio track!", Toast.LENGTH_SHORT).show();
                //    mButton2.setText("no audio track in this fille");
                //    return;
                //}
                mButton2.setText("extract audio and decoding...");
                prepare(1);
                mCodecHandler.sendEmptyMessage(MSG_CODEC_START);
                mRenderHandler.sendEmptyMessage(MSG_RENDER_START);
            }
        });

        mButton3 = (Button) findViewById(R.id.button3);
        mButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, "quit Activity!");
                if (mMediaExtractor != null) {
                    mMediaExtractor.release();
                    mMediaExtractor = null;
                }
                finish();
            }
        });

        mMainHandler = new Handler();
    }

    protected void onDestroy() {
        Log.w(TAG, "onDestroy!");
        super.onDestroy();
    }
}
