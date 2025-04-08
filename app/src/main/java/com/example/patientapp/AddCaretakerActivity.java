package com.example.patientapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddCaretakerActivity extends AppCompatActivity {

    private EditText caretakerEmailEditText;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private String patientEmail;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_caretaker);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Initialize UI elements
        TextView patientEmailText = findViewById(R.id.patientEmailText);
        caretakerEmailEditText = findViewById(R.id.caretakerEmailEditText);
        Button addCaretakerButton = (Button)findViewById(R.id.addCaretakerButton);
        Button logoutButton = (Button)findViewById(R.id.button3);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        // Get current patient email
        if (currentUser != null) {
            patientEmail = currentUser.getEmail();
            String s = "Patient: " + patientEmail;
            patientEmailText.setText(s);
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initialize Firebase Database
        databaseReference = FirebaseDatabase.getInstance().getReference("patients");

        // Add caretaker when button is clicked
        addCaretakerButton.setOnClickListener(v -> addCaretaker());

        // Logout button click listener
        logoutButton.setOnClickListener(v -> logoutUser());
    }

    private void addCaretaker() {
        String caretakerEmail = caretakerEmailEditText.getText().toString().trim();

        if (caretakerEmail.isEmpty()) {
            Toast.makeText(this, "Please enter a caretaker email", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert emails to Firebase-friendly format (replace "." with "_")
        String patientKey = patientEmail.replace(".", "_");
        String caretakerKey = caretakerEmail.replace(".", "_");

        // Check if the caretaker exists in Firebase
        DatabaseReference caretakerRef = FirebaseDatabase.getInstance().getReference("caretakers").child(caretakerKey);
        caretakerRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    // Caretaker exists, link the patient to this caretaker
                    saveCaretaker(patientKey, caretakerEmail);
                } else {
                    // Caretaker does not exist
                    Toast.makeText(AddCaretakerActivity.this, "Caretaker not found! Check the email.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(AddCaretakerActivity.this, "Error checking caretaker", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveCaretaker(String patientKey, String caretakerEmail) {
        databaseReference.child(patientKey).child("caretaker_email").setValue(caretakerEmail)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(AddCaretakerActivity.this, "Caretaker added successfully!", Toast.LENGTH_SHORT).show();
                        sharedPreferences.edit().putString("caretaker_email", caretakerEmail).apply();
                        startActivity(new Intent(AddCaretakerActivity.this, Dashboard.class));
                        finish();
                    } else {
                        Toast.makeText(AddCaretakerActivity.this, "Failed to add caretaker", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void logoutUser() {
        // Clear SharedPreferences (remove cached email and caretaker info)
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("user_email"); // Remove user email
        editor.remove("caretaker_email"); // Remove caretaker email
        editor.apply();  // Save changes

//        Sign out from Firebase
        mAuth.signOut();

        // Redirect to MainActivity (login screen)
        Intent intent = new Intent(AddCaretakerActivity.this, MainActivity.class);
        startActivity(intent);
        finish();  // Close AddCaretakerActivity
    }
}
