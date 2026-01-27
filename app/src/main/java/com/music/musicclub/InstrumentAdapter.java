package com.music.musicclub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class InstrumentAdapter extends RecyclerView.Adapter<InstrumentAdapter.ViewHolder> {

    private final ArrayList<Instrument> list;
    private final OnInstrumentActionListener listener;

    public InstrumentAdapter(ArrayList<Instrument> list, OnInstrumentActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_instrument, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Instrument instrument = list.get(position);

        holder.tvName.setText(instrument.getName());

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(instrument));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(instrument));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvName;
        View btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvInstrumentName);
            btnEdit = itemView.findViewById(R.id.btnEditInstrument);
            btnDelete = itemView.findViewById(R.id.btnDeleteInstrument);
        }
    }

    // Interface
    public interface OnInstrumentActionListener {
        void onEdit(Instrument instrument);
        void onDelete(Instrument instrument);
    }
}
