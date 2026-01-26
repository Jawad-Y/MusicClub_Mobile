package com.music.musicclub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.VH> {

    private final List<User> items = new ArrayList<>();
    private OnItemActionListener listener;

    public interface OnItemActionListener {
        void onItemClicked(User user);
        void onEditClicked(User user);
        void onDeleteClicked(User user);
    }

    public void setOnItemActionListener(OnItemActionListener l) {
        this.listener = l;
    }

    public void setItems(List<User> users) {
        items.clear();
        if (users != null) items.addAll(users);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        User u = items.get(position);
        holder.name.setText(u.getName());
        holder.role.setText(u.getRole());
        holder.status.setText(u.getStatus());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClicked(u);
        });

        holder.edit.setVisibility(View.VISIBLE);
        holder.edit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClicked(u);
        });

        holder.delete.setVisibility(View.VISIBLE);
        holder.delete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClicked(u);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, role, status;
        View edit, delete;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.user_name);
            role = itemView.findViewById(R.id.user_role);
            status = itemView.findViewById(R.id.user_status);
            edit = itemView.findViewById(R.id.btnEditUser);
            delete = itemView.findViewById(R.id.btnDeleteUser);
        }
    }
}
