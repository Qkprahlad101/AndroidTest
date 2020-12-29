package com.labters.documentscannerandroid;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.labters.documentscanner.ImageCropActivity;
import com.labters.documentscanner.helpers.ScannerConstants;
import com.labters.documentscannerandroid.CameraOverlay;
import com.labters.documentscannerandroid.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class Custom_CameraActivity extends Activity {

    Camera camera;
    CameraOverlay cameraOverlay;
    FrameLayout frameLayout;
    Button btnCapture;
    CameraManager cameraManager;
    Button flash;
    Integer i=0;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_overlay);
        frameLayout = findViewById(R.id.customcamframe);
        camera = camera.open();
        cameraOverlay = new CameraOverlay(this, camera);
        frameLayout.addView(cameraOverlay);
        btnCapture = findViewById(R.id.capturebtn);
        flash = findViewById(R.id.flashbtn);
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCapture(v);
            }
        });

    }


    PictureCallback cameraPictureCallbackJpeg = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            Bitmap cameraBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            int wid = cameraBitmap.getWidth();
            int hgt = cameraBitmap.getHeight();
            if (wid > hgt) {
                int t = wid;
                wid = hgt;
                hgt = t;
                cameraBitmap = rotateImage(cameraBitmap, 90);
            }
            Bitmap output = Bitmap.createBitmap(wid, hgt, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            canvas.drawBitmap(cameraBitmap, 0f, 0f, null);
            ScannerConstants.selectedImageBitmap = output;
            Intent intent = new Intent(Custom_CameraActivity.this, ImageCropActivity.class);
            setResult(RESULT_OK);
            startActivityForResult(intent, 1234);
            finish();
        }
    };

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public void onCapture(View v) {
        if (camera != null) {
            camera.takePicture(null, null, cameraPictureCallbackJpeg);
        }
    }


}