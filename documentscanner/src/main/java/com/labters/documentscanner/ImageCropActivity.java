/*
 * *
 *  * Created by Ali YÜCE on 3/2/20 11:18 PM
 *  * https://github.com/mayuce/
 *  * Copyright (c) 2020 . All rights reserved.
 *  * Last modified 3/2/20 11:10 PM
 *
 */

package com.labters.documentscanner;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;


import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.UploadProgressListener;
import com.google.android.gms.security.ProviderInstaller;
import com.labters.documentscanner.base.CropperErrorType;
import com.labters.documentscanner.base.DocumentScanActivity;
import com.labters.documentscanner.helpers.ScannerConstants;
import com.labters.documentscanner.libraries.PolygonView;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ImageCropActivity extends DocumentScanActivity {
    private static final String BASE_URL = "https://192.168.238.154:8000/AndroidImage/";
    private FrameLayout holderImageCrop;
    private ImageView imageView;
    private PolygonView polygonView;
    private boolean isInverted, isMagic, isSharp;
    private ProgressBar progressBar;
    private Bitmap cropImage;

    Interpreter tflite;
    private  int imageSizeX;
    private  int imageSizeY;
    private  TensorBuffer outputProbabilityBuffer;
    private  TensorProcessor probabilityProcessor;
    private MappedByteBuffer tfliteModel;
    private TensorImage inputImageBuffer;
    private static final float IMAGE_MEAN = 0.0f;
    private static final float IMAGE_STD = 1.0f;
    private static final float PROBABILITY_MEAN = 0.0f;
    private static final float PROBABILITY_STD = 255.0f;
    private Bitmap bitmap;
    private List<String> labels;
    String ba1;
    String filePath;


    private OnClickListener btnImageEnhanceClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            showProgressBar();
            disposable.add(
                    Observable.fromCallable(() -> {
                        cropImage = getCroppedImage();
                        if (cropImage == null)
                            return false;
                        return false;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
                                hideProgressBar();
                                if (cropImage != null) {
                                    ScannerConstants.selectedImageBitmap = cropImage;
                                    filePath = saveToInternalStorage(cropImage);
                                    //UploadFile(filePath);
                                    doFileUpload(filePath);
                                    setResult(RESULT_OK);
                                    //doInference();
                                    finish();
                                }
                            })
            );
        }
    };
    private OnClickListener btnRebase = v -> {
        cropImage = ScannerConstants.selectedImageBitmap.copy(ScannerConstants.selectedImageBitmap.getConfig(), true);
        isInverted = false;
        isMagic = false;
        isSharp = false;
        startCropping();
    };
    private OnClickListener btnCloseClick = v -> finish();
    private OnClickListener btnInvertColor = new OnClickListener() {
        @Override
        public void onClick(View v) {
            showProgressBar();
            disposable.add(
                    Observable.fromCallable(() -> {
                        invertColor();
                        return false;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
                                hideProgressBar();
                                Bitmap scaledBitmap = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                                imageView.setImageBitmap(scaledBitmap);
                            })
            );
        }
    };

    public void onBackPressed() {
    }

    private OnClickListener btnMagicColor = new OnClickListener() {
        @Override
        public void onClick(View v) {
            showProgressBar();
            disposable.add(
                    Observable.fromCallable(() -> {
                        magicColor();
                        return false;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
                                hideProgressBar();
                                Bitmap scaledBitmap = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                                imageView.setImageBitmap(scaledBitmap);
                            })
            );
        }
    };
