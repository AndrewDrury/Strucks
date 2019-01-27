package com.example.strucks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.location.Location;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import android.Manifest;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class FaceRecognition extends AppCompatActivity {
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static{
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }

    Context context = this;
    MediaPlayer mp;


    private FusedLocationProviderClient mFusedLocationClient;
    private String message;

    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;

    private File file;
    private String path;
    private Image pic;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Handler mBackgrounderHandler;
    private HandlerThread mBackgroundThread;

    private FirebaseAnalytics mFirebaseAnalytics;

    private int initID = -1;
    private int currentID;
    private int period = 5;
    private float rightEyeOpenProb = 1;
    private float leftEyeOpenProb = 1;

    CameraDevice.StateCallback stateCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_recognition);

        textureView = findViewById(R.id.textureView);
        assert textureView != null;
        textureView.setSurfaceTextureListener(texturelistener);



        FirebaseApp.initializeApp(FaceRecognition.this);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        Log.i("testing", "Getting inital face id --------");

        /*while(initID == -1) {
            takePicture();


            Bitmap bmp = BitmapFactory.decodeFile(file.getPath());
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bmp);
            detectFaces(image);
            initID = currentID;

            Log.i("testing", "initID: " + initID);

        }*/

        //logic();
        //Log.i("testing", "log --------");
        Bundle bundle = new Bundle();

        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "Strucks");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Strucks");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

       //Log.i("testing", "done --------");


        Log.i("testing", "currentID: " + currentID);

        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                checkID();
            }
        }, 5, period, TimeUnit.SECONDS);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        final ScheduledExecutorService newService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                alertTest();
            }
        }, 16, 30, TimeUnit.SECONDS);
    }

    private void alertTest() {
        getCurrentLocation();
    }

    private void checkID() {

        Log.i("testing", "Taking Pic --------");
        takePicture();
        Log.i("testing", "path: " + path);

        //Bitmap bmp = BitmapFactory.decodeResource(getResources(), Image);
        //Bitmap bmp = BitmapFactory.decodeFile(path);
        //Bitmap bmp = BitmapFactory.decodeStream((InputStream)new URL(path).getContent());
        //bmp = RotateBitmap(bmp, 180);
        //Bitmap bmp = BitmapFactory.decodeResource(getResources(), path.);


        //FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bmp);
        //FirebaseVisionImage image = FirebaseVisionImage.fromMediaImage(pic, 180);

        if(file.exists()){

            Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            file.delete();

            myBitmap = RotateBitmap(myBitmap, 270);

            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(myBitmap);
            detectFaces(image);

        }
        //Bitmap bmp = BitmapFactory.decodeResource(getResources(), path.);

/*
        Log.i("testing", "yolo2 -------");
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bmp);
        //FirebaseVisionImage image = FirebaseVisionImage.fromMediaImage(pic, 180);
        Log.i("testing", "yolo3 -------");*/



        Log.i("testing", "updatedID: " + currentID);
        Log.i("testing", "initID: " + initID);




        if(currentID != initID && currentID != -1 && initID != -1) {
            Log.i("testing", "INTRUDER ALERT --------");
            getCurrentLocation();
        } else {
            Log.i("testing", "All Good --------");
            if(currentID != -1) {
                initID = currentID;
            }

            if(rightEyeOpenProb < 0.5 && leftEyeOpenProb < 0.5) {
                Handler h = new Handler();
                h.postDelayed(r, 10);

            }
        }


    }
    Runnable r = new Runnable() {
        @Override
        public void run(){
            int loops = 0;
            while(rightEyeOpenProb < 0.5 && leftEyeOpenProb < 0.5) {
                loops++;

                takePicture();
                if(file.exists()) {

                    Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(myBitmap);
                    detectFaces(image);

                }
                Handler h = new Handler();
                h.postDelayed(r, 25);

                //User is tired, play loud sound
                if(loops == 5) {
                    mp = MediaPlayer.create(context, R.raw.song);try {

                        if (mp.isPlaying()) {
                            mp.stop();
                            mp.release();
                            mp = MediaPlayer.create(context, R.raw.song);
                        } mp.start();

                        return;
                    } catch(Exception e) { e.printStackTrace(); }
                }
            }
        }
    };

    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void openCamera() {
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try
        {
            cameraId = manager.getCameraIdList()[1];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map!= null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this,new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId,stateCallBack,null );
        }
        catch(CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    private void takePicture()
    {
        if(cameraDevice == null)

            return;
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try
        {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if(characteristics!= null)
            {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if(jpegSizes != null && jpegSizes.length>0)
            {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            final ImageReader reader = ImageReader.newInstance(width,height,ImageFormat.JPEG,1);
            List<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(reader.getSurface());
            outputSurface.add(new Surface(textureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(rotation));

            path = Environment.getExternalStorageDirectory().getPath()+"/temp.jpg";
            file = new File(path);

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    Image image = null;
                    try
                    {
                        image = reader.acquireLatestImage();
                        pic = image;
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    }
                    catch(FileNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                    }
                    finally {
                        if(image!=null)
                            image.close();
                    }
                }
                private void save(byte[] bytes) throws IOException
                {
                    OutputStream outputStream = null;
                    try {
                        outputStream = new FileOutputStream(file);
                        outputStream.write(bytes);
                    }
                    finally {
                        if(outputStream != null)
                        {
                            outputStream.close();
                        }
                    }

                }
            };
            reader.setOnImageAvailableListener(readerListener,mBackgrounderHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(FaceRecognition.this, "Saved"+file,Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try
                    {
                        cameraCaptureSession.capture(captureBuilder.build(),captureListener,mBackgrounderHandler);

                    }
                    catch (CameraAccessException e)
                    {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            },mBackgrounderHandler);
        }
        catch(CameraAccessException e)
        {
            e.printStackTrace();
        }

    }

    private void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if(cameraDevice==null)
                    {
                        return;
                    }
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(FaceRecognition.this,"Changed",Toast.LENGTH_SHORT).show();
                }
            },null);
        }
        catch(CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if(cameraDevice == null)
            Toast.makeText(this,"Error",Toast.LENGTH_SHORT).show();
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        try
        {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(),null,mBackgrounderHandler);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }
    TextureView.SurfaceTextureListener texturelistener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CAMERA_PERMISSION)
        {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this,"You can't use camera without permission",Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //       if (flag==1) {
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(texturelistener);
        }
        //      }
    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try
        {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgrounderHandler = null;
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgrounderHandler = new Handler(mBackgroundThread.getLooper());

    }


    void logic() {
        URL url = null;
        try {
            String address = "http://genfkd.wpengine.netdna-cdn.com/wp-content/uploads/2018/05/shutterstock_793117360-503x518.jpg";
            url = new URL(address);
        } catch (MalformedURLException e) {}
        /*
        Bitmap bmp = null;
        try {
            //bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            bmp = getBitmapFromURL("http://genfkd.wpengine.netdna-cdn.com/wp-content/uploads/2018/05/shutterstock_793117360-503x518.jpg");
        } catch (java.io.IOException e) {
            System.out.print("Exception 2 reached!!!!");
        }*/
        //Bitmap bmp = getImageBitmapFromUrl(url);


        //imageView.setImageBitmap(bmp);


        //ANALYZE IMAGE
        //----- use this line to convert to bitmap
        //Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.INSERT_PIC); //Converting image to bitmap
        //FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bmp);
        //detectFaces(image);

    }


    //---------
    private void detectFaces(FirebaseVisionImage fireImage) {


        currentID = -1;

        // [START set_detector_options]
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                        //.setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .setMinFaceSize(0.05f)
                        .enableTracking()
                        .build();
        // [END set_detector_options]

        // [START get_detector]git add -
        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);
        // [END get_detector]

        // [START run_detector]
        Task<List<FirebaseVisionFace>> result =
                detector.detectInImage(fireImage)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionFace>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionFace> faces) {
                                        // Task completed successfully
                                        // [START_EXCLUDE]
                                        // [START get_face_info]
                                        for (FirebaseVisionFace face : faces) {
                                            Rect bounds = face.getBoundingBox();
                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

                                            Log.i("testing","bounds " + bounds.toString());
                                            Log.i("testing","rotY " + rotY);
                                            Log.i("testing","rotZ " + rotZ);

                                            // If classification was enabled:
                                            if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                                rightEyeOpenProb = face.getRightEyeOpenProbability();

                                                Log.i("testing","RightEyeOpenProb: " + rightEyeOpenProb);
                                            }
                                            if (face.getLeftEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                                leftEyeOpenProb = face.getLeftEyeOpenProbability();

                                                Log.i("testing","leftEyeOpenProb: " + leftEyeOpenProb);
                                            }

                                            // If face tracking was enabled:
                                            if (face.getTrackingId() != FirebaseVisionFace.INVALID_ID) {
                                                currentID = face.getTrackingId();

                                                Log.i("testing","faceID: " + currentID);

                                            }
                                        }
                                        // [END get_face_info]
                                        // [END_EXCLUDE]
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
        try {
            detector.close();
            Log.i("testing","released resources: " + currentID);

        } catch (java.io.IOException e) {

        }

        // [END run_detector]
    }

    private void processFaceList(List<FirebaseVisionFace> faces) {
        // [START mlkit_face_list]
        for (FirebaseVisionFace face : faces) {
            Rect bounds = face.getBoundingBox();
            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
            // nose available):
            FirebaseVisionFaceLandmark leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR);
            if (leftEar != null) {
                FirebaseVisionPoint leftEarPos = leftEar.getPosition();
            }

            // If contour detection was enabled:
            List<FirebaseVisionPoint> leftEyeContour =
                    face.getContour(FirebaseVisionFaceContour.LEFT_EYE).getPoints();
            List<FirebaseVisionPoint> upperLipBottomContour =
                    face.getContour(FirebaseVisionFaceContour.UPPER_LIP_BOTTOM).getPoints();

            if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                float rightEyeOpenProb = face.getRightEyeOpenProbability();
            }

            // If face tracking was enabled:
            if (face.getTrackingId() != FirebaseVisionFace.INVALID_ID) {
                int id = face.getTrackingId();
            }
        }
    }

    public static Bitmap getImageBitmapFromUrl(URL url)
    {
        Bitmap bm = null;
        try {
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            if(conn.getResponseCode() != 200)
            {
                return bm;
            }
            conn.connect();
            InputStream is = conn.getInputStream();

            BufferedInputStream bis = new BufferedInputStream(is);
            try
            {
                bm = BitmapFactory.decodeStream(bis);
            }
            catch(OutOfMemoryError ex)
            {
                bm = null;
            }
            bis.close();
            is.close();
        } catch (Exception e) {}
        return bm;
    }

    /////////////////////
    //LOCATION STUFF
    public void sendSMS() {
        SmsManager smsManager = SmsManager.getDefault();
        Log.i("log", "SMS is called");
        smsManager.sendTextMessage(new String("2263486765"), null, message, null, null);
        Toast.makeText(getApplicationContext(), "SMS sent.",
                Toast.LENGTH_LONG).show();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            double longitude = location.getLongitude();
                            double latitude = location.getLatitude();
                            message= String.format("ALERT! Your Truck has been hijacked!\nDriver:Steven\nVIN:4V4MC9EH7CN559763\nLocation:https://www.google.com/maps/place/%s,%s", latitude, longitude);

                            Log.i("log", message);
                            if(message != null){
                                sendSMS();
                            }

                        }
                        else{
                            Log.i("log", "Location is null");
                        }
                    }
                });
    }

    public void emergencyClick(View view) {
        getCurrentLocation();
    }
}
