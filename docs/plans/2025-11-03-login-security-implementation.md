# Login & Security Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement secure user authentication with login screen, token storage, and session management to replace hardcoded tokens.

**Architecture:** Three-layer approach - UI layer (SplashActivity, LoginActivity), Service layer (AuthenticationService, SessionManager), and Storage layer (SecureTokenManager). Backend provides token via `/api/auth/login/` endpoint.

**Tech Stack:** Android (Java), EncryptedSharedPreferences for secure storage, HttpURLConnection for API calls, Django REST Framework on backend.

---

## Task 1: Add Security Dependencies

**Files:**
- Modify: `PhotoViewer/app/build.gradle.kts`

**Step 1: Add security-crypto dependency**

Open `PhotoViewer/app/build.gradle.kts` and add to the `dependencies` block:

```kotlin
dependencies {
    // ... existing dependencies ...
    implementation "androidx.security:security-crypto:1.1.0-alpha06"
}
```

**Step 2: Sync Gradle**

Run: `cd PhotoViewer && ./gradlew clean build`

Expected: Build succeeds with no errors

**Step 3: Commit**

```bash
cd PhotoViewer
git add app/build.gradle.kts
git commit -m "build: add androidx.security:security-crypto dependency"
```

---

## Task 2: Create SecureTokenManager Utility Class

**Files:**
- Create: `PhotoViewer/app/src/main/java/com/example/photoviewer/utils/SecureTokenManager.java`

**Step 1: Create directory structure**

Run: `mkdir -p PhotoViewer/app/src/main/java/com/example/photoviewer/utils`

**Step 2: Write SecureTokenManager class**

Create the file with this content:

```java
package com.example.photoviewer.utils;

import android.content.Context;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecureTokenManager {
    private static final String PREFS_NAME = "auth_prefs";
    private static final String TOKEN_KEY = "auth_token";
    private static final String USERNAME_KEY = "remembered_username";

    private static SecureTokenManager instance;
    private EncryptedSharedPreferences encryptedPrefs;

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
```

**Step 3: Verify file created**

Run: `ls -la PhotoViewer/app/src/main/java/com/example/photoviewer/utils/`

Expected: `SecureTokenManager.java` exists

**Step 4: Commit**

```bash
git add PhotoViewer/app/src/main/java/com/example/photoviewer/utils/SecureTokenManager.java
git commit -m "feat: add SecureTokenManager for encrypted token storage"
```

---

## Task 3: Create SessionManager Service Class

**Files:**
- Create: `PhotoViewer/app/src/main/java/com/example/photoviewer/services/SessionManager.java`

**Step 1: Create services directory**

Run: `mkdir -p PhotoViewer/app/src/main/java/com/example/photoviewer/services`

**Step 2: Write SessionManager class**

Create the file with this content:

```java
package com.example.photoviewer.services;

import com.example.photoviewer.utils.SecureTokenManager;

public class SessionManager {
    private static SessionManager instance;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public boolean isLoggedIn() {
        return SecureTokenManager.getInstance().hasToken();
    }

    public String getToken() {
        return SecureTokenManager.getInstance().getToken();
    }

    public String getUsername() {
        return SecureTokenManager.getInstance().getUsername();
    }

    public void saveSession(String username, String token) {
        SecureTokenManager.getInstance().saveUsername(username);
        SecureTokenManager.getInstance().saveToken(token);
    }

    public void logout() {
        SecureTokenManager.getInstance().clearAll();
    }
}
```

**Step 3: Verify file created**

Run: `ls -la PhotoViewer/app/src/main/java/com/example/photoviewer/services/`

Expected: `SessionManager.java` exists

**Step 4: Commit**

```bash
git add PhotoViewer/app/src/main/java/com/example/photoviewer/services/SessionManager.java
git commit -m "feat: add SessionManager for authentication state management"
```

---

## Task 4: Create AuthenticationService Class

**Files:**
- Create: `PhotoViewer/app/src/main/java/com/example/photoviewer/services/AuthenticationService.java`

**Step 1: Write AuthenticationService class**

Create the file with this content:

