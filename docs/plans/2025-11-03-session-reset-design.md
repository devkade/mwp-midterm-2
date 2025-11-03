# Session Reset Design

**Date:** 2025-11-03
**Status:** Approved

## Overview

Implement automatic session clearing when the app is force-closed or inactive for 10 minutes. This ensures users must re-authenticate after these events while maintaining session persistence for normal app usage.

## Requirements

1. **Force Close Detection:** When user swipes app from recents (process killed), require login on next startup
2. **Inactivity Timeout:** Clear session after 10 minutes of inactivity
3. **Normal Usage:** Keep session active when app is reopened within 10 minutes
4. **Data to Clear:** Auth token and user profile information

## Technical Approach

### Dual Detection Mechanism

We use two mechanisms to detect when session should be cleared:

1. **Process Alive Flag** (`session_active` boolean)
   - Set to `true` when app is in foreground
   - Cleared when process is destroyed
   - On startup, if missing/false = process was killed = clear session

2. **Last Active Timestamp** (`last_active_time` long)
   - Updated when app goes to background
   - On startup, check if (current_time - last_active_time) > 10 minutes
   - If yes = inactivity timeout = clear session

### Session Check Logic

```
App Startup:
├─ Read session_active flag
├─ If flag is false/missing:
│  └─ Process was killed → Clear all session data
└─ If flag is true:
   ├─ Read last_active_time
   ├─ If (current_time - last_active_time) > 10 min:
   │  └─ Clear all session data (inactivity timeout)
   └─ Else: Keep session (user still logged in)
```

## Implementation Components

### 1. Application Class (New: `PhotoViewerApplication.java`)

**Purpose:** Central lifecycle management for the entire application

**Responsibilities:**
- Register `ActivityLifecycleCallbacks` to track app foreground/background state
- Perform session validity check in `onCreate()`
- Update `session_active` flag and `last_active_time` timestamp
- Trigger session clearing when conditions are met

**Key Methods:**
- `onCreate()` - Check session validity on startup
- `onActivityStarted()` - Set session_active = true (app in foreground)
- `onActivityStopped()` - Update last_active_time, track if all activities stopped
- Lifecycle counter to detect when ALL activities are stopped

### 2. SecureTokenManager Updates

**New Methods:**
- `setSessionActive(boolean active)` - Set/clear the session_active flag
- `isSessionActive()` - Check if session is currently active
- `setLastActiveTime(long timestamp)` - Store last active timestamp
- `getLastActiveTime()` - Retrieve last active timestamp
- `clearSession()` - Clear all session data (token + profile + flags)

**SharedPreferences Keys:**
- `auth_token` - Existing auth token
- `session_active` - Boolean flag for process state
- `last_active_time` - Long timestamp (System.currentTimeMillis())

### 3. MainActivity Updates

**Changes:**
- Add session check in `onCreate()` or `onResume()`
- If session is invalid, redirect to `LoginActivity`
- Clear any cached user data when redirecting

### Constants

```java
public static final long SESSION_TIMEOUT_MS = 600000; // 10 minutes
```

## Data to Clear

When `clearSession()` is called, remove:
1. Auth token (`auth_token`)
2. User profile information (if stored)
3. Session flags (`session_active`, `last_active_time`)

## Edge Cases

### 1. First App Launch
- No timestamps/flags exist
- Should NOT clear session (nothing to clear)
- Initialize `session_active = true` and set initial timestamp

### 2. App Crash
- Process killed unexpectedly
- `session_active` flag will be false on next startup
- Session correctly cleared

### 3. System-Initiated Process Kill
- Android kills app for memory management
- Same as crash - flag absent/false, session cleared

### 4. Quick Background/Foreground Cycles
- User switches apps briefly (< 10 min)
- Timestamp updated, but within timeout window
- Session persists correctly

## Testing Scenarios

### Test 1: Force Close Detection
1. Login to app successfully
2. Force close app (swipe from recent apps)
3. Reopen app
4. **Expected:** Redirected to login screen

