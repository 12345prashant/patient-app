package com.example.patientapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private Context context;
    private List<String> messages;
//    private SendMessage sendMessageActivity;


    public MessageAdapter(Context context, List<String> messages) {
        this.context = context;
        this.messages = messages;
//        this.sendMessageActivity = sendMessageActivity;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_message_card, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        String message = messages.get(position);
        holder.messageButton.setText(message);

        // Handle button click
        holder.messageButton.setOnClickListener(v -> {
            this.sendRequestToCaretaker(context, message);
//            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        });

    }

    @Override
    public int getItemCount() {
        return messages.size();
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
                Toast.makeText(context, "Message sent", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show();
            }
        });
    }


    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        Button messageButton;

        public MessageViewHolder(View itemView) {
            super(itemView);
            messageButton = itemView.findViewById(R.id.messageButton);
        }
    }
}
