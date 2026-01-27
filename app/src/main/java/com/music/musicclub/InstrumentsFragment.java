package com.music.musicclub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
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

import java.util.ArrayList;

public class InstrumentsFragment extends Fragment implements InstrumentAdapter.OnInstrumentActionListener {

    private RecyclerView recyclerView;
    private InstrumentAdapter adapter;
    private ArrayList<Instrument> instruments;

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

        FloatingActionButton fab = view.findViewById(R.id.fabAddInstrument);
        fab.setOnClickListener(v -> showAddDialog());

        view.findViewById(R.id.btnAddType).setOnClickListener(v -> showAddTypeDialog());


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

                for (int i = 0; i < data.length(); i++) {
                    JSONObject obj = data.getJSONObject(i);

                    int id = obj.getInt("id");
                    String name = obj.getString("name");
                    String uniqueCode = obj.getString("unique_code");
                    int typeId = obj.getInt("instrument_type_id");

                    instruments.add(new Instrument(id, name, uniqueCode, typeId));
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

    // ===== Delete API =====
    private void deleteInstrument(int id) {
        ApiService api = new ApiService(
                requireContext(),
                new LoginManager(requireContext()).getToken()
        );

        api.delete(ApiConfig.INSTRUMENTS + "/" + id, response -> {
            Toast.makeText(requireContext(),
                    "Instrument deleted successfully",
                    Toast.LENGTH_SHORT).show();

            loadInstruments();

        }, error -> {
            Toast.makeText(requireContext(),
                    "Failed to delete instrument",
                    Toast.LENGTH_SHORT).show();
        });
    }

    // ===== Edit + Delete callbacks =====
    @Override
    public void onEdit(Instrument instrument) {
        showEditDialog(instrument);
    }

    @Override
    public void onDelete(Instrument instrument) {
        ApiService api = new ApiService(requireContext(), new LoginManager(requireContext()).getToken());

        api.deleteString(ApiConfig.INSTRUMENTS + "/" + instrument.getId(),
                response -> {
                    Toast.makeText(requireContext(),
                            "Instrument deleted successfully",
                            Toast.LENGTH_SHORT).show();
                    loadInstruments();
                },
                error -> {
                    String msg = "Failed to delete instrument";

                    if (error.networkResponse != null) {
                        msg += " (code " + error.networkResponse.statusCode + ")";
                        try {
                            msg += "\n" + new String(error.networkResponse.data);
                        } catch (Exception ignored) {}
                    }

                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                });
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



}
