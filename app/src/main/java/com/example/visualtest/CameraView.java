package com.example.visualtest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class CameraView extends SurfaceView implements  SurfaceHolder.Callback{

    private static final int ImgW = 1920;
    private static final int ImgH = 1080;
    private static final float Cam_FocusLength = 1.0f;

    private CaptureRequest.Builder previewRequestBuilder;
    private HandlerThread handlerThread = new HandlerThread("camera");
    private Handler mCameraHandler;
    private ImageReader mImageReader;
    public static CameraDevice mCameraDevice;
    private SurfaceHolder mHoderCamera;

    public CameraView(Context context) {
        super(context);
        getHolder().addCallback(this);
    }

    public CameraView(Context context, AttributeSet set)
    {
        super(context, set);
        getHolder().addCallback(this);
    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            initLooper();
            open_camera2();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    // ImageReader???????????????????????????????????????????????????
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            // ???????????????bitmap ????????????????????????????????????
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            image.close();
            //?????????????????????????????????????????????????????????
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    //?????????Mat
                    Mat imgMat = new OpenCVFrameConverter.ToMat().convert(new AndroidFrameConverter().convert(bitmap));
                    //Log.d("ImgSize:", Integer.toString(imgMat.cols()) + "," + Integer.toString(imgMat.rows()));
                }
            });

        }
    };

    private  CameraDevice.StateCallback mStateCallBack_device = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice)
        {
            try
            {
                Log.d("opencv", "start open2");
                mCameraDevice = cameraDevice;
                //??????????????????????????????????????????????????????????????????
                previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mImageReader = ImageReader.newInstance(ImgW, ImgH, ImageFormat.JPEG, 2);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mCameraHandler);

                previewRequestBuilder.addTarget(getHolder().getSurface());
                previewRequestBuilder.addTarget(mImageReader.getSurface());
                mCameraDevice.createCaptureSession(Arrays.asList(getHolder().getSurface(), mImageReader.getSurface()), mStateCallBack_session, mCameraHandler);
            }
            catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
        }
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            if (cameraDevice != null) {
                cameraDevice.close();
            }
        }
    };

    private CameraCaptureSession.StateCallback mStateCallBack_session = new CameraCaptureSession.StateCallback(){
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            try {
                //????????????
//              previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                //????????????
                previewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, Cam_FocusLength);
                //??????????????????
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

                //??????
                CaptureRequest request = previewRequestBuilder.build();
                //cameraCaptureSession.capture(request, null, mCameraHandler);
                //displaying the camera preview
                cameraCaptureSession.setRepeatingRequest(request, null, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

        }
    };

    private void initLooper()
    {
        handlerThread = new HandlerThread("CAMERA2");
        handlerThread.start();
        mCameraHandler = new Handler(handlerThread.getLooper());
    }

    public void open_camera2() throws CameraAccessException
    {
        this.setVisibility(View.VISIBLE);

        if (ContextCompat.checkSelfPermission(this.getContext().getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Log.d("Info:", "Camera Permission OK");
        } else {
            //ActivityCompat.requestPermissions(this.getContext().ac,new String[]{Manifest.permission.CAMERA},1);
            Log.d("Info:", "Camera Permission Failed");
        }

        Log.d("opencv", "start open");
        //?????????????????????????????????CameraManager
        CameraManager cameraManager = (CameraManager)this.getContext().getSystemService(this.getContext().CAMERA_SERVICE);
        //????????????????????????????????????????????????
        CameraCharacteristics characteristics = null;
        characteristics = cameraManager.getCameraCharacteristics("0");
        //?????????STREAM CONFIGURATION
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        //????????????????????????siz??????
        Size[] mPreviewSize = map.getOutputSizes(SurfaceTexture.class);

        try {
            cameraManager.openCamera("0", mStateCallBack_device, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
