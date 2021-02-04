package com.example.camera1test;

import android.hardware.Camera;
import android.media.MediaRecorder;

//import android.os.Handler;
//import android.os.HandlerThread;

import android.content.Context;

import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

import android.util.Log;
import android.util.DisplayMetrics;

//import android.os.EnvironmentEx;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

public class CameraHelper implements SurfaceHolder.Callback, Camera.PictureCallback {
    private final static String TAG = "CameraHelper";

    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;

    private Camera mCamera;
    private MediaRecorder mRecorder;

    //private Handler mCameraHandler;

    private String mCamera1TestDir;

    private int mPreviewWidth = 1280;
    private int mPreviewHeight = 720;
    private int mVideoWidth = 1920;
    private int mVideoHeight = 1080;

    private Context mContext;

    private int mCamId = 0;

    private int mPhotoNum = 0;
    private int mVideoNum = 0;

    private Boolean mPreviewState = false;
    private Boolean mRecorderState = false;

    public static int getCameraDeviceCount() {
        int cameraCnt = Camera.getNumberOfCameras();
        if (cameraCnt == 0) {
            Log.e(TAG, "no camera device!");
        }
        return cameraCnt;
    }

    public CameraHelper(Context ctx, SurfaceView sv) {
        Log.d(TAG, "CameraHelper ctor begin!");
        mContext = ctx;

        mSurfaceView = sv;
        //mSurfaceHolder = mSurfaceView.getHolder();
        //mSurfaceHolder.setFixedSize(mPreviewWidth, mPreviewHeight);
        //mSurfaceHolder.addCallback(mSurfaceHolderCallback);

        //HandlerThread handlerThread = new HandlerThread("CameraHelper");
        //handlerThread.start();
        //mCameraHandler = new Handler(handlerThread.getLooper());

        // prepare dir for photos and videos
        String storage_dir = Environment.getExternalStorageDirectory().getPath();
        mCamera1TestDir = storage_dir + "/camera1test";
        File f_dir = new File(mCamera1TestDir);
        if (!f_dir.isDirectory()) {
            Log.w(TAG, "no dir[" + mCamera1TestDir + "] to keep photos/videos, create it!");
            f_dir.mkdirs();
        }

        Log.d(TAG, "CameraHelper ctor end!");
    }

    private void initCameraParameters() {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.set("rotation", 90); //相片镜头角度转90度（默认摄像头是横拍）
        parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
        parameters.setPictureSize(1920, 1080);
        parameters.setJpegThumbnailSize(0, 0); // no thumbnail in jpeg APP1(Exif)
        parameters.dump();
        mCamera.setParameters(parameters);//添加参数
        mCamera.setDisplayOrientation(90);//设置显示方向
    }

    public void openCameraAndStartPreview(int camId) {
        mCamId = camId;
        try {
            // default open BACK camera without inparam camId.
            //mCamera = Camera.open();
            mCamera = Camera.open(camId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        initCameraParameters();

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setFixedSize(mPreviewWidth, mPreviewHeight);
        mSurfaceHolder.addCallback(this);
    }

    public void closeCameraAndStopPreview() {
		try {
            mCamera.stopPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mCamera.release();
        mCamera = null;
        mPreviewState = false;
	}

    private void configureRecorder() {
        mRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set output file format
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        // Step 4: Set enc parameters
        mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mRecorder.setVideoSize(mVideoWidth, mVideoHeight);
        mRecorder.setVideoEncodingBitRate(6*1024*1024);
        mRecorder.setVideoFrameRate(30);

        // Step 5: Set rotation degree in mp4 file
        mRecorder.setOrientationHint(90);

        //String storage_dir = Environment.getExternalStorageDirectory().getPath();
        //String video_dir = storage_dir + "/zjz";
        String video_path = mCamera1TestDir + "/video_camid" + mCamId + "_num" + mVideoNum + ".mp4";
        Log.d(TAG, "video_path=" + video_path);
        mVideoNum++;
        // Step 6: Set output file path
        mRecorder.setOutputFile(video_path);

        // Step 7: Register error listener
        mRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                mRecorder.stop();
                mRecorder.reset();
            }
        }); 
    }

    public void startRecorder() {
        if (mRecorderState == true)
            return;

        Log.d(TAG, "startRecorder");
        configureRecorder();

        // Step 8: Prepare configured MediaRecorder
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Step 9: Start recorder
        mRecorder.start();
        mRecorderState = true;
    }

    public void stopRecorder() {
        if (mRecorderState == false)
            return;

        Log.d(TAG, "stopRecorder");
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;    
            mCamera.lock();
            mRecorderState = false;
        }
    }

    public void updatePreview() {
        if (mPreviewState == true) {
            return;
        }
        Log.d(TAG, "need updatePreview!");

        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
        Log.w(TAG, "surfaceDestroyed");
        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }
    }

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		Log.w(TAG, "format=" + format + ", w x h=" + w + "x" + h);
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mSurfaceHolder.getSurface() == null){
          // preview surface does not exist
          return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
        mPreviewState = true;
    }

    public void takePhoto() {
        Log.d(TAG, "takePhoto begin");
        mCamera.takePicture(null, null, this);
        Log.d(TAG, "takePhoto end");
    }

    public void onPictureTaken(byte[] data, Camera camera) {
        Log.d(TAG, "onPictureTaken begin");
        //String storage_dir = Environment.getExternalStorageDirectory().getPath();
        //String photo_dir = storage_dir + "/zjz";

        String photo_name = "/photo_camid" + mCamId + "_num" + mPhotoNum + ".jpg";
        String photo_path = mCamera1TestDir + photo_name;
        Log.d(TAG, "photo_path=" + photo_path);
        try {
            FileOutputStream fos = new FileOutputStream(new File(photo_path));
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "onPictureTaken end");

        // don't forget startPreview!
        mCamera.startPreview();
    }
}
