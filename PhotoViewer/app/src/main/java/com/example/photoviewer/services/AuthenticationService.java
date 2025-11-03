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
