package com.example.patientapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlinkDetectionHelper {

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private FaceDetector faceDetector;
    private boolean blinkDetected = false;
    private boolean blinkCooldown = false;
    private Handler handler = new Handler();
    private Context context;
    private BlinkListener blinkListener;

    private DatabaseReference firebaseRef;
    private Handler frameHandler = new Handler();
    private Runnable frameUploadRunnable;
    private long uploadInterval = 1000; // 1 second
    private Bitmap lastProcessedFrame;

    public interface BlinkListener {
        void onBlinkDetected();
    }

    public BlinkDetectionHelper(Context context, PreviewView previewView, BlinkListener blinkListener, String patientId) {
        this.context = context;
        this.previewView = previewView;
        this.blinkListener = blinkListener;
        this.cameraExecutor = Executors.newSingleThreadExecutor();
        if (patientId != null) {
            firebaseRef = FirebaseDatabase.getInstance().getReference()
                    .child("patient_frames").child(patientId);
        }
        setupFaceDetector();
        setupFrameUploader();
    }
    private void setupFrameUploader() {
        frameUploadRunnable = new Runnable() {
            @Override
            public void run() {
                if (lastProcessedFrame != null) {
                    uploadFrameToFirebase(lastProcessedFrame);
                }
                frameHandler.postDelayed(this, uploadInterval);
            }
        };
        frameHandler.postDelayed(frameUploadRunnable, uploadInterval);
    }
    private void uploadFrameToFirebase(Bitmap frame) {
        if (firebaseRef == null) return;

        // Compress frame to JPEG (reduce size)
        firebaseRef.child("streaming_active").setValue(true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        frame.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] imageBytes = baos.toByteArray();
        String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        // Prepare Firebase data
        Map<String, Object> frameData = new HashMap<>();
        frameData.put("image", base64Image);
        frameData.put("timestamp", System.currentTimeMillis());

        // Upload
//        firebaseRef.child("frames").push().setValue(frameData)
//                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Frame uploaded"))
//                .addOnFailureListener(e -> Log.e("Firebase", "Upload failed", e));
        firebaseRef.child("current_frame").setValue(frameData)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Frame uploaded"))
                .addOnFailureListener(e -> Log.e("Firebase", "Upload failed", e));
    }
    public static Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        @SuppressWarnings("UnsafeOptInUsageError")
        Image image = imageProxy.getImage();
        if (image == null) return null;

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        image.close();
        return bitmap;
    }

    private void setupFaceDetector() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
        faceDetector = FaceDetection.getClient(options);
    }

    public void startCamera(LifecycleOwner lifecycleOwner) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                Preview preview = new Preview.Builder()
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    processImage(image);
                });

                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Error starting camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void processImage(ImageProxy imageProxy) {
        @SuppressWarnings("UnsafeOptInUsageError")
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            lastProcessedFrame = imageProxy.toBitmap();
            faceDetector.process(image)
                    .addOnSuccessListener(faces -> {
                        for (Face face : faces) {
                            Float leftEyeOpen = face.getLeftEyeOpenProbability();
                            Float rightEyeOpen = face.getRightEyeOpenProbability();

                            if (leftEyeOpen != null && rightEyeOpen != null) {
                                if (leftEyeOpen < 0.2 && rightEyeOpen < 0.2) {
                                    if (!blinkDetected && !blinkCooldown) {
                                        blinkDetected = true;
                                        handler.post(() -> performBlinkAction());
                                        Log.d("BlinkDetect", "Blink detected!");
                                        blinkCooldown = true;
                                        handler.postDelayed(() -> blinkCooldown = false, 2000);
                                    }
                                } else {
                                    blinkDetected = false;
                                }
                            }
                        }
                        imageProxy.close();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("BlinkDetect", "Face detection failed", e);
                        imageProxy.close();
                    });
        }
    }

    private void performBlinkAction() {
        if (blinkListener != null) {
            blinkListener.onBlinkDetected();
        }
    }

    public void shutdownCameraExecutor() {
        cameraExecutor.shutdown();
    }
}