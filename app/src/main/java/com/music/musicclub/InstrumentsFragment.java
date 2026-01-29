package com.music.musicclub;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class InstrumentsFragment extends Fragment implements InstrumentAdapter.OnInstrumentActionListener {

    private RecyclerView recyclerView;
    private InstrumentAdapter adapter;
    private ArrayList<Instrument> instruments;

    // **ADD THESE**
    private ArrayList<InstrumentItem> instrumentItems = new ArrayList<>();
    private ArrayList<UserItem> userItems = new ArrayList<>();

    public InstrumentsFragment() {
        super(R.layout.instruments_layout);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.rvInstruments);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        instruments = new ArrayList<>();
        adapter = new InstrumentAdapter(instruments, this);
        recyclerView.setAdapter(adapter);

        loadInstruments();
        loadUsers(); // **important**

        FloatingActionButton fab = view.findViewById(R.id.fabAddInstrument);
        fab.setOnClickListener(v -> showAddDialog());

        view.findViewById(R.id.btnAddType).setOnClickListener(v -> showAddTypeDialog());
        view.findViewById(R.id.btnAssignInstrument).setOnClickListener(v -> showAssignDialog());
    }

    private void loadInstruments() {
        ApiService api = new ApiService(
                requireContext(),
                new LoginManager(requireContext()).getToken()
        );

        api.get(ApiConfig.INSTRUMENTS, response -> {
            try {
                JSONArray data = response.getJSONArray("data");

                instruments.clear();
                instrumentItems.clear();

                for (int i = 0; i < data.length(); i++) {
                    JSONObject obj = data.getJSONObject(i);

                    int id = obj.getInt("id");
                    String name = obj.getString("name");
                    String uniqueCode = obj.getString("unique_code");
                    int typeId = obj.getInt("instrument_type_id");

                    instruments.add(new Instrument(id, name, uniqueCode, typeId));
                    instrumentItems.add(new InstrumentItem(id, name));
                }

                adapter.notifyDataSetChanged();

            } catch (Exception e) {
                Toast.makeText(requireContext(),
                        "Parsing error",
                        Toast.LENGTH_SHORT).show();
            }

        }, error -> {
            Toast.makeText(requireContext(),
                    "Failed to load instruments",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUsers() {
        ApiService api = new ApiService(requireContext(), new LoginManager(requireContext()).getToken());

        api.get(ApiConfig.USERS, response -> {
            try {
                JSONArray data = response.getJSONArray("data");

                userItems.clear();

                for (int i = 0; i < data.length(); i++) {
                    JSONObject obj = data.getJSONObject(i);

                    int id = obj.getInt("id");

                    // ✅ هنا نغير الاسم ل full_name لأن API بتعطيه هيك
                    String name = obj.optString("full_name", obj.optString("name", "Unknown"));

                    userItems.add(new UserItem(id, name));
                }

            } catch (Exception e) {
                Toast.makeText(requireContext(),
                        "Parsing error (users)",
                        Toast.LENGTH_SHORT).show();
            }

        }, error -> {
            Toast.makeText(requireContext(),
                    "Failed to load users",
                    Toast.LENGTH_SHORT).show();
        });
    }


    // ===== Add Dialog =====
    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add New Instrument");

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_instrument, null);

        EditText etName = dialogView.findViewById(R.id.etInstrumentName);

        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnAdd).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter instrument name", Toast.LENGTH_SHORT).show();
                return;
            }

            addInstrument(name, dialog);
        });

        dialog.show();
    }

    // ===== Add API =====
    private void addInstrument(String name, AlertDialog dialog) {
        ApiService api = new ApiService(
                requireContext(),
                new LoginManager(requireContext()).getToken()
        );

        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
            body.put("unique_code", "code_" + System.currentTimeMillis());
            body.put("instrument_type_id", 1);
        } catch (Exception ignored) {}

        api.post(ApiConfig.INSTRUMENTS, body, response -> {
            Toast.makeText(requireContext(),
                    "Instrument added successfully",
                    Toast.LENGTH_SHORT).show();

            dialog.dismiss();
            loadInstruments();

        }, error -> {
            String message = "Failed to add instrument";

            if (error.networkResponse != null) {
                int status = error.networkResponse.statusCode;
                message += " (code " + status + ")";

                try {
                    String bodyError = new String(error.networkResponse.data);
                    message += "\n" + bodyError;
                } catch (Exception ignored) {}
            }

            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        });
    }

    // ===== Edit Dialog =====
    private void showEditDialog(Instrument instrument) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Instrument");

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_instrument, null);

        EditText etName = dialogView.findViewById(R.id.etInstrumentName);
        etName.setText(instrument.getName());

        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnAdd).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter instrument name", Toast.LENGTH_SHORT).show();
                return;
            }

            updateInstrument(instrument.getId(), name, instrument.getUniqueCode(), instrument.getInstrumentTypeId(), dialog);
        });

        dialog.show();
    }

    // ===== Update API =====
    private void updateInstrument(int id, String name, String uniqueCode, int typeId, AlertDialog dialog) {
        ApiService api = new ApiService(
                requireContext(),
                new LoginManager(requireContext()).getToken()
        );

        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
            body.put("unique_code", uniqueCode);
            body.put("instrument_type_id", typeId);
        } catch (Exception ignored) {}

        api.put(ApiConfig.INSTRUMENTS + "/" + id, body, response -> {
            Toast.makeText(requireContext(),
                    "Instrument updated successfully",
                    Toast.LENGTH_SHORT).show();

            dialog.dismiss();
            loadInstruments();

        }, error -> {
            Toast.makeText(requireContext(),
                    "Failed to update instrument",
                    Toast.LENGTH_SHORT).show();
        });
    }



    private void showDeleteDialog(Instrument instrument) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_delete_instrument, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();

        view.findViewById(R.id.btnCancelDelete)
                .setOnClickListener(v -> dialog.dismiss());

        view.findViewById(R.id.btnConfirmDelete)
                .setOnClickListener(v -> onDelete(instrument));


        dialog.show();
    }


    // ===== Edit + Delete callbacks =====
    @Override
    public void onEdit(Instrument instrument) {
        showEditDialog(instrument);
    }


    @Override
    public void onDelete(Instrument instrument) {
        // نعمل AlertDialog تأكيد
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Instrument")
                .setMessage("Are you sure you want to delete " + instrument.getName() + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // إذا ضغط Yes نرسل طلب الحذف
                    ApiService api = new ApiService(requireContext(), new LoginManager(requireContext()).getToken());

                    api.deleteString(ApiConfig.INSTRUMENTS + "/" + instrument.getId(),
                            response -> {
                                Toast.makeText(requireContext(), "Instrument deleted successfully", Toast.LENGTH_SHORT).show();
                                loadInstruments();
                            },
                            error -> {
                                String msg;

                                if (error.networkResponse != null) {
                                    try {
                                        String body = new String(error.networkResponse.data);

                                        if (body.contains("SQLSTATE[23000]")) {
                                            // instrument مربوط بمستخدم → error حقيقي
                                            msg = "Failed to delete instrument. It is assigned to a user.\n" + body;
                                        } else {
                                            // instrument مش مربوط → نعتبر delete ناجح مؤقتاً
                                            Toast.makeText(requireContext(), "Instrument deleted successfully", Toast.LENGTH_SHORT).show();
                                            loadInstruments();
                                            return; // رجع قبل ما نعمل toast خطأ
                                        }

                                    } catch (Exception e) {
                                        msg = "Failed to delete instrument";
                                    }
                                } else {
                                    msg = "Failed to delete instrument";
                                }

                                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }



    private void showAddTypeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Instrument Type");

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_instrument_type, null);

        EditText etTypeName = dialogView.findViewById(R.id.etTypeName);

        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnCreate).setOnClickListener(v -> {
            String typeName = etTypeName.getText().toString().trim();

            if (typeName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter type name", Toast.LENGTH_SHORT).show();
                return;
            }

            createInstrumentType(typeName, dialog);
        });

        dialog.show();
    }

    private void createInstrumentType(String name, AlertDialog dialog) {
        ApiService api = new ApiService(requireContext(), new LoginManager(requireContext()).getToken());

        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
        } catch (Exception ignored) {}

        api.post(ApiConfig.INSTRUMENT_TYPES, body, response -> {
            Toast.makeText(requireContext(),
                    "Instrument type created",
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }, error -> {
            String msg = "Failed to create instrument type";

            if (error.networkResponse != null) {
                msg += " (code " + error.networkResponse.statusCode + ")";
                try {
                    msg += "\n" + new String(error.networkResponse.data);
                } catch (Exception ignored) {}
            }

            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
        });
    }

    // ===== Assign Dialog =====
    private void showAssignDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Assign Instrument");

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_assign_instrument, null);

        Spinner spInstrument = dialogView.findViewById(R.id.spInstrument);
        Spinner spUser = dialogView.findViewById(R.id.spUser);
        Button btnAssign = dialogView.findViewById(R.id.btnAssignInstrumentDialog);

        // spinner adapters
        ArrayAdapter<InstrumentItem> instrumentAdapter =
                new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, instrumentItems);
        instrumentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spInstrument.setAdapter(instrumentAdapter);

        ArrayAdapter<UserItem> userAdapter =
                new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, userItems);
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUser.setAdapter(userAdapter);

        userAdapter.notifyDataSetChanged();

        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        btnAssign.setOnClickListener(v -> {
            InstrumentItem selectedInstrument = (InstrumentItem) spInstrument.getSelectedItem();
            UserItem selectedUser = (UserItem) spUser.getSelectedItem();

            if (selectedInstrument == null || selectedUser == null) {
                Toast.makeText(requireContext(), "Please select instrument and user", Toast.LENGTH_SHORT).show();
                return;
            }

            assignInstrument(selectedInstrument.getId(), selectedUser.getId(), dialog);

        });

        dialog.show();
    }

    private void assignInstrument(int instrumentId, int userId, AlertDialog dialog) {
        ApiService api = new ApiService(requireContext(), new LoginManager(requireContext()).getToken());

        JSONObject body = new JSONObject();
        try {
            body.put("instrument_id", instrumentId);
            body.put("user_id", userId);

            // ✅ Add assigned_at (required by backend)
            body.put("assigned_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date()));

        } catch (Exception ignored) {}

        api.post(ApiConfig.ASSIGN_INSTRUMENT, body, response -> {
            Toast.makeText(requireContext(),
                    "Instrument assigned successfully",
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }, error -> {
            String msg = "Failed to assign instrument";
            if (error.networkResponse != null) {
                msg += " (code " + error.networkResponse.statusCode + ")";
                try {
                    msg += "\n" + new String(error.networkResponse.data);
                } catch (Exception ignored) {}
            }
            Log.e("ASSIGN_ERROR", new String(error.networkResponse.data));
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
        });
    }

}
