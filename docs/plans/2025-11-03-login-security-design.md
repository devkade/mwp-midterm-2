# Login & Security Features Design
**Date:** 2025-11-03
**Project:** Photo Blog (Android + Django Backend)
**Objective:** Improve security by implementing user authentication instead of hardcoded tokens

---

## Overview

Currently, the Android app has a hardcoded authentication token, which is a security risk. This design implements a proper login system where users authenticate with username/password, and the app securely stores their token locally.

**Key Goals:**
- Remove hardcoded token from codebase
- Allow multiple users to log in with their own accounts
- Secure token storage on Android device
- Improve overall system security
- Maintain simple, clean user experience

---

## Architecture & Components

### New Components (Android)

#### 1. **SplashActivity**
- **Purpose:** Check authentication status on app startup
- **Responsibilities:**
  - Read stored token from EncryptedSharedPreferences
  - If token exists → navigate to MainActivity
  - If no token → navigate to LoginActivity
  - Show splash screen for 2 seconds during check
  - Handle token expiration validation

#### 2. **LoginActivity**
- **Purpose:** Handle user authentication
- **Responsibilities:**
  - Display login form (username, password)
  - Validate input (non-empty fields)
  - Call AuthenticationService to authenticate with backend
  - Store token securely on success
  - Remember username if checkbox is checked
  - Display error messages on failure
  - Handle network errors with retry option

#### 3. **AuthenticationService**
- **Purpose:** Manage API authentication calls
- **Responsibilities:**
  - Make POST request to backend `/api/auth/login/` endpoint
  - Send username/password
  - Parse token from response
  - Handle authentication errors (invalid credentials, network errors)
  - Return token or error to LoginActivity

#### 4. **SecureTokenManager**
- **Purpose:** Securely manage token storage and retrieval
- **Responsibilities:**
  - Store token in EncryptedSharedPreferences (not regular SharedPreferences)
  - Retrieve token when needed
  - Delete token on logout
  - Check if token exists
  - Handle encryption/decryption automatically

#### 5. **SessionManager**
- **Purpose:** Manage authentication state
- **Responsibilities:**
  - Track if user is currently authenticated
  - Provide current token to API requests
  - Provide current username
  - Handle logout (clear token, clear username)
  - Singleton pattern for app-wide access

#### 6. **MainActivity (Modified)**
- **Purpose:** Main photo feed (updated with auth requirements)
- **Modifications:**
  - Check authentication at startup (redirect to login if needed)
  - Add logout button in toolbar/menu
  - Display username in toolbar (optional)
  - Clear stored credentials on logout and return to SplashActivity

### Backend Changes (Minimal)

**Endpoint:** `POST /api/auth/login/`
- **Request:** `{"username": "user", "password": "pass"}`
- **Response:** `{"token": "abc123..."}`
- **Error:** `{"error": "Invalid credentials"}` (HTTP 401)

Can be implemented using Django REST Framework's built-in token authentication or a simple view that validates credentials and returns the token.

---

## UI Screens & Layout

### Screen 1: SplashActivity (Start Page)

```
┌─────────────────────────┐
│                         │
│                         │
│    [App Logo/Image]     │
│                         │
│   Photo Blog (Title)    │
│                         │
│   [Loading Spinner]     │
│                         │
└─────────────────────────┘
```

**Details:**
- Full-screen background image or color
- App title/logo centered
- Loading spinner
- Auto-transition after 2 seconds
- No user interaction required

### Screen 2: LoginActivity (Login Page)

```
┌─────────────────────────┐
│                         │
│   Photo Blog (Title)    │
│                         │
│ [Error message area]    │
│ (empty until error)     │
│                         │
│ ┌─────────────────────┐ │
│ │ Username            │ │
│ └─────────────────────┘ │
│                         │
│ ┌─────────────────────┐ │
│ │ Password            │ │
│ └─────────────────────┘ │
│                         │
│ ☐ Remember username    │
│                         │
│  ┌─────────────────────┐│
│  │   Log In            ││
│  └─────────────────────┘│
│                         │
└─────────────────────────┘
```

**Details:**
- Title at top: "Photo Blog"
- Error message area (red text, appears on failed login)
- Username EditText with hint "Username"
- Password EditText with input type password
- Remember username checkbox
- Full-width "Log In" button
- Centered, clean layout
- Handle network errors gracefully

### Screen 3: MainActivity (Modified)

```
┌─────────────────────────┐
│ Photo Blog   [Logout]   │  ← Toolbar with logout button
├─────────────────────────┤
│                         │
│  [Image 1]  [Image 2]   │
│  [Image 3]  [Image 4]   │
│  [Image 5]  [Image 6]   │  ← RecyclerView (unchanged)
│                         │
│  ...scrollable...       │
│                         │
└─────────────────────────┘
```

**Changes:**
- Add logout button/menu item in toolbar (top-right)
- Optional: Show username next to app title
- On logout:
  - Clear stored token and username
  - Delete all data
  - Navigate back to SplashActivity

---

## Implementation Approach

### Phase 1: Dependency Setup
- Add Android Security library: `androidx.security:security-crypto`
- This provides EncryptedSharedPreferences out of the box

### Phase 2: Create Utility Classes
- Implement SecureTokenManager
- Implement SessionManager
- Implement AuthenticationService

