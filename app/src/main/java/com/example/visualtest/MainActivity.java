package com.example.visualtest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
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
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.*;


public class MainActivity extends Activity {

    private static final int ImgW = 1920;
    private static final int ImgH = 1080;
    private static final float Cam_FocusLength = 1.0f;

    private CaptureRequest.Builder previewRequestBuilder;
    private HandlerThread handlerThread = new HandlerThread("camera");
    private Handler mCameraHandler;
    private ImageReader mImageReader;
    public static CameraDevice mCameraDevice;
    private SurfaceHolder mHoderCamera;

    private SurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.view);

        mHoderCamera = surfaceView.getHolder();
        initLooper();
        mHoderCamera.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder)
            {
                try {
                    open_camera2();
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }

            }
            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            }
            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
            }
        });
    }

    // ImageReader的监听类，当预览中有新图像时会进入
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            // 此处获取的bitmap 就是预览视频中的每一张图
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            image.close();
            //在子线程进行图像处理，防止预览界面卡顿
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    //先转成Mat
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
                //设置捕获请求为预览，这里还有拍照啊、录像啊等
                previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mImageReader = ImageReader.newInstance(ImgW, ImgH, ImageFormat.JPEG, 2);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mCameraHandler);

                previewRequestBuilder.addTarget(surfaceView.getHolder().getSurface());
                previewRequestBuilder.addTarget(mImageReader.getSurface());
                mCameraDevice.createCaptureSession(Arrays.asList(surfaceView.getHolder().getSurface(), mImageReader.getSurface()), mStateCallBack_session, mCameraHandler);
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
                //自动对焦
//              previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                //设置焦距
                previewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, Cam_FocusLength);
                //设置自动曝光
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

                //拍照
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
        surfaceView.setVisibility(View.VISIBLE);
        if (ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Log.d("Info:", "Camera Permission OK");
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},1);
        }

        Log.d("opencv", "start open");
        //获得所有摄像头的管理者CameraManager
        CameraManager cameraManager = (CameraManager)this.getSystemService(this.CAMERA_SERVICE);
        //获得某个摄像头的特征，支持的参数
        CameraCharacteristics characteristics = null;
        characteristics = cameraManager.getCameraCharacteristics("0");
        //支持的STREAM CONFIGURATION
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        //摄像头支持的预览siz数组
        Size[] mPreviewSize = map.getOutputSizes(SurfaceTexture.class);

        try {
            cameraManager.openCamera("0", mStateCallBack_device, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

}

