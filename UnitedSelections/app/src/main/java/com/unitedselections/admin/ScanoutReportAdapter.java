package com.unitedselections.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.unitedselections.R;

import org.jspecify.annotations.NonNull;

import java.util.List;

public class ScanoutReportAdapter extends RecyclerView.Adapter<ScanoutReportAdapter.ViewHolder> {

    List<ScanoutGroup> list;

    public ScanoutReportAdapter(List<ScanoutGroup> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scanout_group, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {

        ScanoutGroup g = list.get(position);

        h.tvDestination.setText(g.destination);
        h.tvTarget.setText("Target: " + g.targetName);
        h.tvDates.setText("Harvest: " + g.harvestDate + " | Out: " + g.outDate);

        StringBuilder itemsText = new StringBuilder();

        for (ScanoutItem i : g.items) {
            itemsText.append("• ")
                    .append(i.variety)
                    .append(" - ")
                    .append(i.qty)
                    .append("\n");
        }

        h.tvItems.setText(itemsText.toString());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvDestination, tvTarget, tvDates, tvItems;

        public ViewHolder(@NonNull View v) {
            super(v);

            tvDestination = v.findViewById(R.id.tvDestination);
            tvTarget = v.findViewById(R.id.tvTarget);
            tvDates = v.findViewById(R.id.tvDates);
            tvItems = v.findViewById(R.id.tvItems);
        }
    }
}
