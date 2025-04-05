package com.example.patientapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.card.MaterialCardView;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Settings");

        // Get caretaker email from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String caretakerEmail = sharedPreferences.getString("caretaker_email", "No caretaker assigned");

        // Display caretaker email
        TextView caretakerEmailText = findViewById(R.id.caretaker_email_text);
        caretakerEmailText.setText(caretakerEmail);

        // Setup add caretaker button
        MaterialCardView addCaretakerBtn = findViewById(R.id.add_caretaker_btn);
        addCaretakerBtn.setOnClickListener(v -> {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            startActivity(new Intent(SettingsActivity.this, AddCaretakerActivity.class));

        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}