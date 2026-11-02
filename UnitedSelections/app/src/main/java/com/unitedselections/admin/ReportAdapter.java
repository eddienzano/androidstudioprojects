package com.unitedselections.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.unitedselections.R;

import org.json.JSONObject;

import java.util.ArrayList;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    ArrayList<JSONObject> list;

    public ReportAdapter(ArrayList<JSONObject> list) {
        this.list = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtVariety, txtQty;

        public ViewHolder(View v) {
            super(v);
            txtVariety = v.findViewById(R.id.txtVariety);
            txtQty = v.findViewById(R.id.txtQty);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_variety, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            JSONObject obj = list.get(position);
            holder.txtVariety.setText(obj.getString("variety"));
            holder.txtQty.setText(obj.getString("qty"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}