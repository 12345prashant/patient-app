package com.example.patientapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull; // Import NonNull
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private Context context;
    private List<String> messages;
    private int highlightedPosition = -1; // Initialize to -1 (no highlight)

    public MessageAdapter(Context context, List<String> messages) {
        this.context = context;
        this.messages = messages;
    }

    @NonNull // Use NonNull annotation
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) { // Use NonNull annotation
        View view = LayoutInflater.from(context).inflate(R.layout.item_message_card, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) { // Use NonNull annotation
        String message = messages.get(position);
        holder.messageButton.setText(message);

        // Set background based on highlighted state
        if (position == highlightedPosition) {
            // Consider using a less visually intrusive highlight, e.g., a border or slight color change
            // Color.BLUE might be too strong and obscure text. Using a lighter blue or gray might be better.
            holder.itemView.setBackgroundColor(Color.LTGRAY); // Example: Light Gray highlight
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT); // Default: No background highlight
        }

        // Handle button click (manual click)
        holder.messageButton.setOnClickListener(v -> {
            sendRequestToCaretaker(context, message);
            // Optional: Add visual feedback on click if needed
            // Toast.makeText(context, "Clicked: " + message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // Method to update the highlighted position and refresh the view
    public void highlightTask(int position) {
        int previousHighlightedPosition = this.highlightedPosition;
        this.highlightedPosition = position;

        // Efficiently update only the changed items
        if (previousHighlightedPosition != -1) {
            notifyItemChanged(previousHighlightedPosition); // Unhighlight previous
        }
        if (this.highlightedPosition != -1) {
            notifyItemChanged(this.highlightedPosition);    // Highlight current
        }
    }

    // Getter for the currently highlighted position
    public int getHighlightedPosition() {
        return highlightedPosition;
    }

    // Method to send the message associated with a specific item
    private void sendRequestToCaretaker(Context context, String request) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String patientEmail = sharedPreferences.getString("user_email", null);
        String caretakerEmail = sharedPreferences.getString("caretaker_email", null);

        if (patientEmail == null || caretakerEmail == null) {
            Toast.makeText(context, "User or caretaker email not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Replace invalid Firebase key characters
        String patientKey = patientEmail.replace(".", "_");
        String caretakerKey = caretakerEmail.replace(".", "_");
        String chatRoomId = caretakerKey + "_" + patientKey;


        DatabaseReference messagesDatabase = FirebaseDatabase.getInstance().getReference("messages").child(chatRoomId);

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("sender", "patient"); // Indicate sender is patient
        messageData.put("receiver", "caretaker"); // Indicate receiver is caretaker
        messageData.put("text", request);
        messageData.put("timestamp", System.currentTimeMillis());

        messagesDatabase.push().setValue(messageData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Provide more specific feedback
                Toast.makeText(context, "Message sent", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show();
                // Log the error for debugging
                // Log.e("MessageAdapter", "Failed to send message", task.getException());
            }
        });
    }

    /**
     * Performs the action associated with the item at the given position.
     * In this case, it sends the message text of that item.
     *
     * @param position The adapter position of the item to act upon.
     */
    public void performTaskAction(int position) {
        // Validate position before accessing the list
        if (position >= 0 && position < messages.size()) {
            String messageToSend = messages.get(position);
            sendRequestToCaretaker(context, messageToSend);
        } else {
            Toast.makeText(context, "Invalid position for action: " + position, Toast.LENGTH_SHORT).show();
            // Log error if needed
            // Log.e("MessageAdapter", "Attempted to perform action on invalid position: " + position);
        }
    }


    // ViewHolder class
    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        Button messageButton;

        public MessageViewHolder(View itemView) {
            super(itemView);
            messageButton = itemView.findViewById(R.id.messageButton);
        }
    }
}