package com.example.patientapp;

import android.Manifest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, registerButton;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private SharedPreferences sharedPreferences;
    private static final String TAG = "OCVSample::Activity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
//    public static final String CAMERA;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully");
            Toast.makeText(this, "OpenCV initializated", Toast.LENGTH_LONG).show();

        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show();
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA }, CAMERA_PERMISSION_REQUEST_CODE);
        }



        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("patients");

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        String cachedEmail = sharedPreferences.getString("user_email", null);
        String caretakerEmail = sharedPreferences.getString("caretaker_email", null);

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
    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        sharedPreferences.edit().putString("user_email", email).apply();
                        checkPatientCaretaker(email);
                    } else {
                        Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Check if the patient has a linked caretaker
    private void checkPatientCaretaker(String email) {
        String patientKey = email.replace(".", "_");

        databaseReference.child(patientKey).child("caretaker_email").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DataSnapshot snapshot = task.getResult();
                        if (snapshot.exists()) {
                            String caretakerEmail = snapshot.getValue(String.class);
                            sharedPreferences.edit().putString("caretaker_email", caretakerEmail).apply();
                            startActivity(new Intent(MainActivity.this, Dashboard.class));
                        } else {
                            startActivity(new Intent(MainActivity.this, AddCaretakerActivity.class));
                        }
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "Error fetching caretaker data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission Granted.", Toast.LENGTH_LONG).show();

            } else {
                // Permission denied, inform the user
                Toast.makeText(this, "Camera permission is required to use the camera.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
