package com.example.patientapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import com.google.firebase.database.DatabaseError;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.camera.view.PreviewView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Dashboard extends AppCompatActivity {

    private RecyclerView taskRecyclerView;
    private TaskCardAdapter taskCardAdapter;
    private List<String> taskList;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private static final int CAMERA_REQUEST_CODE = 100;
    private FaceDetector faceDetector;
    private boolean blinkDetected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        taskRecyclerView = findViewById(R.id.taskRecyclerView);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        taskRecyclerView.setLayoutManager(layoutManager);

        // Sample tasks
        taskList = new ArrayList<>();
        taskList.add("Drink Water");
        taskList.add("Washroom");
        taskList.add("Bedtime");
        taskList.add("Turn on/off light");
        taskList.add("Fan");
        taskList.add("Medicine Reminder");
        taskList.add("Send Message");
        taskList.add("Emergency Alert");

        taskCardAdapter = new TaskCardAdapter(this, taskList);
        taskRecyclerView.setAdapter(taskCardAdapter);
        setCardSize();
//        int margin = 8; // You can adjust this value as needed
//        taskRecyclerView.addItemDecoration(new ItemOffsetDecoration(margin));
        startHighlightingTasks();

        previewView = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }

        setupFaceDetector();

    }

    private void setCardSize() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
        int screenWidth = displayMetrics.widthPixels;
        int cardWidth = screenWidth / 3;

        int cardHeight = screenHeight/4;

        taskCardAdapter.setCardSize(cardWidth, cardHeight);
    }
    private void startHighlightingTasks() {
        final Handler handler = new Handler();
        final int delay = 3000; // 3 seconds delay

        Runnable taskHighlightRunnable = new Runnable() {
            int currentTaskIndex = 0;

            @Override
            public void run() {
                // Highlight the current task
                taskCardAdapter.highlightTask(currentTaskIndex);

                // Move to the next task
                currentTaskIndex++;
                if (currentTaskIndex >= taskList.size()) {
                    currentTaskIndex = 0; // Reset to loop infinitely
                }

                // Post the runnable again after delay
                handler.postDelayed(this, delay);
            }
        };

        handler.postDelayed(taskHighlightRunnable, delay);
    }


    private void setupFaceDetector() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
        faceDetector = FaceDetection.getClient(options);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT) // Use front camera
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
                        this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processImage(ImageProxy imageProxy) {
        @SuppressWarnings("UnsafeOptInUsageError")
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            faceDetector.process(image)
                    .addOnSuccessListener(faces -> {
                        for (Face face : faces) {
                            Float leftEyeOpen = face.getLeftEyeOpenProbability();
                            Float rightEyeOpen = face.getRightEyeOpenProbability();

                            if (leftEyeOpen != null && rightEyeOpen != null) {
                                if (leftEyeOpen < 0.2 && rightEyeOpen < 0.2) {  // Eye closed threshold
                                    if (!blinkDetected) {
                                        blinkDetected = true;
                                        runOnUiThread(() -> {
                                            performBlinkAction();
                                        });
//                                        runOnUiThread(() -> Toast.makeText(Dashboard.this, "Eye Blink Detected", Toast.LENGTH_SHORT).show());

//                                        Log.d("BlinkDetect", "Blink detected!");
                                    }
                                } else {
                                    blinkDetected = false;  // Reset when eyes are open
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
//        Toast.makeText(Dashboard.this, "Eye Blink Detected", Toast.LENGTH_SHORT).show();
        int highlightedPosition = taskCardAdapter.getHighlightedPosition();
        if (highlightedPosition != -1) {
            // Call the performTaskAction() method of the adapter
            taskCardAdapter.performTaskAction(highlightedPosition); // Use the adapter to perform the task action
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

}