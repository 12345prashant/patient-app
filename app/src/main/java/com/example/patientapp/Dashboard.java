package com.example.patientapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import com.google.firebase.database.DatabaseError;
import android.os.Bundle;
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
public class Dashboard extends AppCompatActivity {

<<<<<<< HEAD
    private RecyclerView taskRecyclerView;
    private TaskCardAdapter taskCardAdapter;
    private List<String> taskList;
=======


  private ImageButton sendMessage ;
  private Button logoutbutton;
    private FirebaseAuth mAuth;
>>>>>>> 66607dfcde96434f1e3400c42cb93c2075c83e93

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

<<<<<<< HEAD
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
=======
        sendMessage = findViewById(R.id.imageButton);
        logoutbutton = findViewById(R.id.button2);
//         Initialize Firebase Auth
                mAuth = FirebaseAuth.getInstance();

        sendMessage.setOnClickListener((v -> opensendmessageactivity()));
        logoutbutton.setOnClickListener((v-> logoutUser()));





    }
    private void opensendmessageactivity(){
        Intent intent = new Intent(Dashboard.this, SendMessage.class);
        startActivity(intent);
        finish();
    }
    private void logoutUser() {
        // Clear the cached email from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        sharedPreferences.edit().remove("user_email").apply();

        // Sign out from Firebase
        mAuth.signOut();

        // Redirect to MainActivity (Login screen)
        Intent intent = new Intent(Dashboard.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

}
>>>>>>> 66607dfcde96434f1e3400c42cb93c2075c83e93
