package com.example.chatgptglass;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends Activity {
    private static final String TAG = "ChatActivity";
    private TextView tvResponse;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private ToneGenerator toneGen;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        tvResponse = findViewById(R.id.tvResponse);
        mediaPlayer = MediaPlayer.create(this, R.raw.waiting);
        mediaPlayer.setLooping(true);
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.US);
            }
        });
        toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100); // 100 = max volume

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) { }

            @Override
            public void onBeginningOfSpeech() {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP);

            }

            @Override
            public void onRmsChanged(float rmsdB) { }

            @Override
            public void onBufferReceived(byte[] buffer) { }

            @Override
            public void onEndOfSpeech() { }

            @Override
            public void onError(int error) { }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && matches.size() > 0) {
                    toneGen.startTone(ToneGenerator.TONE_PROP_ACK);

                    String question = matches.get(0);
                    JSONObject jsonPayload = new JSONObject();
                    try {
                        JSONArray jsonArray = new JSONArray();
                        jsonArray.put(new JSONObject().put("role", "user").put("content", question));
                        jsonPayload.put("model", "gpt-3.5-turbo");
                        jsonPayload.put("messages", jsonArray);
                        jsonPayload.put("temperature", 0.7);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String jsonString = jsonPayload.toString();
                    new PostTask().execute("https://api.openai.com/v1/chat/completions", jsonString);
                    mediaPlayer.start();

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
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.example.chatgptglass");
            recognizer.startListening(intent);
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    private class PostTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String urlString = params[0];
            String data = params[1];

            try {
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

                            @Override
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new java.security.cert.X509Certificate[]{};
                            }
                        }
                };

                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

                OkHttpClient.Builder newBuilder = new OkHttpClient.Builder();
                newBuilder.sslSocketFactory(new TLSSocketFactory(), (X509TrustManager) trustAllCerts[0]);
                newBuilder.hostnameVerifier((hostname, session) -> true);
                OkHttpClient newClient = newBuilder.build();

                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), data);
                Request request = new Request.Builder()
                        .url(urlString)
                        .post(requestBody)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", "Bearer " + Secrets.API_KEY)
                        .build();
                Response response = newClient.newCall(request).execute();

                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unsuccessful HTTP Response Code: " + response.code());
                    Log.e(TAG, "Unsuccessful HTTP Response Message: " + response.message());
                    return null;
                }

                String responseBody = response.body().string();
                Log.i(TAG, "Successful HTTP Response: " + responseBody);
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP);

                return responseBody;

            } catch (Exception e) {
                Log.e(TAG, "Error in PostTask: " + e.getMessage(), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
            if (result != null) {
                try {
                    JSONObject jsonResponse = new JSONObject(result);
                    String output = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                    tvResponse.setText(output);

                    // Prepare parameters for TextToSpeech
                    HashMap<String, String> params = new HashMap<>();
                    params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "GPT3_ANSWER");

                    // Use the speak method compatible with pre-Lollipop devices
                    tts.speak(output, TextToSpeech.QUEUE_FLUSH, params);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e(TAG, "Error fetching response from GPT-3");
            }
        }
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
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
}
