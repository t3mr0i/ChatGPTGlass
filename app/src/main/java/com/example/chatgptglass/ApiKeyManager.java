package com.example.chatgptglass;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * A manager class to handle the OpenAI API key.
 * This class prioritizes checking multiple sources for the API key in this order:
 * 1. Memory cache (fastest)
 * 2. Secrets.java file (if available)
 * 3. SharedPreferences (from QR code scan)
 */
public class ApiKeyManager {
    private static final String TAG = "ApiKeyManager";
    private static final String PREF_NAME = "ChatGPTGlassPrefs";
    private static final String API_KEY_PREF = "openai_api_key";
    
    private static String apiKey;
    private static boolean secretsChecked = false;
    
    /**
     * Set the API key at runtime (e.g., from QR code scan)
     * @param key The OpenAI API key
     */
    public static void setApiKey(String key) {
        apiKey = key;
    }
    
    /**
     * Get the API key using a prioritized approach:
     * 1. Memory cache
     * 2. Secrets.java file (if available)
     * 3. SharedPreferences (from QR code scan)
     * 
     * @param context Context needed to access SharedPreferences
     * @return The OpenAI API key, or null if not set in any source
     */
    public static String getApiKey(Context context) {
        // First try memory cache for best performance
        if (apiKey != null && !apiKey.isEmpty()) {
            return apiKey;
        }
        
        // If we haven't checked for Secrets class yet, try that
        if (!secretsChecked) {
            try {
                // Try to get API key from Secrets class using reflection
                Class<?> secretsClass = Class.forName("com.example.chatgptglass.Secrets");
                java.lang.reflect.Field apiKeyField = secretsClass.getField("API_KEY");
                String secretsApiKey = (String) apiKeyField.get(null);
                
                if (secretsApiKey != null && !secretsApiKey.isEmpty() && 
                    !secretsApiKey.equals("paste_your_openai_api_key_here")) {
                    Log.d(TAG, "Using API key from Secrets.java");
                    apiKey = secretsApiKey;
                    secretsChecked = true;
                    return apiKey;
                }
            } catch (Exception e) {
                // Secrets class not found or other error, fall back to SharedPreferences
                Log.d(TAG, "No valid Secrets.java found: " + e.getMessage());
            }
            secretsChecked = true;
        }
        
        // Finally, try to get from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedKey = prefs.getString(API_KEY_PREF, null);
        
        if (savedKey != null && !savedKey.isEmpty()) {
            Log.d(TAG, "Using API key from SharedPreferences");
            apiKey = savedKey;
            return apiKey;
        }
        
        return null;
    }
    
    /**
     * Check if an API key is available from any source
     * @param context Context needed to access SharedPreferences
     * @return true if an API key exists, false otherwise
     */
    public static boolean hasApiKey(Context context) {
        return getApiKey(context) != null;
    }
    
    /**
     * Check if the API key is available specifically from the Secrets.java file
     * @return true if the Secrets.java file contains a valid API key
     */
    public static boolean hasSecretsApiKey() {
        if (!secretsChecked) {
            try {
                Class<?> secretsClass = Class.forName("com.example.chatgptglass.Secrets");
                java.lang.reflect.Field apiKeyField = secretsClass.getField("API_KEY");
                String secretsApiKey = (String) apiKeyField.get(null);
                
                secretsChecked = true;
                return secretsApiKey != null && !secretsApiKey.isEmpty() && 
                       !secretsApiKey.equals("paste_your_openai_api_key_here");
            } catch (Exception e) {
                secretsChecked = true;
                return false;
            }
        }
        return apiKey != null && !apiKey.isEmpty();
    }
}
