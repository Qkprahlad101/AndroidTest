package com.labters.documentscannerandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.labters.documentscanner.ImageCropActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CameraOverlay extends SurfaceView implements SurfaceHolder.Callback {

    private static final int REQUEST_CODE = 1234;
    Camera camera;
    SurfaceHolder holder;
    ImageView rectangle;
    RelativeLayout relativeLayout;
    private SurfaceView cameraSurfaceView = null;

    public CameraOverlay(Context context, Camera camera) {
        super(context);
        this.camera = camera;
        holder = getHolder();
        holder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Camera.Parameters params = camera.getParameters();
        if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            params.set("orientation", "portrait");
            camera.setDisplayOrientation(90);
            params.setRotation(90);
        } else {
            params.set("orientation", "landscape");
            camera.setDisplayOrientation(0);
            params.setRotation(0);
        }
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        //best quality images ( currently in my phone 3000x4000 resolution )

        /* Usable Parameters
        params = mCamera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        params.setExposureCompensation(0);
        params.setPictureFormat(ImageFormat.JPEG);
        params.setJpegQuality(100);
        params.setRotation(90);
         */
        params.setJpegQuality(100);
        params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        params.setExposureCompensation(0);
        camera.setParameters(params);
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.startPreview();
        //to set rectangle on screen
        rectangle = findViewById(R.id.imageView1);
        rectangle.setImageDrawable(getResources().getDrawable(R.drawable.vector_rectangle));

        relativeLayout = findViewById(R.id.containerImg);

        cameraSurfaceView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setCamFocusMode();
            }
        });

    }

    private void setCamFocusMode(){

        if(camera == null) {
            return;
        }
        /* Set Auto focus */
        Camera.Parameters parameters = camera.getParameters();
        List<String>    focusModes = parameters.getSupportedFocusModes();
        if(focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else
        if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        camera.setParameters(parameters);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
    }


}