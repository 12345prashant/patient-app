package com.example.patientapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.gms.tasks.Task;

public class Register extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, registerButton;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_xml);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("patients");

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        // Bind UI elements
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        registerButton = findViewById(R.id.registerButton);
        loginButton = findViewById(R.id.loginButton);

        // Login button click listener
        loginButton.setOnClickListener(v -> {
            startActivity(new Intent(Register.this, MainActivity.class));
            finish();
        });

        // Register button click listener
        registerButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(Register.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser(email, password);
        });
    }

    private void registerUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Store patient email in Firebase
                        String patientKey = email.replace(".", "_");
                        databaseReference.child(patientKey).setValue(true)
                                .addOnCompleteListener(saveTask -> {
                                    if (saveTask.isSuccessful()) {
                                        // Cache patient email
                                        sharedPreferences.edit().putString("user_email", email).apply();
                                        startActivity(new Intent(Register.this, AddCaretakerActivity.class));
                                        finish();
                                    } else {
                                        Toast.makeText(Register.this, "Failed to save patient data", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(Register.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
