package com.example.chatgptglass;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorEventListener;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    private static final int SPEECH_REQUEST_CODE = 0;

    private TextView tvResponse;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private ToneGenerator toneGen;
    private MediaPlayer mediaPlayerWaiting;
    private MediaPlayer mediaPlayerTapPrompt;
    private MediaPlayer mediaPlayerResultsDone;
    private GestureDetector detector;
    private boolean allowScroll = false;
    private ProgressBar progressBar;
    private Handler autoScrollHandler;
    private Runnable autoScrollRunnable;
    private final int SCROLL_DELAY_MS = 500; // Scroll every 500 milliseconds
    private final int SCROLL_AMOUNT_PX = 25; // Scroll by 22 pixels


    private ScrollView scrollView;

    @Override
    protected void onPause() {
        super.onPause();
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.destroy();
            recognizer = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new MyRecognitionListener());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        tvResponse = findViewById(R.id.tvResponse);
        progressBar = findViewById(R.id.progressBar);
        scrollView = findViewById(R.id.scrollView);

        mediaPlayerWaiting = MediaPlayer.create(this, R.raw.waiting);
        mediaPlayerTapPrompt = MediaPlayer.create(this, R.raw.tap_prompt);
        mediaPlayerResultsDone = MediaPlayer.create(this, R.raw.results_done);
        mediaPlayerWaiting.setLooping(true);

        detector = createGestureDetector(this);

        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.US);
            }
        });
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                // This is called when the speech starts.
                // Start auto-scrolling here.
                autoScrollHandler.post(autoScrollRunnable);
            }

            @Override
            public void onDone(String utteranceId) {
                // This is called when the speech finishes.
                // Stop auto-scrolling here.
                autoScrollHandler.removeCallbacks(autoScrollRunnable);
            }

            @Override
            public void onError(String utteranceId) {
                // This is called when there is an error.
                // Stop auto-scrolling here.
                autoScrollHandler.removeCallbacks(autoScrollRunnable);
            }
        });

        toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 78); // 100 = max volume

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new MyRecognitionListener());

        autoScrollHandler = new Handler();
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                int newScrollYPosition = scrollView.getScrollY() + SCROLL_AMOUNT_PX;
                scrollView.smoothScrollTo(0, newScrollYPosition);
                autoScrollHandler.postDelayed(this, SCROLL_DELAY_MS);
            }
        };
    }


    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);
        // Create the listener for the GestureDetector
        gestureDetector.setBaseListener(gesture -> {
            // Implement scrolling
            if (gesture == Gesture.SWIPE_RIGHT) {
                if (allowScroll) {
                    // Move TextView scroll down
                    tvResponse.scrollBy(0, 60);
                } else {
                    finish();
                }
                return true;
            } else if (gesture == Gesture.SWIPE_LEFT) {
                if (allowScroll) {
                    tvResponse.scrollBy(0, -60);
                }
                return true;
            } else if (gesture == Gesture.TAP) {
                mediaPlayerTapPrompt.start();
                tts.stop();
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                startActivityForResult(intent, SPEECH_REQUEST_CODE);
                allowScroll = false;

                return true;
            }
            return false;
        });
        return gestureDetector;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (detector != null) {
            return detector.onMotionEvent(event);
        }
        return super.onGenericMotionEvent(event);
    }

    private class MyRecognitionListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onError(int error) {
        }

        @Override
        public void onResults(Bundle results) {
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && matches.size() > 0) {

                String question = matches.get(0);
                if (question.startsWith("ok glass")) {
                    // Detected a nod!
                    tts.stop();
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    startActivityForResult(intent, SPEECH_REQUEST_CODE);
                    allowScroll = false;
                } else {
                    JSONObject jsonPayload = new JSONObject();
                    try {
                        JSONArray jsonArray = new JSONArray();
                        jsonArray.put(new JSONObject().put("role", "user").put("content", question));
                        jsonPayload.put("model", "gpt-3.5-turbo");
                        jsonPayload.put("messages", jsonArray);
                        jsonPayload.put("max_tokens", 60);
                        jsonPayload.put("temperature", 0.7);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String jsonString = jsonPayload.toString();
                    new PostTask().execute("https://api.openai.com/v1/chat/completions", jsonString);
                    progressBar.setVisibility(View.VISIBLE);
                    tvResponse.setVisibility(View.GONE);

                    mediaPlayerWaiting.start();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                            }

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
                return responseBody;

            } catch (Exception e) {
                Log.e(TAG, "Error in PostTask: " + e.getMessage(), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            progressBar.setVisibility(View.GONE);
            mediaPlayerWaiting.pause();
            mediaPlayerWaiting.seekTo(0);
            mediaPlayerResultsDone.start();
            tvResponse.setVisibility(View.VISIBLE);

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

                    // Inject a 14000ms delay before allowing scroll
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            scrollView.fullScroll(View.FOCUS_DOWN);
                            allowScroll = true;
                        }
                    }, 8000);

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
        if (mediaPlayerWaiting != null) {
            mediaPlayerWaiting.release();
        }
        if (mediaPlayerTapPrompt != null) {
            mediaPlayerTapPrompt.release();
        }
        if (mediaPlayerResultsDone != null) {
            mediaPlayerResultsDone.release();
        }
        if (autoScrollHandler != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
    }
}
