package com.example.patientapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskCardAdapter extends RecyclerView.Adapter<TaskCardAdapter.ViewHolder> {

    private Context context;
    private List<String> tasks;
    private int cardWidth;
    private int cardHeight;

    public TaskCardAdapter(Context context, List<String> tasks) {
        this.context = context;
        this.tasks = tasks;
    }

    // Set the size for each card dynamically
    public void setCardSize(int width, int height) {
        this.cardWidth = width;
        this.cardHeight = height;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_button, parent, false);

        // Set dynamic card size (width and height) here
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = cardWidth;
        params.height = cardHeight;
        view.setLayoutParams(params);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.taskTitle.setText(tasks.get(position));
        holder.itemView.setOnClickListener(v -> {
            // Show Toast with the content of the clicked card (task name)
            String taskName = tasks.get(position);
            if (taskName.equals("Emergency Alert")) {
                sendEmergencyAlert();
            } else if (taskName.equals("Drink Water") || taskName.equals("Washroom")) {
                sendMessageToCaretaker(taskName);
            }
            else {
                Toast.makeText(context, taskName, Toast.LENGTH_SHORT).show();
            }
            Toast.makeText(context, taskName, Toast.LENGTH_SHORT).show();



            Intent intent = null;

            switch (taskName) {
                case "Send Message":
                    intent = new Intent(context, SendMessage.class);
                    break;
//                case "Drink Water":
//                    // Example for another task
//                    intent = new Intent(context, DrinkWater.class);
//                    break;
//                case "Washroom":
//                    intent = new Intent(context, Washroom.class);
//                    break;
                // Add more cases for other tasks here...
                default:
                    break;
            }
            if (intent != null) {
                context.startActivity(intent);
            }
        });
    }
    private void sendMessageToCaretaker(String messageText) {
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
        messageData.put("text", messageText);
        messageData.put("timestamp", System.currentTimeMillis());

        messagesDatabase.push().setValue(messageData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(context, "Message sent: " + messageText, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Function to send an emergency alert to Firebase
    private void sendEmergencyAlert() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("emergency_alerts");

        // Get patient email from SharedPreferences
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String patientEmail = sharedPreferences.getString("user_email", null);

        if (patientEmail == null) {
            Toast.makeText(context, "User email not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Replace "." with "," since Firebase keys can't have dots
        String patientKey = patientEmail.replace(".", "_");

        Map<String, Object> alertData = new HashMap<>();
        alertData.put("patientId", patientKey);
        alertData.put("timestamp", System.currentTimeMillis());

        databaseReference.child(patientKey).setValue(alertData)
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Emergency Alert Sent!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to send alert", Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView taskTitle;

        public ViewHolder(View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.taskTitle);
        }
    }
}
class ItemOffsetDecoration extends RecyclerView.ItemDecoration {

    private int mItemOffset;

    public ItemOffsetDecoration(int itemOffset) {
        mItemOffset = itemOffset;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.left = mItemOffset;
        outRect.right = mItemOffset;
        outRect.top = mItemOffset;
        outRect.bottom = mItemOffset;
    }
}
