package com.example.chatgptglass;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class ChatActivity extends Activity {
    private static final String TAG = "ChatActivity";

    private TextView tvResponse;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        tvResponse = findViewById(R.id.tvResponse);

        // Check if device supports speech recognition.
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech Recognition is not available on this device!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initialize TextToSpeech.
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.ERROR) {
                    Log.e(TAG, "TextToSpeech initialization failed!");
                } else {
                    tts.setLanguage(Locale.US);
                }
            }
        });

        // Initialize SpeechRecognizer and its listener.
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) { }

            @Override
            public void onBeginningOfSpeech() { }

            @Override
            public void onRmsChanged(float rmsdB) { }

            @Override
            public void onBufferReceived(byte[] buffer) { }

            @Override
            public void onEndOfSpeech() { }

            @Override
            public void onError(int error) {
                if (error == SpeechRecognizer.ERROR_NETWORK || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) {
                    Toast.makeText(ChatActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                } else if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                    Toast.makeText(ChatActivity.this, "No speech input. Please try again.", Toast.LENGTH_SHORT).show();
                } else if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    Toast.makeText(ChatActivity.this, "Recognition service is busy. Please try again.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ChatActivity.this, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                }
                startVoiceRecognition(); // Retry recognition on error
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && matches.size() > 0) {
                    String question = matches.get(0);
                    String jsonPayload = String.format("{\"prompt\": \"%s\", \"max_tokens\": 60}", question);
                    new PostTask().execute("https://api.openai.com/v1/engines/davinci-codex/completions", jsonPayload);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) { }

            @Override
            public void onEvent(int eventType, Bundle params) { }
        });
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startVoiceRecognition();
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.example.chatgptglass");
        recognizer.startListening(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (recognizer != null) {
            recognizer.destroy();
        }
    }

    private class PostTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String urlString = params[0];
            String data = params[1];

            OutputStream out = null;
            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Authorization", "Bearer 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");
                out = new BufferedOutputStream(urlConnection.getOutputStream());

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                writer.write(data);
                writer.flush();
                writer.close();
                out.close();

                urlConnection.connect();

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                return readStream(in);
            } catch (Exception e) {
                Log.e(TAG, "Error in PostTask", e);
                return null;
            }
        }

        private String readStream(InputStream in) {
            BufferedReader reader = null;
            StringBuilder response = new StringBuilder();
            try {
                reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading InputStream", e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing BufferedReader", e);
                    }
                }
            }
            return response.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                tvResponse.setText(result);
                HashMap<String, String> params = new HashMap<>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "GPT3_ANSWER");
                tts.speak(result, TextToSpeech.QUEUE_FLUSH, params);
            } else {
                Toast.makeText(ChatActivity.this, "Error fetching response from GPT-3", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
