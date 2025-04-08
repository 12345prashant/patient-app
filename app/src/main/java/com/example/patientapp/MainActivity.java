package com.example.patientapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class MainActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, registerButton;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private SharedPreferences sharedPreferences;
    private LiveStreamService liveStreamService;
    private static final int PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


//        liveStreamService = new LiveStreamService(this);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("patients");

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        String cachedEmail = sharedPreferences.getString("user_email", null);
        String caretakerEmail = sharedPreferences.getString("caretaker_email", null);
//        assert caretakerEmail != null;
//        Log.e("hehe", caretakerEmail);

        // Auto-login if email is cached
        if (cachedEmail != null) {

            if (caretakerEmail != null) {
                startActivity(new Intent(MainActivity.this, Dashboard.class));

            } else {
                startActivity(new Intent(MainActivity.this, AddCaretakerActivity.class));
            }
            finish(); // Close MainActivity after redirection
        }

        // Bind UI elements
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        // Login button click listener
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            loginUser(email, password);
        });

        // Register button click listener
        registerButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, Register.class));
            finish();
        });
    }




    // Function to handle user login
//    private void loginUser(String email, String password) {
//        String caretakerEmail = sharedPreferences.getString("caretaker_email", null);
//        mAuth.signInWithEmailAndPassword(email, password)
//                .addOnCompleteListener(this, task -> {
//                    if (task.isSuccessful()) {
//                        sharedPreferences.edit().putString("user_email", email).apply();
////                        checkPatientCaretaker(email);
//                        if (caretakerEmail != null) {
//                            startActivity(new Intent(MainActivity.this, Dashboard.class));
//                            finish();
//
//                        } else {
//                            startActivity(new Intent(MainActivity.this, AddCaretakerActivity.class));
//                        }
//                    } else {
//                        Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Save the user's email to SharedPreferences
                        sharedPreferences.edit().putString("user_email", email).apply();

                        // Check Firebase to see if the user has a linked caretaker
                        checkPatientCaretaker(email);
                    } else {
                        Toast.makeText(MainActivity.this, "Invalid email or Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Check if the patient has a linked caretaker
//    private void checkPatientCaretaker(String email) {
//        String patientKey = email.replace(".", "_");
//
//        databaseReference.child(patientKey).child("caretaker_email").get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        DataSnapshot snapshot = task.getResult();
//                        if (snapshot.exists()) {
//                            String caretakerEmail = snapshot.getValue(String.class);
//                            sharedPreferences.edit().putString("caretaker_email", caretakerEmail).apply();
//                            startActivity(new Intent(MainActivity.this, Dashboard.class));
//                        } else {
//                            startActivity(new Intent(MainActivity.this, AddCaretakerActivity.class));
//                        }
//                        finish();
//                    } else {
//                        Toast.makeText(MainActivity.this, "Error fetching caretaker data", Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }

    private void checkPatientCaretaker(String email) {
        String patientKey = email.replace(".", "_");

        // Reference to the patient's node in Firebase
        DatabaseReference patientRef = databaseReference.child(patientKey);

        // Fetch the caretaker_email field from Firebase
        patientRef.child("caretaker_email").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    // Caretaker is linked, save the caretaker's email to SharedPreferences
                    String caretakerEmail = snapshot.getValue(String.class);
                    sharedPreferences.edit().putString("caretaker_email", caretakerEmail).apply();

                    // Redirect to Dashboard
                    startActivity(new Intent(MainActivity.this, Dashboard.class));
                } else {
                    // No caretaker linked, redirect to AddCaretakerActivity
                    startActivity(new Intent(MainActivity.this, AddCaretakerActivity.class));
                }
                finish(); // Close MainActivity
            } else {
                // Handle Firebase error
                Toast.makeText(MainActivity.this, "Error fetching caretaker data", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
