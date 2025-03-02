package com.example.patientapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskCardAdapter extends RecyclerView.Adapter<TaskCardAdapter.ViewHolder> {

    private Context context;
    private List<String> tasks;
    private int cardWidth;
    private int cardHeight;
    private int highlightedPosition = -1;
    public TaskCardAdapter(Context context, List<String> tasks) {
        this.context = context;
        this.tasks = tasks;
    }

    // Set the size for each card dynamically
    public void setCardSize(int width, int height) {
        this.cardWidth = width;
        this.cardHeight = height;
    }
    public void highlightTask(int position) {
        this.highlightedPosition = position;
        notifyDataSetChanged(); // Notify adapter to refresh the views
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
        if (position == highlightedPosition) {
            holder.itemView.setBackgroundColor(Color.YELLOW); // Highlight color
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT); // No highlight
        }

        holder.itemView.setOnClickListener(v -> {
            // Show Toast with the content of the clicked card (task name)
            String taskName = tasks.get(position);
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
