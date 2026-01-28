package com.music.musicclub;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class CreateClassDialog {
    public interface Callback { void onCreate(String className, int leaderId); }

    public static void show(@NonNull Context ctx, @NonNull Callback cb) {
        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(12 * ctx.getResources().getDisplayMetrics().density);
        container.setPadding(pad,pad,pad,pad);

        EditText etName = new EditText(ctx);
        etName.setHint("Class name");
        container.addView(etName);

        TextView leaderLabel = new TextView(ctx);
        leaderLabel.setText("Leader: (optional)");
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
                .setPositiveButton("Create", (d,w) -> {
                    String name = etName.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(ctx, "Please enter a class name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    cb.onCreate(name, selectedLeader[0]);
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    // For editing existing class: prefill
    public static void showEdit(@NonNull Context ctx, @NonNull String initialName, int initialLeaderId, @NonNull Callback cb) {
        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(12 * ctx.getResources().getDisplayMetrics().density);
        container.setPadding(pad,pad,pad,pad);

        EditText etName = new EditText(ctx);
        etName.setHint("Class name");
        etName.setText(initialName);
        container.addView(etName);

        TextView leaderLabel = new TextView(ctx);
        leaderLabel.setText("Leader: (optional)");
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
                .setPositiveButton("Save", (d,w) -> {
                    String name = etName.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(ctx, "Please enter a class name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    cb.onCreate(name, selectedLeader[0]);
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }
}
