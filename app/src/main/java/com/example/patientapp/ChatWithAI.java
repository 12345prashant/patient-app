package com.example.patientapp; // Use your actual package name

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity; // Correct import for Gravity
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast; // For showing error messages
import android.speech.tts.TextToSpeech;
import androidx.annotation.NonNull; // Import for NonNull
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// Imports for Gemini SDK
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.BlockThreshold;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.ai.client.generativeai.type.HarmCategory;
import com.google.ai.client.generativeai.type.SafetySetting;

// Imports for Concurrency (Futures)
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import com.google.android.material.card.MaterialCardView;
import android.view.LayoutInflater;

public class ChatWithAI extends AppCompatActivity implements BlinkDetectionHelper.BlinkListener{

    private static final String TAG = "ChatWithAI_DEBUG"; // Tag for logging
    private Context context;

    // UI Elements
    private Button chatButton; // Consider removing or repurposing this button
    private LinearLayout chatContainer;
    private LinearLayout promptContainer;
    private ScrollView scrollView;
    private TextToSpeech tts;

    // State & Data
    private List<String> currentPrompts = new ArrayList<>();
    private GenerativeModelFutures model;

    // Threading
    // Use a cached thread pool for background tasks (API calls)
    private final Executor backgroundExecutor = Executors.newCachedThreadPool();
    // Handler to post runnable tasks back to the main (UI) thread
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    // Constants for Prompts/Options
    private final String[] INITIAL_PROMPTS = {"Hello", "I need advice", "Latest news updates"};
    private final String[] ERROR_PROMPTS = {"Try again", "Hello", "I need help"};
    private final String[] DEFAULT_RESPONSE_OPTIONS = {"Continue", "Tell me more", "Thanks"};

    private Handler handler1 = new Handler();

    private MaterialCardView cardprompt1, cardprompt2, cardprompt3;
    private List<MaterialCardView> cards;
    private PreviewView previewView;
    private int highlightedIndex = 0;
    private BlinkDetectionHelper blinkDetectionHelper;