### Test 2: Inactivity Timeout
1. Login to app successfully
2. Press home button (app goes to background)
3. Wait 11 minutes
4. Reopen app
5. **Expected:** Redirected to login screen

### Test 3: Quick Switch (Session Persistence)
1. Login to app successfully
2. Press home button
3. Wait 2 minutes
4. Reopen app
5. **Expected:** Still logged in, main screen shown

### Test 4: Normal Usage
1. Login to app successfully
2. Use app normally
3. Close and reopen within 10 minutes
4. **Expected:** Still logged in

### Test 5: First Launch
1. Fresh install of app
2. App starts for first time
3. **Expected:** Shows login screen, no crashes

## Security Considerations

1. **SharedPreferences Security:** Currently using standard SharedPreferences. Consider using EncryptedSharedPreferences for production.
2. **Timeout Duration:** 10 minutes provides balance between security and user convenience. Adjust based on security requirements.
3. **Token Storage:** Auth token should never be logged or exposed in error messages.

## Implementation Order

1. Create `PhotoViewerApplication` class with lifecycle callbacks
2. Update `SecureTokenManager` with new session methods
3. Update `MainActivity` to check session validity
4. Test all scenarios thoroughly
5. Update manifest to register Application class

## Success Criteria

- Force-closed app requires re-login on next startup
- 10+ minute inactivity triggers session clear
- Normal usage within 10 minutes maintains session
- No crashes or data loss during session transitions
- All test scenarios pass consistently

---

## Implementation Details

### Architecture Change: Volatile Session State

**Initial Design Issue:**
The original design stored `session_active` in SharedPreferences (persistent storage). This caused process death detection to fail because:
1. When app backgrounded → `setSessionActive(false)` saved to SharedPreferences
2. Process killed by system
3. New process started → Read `session_active=false` from SharedPreferences
4. **Problem:** Cannot distinguish between "normal background exit" vs "process death"

**Solution: Hybrid Storage Model**

| Data | Storage | Lifetime | Purpose |
|------|---------|----------|---------|
| `session_active` | **Volatile memory** (Application class) | Process lifetime only | Process death detection |
| `last_active_time` | **SharedPreferences** (persistent) | Survives restarts | Inactivity timeout |
| `auth_token` | **EncryptedSharedPreferences** (persistent) | Until cleared | Authentication |

### Implemented Components

#### 1. PhotoViewerApplication.java

**Location:** `app/src/main/java/com/example/photoviewer/PhotoViewerApplication.java`

**Key Implementation:**
```java
// Volatile session state - resets to false when process dies
private static boolean sessionActive = false;

public static boolean isSessionActive() {
    return sessionActive;
}

public static void setSessionActive(boolean active) {
    sessionActive = active;
}
```

**Lifecycle Callbacks:**
- `onActivityStarted()`: Set `sessionActive = true` (app in foreground)
- `onActivityStopped()`:
  - Save `last_active_time` to SharedPreferences
  - **DO NOT** set `sessionActive = false` (keep it true in memory)
  - When process dies, `sessionActive` automatically resets to `false`

**Critical Design Point:**
> We intentionally DO NOT call `setSessionActive(false)` when backgrounding. This allows the volatile memory to serve as a "process alive" indicator. Only process death resets it to false.

#### 2. SecureTokenManager.java

**Location:** `app/src/main/java/com/example/photoviewer/utils/SecureTokenManager.java`

**Changes:**
- **Removed** `SESSION_ACTIVE_KEY` from SharedPreferences
- **Removed** `setSessionActive()` and `isSessionActive()` methods (moved to Application)
- **Kept** `setLastActiveTime()` and `getLastActiveTime()` (persistent storage)
- Updated `clearSession()` to only clear persistent data (token, username, timestamp)

#### 3. SplashActivity.java

**Location:** `app/src/main/java/com/example/photoviewer/SplashActivity.java`

