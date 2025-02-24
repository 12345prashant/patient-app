package com.example.patientapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendMessage extends AppCompatActivity {

    private RecyclerView messageRecyclerView;
    private MessageAdapter messageAdapter;
    private List<String> messageList;

    private FirebaseAuth mAuth;
    private DatabaseReference messagesDatabase;
    private String patientEmail, caretakerEmail;
    private String chatRoomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        patientEmail = sharedPreferences.getString("user_email", null);
        caretakerEmail = sharedPreferences.getString("caretaker_email", null);

        if (currentUser == null || patientEmail == null || caretakerEmail == null) {
            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String patientKey = patientEmail.replace(".", "_");
        String caretakerKey = caretakerEmail.replace(".", "_");
        chatRoomId = caretakerKey + "_" + patientKey;
        messagesDatabase = FirebaseDatabase.getInstance().getReference("messages").child(chatRoomId);

        messageRecyclerView = findViewById(R.id.messageRecyclerView);
        messageRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        messageList = new ArrayList<>();
        messageList.add("I need Help");
        messageList.add("I need Help");
        messageList.add("I need Help");
        messageAdapter = new MessageAdapter(this, messageList, this);
        messageRecyclerView.setAdapter(messageAdapter);

//        loadMessages();
    }

    public void sendMessage(String messageText) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("sender", "patient");
        messageData.put("receiver", "caretaker");
        messageData.put("text", messageText);
        messageData.put("timestamp", System.currentTimeMillis());

        messagesDatabase.push().setValue(messageData).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(SendMessage.this, "Failed to send message", Toast.LENGTH_SHORT).show();
            } else {
                // Add the message to the list and notify the adapter
                messageList.add(messageText);
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                messageRecyclerView.scrollToPosition(messageList.size() - 1);
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
                        messageList.add("Caretaker: " + text);
                    }
                }
                messageAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(SendMessage.this, "Failed to load messages: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
