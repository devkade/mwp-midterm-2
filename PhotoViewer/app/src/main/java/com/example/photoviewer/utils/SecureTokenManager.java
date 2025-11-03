package com.example.photoviewer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecureTokenManager {
    private static final String PREFS_NAME = "auth_prefs";
    private static final String TOKEN_KEY = "auth_token";
    private static final String USERNAME_KEY = "remembered_username";

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
}
