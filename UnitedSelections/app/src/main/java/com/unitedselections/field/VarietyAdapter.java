package com.unitedselections.field;

import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.List;

public class VarietyAdapter extends RecyclerView.Adapter<VarietyAdapter.ViewHolder> {

    List<JSONObject> list;

    public VarietyAdapter(List<JSONObject> list) {
        this.list = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text;

        public ViewHolder(View v) {
            super(v);
            text = v.findViewById(android.R.id.text1);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        JSONObject obj = list.get(position);

        String name = obj.optString("variety_name");
        String qty = obj.optString("quantity");

        holder.text.setText(name + "  (" + qty + ")");
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}