**Process Death Detection Logic:**
```java
boolean sessionActive = PhotoViewerApplication.isSessionActive();
boolean hasToken = SecureTokenManager.getInstance().hasToken();

if (hasToken && !sessionActive) {
    // Process death detected: we have a token but sessionActive is false
    // This means the process was killed (sessionActive reset to false)
    Log.d(TAG, "Process death detected - clearing session");
    SecureTokenManager.getInstance().clearSession();
    return;
}
```

**How It Works:**
1. **Normal startup after clean background:**
   - Process still alive → `sessionActive = true` (memory preserved)
   - Check timeout, session valid → Keep session

2. **Startup after process death:**
   - New process → `sessionActive = false` (volatile reset)
   - Has token but `sessionActive = false` → **Process died** → Clear session

3. **Startup after 10min timeout:**
   - Process alive → `sessionActive = true`
   - Check timestamp → Timeout exceeded → Clear session

### File Changes Summary

```
Modified Files:
├── PhotoViewerApplication.java (major changes)
│   ├── Added: static boolean sessionActive (volatile)
│   ├── Added: isSessionActive() / setSessionActive() methods
│   ├── Modified: onActivityStarted() - use Application.setSessionActive()
│   └── Modified: onActivityStopped() - removed setSessionActive(false) call
│
├── SecureTokenManager.java (refactoring)
│   ├── Removed: SESSION_ACTIVE_KEY constant
│   ├── Removed: setSessionActive() / isSessionActive() methods
│   ├── Modified: hasSessionData() - no longer checks session_active
│   └── Modified: clearSession() - no longer clears session_active
│
└── SplashActivity.java (logic update)
    └── Modified: checkAndClearInvalidSession() - use PhotoViewerApplication.isSessionActive()
```

---

## Troubleshooting

### Issue #1: Process Death Not Detected (Initial Implementation)

**Symptom:**
```
17:16:35.567 - All activities stopped - updating last_active_time and marking app as background
17:16:35.592 - AFTER setSessionActive(false): session_active=false
17:16:35.713 - Killing 20052 (process killed)
17:16:42.482 - Initial session_active: false (new process)
17:16:42.562 - Session still valid - keeping session ❌ (should have cleared!)
```

**Root Cause:**
- `session_active` was stored in SharedPreferences (persistent storage)
- When app backgrounded → `setSessionActive(false)` saved to disk
- Process killed → New process read `session_active=false` from disk
- **Could not distinguish** between:
  - Normal background exit (user pressed Home, process still alive)
  - Force close (user killed process from Recents)

**Solution:**
Move `session_active` to **volatile memory** (Application class static field):
- Default value: `false` (when process starts)
- Set to `true` when app enters foreground
- **Never set to `false`** when backgrounding
- Only resets to `false` when process dies (automatic)

**Detection Logic:**
```
if (hasToken && !sessionActive) {
    // We have a persisted token (SharedPreferences)
    // But sessionActive is false (volatile memory)
    // This only happens when process dies and restarts
    clearSession();
}
```

### Issue #2: SharedPreferences vs Volatile Memory Confusion

**Problem:**
Initial confusion about when to use persistent vs volatile storage.

**Decision Matrix:**

| Requirement | Storage Type | Reason |
|-------------|--------------|--------|
| Survive process death? | SharedPreferences | Data must persist across restarts |
| Reset on process death? | Volatile memory | Automatic cleanup on process restart |
| Security sensitive? | EncryptedSharedPreferences | Encryption at rest |

**Applied to Our Data:**

| Data | Choice | Reason |
|------|--------|--------|
| `auth_token` | EncryptedSharedPreferences | Persists + encrypted |
| `last_active_time` | SharedPreferences | Persists (needed for timeout check) |
| `session_active` | Volatile (Application class) | **Must reset** on process death |

### Issue #3: Lifecycle Callback Timing

**Challenge:**
Understanding when `onActivityStopped()` is called vs when process is killed.

