package com.example.patientapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import com.google.firebase.database.DatabaseError;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SendMessage extends AppCompatActivity {
    private Button message1Button, message2Button, message3Button, message4Button;
    private ListView messageListView;
    private ArrayAdapter<String> messageAdapter;
    private ArrayList<String> messageList;

    private FirebaseAuth mAuth;
    private DatabaseReference messagesDatabase;
    private String patientEmail, caretakerEmail;
    private String chatRoomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Initialize SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        patientEmail = sharedPreferences.getString("user_email", null);
        caretakerEmail = sharedPreferences.getString("caretaker_email", null);  // Caretaker's email should be stored during patient login

        if (currentUser == null || patientEmail == null || caretakerEmail == null) {
            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Format emails to Firebase-friendly keys
        String patientKey = patientEmail.replace(".", "_");
        String caretakerKey = caretakerEmail.replace(".", "_");

        // Generate unique chat room ID for caretaker-patient
        chatRoomId = caretakerKey + "_" + patientKey;
        messagesDatabase = FirebaseDatabase.getInstance().getReference("messages").child(chatRoomId);

        // Initialize UI elements
        messageListView = findViewById(R.id.messageListView);
        message1Button = findViewById(R.id.message1Button);
        message2Button = findViewById(R.id.message2Button);
        message3Button = findViewById(R.id.message3Button);
        message4Button = findViewById(R.id.message4Button);

        // Initialize message list
        messageList = new ArrayList<>();
        messageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messageList);
        messageListView.setAdapter(messageAdapter);

        // Load existing messages
        loadMessages();

        // Button click listeners for predefined messages
        message1Button.setOnClickListener(v -> sendMessage("I need help"));
        message2Button.setOnClickListener(v -> sendMessage("I'm hungry"));
        message3Button.setOnClickListener(v -> sendMessage("I'm thirsty"));
        message4Button.setOnClickListener(v -> sendMessage("Emergency!"));
    }

    private void sendMessage(String messageText) {
        // Create message object
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("sender", "patient");
        messageData.put("receiver", "caretaker");
        messageData.put("text", messageText);
        messageData.put("timestamp", System.currentTimeMillis());

        // Save to Firebase
        messagesDatabase.push().setValue(messageData).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(SendMessage.this, "Failed to send message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessages() {
        messagesDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                messageList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String sender = snapshot.child("sender").getValue(String.class);
                    String text = snapshot.child("text").getValue(String.class);

                    if (sender != null && text != null && sender.equals("caretaker")) {
                        // Show only messages sent by the caretaker
                        messageList.add("Caretaker: " + text);
                    }
                }
                messageAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(SendMessage.this, "Failed to load messages: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