### Phase 3: Create UI Activities
- Create SplashActivity with auth checking logic
- Create LoginActivity with form handling
- Modify MainActivity to require authentication
- Set SplashActivity as launcher activity

### Phase 4: Backend Integration
- Create/update `/api/auth/login/` endpoint in Django
- Test endpoint with curl or Postman
- Verify token response format

### Phase 5: Testing
- Test login with valid credentials
- Test login with invalid credentials
- Test token persistence (app restart)
- Test logout functionality
- Test network error handling

---

## Security Considerations

### 1. Token Storage
- **Why EncryptedSharedPreferences?**
  - Automatically encrypts data at rest
  - No need to manage encryption keys manually
  - Better than plain SharedPreferences
- **Not used:** Don't use plain SharedPreferences for tokens

### 2. Network Security
- **HTTPS in production:** Backend should use HTTPS
- **Certificate pinning (optional):** For extra security
- **Existing:** Keep `network_security_config.xml` for development (HTTP allowed for localhost)

### 3. Password Transmission
- **Always over HTTPS:** Once deployed
- **Never log passwords:** Don't store or log password after login
- **Input validation:** Check for empty fields before sending

### 4. Token Expiration
- **Backend decision:** Should tokens expire? (Optional for Phase 1)
- **If yes:** Add expiration validation in SessionManager
- **If no:** Current implementation with persistent token is fine

### 5. Logout Security
- **Clear all data:** Delete token and username on logout
- **No cached data:** Make sure no sensitive data remains

---

## Data Persistence

### EncryptedSharedPreferences (Token)
```
Key: "auth_token"
Value: "actual_token_value_here"
Encryption: Automatic
Accessibility: App-only (even if device is rooted)
```

### Regular SharedPreferences (Username only)
```
Key: "remembered_username"
Value: "username_string"
Note: This is optional, not sensitive
```

### Clearing on Logout
- Delete both "auth_token" and "remembered_username"
- Clear any in-memory state in SessionManager

---

## API Contract

### Backend Endpoint: POST /api/auth/login/

**Request:**
```json
{
  "username": "admin",
  "password": "password123"
}
```

**Success Response (HTTP 200):**
```json
{
  "token": "abc123xyz..."
}
```

**Error Response (HTTP 401 Unauthorized):**
```json
{
  "error": "Invalid credentials"
}
```

**Error Response (HTTP 400 Bad Request):**
```json
{
  "error": "Username and password required"
}
```

### Usage in MainActivity (for API calls)
```java
// Get token from SessionManager
String token = SessionManager.getToken();

// Add to Authorization header
request.addHeader("Authorization", "Token " + token);
```

---

## Testing Strategy

### Unit Tests
- SecureTokenManager: Test storage and retrieval
- SessionManager: Test state management
- AuthenticationService: Mock API responses

### Integration Tests
- LoginActivity: Valid/invalid credentials, network errors
- SplashActivity: Token existence check, navigation
- MainActivity: Logout functionality

### Manual Testing Checklist
- [ ] App starts on SplashActivity
- [ ] If no token, navigates to LoginActivity
- [ ] Login with valid credentials → navigates to MainActivity
- [ ] Login with invalid credentials → shows error
- [ ] Remember username checkbox works
- [ ] Logout button clears data and returns to login
- [ ] App restart with stored token → goes directly to MainActivity
- [ ] Network errors handled gracefully

---

## Dependencies

### Android Libraries
- `androidx.security:security-crypto` - EncryptedSharedPreferences
- `androidx.appcompat:appcompat` - AppCompatActivity (already used)
- `com.google.android.material:material` - Material design components

### Gradle Configuration
```gradle
dependencies {
    implementation "androidx.security:security-crypto:1.1.0-alpha06"
}
```

---

## File Structure

```
PhotoViewer/app/src/main/java/com/example/photoviewer/
├── MainActivity.java (modified)
├── Post.java (unchanged)
├── ImageAdapter.java (unchanged)
├── SplashActivity.java (new)
├── LoginActivity.java (new)
├── services/
│   ├── AuthenticationService.java (new)
│   └── SessionManager.java (new)
└── utils/
    └── SecureTokenManager.java (new)

PhotoViewer/app/src/main/res/layout/
├── activity_main.xml (unchanged)
├── activity_splash.xml (new)
├── activity_login.xml (new)
├── item_image.xml (unchanged)
└── dialog_post_detail.xml (unchanged)

PhotoViewer/app/src/main/AndroidManifest.xml (modified)
├── Add SplashActivity as launcher
├── Add LoginActivity declaration
```

---

## Success Criteria

- ✅ Users can log in with username/password
- ✅ Token is stored securely (EncryptedSharedPreferences)
- ✅ Token persists across app restarts
- ✅ Logout clears all stored credentials
- ✅ Error messages display for invalid credentials
- ✅ Network errors are handled gracefully
- ✅ SplashActivity properly routes based on auth state
- ✅ No hardcoded tokens in code
- ✅ Username can be remembered for convenience

---

## Next Steps

Once this design is approved:
1. Use `superpowers:writing-plans` to create detailed implementation tasks
2. Create isolated git worktree for development
3. Implement components in order: utilities → activities → testing
4. Create backend `/api/auth/login/` endpoint
5. Integrate and test end-to-end