**Timeline Analysis:**
```
Normal Background Flow:
  User presses Home
  ├─ onActivityPaused()    ← Activity going to background
  ├─ onActivityStopped()   ← Activity no longer visible
  │  └─ Save last_active_time
  └─ Process MAY be alive (system decides)

Force Close Flow:
  User swipes from Recents
  ├─ onActivityStopped()   ← May or may not be called
  └─ Process.killProcess() ← Immediate termination
     └─ sessionActive lost (volatile memory cleared)
```

**Key Insight:**
> We cannot rely on callbacks to detect force-close. We must use the **absence of data** (volatile memory reset) as the detection signal.

### Issue #4: Testing Process Death

**Challenge:**
How to reliably test process death scenarios.

**Testing Methods:**

| Method | Command | Simulates |
|--------|---------|-----------|
| Force Stop | `adb shell am force-stop com.example.photoviewer` | User force-stopping from Settings |
| Kill Process | `adb shell am kill com.example.photoviewer` | System killing for memory |
| Swipe from Recents | Manual UI action | User closing app |

**Recommended Test Flow:**
1. Login to app
2. Press Home (background)
3. Check logcat: `session_active remains: true`
4. Force stop: `adb shell am force-stop com.example.photoviewer`
5. Relaunch app
6. Check logcat: `Initial session_active: false` + `Process death detected`

### Issue #5: First Launch Edge Case

**Scenario:**
Fresh app install, no SharedPreferences data exists.

**Potential Bug:**
```java
if (hasToken && !sessionActive) {
    clearSession(); // Don't call this if no token exists!
}
```

**Solution:**
Check `hasToken` first:
```java
if (!SecureTokenManager.getInstance().hasSessionData()) {
    // No data exists - first launch, nothing to clear
    return;
}
```

**Implemented in:** `SplashActivity.checkAndClearInvalidSession()`

---

## Testing Results

### Expected Logcat Output

**Scenario 1: Normal Background (Process Alive)**
```
PhotoViewerApplication: onActivityStopped (active count: 0)
PhotoViewerApplication: session_active remains: true (volatile, will reset if process dies)
PhotoViewerApplication: Set last_active_time to: 1762157795592

[User relaunches app]

PhotoViewerApplication: Initial session_active: true (process still alive)
SplashActivity: Session still valid - keeping session ✅
```

**Scenario 2: Process Death**
```
PhotoViewerApplication: onActivityStopped (active count: 0)
PhotoViewerApplication: Set last_active_time to: 1762157795592
[System kills process]

[User relaunches app - NEW PROCESS]

PhotoViewerApplication: Initial session_active: false (always false on new process) ✅
SplashActivity: Process death detected (has token but sessionActive=false) ✅
SecureTokenManager: === CLEARING SESSION ===
[Redirects to LoginActivity]
```

**Scenario 3: 10-Minute Timeout**
```
PhotoViewerApplication: Set last_active_time to: 1762157795592
[Wait 11 minutes]

PhotoViewerApplication: Initial session_active: true (process alive)
SplashActivity: Last active: 1762157795592, Current: 1762158455123, Elapsed: 659s
SplashActivity: Inactivity timeout exceeded (659s > 600s) - clearing session ✅
```

---

## Lessons Learned

1. **Persistent vs Volatile Storage:**
   - Use persistent storage (SharedPreferences) for data that must survive restarts
   - Use volatile storage (Application class) for data that should reset on restart
   - Process death detection requires volatile state

2. **Lifecycle Callback Limitations:**
   - Cannot rely on `onDestroy()` or lifecycle callbacks for force-close detection
   - System can kill process without calling cleanup methods
   - Must use "absence of evidence" (volatile reset) instead of "evidence of absence"

3. **Android Process Model:**
   - Android can kill background processes at any time for memory management
   - Application class `onCreate()` called on every process start
   - Static fields reset to default values when process dies

4. **Security Design:**
   - Session timeout provides defense-in-depth
   - Process death detection prevents session hijacking via process inspection
   - Combining both mechanisms provides robust session management
