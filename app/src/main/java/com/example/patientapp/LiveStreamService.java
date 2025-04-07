package com.example.patientapp;
//
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.os.Handler;
//import android.os.Looper;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//import androidx.camera.core.ImageCapture;
//import androidx.camera.core.ImageCaptureException;
//import androidx.camera.core.ImageProxy;
//import androidx.camera.lifecycle.ProcessCameraProvider;
//import androidx.lifecycle.LifecycleOwner;
//
//import com.google.common.util.concurrent.ListenableFuture;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.StorageReference;
//
//import java.io.ByteArrayOutputStream;
//import java.nio.ByteBuffer;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class LiveStreamService {
//    private ImageCapture imageCapture;
//    private final Handler handler = new Handler(Looper.getMainLooper());
//    private final ExecutorService executorService;
//    private final DatabaseReference databaseReference;
//    private final StorageReference storageReference;
//    private final String patientEmail;
//
//    public LiveStreamService(LifecycleOwner lifecycleOwner) {
//        executorService = Executors.newSingleThreadExecutor();
//        FirebaseAuth auth = FirebaseAuth.getInstance();
//        if (auth.getCurrentUser() != null && auth.getCurrentUser().getEmail() != null) {
//            patientEmail = auth.getCurrentUser().getEmail().replace(".", "_");
//        } else {
//            Log.e("LiveStreamService", "User not authenticated");
//            throw new IllegalStateException("User must be authenticated to use LiveStreamService");
//        }
//
//        databaseReference = FirebaseDatabase.getInstance().getReference("patients")
//                .child(patientEmail).child("liveStream");
//        storageReference = FirebaseStorage.getInstance().getReference("live_streams")
//                .child(patientEmail + ".jpg");
//
//        startCamera(lifecycleOwner);
//        startUploadingImages();
//    }
//
//    private void startCamera(LifecycleOwner lifecycleOwner) {
//        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(
//                ((androidx.fragment.app.FragmentActivity) lifecycleOwner).getApplicationContext()
//        );
//
//        cameraProviderFuture.addListener(() -> {
//            try {
//                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
//                imageCapture = new ImageCapture.Builder()
//                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
//                        .build();
//                cameraProvider.bindToLifecycle(
//                        lifecycleOwner,
//                        androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA,
//                        imageCapture
//                );
//            } catch (Exception e) {
//                Log.e("CameraError", "Failed to start camera", e);
//            }
//        }, executorService);
//    }
//
//    private void startUploadingImages() {
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                captureAndUploadImage();
//                handler.postDelayed(this, 5000); // Capture every 5 seconds
//            }
//        }, 5000);
//    }
//
//    private void captureAndUploadImage() {
//        if (imageCapture == null) return;
//
//        imageCapture.takePicture(executorService, new ImageCapture.OnImageCapturedCallback() {
//            @Override
//            public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
//                Bitmap bitmap = convertImageProxyToBitmap(imageProxy);
//                imageProxy.close();
//
//                if (bitmap != null) {
//                    uploadImage(bitmap);
//                }
//            }
//
//            @Override
//            public void onError(@NonNull ImageCaptureException exception) {
//                Log.e("ImageCaptureError", "Failed to capture image", exception);
//            }
//        });
//    }
//
//    private void uploadImage(Bitmap bitmap) {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
//        byte[] data = baos.toByteArray();
//
//        storageReference.putBytes(data)
//                .addOnSuccessListener(taskSnapshot ->
//                    storageReference.getDownloadUrl()
//                            .addOnSuccessListener(uri ->
//                                databaseReference.setValue(uri.toString())
//                            )
//                )
//                .addOnFailureListener(e ->
//                    Log.e("FirebaseUploadError", "Failed to upload image", e)
//                );
//    }
//
//    private Bitmap convertImageProxyToBitmap(ImageProxy imageProxy) {
//        ImageProxy.PlaneProxy plane = imageProxy.getPlanes()[0];
//        ByteBuffer buffer = plane.getBuffer();
//        byte[] bytes = new byte[buffer.remaining()];
//        buffer.get(bytes);
//        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//    }
//
//    public void cleanup() {
//        handler.removeCallbacksAndMessages(null);
//        executorService.shutdown();
//    }
//}


