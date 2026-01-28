package com.music.musicclub;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import android.util.Log;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

public class ProfileFragment extends Fragment {

    private TextInputEditText etUsername, etEmail, etPassword, etConfirmPassword;
    private TextInputLayout layoutUsername, layoutEmail, layoutPassword, layoutConfirmPassword;
    private MaterialButton btnSaveProfile, btnLogout;
    private android.widget.TextView tvRole;

    private String currentUsername = "";
    private String currentEmail = "";
    private String currentRole = "";
    private int currentUserId = -1;

    private ApiService apiService;
    private String token;

    public ProfileFragment() {
        super(R.layout.profile_layout);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // Initialize views
        etUsername = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        tvRole = view.findViewById(R.id.tvRole);
        layoutUsername = view.findViewById(R.id.layoutName);
        layoutEmail = view.findViewById(R.id.layoutEmail);
        layoutPassword = view.findViewById(R.id.layoutPassword);
        layoutConfirmPassword = view.findViewById(R.id.layoutConfirmPassword);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Get token
        token = new LoginManager(requireContext()).getToken();
        apiService = new ApiService(requireContext(), token);

        // Load profile data
        loadProfile();

        // Set save button listener
        btnSaveProfile.setOnClickListener(v -> saveProfile());

        // Set logout button listener
        btnLogout.setOnClickListener(v -> logout());

        return view;
    }

    private void loadProfile() {
        apiService.me(
                response -> {
                    Log.d("ProfileFragment", "API Response: " + response.toString());
                    requireActivity().runOnUiThread(() -> {
                        try {
                            // API returns {data: {full_name, email, ...}}
                            JSONObject data = response.optJSONObject("data");
                            if (data != null) {
                                currentUserId = data.optInt("id", -1);
                                currentUsername = data.optString("full_name", "");
                                currentEmail = data.optString("email", "");
                                currentRole = data.optString("role_name", "");
                            } else {
                                // Fallback to direct fields
                                JSONObject user = response.optJSONObject("user");
                                if (user != null) {
                                    currentUserId = user.optInt("id", -1);
                                    currentUsername = user.optString("name", "");
                                    currentEmail = user.optString("email", "");
                                    currentRole = user.optString("role", "");
                                } else {
                                    currentUserId = response.optInt("id", -1);
                                    currentUsername = response.optString("name", "");
                                    currentEmail = response.optString("email", "");
                                    currentRole = response.optString("role", "");
                                }
                            }

                            etUsername.setText(currentUsername);
                            etEmail.setText(currentEmail);
                            
                            // Set role text
                            if (!currentRole.isEmpty()) {
                                tvRole.setText("Role: " + currentRole);
                            }

                        } catch (Exception e) {
                            Log.e("ProfileFragment", "Error parsing response", e);
                            Toast.makeText(requireContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
                        }
                    });
                },
                error -> {
                    Log.e("ProfileFragment", "API Error: ", error);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Failed to load profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
        );
    }

    private void saveProfile() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Use current values if empty
        if (username.isEmpty()) username = currentUsername;
        if (email.isEmpty()) email = currentEmail;

        // Clear errors
        layoutUsername.setError(null);
        layoutEmail.setError(null);
        layoutPassword.setError(null);
        layoutConfirmPassword.setError(null);

        boolean hasError = false;

        if (username.isEmpty()) {
            layoutUsername.setError("Username is required");
            hasError = true;
        }

        if (email.isEmpty()) {
            layoutEmail.setError("Email is required");
            hasError = true;
        }

        if (!password.isEmpty()) {
            if (!password.equals(confirmPassword)) {
                layoutConfirmPassword.setError("Passwords do not match");
                hasError = true;
            }
        }

        if (hasError) return;

        // Prepare JSON
        JSONObject body = new JSONObject();
        try {
            body.put("full_name", username);
            body.put("email", email);
            if (!password.isEmpty()) {
                body.put("password", password);
            }
        } catch (JSONException e) {
            Toast.makeText(requireContext(), "Error preparing data", Toast.LENGTH_SHORT).show();
            return;
        }

        // Send update
        if (currentUserId == -1) {
            Toast.makeText(requireContext(), "User ID not found, cannot update", Toast.LENGTH_SHORT).show();
            return;
        }
        apiService.put(ApiConfig.USERS + "/" + currentUserId, body,
                response -> {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        // Clear password fields
                        etPassword.setText("");
                        etConfirmPassword.setText("");
                    });
                },
                error -> {
                    requireActivity().runOnUiThread(() -> {
                        String errorMsg = error.getMessage();
                        if (errorMsg == null || errorMsg.isEmpty()) {
                            if (error.networkResponse != null) {
                                errorMsg = "HTTP " + error.networkResponse.statusCode;
                                if (error.networkResponse.data != null) {
                                    try {
                                        JSONObject errorJson = new JSONObject(new String(error.networkResponse.data));
                                        String apiMessage = errorJson.optString("message", "");
                                        if (!apiMessage.isEmpty()) {
                                            errorMsg += ": " + apiMessage;
                                        }
                                    } catch (Exception e) {
                                        // Ignore
                                    }
                                }
                            } else {
                                errorMsg = "Unknown error";
                            }
                        }
                        Toast.makeText(requireContext(), "Failed to update profile: " + errorMsg, Toast.LENGTH_LONG).show();
                    });
                }
        );
    }

    private void logout() {
        apiService.logout(
                response -> {
                    requireActivity().runOnUiThread(() -> {
                        // Clear token
                        new LoginManager(requireContext()).clear();
                        // Go to login
                        startActivity(new Intent(requireContext(), LoginActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        requireActivity().finish();
                    });
                },
                error -> {
                    requireActivity().runOnUiThread(() -> {
                        // Still clear token and go to login
                        new LoginManager(requireContext()).clear();
                        startActivity(new Intent(requireContext(), LoginActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        requireActivity().finish();
                    });
                }
        );
    }
}
