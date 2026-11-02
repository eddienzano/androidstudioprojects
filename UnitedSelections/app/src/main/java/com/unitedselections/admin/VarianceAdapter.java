package com.unitedselections.admin;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.unitedselections.R;

import java.util.List;

public class VarianceAdapter extends RecyclerView.Adapter<VarianceAdapter.VH> {

    List<VarianceItem> list;
    Context context;

    public VarianceAdapter(Context context, List<VarianceItem> list){
        this.context = context;
        this.list = list;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_variance, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH h, int pos) {

        VarianceItem item = list.get(pos);

        h.txtQR.setText("QR: " + item.qr);
        h.txtHarvest.setText("Harvest → " + item.harvestVariety + " (" + item.harvestQty + ")");
        h.txtIntake.setText("Intake → " + item.intakeVariety + " (" + item.intakeQty + ")");
        h.txtStatus.setText(item.status);

        // =========================================
        // 🎨 STATUS COLOR + CARD BORDER LOGIC
        // =========================================

        int borderColor;

        switch (item.status) {

            case "OK":
                h.txtStatus.setTextColor(0xFF4CAF50); // green
                borderColor = 0xFF4CAF50;
                break;

            case "VARIETY MISMATCH":
            case "QTY MISMATCH":
                h.txtStatus.setTextColor(0xFFF44336); // red
                borderColor = 0xFFF44336;
                break;

            case "NOT VERIFIED":
            default:
                h.txtStatus.setTextColor(0xFF9E9E9E); // grey
                borderColor = 0xFF9E9E9E;
                break;
        }

        // 🔥 APPLY BORDER TO CARD
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(20);
        drawable.setStroke(6, borderColor);
        drawable.setColor(0xFFFFFFFF); // white background

        h.container.setBackground(drawable);

        // =========================================
        // 🔘 BUTTON VISIBILITY LOGIC
        // =========================================

        if (isResolvable(item.status)) {
            h.btnResolve.setVisibility(View.VISIBLE);
        } else {
            h.btnResolve.setVisibility(View.GONE);
        }

        // =========================================
        // 🚀 CLICK HANDLER
        // =========================================

        h.btnResolve.setOnClickListener(v -> {

            Intent intent = new Intent(context, VarianceEditActivity.class);

            intent.putExtra("id", item.id);
            intent.putExtra("qr", item.qr);
            intent.putExtra("field_qty", item.harvestQty);
            intent.putExtra("intake_qty", item.intakeQty);
            intent.putExtra("field_variety", item.harvestVariety);
            intent.putExtra("intake_variety", item.intakeVariety);

            context.startActivity(intent);
        });
    }

    private boolean isResolvable(String status) {
        return status.equals("VARIETY MISMATCH")
                || status.equals("QTY MISMATCH");
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView txtQR, txtHarvest, txtIntake, txtStatus;
        Button btnResolve;
        LinearLayout container;

        VH(View v){
            super(v);

            txtQR = v.findViewById(R.id.txtQR);
            txtHarvest = v.findViewById(R.id.txtHarvest);
            txtIntake = v.findViewById(R.id.txtIntake);
            txtStatus = v.findViewById(R.id.txtStatus);

            btnResolve = v.findViewById(R.id.btnResolve);

            // 🔥 THIS IS YOUR CARD ROOT LAYOUT
            container = v.findViewById(R.id.cardContainer);
        }
    }
}