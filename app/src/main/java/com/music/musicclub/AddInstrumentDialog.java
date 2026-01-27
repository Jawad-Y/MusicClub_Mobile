package com.music.musicclub;

import android.app.Dialog;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;

public class AddInstrumentDialog extends Dialog {

    public interface OnAddListener {
        void onAdd(String name);
    }

    public AddInstrumentDialog(@NonNull Context context, OnAddListener listener) {
        super(context);
        setContentView(R.layout.dialog_add_instrument);

        EditText etName = findViewById(R.id.etInstrumentName);
        Button btnAdd = findViewById(R.id.btnAdd);
        Button btnCancel = findViewById(R.id.btnCancel);

        btnAdd.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                listener.onAdd(name);
                dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }
}
