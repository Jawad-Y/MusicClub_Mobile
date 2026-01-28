package com.music.musicclub;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class DepartmentAdapter extends RecyclerView.Adapter<DepartmentAdapter.ViewHolder> {

    private Fragment fragment;
    private Context context;
    private ArrayList<Department> departments;
    private LoginManager loginManager;

    public DepartmentAdapter(Fragment fragment, ArrayList<Department> departments) {
        this.fragment = fragment;
        this.context = fragment.requireContext();
        this.departments = departments;
        this.loginManager = new LoginManager(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_department, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Department dept = departments.get(position);

        holder.tvName.setText(dept.getDepartmentName());
        holder.tvLeader.setText("Leader: " + dept.getLeaderName());

        // EDIT
        holder.btnEdit.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("department_id", dept.getId());

            NavHostFragment.findNavController(fragment)
                    .navigate(R.id.action_department_to_addEditDepartment, bundle);

        });

        // DELETE
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Department")
                    .setMessage("Are you sure you want to delete this department?")
                    .setPositiveButton("Yes", (dialog, which) ->
                            deleteDepartment(dept, position))
                    .setNegativeButton("No", null)
                    .show();
        });

        // VIEW
        holder.btnView.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("department_id", dept.getId());

            NavHostFragment.findNavController(fragment)
                    .navigate(R.id.classesFragment, bundle);
        });
    }

    @Override
    public int getItemCount() {
        return departments.size();
    }

    private void deleteDepartment(Department dept, int position) {
        ApiService api = new ApiService(context, loginManager.getToken());

        api.deleteString(ApiConfig.DEPARTMENTS + "/" + dept.getId(),
                response -> {
                    departments.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(context, "Department deleted", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    Toast.makeText(context, "Failed to delete department", Toast.LENGTH_SHORT).show();
                });
    }


    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvLeader;
        MaterialButton btnView, btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvDepartmentName);
            tvLeader = itemView.findViewById(R.id.tvDepartmentLeader);
            btnView = itemView.findViewById(R.id.btnViewClasses);
            btnEdit = itemView.findViewById(R.id.btnEditDepartment);
            btnDelete = itemView.findViewById(R.id.btnDeleteDepartment);
        }
    }
}
