package com.example.patientapp;

import androidx.annotation.NonNull; // Import NonNull
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.camera.view.PreviewView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Removed unused Firebase imports if you are handling sending purely in the adapter
// import com.google.firebase.auth.FirebaseAuth;
// import com.google.firebase.auth.FirebaseUser;
// import com.google.firebase.database.DataSnapshot;
// import com.google.firebase.database.DatabaseError;
// import com.google.firebase.database.DatabaseReference;
// import com.google.firebase.database.FirebaseDatabase;
// import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
// Removed unused HashMap and Map imports if not used here
// import java.util.HashMap;
import java.util.List;
// import java.util.Map;


public class SendMessage extends AppCompatActivity implements BlinkDetectionHelper.BlinkListener {

    private static final String TAG = "SendMessageActivity"; // Tag for logging
    private RecyclerView messageRecyclerView;
    private MessageAdapter messageAdapter;
    private List<String> messageList;
    // Removed highlightedIndex as highlighting is managed by the adapter
    // private int highlightedIndex = 0;
    private BlinkDetectionHelper blinkDetectionHelper;
    private PreviewView previewView;
    private Context context;

    // Handler and Runnable for cycling through tasks
    private final Handler highlightingHandler = new Handler(Looper.getMainLooper());
    private Runnable taskHighlightRunnable;
    private final int HIGHLIGHT_DELAY_MS = 3000; // 3 seconds delay
    private int currentHighlightIndex = -1; // Start before the first item

    // Removed Firebase variables if sending logic is solely in adapter
    // private FirebaseAuth mAuth;
    // private DatabaseReference messagesDatabase;
    // private String patientEmail, caretakerEmail;
    // private String chatRoomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);
        Log.d(TAG, "onCreate called");

        context = this; // Store context
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userEmail = prefs.getString("user_email", null);
        String patientId = userEmail != null ? userEmail.replace(".", ",") : null;
        // Initialize Views
        previewView = findViewById(R.id.previewView);
        messageRecyclerView = findViewById(R.id.messageRecyclerView);

        // Initialize Blink Detection
        blinkDetectionHelper = new BlinkDetectionHelper(this, previewView, this,patientId );

        // Setup RecyclerView
        messageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageList = new ArrayList<>();
        // Add predefined messages
        messageList.add("Come here");
        messageList.add("I need help");
        messageList.add("Feeling uncomfortable");
        messageList.add("Thank you");
        messageList.add("Yes");
        messageList.add("No");


        messageAdapter = new MessageAdapter(this, messageList);
        messageRecyclerView.setAdapter(messageAdapter);

        // Start Camera for Blink Detection AFTER permissions are likely granted (or handle permissions)
        // Assuming permissions are handled elsewhere or implicitly granted
        Log.d(TAG, "Starting camera");
        blinkDetectionHelper.startCamera(this);

        // Start the task highlighting cycle
        setupHighlightingCycle();

        // Removed Firebase setup code if not needed directly in Activity
        /*
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        patientEmail = sharedPreferences.getString("user_email", null);
        caretakerEmail = sharedPreferences.getString("caretaker_email", null);

        if (currentUser == null || patientEmail == null || caretakerEmail == null) {
            Toast.makeText(this, "User or caretaker email not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String patientKey = patientEmail.replace(".", "_");
        String caretakerKey = caretakerEmail.replace(".", "_");
        chatRoomId = caretakerKey + "_" + patientKey;
        messagesDatabase = FirebaseDatabase.getInstance().getReference("messages").child(chatRoomId);
        */

    }

    private void setupHighlightingCycle() {
        Log.d(TAG, "Setting up highlighting cycle");
        taskHighlightRunnable = new Runnable() {
            @Override
            public void run() {
                if (messageList.isEmpty()) {
                    Log.w(TAG, "Message list is empty, stopping highlight cycle.");
                    return; // Stop if list is empty
                }

                // Move to the next task index
                currentHighlightIndex++;
                if (currentHighlightIndex >= messageList.size()) {
                    currentHighlightIndex = 0; // Wrap around to the beginning
                }

                Log.d(TAG, "Highlighting item at index: " + currentHighlightIndex);
                // Highlight the current task using the adapter
                messageAdapter.highlightTask(currentHighlightIndex);

                // Ensure the highlighted item is visible
                messageRecyclerView.smoothScrollToPosition(currentHighlightIndex);


                // Post the runnable again after delay
                highlightingHandler.postDelayed(this, HIGHLIGHT_DELAY_MS);
            }
        };
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Starting highlighting");

        // Check if camera is already running (coming from Dashboard)
        if (!blinkDetectionHelper.isCameraRunning()) {
            blinkDetectionHelper.startCamera(this);
        } else {
            blinkDetectionHelper.resumeDetection(this);
        }

        highlightingHandler.postDelayed(taskHighlightRunnable, 500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Stopping highlighting");

        // Don't release camera completely - just pause detection
        blinkDetectionHelper.pauseDetection();
        highlightingHandler.removeCallbacks(taskHighlightRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Cleaning up");

        // Only shutdown completely if finishing
        if (isFinishing()) {
            blinkDetectionHelper.shutdown();
        }
        highlightingHandler.removeCallbacks(taskHighlightRunnable);
    }



    @Override
    public void onBlinkDetected() {

            int highlightedPosition = messageAdapter.getHighlightedPosition();
            Log.d(TAG, "Blink Detected! Highlighted position: " + highlightedPosition);

            if (highlightedPosition != -1) {
                Log.d(TAG, "Performing task action for position: " + highlightedPosition);
                messageAdapter.performTaskAction(highlightedPosition);
            } else {
                Log.w(TAG, "Blink detected but no item was highlighted.");
            }
    }

    // Removed sendMessage and loadMessages methods if Firebase logic is handled by adapter
    /*
    public void sendMessage(String messageText) { ... }
    private void loadMessages() { ... }
    */
}