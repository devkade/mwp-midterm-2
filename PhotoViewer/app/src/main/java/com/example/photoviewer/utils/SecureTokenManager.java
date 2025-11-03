package com.example.photoviewer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecureTokenManager {
    private static final String TAG = "SecureTokenManager";
    private static final String PREFS_NAME = "auth_prefs";
    private static final String TOKEN_KEY = "auth_token";
    private static final String USERNAME_KEY = "remembered_username";
    private static final String LAST_ACTIVE_TIME_KEY = "last_active_time";
    // NOTE: session_active is now stored in PhotoViewerApplication (volatile memory)

    private static SecureTokenManager instance;
    private SharedPreferences encryptedPrefs;

    private SecureTokenManager(Context context) throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        this.encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    public static void initialize(Context context) throws GeneralSecurityException, IOException {
        if (instance == null) {
            instance = new SecureTokenManager(context);
        }
    }

    public static SecureTokenManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("SecureTokenManager not initialized. Call initialize() first.");
        }
        return instance;
    }

    public void saveToken(String token) {
        encryptedPrefs.edit().putString(TOKEN_KEY, token).apply();
    }

    public String getToken() {
        return encryptedPrefs.getString(TOKEN_KEY, null);
    }

    public boolean hasToken() {
        return getToken() != null;
    }

    public void deleteToken() {
        encryptedPrefs.edit().remove(TOKEN_KEY).apply();
    }

    public void saveUsername(String username) {
        encryptedPrefs.edit().putString(USERNAME_KEY, username).apply();
    }

    public String getUsername() {
        return encryptedPrefs.getString(USERNAME_KEY, "");
    }

    public void deleteUsername() {
        encryptedPrefs.edit().remove(USERNAME_KEY).apply();
    }

    public void clearAll() {
        encryptedPrefs.edit().clear().apply();
    }

    // Session management methods for session reset feature
    // NOTE: setSessionActive/isSessionActive moved to PhotoViewerApplication (volatile memory)

    public void setLastActiveTime(long timestamp) {
        encryptedPrefs.edit().putLong(LAST_ACTIVE_TIME_KEY, timestamp).apply();
    }

    public long getLastActiveTime() {
        return encryptedPrefs.getLong(LAST_ACTIVE_TIME_KEY, 0);
    }

    public boolean hasSessionData() {
        // Check if any session data exists (token or timestamp)
        return encryptedPrefs.contains(LAST_ACTIVE_TIME_KEY) || hasToken();
    }

    public void clearSession() {
        // Log state before clearing
        Log.d(TAG, "=== CLEARING SESSION ===");
        Log.d(TAG, "Before clear - last_active_time: " + getLastActiveTime());
        Log.d(TAG, "Before clear - has token: " + hasToken());

        // Clear all session data (token, username, and timestamps)
        // NOTE: session_active is in PhotoViewerApplication (volatile), no need to clear
        encryptedPrefs.edit()
                .remove(TOKEN_KEY)
                .remove(USERNAME_KEY)
                .remove(LAST_ACTIVE_TIME_KEY)
                .apply();

        // Log state after clearing
        Log.d(TAG, "After clear - last_active_time: " + getLastActiveTime());
        Log.d(TAG, "After clear - has token: " + hasToken());
        Log.d(TAG, "=====================");
    }
}
