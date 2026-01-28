package com.music.musicclub;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UsersFragment extends Fragment {

    private static final String TAG = "UsersFragment";

    private RecyclerView recyclerView;
    private UsersAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;

    public UsersFragment(){
        super(R.layout.users_layout);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.usersRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);

        adapter = new UsersAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Listen for clicks from adapter
        adapter.setOnItemActionListener(new UsersAdapter.OnItemActionListener() {
            @Override
            public void onItemClicked(User user) {
                navigateToDetail(user);
            }

            @Override
            public void onEditClicked(User user) {
                navigateToDetail(user); // same destination, detail screen can allow editing
            }

            @Override
            public void onDeleteClicked(User user) {
                showDeleteConfirmation(user);
            }
        });

        // Add User button (FAB)
        view.findViewById(R.id.fab).setOnClickListener(v -> {
            navigateToCreateUser();
        });

        loadUsers();
    }

    private void navigateToDetail(User user) {
        if (getView() == null) return;
        try {
            Bundle b = new Bundle();
            b.putInt("user_id", user.getId());
            // Put a compact JSON representation so detail fragment can show immediately without refetch
            JSONObject json = new JSONObject();
            json.put("id", user.getId());
            json.put("name", user.getName());
            json.put("role", user.getRole());
            json.put("status", user.getStatus());
            b.putString("user_json", json.toString());
            Navigation.findNavController(getView()).navigate(R.id.userDetailFragment, b);
        } catch (Exception e) {
            Log.e(TAG, "navigate error", e);
        }
    }

    private void navigateToCreateUser() {
        if (getView() == null) return;
        try {
            Bundle b = new Bundle();
            b.putInt("user_id", -1); // -1 indicates new user creation
            Navigation.findNavController(getView()).navigate(R.id.userDetailFragment, b);
        } catch (Exception e) {
            Log.e(TAG, "navigate to create user error", e);
        }
    }

    private void loadUsers() {
        showLoading(true);

        // Use the app's ApiService and ApiConfig to call the real backend
        String token = new LoginManager(requireContext()).getToken();
        ApiService apiService = new ApiService(requireContext(), token);

        apiService.get(
                ApiConfig.USERS,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        List<User> users = new ArrayList<>();
                        try {
                            JSONArray arr = null;

                            // Common patterns: { data: [...] } or { users: [...] }
                            if (response != null) {
                                if (response.optJSONArray("data") != null) {
                                    arr = response.optJSONArray("data");
                                } else if (response.optJSONArray("users") != null) {
                                    arr = response.optJSONArray("users");
                                } else {
                                    // Try to find any top-level JSONArray value
                                    JSONArray names = response.names();
                                    if (names != null) {
                                        for (int i = 0; i < names.length(); i++) {
                                            String key = names.optString(i);
                                            Object o = response.opt(key);
                                            if (o instanceof JSONArray) {
                                                arr = (JSONArray) o;
                                                break;
                                            }
                                        }
                                    }
                                }

                                if (arr != null) {
                                    for (int i = 0; i < arr.length(); i++) {
                                        JSONObject obj = arr.optJSONObject(i);
                                        if (obj != null) {
                                            Log.d(TAG, "Raw user json: " + obj.toString());
                                            users.add(User.fromJson(obj));
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "parse users response", e);
                        }

                        adapter.setItems(users);
                        showLoading(false);
                        showEmpty(users.isEmpty());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "users api error", error);
                        showLoading(false);
                        showEmpty(true);
                    }
                }
        );
    }

    private void showLoading(boolean visible) {
        progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(visible ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    private void showEmpty(boolean isEmpty) {
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showDeleteConfirmation(User user) {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + user.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    performDeleteUser(user);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void performDeleteUser(User user) {
        String token = new LoginManager(requireContext()).getToken();
        ApiService apiService = new ApiService(requireContext(), token);
        String deleteUrl = ApiConfig.USERS + "/" + user.getId();
        
        Log.d(TAG, "Deleting user: " + deleteUrl);
        
        // Use a custom StringRequest for DELETE since we might get 204 No Content
        com.android.volley.toolbox.StringRequest deleteRequest = new com.android.volley.toolbox.StringRequest(
                com.android.volley.Request.Method.DELETE,
                deleteUrl,
                response -> {
                    requireActivity().runOnUiThread(() -> {
                        android.widget.Toast.makeText(requireContext(), "User deleted successfully", android.widget.Toast.LENGTH_SHORT).show();
                        loadUsers(); // Reload the list
                    });
                },
                error -> {
                    int status = -1;
                    String respBody = null;
                    if (error != null && error.networkResponse != null) {
                        status = error.networkResponse.statusCode;
                        try {
                            respBody = new String(error.networkResponse.data, "UTF-8");
                        } catch (Exception ex) {
                            respBody = null;
                        }
                    }
                    
                    // Treat 2xx responses as success even with empty body (e.g., 204 No Content)
                    if (status >= 200 && status < 300) {
                        Log.i(TAG, "DELETE returned 2xx with empty body — treating as success, status=" + status);
                        requireActivity().runOnUiThread(() -> {
                            android.widget.Toast.makeText(requireContext(), "User deleted successfully", android.widget.Toast.LENGTH_SHORT).show();
                            loadUsers(); // Reload the list
                        });
                        return;
                    }
                    
                    Log.e(TAG, "Delete user failed: status=" + status + " body=" + respBody, error);
                    final int finalStatus = status;
                    final String finalRespBody = respBody;
                    requireActivity().runOnUiThread(() -> {
                        android.widget.Toast.makeText(requireContext(), "Delete failed (code=" + finalStatus + ")", android.widget.Toast.LENGTH_LONG).show();
                        try {
                            String msg = "Status: " + finalStatus + "\n" + (finalRespBody != null ? finalRespBody : "(no body)");
                            new android.app.AlertDialog.Builder(requireContext())
                                    .setTitle("Delete Failed")
                                    .setMessage(msg)
                                    .setPositiveButton("OK", (d, w) -> d.dismiss())
                                    .show();
                        } catch (Exception ignored) {}
                    });
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() throws com.android.volley.AuthFailureError {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Accept", "application/json");
                if (token != null && !token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                }
                return headers;
            }
        };
        
        deleteRequest.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                15000,
                1,
                1.0f
        ));
        
        VolleySingleton.getInstance(requireContext()).add(deleteRequest);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // no-op
    }
}
