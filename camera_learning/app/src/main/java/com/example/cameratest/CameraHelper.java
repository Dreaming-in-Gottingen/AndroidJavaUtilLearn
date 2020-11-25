package com.example.cameratest;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;

import android.os.Handler;
import android.os.HandlerThread;

import android.content.Context;
import android.app.Activity;

import android.media.Image;
import android.media.ImageReader;
import android.graphics.ImageFormat;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Arrays;
import java.util.ArrayList;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;

public class CameraHelper /*implements MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener*/ {
    private final static String TAG = "CameraHelper";

    private SurfaceHolder mSurfaceHolder;
    //private SurfaceTexture mSurfaceTexture;
    private SurfaceView mSurfaceView;

    private ImageReader mImageReader;
    private CameraCaptureSession mPreviewSession;

    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private Handler mCameraHandler;
    private int mPreviewWidth = 1280;
    private int mPreviewHeight = 720;

    private Context mContext;

    private volatile boolean mPhotoFlag = false;
    private int mPhotoId = 0;

    public CameraHelper(Context ctx, SurfaceView sv) {
        Log.d(TAG, "CameraHelper ctor begin!");
        mContext = ctx;

        mSurfaceView = sv;
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setFixedSize(mPreviewWidth, mPreviewHeight);
        mSurfaceHolder.addCallback(mSurfaceHolderCallback);

        HandlerThread handlerThread = new HandlerThread("CameraHelper");
        handlerThread.start();
        mCameraHandler = new Handler(handlerThread.getLooper());
        Log.d(TAG, "CameraHelper ctor end!");
    }

    public void takePhoto() {
        mPhotoFlag = true;
    }

    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
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

    public void openCamera() {
        Log.d(TAG, "openCamera begin!");
        CameraManager camMgr = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics cc = camMgr.getCameraCharacteristics(Integer.toString(CameraCharacteristics.LENS_FACING_FRONT));
            StreamConfigurationMap map = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map!=null;

            mImageReader = ImageReader.newInstance(mPreviewWidth, mPreviewHeight, ImageFormat.YUV_420_888, 1);
            //mImageReader = ImageReader.newInstance(mPreviewWidth, mPreviewHeight, ImageFormat.JPEG, 1);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mCameraHandler);

            camMgr.openCamera(Integer.toString(CameraCharacteristics.LENS_FACING_FRONT), mCamDevStateCB, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "openCamera end!");
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image img = reader.acquireNextImage();
            if (mPhotoFlag == true) {
                mPhotoFlag = false;

                ByteBuffer bbY = img.getPlanes()[0].getBuffer();
                ByteBuffer bbU = img.getPlanes()[1].getBuffer();
                ByteBuffer bbV = img.getPlanes()[2].getBuffer();

                int picSize = bbY.remaining();
                byte[] bArrayNV21 = new byte[picSize*3>>1];
                bbY.get(bArrayNV21, 0, picSize);
                bbU.get(bArrayNV21, picSize+1, bbU.remaining());
                bbV.get(bArrayNV21, picSize, 1);

                String picPath = "/mnt/sdcard/photo_id" + mPhotoId + "_" + img.getWidth() + "_" + img.getHeight() + ".yuv";
                Log.d(TAG, "new photo:" + picPath);
                mPhotoId++;
                try {
                    DataOutputStream yuvFile = new DataOutputStream(new FileOutputStream(picPath));
                    yuvFile.write(bArrayNV21, 0, bArrayNV21.length);
                    yuvFile.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            img.close();
        }
    };

    private CameraDevice.StateCallback mCamDevStateCB = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cd) {
            try {
                Log.i(TAG, "onOpened begin!");
                mCameraDevice = cd;
                Surface previewSurface = mSurfaceHolder.getSurface();
                Surface imageSurface = mImageReader.getSurface();
                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewBuilder.addTarget(previewSurface);
                mPreviewBuilder.addTarget(imageSurface);
                mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, imageSurface), mCapStateCB, mCameraHandler);
                //mCameraDevice.createCaptureSession(Arrays.asList(previewSurface), mCapStateCB, mCameraHandler);
                Log.d(TAG, "camDev=" + cd + ", previewSurface=" + previewSurface);
                Log.i(TAG, "onOpened end!");
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        private CameraCaptureSession.StateCallback mCapStateCB = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                Log.i(TAG, "onConfigured begin!");
                mPreviewSession = session;
                try {
                    mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    CaptureRequest request = mPreviewBuilder.build();
                    mPreviewSession.setRepeatingRequest(request, null, mCameraHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "onConfigured end!");
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {
                Log.d(TAG, "onConfiguredFailed");
            }
        };

        @Override
        public void onDisconnected(CameraDevice camDev) {
            Log.d(TAG, "onDisconnected");
            camDev.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camDev, int err) {
            Log.e(TAG, "onError");
            camDev.close();
            mCameraDevice = null;
        }
    };

    public void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    public void closeCameraDevice() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    public void stopCameraAndPreview() {
        Log.w(TAG, "stopPreview begin!");
        closeCameraDevice();
        closePreviewSession();
        Log.w(TAG, "stopPreview end!");
    }

    public void destroy() {
        Log.w(TAG, "destroy begin!");
        stopCameraAndPreview();
        Log.w(TAG, "destroy end!");
    }
}
