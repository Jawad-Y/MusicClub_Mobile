package com.music.musicclub;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CreateClassDialog {
    public interface Callback { void onCreate(String className, int leaderId, int departmentId); }

    public static void show(@NonNull Context ctx, @NonNull Callback cb) {
        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(12 * ctx.getResources().getDisplayMetrics().density);
        container.setPadding(pad,pad,pad,pad);

        EditText etName = new EditText(ctx);
        etName.setHint("Class name");
        container.addView(etName);

        // Department spinner
        TextView deptLabel = new TextView(ctx);
        deptLabel.setText("Department:");
        deptLabel.setPadding(0, 16, 0, 4);
        container.addView(deptLabel);
        
        Spinner spinnerDept = new Spinner(ctx);
        container.addView(spinnerDept);
        
        final List<Integer> deptIds = new ArrayList<>();
        final List<String> deptNames = new ArrayList<>();
        
        // Load departments
        LoginManager lm = new LoginManager(ctx);
        ApiService api = new ApiService(ctx, lm.getToken());
        api.get(ApiConfig.DEPARTMENTS, response -> {
            try {
                JSONArray arr = response.optJSONArray("data");
                if (arr == null) arr = response.optJSONArray("departments");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject d = arr.optJSONObject(i);
                        if (d != null) {
                            deptIds.add(d.optInt("id", -1));
                            deptNames.add(d.optString("department_name", d.optString("name", "Unknown")));
                        }
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item, deptNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDept.setAdapter(adapter);
            } catch (Exception ignored) {}
        }, error -> Toast.makeText(ctx, "Failed to load departments", Toast.LENGTH_SHORT).show());

        TextView leaderLabel = new TextView(ctx);
        leaderLabel.setText("Leader: (optional)");
        leaderLabel.setPadding(0, 16, 0, 0);
        container.addView(leaderLabel);

        // selected leader id holder
        final int[] selectedLeader = {-1};
        TextView leaderChosen = new TextView(ctx);
        leaderChosen.setText("None");
        leaderChosen.setPadding(0,8,0,0);
        container.addView(leaderChosen);

        // open picker when label clicked
        leaderLabel.setOnClickListener(v -> {
            List<Integer> exclude = new ArrayList<>();
            ClassDetailFragment.MemberPickerDialog.show(ctx, exclude, (userId, roleId, roleName) -> {
                selectedLeader[0] = userId;
                leaderChosen.setText("Selected user: " + userId + (roleName != null ? (" (" + roleName + ")") : (roleId > 0 ? (" (role:" + roleId + ")") : "")));
            });
        });

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setTitle("Create Class")
                .setView(container)
                .setPositiveButton("Create", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
        
        // Override positive button to prevent auto-dismiss on validation failure
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(ctx, "Please enter a class name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (deptIds.isEmpty() || spinnerDept.getSelectedItemPosition() < 0) {
                Toast.makeText(ctx, "Please select a department", Toast.LENGTH_SHORT).show();
                return;
            }
            int deptId = deptIds.get(spinnerDept.getSelectedItemPosition());
            cb.onCreate(name, selectedLeader[0], deptId);
            dialog.dismiss();
        });
    }

    // For editing existing class: prefill
    public static void showEdit(@NonNull Context ctx, @NonNull String initialName, int initialLeaderId, int initialDeptId, @NonNull Callback cb) {
        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(12 * ctx.getResources().getDisplayMetrics().density);
        container.setPadding(pad,pad,pad,pad);

        EditText etName = new EditText(ctx);
        etName.setHint("Class name");
        etName.setText(initialName);
        container.addView(etName);

        // Department spinner
        TextView deptLabel = new TextView(ctx);
        deptLabel.setText("Department:");
        deptLabel.setPadding(0, 16, 0, 4);
        container.addView(deptLabel);
        
        Spinner spinnerDept = new Spinner(ctx);
        container.addView(spinnerDept);
        
        final List<Integer> deptIds = new ArrayList<>();
        final List<String> deptNames = new ArrayList<>();
        
        // Load departments
        LoginManager lm = new LoginManager(ctx);
        ApiService api = new ApiService(ctx, lm.getToken());
        api.get(ApiConfig.DEPARTMENTS, response -> {
            try {
                JSONArray arr = response.optJSONArray("data");
                if (arr == null) arr = response.optJSONArray("departments");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject d = arr.optJSONObject(i);
                        if (d != null) {
                            deptIds.add(d.optInt("id", -1));
                            deptNames.add(d.optString("department_name", d.optString("name", "Unknown")));
                        }
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item, deptNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDept.setAdapter(adapter);
                
                // Select initial department
                int pos = deptIds.indexOf(initialDeptId);
                if (pos >= 0) spinnerDept.setSelection(pos);
            } catch (Exception ignored) {}
        }, error -> Toast.makeText(ctx, "Failed to load departments", Toast.LENGTH_SHORT).show());

        TextView leaderLabel = new TextView(ctx);
        leaderLabel.setText("Leader: (optional)");
        leaderLabel.setPadding(0, 16, 0, 0);
        container.addView(leaderLabel);

        final int[] selectedLeader = {initialLeaderId};
        TextView leaderChosen = new TextView(ctx);
        leaderChosen.setText(initialLeaderId > 0 ? ("Selected user: " + initialLeaderId) : "None");
        leaderChosen.setPadding(0,8,0,0);
        container.addView(leaderChosen);

        leaderLabel.setOnClickListener(v -> {
            List<Integer> exclude = new ArrayList<>();
            ClassDetailFragment.MemberPickerDialog.show(ctx, exclude, (userId, roleId, roleName) -> {
                selectedLeader[0] = userId;
                leaderChosen.setText("Selected user: " + userId + (roleName != null ? (" (" + roleName + ")") : (roleId > 0 ? (" (role:" + roleId + ")") : "")));
            });
        });

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setTitle("Edit Class")
                .setView(container)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
        
        // Override positive button to prevent auto-dismiss on validation failure
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(ctx, "Please enter a class name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (deptIds.isEmpty() || spinnerDept.getSelectedItemPosition() < 0) {
                Toast.makeText(ctx, "Please select a department", Toast.LENGTH_SHORT).show();
                return;
            }
            int deptId = deptIds.get(spinnerDept.getSelectedItemPosition());
            cb.onCreate(name, selectedLeader[0], deptId);
            dialog.dismiss();
        });
    }
}
