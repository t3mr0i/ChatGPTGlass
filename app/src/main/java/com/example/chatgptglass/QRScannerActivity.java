package com.example.chatgptglass;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

/**
 * QR Scanner Activity for Google Glass Explorer Edition
 * Uses ZXing for barcode scanning, which is compatible with Glass
 */
public class QRScannerActivity extends Activity {
    private static final String TAG = "QRScannerActivity";
    private static final String PREF_NAME = "ChatGPTGlassPrefs";
    private static final String API_KEY_PREF = "openai_api_key";
    
    private GestureDetector mGestureDetector;
    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);
        
        statusTextView = findViewById(R.id.tvScanInstructions);
        
        // Setup Glass gesture detector
        mGestureDetector = createGestureDetector();
        
        // Check if we already have an API key from any source (Secrets.java or SharedPreferences)
        if (ApiKeyManager.hasApiKey(this)) {
            // If API key exists, go directly to ChatActivity
            // ApiKeyManager already prioritizes Secrets.java over SharedPreferences
            Log.d(TAG, "Found existing API key, skipping QR scanner");
            startChatActivity();
            return;
        }
        
        // If we have a secrets file but no stored key, try to use it directly
        if (ApiKeyManager.hasSecretsApiKey()) {
            Log.d(TAG, "Found API key in Secrets.java, using it directly");
            startChatActivity();
            return;
        }
        
        // Start QR scanning
        startQRScanner();
    }
    
    private void startQRScanner() {
        statusTextView.setText("Tap to start QR code scanning");
    }
    
    private void initiateScan() {
        statusTextView.setText("Scanning for QR code...");
        
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan a QR code with your API key");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                // QR code successfully scanned
                String apiKey = result.getContents();
                Log.d(TAG, "Scanned API key: " + apiKey.substring(0, 5) + "...");
                
                statusTextView.setText("API Key detected! Saving...");
                saveApiKey(apiKey);
            } else {
                // User cancelled the scan
                statusTextView.setText("Scan cancelled. Tap to try again.");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    private void saveApiKey(String apiKey) {
        // Store the API key in SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(API_KEY_PREF, apiKey);
        editor.apply();
        
        // Set the API key in the manager
        ApiKeyManager.setApiKey(apiKey);
        
        // Show success message and start ChatActivity after a delay
        statusTextView.setText("API Key saved successfully!");
        new Handler().postDelayed(this::startChatActivity, 2000);
    }
    
    private void startChatActivity() {
        Intent intent = new Intent(this, ChatActivity.class);
        startActivity(intent);
        finish();
    }
    
    private GestureDetector createGestureDetector() {
        return new GestureDetector(this)
                .setBaseListener(gesture -> {
                    if (gesture == Gesture.TAP) {
                        // Start scanning on tap
                        initiateScan();
                        return true;
                    } else if (gesture == Gesture.SWIPE_DOWN) {
                        finish();
                        return true;
                    }
                    return false;
                });
    }
    
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return super.onGenericMotionEvent(event);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            // Center button tap - start scanning
            initiateScan();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
