package com.example.patientapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Dashboard extends AppCompatActivity {
    private DatabaseReference firebaseRef;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;

    private TextView latestMessage, msg;
    private DatabaseReference messagesRef;
    private ImageView notificationIcon;
    private Handler handler = new Handler();

    private TextToSpeech tts;
    private String lastSpokenMessage = "";

    private MaterialCardView emergencyCard, waterRequestCard, foodRequestCard, 
                           bathroomRequestCard, medicineRequestCard, videoCallCard, messageContainer;
    private AnimationDrawable animatedBackground;
    private NestedScrollView scrollView;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initializeViews();
        setupAnimations();
        setupClickListeners();
        initializeFirebase();
        initializeTextToSpeech();
        listenForMessages();
    }

    private void initializeViews() {
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize cards
        emergencyCard = findViewById(R.id.emergencyCard);
        waterRequestCard = findViewById(R.id.waterRequestCard);
        foodRequestCard = findViewById(R.id.foodRequestCard);
        bathroomRequestCard = findViewById(R.id.bathroomRequestCard);
        medicineRequestCard = findViewById(R.id.medicineRequestCard);
        videoCallCard = findViewById(R.id.videoCallCard);
        
        // Initialize toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.inflateMenu(R.menu.dashboard_menu);
        
        // Initialize scroll view with correct ID
        scrollView = findViewById(R.id.scrollView);

        // Set background animation
        View rootView = findViewById(android.R.id.content);
        rootView.setBackgroundResource(R.drawable.gradient_background);
        animatedBackground = (AnimationDrawable) rootView.getBackground();
        
        // Set toolbar menu for logout
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                logoutUser();
                return true;
            }
            return false;
        });

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
        new Handler().postDelayed(() -> medicineRequestCard.startAnimation(slideUp), 500);
        new Handler().postDelayed(() -> videoCallCard.startAnimation(fadeIn), 600);
    }

    private void setupClickListeners() {
        // Add ripple effect and click listeners to cards
        View.OnClickListener cardClickListener = v -> {
            MaterialCardView card = (MaterialCardView) v;
            card.setPressed(true);
            new Handler().postDelayed(() -> card.setPressed(false), 200);

            String request = "";
            if (v == emergencyCard) {
                request = "Emergency Help";
                sendEmergencyAlert();
            } else if (v == waterRequestCard) {
                request = "Water";
            } else if (v == foodRequestCard) {
                request = "Food";
            } else if (v == bathroomRequestCard) {
                request = "Bathroom";
            } else if (v == medicineRequestCard) {
                request = "Medicine";
            } else if (v == videoCallCard) {
                startVideoCall();
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
        medicineRequestCard.setOnClickListener(cardClickListener);
        videoCallCard.setOnClickListener(cardClickListener);
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

    private void sendEmergencyAlert() {
        if (messagesRef != null) {
            String timestamp = String.valueOf(System.currentTimeMillis());
            Map<String, Object> message = new HashMap<>();
            message.put("sender", "patient");
            message.put("text", "EMERGENCY: Immediate help needed!");
            message.put("timestamp", timestamp);
            message.put("priority", "high");

            messagesRef.child(timestamp).setValue(message)
                    .addOnSuccessListener(aVoid -> showSuccessMessage("Emergency alert sent!"))
                    .addOnFailureListener(e -> showErrorMessage("Failed to send emergency alert"));
        }
    }

    private void startVideoCall() {
        Intent intent = new Intent(this, VideoCallActivity.class);
        startActivity(intent);
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

                            // Only process messages sent by caretaker
                            if (sender.equals("caretaker") && text != null) {
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
        handler.postDelayed(() -> messageContainer.setVisibility(View.GONE), 10000);
    }

    private void speakMessage(String sender, String message) {
        String spokenText = "Message from " + sender + ": " + message;
        tts.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}

