package com.music.musicclub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private RecyclerView rvDashboard;
    private DashboardAdapter adapter;
    private List<DashboardItem> dashboardItems;

    private ApiService apiService;
    private String token;

    private int totalUsers = 0;
    private int totalInstruments = 0;
    private List<Session> sessionsList;

    public DashboardFragment() {
        super(R.layout.dashboard_layout);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        rvDashboard = view.findViewById(R.id.rvDashboard);

        token = new LoginManager(requireContext()).getToken();
        apiService = new ApiService(requireContext(), token);

        dashboardItems = new ArrayList<>();
        sessionsList = new ArrayList<>();
        adapter = new DashboardAdapter(dashboardItems);
        rvDashboard.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvDashboard.setAdapter(adapter);

        loadData();

        return view;
    }

    private void loadData() {
        loadTotalUsers();
        loadTotalInstruments();
        loadSessions();
    }

    private void loadTotalUsers() {
        apiService.get(ApiConfig.USERS, response -> {
            requireActivity().runOnUiThread(() -> {
                try {
                    JSONArray users = response.optJSONArray("data");
                    if (users == null) users = response.optJSONArray("users");
                    if (users != null) {
                        totalUsers = users.length();
                        updateDashboard();
                    } else {
                        totalUsers = 0;
                        updateDashboard();
                    }
                } catch (Exception e) {
                    totalUsers = 0;
                    updateDashboard();
                }
            });
        }, error -> {
            requireActivity().runOnUiThread(() -> {
                totalUsers = 0;
                updateDashboard();
                Toast.makeText(requireContext(), "Failed to load users", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void loadTotalInstruments() {
        apiService.get(ApiConfig.INSTRUMENTS, response -> {
            requireActivity().runOnUiThread(() -> {
                try {
                    JSONArray instruments = response.optJSONArray("data");
                    if (instruments == null) instruments = response.optJSONArray("instruments");
                    if (instruments != null) {
                        totalInstruments = instruments.length();
                        updateDashboard();
                    } else {
                        totalInstruments = 0;
                        updateDashboard();
                    }
                } catch (Exception e) {
                    totalInstruments = 0;
                    updateDashboard();
                }
            });
        }, error -> {
            requireActivity().runOnUiThread(() -> {
                totalInstruments = 0;
                updateDashboard();
                Toast.makeText(requireContext(), "Failed to load instruments", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void loadSessions() {
        apiService.get(ApiConfig.TRAINING_SESSIONS, response -> {
            requireActivity().runOnUiThread(() -> {
                try {
                    JSONArray sessions = response.optJSONArray("data");
                    if (sessions == null) sessions = response.optJSONArray("sessions");
                    if (sessions != null) {
                        sessionsList.clear();
                        for (int i = 0; i < sessions.length(); i++) {
                            JSONObject obj = sessions.optJSONObject(i);
                            if (obj != null) {
                                // Backend returns "subject" not "title"
                                String subject = obj.optString("subject", "Session");
                                String date = obj.optString("date", "");
                                String location = obj.optString("location", "");
                                Session session = new Session(subject, date, location);
                                sessionsList.add(session);
                            }
                        }
                        updateDashboard();
                    }
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Failed to load sessions", Toast.LENGTH_SHORT).show();
                }
            });
        }, error -> {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Failed to load sessions", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void updateDashboard() {
        dashboardItems.clear();
        dashboardItems.add(new DashboardItem(DashboardItem.Type.METRIC_USERS, "Total Users", String.valueOf(totalUsers), null));
        dashboardItems.add(new DashboardItem(DashboardItem.Type.METRIC_INSTRUMENTS, "Total Instruments", String.valueOf(totalInstruments), null));
        for (Session session : sessionsList) {
            dashboardItems.add(new DashboardItem(DashboardItem.Type.SESSION, "", "", session));
        }
        adapter.notifyDataSetChanged();
    }

    // DashboardItem class
    public static class DashboardItem {
        public enum Type { METRIC_USERS, METRIC_INSTRUMENTS, SESSION }
        public Type type;
        public String title;
        public String value;
        public Session session;

        public DashboardItem(Type type, String title, String value, Session session) {
            this.type = type;
            this.title = title;
            this.value = value;
            this.session = session;
        }
    }

    // DashboardAdapter
    public static class DashboardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<DashboardItem> items;

        public DashboardAdapter(List<DashboardItem> items) {
            this.items = items;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).type.ordinal();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == DashboardItem.Type.SESSION.ordinal()) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session_card, parent, false);
                return new SessionViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_metric, parent, false);
                return new MetricViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            DashboardItem item = items.get(position);
            if (holder instanceof SessionViewHolder) {
                SessionViewHolder vh = (SessionViewHolder) holder;
                Session session = item.session;
                vh.tvTitle.setText(session.title);
                vh.tvDate.setText(session.date);
                vh.tvLocation.setText(session.location);
            } else if (holder instanceof MetricViewHolder) {
                MetricViewHolder vh = (MetricViewHolder) holder;
                vh.tvTitle.setText(item.title);
                vh.tvValue.setText(item.value);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class MetricViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvValue;

            public MetricViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvMetricTitle);
                tvValue = itemView.findViewById(R.id.tvMetricValue);
            }
        }

        public static class SessionViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDate, tvLocation;

            public SessionViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvSessionTitle);
                tvDate = itemView.findViewById(R.id.tvSessionDate);
                tvLocation = itemView.findViewById(R.id.tvSessionLocation);
            }
        }
    }

    // Session class
    public static class Session {
        public String title;
        public String date;
        public String location;

        public Session(String title, String date, String location) {
            this.title = title;
            this.date = date;
            this.location = location;
        }
    }
}