    private static final int PERMISSION_REQ_ID = 22;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_with_ai);

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

        String selectedLanguage = getIntent().getStringExtra("selected_language");
        if ("English".equals(selectedLanguage)) {
            initializeTextToSpeechEnglish();
        } else {
            initializeTextToSpeechHindi();
        }


        // --- Securely Get API Key ---
        String apiKey = "AIzaSyB-DKESfggtEiysvoN3h8yssD7qQXo8r8M"; // Read from BuildConfig

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("\"\"")) {
            Log.e(TAG, "API Key not found in BuildConfig. Check local.properties and build.gradle setup.");
            Toast.makeText(this, "API Key Configuration Error!", Toast.LENGTH_LONG).show();
            // Disable AI functionality if key is missing
            // Potentially finish() or disable buttons
            // For now, we proceed but model initialization will fail
        }

        // --- Initialize Gemini Model ---
        // Configure safety settings (adjust as needed, be mindful of blocking)


        // Configure generation parameters


        try {
            GenerativeModel gm = new GenerativeModel(
                    // Use a model suitable for free tier, like flash
                    "gemini-1.5-flash",
                    apiKey // Use the key from BuildConfig

            );
            model = GenerativeModelFutures.from(gm);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing GenerativeModel", e);
            Toast.makeText(this, "Failed to initialize AI Model. Check API Key and config.", Toast.LENGTH_LONG).show();
            // Handle model initialization failure (e.g., disable related UI)
            model = null; // Ensure model is null if init failed
        }


        // --- Initialize UI ---
        chatButton = findViewById(R.id.chat_with_ai_button); // Still unsure about this button's purpose
        chatContainer = findViewById(R.id.chat_container);
        promptContainer = findViewById(R.id.prompt_container);
        scrollView = findViewById(R.id.scroll_view);

        // Start animation (optional)
         animateButton(chatButton); // Uncomment if you have this method and want the animation

        // Set initial prompts (only if model initialized successfully)
        if (model != null) {
            addMessage("System", "Welcome! Select an option below.", true);
            updatePrompts(INITIAL_PROMPTS);
        } else {
            addMessage("System", "AI features are unavailable. Check configuration.", true);
            // Disable UI elements that require the model
            promptContainer.setVisibility(View.GONE);
            chatButton.setEnabled(false);
        }


        // The main chat button's functionality seems unclear in the flow.
        // It currently just adds a message. Consider removing or defining its action.
        chatButton.setOnClickListener(v -> {
            Log.d(TAG, "Main Chat button clicked - Action undefined.");
            Toast.makeText(this, "Please select a prompt.", Toast.LENGTH_SHORT).show();
            // addMessage("Patient", "Started chat with AI", false); // Original action
        });
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

    private void initializeTextToSpeechEnglish() {

        tts = new TextToSpeech(this, status -> {

            if (status == TextToSpeech.SUCCESS) {

                tts.setLanguage(Locale.US);

            }

        });

    }
    private void initializeTextToSpeechHindi() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Define the Locale for Hindi (India)
                // Language code "hi", Country code "IN"
                Locale localeHi = new Locale("hi", "IN");

                // Check if the Hindi language is available (or requires download)
                int result = tts.isLanguageAvailable(localeHi);

                if (result == TextToSpeech.LANG_MISSING_DATA) {
                    Log.w(TAG, "TTS: Hindi language data missing. Setting language anyway, system might prompt for download.");
                    // Attempt to set the language. The system might handle download prompt.
                    int setResult = tts.setLanguage(localeHi);
                    if (setResult == TextToSpeech.LANG_MISSING_DATA || setResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "TTS: Failed to set Hindi language after checking (Missing Data or Not Supported). Falling back to default.");
                        // Optional: Fallback to default or US English if Hindi setup fails
                        // tts.setLanguage(Locale.getDefault());
                        // Or inform the user
                    } else {
                        Log.i(TAG, "TTS: Hindi language set (data was missing, system might download).");
                    }
                } else if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS: Hindi language is not supported on this device/engine.");
                    // Handle the error: Inform the user, disable TTS features, or fallback
                    // Optional: Fallback to US English or default
                    // tts.setLanguage(Locale.US);
                    // tts.setLanguage(Locale.getDefault());
                    Toast.makeText(this, "Hindi speech is not supported on this device.", Toast.LENGTH_LONG).show(); // Example user feedback
                } else {
                    // Language is available (LANG_AVAILABLE, LANG_COUNTRY_AVAILABLE, LANG_COUNTRY_VAR_AVAILABLE)
                    int setResult = tts.setLanguage(localeHi);
                    if (setResult == TextToSpeech.LANG_MISSING_DATA || setResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // This case shouldn't ideally happen if isLanguageAvailable passed, but check anyway
                        Log.e(TAG, "TTS: Failed to set Hindi language even though it seemed available. Result: " + setResult);
                    } else {
                        Log.i(TAG, "TTS: Hindi language successfully set.");
                    }
                }
            } else {
                // Initialization failed
                Log.e(TAG, "TTS Initialization failed! Status: " + status);
                Toast.makeText(this, "Text-to-Speech initialization failed.", Toast.LENGTH_SHORT).show(); // Example user feedback
                // Handle TTS initialization failure (e.g., disable related features)
                tts = null; // Ensure tts is null if init failed
            }
        });

    }

    // Animation function (keep if desired)
    private void animateButton(View view) {
        // Ensure this runs on the main thread if called from elsewhere, though onCreate is fine.
        mainThreadHandler.post(() -> {
            PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.1f, 1f);

            PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.1f, 1f);

            PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.9f, 1f);



            ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY, alpha);

            animator.setDuration(2000);

            animator.setRepeatCount(ObjectAnimator.INFINITE);

            animator.setRepeatMode(ObjectAnimator.REVERSE);

            animator.start();
        });
    }


    // --- UI Update Methods (Ensure they run on Main Thread) ---

    private void updatePrompts(final String[] prompts) {
        mainThreadHandler.post(() -> {
            Log.d(TAG, "Updating prompts on Main Thread");
            promptContainer.removeAllViews();
            currentPrompts.clear();
            MaterialCardView exitCard = (MaterialCardView) LayoutInflater.from(this)
                    .inflate(R.layout.prompt_card, promptContainer, false);

            TextView exitText = exitCard.findViewById(R.id.promptText);
//            ImageView exitIcon = exitCard.findViewById(R.id.promptIcon);

            exitText.setText("Exit");
            exitText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
//            exitIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
//            exitIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark));

            exitCard.setOnClickListener(v -> {
                // Redirect to Dashboard
                startActivity(new Intent(this, Dashboard.class));
                finish();
            });

            promptContainer.addView(exitCard);

            if (prompts == null || prompts.length == 0) {
                Log.w(TAG, "updatePrompts called with no prompts.");
                return;
            }

            currentPrompts.addAll(Arrays.asList(prompts));

            LayoutInflater inflater = LayoutInflater.from(this);
            cards = new ArrayList<>();
            cards.add(exitCard);
            for (String prompt : currentPrompts) {
                if (prompt == null || prompt.trim().isEmpty()) continue;

                // Inflate the card layout
                MaterialCardView card = (MaterialCardView) inflater.inflate(
                        R.layout.prompt_card,
                        promptContainer,
                        false
                );
                cards.add(card);

                // Set up the card content
                TextView promptText = card.findViewById(R.id.promptText);
                promptText.setText(prompt.trim());


                // Set click listener
                card.setOnClickListener(v -> {
                    if (model == null) {
                        Toast.makeText(this, "AI Model not available.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String selectedPrompt = promptText.getText().toString();
                    addMessage("You", selectedPrompt, false);
                    setPromptCardsEnabled(false);
                    processUserInput(selectedPrompt);
                });

                promptContainer.addView(card);
                cards.add(card);
            }
            handler1.post(highlightRunnable);
            setPromptCardsEnabled(true);
        });
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
            handler1.postDelayed(this, 3000); // 3000ms = 3 seconds
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

    private void setPromptCardsEnabled(final boolean enabled) {
        mainThreadHandler.post(() -> {
            Log.d(TAG, "Setting prompt cards enabled: " + enabled + " on Main Thread");
            for (int i = 0; i < promptContainer.getChildCount(); i++) {
                View child = promptContainer.getChildAt(i);
                if (child instanceof MaterialCardView) {
                    child.setEnabled(enabled);
                    child.setAlpha(enabled ? 1.0f : 0.5f);
                }
            }
        });
    }

    private void addMessage(final String sender, final String message, final boolean isAI) {
        // Use handler to ensure UI modification happens on the main thread
        mainThreadHandler.post(() -> {
            if (message == null || message.trim().isEmpty()) {
                Log.w(TAG, "Attempted to add empty message from " + sender);
                return;
            }
            Log.d(TAG, "Adding message from " + sender + " on Main Thread");
            TextView messageView = new TextView(this);
            messageView.setText(sender + ": " + message.trim());

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);

            if (isAI) {
                params.gravity = Gravity.START;
                messageView.setBackgroundResource(R.drawable.ai_message_bubble); // Ensure drawable exists
                messageView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            } else {
                params.gravity = Gravity.END;
                messageView.setBackgroundResource(R.drawable.user_message_bubble); // Ensure drawable exists
                messageView.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            }

            params.setMargins(16, 8, 16, 8);
            messageView.setLayoutParams(params);
            messageView.setPadding(24, 12, 24, 12); // Consistent padding

            chatContainer.addView(messageView);

            // Scroll to bottom smoothly after the view has been added
            scrollView.post(() -> scrollView.smoothScrollTo(0, chatContainer.getBottom()));
        });
    }

    // Helper to enable/disable prompt buttons (runs on Main Thread)
    private void setPromptButtonsEnabled(final boolean enabled) {
        mainThreadHandler.post(() -> {
            Log.d(TAG, "Setting prompt buttons enabled: " + enabled + " on Main Thread");
            for (int i = 0; i < promptContainer.getChildCount(); i++) {
                View child = promptContainer.getChildAt(i);
                if (child instanceof Button) {
                    child.setEnabled(enabled);
                    child.setAlpha(enabled ? 1.0f : 0.5f);
                }
            }
            // Also manage the main chat button if it's active
            // chatButton.setEnabled(enabled);
            // chatButton.setAlpha(enabled ? 1.0f : 0.5f);
        });
    }

    // Helper to remove the last message if it's the "Thinking..." indicator (runs on Main Thread)
    private void removeLastMessageIfThinking() {
        mainThreadHandler.post(() -> {
            int childCount = chatContainer.getChildCount();
            if (childCount > 0) {
                View lastView = chatContainer.getChildAt(childCount - 1);
                if (lastView instanceof TextView) {
                    TextView lastMessageView = (TextView) lastView;
                    // Check content carefully, include sender prefix
                    if (lastMessageView.getText().toString().equals("AI: Thinking...")) {
                        Log.d(TAG, "Removing 'Thinking...' message on Main Thread");
                        chatContainer.removeViewAt(childCount - 1);
                    }
                }
            }
        });
    }


    // --- AI Interaction Logic ---

    private void processUserInput(String input) {
        if (model == null) {
            Log.e(TAG, "processUserInput called but model is null!");
            addMessage("System", "AI is not available.", true);
            setPromptButtonsEnabled(true); // Re-enable buttons
            return;
        }
        String selectedLanguage = getIntent().getStringExtra("selected_language");

        Log.d(TAG, "Processing input: " + input);
        addMessage("AI", "Thinking...", true); // Add thinking indicator immediately (on main thread via addMessage)

        // Construct the prompt for the AI
        String promptText = "You are an AI companion designed to be a supportive friend for a paralyzed patient. " +
                "Your tone should always be warm, patient, understanding, and gently positive. " +
                "The patient said: \"" + input + "\". " +
                "Respond in \""+ selectedLanguage +"\" text only .Acknowledge their input with empathy. Provide a relevant, supportive, and encouraging response, keeping in mind their situation. " +
                "Focus on listening, validating feelings, and offering comfort or engaging in light, positive conversation. " +
                "Avoid medical advice completely. Keep responses clear and reasonably concise. " +
                "While focusing on support, feel free to engage in light conversation about shared interests (like accessible hobbies, stories, news) if the patient seems open to it. " +
                "Conclude by suggesting exactly three short, distinct follow-up options the patient could choose to continue the interaction. " +
                "Strict Format Required:\n" +
                "Response: [Your warm, supportive response here]\n" +
                "Options: [Option 1] | [Option 2] | [Option 3]";

        Content content = new Content.Builder().addText(promptText).build();

        // Asynchronously call the AI model
        ListenableFuture<GenerateContentResponse> responseFuture = model.generateContent(content);

        // Add the callback to handle the response, using the background executor
        Futures.addCallback(responseFuture, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                // **CRITICAL FIX:** Post UI updates back to the main thread
                mainThreadHandler.post(() -> {
                    Log.d(TAG, "onSuccess running on Main Thread");
                    removeLastMessageIfThinking(); // Remove indicator
                    try {
                        String fullResponseText = result.getText();
                        if (fullResponseText != null && !fullResponseText.isEmpty()) {
                            Log.i(TAG, "AI Raw Response: " + fullResponseText);
                            processAIResponse(fullResponseText); // This will call addMessage/updatePrompts (which use handler)


                        } else {
                            Log.w(TAG, "AI response was empty or null. FinishReason: " +
                                    (result.getCandidates() != null && !result.getCandidates().isEmpty() ?
                                            result.getCandidates().get(0).getFinishReason() : "Unknown"));
                            // Check safety ratings if needed: result.getSafetyRatings() or result.getCandidates().get(0).getSafetyRatings()
                            addMessage("AI", "I couldn't generate a response for that request.", true);
                            updatePrompts(ERROR_PROMPTS);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing successful AI response", e);
                        addMessage("AI", "Sorry, I had trouble understanding the response.", true);
                        updatePrompts(ERROR_PROMPTS);
                    } finally {
                        setPromptButtonsEnabled(true); // Re-enable buttons
                    }
                });
            }


            @Override
            public void onFailure(@NonNull Throwable t) {
                // **CRITICAL FIX:** Post UI updates back to the main thread
                mainThreadHandler.post(() -> {
                    Log.e(TAG, "onFailure running on Main Thread", t); // Log the full error trace
                    removeLastMessageIfThinking(); // Remove indicator
                    addMessage("AI", "Sorry, an error occurred. Please check connection and try again.", true);
                    updatePrompts(ERROR_PROMPTS); // Reset prompts
                    setPromptButtonsEnabled(true); // Re-enable buttons
                });
            }
        }, backgroundExecutor); // IMPORTANT: Execute the callback logic itself on the background thread
    }

    // Parses the AI response and updates UI (called from main thread via onSuccess handler)
    private void processAIResponse(String fullResponse) {
        Log.d(TAG, "Processing AI response content on Main Thread");
        String aiMessage = fullResponse.trim(); // Default to full response
        String[] options = DEFAULT_RESPONSE_OPTIONS; // Default options

        int responseMarker = fullResponse.indexOf("Response:");
        int optionsMarker = fullResponse.indexOf("Options:");

        if (responseMarker != -1 && optionsMarker != -1 && optionsMarker > responseMarker) {
            aiMessage = fullResponse.substring(responseMarker + "Response:".length(), optionsMarker).trim();
            speakMessage(aiMessage);
            String optionsPart = fullResponse.substring(optionsMarker + "Options:".length()).trim();
            String[] parsedOptions = optionsPart.split("\\|");
            List<String> cleanOptions = new ArrayList<>();
            for (String opt : parsedOptions) {
                String trimmedOpt = opt.trim();
                if (!trimmedOpt.isEmpty()) {
                    cleanOptions.add(trimmedOpt);
                }
            }
            if (!cleanOptions.isEmpty()) {
                options = cleanOptions.toArray(new String[0]);
            } else {
                Log.w(TAG, "Could not parse options from response: " + optionsPart);
                options = DEFAULT_RESPONSE_OPTIONS; // Fallback
            }
        } else {
            Log.w(TAG, "AI response did not match expected format. Displaying full text.");
            // aiMessage is already the full trimmed response
            options = DEFAULT_RESPONSE_OPTIONS; // Provide default options
        }

        // Add message and update prompts (these methods use the handler internally)
//        addMessage("AI", aiMessage, true);
        updatePrompts(options);
    }
    private void speakMessage(String message) {

        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
    }
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
        blinkDetectionHelper.shutdown();


    }
}