/*
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor assetFileDescriptor = this.getAssets().openFd("mobilenet_model.tflite");
        FileInputStream fileInputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        long startOffset = assetFileDescriptor.getStartOffset();
        long length = assetFileDescriptor.getLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, length);
    }

    public void doInference() {
        int imageTensorIndex = 0;
        int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape(); // {1, height, width, 3}
        imageSizeY = imageShape[1];
        imageSizeX = imageShape[2];
        DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();

        int probabilityTensorIndex = 0;
        int[] probabilityShape =
                tflite.getOutputTensor(probabilityTensorIndex).shape(); // {1, NUM_CLASSES}
        DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

        inputImageBuffer = new TensorImage(imageDataType);
        outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
        probabilityProcessor = new TensorProcessor.Builder().add(getPostprocessNormalizeOp()).build();

        inputImageBuffer = loadImage(bitmap);

        tflite.run(inputImageBuffer.getBuffer(),outputProbabilityBuffer.getBuffer().rewind());
        showresult();
    }

    private TensorImage loadImage(final Bitmap bitmap) {
        // Loads bitmap into a TensorImage.
        inputImageBuffer.load(bitmap);

        // Creates processor for the TensorImage.
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        // TODO(b/143564309): Fuse ops inside ImageProcessor.
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                        .add(getPreprocessNormalizeOp())
                        .build();
        return imageProcessor.process(inputImageBuffer);
    }
    private TensorOperator getPreprocessNormalizeOp() {
        return new NormalizeOp(IMAGE_MEAN, IMAGE_STD);
    }
    private TensorOperator getPostprocessNormalizeOp(){
        return new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD);
    }

    private void showresult(){

        try{
            labels = FileUtil.loadLabels(this,"newdict.txt");
        }catch (Exception e){
            e.printStackTrace();
        }
        Map<String, Float> labeledProbability =
                new TensorLabel(labels, probabilityProcessor.process(outputProbabilityBuffer))
                        .getMapWithFloatValue();
        float maxValueInMap =(Collections.max(labeledProbability.values()));

        for (Map.Entry<String, Float> entry : labeledProbability.entrySet()) {
            if (entry.getValue()==maxValueInMap) {
                //classitext.setText(entry.getKey());
                Toast.makeText(this, "NIMA Score: "+entry.getKey(), Toast.LENGTH_SHORT).show();
            }
        }
    }

 */

    private void magicColor() {
        if (!isMagic) {
            Bitmap bmpMonochrome = Bitmap.createBitmap(cropImage.getWidth(), cropImage.getHeight(), Bitmap.Config.ARGB_8888);
            float[] colorTransform = {
                    85, 85, 85, 0, -128 * 255,
                    85, 85, 85, 0, -128 * 255,
                    85, 85, 85, 0, -128 * 255,
                    0, 0, 0, 1, 0};

            Canvas canvas = new Canvas(bmpMonochrome);
            ColorMatrix ma = new ColorMatrix();
            ma.setSaturation(0f); //Remove Colour
            ma.set(colorTransform);
            Paint paint = new Paint();
            paint.setColorFilter(new ColorMatrixColorFilter(ma));

            canvas.drawBitmap(cropImage, 0, 0, paint);
            cropImage = bmpMonochrome.copy(bmpMonochrome.getConfig(), true);
        } else {
            cropImage = cropImage.copy(cropImage.getConfig(), true);
        }
        isMagic = !isMagic;
    }

    private OnClickListener btnSharp = new OnClickListener() {
        @Override
        public void onClick(View v) {
            showProgressBar();
            disposable.add(
                    Observable.fromCallable(() -> {
                        sharpen();
                        return false;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
                                hideProgressBar();
                                Bitmap scaledBitmap = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                                imageView.setImageBitmap(scaledBitmap);
                            })
            );
        }
    };

    private void sharpen() {
        if (!isSharp) {
            try {
                Bitmap bmpMonochrome = Bitmap.createBitmap(cropImage.getWidth(), cropImage.getHeight(), Bitmap.Config.ARGB_8888);
                Mat source = new Mat();
                Utils.bitmapToMat(bmpMonochrome, source);
                Mat destination = new Mat(source.rows(), source.cols(), source.type());

                // filtering
                Imgproc.GaussianBlur(source, destination, new Size(0, 0), 10);
                Core.addWeighted(source, 1.5, destination, -0.5, 0, destination);
                Utils.bitmapToMat(bmpMonochrome, destination);
                //               cropImage = bmpMonochrome.copy(bmpMonochrome.getConfig(), true);
                isSharp = !isSharp;
            } catch (Exception e) {
            }
        }
    }


    private OnClickListener onRotateClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            showProgressBar();
            disposable.add(
                    Observable.fromCallable(() -> {
                        if (isInverted)
                            invertColor();
                        cropImage = rotateBitmap(cropImage, 90);
                        return false;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
                                hideProgressBar();
                                startCropping();
                            })
            );
        }
    };

    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);
        cropImage = ScannerConstants.selectedImageBitmap;
        isInverted = false;
        isMagic = false;
        isSharp = false;
        if (ScannerConstants.selectedImageBitmap != null)
            initView();
        else {
            Toast.makeText(this, ScannerConstants.imageError, Toast.LENGTH_LONG).show();
            finish();
        }

