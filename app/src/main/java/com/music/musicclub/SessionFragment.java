package com.music.musicclub;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SessionFragment extends Fragment {

    private static final String TAG = "SessionDetailFragment";

    private TextView tvTitle, tvDate, tvTime, tvTrainer, tvLocation, tvDescription,
            tvAttendanceTotal, tvAttendancePresent, tvAttendanceAbsent, tvAttendanceLate;
    private MaterialButton btnEdit, btnDelete;

    private TrainingSession session;

    public SessionFragment() {
        super(R.layout.session_layout); // Correct layout
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        tvTitle = view.findViewById(R.id.tvSessionTitle);
        tvDate = view.findViewById(R.id.tvDate);
        tvTime = view.findViewById(R.id.tvTime);
        tvTrainer = view.findViewById(R.id.tvTrainer);
        tvLocation = view.findViewById(R.id.tvLocation);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvAttendanceTotal = view.findViewById(R.id.tvAttendanceTotal);
        tvAttendancePresent = view.findViewById(R.id.tvAttendancePresent);
        tvAttendanceAbsent = view.findViewById(R.id.tvAttendanceAbsent);
        tvAttendanceLate = view.findViewById(R.id.tvAttendanceLate);

        btnEdit = view.findViewById(R.id.btnEdit);
        btnDelete = view.findViewById(R.id.btnDelete);

        // Get session from arguments
        Bundle args = getArguments();
        if (args != null) {
            String jsonStr = args.getString("session_json");
            try {
                JSONObject obj = new JSONObject(jsonStr);
                session = TrainingSession.fromJson(obj);

                bindSession(session); // Show initial data
                loadSessionAttendance(session.getId()); // Update attendance from API

            } catch (Exception e) {
                Log.e(TAG, "Error parsing session JSON", e);
            }
        }

        btnEdit.setOnClickListener(v -> showEditDialog());
        btnDelete.setOnClickListener(v -> showDeleteDialog());
    }

    // --- Bind session details to UI ---
    private void bindSession(TrainingSession s) {
        tvTitle.setText(s.getSubject());
        tvDate.setText(s.getDate());
        tvTime.setText(s.getStartTime() + " - " + s.getEndTime());
        tvTrainer.setText(s.getTrainerName());
        tvLocation.setText(s.getLocation());
        tvDescription.setText(s.getDescription());

        tvAttendanceTotal.setText("Total: " + s.getAttendanceTotal());
        tvAttendancePresent.setText("Present: " + s.getAttendancePresent());
        tvAttendanceAbsent.setText("Absent: " + s.getAttendanceAbsent());
        tvAttendanceLate.setText("Late: " + s.getAttendanceLate());
    }

    // --- Load attendance from API ---
    private void loadSessionAttendance(int sessionId) {
        String token = new LoginManager(requireContext()).getToken();
        ApiService apiService = new ApiService(requireContext(), token);

        apiService.get(ApiConfig.ATTENDANCE,
                response -> {
                    try {
                        JSONArray arr = response.optJSONArray("data");
                        if (arr != null) {
                            // Reset counts
                            session.setAttendanceTotal(0);
                            session.setAttendancePresent(0);
                            session.setAttendanceAbsent(0);
                            session.setAttendanceLate(0);

                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.optJSONObject(i);
                                if (obj != null && obj.optInt("session_id", -1) == sessionId) {
                                    String status = obj.optString("status", "");
                                    session.setAttendanceTotal(session.getAttendanceTotal() + 1);

                                    switch (status) {
                                        case "present":
                                            session.setAttendancePresent(session.getAttendancePresent() + 1);
                                            break;
                                        case "absent":
                                            session.setAttendanceAbsent(session.getAttendanceAbsent() + 1);
                                            break;
                                        case "late":
                                            session.setAttendanceLate(session.getAttendanceLate() + 1);
                                            break;
                                    }
                                }
                            }
                        }

                        // Update UI
                        bindSession(session);

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing attendance", e);
                    }
                },
                error -> Log.e(TAG, "Attendance API error", error)
        );
    }

    // --- Edit dialog ---
    private void showEditDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_session, null);

        EditText etSubject = dialogView.findViewById(R.id.etSubject);
        EditText etDate = dialogView.findViewById(R.id.etDate);
        EditText etStartTime = dialogView.findViewById(R.id.etStartTime);
        EditText etEndTime = dialogView.findViewById(R.id.etEndTime);
        EditText etLocation = dialogView.findViewById(R.id.etLocation);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        Spinner spTrainer = dialogView.findViewById(R.id.spTrainer);

        // Pre-fill current values
        etSubject.setText(session.getSubject());
        etDate.setText(session.getDate());
        etStartTime.setText(session.getStartTime());
        etEndTime.setText(session.getEndTime());
        etLocation.setText(session.getLocation());
        etDescription.setText(session.getDescription());

        // Load trainers dynamically
        String token = new LoginManager(requireContext()).getToken();
        ApiService apiService = new ApiService(requireContext(), token);

        apiService.get(ApiConfig.USERS,
                response -> {
                    try {
                        List<String> trainerNames = new ArrayList<>();
                        List<Integer> trainerIds = new ArrayList<>();

                        JSONArray arr = response.optJSONArray("data");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.optJSONObject(i);
                                if (obj != null && obj.optInt("role_id") == 7) { // Only trainers
                                    trainerNames.add(obj.optString("full_name"));
                                    trainerIds.add(obj.optInt("id"));
                                }
                            }
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                                android.R.layout.simple_spinner_item, trainerNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spTrainer.setAdapter(adapter);

                        // Pre-select current trainer
                        int pos = trainerNames.indexOf(session.getTrainerName());
                        if (pos >= 0) spTrainer.setSelection(pos);

                        // Save trainerIds for saving
                        spTrainer.setTag(trainerIds);

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing trainers", e);
                    }
                },
                error -> Log.e(TAG, "Trainer API error", error)
        );

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Session")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> saveEditedSession(etSubject, etDate, etStartTime, etEndTime,
                        etLocation, etDescription, spTrainer, apiService))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // --- Save edited session ---
    private void saveEditedSession(EditText etSubject, EditText etDate, EditText etStartTime, EditText etEndTime,
                                   EditText etLocation, EditText etDescription, Spinner spTrainer, ApiService apiService) {
        try {
            JSONObject body = session.toJson(); // get original JSON
            body.put("subject", etSubject.getText().toString());
            body.put("date", etDate.getText().toString());
            body.put("start_time", etStartTime.getText().toString());
            body.put("end_time", etEndTime.getText().toString());
            body.put("location", etLocation.getText().toString());
            body.put("description", etDescription.getText().toString());

            int selectedPos = spTrainer.getSelectedItemPosition();
            List<Integer> trainerIds = (List<Integer>) spTrainer.getTag();
            if (trainerIds != null && selectedPos >= 0 && selectedPos < trainerIds.size()) {
                int selectedTrainerId = trainerIds.get(selectedPos);
                body.put("trainer_id", selectedTrainerId);
                session.setTrainerId(selectedTrainerId);
            }

            apiService.put(ApiConfig.TRAINING_SESSIONS + "/" + session.getId(), body,
                    resp -> {
                        // Update session object and UI
                        session.setSubject(etSubject.getText().toString());
                        session.setDate(etDate.getText().toString());
                        session.setStartTime(etStartTime.getText().toString());
                        session.setEndTime(etEndTime.getText().toString());
                        session.setLocation(etLocation.getText().toString());
                        session.setDescription(etDescription.getText().toString());
                        session.setTrainerName(spTrainer.getSelectedItem().toString());
                        bindSession(session);
                    },
                    err -> Log.e(TAG, "Edit session error", err)
            );
        } catch (Exception e) {
            Log.e(TAG, "Save edited session error", e);
        }
    }

    // --- Delete dialog ---
    private void showDeleteDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Session")
                .setMessage("Are you sure you want to delete this session?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    String token = new LoginManager(requireContext()).getToken();
                    ApiService apiService = new ApiService(requireContext(), token);

                    apiService.delete(ApiConfig.TRAINING_SESSIONS + "/" + session.getId(),
                            response -> {
                                Log.i(TAG, "Session deleted");
                                if (getView() != null) {
                                    androidx.navigation.Navigation.findNavController(getView()).popBackStack();
                                }
                            },
                            error -> Log.e(TAG, "Delete session error", error)
                    );
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}