import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LiveStreamService extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private static final String TAG = "PatientCameraApp";
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final String PATIENT_ID = "patient_unique_id"; // Use actual patient ID
    private static final int UPLOAD_INTERVAL_MS = 1000; // 1 second between uploads

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private TextView statusTextView;
    private DatabaseReference firebaseRef;
    private Executor executor;
    private Timer uploadTimer;
    private boolean isStreaming = false;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        surfaceView = findViewById(R.id.surface_view);
        statusTextView = findViewById(R.id.status_text);

        // Initialize Firebase
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("user_email", null);

        if (userEmail != null) {
            // Format email for Firebase path (replace dots with commas)
            String patientId = userEmail.replace(".", ",");

            // Initialize Firebase reference using the email as patient ID
            firebaseRef = FirebaseDatabase.getInstance().getReference()
                    .child("patient_frames").child(patientId);
        } else {
            // Handle the case where email is not found
            updateStatus("User email not found. Please log in again.");
            // Consider redirecting to login screen
        }

        // Initialize executor for background tasks
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(getMainLooper());

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        } else {
            setupCamera();
        }
    }

    private void setupCamera() {
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        // Set status
        updateStatus("Initializing camera...");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            camera.setPreviewDisplay(holder);
            camera.setPreviewCallback(this);

            // Set appropriate preview size
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewSize(640, 480); // Lower resolution for efficiency
            camera.setParameters(parameters);

            camera.startPreview();
            startStreaming();
            updateStatus("Camera started - Streaming active");
        } catch (IOException e) {
            Log.e(TAG, "Error setting camera preview: " + e.getMessage());
            updateStatus("Camera error: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (surfaceHolder.getSurface() == null) {
            return;
        }

        try {
            camera.stopPreview();
        } catch (Exception e) {
            // Ignore: tried to stop a non-existent preview
        }

        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
            Log.e(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopStreaming();
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // This runs for every frame - we'll process frames at intervals
        // The actual processing and upload happens in the timer
    }

    private void startStreaming() {
        if (isStreaming) return;

        isStreaming = true;
        uploadTimer = new Timer();
        uploadTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (camera != null) {
                    camera.setOneShotPreviewCallback((data, cam) -> {
                        uploadFrameToFirebase(data, cam);
                    });
                }
            }
        }, 0, UPLOAD_INTERVAL_MS);

        // Update streaming status in Firebase
        firebaseRef.child("streaming_active").setValue(true);
    }

    private void stopStreaming() {
        isStreaming = false;
        if (uploadTimer != null) {
            uploadTimer.cancel();
            uploadTimer = null;
        }

        // Update streaming status in Firebase
        firebaseRef.child("streaming_active").setValue(false);
    }

    private void uploadFrameToFirebase(byte[] data, Camera camera) {
        executor.execute(() -> {
            try {
                // Convert YUV data to JPEG
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = parameters.getPreviewSize();
                YuvImage image = new YuvImage(data, ImageFormat.NV21,
                        size.width, size.height, null);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, size.width, size.height), 50, out);
                byte[] imageBytes = out.toByteArray();

                // Convert to Base64 string
                String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                // Create frame data with timestamp
                Map<String, Object> frameData = new HashMap<>();
                frameData.put("image", base64Image);
                frameData.put("timestamp", System.currentTimeMillis());

                // Upload to Firebase
                firebaseRef.child("current_frame").setValue(frameData)
                        .addOnSuccessListener(aVoid -> {
                            mainHandler.post(() -> updateStatus("Frame uploaded successfully"));
                        })
                        .addOnFailureListener(e -> {
                            mainHandler.post(() -> updateStatus("Upload failed: " + e.getMessage()));
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error processing frame: " + e.getMessage());
                mainHandler.post(() -> updateStatus("Frame processing error"));
            }
        });
    }

    private void updateStatus(String message) {
        runOnUiThread(() -> {
            statusTextView.setText(message);
            Log.d(TAG, message);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera();
            } else {
                updateStatus("Camera permission denied");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopStreaming();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (camera != null && !isStreaming) {
            startStreaming();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopStreaming();
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }
}