//        AndroidNetworking.initialize(getApplicationContext());

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            try {
//                ProviderInstaller.installIfNeeded(this);
//            } catch (Exception ignored) {
//                Log.e("EXCEPTIONNN",ignored.getMessage()+" ");
//            }

/*
        try {
            tflite = new Interpreter(loadModelFile(), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
 
 */

    }


    public void doFileUpload(String path){
        path = "D:\\Turtlemint\\Github\\New folder\\QIS_Android\\documentscanner\\src\\main\\res\\mipmap-hdpi\\magic.png";
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        DataInputStream inStream = null;
        String lineEnd = "\r\n";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1*1024*1024;
        String urlString = BASE_URL;   // server ip
        try
        {
            //------------------ CLIENT REQUEST
            FileInputStream fileInputStream = new FileInputStream(new File(path) );
            // open a URL connection to the Servlet
            URL url = new URL(urlString);
            // Open a HTTP connection to the URL
            conn = (HttpURLConnection) url.openConnection();
            // Allow Inputs
            conn.setDoInput(true);
            // Allow Outputs
            conn.setDoOutput(true);
            // Don't use a cached copy.
            conn.setUseCaches(false);
            // Use a post method.
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="+"    ");
            dos = new DataOutputStream( conn.getOutputStream() );
            dos.writeBytes(lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + path + "\"" + lineEnd);
            dos.writeBytes(lineEnd);

            // create a buffer of maximum size
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // read file and write it into form...
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            while (bytesRead > 0)
            {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            // send multipart form data necesssary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(lineEnd);

            // close streams
            Log.e("Debug","File is written");
            fileInputStream.close();
            dos.flush();
            dos.close();
        }
        catch (MalformedURLException ex)
        {
            Log.e("Debug", "error: " + ex.getMessage(), ex);
        }
        catch (IOException ioe)
        {
            Log.e("Debug", "error: " + ioe.getMessage(), ioe);
        }

        //------------------ read the SERVER RESPONSE
        try {
            inStream = new DataInputStream ( conn.getInputStream() );
            String str;
            while (( str = inStream.readLine()) != null)
            {
                Log.e("Debug","Server Response "+str);
            }
            inStream.close();
        }
        catch (IOException ioex){
            Log.e("Debug", "error: " + ioex.getMessage(), ioex);
        }
    }

//    public void UploadFile(String filePath) {
//        File file = new File(filePath);
//
//        AndroidNetworking.upload(BASE_URL)
//                .addMultipartFile("image", file)
//                .setPriority(Priority.HIGH)
//                .build()
//                .setUploadProgressListener(new UploadProgressListener() {
//                    @Override
//                    public void onProgress(long bytesUploaded, long totalBytes) {
//                        // do anything with progress
//                    }
//                })
//                .getAsJSONObject(new JSONObjectRequestListener() {
//                    @Override
//                    public void onResponse(JSONObject response) {
//                        // do anything with response
//                        Toast.makeText(ImageCropActivity.this, "Success: "+response, Toast.LENGTH_SHORT).show();
//                    }
//                    @Override
//                    public void onError(ANError error) {
//                        // handle error
//                        Toast.makeText(ImageCropActivity.this, "Error Occured: "+error, Toast.LENGTH_SHORT).show();
//                    }
//                });
//
//
//    }


    @Override
    protected FrameLayout getHolderImageCrop() {
        return holderImageCrop;
    }

    @Override
    protected ImageView getImageView() {
        return imageView;
    }

    @Override
    protected PolygonView getPolygonView() {
        return polygonView;
    }

    @Override
    protected void showProgressBar() {
        RelativeLayout rlContainer = findViewById(R.id.rlContainer);
        setViewInteract(rlContainer, false);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void hideProgressBar() {
        RelativeLayout rlContainer = findViewById(R.id.rlContainer);
        setViewInteract(rlContainer, true);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void showError(CropperErrorType errorType) {
        switch (errorType) {
            case CROP_ERROR:
                Toast.makeText(this, ScannerConstants.cropError, Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    protected Bitmap getBitmapImage() {
        return cropImage;
    }

    private void setViewInteract(View view, boolean canDo) {
        view.setEnabled(canDo);
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setViewInteract(((ViewGroup) view).getChildAt(i), canDo);
            }
        }
    }

    private void initView() {
        Button btnImageCrop = findViewById(R.id.btnImageCrop);
        Button btnClose = findViewById(R.id.btnClose);
        holderImageCrop = findViewById(R.id.holderImageCrop);
        imageView = findViewById(R.id.imageView);
        ImageView ivRotate = findViewById(R.id.ivRotate);
        ImageView ivInvert = findViewById(R.id.ivInvert);
        ImageView ivRebase = findViewById(R.id.ivRebase);
        ImageView ivMagicColor = findViewById(R.id.ivmagic);
        ImageView ivSharpen = findViewById(R.id.ivsharp);
        btnImageCrop.setText(ScannerConstants.cropText);
        btnClose.setText(ScannerConstants.backText);
        polygonView = findViewById(R.id.polygonView);
        progressBar = findViewById(R.id.progressBar);
        if (progressBar.getIndeterminateDrawable() != null && ScannerConstants.progressColor != null)
            progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor(ScannerConstants.progressColor), android.graphics.PorterDuff.Mode.MULTIPLY);
        else if (progressBar.getProgressDrawable() != null && ScannerConstants.progressColor != null)
            progressBar.getProgressDrawable().setColorFilter(Color.parseColor(ScannerConstants.progressColor), android.graphics.PorterDuff.Mode.MULTIPLY);
        btnImageCrop.setBackgroundColor(Color.parseColor(ScannerConstants.cropColor));
        btnClose.setBackgroundColor(Color.parseColor(ScannerConstants.backColor));
        btnImageCrop.setOnClickListener(btnImageEnhanceClick);
        btnClose.setOnClickListener(btnCloseClick);
        ivRotate.setOnClickListener(onRotateClick);
        ivInvert.setOnClickListener(btnInvertColor);
        ivRebase.setOnClickListener(btnRebase);
        ivMagicColor.setOnClickListener(btnMagicColor);
        ivSharpen.setOnClickListener(btnSharp);
        startCropping();
    }

    private void invertColor() {
        if (!isInverted) {
            Bitmap bmpMonochrome = Bitmap.createBitmap(cropImage.getWidth(), cropImage.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmpMonochrome);
            ColorMatrix ma = new ColorMatrix();
            ma.setSaturation(0);
            Paint paint = new Paint();
            paint.setColorFilter(new ColorMatrixColorFilter(ma));
            canvas.drawBitmap(cropImage, 0, 0, paint);
            cropImage = bmpMonochrome.copy(bmpMonochrome.getConfig(), true);
        } else {
            cropImage = cropImage.copy(cropImage.getConfig(), true);
        }
        isInverted = !isInverted;
    }

    private String saveToInternalStorage(Bitmap bitmapImage) {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root + "/TurtleScans");
        if (!myDir.exists())
            myDir.mkdirs();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "turtledoc_" + timeStamp + ".jpg";
        File mypath = new File(myDir, imageFileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return myDir.getAbsolutePath()+imageFileName;
    }

//    private void upload() {
//        // Image location URL
//        // Image
//        ByteArrayOutputStream bao = new ByteArrayOutputStream();
//        cropImage.compress(Bitmap.CompressFormat.JPEG, 90, bao);
//        byte[] ba = bao.toByteArray();
//        ba1 = Base64.encodeToString(ba, Base64.DEFAULT);
//
//        Log.e("base64", "-----" + ba1);
//
//        // Upload image to server
//        new uploadToServer().execute();
//
//    }
//
//    public class uploadToServer extends AsyncTask<Void, Void, String> {
//
//        private ProgressDialog pd = new ProgressDialog(ImageCropActivity.this);
//        protected void onPreExecute() {
//            super.onPreExecute();
//            pd.setMessage("Wait image uploading!");
//            pd.show();
//        }
//    protected String doInBackground(Void... params) {
//
//        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
//        nameValuePairs.add(new BasicNameValuePair("base64", ba1));
//        nameValuePairs.add(new BasicNameValuePair("ImageName", System.currentTimeMillis() + ".jpg"));
//        try {
//            HttpClient httpclient = new DefaultHttpClient();
//            HttpPost httppost = new HttpPost(URL);
//            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
//            HttpResponse response = httpclient.execute(httppost);
//            String st = EntityUtils.toString(response.getEntity());
//            Log.v("log_tag", "In the try Loop" + st);
//
//        } catch (Exception e) {
//            Log.v("log_tag", "Error in http connection " + e.toString());
//        }
//        return "Success";
//
//    }
//
//    protected void onPostExecute(String result) {
//        super.onPostExecute(result);
//        pd.hide();
//        pd.dismiss();
//    }
//}

}
