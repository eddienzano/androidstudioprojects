package com.yourapp.summers;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yourapp.R;

import org.json.JSONObject;

import java.util.List;

public class EditVarietyAdapter
        extends RecyclerView.Adapter<EditVarietyAdapter.Holder> {

    private final Context context;
    private final List<JSONObject> list;
    private final List<String> varieties;

    public EditVarietyAdapter(
            Context context,
            List<JSONObject> list,
            List<String> varieties) {

        this.context = context;
        this.list = list;
        this.varieties = varieties;
    }

    static class Holder extends RecyclerView.ViewHolder {

        TextView txtCurrentVariety;
        TextView txtQty;
        AutoCompleteTextView txtNewVariety;

        TextWatcher watcher;

        Holder(View v) {
            super(v);

            txtCurrentVariety = v.findViewById(R.id.txtCurrentVariety);
            txtQty = v.findViewById(R.id.txtQty);
            txtNewVariety = v.findViewById(R.id.txtNewVariety);
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_variety_edit, parent, false);

        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder holder,
            int position) {

        JSONObject obj = list.get(position);

        try {

            String currentVariety = obj.optString("variety_name");
            int qty = obj.optInt("quantity");

            holder.txtCurrentVariety.setText("Current: " + currentVariety);
            holder.txtQty.setText("Qty: " + qty);

            // =========================
            // AUTO COMPLETE SETUP
            // =========================
            ArrayAdapter<String> adapter =
                    new ArrayAdapter<>(
                            context,
                            android.R.layout.simple_dropdown_item_1line,
                            varieties
                    );

            holder.txtNewVariety.setAdapter(adapter);
            holder.txtNewVariety.setThreshold(1);

            // =========================
            // REMOVE OLD WATCHER (IMPORTANT)
            // =========================
            if (holder.watcher != null) {
                holder.txtNewVariety.removeTextChangedListener(holder.watcher);
            }

            // =========================
            // SET VALUE WITHOUT TRIGGERING LISTENER
            // =========================
            holder.txtNewVariety.setText(
                    obj.optString("new_variety", currentVariety),
                    false
            );

            // =========================
            // CLEAR OLD VALUE ON CLICK
            // =========================
            holder.txtNewVariety.setOnClickListener(v -> {
                holder.txtNewVariety.setSelection(holder.txtNewVariety.getText().length());
                holder.txtNewVariety.showDropDown();
            });

            // =========================
            // SHOW DROPDOWN ON FOCUS
            // =========================
            holder.txtNewVariety.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    holder.txtNewVariety.showDropDown();
                }
            });

            // =========================
            // SINGLE CLEAN WATCHER
            // =========================
            TextWatcher watcher = new TextWatcher() {

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        obj.put("new_variety", s.toString());
                    } catch (Exception e) {
                        Log.e("VARIETY_ADAPTER", "JSON update failed");
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            };

            holder.txtNewVariety.addTextChangedListener(watcher);
            holder.watcher = watcher;

        } catch (Exception e) {

            Log.e("VARIETY_ADAPTER",
                    "Bind error: " + e.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}