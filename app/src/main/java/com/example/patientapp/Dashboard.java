package com.example.patientapp;

import android.Manifest;
import android.content.Context;
import android.graphics.Color;
//import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

public class Dashboard extends AppCompatActivity implements BlinkDetectionHelper.BlinkListener{
    private DatabaseReference firebaseRef;
    private Context context;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;

    private TextView latestMessage, msg;
    private DatabaseReference messagesRef, databaseReference;
    private ImageView notificationIcon;
    private Handler handler = new Handler();

    private TextToSpeech tts;
    private String lastSpokenMessage = "";

    private MaterialCardView emergencyCard, waterRequestCard, foodRequestCard,
            bathroomRequestCard, homeControlCard, sendMessageCard, messageContainer;
    private AnimationDrawable animatedBackground;
    private NestedScrollView scrollView;
    private MaterialToolbar toolbar;

    private List<MaterialCardView> cards;
    private PreviewView previewView;

//    private static final int CAMERA_REQUEST_CODE = 100;

    private int highlightedIndex = 0;
    private BlinkDetectionHelper blinkDetectionHelper;

    private static final int PERMISSION_REQ_ID = 22;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userEmail = prefs.getString("user_email", null);
        String patientId = userEmail != null ? userEmail.replace(".", ",") : null;

        context = this;
        previewView = findViewById(R.id.previewView);
        blinkDetectionHelper = new BlinkDetectionHelper(this, previewView, this, patientId);




//        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
//            blinkDetectionHelper.startCamera(this);
//        } else {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
//        }


