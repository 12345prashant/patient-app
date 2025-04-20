package com.example.patientapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import com.google.android.material.card.MaterialCardView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;


public class AskForLanguageChatWithAI extends AppCompatActivity implements BlinkDetectionHelper.BlinkListener{
    private List<MaterialCardView> cards;
    private Handler handler = new Handler();
    private int highlightedIndex = 0;
    private BlinkDetectionHelper blinkDetectionHelper;
    private PreviewView previewView;
    private Context context;

    private static final int PERMISSION_REQ_ID = 22;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ask_for_language_chat_with_ai);

        MaterialCardView cardEnglish = findViewById(R.id.card_english);
        MaterialCardView cardHindi = findViewById(R.id.card_hindi);

        cardEnglish.setOnClickListener(v -> launchChatActivity("English"));
        cardHindi.setOnClickListener(v -> launchChatActivity("Hindi"));

        handler = new Handler();
        cards = new ArrayList<>();
        cards.add(cardEnglish);
        cards.add(cardHindi);

        handler.post(highlightRunnable);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userEmail = prefs.getString("user_email", null);
        String patientId = userEmail != null ? userEmail.replace(".", ",") : null;

        context = this;
        previewView = findViewById(R.id.previewView);
        blinkDetectionHelper = new BlinkDetectionHelper(this, previewView, this, patientId);
        if (checkPermissions()) {

//            startVideoCalling();
            blinkDetectionHelper.startCamera(this);
        } else {
            requestPermissions();
        }
    }
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, getRequiredPermissions(), PERMISSION_REQ_ID);
    }
    private boolean checkPermissions() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    private String[] getRequiredPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return new String[]{
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.READ_PHONE_STATE,
                    android.Manifest.permission.BLUETOOTH_CONNECT
            };
        } else {
            return new String[]{
                    android.Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
            };
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID && checkPermissions()) {

//            startVideoCalling();
            blinkDetectionHelper.startCamera(this);
        }
    }
    private void highlightCard(MaterialCardView card, boolean highlight) {
        if (highlight) {
            // Highlight the card (e.g., change background color or stroke color)
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.warning)); // Use a highlight color
            card.setStrokeWidth(4); // Add a border
            card.setStrokeColor(ContextCompat.getColor(this, R.color.warning)); // Use a stroke color
        } else {
            // Reset the card to its original state
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white)); // Use the default card color
            card.setStrokeWidth(0); // Remove the border
        }
    }

    private Runnable highlightRunnable = new Runnable() {
        @Override
        public void run() {
            // Reset all cards to their default state
            resetAllCards(cards);

            // Move to the next card
            highlightedIndex = (highlightedIndex + 1) % cards.size();
            // Highlight the current card
            highlightCard(cards.get(highlightedIndex), true);



            // Repeat after a delay (e.g., 3 seconds)
            handler.postDelayed(this, 3000); // 3000ms = 3 seconds
        }
    };

    private void resetAllCards(List<MaterialCardView> cards) {
        for (MaterialCardView card : cards) {
            highlightCard(card, false);
        }
    }
    @Override
    public void onBlinkDetected() {
        cards.get(highlightedIndex).performClick();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (blinkDetectionHelper != null) {
            // Only pause detection if we're not finishing
            if (!isFinishing()) {
                blinkDetectionHelper.pauseDetection();
            } else {
                blinkDetectionHelper.shutdown();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissions() && blinkDetectionHelper != null) {
            if (!blinkDetectionHelper.isCameraRunning()) {
                blinkDetectionHelper.startCamera(this);
            } else {
                blinkDetectionHelper.resumeDetection(this);
            }
        }
    }

    private void launchChatActivity(String selectedLanguage) {
        Intent intent = new Intent(AskForLanguageChatWithAI.this, ChatWithAI.class);
        intent.putExtra("selected_language", selectedLanguage);
        startActivity(intent);
    }
    protected void onDestroy() {

        super.onDestroy();
        blinkDetectionHelper.shutdown();

    }
}