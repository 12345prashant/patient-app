package com.example.patientapp;



public class LiveStreamService {
//    private ImageCapture imageCapture;
//    private Handler handler = new Handler();
//    private ExecutorService executorService;
//    private DatabaseReference databaseReference;
//    private StorageReference storageReference;
//    private String patientEmail;
//
//    public LiveStreamService(LifecycleOwner lifecycleOwner) {
//        executorService = Executors.newSingleThreadExecutor();
////        patientEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().replace(".", "_");
//        FirebaseAuth auth = FirebaseAuth.getInstance();
//        if (auth.getCurrentUser() != null && auth.getCurrentUser().getEmail() != null) {
//            patientEmail = auth.getCurrentUser().getEmail().replace(".", "_");
//        } else {
//            Log.e("LiveStreamService", "User not authenticated");
//            return;
//        }
//
//        databaseReference = FirebaseDatabase.getInstance().getReference("patients").child(patientEmail).child("liveStream");
//        storageReference = FirebaseStorage.getInstance().getReference("live_streams").child(patientEmail + ".jpg");
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
//
//        cameraProviderFuture.addListener(() -> {
//            try {
//                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
//                imageCapture = new ImageCapture.Builder().build();
//                cameraProvider.bindToLifecycle(lifecycleOwner, androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA, imageCapture);
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
//        storageReference.putBytes(data).addOnSuccessListener(taskSnapshot -> {
//            storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
//                databaseReference.setValue(uri.toString());
//            });
//        }).addOnFailureListener(e -> {
//            Log.e("FirebaseUploadError", "Failed to upload image", e);
//        });
//    }
//
//    private Bitmap convertImageProxyToBitmap(ImageProxy imageProxy) {
//        ImageProxy.PlaneProxy plane = imageProxy.getPlanes()[0];
//        ByteBuffer buffer = plane.getBuffer();
//        byte[] bytes = new byte[buffer.remaining()];
//        buffer.get(bytes);
//        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//        return bitmap;
//    }


}
