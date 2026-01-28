package com.music.musicclub;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ClassDetailFragment extends Fragment {

    // Persistent UI fields
    private TextView title;
    private TextView instructor;
    private TextView dateTime;
    private TextView location;
    private TextView level;
    private TextView capacity;
    private TextView equipment;
    private View trainingTabContent;
    private NestedScrollView infoTabContent;
    private View membersTabContent;
    private int classId = -1;

    // Info stats
    private TextView nbSessionsCountView;
    private TextView nbMembersCountView;

    // Adapters (backed by lists). Keep as instance fields so loader methods can update them.
    private TrainingSessionAdapter trainingAdapter;
    private MembersAdapter membersAdapter;

    public ClassDetailFragment() {
        super(R.layout.class_detail_layout);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.class_detail_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            classId = getArguments().getInt("class_id", -1);
        }

        // Bind persistent views
        title = view.findViewById(R.id.title);
        instructor = view.findViewById(R.id.instructor);
        dateTime = view.findViewById(R.id.date_time);
        location = view.findViewById(R.id.location);
        level = view.findViewById(R.id.level);
        capacity = view.findViewById(R.id.capacity);
        equipment = view.findViewById(R.id.equipment);

        // make tabLayout local since only used here
        TabLayout localTabLayout = view.findViewById(R.id.tab_layout);
        trainingTabContent = view.findViewById(R.id.training_tab_content);
        infoTabContent = view.findViewById(R.id.info_tab_content);
        membersTabContent = view.findViewById(R.id.members_tab_content);

        // Ephemeral views as locals
        RecyclerView trainingSessionsList = view.findViewById(R.id.training_sessions_list);
        RecyclerView membersList = view.findViewById(R.id.members_list);
        TextView addMemberButton = view.findViewById(R.id.add_member_button);
        TextView nbSessionsCount = view.findViewById(R.id.nb_sessions_count);
        TextView nbMembersCount = view.findViewById(R.id.nb_members_count);
        nbSessionsCountView = nbSessionsCount; nbMembersCountView = nbMembersCount;
        View addTrainingButton = view.findViewById(R.id.add_training_button);
        // Setup adapters
        trainingAdapter = new TrainingSessionAdapter(new ArrayList<>());
        membersAdapter = new MembersAdapter(new ArrayList<>(), null);

        trainingSessionsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        trainingSessionsList.setAdapter(trainingAdapter);

        membersList.setLayoutManager(new LinearLayoutManager(requireContext()));
        membersList.setAdapter(membersAdapter);

        // Place addMemberButton inside members tab if needed
        try {
            ViewGroup oldParent = (ViewGroup) addMemberButton.getParent();
            if (oldParent != null) oldParent.removeView(addMemberButton);
            if (membersTabContent instanceof ViewGroup) {
                ViewGroup membersContainer = (ViewGroup) membersTabContent;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(48));
                lp.setMargins(0, dpToPx(16), 0, 0);
                membersContainer.addView(addMemberButton, lp);
            }
        } catch (Exception ignored) {}

        // Add member button visible only on members tab
        addMemberButton.setVisibility(View.GONE);
        addMemberButton.setEnabled(classId != -1);
        addMemberButton.setOnClickListener(v -> {
            if (classId == -1) {
                Toast.makeText(requireContext(), "Cannot add member to unsaved class", Toast.LENGTH_SHORT).show();
                return;
            }
            // build exclude set of already assigned member ids
            List<Integer> excludeIds = new ArrayList<>();
            for (Member m : membersAdapter.items) excludeIds.add(m.id);
            // show searchable picker (filters out already assigned members)
            MemberPickerDialog.show(requireContext(), excludeIds, this::performAddMember);
        });

        // Back handling using dispatcher
        View btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        // Edit class button (open edit dialog when classId exists)
        View btnEditClass = view.findViewById(R.id.btn_edit_class);
        btnEditClass.setOnClickListener(v -> {
            if (classId == -1) {
                // create new class via dialog
                CreateClassDialog.show(requireContext(), (name, leaderId) -> {
                    LoginManager lm = new LoginManager(requireContext());
                    ApiService api = new ApiService(requireContext(), lm.getToken());
                    org.json.JSONObject body = new org.json.JSONObject();
                    try { body.put("class_name", name); if (leaderId > 0) body.put("class_leader_id", leaderId); }
                    catch (Exception ignored) {}
                    api.post(ApiConfig.MY_CLASSES, body, resp -> {
                        int id = resp.optInt("id", -1);
                        if (id == -1) { JSONObject d = resp.optJSONObject("data"); if (d != null) id = d.optInt("id", -1); }
                        if (id != -1) {
                            classId = id; loadClassDetails(classId); loadMembers(classId); loadTrainingSessions(classId);
                        }
                    }, err -> Toast.makeText(requireContext(), "Failed to create class", Toast.LENGTH_SHORT).show());
                });
            } else {
                // edit existing class
                // prefill with current values
                String currentName = title.getText().toString();
                int currentLeaderId = -1; // unknown; we can try to fetch from loaded class details if stored — keep -1
                CreateClassDialog.showEdit(requireContext(), currentName, currentLeaderId, (name, leaderId) -> {
                    LoginManager lm = new LoginManager(requireContext());
                    ApiService api = new ApiService(requireContext(), lm.getToken());
                    org.json.JSONObject body = new org.json.JSONObject();
                    try { body.put("class_name", name); if (leaderId > 0) body.put("class_leader_id", leaderId); }
                    catch (Exception ignored) {}
                    api.put(ApiConfig.MY_CLASSES + "/" + classId, body, resp -> {
                        Toast.makeText(requireContext(), "Class updated", Toast.LENGTH_SHORT).show();
                        loadClassDetails(classId);
                    }, err -> Toast.makeText(requireContext(), "Failed to update class", Toast.LENGTH_SHORT).show());
                });
            }
        });

        // Tab handling
        localTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { switchToTab(tab.getPosition(), addMemberButton, addTrainingButton); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        if (localTabLayout.getTabCount() > 0) {
            TabLayout.Tab t = localTabLayout.getTabAt(0);
            if (t != null) t.select();
        }

        if (classId != -1) {
            loadClassDetails(classId);
            loadTrainingSessions(classId);
            loadMembers(classId);
        } else {
            title.setText("New Class");
        }

        // Add training button handler
        addTrainingButton.setOnClickListener(v -> {
            if (classId == -1) {
                Toast.makeText(requireContext(), "Save class before adding sessions", Toast.LENGTH_SHORT).show();
                return;
            }
            TrainingSessionDialog.show(requireContext(), classId, () -> loadTrainingSessions(classId));
        });

        // OnBackPressed safety callback to navigate up
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                try {
                    androidx.navigation.fragment.NavHostFragment.findNavController(ClassDetailFragment.this).navigateUp();
                } catch (Exception e) {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void switchToTab(int position, View addMemberButton, View addTrainingButton) {
        trainingTabContent.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        infoTabContent.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        membersTabContent.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
        addMemberButton.setVisibility(position == 2 && classId != -1 ? View.VISIBLE : View.GONE);
        addTrainingButton.setVisibility(position == 0 && classId != -1 ? View.VISIBLE : View.GONE);
    }

    private void loadClassDetails(int id) {
        LoginManager lm = new LoginManager(requireContext());
        ApiService api = new ApiService(requireContext(), lm.getToken());

        api.get(ApiConfig.MY_CLASSES + "/" + id, response -> {
            // robust parsing: class may be wrapped
            JSONObject obj = response.optJSONObject("data");
            if (obj == null) obj = response.optJSONObject("myclass");
            if (obj == null) obj = response.optJSONObject("class");
            if (obj == null) obj = response;

            String className = obj.optString("class_name", obj.optString("name", "Unnamed Class"));
            title.setText(className);

            // Leader
            JSONObject leaderObj = obj.optJSONObject("class_leader");
            if (leaderObj == null) leaderObj = obj.optJSONObject("leader");
            String leaderName = "-";
            if (leaderObj != null) leaderName = leaderObj.optString("full_name", leaderObj.optString("name", "-"));
            instructor.setText("Leader: " + leaderName);

            // description -> equipment field reuse if present
            String desc = obj.optString("description", obj.optString("details", ""));
            if (!TextUtils.isEmpty(desc)) {
                equipment.setText(desc);
            }

            // capacity / level
            String cap = obj.has("capacity") ? String.valueOf(obj.optInt("capacity", 0)) : obj.optString("capacity", "");
            if (!TextUtils.isEmpty(cap)) capacity.setText("Capacity: " + cap);
            String lvl = obj.optString("level", "");
            if (!TextUtils.isEmpty(lvl)) level.setText("Level: " + lvl);

            // optionally populate date/time/location from a next_session field
            JSONObject nextSession = obj.optJSONObject("next_session");
            if (nextSession != null) {
                String date = nextSession.optString("date", "");
                String start = nextSession.optString("start_time", "");
                String end = nextSession.optString("end_time", "");
                String dt = "";
                if (!TextUtils.isEmpty(date)) dt += date;
                if (!TextUtils.isEmpty(start) && !TextUtils.isEmpty(end)) dt += " • " + start + " - " + end;
                if (!dt.isEmpty()) dateTime.setText(dt);
                String loc = nextSession.optString("location", "");
                if (!TextUtils.isEmpty(loc)) location.setText(loc);
            }

        }, error -> Toast.makeText(requireContext(), "Failed to load class details", Toast.LENGTH_SHORT).show());
    }

    private void loadTrainingSessions(int id) {
        LoginManager lm = new LoginManager(requireContext());
        ApiService api = new ApiService(requireContext(), lm.getToken());

        String url = ApiConfig.TRAINING_SESSIONS + "?class_id=" + id;
        api.get(url, response -> {
            JSONArray arr = response.optJSONArray("data");
            if (arr == null) arr = response.optJSONArray("items");
            if (arr == null) arr = response.optJSONArray("training_sessions");
            if (arr == null) arr = response.optJSONArray("sessions");

            trainingAdapter.clear();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    int sid = o.optInt("id", -1);
                    String subject = o.optString("subject", o.optString("name", "Session"));
                    String date = o.optString("date", "");
                    String start = o.optString("start_time", "");
                    String end = o.optString("end_time", "");
                    String loc = o.optString("location", "");
                    trainingAdapter.add(new TrainingSession(sid, subject, date, start, end, loc));
                }
            }
            trainingAdapter.notifyDataSetChanged();
            // update sessions count in info tab
            if (nbSessionsCountView != null) nbSessionsCountView.setText(String.valueOf(trainingAdapter.items.size()));
        }, error -> Toast.makeText(requireContext(), "Failed to load training sessions", Toast.LENGTH_SHORT).show());
    }

    private void loadMembers(int id) {
        LoginManager lm = new LoginManager(requireContext());
        ApiService api = new ApiService(requireContext(), lm.getToken());

        String url = ApiConfig.BASE_URL + "classmembers?class_id=" + id;
        api.get(url, response -> {
            JSONArray arr = response.optJSONArray("data");
            if (arr == null) arr = response.optJSONArray("classmembers");
            if (arr == null) arr = response.optJSONArray("items");

            // dedupe
            HashSet<Integer> seen = new HashSet<>();
            membersAdapter.clear();

            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;

                    // Ensure this classmember belongs to the requested class (some endpoints may return all members)
                    int memberClassId = o.optInt("class_id", -1);
                    if (memberClassId == -1) memberClassId = o.optInt("myclass_id", -1);
                    if (memberClassId == -1) {
                        String s = o.optString("class_id", ""); if (!s.isEmpty()) { try { memberClassId = Integer.parseInt(s); } catch (NumberFormatException ignored) {} }
                    }
                    if (memberClassId == -1) {
                        JSONObject nested = o.optJSONObject("class"); if (nested != null) memberClassId = nested.optInt("id", -1);
                    }
                    if (memberClassId != -1 && memberClassId != id) continue; // not for this class

                    JSONObject user = o.optJSONObject("user");
                    if (user == null) user = o.optJSONObject("member");
                    if (user == null) user = o;

                    int uid = user.optInt("id", -1);
                    if (uid == -1) {
                        String sid = user.optString("id", "");
                        if (!sid.isEmpty()) {
                            try { uid = Integer.parseInt(sid); } catch (NumberFormatException ignored) {}
                        }
                    }
                    if (uid == -1 || seen.contains(uid)) continue;
                    seen.add(uid);

                    String name = user.optString("full_name", user.optString("name", "Unnamed"));
                    String email = user.optString("email", "");
                    membersAdapter.add(new Member(uid, name, email));
                }
            }

            membersAdapter.notifyDataSetChanged();
            if (nbMembersCountView != null) nbMembersCountView.setText(String.valueOf(membersAdapter.items.size()));
        }, error -> Toast.makeText(requireContext(), "Failed to load members", Toast.LENGTH_SHORT).show());
    }

    private void performAddMember(int userId, int roleId, String roleName) {
        // role is mandatory per backend: refuse to add if missing
        if (roleId <= 0 && (roleName == null || roleName.isEmpty())) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Role required")
                    .setMessage("The selected user does not have a role assigned. Role is required to add a member to a class. Please assign a role to the user or choose another user.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // If roleId is missing but roleName provided, attempt to resolve roleId from API /roles
        if (roleId <= 0 && roleName != null && !roleName.isEmpty()) {
            LoginManager lm = new LoginManager(requireContext());
            ApiService api = new ApiService(requireContext(), lm.getToken());
            String rolesUrl = ApiConfig.BASE_URL + "roles";
            api.get(rolesUrl, resp -> {
                JSONArray arr = resp.optJSONArray("data");
                if (arr == null) arr = resp.optJSONArray("roles");
                if (arr == null) arr = resp.optJSONArray("items");
                int resolved = -1;
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.optJSONObject(i);
                        if (o == null) continue;
                        int rid = o.optInt("id", -1);
                        String rname = o.optString("role_name", o.optString("name", ""));
                        if (rname != null && rname.equalsIgnoreCase(roleName)) { resolved = rid; break; }
                    }
                }
                // call post with resolved id (may be -1)
                postAddMember(userId, resolved, roleName);
            }, err -> {
                // roles endpoint not available or failed -> fallback to posting with roleName
                postAddMember(userId, -1, roleName);
            });
        } else {
            postAddMember(userId, roleId, roleName);
        }
    }

    private void postAddMember(int userId, int roleId, String roleName) {
        LoginManager lm = new LoginManager(requireContext());
        ApiService api = new ApiService(requireContext(), lm.getToken());

        org.json.JSONObject body = new org.json.JSONObject();
        try {
            body.put("class_id", classId);
            body.put("myclass_id", classId);
            body.put("user_id", userId);
            body.put("member_id", userId);
            if (roleId > 0) body.put("role_id", roleId);
            if (roleName != null && !roleName.isEmpty()) body.put("role", roleName);
        } catch (Exception ignored) {}

        api.post(ApiConfig.BASE_URL + "classmembers", body, response -> {
            Toast.makeText(requireContext(), "Member added", Toast.LENGTH_SHORT).show();
            loadMembers(classId);
        }, error -> {
            String msg = "Failed to add member";
            try {
                if (error.networkResponse != null && error.networkResponse.data != null) {
                    String bodyStr = new String(error.networkResponse.data);
                    msg += ": " + bodyStr;
                } else if (error.getMessage() != null) {
                    msg += ": " + error.getMessage();
                }
            } catch (Exception ex) { }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
        });
    }

    // --- Models and adapters ---
    private static class TrainingSession {
        int id; String subject; String date; String start; String end; String location;
        TrainingSession(int id, String subject, String date, String start, String end, String location) { this.id = id; this.subject = subject; this.date = date; this.start = start; this.end = end; this.location = location; }
    }

    private static class TrainingSessionAdapter extends RecyclerView.Adapter<TrainingSessionAdapter.VH> {
        final ArrayList<TrainingSession> items;
        TrainingSessionAdapter(ArrayList<TrainingSession> items) { this.items = items; }
        void clear() { items.clear(); }
        void add(TrainingSession s) { items.add(s); }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_training_session_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            TrainingSession s = items.get(position);
            holder.subject.setText(s.subject);
            String meta = s.date;
            if (!TextUtils.isEmpty(s.start) && !TextUtils.isEmpty(s.end)) meta += " • " + s.start + " - " + s.end;
            if (!TextUtils.isEmpty(s.location)) meta += "  •  " + s.location;
            holder.meta.setText(meta);
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView subject, meta;
            VH(@NonNull View itemView) { super(itemView); subject = itemView.findViewById(R.id.session_subject); meta = itemView.findViewById(R.id.session_meta); }
        }
    }

    public static class Member { int id; String name; String email; int roleId; String roleName; Member(int id, String name, String email){this(id,name,email,-1,null);} Member(int id, String name, String email, int roleId, String roleName){this.id=id;this.name=name;this.email=email;this.roleId=roleId;this.roleName=roleName;} }

    private static class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.VH> {
        final List<Member> items;
        private final OnItemClickListener listener;

        public interface OnItemClickListener { void onItemClick(Member m); }

        MembersAdapter(List<Member> items, OnItemClickListener listener){this.items=items;this.listener=listener;}
        void clear(){items.clear();}
        void add(Member m){items.add(m);}

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Member m = items.get(position);
            holder.title.setText(m.name);
            holder.subtitle.setText(m.email != null ? m.email : "");
            holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onItemClick(m); });
        }

        @Override
        public int getItemCount(){return items.size();}

        static class VH extends RecyclerView.ViewHolder { TextView title, subtitle; VH(@NonNull View itemView){ super(itemView); title = itemView.findViewById(android.R.id.text1); subtitle = itemView.findViewById(android.R.id.text2); } }
    }

    // --- Member picker dialog (searchable) ---
    public static class MemberPickerDialog {
        // userId, roleId (may be -1), roleName (may be null)
        public interface Callback { void onSelect(int userId, int roleId, String roleName); }

        // excludeIds: list of user IDs to hide from the picker (already members)
        public static void show(@NonNull Context ctx, @NonNull List<Integer> excludeIds, @NonNull Callback cb) {
            LinearLayout container = new LinearLayout(ctx);
            container.setOrientation(LinearLayout.VERTICAL);
            int pad = (int)(12 * ctx.getResources().getDisplayMetrics().density);
            container.setPadding(pad, pad, pad, pad);

            EditText search = new EditText(ctx);
            search.setHint("Search users by name or email");
            container.addView(search, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            RecyclerView rv = new RecyclerView(ctx);
            rv.setLayoutManager(new LinearLayoutManager(ctx));
            container.addView(rv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            AlertDialog dialog = new AlertDialog.Builder(ctx).setTitle("Select User").setView(container).create();

            List<Member> allUsers = new ArrayList<>();
            MembersAdapter adapter = new MembersAdapter(allUsers, m -> {
                // If user has a role id or role name, forward it. Otherwise prompt user to choose role name.
                if (m.roleId > 0 || (m.roleName != null && !m.roleName.isEmpty())) {
                    cb.onSelect(m.id, m.roleId, m.roleName);
                    dialog.dismiss();
                } else {
                    // prompt to choose Trainer or Trainee (role name) when role missing
                    new AlertDialog.Builder(ctx)
                            .setTitle("Select role")
                            .setItems(new CharSequence[]{"Trainer","Trainee"}, (d, which) -> {
                                String chosen = which == 0 ? "Trainer" : "Trainee";
                                cb.onSelect(m.id, -1, chosen);
                                dialog.dismiss();
                            }).show();
                }
            });
            rv.setAdapter(adapter);

            // Load users from API
            LoginManager lm = new LoginManager(ctx);
            ApiService api = new ApiService(ctx, lm.getToken());
            api.get(ApiConfig.USERS, response -> {
                JSONArray arr = response.optJSONArray("data");
                if (arr == null) arr = response.optJSONArray("users");
                if (arr == null) arr = response.optJSONArray("items");

                allUsers.clear();
                if (arr != null) {
                    for (int i=0;i<arr.length();i++){
                        JSONObject o = arr.optJSONObject(i);
                        if (o==null) continue;
                        int uid = o.optInt("id", -1);
                        if (uid==-1) continue;
                        if (excludeIds.contains(uid)) continue; // skip already-assigned
                        String name = o.optString("full_name", o.optString("name","Unnamed"));
                        String email = o.optString("email","");
                        int roleId = o.optInt("role_id", -1);
                        JSONObject roleObj = o.optJSONObject("role");
                        String roleName = null;
                        if (roleObj != null) {
                            roleId = roleObj.optInt("id", roleId);
                            roleName = roleObj.optString("role_name", roleObj.optString("name", null));
                        }
                        allUsers.add(new Member(uid,name,email,roleId,roleName));
                    }
                }
                // refresh adapter
                adapter.notifyDataSetChanged();

                // Note: adapter's click listener handles selection

            }, error -> Toast.makeText(ctx, "Failed to load users", Toast.LENGTH_SHORT).show());

            // Search filter (basic client-side)
            search.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {
                    String q = s.toString().toLowerCase().trim();
                    List<Member> filtered = new ArrayList<>();
                    for (Member m : allUsers) {
                        if (m.name.toLowerCase().contains(q) || (m.email!=null && m.email.toLowerCase().contains(q))) filtered.add(m);
                    }
                    adapter.items.clear();
                    adapter.items.addAll(filtered);
                    adapter.notifyDataSetChanged();
                }
            });

            dialog.show();
        }
    }

    // --- Training session creation dialog ---
    public static class TrainingSessionDialog {
        public interface Callback { void onCreated(); }

        public static void show(@NonNull Context ctx, int classId, @NonNull Callback cb) {
            LinearLayout container = new LinearLayout(ctx);
            container.setOrientation(LinearLayout.VERTICAL);
            int pad = (int)(12 * ctx.getResources().getDisplayMetrics().density);
            container.setPadding(pad, pad, pad, pad);

            EditText subject = new EditText(ctx); subject.setHint("Subject"); container.addView(subject);
            EditText date = new EditText(ctx); date.setHint("Date (YYYY-MM-DD)"); container.addView(date);
            EditText start = new EditText(ctx); start.setHint("Start time (HH:MM)"); container.addView(start);
            EditText end = new EditText(ctx); end.setHint("End time (HH:MM)"); container.addView(end);
            EditText loc = new EditText(ctx); loc.setHint("Location (optional)"); container.addView(loc);

            AlertDialog dialog = new AlertDialog.Builder(ctx).setTitle("Add Training Session").setView(container)
                    .setPositiveButton("Add", (d, w) -> {
                        String sub = subject.getText().toString().trim();
                        String dt = date.getText().toString().trim();
                        String st = start.getText().toString().trim();
                        String en = end.getText().toString().trim();
                        String lc = loc.getText().toString().trim();
                        if (sub.isEmpty() || dt.isEmpty() || st.isEmpty() || en.isEmpty()) {
                            Toast.makeText(ctx, "Please fill subject, date, start and end", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        org.json.JSONObject body = new org.json.JSONObject();
                        try { body.put("class_id", classId); body.put("subject", sub); body.put("date", dt); body.put("start_time", st); body.put("end_time", en); if (!lc.isEmpty()) body.put("location", lc); }
                        catch (Exception ignored) {}
                        LoginManager lm = new LoginManager(ctx);
                        ApiService api = new ApiService(ctx, lm.getToken());
                        api.post(ApiConfig.TRAINING_SESSIONS, body, resp -> cb.onCreated(), err -> Toast.makeText(ctx, "Failed to create session", Toast.LENGTH_SHORT).show());
                    }).setNegativeButton("Cancel", null).create();
            dialog.show();
        }
    }

    // --- Utility ---
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, requireContext().getResources().getDisplayMetrics());
    }
}
