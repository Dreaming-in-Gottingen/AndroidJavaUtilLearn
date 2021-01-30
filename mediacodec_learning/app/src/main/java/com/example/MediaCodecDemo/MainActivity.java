package com.example.MediaCodecDemo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.widget.Button;

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

public class MainActivity extends Activity {
    private static final String TAG = "MediaCodecDemo";
    private Button button0;
    private Button button1;
    private Button button2; // show encode counter

    private int mCurState = 0; // 0-uninitialized; 1-prepared; 2-running; 3-finished
    private int mEncFrmCnt = 0;

    private Handler mMainHandler;

    // feed yuv to MC's inport-buffer in this loop
    private HandlerThread mCodecThread; // queue MC input-buffer.
    private Handler mCodecHandler;

    private HandlerThread mMuxerThread; // dequeue MC output-buffer, and muxer.
    private Handler mMuxerHandler;

    InputStream mYuvStream;
    OutputStream mAvcStream;

    private int mWidth = 640;
    private int mHeight = 620;
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
            dir_path = EnvironmentEx.getExternalStoragePath().toString() + "/zjz";
        } else {
            dir_path = EnvironmentEx.getInternalStoragePath().toString() + "/zjz";
        }
        Log.d(TAG, "dir_path:" + dir_path);

        File yuv_dir = new File(dir_path);
        if (yuv_dir.isDirectory()) {
            File lists[] = yuv_dir.listFiles();
            Log.d(TAG, "total file_cnt=" + lists.length + " in " + yuv_dir);
            for (int i=0; i<lists.length; i++)
                Log.d(TAG, "    [" + i + "]: " + lists[i].getName());

            try {
                String src_yuv = lists[0].getPath();
                Log.d(TAG, "read from file(" + src_yuv + ") to feed MediaCodec!");
                File f_in = new File(src_yuv);
                mYuvStream = new FileInputStream(f_in);

                String dst_avc = dir_path + "/output.h264";
                Log.d(TAG, "write h264 to file(" + dst_avc + ")!");
                File f_out = new File(dst_avc);
                mAvcStream = new FileOutputStream(f_out);

                mPicSz = mWidth * mHeight * 3 >> 1;
                mByteArray = new byte[mPicSz];
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

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(saveInstanceState);
        setContentView(R.layout.main_activity);

        mMainHandler = new Handler();

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
