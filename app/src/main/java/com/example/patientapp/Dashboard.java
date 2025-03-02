package com.example.patientapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import com.google.firebase.database.DatabaseError;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.android.Utils;


//public class Dashboard extends AppCompatActivity implements CvCameraViewListener2 {
//
//    private CameraBridgeViewBase mOpenCvCameraView;
//    private CascadeClassifier faceCascade;
//    private CascadeClassifier eyeCascade;
//    private BlinkDetection blinkDetection;
//
//    private TaskCardAdapter taskCardAdapter;
//    private List<String> taskList;
//
//    private int highlightedTaskIndex = -1; // Track the currently highlighted task
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_dashboard);
//
//        // Initialize OpenCV
//        OpenCVLoader.initDebug();
//
////        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.openCvCameraView);
////        mOpenCvCameraView.setCvCameraViewListener(this);
//
//        // Load Haar Cascade Classifiers for face and eyes
//        faceCascade = new CascadeClassifier("path_to_haar_face_cascade.xml");
//        eyeCascade = new CascadeClassifier("path_to_haar_eye_cascade.xml");
//
//        // Initialize BlinkDetection
////        blinkDetection = new BlinkDetection(faceCascade, eyeCascade);
//
//        // Initialize tasks and adapter
//        taskList = new ArrayList<>();
//        taskList.add("Drink Water");
//        taskList.add("Washroom");
//        taskList.add("Bedtime");
//        taskList.add("Turn on/off light");
//        taskList.add("Fan");
//        taskList.add("Medicine Reminder");
//        taskList.add("Send Message");
//        taskList.add("Emergency Alert");
//
//        taskCardAdapter = new TaskCardAdapter(this, taskList);
//        RecyclerView taskRecyclerView = findViewById(R.id.taskRecyclerView);
//        taskRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
//        taskRecyclerView.setAdapter(taskCardAdapter);
//        startHighlightingTasks();
//    }
//
//    private void startHighlightingTasks() {
//        final Handler handler = new Handler();
//        final int delay = 3000; // 3 seconds delay
//
//        Runnable taskHighlightRunnable = new Runnable() {
//            int currentTaskIndex = 0;
//
//            @Override
//            public void run() {
//                // Highlight the current task
//                taskCardAdapter.highlightTask(currentTaskIndex);
//
//                // Move to the next task
//                currentTaskIndex++;
//                if (currentTaskIndex >= taskList.size()) {
//                    currentTaskIndex = 0; // Reset to loop infinitely
//                }
//
//                // Post the runnable again after delay
//                handler.postDelayed(this, delay);
//            }
//        };
//
//        handler.postDelayed(taskHighlightRunnable, delay);
//    }
//
////    @Override
////    public void onResume() {
////        super.onResume();
////        mOpenCvCameraView.enableView();
////    }
////
////    @Override
////    public void onPause() {
////        super.onPause();
////        if (mOpenCvCameraView != null) {
////            mOpenCvCameraView.disableView();
////        }
////    }
//
//    @Override
//    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//        Mat rgbaFrame = inputFrame.rgba();
//        Mat grayFrame = inputFrame.gray();
//
//        // Use the BlinkDetection class to process frames
//        blinkDetection.processFrame(rgbaFrame, grayFrame, this::onBlinkDetected);
//        return rgbaFrame;
//    }
//
//    private void onBlinkDetected() {
//        // When blink is detected, trigger the task
//        if (highlightedTaskIndex >= 0) {
//            String taskName = taskList.get(highlightedTaskIndex);
//            Toast.makeText(this, taskName + " triggered by blink!", Toast.LENGTH_SHORT).show();
//            // Add logic to trigger the task's action
//        }
//    }
//
//    // Example method to update the highlighted task
//    private void updateHighlightedTask(int index) {
//        highlightedTaskIndex = index;
//        taskCardAdapter.highlightTask(index); // Assuming the adapter has this method to highlight task
//    }
//
//    @Override
//    public void onCameraViewStarted(int width, int height) {
//
//    }
//
//    @Override
//    public void onCameraViewStopped() {
//
//    }
//
//
//}






















public class Dashboard extends AppCompatActivity {

    private RecyclerView taskRecyclerView;
    private TaskCardAdapter taskCardAdapter;
    private List<String> taskList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        taskRecyclerView = findViewById(R.id.taskRecyclerView);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        taskRecyclerView.setLayoutManager(layoutManager);

        // Sample tasks
        taskList = new ArrayList<>();
        taskList.add("Drink Water");
        taskList.add("Washroom");
        taskList.add("Bedtime");
        taskList.add("Turn on/off light");
        taskList.add("Fan");
        taskList.add("Medicine Reminder");
        taskList.add("Send Message");
        taskList.add("Emergency Alert");

        taskCardAdapter = new TaskCardAdapter(this, taskList);
        taskRecyclerView.setAdapter(taskCardAdapter);
        setCardSize();
//        int margin = 8; // You can adjust this value as needed
//        taskRecyclerView.addItemDecoration(new ItemOffsetDecoration(margin));
        startHighlightingTasks();
    }

    private void startHighlightingTasks() {
        final Handler handler = new Handler();
        final int delay = 3000; // 3 seconds delay

        Runnable taskHighlightRunnable = new Runnable() {
            int currentTaskIndex = 0;

            @Override
            public void run() {
                // Highlight the current task
                taskCardAdapter.highlightTask(currentTaskIndex);

                // Move to the next task
                currentTaskIndex++;
                if (currentTaskIndex >= taskList.size()) {
                    currentTaskIndex = 0; // Reset to loop infinitely
                }

                // Post the runnable again after delay
                handler.postDelayed(this, delay);
            }
        };

        handler.postDelayed(taskHighlightRunnable, delay);
    }



    private void setCardSize() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
        int screenWidth = displayMetrics.widthPixels;
        int cardWidth = screenWidth / 3;

        int cardHeight = screenHeight/4;

        taskCardAdapter.setCardSize(cardWidth, cardHeight);
    }
}