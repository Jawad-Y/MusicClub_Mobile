package com.music.musicclub;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

public class UserDetailFragment extends Fragment {
    public UserDetailFragment(){
        super(R.layout.user_detail_layout);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView nameTv = view.findViewById(R.id.name);
        TextView usernameTv = view.findViewById(R.id.username);
        TextView bioTv = view.findViewById(R.id.bio);
        Button editBtn = view.findViewById(R.id.btn_edit);
        View backBtn = view.findViewById(R.id.btn_back);

        // Default UI
        nameTv.setText("");
        usernameTv.setText("");
        bioTv.setText("");

        // Back button
        backBtn.setOnClickListener(v -> {
            requireActivity().onBackPressed();
        });

        Bundle args = getArguments();
        if (args != null && args.containsKey("user_json")) {
            try {
                JSONObject u = new JSONObject(args.getString("user_json"));
                nameTv.setText(u.optString("name", ""));
                usernameTv.setText(u.optString("username", u.optString("email", "")));

                String role = u.optString("role", "");
                String status = u.optString("status", "");
                String bio = "";
                if (!role.isEmpty()) bio += "Role: " + role + "\n";
                if (!status.isEmpty()) bio += "Status: " + status + "\n";
                bioTv.setText(bio.trim());

                // Put edit button visible
                editBtn.setVisibility(View.VISIBLE);
                editBtn.setOnClickListener(v -> {
                    // For now, just allow same fragment to edit fields or navigate to a dedicated edit screen later
                    // Could show a dialog to edit or enable fields
                });

            } catch (Exception ignored) {}
        }
    }
}
