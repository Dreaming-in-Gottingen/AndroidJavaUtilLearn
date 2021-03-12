package com.example.MediaExtractorDemo;

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


import java.util.Arrays;

public class MainActivity extends Activity {
    private static final String TAG = "MediaExtractorDemo";
    private Button mButton0; // reset button, select which video-file to extract
    private Button mButton1; // extract video track
    private Button mButton2; // extract audio track
    private Button mButton3; // exit

    final private int MAX_BS_LEN = 1024*1024;

    private OutputStream mAvcStream;
    private OutputStream mAacStream;

    private final String kMimeTypeAvc = "video/avc";
    private final String kMimeTypeAac = "audio/mp4a-latm";

    private String mVideoPath;
    private String mVBsPath;
    private String mABsPath;

    private MediaExtractor mExtractor;

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

            if (mExtractor != null) {
                mExtractor.release();
                mExtractor = null;
            }
            mExtractor = new MediaExtractor();
            try {
                mExtractor.setDataSource(mVideoPath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            int track_cnt = mExtractor.getTrackCount();
            Log.d(TAG, "track_cnt=" + track_cnt);

            for (int i=0; i<track_cnt; i++) {
                MediaFormat media_format = mExtractor.getTrackFormat(i);
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
                // make new file path
                // /storage/emulated/0/xcom/dji_x264.mp4 -> /storage/emulated/0/xcom/dji_x264_vbs.h264
                int idx = mVideoPath.lastIndexOf(".");
                mVBsPath = mVideoPath.substring(0, idx).concat("_vbs.h264");
                Log.d(TAG, "write h264 to file(" + mVBsPath + ")!");

                File out_avc = new File(mVBsPath);
                try {
                    mAvcStream = new FileOutputStream(out_avc);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                MediaFormat media_format = mExtractor.getTrackFormat(0);

                try {
                    ByteBuffer csd_buffer = media_format.getByteBuffer("csd-0");
                    int csd_sz = csd_buffer.capacity();
                    byte[] csd_data = new byte[csd_sz];
                    csd_buffer.get(csd_data);
                    mAvcStream.write(csd_data, 0, csd_sz);

                    csd_buffer = media_format.getByteBuffer("csd-1");
                    csd_sz = csd_buffer.capacity();
                    csd_data = new byte[csd_sz];
                    csd_buffer.get(csd_data);
                    mAvcStream.write(csd_data, 0, csd_sz);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ByteBuffer input_buffer = ByteBuffer.allocate(MAX_BS_LEN);
                mExtractor.selectTrack(0);
                int frm_idx = 0;

                while (mExtractor.readSampleData(input_buffer, 0) != -1) {
                    int index = mExtractor.getSampleTrackIndex();
                    long pts = mExtractor.getSampleTime();
                    long size = mExtractor.getSampleSize();
                    Log.d(TAG, "track_idx=" + index + ", frm_idx=" + frm_idx++ + ", pts=" + pts/1000 + ", size=" + size);
                    //mButton1.setText("frm_cnt:" + frm_idx);

                    try {
                        int bs_sz = (int)size;
                        byte[] bs_data = new byte[bs_sz];
                        input_buffer.get(bs_data);
                        mAvcStream.write(bs_data, 0, bs_sz);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    boolean eos = mExtractor.advance();
                    if (eos == false) {
                        Log.w(TAG, "warning: end of stream!");
                        break;
                    }
                }

                try {
                    mAvcStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mButton1.setText("extract vbs to path=" + mVBsPath);
                Toast.makeText(MainActivity.this, "extract video bitstream finished!", Toast.LENGTH_SHORT).show();
            }
        });

        mButton2 = (Button) findViewById(R.id.button2);
        mButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int track_cnt = mExtractor.getTrackCount();
                if (track_cnt < 2) {
                    Log.w(TAG, "only have one track, no audio track!");
                    Toast.makeText(MainActivity.this, "no audio track!", Toast.LENGTH_SHORT).show();
                    mButton2.setText("no audio track in this fille");
                    return;
                }

                // /storage/emulated/0/xcom/dji_x264.mp4 -> /storage/emulated/0/xcom/dji_x264_abs.aac
                int idx = mVideoPath.lastIndexOf(".");
                mABsPath = mVideoPath.substring(0, idx).concat("_abs.aac");
                Log.d(TAG, "abs_path=" + mABsPath);

                File out_aac = new File(mABsPath);
                try {
                    mAacStream = new FileOutputStream(out_aac);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                MediaFormat media_format = mExtractor.getTrackFormat(1);

                ByteBuffer csd_buffer = media_format.getByteBuffer("csd-0");
                int csd_sz = csd_buffer.capacity();
                byte[] csd_data = new byte[csd_sz];
                csd_buffer.get(csd_data);

                String str0 = Integer.toHexString(0xFF & csd_data[0]);
                String str1 = Integer.toHexString(0xFF & csd_data[1]);
                Log.d(TAG, "csd-data: 0x" + str0 + ", 0x" + str1);

                byte[] adts = new byte[7];

                byte profile = (byte)((csd_data[0] >>> 3) - 1);
                byte sampling_freq_index = (byte)(((csd_data[0] & 7) << 1) | ((csd_data[1] >>> 7) & 0x1));
                byte channel_configuration = (byte)((csd_data[1] >>> 3) & 0x0f);

                String str2 = Integer.toHexString(0xFF & profile);
                String str3 = Integer.toHexString(0xFF & sampling_freq_index);
                String str4 = Integer.toHexString(0xFF & channel_configuration);
                Log.d(TAG, "profile:0x" + str2 + ", sample_idx:0x" + str3 + ", chn:0x" + str4);

                adts[0] = (byte)0xff;
                adts[1] = (byte)0xf1;
                adts[2] = (byte)(profile << 6
                                 | sampling_freq_index << 2
                                 | ((channel_configuration >>> 2) & 1));

                ByteBuffer input_buffer = ByteBuffer.allocate(MAX_BS_LEN);
                mExtractor.selectTrack(1);
                int frm_idx = 0;

                int is_adts = media_format.getInteger(MediaFormat.KEY_IS_ADTS, 0);
                Log.d(TAG, "is_adts:" + is_adts);

                while (mExtractor.readSampleData(input_buffer, 1) > 0) {
                    int index = mExtractor.getSampleTrackIndex();
                    long pts = mExtractor.getSampleTime();
                    long size = mExtractor.getSampleSize();
                    Log.d(TAG, "track_idx=" + index + ", frm_idx=" + frm_idx++ + ", pts=" + pts/1000 + ", size=" + size);
                    //mButton2.setText("frm_cnt:" + frm_idx);

                    try {
                        int bs_sz = (int)size;
                        byte[] bs_data = new byte[bs_sz];
                        input_buffer.get(bs_data);

                        if (is_adts == 0) {
                            int tmp_sz = bs_sz + 7;  // size(header) + size(AACFrame)
                            adts[3] = (byte)((channel_configuration & 3) << 6 | tmp_sz >>> 11);
                            adts[4] = (byte)((tmp_sz >>> 3) & 0xff);
                            adts[5] = (byte)((tmp_sz & 7) << 5);
                            adts[6] = 0;
                            mAacStream.write(adts, 0, 7);
                        }
                        mAacStream.write(bs_data, 0, bs_sz);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    boolean eos = mExtractor.advance();
                    if (eos == false) {
                        Log.w(TAG, "warning: end of stream!");
                        break;
                    }
                }

                try {
                    mAacStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mButton2.setText("extract abs to path=" + mABsPath);
                Toast.makeText(MainActivity.this, "extract audio bitstream finished!", Toast.LENGTH_SHORT).show();
            }
        });

        mButton3 = (Button) findViewById(R.id.button3);
        mButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, "quit Activity!");
                if (mExtractor != null) {
                    mExtractor.release();
                    mExtractor = null;
                }
                finish();
            }
        });
    }

    protected void onDestroy() {
        Log.w(TAG, "onDestroy!");
        super.onDestroy();
    }
}
