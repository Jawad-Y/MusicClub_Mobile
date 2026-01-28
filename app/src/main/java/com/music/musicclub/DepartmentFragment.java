package com.music.musicclub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class DepartmentFragment extends Fragment {

    private RecyclerView recyclerView;
    private DepartmentAdapter adapter;
    private ArrayList<Department> departments;
    private MaterialButton btnAddDepartment;

    public DepartmentFragment() {
        // empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.department_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.rvDepartments);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        btnAddDepartment = view.findViewById(R.id.btnAddDepartment);

        departments = new ArrayList<>();
        adapter = new DepartmentAdapter(this, departments);
        recyclerView.setAdapter(adapter);

        // 1) Add Department button
        btnAddDepartment.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_department_to_addEditDepartment);
        });

        // 2) Load departments from API
        loadDepartments();
    }

    private void loadDepartments() {
        LoginManager loginManager = new LoginManager(requireContext());
        ApiService api = new ApiService(requireContext(), loginManager.getToken());

        api.get(ApiConfig.DEPARTMENTS, response -> {

            departments.clear();

            JSONArray arr = response.optJSONArray("data");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.optJSONObject(i);
                    if (obj != null) {

                        int id = obj.optInt("id");
                        String name = obj.optString("department_name", "Unknown");
                        int leaderId = obj.optInt("leader_id", -1);

                        // ⭐ هنا نجيب leader object
                        JSONObject leaderObj = obj.optJSONObject("leader");
                        String leaderName = "No Leader";

                        if (leaderObj != null) {
                            leaderName = leaderObj.optString("full_name", "No Leader");
                        }

                        departments.add(new Department(id, name, leaderId, leaderName));
                    }
                }
            }

            adapter.notifyDataSetChanged();

        }, error -> {
            Toast.makeText(requireContext(), "Failed to load departments", Toast.LENGTH_SHORT).show();
        });
    }


}