```java
package com.example.photoviewer.services;

import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AuthenticationService {
    private static final String API_BASE_URL = "http://10.0.2.2:8000";
    private static final String LOGIN_ENDPOINT = "/api/auth/login/";

    public interface LoginCallback {
        void onSuccess(String token);
        void onError(String errorMessage);
    }

    public static void login(String username, String password, LoginCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(API_BASE_URL + LOGIN_ENDPOINT);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                // Create request body
                JSONObject requestBody = new JSONObject();
                requestBody.put("username", username);
                requestBody.put("password", password);

                // Send request
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Handle response
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    StringBuilder response = new StringBuilder();
                    try (java.io.BufferedReader br = new java.io.BufferedReader(
                            new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line);
                        }
                    }

                    JSONObject responseJson = new JSONObject(response.toString());
                    String token = responseJson.getString("token");
                    callback.onSuccess(token);
                } else {
                    // Handle error response
                    StringBuilder error = new StringBuilder();
                    try (java.io.BufferedReader br = new java.io.BufferedReader(
                            new java.io.InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            error.append(line);
                        }
                    }

                    String errorMessage = "Login failed";
                    try {
                        JSONObject errorJson = new JSONObject(error.toString());
                        if (errorJson.has("error")) {
                            errorMessage = errorJson.getString("error");
                        }
                    } catch (Exception e) {
                        // Use default error message
                    }

                    callback.onError(errorMessage);
                }

                conn.disconnect();
            } catch (Exception e) {
                callback.onError("Network error: " + e.getMessage());
            }
        }).start();
    }
}
```

**Step 2: Verify file created**

Run: `ls -la PhotoViewer/app/src/main/java/com/example/photoviewer/services/`

Expected: `AuthenticationService.java` exists

**Step 3: Commit**

```bash
git add PhotoViewer/app/src/main/java/com/example/photoviewer/services/AuthenticationService.java
git commit -m "feat: add AuthenticationService for backend login API calls"
```

---

## Task 5: Create SplashActivity

**Files:**
- Create: `PhotoViewer/app/src/main/java/com/example/photoviewer/SplashActivity.java`
- Create: `PhotoViewer/app/src/main/res/layout/activity_splash.xml`

**Step 1: Create SplashActivity Java class**

```java
package com.example.photoviewer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.example.photoviewer.services.SessionManager;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DURATION = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

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
}
```

Save to: `PhotoViewer/app/src/main/java/com/example/photoviewer/SplashActivity.java`

**Step 2: Create SplashActivity layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:background="#FFFFFF">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Photo Blog"
        android:textSize="32sp"
        android:textStyle="bold"
        android:textColor="#000000"
        android:layout_marginBottom="32dp"/>

    <ProgressBar
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:indeterminate="true"/>

</LinearLayout>
```

Save to: `PhotoViewer/app/src/main/res/layout/activity_splash.xml`

**Step 3: Verify files created**

Run: `ls -la PhotoViewer/app/src/main/java/com/example/photoviewer/SplashActivity.java PhotoViewer/app/src/main/res/layout/activity_splash.xml`

Expected: Both files exist

**Step 4: Commit**

```bash
git add PhotoViewer/app/src/main/java/com/example/photoviewer/SplashActivity.java PhotoViewer/app/src/main/res/layout/activity_splash.xml
git commit -m "feat: add SplashActivity with auth check and layout"
```

---

## Task 6: Create LoginActivity

**Files:**
- Create: `PhotoViewer/app/src/main/java/com/example/photoviewer/LoginActivity.java`
- Create: `PhotoViewer/app/src/main/res/layout/activity_login.xml`

**Step 1: Create LoginActivity Java class**

```java
package com.example.photoviewer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.photoviewer.services.AuthenticationService;
import com.example.photoviewer.services.SessionManager;
import com.example.photoviewer.utils.SecureTokenManager;