        initializeViews();
        setupAnimations();
        setupClickListeners();
        initializeFirebase();
        initializeTextToSpeech();
        listenForMessages();
        if (checkPermissions()) {

//            startVideoCalling();
            blinkDetectionHelper.startCamera(this);
        } else {
            requestPermissions();
        }
    }
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, getRequiredPermissions(), PERMISSION_REQ_ID);
    }
    private boolean checkPermissions() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    private String[] getRequiredPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
        } else {
            return new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
            };
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID && checkPermissions()) {

//            startVideoCalling();
            blinkDetectionHelper.startCamera(this);
        }
    }


    private void initializeViews() {
        mAuth = FirebaseAuth.getInstance();

        // Initialize cards
        emergencyCard = findViewById(R.id.emergencyCard);
        waterRequestCard = findViewById(R.id.waterRequestCard);
        foodRequestCard = findViewById(R.id.foodRequestCard);
        bathroomRequestCard = findViewById(R.id.bathroomRequestCard);
        homeControlCard = findViewById(R.id.homeControlCard);
        sendMessageCard = findViewById(R.id.sendMessageCard);

        // Initialize toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.inflateMenu(R.menu.dashboard_menu);

        // Initialize scroll view with correct ID
        scrollView = findViewById(R.id.scrollView);

        // Set background animation
        View rootView = findViewById(R.id.coordinatorLayout);
        rootView.setBackgroundResource(R.drawable.gradient_background);
        animatedBackground = (AnimationDrawable) rootView.getBackground();

        // Set toolbar menu for logout
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                logoutUser();
                return true;
            }else if (item.getItemId() == R.id.action_settings) {
                openSettings();
                return true;
            }else if (item.getItemId() == R.id.action_bedtime) {

                Toast.makeText(this, "BedTime clicked", Toast.LENGTH_SHORT).show();
                toggleBedTimeMode();

                return true;
            }
            return false;
        });

        handler = new Handler();

        // Initialize the list..
        cards = new ArrayList<>();
        cards.add(emergencyCard);
        cards.add(waterRequestCard);
        cards.add(foodRequestCard);
        cards.add(bathroomRequestCard);
        cards.add(homeControlCard);
        cards.add(sendMessageCard);

        handler.post(highlightRunnable);

        // Initialize message views with new IDs
        latestMessage = findViewById(R.id.message);
        notificationIcon = findViewById(R.id.notification_icon);
        msg = findViewById(R.id.msg);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    private void setupAnimations() {
        // Start background animation
        animatedBackground.setEnterFadeDuration(2000);
        animatedBackground.setExitFadeDuration(4000);
        animatedBackground.start();

        // Load animations
        android.view.animation.Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        android.view.animation.Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        // Apply animations to cards with delays
        new Handler().postDelayed(() -> emergencyCard.startAnimation(fadeIn), 100);
        new Handler().postDelayed(() -> waterRequestCard.startAnimation(slideUp), 200);
        new Handler().postDelayed(() -> foodRequestCard.startAnimation(slideUp), 300);
        new Handler().postDelayed(() -> bathroomRequestCard.startAnimation(slideUp), 400);
        new Handler().postDelayed(() -> homeControlCard.startAnimation(slideUp), 500);
        new Handler().postDelayed(() -> sendMessageCard.startAnimation(fadeIn), 600);

    }
    public void onBackPressed() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> finishAffinity()) // Exit the app
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss()) // Dismiss dialog
                .show();
    }

    private void setupClickListeners() {
        // Add ripple effect and click listeners to cards
        View.OnClickListener cardClickListener = v -> {
            MaterialCardView card = (MaterialCardView) v;
            card.setPressed(true);
            new Handler().postDelayed(() -> card.setPressed(false), 200);

            String request = "";
            if (v == emergencyCard) {
//                request = "Emergency Help";
                sendEmergencyAlert();

            } else if (v == waterRequestCard) {
                request = "Water";
            } else if (v == foodRequestCard) {
                request = "Food";
            } else if (v == bathroomRequestCard) {
                request = "Bathroom";
            } else if (v == homeControlCard) {
                controlLights();

            } else if (v == sendMessageCard) {
                sendCustomMessage();

                return;
            }

            if (!request.isEmpty()) {
                sendRequestToCaretaker(getApplicationContext(), request);
            }
        };

        // Set click listeners
        emergencyCard.setOnClickListener(cardClickListener);
        waterRequestCard.setOnClickListener(cardClickListener);
        foodRequestCard.setOnClickListener(cardClickListener);
        bathroomRequestCard.setOnClickListener(cardClickListener);
        homeControlCard.setOnClickListener(cardClickListener);
        sendMessageCard.setOnClickListener(cardClickListener);
    }

    private void initializeFirebase() {
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        String patientEmail = sharedPreferences.getString("user_email", "").replace(".", "_");
        String caretakerEmail = sharedPreferences.getString("caretaker_email", "").replace(".", "_");

        if (!caretakerEmail.isEmpty() && !patientEmail.isEmpty()) {
            String messageKey = caretakerEmail + "_" + patientEmail;
            messagesRef = FirebaseDatabase.getInstance().getReference("messages").child(messageKey);

            messageContainer = findViewById(R.id.message_container);
            messageContainer.setVisibility(View.GONE);

        }
    }



    private void initializeTextToSpeech() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
            }
        });
    }

    private void sendRequestToCaretaker(Context context, String request) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String patientEmail = sharedPreferences.getString("user_email", null);
        String caretakerEmail = sharedPreferences.getString("caretaker_email", null);

        if (patientEmail == null || caretakerEmail == null) {
            Toast.makeText(context, "User or caretaker email not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        String patientKey = patientEmail.replace(".", "_");
        String caretakerKey = caretakerEmail.replace(".", "_");
        String chatRoomId = caretakerKey + "_" + patientKey;

        DatabaseReference messagesDatabase = FirebaseDatabase.getInstance().getReference("messages").child(chatRoomId);

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("sender", "patient");
        messageData.put("receiver", "caretaker");
        messageData.put("text", request);
        messageData.put("timestamp", System.currentTimeMillis());

        messagesDatabase.push().setValue(messageData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(context, "Message sent: " + request, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show();
            }
        });
    }




    private void controlLights(){
        String packageName = "com.example.smarthomecontrol"; // Your app's package name
        String activityName = "com.example.smarthomecontrol.MainActivity"; // The fully qualified class name

        Intent intent = new Intent();
        intent.setClassName(packageName, activityName);

        try {
            context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {

            e.printStackTrace(); // Log the error for debugging
            // Optionally, show a message to the user:
            Toast.makeText(context, "Dashboard activity not found.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendCustomMessage(){

        Intent intent = new Intent(context, SendMessage.class);

        try {
            context.startActivity(intent);
            finish();
        } catch (android.content.ActivityNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(context, "Send Message activity not found.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSuccessMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showErrorMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void logoutUser() {
        // Clear the cached email from SharedPreferences
        sharedPreferences.edit().remove("user_email").apply();
        sharedPreferences.edit().remove("caretaker_email").apply();
        // Sign out from Firebase
        mAuth.signOut();

        // Redirect to MainActivity (Login screen)
        Intent intent = new Intent(Dashboard.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    private void openSettings() {
        // Start your settings activity or show a settings dialog
        Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
        // Or start a SettingsActivity:
        Intent intent = new Intent(Dashboard.this, SettingsActivity.class);
        startActivity(intent);
    }
    private void toggleBedTimeMode(){
        try {
            Intent intent = new Intent(Dashboard.this, BlackScreenActivity.class);
            startActivity(intent);
            // Optional: Add transition animation
//            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Log.e("Dashboard", "Error starting BlackScreenActivity", e);
            Toast.makeText(this, "Error activating bedtime mode", Toast.LENGTH_SHORT).show();
        }
    }




    private void listenForMessages() {
        // Ensure messagesRef is correctly initialized
        if (messagesRef == null) {
            Log.e("Firebase", "messagesRef is null. Check Firebase initialization.");
            return;
        }

        // Fetch the last 10 messages and filter caretaker messages
        messagesRef.orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> caretakerMessages = new ArrayList<>();
                        String latestCaretakerMessage = "";

                        // Log the total number of messages fetched
                        Log.d("Firebase", "Total messages fetched: " + snapshot.getChildrenCount());

                        for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                            // Parse sender and text from the snapshot
                            String sender = msgSnapshot.child("sender").getValue(String.class);
                            String text = msgSnapshot.child("text").getValue(String.class);

                            // Log each message for debugging
                            Log.d("Firebase", "Sender: " + sender + ", Text: " + text);
                            String caretakerEmail = sharedPreferences.getString("caretaker_email", "");
                            // Only process messages sent by caretaker
                            if (sender.equals(caretakerEmail) && text != null) {
                                caretakerMessages.add(text);
                                latestCaretakerMessage = text;
                            }
                        }

                        // Log the number of caretaker messages found
                        Log.d("Firebase", "Caretaker messages found: " + caretakerMessages.size());

                        // Update UI with the latest caretaker message
                        if (!caretakerMessages.isEmpty()) {
                            String finalLatestCaretakerMessage = latestCaretakerMessage;
                            runOnUiThread(() -> {
                                // Display the latest caretaker message
                                latestMessage.setText(finalLatestCaretakerMessage);

                                // Show the notification container
                                if (!finalLatestCaretakerMessage.equals(lastSpokenMessage)) showNotification();
                            });

                            // Speak only if the latest caretaker message is NEW
                            if (!latestCaretakerMessage.equals(lastSpokenMessage)) {
                                speakMessage("caretaker", latestCaretakerMessage);
                                lastSpokenMessage = latestCaretakerMessage; // Update last spoken message
                            }
                        } else {
                            Log.d("Firebase", "No caretaker messages found.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Error fetching messages", error.toException());
                    }
                });
    }

    private void showNotification() {
        messageContainer.setVisibility(View.VISIBLE);
        // Hide after 5 seconds
        handler.postDelayed(() -> messageContainer.setVisibility(View.GONE), 5000);
    }

    private void speakMessage(String sender, String message) {
        String spokenText = "Message from " + sender + ": " + message;
        tts.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, null);
    }


    // Function to send an emergency alert to Firebase
    private void sendEmergencyAlert() {
        databaseReference = FirebaseDatabase.getInstance().getReference("emergency_alerts");

        // Get patient email from SharedPreferences
        SharedPreferences sharedPreferences = Dashboard.this.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE); // Use Dashboard.this as context
        String patientEmail = sharedPreferences.getString("user_email", null);

        if (patientEmail == null) {
            Toast.makeText(Dashboard.this, "User email not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Replace "." with "," since Firebase keys can't have dots
        String patientKey = patientEmail.replace(".", "_");

        Map<String, Object> alertData = new HashMap<>();
        alertData.put("patientId", patientKey);
        alertData.put("timestamp", System.currentTimeMillis());

        databaseReference.child(patientKey).setValue(alertData)
                .addOnSuccessListener(aVoid -> Toast.makeText(Dashboard.this, "Emergency Alert Sent!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(Dashboard.this, "Failed to send alert", Toast.LENGTH_SHORT).show());
    }


    private void highlightCard(MaterialCardView card, boolean highlight) {
        if (highlight) {
            // Highlight the card (e.g., change background color or stroke color)
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.warning)); // Use a highlight color
            card.setStrokeWidth(4); // Add a border
            card.setStrokeColor(ContextCompat.getColor(this, R.color.warning)); // Use a stroke color
        } else {
            // Reset the card to its original state
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white)); // Use the default card color
            card.setStrokeWidth(0); // Remove the border
        }
    }

    private Runnable highlightRunnable = new Runnable() {
        @Override
        public void run() {
            // Reset all cards to their default state
            resetAllCards(cards);

            // Move to the next card
            highlightedIndex = (highlightedIndex + 1) % cards.size();
            // Highlight the current card
            highlightCard(cards.get(highlightedIndex), true);



            // Repeat after a delay (e.g., 3 seconds)
            handler.postDelayed(this, 3000); // 3000ms = 3 seconds
        }
    };

    private void resetAllCards(List<MaterialCardView> cards) {
        for (MaterialCardView card : cards) {
            highlightCard(card, false);
        }
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == CAMERA_REQUEST_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                blinkDetectionHelper.startCamera(this);
//            } else {
//                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }


    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(Dashboard.this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onBlinkDetected() {
        cards.get(highlightedIndex).performClick();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (blinkDetectionHelper != null) {
            // Only pause detection if we're not finishing
            if (!isFinishing()) {
                blinkDetectionHelper.pauseDetection();
            } else {
                blinkDetectionHelper.shutdown();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissions() && blinkDetectionHelper != null) {
            if (!blinkDetectionHelper.isCameraRunning()) {
                blinkDetectionHelper.startCamera(this);
            } else {
                blinkDetectionHelper.resumeDetection(this);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
        blinkDetectionHelper.shutdown();

    }
}