package com.example.visualtest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
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
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.*;
import org.w3c.dom.Text;


public class MainActivity extends Activity {

    private IMUDataGather mImtDataGather;
    private TextView[] acc_TextViews;
    private TextView[] gyro_TextViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        acc_TextViews = new TextView[]{(TextView) findViewById(R.id.textView_accel_0),
                (TextView) findViewById(R.id.textView_accel_1),
                (TextView) findViewById(R.id.textView_accel_2)};

        gyro_TextViews = new TextView[]{ (TextView) findViewById(R.id.textView_gyro_0),
                (TextView) findViewById(R.id.textView_gyro_1),
                (TextView) findViewById(R.id.textView_gyro_2),
                (TextView) findViewById(R.id.textView_gyro_3),
                (TextView) findViewById(R.id.textView_gyro_4),
                (TextView) findViewById(R.id.textView_gyro_5)};

        mImtDataGather = new IMUDataGather(MainActivity.this);
        mImtDataGather.register();

        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable,1000);
    }

    @Override
    protected void onDestroy()
    {
        handler.removeCallbacks(runnable);
        super.onDestroy();
    }

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        public void run () {
            //Log.d("onTime:", "go");
            String sGyro = IMUDataGather.getGyroValue();
            if(sGyro.length() > 0)
            {
                String[] gyros = sGyro.split(",");
                if(gyros.length == 6)
                {
                    for (int i=0; i< 6; i++) {
                        gyro_TextViews[i].setText(gyros[i]);
                    }
                }
            }

            String sAcc = IMUDataGather.getAccValue();
            if(sAcc.length() > 0) {
                String[] accs = sAcc.split(",");
                if(accs.length == 6)
                {
                    for (int i=0; i< 3; i++) {
                        acc_TextViews[i].setText(accs[i]);
                    }
                }
            }
            handler.postDelayed(this,50);
        }
    };
}

