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
                // simple: show empty or later implement delete
                Log.i(TAG, "delete clicked for user: " + user.getId());
            }
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // no-op
    }
}
