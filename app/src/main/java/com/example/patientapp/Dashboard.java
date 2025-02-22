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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Dashboard extends AppCompatActivity {



  private ImageButton sendMessage ;
  private Button logoutbutton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

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
