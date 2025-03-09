package com.example.patientapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.webrtc.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
public class Dashboard extends AppCompatActivity {

    private RecyclerView taskRecyclerView;
    private TaskCardAdapter taskCardAdapter;
    private List<String> taskList;

    private DatabaseReference firebaseRef;

    private SharedPreferences sharedPreferences;

    private Button logoutbutton;
    private FirebaseAuth mAuth;

    private TextView message1, message2, message3;
    private DatabaseReference messagesRef;
    private LinearLayout messageBox;
    private ImageView ivNotifications;
    private Handler handler = new Handler();

    private TextToSpeech tts;
    private String lastSpokenMessage = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mAuth = FirebaseAuth.getInstance();
        logoutbutton = findViewById(R.id.button2);
        taskRecyclerView = findViewById(R.id.taskRecyclerView);
        taskRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        taskList = new ArrayList<>();
        Collections.addAll(taskList, "Drink Water", "Washroom", "Bedtime", "Turn on/off light", "Fan", "Send Message", "Emergency Alert");

        taskCardAdapter = new TaskCardAdapter(this, taskList);
        taskRecyclerView.setAdapter(taskCardAdapter);
        setCardSize();
        logoutbutton.setOnClickListener(v -> logoutUser());


        // Initialize Text-to-Speech
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
            }
        });

        // Initialize message TextViews
        message1 = findViewById(R.id.message1);
        message2 = findViewById(R.id.message2);
        message3 = findViewById(R.id.message3);
        // Get stored emails
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String patientEmail = sharedPreferences.getString("user_email", "").replace(".", "_");
        String caretakerEmail = sharedPreferences.getString("caretaker_email", "").replace(".", "_");
        if (caretakerEmail.isEmpty() || patientEmail.isEmpty()) {
            Log.e("Dashboard", "Caretaker or Patient email missing!");
            return;
        }

        String messageKey = caretakerEmail + "_" + patientEmail;
        messagesRef = FirebaseDatabase.getInstance().getReference("messages").child(messageKey);

        ivNotifications = findViewById(R.id.ivNotifications);
        messageBox = findViewById(R.id.messageBox);

        // Initially hide message box
        messageBox.setVisibility(View.GONE);

        listenForMessages();
    }
    @Override
    public void onBackPressed() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> finishAffinity()) // Exit the app
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss()) // Dismiss dialog
                .show();
    }




    private void setCardSize() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int cardWidth = displayMetrics.widthPixels / 3;
        int cardHeight = displayMetrics.heightPixels / 4;
        taskCardAdapter.setCardSize(cardWidth, cardHeight);
    }
    private void logoutUser() {
        // Clear the cached email from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        sharedPreferences.edit().remove("user_email").apply();
        sharedPreferences.edit().remove("caretaker_email").apply();
        // Sign out from Firebase
        mAuth.signOut();

        // Stop the emergency service to remove old data
//        stopService(new Intent(this, EmergencyAlertService.class));

        // Redirect to MainActivity (Login screen)
        Intent intent = new Intent(Dashboard.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    private void listenForMessages() {
        messagesRef.orderByChild("timestamp").limitToLast(10) // Fetch last 10 to filter caretaker messages
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> caretakerMessages = new ArrayList<>();
                        String latestCaretakerMessage = "";
                        for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                            String sender = msgSnapshot.child("sender").getValue(String.class);
                            String text = msgSnapshot.child("text").getValue(String.class);

                            // Only add messages sent by caretaker
                            if ("caretaker".equals(sender) && text != null) {
                                caretakerMessages.add(text);
//                                speakMessage(sender, text);
                                latestCaretakerMessage = text;
                            }
                        }

                        // Speak only if the latest caretaker message is NEW
                        if (!latestCaretakerMessage.isEmpty() && !latestCaretakerMessage.equals(lastSpokenMessage)) {
                            speakMessage("caretaker", latestCaretakerMessage);
                            lastSpokenMessage = latestCaretakerMessage; // Update last spoken message
                        }

                        // Keep only the last 3 caretaker messages
                        int size = caretakerMessages.size();
                        message1.setText(size > 0 ? caretakerMessages.get(size - 1) : "");
                        message2.setText(size > 1 ? caretakerMessages.get(size - 2) : "");
                        message3.setText(size > 2 ? caretakerMessages.get(size - 3) : "");

                        if (!caretakerMessages.isEmpty()) {
                            showNotification();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Error fetching messages", error.toException());
                    }
                });
    }
    private void showNotification() {
        messageBox.setVisibility(View.VISIBLE);

        // Hide after 5 seconds
        handler.postDelayed(() -> messageBox.setVisibility(View.GONE), 5000);
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

