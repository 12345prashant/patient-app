package com.example.patientapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private Context context;
    private List<String> messages;
    private SendMessage sendMessageActivity;


    public MessageAdapter(Context context, List<String> messages, SendMessage sendMessageActivity) {
        this.context = context;
        this.messages = messages;
        this.sendMessageActivity = sendMessageActivity;
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
            sendMessageActivity.sendMessage(message);
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        });

    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        Button messageButton;

        public MessageViewHolder(View itemView) {
            super(itemView);
            messageButton = itemView.findViewById(R.id.messageButton);
        }
    }
}
