package com.example.patientapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LiveStreamService {
    private ImageCapture imageCapture;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executorService;
    private final DatabaseReference databaseReference;
    private final StorageReference storageReference;
    private final String patientEmail;

    public LiveStreamService(LifecycleOwner lifecycleOwner) {
        executorService = Executors.newSingleThreadExecutor();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null && auth.getCurrentUser().getEmail() != null) {
            patientEmail = auth.getCurrentUser().getEmail().replace(".", "_");
        } else {
            Log.e("LiveStreamService", "User not authenticated");
            throw new IllegalStateException("User must be authenticated to use LiveStreamService");
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("patients")
                .child(patientEmail).child("liveStream");
        storageReference = FirebaseStorage.getInstance().getReference("live_streams")
                .child(patientEmail + ".jpg");

        startCamera(lifecycleOwner);
        startUploadingImages();
    }

    private void startCamera(LifecycleOwner lifecycleOwner) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(
                ((androidx.fragment.app.FragmentActivity) lifecycleOwner).getApplicationContext()
        );

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();
                cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA,
                        imageCapture
                );
            } catch (Exception e) {
                Log.e("CameraError", "Failed to start camera", e);
            }
        }, executorService);
    }

    private void startUploadingImages() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                captureAndUploadImage();
                handler.postDelayed(this, 5000); // Capture every 5 seconds
            }
        }, 5000);
    }

    private void captureAndUploadImage() {
        if (imageCapture == null) return;

        imageCapture.takePicture(executorService, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                Bitmap bitmap = convertImageProxyToBitmap(imageProxy);
                imageProxy.close();

                if (bitmap != null) {
                    uploadImage(bitmap);
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("ImageCaptureError", "Failed to capture image", exception);
            }
        });
    }

    private void uploadImage(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        byte[] data = baos.toByteArray();

        storageReference.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> 
                    storageReference.getDownloadUrl()
                            .addOnSuccessListener(uri -> 
                                databaseReference.setValue(uri.toString())
                            )
                )
                .addOnFailureListener(e -> 
                    Log.e("FirebaseUploadError", "Failed to upload image", e)
                );
    }

    private Bitmap convertImageProxyToBitmap(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy plane = imageProxy.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public void cleanup() {
        handler.removeCallbacksAndMessages(null);
        executorService.shutdown();
    }
}
