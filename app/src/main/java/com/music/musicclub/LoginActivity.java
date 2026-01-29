package com.music.musicclub;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.NoConnectionError;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONObject;

import java.net.SocketException;


public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private LoginManager loginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        loginManager = new LoginManager(this);

        // Check if already logged in
        String existingToken = loginManager.getToken();
        if (existingToken != null && !existingToken.trim().isEmpty()) {
            // Already logged in, go to HostActivity
            Intent intent = new Intent(LoginActivity.this, HostActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                etEmail.setError(getString(R.string.error_email_is_required));
                return;
            }

            if (TextUtils.isEmpty(password)) {
                etPassword.setError(getString(R.string.error_password_required));
                return;
            }

            // Disable button to prevent multiple clicks
            btnLogin.setEnabled(false);

            ApiService.login(
                    this,
                    email,
                    password,
                    response -> {
                        // Success: extract token and save
                        Log.d("LoginActivity", "Login success, response: " + response.toString());

                        String token = null;

                        // Try top-level token
                        if (response != null) {
                            token = response.optString("token", null);

                            // If not present, try response.data.token
                            if (TextUtils.isEmpty(token)) {
                                JSONObject data = response.optJSONObject("data");
                                if (data != null) {
                                    token = data.optString("token", null);
                                }
                            }
                        }

                        if (!TextUtils.isEmpty(token)) {
                            Log.d("LoginActivity", "Token found: " + token);
                            loginManager.saveToken(token);
                            Toast.makeText(LoginActivity.this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(this, HostActivity.class);
                            startActivity(intent);
                            finish(); // Prevent back to login
                        } else {
                            // No token in response
                            Log.d("LoginActivity", "No token in response");
                            etPassword.setError("Login failed: no token received");
                            btnLogin.setEnabled(true);
                        }
                    },
                    error -> {
                        // Error: show message
                        Log.d("LoginActivity", "Login error: " + (error == null ? "null" : error.toString()));

                        String errorMsg = "Login failed";

                        // Detect SocketException (e.g., EPERM) in cause chain
                        Throwable cause = (error == null) ? null : error.getCause();
                        while (cause != null) {
                            if (cause instanceof SocketException) {
                                String m = cause.getMessage();
                                if (m != null && m.toLowerCase().contains("eperm")) {
                                    // Common on some emulators/devices due to network/firewall
                                    errorMsg = "Network blocked (EPERM). If using the Android emulator, try using 10.0.2.2 as the server host or ensure the device has network access and firewall allows connections.";
                                    break;
                                }
                            }
                            cause = cause.getCause();
                        }

                        if (error instanceof NoConnectionError) {
                            errorMsg = "No network connection. Check your device or emulator network and the server URL.";
                        }

                        if (error != null && error.networkResponse != null && error.networkResponse.statusCode == 401) {
                            errorMsg = "Incorrect email or password";
                        }

                        // Friendly toast and field error
                        Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        etPassword.setError(errorMsg);
                        btnLogin.setEnabled(true);
                    }
            );
        });
    }

}
