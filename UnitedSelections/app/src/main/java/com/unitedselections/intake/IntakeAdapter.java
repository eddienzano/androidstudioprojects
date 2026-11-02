package com.unitedselections.intake;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.unitedselections.R;

import org.json.JSONObject;

import java.util.List;

public class IntakeAdapter extends RecyclerView.Adapter<IntakeAdapter.ViewHolder> {

    private final List<JSONObject> list;

    public IntakeAdapter(List<JSONObject> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_intake_variety, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        JSONObject obj = list.get(position);

        String name = obj.optString("variety");
        int original = obj.optInt("original_qty");
        int verified = obj.optInt("verified_qty");

        holder.variety.setText(name);
        holder.original.setText("Original: " + original);

        // 🔥 Remove old watcher
        if (holder.watcher != null) {
            holder.qty.removeTextChangedListener(holder.watcher);
        }

        holder.qty.setText(String.valueOf(verified));

        // 🔥 Add watcher safely
        holder.watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void afterTextChanged(Editable s) {

                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                try {
                    int val = s.toString().isEmpty() ? 0 : Integer.parseInt(s.toString());
                    list.get(pos).put("verified_qty", val);
                } catch (Exception ignored) {}
            }
        };

        holder.qty.addTextChangedListener(holder.watcher);

        // 🔥 Delete row
        holder.delete.setOnClickListener(v -> {

            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                list.remove(pos);
                notifyItemRemoved(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView variety, original;
        EditText qty;
        Button delete;
        TextWatcher watcher;

        ViewHolder(View itemView) {
            super(itemView);

            variety = itemView.findViewById(R.id.varietyName);
            original = itemView.findViewById(R.id.originalQty);
            qty = itemView.findViewById(R.id.verifiedQty);
            delete = itemView.findViewById(R.id.deleteBtn);
        }
    }
}