public class LoginActivity extends AppCompatActivity {
    private EditText usernameInput;
    private EditText passwordInput;
    private Button loginButton;
    private CheckBox rememberUsernameCheckbox;
    private TextView errorMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        loadRememberedUsername();
        setupLoginButton();
    }

    private void initializeViews() {
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        rememberUsernameCheckbox = findViewById(R.id.remember_username_checkbox);
        errorMessage = findViewById(R.id.error_message);
    }

    private void loadRememberedUsername() {
        String saved = SecureTokenManager.getInstance().getUsername();
        if (saved != null && !saved.isEmpty()) {
            usernameInput.setText(saved);
            rememberUsernameCheckbox.setChecked(true);
        }
    }

    private void setupLoginButton() {
        loginButton.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Clear previous error
        errorMessage.setText("");

        // Validate input
        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password required");
            return;
        }

        // Disable button to prevent multiple clicks
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        // Attempt login
        AuthenticationService.login(username, password, new AuthenticationService.LoginCallback() {
            @Override
            public void onSuccess(String token) {
                // Save session
                SessionManager.getInstance().saveSession(username, token);

                // Save username if checkbox is checked
                if (rememberUsernameCheckbox.isChecked()) {
                    SecureTokenManager.getInstance().saveUsername(username);
                } else {
                    SecureTokenManager.getInstance().deleteUsername();
                }

                // Navigate to MainActivity
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    showError(errorMessage);
                    loginButton.setEnabled(true);
                    loginButton.setText("Log In");
                    passwordInput.setText(""); // Clear password on error
                });
            }
        });
    }

    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisibility(android.view.View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
```

Save to: `PhotoViewer/app/src/main/java/com/example/photoviewer/LoginActivity.java`

**Step 2: Create LoginActivity layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="24dp"
    android:gravity="center"
    android:background="#FFFFFF">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Photo Blog"
        android:textSize="32sp"
        android:textStyle="bold"
        android:textColor="#000000"
        android:layout_marginBottom="32dp"/>

    <TextView
        android:id="@+id/error_message"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textColor="#FF0000"
        android:textSize="14sp"
        android:layout_marginBottom="16dp"
        android:visibility="gone"/>

    <EditText
        android:id="@+id/username_input"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:hint="Username"
        android:inputType="text"
        android:paddingStart="12dp"
        android:paddingEnd="12dp"
        android:layout_marginBottom="16dp"
        android:background="#F0F0F0"
        android:textColorHint="#999999"/>

    <EditText
        android:id="@+id/password_input"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:hint="Password"
        android:inputType="textPassword"
        android:paddingStart="12dp"
        android:paddingEnd="12dp"
        android:layout_marginBottom="16dp"
        android:background="#F0F0F0"
        android:textColorHint="#999999"/>

    <CheckBox
        android:id="@+id/remember_username_checkbox"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Remember username"
        android:layout_marginBottom="24dp"
        android:textColor="#000000"/>

    <Button
        android:id="@+id/login_button"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:text="Log In"
        android:textSize="16sp"
        android:textStyle="bold"
        android:textColor="#FFFFFF"
        android:background="#007AFF"
        android:textAllCaps="false"/>

</LinearLayout>
```

Save to: `PhotoViewer/app/src/main/res/layout/activity_login.xml`

**Step 3: Verify files created**

Run: `ls -la PhotoViewer/app/src/main/java/com/example/photoviewer/LoginActivity.java PhotoViewer/app/src/main/res/layout/activity_login.xml`

Expected: Both files exist

**Step 4: Commit**

```bash
git add PhotoViewer/app/src/main/java/com/example/photoviewer/LoginActivity.java PhotoViewer/app/src/main/res/layout/activity_login.xml
git commit -m "feat: add LoginActivity with authentication form and logic"
```

---

## Task 7: Initialize SecureTokenManager in MainActivity

**Files:**
- Modify: `PhotoViewer/app/src/main/java/com/example/photoviewer/MainActivity.java` (add initialization)

**Step 1: Update MainActivity onCreate() to initialize SecureTokenManager**

In `MainActivity.java`, add this to the beginning of `onCreate()` method (after `super.onCreate()`):

```java
try {
    SecureTokenManager.initialize(this);
} catch (Exception e) {
    e.printStackTrace();
}
```

Also add the import at the top:
```java
import com.example.photoviewer.utils.SecureTokenManager;
```

**Step 2: Add logout button to MainActivity toolbar**

In the `onCreate()` method, after setting up the RecyclerView, add:

```java
// Add logout button to toolbar
if (getSupportActionBar() != null) {
    getSupportActionBar().setDisplayShowCustomEnabled(true);
    Button logoutButton = new Button(this);
    logoutButton.setText("Logout");
    logoutButton.setOnClickListener(v -> logout());
    getSupportActionBar().setCustomView(logoutButton);
}
```

Add the import:
```java
import android.widget.Button;
```

**Step 3: Add logout() method to MainActivity**

Add this method to the MainActivity class:

```java
private void logout() {
    SessionManager.getInstance().logout();
    Intent intent = new Intent(MainActivity.this, SplashActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
}
```

Add the import:
```java
import com.example.photoviewer.services.SessionManager;
```

**Step 4: Verify changes**

Run: `grep -n "SecureTokenManager.initialize" PhotoViewer/app/src/main/java/com/example/photoviewer/MainActivity.java`

Expected: Line with initialization is found

**Step 5: Commit**

```bash
git add PhotoViewer/app/src/main/java/com/example/photoviewer/MainActivity.java
git commit -m "feat: initialize SecureTokenManager and add logout button to MainActivity"
```

---

## Task 8: Update AndroidManifest.xml

**Files:**
- Modify: `PhotoViewer/app/src/main/AndroidManifest.xml`

**Step 1: Update launcher activity and add new activities**

In `AndroidManifest.xml`, change the launcher intent filter from MainActivity to SplashActivity. Also add declarations for LoginActivity and SplashActivity.

Replace the `<activity>` section with:

```xml
<activity
    android:name=".SplashActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<activity
    android:name=".LoginActivity"
    android:exported="false" />

<activity
    android:name=".MainActivity"
    android:exported="false" />
```

**Step 2: Verify changes**

Run: `grep -A 5 "SplashActivity" PhotoViewer/app/src/main/AndroidManifest.xml`

Expected: SplashActivity appears as launcher activity

**Step 3: Commit**

```bash
git add PhotoViewer/app/src/main/AndroidManifest.xml
git commit -m "feat: set SplashActivity as launcher and add activity declarations"
```

---

## Task 9: Create Backend Login Endpoint

**Files:**
- Modify: `PhotoBlogServer/blog/views.py`
- Modify: `PhotoBlogServer/mysite/urls.py`

**Step 1: Add login view to Django backend**

In `PhotoBlogServer/blog/views.py`, add this import and view:

```python
from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework.status import HTTP_400_BAD_REQUEST, HTTP_401_UNAUTHORIZED, HTTP_200_OK
from django.contrib.auth import authenticate
from rest_framework.authtoken.models import Token

@api_view(['POST'])
def login(request):
    """
    Authenticate user with username/password and return token
    """
    username = request.data.get('username')
    password = request.data.get('password')

    if not username or not password:
        return Response(
            {'error': 'Username and password required'},
            status=HTTP_400_BAD_REQUEST
        )

    user = authenticate(username=username, password=password)

    if user is None:
        return Response(
            {'error': 'Invalid credentials'},
            status=HTTP_401_UNAUTHORIZED
        )

    token, created = Token.objects.get_or_create(user=user)
    return Response(
        {'token': token.key},
        status=HTTP_200_OK
    )
```

**Step 2: Add URL pattern for login endpoint**

In `PhotoBlogServer/mysite/urls.py`, add this import and URL pattern:

```python
from blog.views import login

urlpatterns = [
    # ... existing patterns ...
    path('api/auth/login/', login, name='api-login'),
]
```

**Step 3: Test endpoint**

Run Django server:
```bash
cd PhotoBlogServer
python manage.py runserver
```

Test with curl:
```bash
curl -X POST http://127.0.0.1:8000/api/auth/login/ \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "your_password"}'
```

Expected: `{"token": "abc123..."}`

**Step 4: Commit**

```bash
cd PhotoBlogServer
git add blog/views.py mysite/urls.py
git commit -m "feat: add POST /api/auth/login/ endpoint for user authentication"
```

---

## Task 10: Build and Test Android Application

**Files:**
- Test: `PhotoViewer/app/src/main/java/com/example/photoviewer/SplashActivity.java`
- Test: `PhotoViewer/app/src/main/java/com/example/photoviewer/LoginActivity.java`

**Step 1: Build the project**

Run:
```bash
cd PhotoViewer
./gradlew clean build
```

Expected: Build succeeds with no errors

**Step 2: Manual Testing Checklist**

- [ ] Open app → sees SplashActivity
- [ ] After 2 seconds → redirects to LoginActivity (no token stored)
- [ ] Enter valid username/password → navigates to MainActivity
- [ ] Check token is stored (inspect with Android Studio debugger)
- [ ] Logout button visible in MainActivity
- [ ] Click logout → returns to LoginActivity with cleared data
- [ ] Close and reopen app → goes directly to MainActivity (token persists)
- [ ] Try invalid credentials → shows error message
- [ ] Network error when server is down → shows "Network error" message

**Step 3: Deploy to emulator/device**

Run:
```bash
./gradlew installDebug
```

Then launch app and perform manual testing above.

**Step 4: Commit**

```bash
git add .
git commit -m "test: verify login and authentication flow end-to-end"
```

---

## Task 11: Fix Import Statement in MainActivity

**Files:**
- Modify: `PhotoViewer/app/src/main/java/com/example/photoviewer/MainActivity.java`

**Step 1: Add missing imports to MainActivity**

If gradle build shows unresolved imports, add these to MainActivity:

```java
import android.content.Intent;
import com.example.photoviewer.services.SessionManager;
import com.example.photoviewer.utils.SecureTokenManager;
```

**Step 2: Build again**

Run: `cd PhotoViewer && ./gradlew clean build`

Expected: Build succeeds

**Step 3: Commit**

```bash
git add PhotoViewer/app/src/main/java/com/example/photoviewer/MainActivity.java
git commit -m "fix: add missing imports to MainActivity"
```

---

## Task 12: Update API Calls in MainActivity to Use Token

**Files:**
- Modify: `PhotoViewer/app/src/main/java/com/example/photoviewer/MainActivity.java`

**Step 1: Find the downloadPosts() method and update it**

In MainActivity, locate where HttpURLConnection is used to fetch posts. Update the request to include the Authorization header:

```java
// Add this before making the request
String token = SessionManager.getInstance().getToken();
conn.setRequestProperty("Authorization", "Token " + token);
```

Example context (around line 60 where token is currently hardcoded):

```java
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setRequestMethod("GET");
conn.setRequestProperty("Authorization", "Token " + SessionManager.getInstance().getToken()); // Updated
```

**Step 2: Find the downloadImage() method and update it**

Similarly, update any other HTTP calls to use the SessionManager token instead of hardcoded token.

**Step 3: Build and test**

Run: `cd PhotoViewer && ./gradlew build`

Expected: Build succeeds, app downloads posts with token from SessionManager

**Step 4: Commit**

```bash
git add PhotoViewer/app/src/main/java/com/example/photoviewer/MainActivity.java
git commit -m "feat: use SessionManager token in API calls instead of hardcoded token"
```

---

## Task 13: Create Testing Documentation

**Files:**
- Create: `docs/testing/login-security-testing.md`

**Step 1: Create testing guide document**

```markdown
# Login & Security Feature Testing Guide

## Setup

### Prerequisites
- Django server running on localhost:8000
- Android emulator running
- Test user account created (username: testuser, password: testpass123)

### Server Setup
```bash
cd PhotoBlogServer
python manage.py runserver
```

Create test user in Django shell:
```bash
python manage.py shell
from django.contrib.auth.models import User
User.objects.create_user(username='testuser', password='testpass123')
```

## Test Cases

### TC1: Initial App Launch (No Stored Token)
**Expected:** SplashActivity shows for 2 seconds, then LoginActivity appears
**Steps:**
1. Uninstall app from emulator
2. Launch app
3. Verify splash screen displays
4. Verify redirects to LoginActivity after 2 seconds

### TC2: Valid Login
**Expected:** User logs in successfully and sees photo feed
**Steps:**
1. On LoginActivity, enter username: testuser
2. Enter password: testpass123
3. Click "Log In"
4. Verify navigates to MainActivity with photo feed
5. Verify token is stored securely

### TC3: Invalid Credentials
**Expected:** Error message displayed, password cleared
**Steps:**
1. On LoginActivity, enter username: testuser
2. Enter password: wrongpassword
3. Click "Log In"
4. Verify error message appears: "Invalid credentials"
5. Verify password field is cleared
6. Verify username field still has "testuser"

### TC4: Empty Fields
**Expected:** Error message displayed
**Steps:**
1. On LoginActivity, leave username empty
2. Click "Log In"
3. Verify error message: "Username and password required"

### TC5: Remember Username
**Expected:** Username persists after logout/login cycle
**Steps:**
1. Enter username: testuser and password
2. Check "Remember username" checkbox
3. Click "Log In"
4. Click "Logout" button in MainActivity
5. Verify LoginActivity shows "testuser" in username field
6. Verify checkbox is checked

### TC6: Forget Username
**Expected:** Username cleared after logout
**Steps:**
1. Log in with "Remember username" unchecked
2. Click "Logout"
3. Verify username field is empty
4. Verify checkbox is unchecked

### TC7: Token Persistence
**Expected:** App goes directly to MainActivity on restart
**Steps:**
1. Log in successfully
2. Close app completely
3. Reopen app
4. Verify SplashActivity briefly shows
5. Verify app goes directly to MainActivity (no login required)

### TC8: Logout
**Expected:** Session cleared, returns to LoginActivity
**Steps:**
1. Log in successfully
2. Click "Logout" button
3. Verify navigates to LoginActivity
4. Verify token is deleted from storage
5. Verify cannot view photos without logging in again

### TC9: Network Error (Server Down)
**Expected:** Error message displayed
**Steps:**
1. Stop Django server
2. On LoginActivity, enter credentials
3. Click "Log In"
4. Verify error message appears: "Network error: ..."
5. Verify login button becomes enabled again (can retry)

### TC10: Token Used in API Calls
**Expected:** Posts are fetched with authentication token
**Steps:**
1. Log in successfully
2. Verify MainActivity displays photos
3. In Android Studio Logcat, verify "Authorization: Token ..." header in requests

## Manual Testing Checklist
- [ ] Splash screen appears on startup
- [ ] Login form has username and password fields
- [ ] Login button is visible and clickable
- [ ] Valid credentials allow login
- [ ] Invalid credentials show error
- [ ] Remember username works correctly
- [ ] Logout clears session and token
- [ ] Token persists across app restarts
- [ ] Photo feed loads after successful login
- [ ] Network errors are handled gracefully
- [ ] No hardcoded tokens in code

## Debugging Tips
- Check Logcat for "AuthenticationService" logs
- Use Android Studio debugger to inspect SecureTokenManager
- Check Django server logs for POST /api/auth/login/ requests
- Verify token format in database: `Token.objects.all()` in Django shell
```

Save to: `docs/testing/login-security-testing.md`

**Step 2: Create directory if needed**

Run: `mkdir -p docs/testing`

**Step 3: Commit**

```bash
git add docs/testing/login-security-testing.md
git commit -m "docs: add comprehensive testing guide for login/security features"
```

---

## Summary

This plan implements a complete login and security system with:
- ✅ Secure token storage using EncryptedSharedPreferences
- ✅ User authentication via username/password
- ✅ Session management and logout functionality
- ✅ Remember username feature
- ✅ Proper error handling and user feedback
- ✅ Backend API endpoint for login
- ✅ Comprehensive testing guide

All tasks include exact file paths, complete code examples, and verification steps.

---

## Next Steps: Execution

Plan complete and saved to `docs/plans/2025-11-03-login-security-implementation.md`. Two execution options:

**Option 1: Subagent-Driven (this session)**
- I dispatch a fresh subagent per task
- Code review between tasks
- Fast iteration with quality gates
- Recommended for collaborative feedback

**Option 2: Parallel Session (separate)**
- Open new session with `superpowers:executing-plans`
- Batch execution with checkpoints
- Good for autonomous implementation

**Which approach would you prefer?**

