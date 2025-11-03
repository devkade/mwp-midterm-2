package com.example.photoviewer;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.example.photoviewer.utils.SecureTokenManager;

public class PhotoViewerApplication extends Application {
    private static final String TAG = "PhotoViewerApplication";
    private static final long SESSION_TIMEOUT_MS = 600000; // 10 minutes

    // Volatile session state - resets to false when process dies
    private static boolean sessionActive = false;

    private int activeActivityCount = 0;

    /**
     * Get current session active state (volatile - survives only while process is alive)
     */
    public static boolean isSessionActive() {
        return sessionActive;
    }

    /**
     * Set session active state (volatile - will reset to false when process dies)
     */
    public static void setSessionActive(boolean active) {
        Log.d(TAG, "Setting sessionActive to: " + active);
        sessionActive = active;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application onCreate called");

        // Initialize SecureTokenManager
        try {
            SecureTokenManager.initialize(this);
            // Log initial state on app startup
            // NOTE: sessionActive is always false on process start (volatile memory)
            long initialLastActive = SecureTokenManager.getInstance().getLastActiveTime();
            boolean hasToken = SecureTokenManager.getInstance().hasToken();
            Log.d(TAG, "=== APP STARTUP STATE ===");
            Log.d(TAG, "Initial session_active: " + sessionActive + " (always false on new process)");
            Log.d(TAG, "Initial last_active_time: " + initialLastActive);
            Log.d(TAG, "Has token: " + hasToken);
            Log.d(TAG, "======================");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize SecureTokenManager", e);
        }

        // Register ActivityLifecycleCallbacks to track app lifecycle
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                Log.d(TAG, "onActivityCreated: " + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityStarted(Activity activity) {
                activeActivityCount++;
                Log.d(TAG, "onActivityStarted: " + activity.getClass().getSimpleName() +
                      " (active count: " + activeActivityCount + ")");

                // Log state before setting session_active
                Log.d(TAG, "BEFORE setSessionActive(true): session_active=" + sessionActive);

                // App came to foreground - set session_active = true (volatile memory)
                setSessionActive(true);

                // Log state after setting session_active
                Log.d(TAG, "AFTER setSessionActive(true): session_active=" + sessionActive);
            }

            @Override
            public void onActivityResumed(Activity activity) {
                Log.d(TAG, "onActivityResumed: " + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityPaused(Activity activity) {
                Log.d(TAG, "onActivityPaused: " + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityStopped(Activity activity) {
                activeActivityCount--;
                Log.d(TAG, "onActivityStopped: " + activity.getClass().getSimpleName() +
                      " (active count: " + activeActivityCount + ")");

                // All activities stopped - save last_active_time for timeout check
                // NOTE: We DO NOT set sessionActive=false here. Keep it true (in volatile memory).
                // If process dies, sessionActive resets to false automatically, allowing detection.
                if (activeActivityCount == 0) {
                    Log.d(TAG, "All activities stopped - saving last_active_time for timeout check");

                    // Save timestamp to SharedPreferences (persistent)
                    long timestamp = System.currentTimeMillis();
                    SecureTokenManager.getInstance().setLastActiveTime(timestamp);

                    Log.d(TAG, "session_active remains: " + sessionActive + " (volatile, will reset to false if process dies)");
                    Log.d(TAG, "Set last_active_time to: " + timestamp);
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                Log.d(TAG, "onActivitySaveInstanceState: " + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                Log.d(TAG, "onActivityDestroyed: " + activity.getClass().getSimpleName());
            }
        });
    }

}
