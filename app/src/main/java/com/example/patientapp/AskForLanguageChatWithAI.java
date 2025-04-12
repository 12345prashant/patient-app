package com.example.patientapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class AskForLanguageChatWithAI extends AppCompatActivity {

    Button btnEnglish, btnHindi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ask_for_language_chat_with_ai);

        btnEnglish = findViewById(R.id.btn_english);
        btnHindi = findViewById(R.id.btn_hindi);

        btnEnglish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchChatActivity("English");
            }
        });

        btnHindi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchChatActivity("Hindi");
            }
        });
    }

    private void launchChatActivity(String selectedLanguage) {
        Intent intent = new Intent(AskForLanguageChatWithAI.this, ChatWithAI.class);
        intent.putExtra("selected_language", selectedLanguage);
        startActivity(intent);
    }
}
