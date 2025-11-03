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
