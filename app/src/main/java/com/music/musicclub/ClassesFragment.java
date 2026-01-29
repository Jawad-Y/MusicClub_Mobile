package com.music.musicclub;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.chip.Chip;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ClassesFragment extends Fragment {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAddClass;
    private ClassesAdapter adapter;
    private ArrayList<MyClass> classes;

    private int filterDepartmentId = -1;

    public ClassesFragment() { super(); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.classes_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            filterDepartmentId = getArguments().getInt("department_id", -1);
        }

        recyclerView = view.findViewById(R.id.recyclerViewClasses);
        fabAddClass = view.findViewById(R.id.fabAddClass);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        classes = new ArrayList<>();
        adapter = new ClassesAdapter(classes);
        recyclerView.setAdapter(adapter);

        fabAddClass.setOnClickListener(v -> {
            CreateClassDialog.show(requireContext(), (className, leaderId, departmentId) -> {
                LoginManager lm = new LoginManager(requireContext());
                ApiService api = new ApiService(requireContext(), lm.getToken());
                JSONObject body = new JSONObject();
                try { 
                    body.put("class_name", className); 
                    body.put("department_id", departmentId);
                    if (leaderId > 0) body.put("class_leader_id", leaderId); 
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Failed to prepare request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                api.post(ApiConfig.MY_CLASSES, body, 
                    resp -> {
                        Toast.makeText(requireContext(), "Class created successfully", Toast.LENGTH_SHORT).show();
                        loadClasses();
                    }, 
                    error -> {
                        String msg = "Failed to create class";
                        try {
                            if (error.networkResponse != null && error.networkResponse.data != null) {
                                msg += ": " + new String(error.networkResponse.data);
                            } else if (error.getMessage() != null) {
                                msg += ": " + error.getMessage();
                            }
                        } catch (Exception ex) {}
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                    });
            });
        });

        loadClasses();
    }

    private void loadClasses() {
        LoginManager loginManager = new LoginManager(requireContext());
        ApiService api = new ApiService(requireContext(), loginManager.getToken());

        api.get(ApiConfig.MY_CLASSES, response -> {
            classes.clear();

            // Try several possible keys for the array to be robust
            JSONArray arr = response.optJSONArray("data");
            if (arr == null) arr = response.optJSONArray("myclasses");
            if (arr == null) arr = response.optJSONArray("classes");
            if (arr == null) arr = response.optJSONArray("items");

            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.optJSONObject(i);
                    if (obj == null) continue;

                    int id = obj.optInt("id", -1);
                    if (id == -1) id = obj.optInt("myclass_id", -1);
                    if (id == -1) id = obj.optInt("my_class_id", -1);
                    if (id == -1) {
                        String sid = obj.optString("id", "");
                        if (sid.isEmpty()) sid = obj.optString("class_id", "");
                        if (!sid.isEmpty()) {
                            try { id = Integer.parseInt(sid); } catch (NumberFormatException ignored) {}
                        }
                    }
                    if (id == -1) {
                        JSONObject classObj = obj.optJSONObject("class");
                        if (classObj != null) id = classObj.optInt("id", -1);
                    }
                    if (id == -1) continue; // skip malformed class

                    String className = obj.optString("class_name", obj.optString("name", "Unnamed Class"));

                    // Department id and name: try multiple keys
                    int deptId = obj.optInt("department_id", -1);
                    String departmentName = "Unknown";
                    JSONObject deptObj = obj.optJSONObject("department");
                    if (deptObj != null) {
                        departmentName = deptObj.optString("department_name", deptObj.optString("name", departmentName));
                        if (deptObj.has("id")) deptId = deptObj.optInt("id", deptId);
                    } else {
                        departmentName = obj.optString("department_name", departmentName);
                    }

                    // Leader name
                    String leaderName = "No Leader";
                    JSONObject leaderObj = obj.optJSONObject("class_leader");
                    if (leaderObj == null) leaderObj = obj.optJSONObject("leader");
                    if (leaderObj != null) {
                        leaderName = leaderObj.optString("full_name", leaderObj.optString("name", leaderName));
                    } else {
                        leaderName = obj.optString("leader_name", leaderName);
                    }

                    // Members count: try members_count or members array
                    int membersCount = -1;
                    if (obj.has("members_count")) {
                        membersCount = obj.optInt("members_count", -1);
                    } else {
                        JSONArray membersArr = obj.optJSONArray("members");
                        if (membersArr != null) membersCount = membersArr.length();
                    }

                    MyClass c = new MyClass(id, className, deptId, departmentName, leaderName, membersCount);

                    // apply optional department filter
                    if (filterDepartmentId == -1 || filterDepartmentId == deptId) {
                        classes.add(c);
                    }
                }
            }

            adapter.notifyDataSetChanged();
            api.get(ApiConfig.BASE_URL + "classmembers", membersResp -> {
                Map<Integer, Integer> counts = new HashMap<>();

                JSONArray mArr = membersResp.optJSONArray("data");
                if (mArr == null) mArr = membersResp.optJSONArray("classmembers");
                if (mArr == null) mArr = membersResp.optJSONArray("items");

                if (mArr != null) {
                    for (int j = 0; j < mArr.length(); j++) {
                        JSONObject mo = mArr.optJSONObject(j);
                        if (mo == null) continue;

                        int classId = mo.optInt("class_id", mo.optInt("myclass_id", mo.optInt("classId", -1)));
                        if (classId == -1) {
                            // try string forms safely
                            String s = mo.optString("class_id", "");
                            if (s.isEmpty()) s = mo.optString("myclass_id", "");
                            if (!s.isEmpty()) {
                                try { classId = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
                            }
                        }
                        if (classId == -1) {
                            JSONObject nested = mo.optJSONObject("class");
                            if (nested != null) classId = nested.optInt("id", -1);
                        }
                        if (classId == -1) {
                            JSONObject nested2 = mo.optJSONObject("myclass");
                            if (nested2 != null) classId = nested2.optInt("id", -1);
                        }
                        if (classId == -1) continue;

                        if (counts.containsKey(classId)) {
                            counts.put(classId, counts.get(classId) + 1);
                        } else {
                            counts.put(classId, 1);
                        }
                    }
                } else {
                    // fallback: try parse mapping classId->count
                    java.util.Iterator<String> it = membersResp.keys();
                    while (it.hasNext()) {
                        String key = it.next();
                        try {
                            int cid = Integer.parseInt(key);
                            int val = membersResp.optInt(key, -1);
                            if (val >= 0) counts.put(cid, val);
                        } catch (NumberFormatException ignored) {
                            // ignore
                        }
                    }
                }

                // merge counts into classes (override provisional values)
                for (MyClass cc : classes) {
                    Integer cnt = counts.get(cc.id);
                    if (cnt != null) cc.membersCount = cnt;
                    else if (cc.membersCount < 0) cc.membersCount = 0;
                }

                adapter.notifyDataSetChanged();
            }, err -> {
                // fallback to provisional counts
                adapter.notifyDataSetChanged();
            });

        }, error -> Toast.makeText(requireContext(), "Failed to load classes", Toast.LENGTH_SHORT).show());
    }

    // Inner simple model to avoid adding new files
    private static class MyClass {
        int id;
        int departmentId;
        String name;
        String departmentName;
        String leaderName;
        int membersCount;

        MyClass(int id, String name, int departmentId, String departmentName, String leaderName, int membersCount) {
            this.id = id;
            this.departmentId = departmentId;
            this.name = name;
            this.departmentName = departmentName;
            this.leaderName = leaderName;
            this.membersCount = membersCount;
        }
    }

    // Adapter implemented as inner class
    private class ClassesAdapter extends RecyclerView.Adapter<ClassesAdapter.VH> {

        private final ArrayList<MyClass> items;

        ClassesAdapter(ArrayList<MyClass> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_class, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            MyClass c = items.get(position);

            holder.title.setText(c.name);
            holder.chipDepartment.setText(c.departmentName);

            holder.chipDepartment.setEllipsize(TextUtils.TruncateAt.END);
            holder.chipDepartment.setMaxLines(1);
            int screenWidth = holder.itemView.getResources().getDisplayMetrics().widthPixels;
            int reserved = dpToPx(140);
            int avail = screenWidth - dpToPx(48) - reserved;
            if (avail < dpToPx(80)) avail = dpToPx(120);
            holder.chipDepartment.setMaxWidth(avail);

            if (c.membersCount >= 0) {
                holder.members.setText("Members: " + c.membersCount);
            } else {
                holder.members.setText("Members: -");
            }

            holder.leader.setText("Leader: " + c.leaderName);

            // Item click -> open detail
            holder.itemView.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putInt("class_id", c.id);
                NavHostFragment.findNavController(ClassesFragment.this)
                        .navigate(R.id.classDetailFragment, bundle);
            });

            // Edit (navigate to detail for editing)
            holder.btnEdit.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putInt("class_id", c.id);
                NavHostFragment.findNavController(ClassesFragment.this)
                        .navigate(R.id.classDetailFragment, bundle);
            });

            // Delete
            holder.btnDelete.setOnClickListener(v ->
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Delete Class")
                            .setMessage("Are you sure you want to delete this class?")
                            .setPositiveButton("Yes", (dialog, which) -> deleteClass(c, position))
                            .setNegativeButton("No", null)
                            .show()
            );
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView title, members, leader;
            Chip chipDepartment;
            ImageButton btnEdit, btnDelete;

            VH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.textClassTitle);
                members = itemView.findViewById(R.id.textMembers);
                leader = itemView.findViewById(R.id.textLeader);
                chipDepartment = itemView.findViewById(R.id.chipDepartment);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }

    private void deleteClass(MyClass c, int position) {
        LoginManager loginManager = new LoginManager(requireContext());
        ApiService api = new ApiService(requireContext(), loginManager.getToken());

        api.deleteString(ApiConfig.MY_CLASSES + "/" + c.id, response -> {
            classes.remove(position);
            adapter.notifyItemRemoved(position);
            Toast.makeText(requireContext(), "Class deleted", Toast.LENGTH_SHORT).show();
        }, error -> Toast.makeText(requireContext(), "Failed to delete class", Toast.LENGTH_SHORT).show());
    }
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
