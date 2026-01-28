package com.music.musicclub;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.music.musicclub.TrainingSessionAdapter;
import com.music.musicclub.TrainingSession;
import com.music.musicclub.ApiConfig;
import com.music.musicclub.ApiService;

public class TrainingFragment extends Fragment {

    private static final String TAG = "TrainingFragment";

    private RecyclerView recyclerView;
    private TrainingSessionAdapter adapter;
    private ProgressBar progressBar;
    private MaterialButton btnAddSession, btnAllSessions, btnUpcomingSessions;

    private List<TrainingSession> allSessions = new ArrayList<>();

    private enum FilterType { ALL, UPCOMING, ATTENDANCE }
    private FilterType currentFilter = FilterType.ALL;

    private View lineAll, lineUpcoming;

    public TrainingFragment() {
        super(R.layout.training_layout);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        recyclerView = view.findViewById(R.id.sessionRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        btnAddSession = view.findViewById(R.id.btnAddSession);
        btnAllSessions = view.findViewById(R.id.btnAllSessions);
        btnUpcomingSessions = view.findViewById(R.id.btnUpcomingSessions);

        // Setup RecyclerView & Adapter
        adapter = new TrainingSessionAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemActionListener(this::navigateToDetail);


        lineAll = view.findViewById(R.id.lineAll);
        lineUpcoming = view.findViewById(R.id.lineUpcoming);


        lineAll.setVisibility(View.VISIBLE);
        lineUpcoming.setVisibility(View.INVISIBLE);


        // Button listeners
        btnAddSession.setOnClickListener(v -> showAddSessionDialog());




        lineAll.setVisibility(View.VISIBLE);
        lineUpcoming.setVisibility(View.INVISIBLE);

        btnAllSessions.setOnClickListener(v -> {
            currentFilter = FilterType.ALL;
            adapter.setItems(new ArrayList<>(allSessions));
            lineAll.setVisibility(View.VISIBLE);
            lineUpcoming.setVisibility(View.INVISIBLE);

            btnAllSessions.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            );
            btnUpcomingSessions.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorAccent)
            );
        });

        btnUpcomingSessions.setOnClickListener(v -> {
            currentFilter = FilterType.UPCOMING;
            filterUpcomingSessions();
            lineAll.setVisibility(View.INVISIBLE);
            lineUpcoming.setVisibility(View.VISIBLE);

            btnUpcomingSessions.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            );
            btnAllSessions.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorAccent)
            );
        });




        loadSessions();
    }

    // --- Filter upcoming sessions ---
    private void filterUpcomingSessions() {
        List<TrainingSession> upcoming = new ArrayList<>();
        SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date today = new Date();

        for (TrainingSession s : allSessions) {
            try {
                Date sessionDate = apiFormat.parse(s.getDate());
                if (sessionDate != null && sessionDate.after(today)) upcoming.add(s);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing date", e);
            }
        }

        adapter.setItems(upcoming);
    }




    // --- Add session dialog ---
    private void showAddSessionDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_session, null);

        EditText etSubject = dialogView.findViewById(R.id.etSubject);
        EditText etDate = dialogView.findViewById(R.id.etDate);
        EditText etStartTime = dialogView.findViewById(R.id.etStartTime);
        EditText etEndTime = dialogView.findViewById(R.id.etEndTime);
        EditText etLocation = dialogView.findViewById(R.id.etLocation);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);

        // Date picker
        etDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(requireContext(),
                    (view, year, month, day) -> etDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day)),
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        // Time pickers
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime));

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Training Session")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> saveSession(etSubject, etDate, etStartTime, etEndTime, etLocation, etDescription))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showTimePicker(EditText target) {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(requireContext(),
                (view, hour, minute) -> target.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute)),
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
        ).show();
    }

    // --- Save session ---
    private void saveSession(EditText etSubject, EditText etDate, EditText etStartTime,
                             EditText etEndTime, EditText etLocation, EditText etDescription) {
        try {
            JSONObject body = new JSONObject();
            body.put("subject", etSubject.getText().toString().trim());
            body.put("date", etDate.getText().toString().trim());
            body.put("start_time", etStartTime.getText().toString().trim());
            body.put("end_time", etEndTime.getText().toString().trim());
            body.put("location", etLocation.getText().toString().trim());
            body.put("description", etDescription.getText().toString().trim());

            String token = new LoginManager(requireContext()).getToken();
            ApiService apiService = new ApiService(requireContext(), token);

            apiService.post(ApiConfig.TRAINING_SESSIONS, body,
                    response -> {
                        TrainingSession newSession = new TrainingSession();
                        newSession.setId(response.optInt("id", -1));
                        newSession.setSubject(etSubject.getText().toString().trim());
                        newSession.setDate(etDate.getText().toString().trim());
                        newSession.setStartTime(etStartTime.getText().toString().trim());
                        newSession.setEndTime(etEndTime.getText().toString().trim());
                        newSession.setLocation(etLocation.getText().toString().trim());
                        newSession.setDescription(etDescription.getText().toString().trim());

                        allSessions.add(newSession);
                        adapter.setItems(new ArrayList<>(allSessions));
                        adapter.highlightLastItem();
                        recyclerView.scrollToPosition(allSessions.size() - 1);

                        Toast.makeText(requireContext(), "Session added!", Toast.LENGTH_SHORT).show();
                    },
                    error -> Log.e(TAG, "Add session error", error)
            );
        } catch (Exception e) {
            Log.e(TAG, "Save session error", e);
        }
    }

    // --- Navigate to detail ---
    private void navigateToDetail(TrainingSession session) {
        if (getView() == null) return;
        try {
            Bundle b = new Bundle();
            b.putInt("session_id", session.getId());

            JSONObject json = new JSONObject();
            json.put("id", session.getId());
            json.put("subject", session.getSubject());
            json.put("date", session.getDate());
            json.put("start_time", session.getStartTime());
            json.put("end_time", session.getEndTime());
            json.put("location", session.getLocation());
            json.put("description", session.getDescription());
            json.put("trainer_id", session.getTrainerId());
            json.put("trainer_name", session.getTrainerName());
            json.put("attendance_total", session.getAttendanceTotal());
            json.put("attendance_present", session.getAttendancePresent());
            json.put("attendance_absent", session.getAttendanceAbsent());
            json.put("attendance_late", session.getAttendanceLate());

            b.putString("session_json", json.toString());

            androidx.navigation.Navigation.findNavController(getView())
                    .navigate(R.id.SessionFragment, b);
        } catch (Exception e) {
            Log.e(TAG, "Navigate error", e);
        }
    }

    // --- Load sessions ---
    private void loadSessions() {
        showLoading(true);

        String token = new LoginManager(requireContext()).getToken();
        ApiService apiService = new ApiService(requireContext(), token);

        apiService.get(ApiConfig.TRAINING_SESSIONS,
                response -> {
                    try {
                        JSONArray arr = response.getJSONArray("data");
                        List<TrainingSession> sessions = new ArrayList<>();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            sessions.add(TrainingSession.fromJson(obj));
                        }

                        allSessions.clear();
                        allSessions.addAll(sessions);
                        adapter.setItems(new ArrayList<>(allSessions));
                    } catch (Exception e) {
                        Log.e(TAG, "Parse sessions response", e);
                    } finally {
                        showLoading(false);
                    }
                },
                error -> {
                    Log.e(TAG, "Sessions API error", error);
                    showLoading(false);
                }
        );
    }

    private void showLoading(boolean visible) {
        progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(visible ? View.GONE : View.VISIBLE);
    }
}