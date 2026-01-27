package com.music.musicclub;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class AddEditDepartmentFragment extends Fragment {

    private TextView tvTitle;
    private EditText etDeptName;
    private Spinner spinnerLeaders;
    private Button btnSave;

    private ArrayList<User> leaders = new ArrayList<>();
    private ArrayAdapter<User> spinnerAdapter;

    private int departmentId = -1; // -1 = ADD , otherwise EDIT

    public AddEditDepartmentFragment() {
        super(R.layout.add_edit_department_layout);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Views
        tvTitle = view.findViewById(R.id.tvTitle);
        etDeptName = view.findViewById(R.id.etDeptName);
        spinnerLeaders = view.findViewById(R.id.spinnerLeaders);
        btnSave = view.findViewById(R.id.btnSave);

        // Get arguments (Edit case)
        if (getArguments() != null) {
            departmentId = getArguments().getInt("department_id", -1);
        }

        // Title handling
        if (departmentId == -1) {
            tvTitle.setText("Add Department");
        } else {
            tvTitle.setText("Edit Department");
        }

        // Spinner setup
        spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                leaders
        );
        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        spinnerLeaders.setAdapter(spinnerAdapter);

        // Load leaders
        loadLeaders();

        // Save button
        btnSave.setOnClickListener(v -> saveDepartment());
    }

    private void loadLeaders() {
        LoginManager loginManager = new LoginManager(requireContext());
        ApiService api = new ApiService(requireContext(), loginManager.getToken());

        api.get(ApiConfig.USERS, response -> {

            leaders.clear();

            JSONArray arr = response.optJSONArray("data");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.optJSONObject(i);
                    if (obj != null) {
                        leaders.add(User.fromJson(obj));
                    }
                }
            }

            spinnerAdapter.notifyDataSetChanged();

            // If EDIT → load department after leaders are ready
            if (departmentId != -1) {
                loadDepartmentData();
            }

        }, error -> {
            Toast.makeText(requireContext(),
                    "Failed to load leaders",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void loadDepartmentData() {
        LoginManager loginManager = new LoginManager(requireContext());
        ApiService api = new ApiService(requireContext(), loginManager.getToken());

        api.get(ApiConfig.DEPARTMENTS + "/" + departmentId, response -> {

            JSONObject data = response.optJSONObject("data");
            if (data != null) {

                etDeptName.setText(data.optString("department_name", ""));

                int leaderId = data.optInt("leader_id", -1);

                // Select leader in spinner
                for (int i = 0; i < leaders.size(); i++) {
                    if (leaders.get(i).getId() == leaderId) {
                        spinnerLeaders.setSelection(i);
                        break;
                    }
                }
            }

        }, error -> {
            Toast.makeText(requireContext(),
                    "Failed to load department",
                    Toast.LENGTH_SHORT).show();
        });
    }


    private void saveDepartment() {
        String name = etDeptName.getText().toString().trim();

        if (name.isEmpty()) {
            etDeptName.setError("Department name required");
            return;
        }

        if (leaders.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Leaders not loaded yet",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        User selectedLeader = (User) spinnerLeaders.getSelectedItem();

        JSONObject body = new JSONObject();
        try {
            body.put("department_name", name);
            body.put("leader_id", selectedLeader.getId());
        } catch (Exception ignored) {}

        LoginManager loginManager = new LoginManager(requireContext());
        ApiService api = new ApiService(requireContext(), loginManager.getToken());

        String url = ApiConfig.DEPARTMENTS;
        if (departmentId != -1) {
            url += "/" + departmentId;
            api.put(url, body, response -> {
                Toast.makeText(requireContext(),
                        "Department updated",
                        Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).popBackStack();
            }, error -> {
                Toast.makeText(requireContext(),
                        "Failed to update department",
                        Toast.LENGTH_SHORT).show();
            });
        } else {
            api.post(url, body, response -> {
                Toast.makeText(requireContext(),
                        "Department saved",
                        Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).popBackStack();
            }, error -> {
                Toast.makeText(requireContext(),
                        "Failed to save department",
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

}
