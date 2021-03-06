package com.example.cameratest;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;

import android.os.Handler;
import android.os.HandlerThread;

import android.content.Context;
import android.app.Activity;

import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.graphics.ImageFormat;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Arrays;
import java.util.ArrayList;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.io.RandomAccessFile;

import android.util.Size;
import android.util.Range;


public class CameraHelper implements MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {
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
    private int mCamId = 0;
    private int mPhotoNum = 0;

    private int mVideoNum = 0;
    private MediaRecorder mRecorder;

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

        // test single request for photo, optimize performance
        try {
            CaptureRequest.Builder singleRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            singleRequest.addTarget(mImageReader.getSurface());
            singleRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            singleRequest.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            singleRequest.set(CaptureRequest.JPEG_ORIENTATION, 90);

            mPreviewSession.stopRepeating();
            mPreviewSession.capture(singleRequest.build(),
                            new CameraCaptureSession.CaptureCallback() {
                                @Override
                                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                                    Log.d(TAG, "onCaptureCompleted! restart preview!");
                                    try {
                                        mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mCameraHandler);
                                    } catch (CameraAccessException e) {
                                        e.printStackTrace();
                                    }
                                }
                            },
                            mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
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

    public int getCameraDeviceCount() {
        try {
            CameraManager camMgr = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            String[] camIdList = camMgr.getCameraIdList();
            Log.d(TAG, "camIdList dump! length=" + camIdList.length);
            int idx = 0;
            for (idx=0; idx<camIdList.length; idx++)
                Log.d(TAG, "    [" + idx + "]:" + camIdList[idx]);
            return camIdList.length;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void openCamera(int camId) {
        Log.d(TAG, "openCamera begin!");
        CameraManager camMgr = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics cc = camMgr.getCameraCharacteristics(Integer.toString(camId));
            StreamConfigurationMap map = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map!=null;
            Log.d(TAG, "list CamDev[" + camId + "] info...");
            Size[] sizeMap = map.getOutputSizes(SurfaceHolder.class);
            for (int i = 0; i < sizeMap.length; i++) {
                Size itemSize = sizeMap[i];
                Log.d(TAG, "    item[" + i + "] width/height=" + itemSize.getWidth() + "," + itemSize.getHeight());
            }

            Range<Integer>[] fpsRanges;
            fpsRanges = cc.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            Log.d(TAG, "fps_range: " + Arrays.toString(fpsRanges));

            mImageReader = ImageReader.newInstance(mPreviewWidth, mPreviewHeight, ImageFormat.YUV_420_888, 1);
            //mImageReader = ImageReader.newInstance(mPreviewWidth, mPreviewHeight, ImageFormat.JPEG, 1);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mCameraHandler);

            camMgr.openCamera(Integer.toString(camId), mCamDevStateCB, mCameraHandler);

            mCamId = camId;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "openCamera end!");
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "onImageAvailable...");
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

                String photoPath = "/mnt/sdcard/photo_camid" + mCamId + "_num" + mPhotoNum + "_" + img.getWidth() + "_" + img.getHeight() + ".yuv";
                Log.d(TAG, "new photoPath=" + photoPath);
                mPhotoNum++;
                try {
                    DataOutputStream yuvFile = new DataOutputStream(new FileOutputStream(photoPath));
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
                //mPreviewBuilder.addTarget(imageSurface);
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

    @Override
    public void onInfo(MediaRecorder recorder, int what, int extra) {
        Log.w(TAG, "MediaRecorder onInfo! what=" + what + ", extra=" + extra);
        switch(what) {
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED: {
                String videoPath = "/mnt/sdcard/video_camid" + mCamId + "_num" + mVideoNum + ".mp4";
                mVideoNum++;
                Log.d(TAG, "new videoPath=" + videoPath);
                RandomAccessFile file;
                try {
                    file = new RandomAccessFile(videoPath, "rw");
                    mRecorder.setNextOutputFile(file.getFD());
                    if (file != null) {
                        file.close();
                    }
                } catch (IOException ioe) {
                    Log.e(TAG, "couldn't set next file:", ioe);
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    public void onError(MediaRecorder recorder, int what, int extra) {
        Log.e(TAG, "MediaRecorder onError! what=" + what + ", extra=" + extra);
    }

    public void configureRecParam() {
        mRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        mRecorder.setVideoEncodingBitRate(6*1000*1000);
        mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mRecorder.setVideoFrameRate(30);
        mRecorder.setVideoSize(mPreviewWidth, mPreviewHeight);

        //mRecorder.setMaxDuration(30*1000); // for loop rec

        String videoPath = "/mnt/sdcard/video_camid" + mCamId + "_num" + mVideoNum + ".mp4";
        Log.d(TAG, "new videoPath=" + videoPath);
        mVideoNum++;

        mRecorder.setOutputFile(videoPath);
        mRecorder.setOnErrorListener(this);
        mRecorder.setOnInfoListener(this);
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startRecorder() {
        Log.d(TAG, "startRecorder begin");

        mRecorder = new MediaRecorder();
        closePreviewSession();
        configureRecParam();

        try {
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        Surface recordSurface = mRecorder.getSurface();
        Surface previewSurface = mSurfaceHolder.getSurface();
        Surface imageSurface = mImageReader.getSurface();
        mPreviewBuilder.addTarget(recordSurface);
        mPreviewBuilder.addTarget(previewSurface);
        //mPreviewBuilder.addTarget(imageSurface);

        try {
            mCameraDevice.createCaptureSession(Arrays.asList(recordSurface, imageSurface, previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    Log.i(TAG, "onConfigured begin!");
                    mPreviewSession = session;
                    try {
                        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                        mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    Log.i(TAG, "onConfigured end!");
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mRecorder.start();

        Log.d(TAG, "startRecorder end");
    }

    public void stopRecorder() {
        if (mRecorder != null) {
            Log.i(TAG, "stopRecorder begin");
            mRecorder.stop();
            mRecorder.reset();
            mRecorder = null;
            Log.i(TAG, "stopRecorder end");
        }

        try {
            Surface previewSurface = mSurfaceHolder.getSurface();
            Surface imageSurface = mImageReader.getSurface();
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.addTarget(previewSurface);
            //mPreviewBuilder.addTarget(imageSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(imageSurface, previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    Log.i(TAG, "onConfigured begin!");
                    mPreviewSession = session;
                    try {
                        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                        mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    Log.i(TAG, "onConfigured end!");
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void closePreviewSession() {
        Log.d(TAG, "closePreviewSession begin");
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
        Log.d(TAG, "closePreviewSession end");
    }

    public void closeCameraDevice() {
        Log.d(TAG, "closeCameraDevice begin");
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        Log.d(TAG, "closeCameraDevice end");
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
