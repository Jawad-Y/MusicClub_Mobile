package com.music.musicclub;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class UserDetailFragment extends Fragment {
    private static final String TAG = "UserDetailFragment";
    private int currentUserId = -1;
    private int currentUserRoleId = -1;
    private String currentUserStatus = "active";
    private String currentUserPhone = "";

    public UserDetailFragment(){
        super(R.layout.user_detail_layout);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView nameTv = view.findViewById(R.id.name);
        TextView emailTv = view.findViewById(R.id.email);
        EditText nameEdit = view.findViewById(R.id.name_edit);
        EditText emailEdit = view.findViewById(R.id.email_edit);
        Button editBtn = view.findViewById(R.id.btn_edit);
        View backBtn = view.findViewById(R.id.btn_back);

        RecyclerView classesRv = view.findViewById(R.id.classes_recycler);
        RecyclerView attendanceRv = view.findViewById(R.id.attendance_recycler);

        classesRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        attendanceRv.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Create adapter first, then set the remove listener to avoid referencing the variable during initialization
        ClassesAdapter classesAdapter = new ClassesAdapter(new ArrayList<>());
        classesAdapter.setOnRemoveListener((classId, membershipId) -> {
            // optimistic refresh handled inside remove flow
            if (currentUserId == -1) return;
            // Call remove; on success refresh list
            removeFromClass(currentUserId, classId, membershipId, () -> {
                classesAdapter.clear();
                loadUserClasses(currentUserId, classesAdapter);
            });
        });

        AttendanceAdapter attendanceAdapter = new AttendanceAdapter(new ArrayList<>());
        classesRv.setAdapter(classesAdapter);
        attendanceRv.setAdapter(attendanceAdapter);

        // Default UI
        nameTv.setText("");
        emailTv.setText("");
        nameEdit.setVisibility(View.GONE);
        emailEdit.setVisibility(View.GONE);

        // Back button
        backBtn.setOnClickListener(v -> requireActivity().onBackPressed());

        Bundle args = getArguments();
        if (args != null) {
            currentUserId = args.getInt("user_id", -1);
            if (args.containsKey("user_json")) {
                try {
                    JSONObject u = new JSONObject(args.getString("user_json"));
                    nameTv.setText(u.optString("name", ""));
                    // email may not be present in compact json
                    emailTv.setText(u.optString("email", ""));
                } catch (Exception ignored) {}
            }
        }

        if (currentUserId != -1) {
            loadUser(currentUserId, nameTv, emailTv);
            loadUserClasses(currentUserId, classesAdapter);
            loadUserAttendance(currentUserId, attendanceAdapter);
        } else {
            // Create mode: automatically enter edit mode
            nameEdit.setVisibility(View.VISIBLE);
            emailEdit.setVisibility(View.VISIBLE);
            nameTv.setVisibility(View.GONE);
            emailTv.setVisibility(View.GONE);
            editBtn.setText("Save");
            // Hide classes and attendance sections for new users
            view.findViewById(R.id.classes_recycler).setVisibility(View.GONE);
            view.findViewById(R.id.attendance_recycler).setVisibility(View.GONE);
        }

        // Toggle edit mode: when in edit mode, show edit fields and change button to Save/Cancel
        editBtn.setOnClickListener(v -> {
            boolean inEdit = nameEdit.getVisibility() == View.VISIBLE;
            if (!inEdit) {
                // Enter edit mode
                nameEdit.setVisibility(View.VISIBLE);
                emailEdit.setVisibility(View.VISIBLE);
                nameEdit.setText(nameTv.getText());
                emailEdit.setText(emailTv.getText());
                nameTv.setVisibility(View.GONE);
                emailTv.setVisibility(View.GONE);
                editBtn.setText("Save");
                classesAdapter.setEditable(true);
            } else {
                // Save changes
                String newName = nameEdit.getText().toString().trim();
                String newEmail = emailEdit.getText().toString().trim();
                if (TextUtils.isEmpty(newName) || TextUtils.isEmpty(newEmail)) {
                    Toast.makeText(requireContext(), "Name and email required", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (currentUserId == -1) {
                    // Create new user
                    performCreateUser(newName, newEmail, () -> {
                        Toast.makeText(requireContext(), "User created successfully", Toast.LENGTH_SHORT).show();
                        requireActivity().onBackPressed(); // Go back to users list
                    });
                } else {
                    // Update existing user
                    performSaveUser(currentUserId, newName, newEmail, () -> {
                        // Exit edit mode
                        nameTv.setText(newName);
                        emailTv.setText(newEmail);
                        nameEdit.setVisibility(View.GONE);
                        emailEdit.setVisibility(View.GONE);
                        nameTv.setVisibility(View.VISIBLE);
                        emailTv.setVisibility(View.VISIBLE);
                        editBtn.setText("Edit");
                        classesAdapter.setEditable(false);
                    });
                }
            }
        });
    }

    private void loadUser(int userId, TextView nameTv, TextView emailTv) {
        LoginManager lm = new LoginManager(requireContext());
        ApiService api = new ApiService(requireContext(), lm.getToken());
        api.get(ApiConfig.USERS + "/" + userId, response -> {
            JSONObject obj = response.optJSONObject("data");
            if (obj == null) obj = response.optJSONObject("user");
            if (obj == null) obj = response;
            final String fullName = obj.optString("full_name", obj.optString("name", ""));
            final String email = obj.optString("email", "");
            // Store additional user data for updates
            currentUserRoleId = obj.optInt("role_id", -1);
            currentUserStatus = obj.optString("status", "active");
            currentUserPhone = obj.optString("phone", "");
            requireActivity().runOnUiThread(() -> {
                nameTv.setText(fullName);
                emailTv.setText(email);
            });
        }, error -> Log.e(TAG, "failed load user", error));
    }

    private void loadUserClasses(int userId, ClassesAdapter adapter) {
        LoginManager lm = new LoginManager(requireContext());
        ApiService api = new ApiService(requireContext(), lm.getToken());

        // Many APIs expose classmembership via /classmembers?user_id= or /classmembers
        String url = ApiConfig.BASE_URL + "classmembers?user_id=" + userId;
        api.get(url, response -> {
            JSONArray arr = response.optJSONArray("data");
            if (arr == null) arr = response.optJSONArray("classmembers");
            if (arr == null) arr = response.optJSONArray("items");
            HashSet<Integer> seen = new HashSet<>();
            adapter.clear();
            if (arr != null) {
                for (int i=0;i<arr.length();i++){
                    JSONObject o = arr.optJSONObject(i);
                    if (o==null) continue;
                    // Some APIs return a membership object (with its own id) that contains the class object inside
                    JSONObject cls = o.optJSONObject("myclass");
                    if (cls == null) cls = o.optJSONObject("class");
                    if (cls == null) cls = o.optJSONObject("myclass_id") != null ? null : o; // fallback

                    int id = cls != null ? cls.optInt("id", -1) : -1;
                    if (id == -1) id = o.optInt("class_id", o.optInt("myclass_id", -1));
                    if (id == -1) continue;
                    if (seen.contains(id)) continue;
                    seen.add(id);

                    String className = cls != null ? cls.optString("class_name", cls.optString("name", "Unnamed")) : o.optString("class_name", "Unnamed");
                    // try to extract membership id (id of the classmember entry)
                    int membershipId = o.optInt("id", -1);
                    if (membershipId == -1) membershipId = o.optInt("member_id", -1);
                    if (membershipId == -1) membershipId = o.optInt("classmember_id", -1);
                    adapter.add(new ClassesAdapter.ClassItem(id, className, membershipId));
                }
            }
            requireActivity().runOnUiThread(adapter::notifyDataSetChanged);
        }, error -> Log.e(TAG, "failed load classmembers", error));
    }

    private void loadUserAttendance(int userId, AttendanceAdapter adapter) {
        LoginManager lm = new LoginManager(requireContext());
        ApiService api = new ApiService(requireContext(), lm.getToken());
        String url = ApiConfig.ATTENDANCE + "?user_id=" + userId;
        api.get(url, response -> {
            JSONArray arr = response.optJSONArray("data");
            if (arr == null) arr = response.optJSONArray("session_attendances");
            if (arr == null) arr = response.optJSONArray("items");
            adapter.clear();
            if (arr != null) {
                for (int i=0;i<arr.length();i++){
                    JSONObject o = arr.optJSONObject(i);
                    if (o==null) continue;
                    // try to extract session info
                    JSONObject session = o.optJSONObject("session");
                    if (session == null) session = o;
                    String subject = session.optString("subject", "");
                    String date = session.optString("date", o.optString("date", ""));
                    adapter.add(new AttendanceAdapter.AttendanceItem(subject + " — " + date));
                }
            }
            requireActivity().runOnUiThread(adapter::notifyDataSetChanged);
        }, error -> Log.e(TAG, "failed load attendance", error));
    }

    private void performSaveUser(int userId, String name, String email, Runnable onSuccess) {
        LoginManager lm = new LoginManager(requireContext());
        ApiService api = new ApiService(requireContext(), lm.getToken());
        JSONObject body = new JSONObject();
        try {
            body.put("full_name", name);
            body.put("email", email);
            // Include required fields as per API specification
            if (currentUserRoleId > 0) {
                body.put("role_id", currentUserRoleId);
            }
            body.put("status", currentUserStatus);
            if (!currentUserPhone.isEmpty()) {
                body.put("phone", currentUserPhone);
            }
        } catch (Exception ignored) {}
        android.util.Log.d(TAG, "performSaveUser: calling PUT for user=" + userId);
        String putUrl = ApiConfig.USERS + "/" + userId;
        android.util.Log.d(TAG, "performSaveUser: PUT url=" + putUrl + " body=" + body.toString());
        api.put(putUrl, body, response -> {
            requireActivity().runOnUiThread(onSuccess);
            Toast.makeText(requireContext(), "User updated", Toast.LENGTH_SHORT).show();
        }, error -> {
            Log.e(TAG, "failed update user", error);
            int status = -1;
            String respBody = null;
            if (error != null && error.networkResponse != null) {
                status = error.networkResponse.statusCode;
                try { respBody = new String(error.networkResponse.data, "UTF-8"); } catch (Exception ex) { respBody = null; }
            }
            // If the server returned a 2xx status but no body (e.g. 204 No Content), JsonObjectRequest may throw a ParseError.
            // Treat 2xx responses as success even if they produced a parse error.
            if (status >= 200 && status < 300) {
                android.util.Log.i(TAG, "PUT returned 2xx with empty body — treating as success, status=" + status);
                requireActivity().runOnUiThread(onSuccess);
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "User updated", Toast.LENGTH_SHORT).show());
                return;
            }
            Toast.makeText(requireContext(), "Update failed (code="+status+")", Toast.LENGTH_LONG).show();
            // Show detailed dialog so it's easy to inspect server response in the app
            final int finalStatus = status;
            final String finalRespBody = respBody;
            requireActivity().runOnUiThread(() -> {
                try {
                    String msg = "Status: " + finalStatus + "\n" + (finalRespBody != null ? finalRespBody : "(no body)");
                    new android.app.AlertDialog.Builder(requireContext())
                            .setTitle("Update failed")
                            .setMessage(msg)
                            .setPositiveButton("OK", (d, w) -> d.dismiss())
                            .show();
                } catch (Exception ignored) {}
            });
        });
    }

    private void performCreateUser(String name, String email, Runnable onSuccess) {
        LoginManager lm = new LoginManager(requireContext());
        ApiService api = new ApiService(requireContext(), lm.getToken());
        JSONObject body = new JSONObject();
        try {
            body.put("full_name", name);
            body.put("email", email);
            body.put("password", "123456"); // Default password for new users
            body.put("role_id", 3); // Default role (adjust as needed)
            body.put("status", "active");
        } catch (Exception ignored) {}
        android.util.Log.d(TAG, "performCreateUser: calling POST");
        android.util.Log.d(TAG, "performCreateUser: POST url=" + ApiConfig.USERS + " body=" + body.toString());
        api.post(ApiConfig.USERS, body, response -> {
            requireActivity().runOnUiThread(onSuccess);
        }, error -> {
            Log.e(TAG, "failed create user", error);
            int status = -1;
            String respBody = null;
            if (error != null && error.networkResponse != null) {
                status = error.networkResponse.statusCode;
                try { respBody = new String(error.networkResponse.data, "UTF-8"); } catch (Exception ex) { respBody = null; }
            }
            // If the server returned a 2xx status but no body (e.g. 201 Created with empty body), treat as success
            if (status >= 200 && status < 300) {
                android.util.Log.i(TAG, "POST returned 2xx with empty body — treating as success, status=" + status);
                requireActivity().runOnUiThread(onSuccess);
                return;
            }
            final int finalStatus = status;
            final String finalRespBody = respBody;
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Create failed (code=" + finalStatus + ")", Toast.LENGTH_LONG).show();
                try {
                    String msg = "Status: " + finalStatus + "\n" + (finalRespBody != null ? finalRespBody : "(no body)");
                    new android.app.AlertDialog.Builder(requireContext())
                            .setTitle("Create failed")
                            .setMessage(msg)
                            .setPositiveButton("OK", (d, w) -> d.dismiss())
                            .show();
                } catch (Exception ignored) {}
            });
        });
    }

    // Remove membership from a class. Prefer deleting by membershipId (/classmembers/{membershipId}) if available,
    // otherwise fallback to deleting by query parameters.
    private void removeFromClass(int userId, int classId, int membershipId, Runnable onDone) {
        LoginManager lm = new LoginManager(requireContext());
        ApiService api = new ApiService(requireContext(), lm.getToken());
        if (membershipId > 0) {
            String deleteUrl = ApiConfig.BASE_URL + "classmembers/" + membershipId;
            android.util.Log.d(TAG, "removeFromClass: deleting by membershipId url=" + deleteUrl);
            api.deleteString(deleteUrl, response -> requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Removed from class", Toast.LENGTH_SHORT).show();
                onDone.run();
            }), error -> {
                int s = -1;
                String body = null;
                if (error != null && error.networkResponse != null) {
                    s = error.networkResponse.statusCode;
                    try { body = new String(error.networkResponse.data, "UTF-8"); } catch (Exception ex) { body = null; }
                }
                // Treat 2xx responses as success even with empty body (e.g., 204 No Content)
                if (s >= 200 && s < 300) {
                    android.util.Log.i(TAG, "DELETE returned 2xx with empty body — treating as success, status=" + s);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Removed from class", Toast.LENGTH_SHORT).show();
                        onDone.run();
                    });
                    return;
                }
                Log.e(TAG, "delete by membershipId failed: " + s + " body=" + body, error);
                final int sFinal = s;
                final String bodyFinal = body;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Failed to remove from class (code="+sFinal+")", Toast.LENGTH_SHORT).show();
                    try {
                        String msg = "Status: " + sFinal + "\n" + (bodyFinal != null ? bodyFinal : "(no body)");
                        new android.app.AlertDialog.Builder(requireContext())
                                .setTitle("Remove failed")
                                .setMessage(msg)
                                .setPositiveButton("OK", (d, w) -> d.dismiss())
                                .show();
                    } catch (Exception ignored) {}
                });
            });
        } else {
            // fallback
            String deleteUrl = ApiConfig.BASE_URL + "classmembers?user_id=" + userId + "&class_id=" + classId;
            android.util.Log.d(TAG, "removeFromClass: deleting by query url=" + deleteUrl);
            api.deleteString(deleteUrl, response -> requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Removed from class", Toast.LENGTH_SHORT).show();
                onDone.run();
            }), error -> {
                int s = -1;
                String body = null;
                if (error != null && error.networkResponse != null) {
                    s = error.networkResponse.statusCode;
                    try { body = new String(error.networkResponse.data, "UTF-8"); } catch (Exception ex) { body = null; }
                }
                // Treat 2xx responses as success even with empty body (e.g., 204 No Content)
                if (s >= 200 && s < 300) {
                    android.util.Log.i(TAG, "DELETE returned 2xx with empty body — treating as success, status=" + s);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Removed from class", Toast.LENGTH_SHORT).show();
                        onDone.run();
                    });
                    return;
                }
                Log.e(TAG, "delete by query failed: " + s + " body=" + body, error);
                final int sFinal = s;
                final String bodyFinal = body;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Failed to remove from class (code="+sFinal+")", Toast.LENGTH_SHORT).show();
                    try {
                        String msg = "Status: " + sFinal + "\n" + (bodyFinal != null ? bodyFinal : "(no body)");
                        new android.app.AlertDialog.Builder(requireContext())
                                .setTitle("Remove failed")
                                .setMessage(msg)
                                .setPositiveButton("OK", (d, w) -> d.dismiss())
                                .show();
                    } catch (Exception ignored) {}
                });
            });
        }
    }

    // --- Classes adapter ---
    private static class ClassesAdapter extends RecyclerView.Adapter<ClassesAdapter.VH> {
        private final List<ClassItem> items;
        private OnRemoveListener onRemoveListener;
        private boolean editable = false;

        public ClassesAdapter(List<ClassItem> items) {
            this.items = items;
        }

        public void setOnRemoveListener(OnRemoveListener listener) {
            this.onRemoveListener = listener;
        }

        public void setEditable(boolean editable) {
            this.editable = editable;
            notifyDataSetChanged();
        }

        public void clear() {
            items.clear();
        }

        public void add(ClassItem item) {
            items.add(item);
            notifyItemInserted(items.size() - 1);
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_class, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ClassItem item = items.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ClassItem {
            public final int id;
            public final String className;
            public final int membershipId;

            public ClassItem(int id, String className, int membershipId) {
                this.id = id;
                this.className = className;
                this.membershipId = membershipId;
            }
        }

        class VH extends RecyclerView.ViewHolder {
            private final TextView classNameTv;
            private final TextView classSubtitleTv;
            private final View removeBtn;

            public VH(@NonNull View itemView) {
                super(itemView);
                classNameTv = itemView.findViewById(R.id.class_title);
                classSubtitleTv = itemView.findViewById(R.id.class_subtitle);
                removeBtn = itemView.findViewById(R.id.btn_remove);
            }

            public void bind(ClassItem item) {
                classNameTv.setText(item.className);
                classSubtitleTv.setText("ID: " + item.id);
                removeBtn.setVisibility(editable ? View.VISIBLE : View.GONE);
                removeBtn.setOnClickListener(v -> {
                    if (onRemoveListener != null) {
                        onRemoveListener.onRemove(item.id, item.membershipId);
                    }
                });
            }
        }

        public interface OnRemoveListener {
            void onRemove(int classId, int membershipId);
        }
    }

    // --- Attendance adapter (sibling of ClassesAdapter) ---
    private static class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.VH> {
        private final List<AttendanceItem> items;

        public AttendanceAdapter(List<AttendanceItem> items) {
            this.items = items;
        }

        public void clear() { items.clear(); }
        public void add(AttendanceItem item) { items.add(item); notifyItemInserted(items.size()-1); }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.text.setText(items.get(position).subjectDate);
        }

        @Override public int getItemCount() { return items.size(); }

        static class AttendanceItem {
            public final String subjectDate;
            public AttendanceItem(String s) {
                this.subjectDate = s;
            }
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView text;
            VH(@NonNull View v) { super(v); text = v.findViewById(android.R.id.text1); }
        }
    }
}
