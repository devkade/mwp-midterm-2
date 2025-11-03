package com.example.photoviewer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.example.photoviewer.services.SessionManager;
import com.example.photoviewer.utils.SecureTokenManager;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DURATION = 2000; // 2 seconds
    private static final long SESSION_TIMEOUT_MS = 600000; // 10 minutes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize SecureTokenManager on app startup (earliest point)
        try {
            SecureTokenManager.initialize(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Check session validity BEFORE any navigation decision
        checkAndClearInvalidSession();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent nextActivity;
            if (SessionManager.getInstance().isLoggedIn()) {
                nextActivity = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                nextActivity = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(nextActivity);
            finish();
        }, SPLASH_DURATION);
    }

    /**
     * Check if session should be cleared due to:
     * 1. Process death (sessionActive is false but we have a token - means process was killed)
     * 2. Inactivity timeout (> 10 minutes since last active)
     */
    private void checkAndClearInvalidSession() {
        Log.d(TAG, "Checking session validity on app startup");

        // If no session data exists, nothing to clear
        if (!SecureTokenManager.getInstance().hasSessionData()) {
            Log.d(TAG, "No session data exists - first launch or already cleared");
            return;
        }

        // Check 1: Did the process die?
        // sessionActive is volatile (in-memory only). On new process start, it's always false.
        // If we have a token but sessionActive=false, the process must have been killed.
        boolean sessionActive = PhotoViewerApplication.isSessionActive();
        boolean hasToken = SecureTokenManager.getInstance().hasToken();

        if (hasToken && !sessionActive) {
            Log.d(TAG, "Process death detected (has token but sessionActive=false) - clearing session");
            SecureTokenManager.getInstance().clearSession();
            return;
        }

        // Check 2: Check for inactivity timeout (if process is still alive)
        long lastActiveTime = SecureTokenManager.getInstance().getLastActiveTime();
        long currentTime = System.currentTimeMillis();
        long timeSinceActive = currentTime - lastActiveTime;

        Log.d(TAG, "Last active: " + lastActiveTime + ", Current: " + currentTime +
              ", Elapsed: " + (timeSinceActive / 1000) + "s");

        if (timeSinceActive > SESSION_TIMEOUT_MS) {
            Log.d(TAG, "Inactivity timeout exceeded (" + (timeSinceActive / 1000) + "s > 600s) - clearing session");
            SecureTokenManager.getInstance().clearSession();
        } else {
            Log.d(TAG, "Session still valid - keeping session");
        }
    }
}
