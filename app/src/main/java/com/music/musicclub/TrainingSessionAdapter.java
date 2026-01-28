package com.music.musicclub;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

 public class TrainingSessionAdapter extends RecyclerView.Adapter<TrainingSessionAdapter.VH> {

    private final List<TrainingSession> items = new ArrayList<>();
    private OnItemActionListener listener;

    // Highlight newly added session
    private int highlightPosition = -1;

    public interface OnItemActionListener {
        void onItemClicked(TrainingSession session);
    }

    public void setOnItemActionListener(OnItemActionListener l) {
        this.listener = l;
    }

    public void setItems(List<TrainingSession> sessions) {
        items.clear();
        if (sessions != null) items.addAll(sessions);
        notifyDataSetChanged();
    }

    public void highlightLastItem() {
        highlightPosition = items.size() - 1;
        notifyItemChanged(highlightPosition);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TrainingSession s = items.get(position);
        holder.subject.setText(s.getSubject());

        // Format date nicely
        String formattedDate = s.getDate();
        try {
            String rawDate = s.getDate();
            String dateOnly = rawDate.contains("T") ? rawDate.split("T")[0]
                    : rawDate.contains(" ") ? rawDate.split(" ")[0] : rawDate;

            SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d = apiFormat.parse(dateOnly);
            if (d != null) {
                SimpleDateFormat uiFormat = new SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault());
                formattedDate = uiFormat.format(d);
            }
        } catch (Exception ignored) {}

        String timeRange = "";
        if (s.getStartTime() != null && !s.getStartTime().isEmpty()
                && s.getEndTime() != null && !s.getEndTime().isEmpty()) {
            timeRange = s.getStartTime() + " - " + s.getEndTime();
        }

        holder.date.setText(formattedDate + (timeRange.isEmpty() ? "" : " • " + timeRange));
        holder.trainer.setText("");

        // Highlight new session
        if (position == highlightPosition) {
            holder.itemView.setBackgroundColor(Color.YELLOW);
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClicked(s);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView subject, date, trainer;

        VH(@NonNull View itemView) {
            super(itemView);
            subject = itemView.findViewById(R.id.session_subject);
            date = itemView.findViewById(R.id.session_date);
            trainer = itemView.findViewById(R.id.session_trainer);
        }
    }
}