package com.unitedselections.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.unitedselections.R;

import org.jspecify.annotations.NonNull;

import java.util.List;

public class IssuanceAdapter extends RecyclerView.Adapter<IssuanceAdapter.VH> {

    private final List<IssuanceItem> list;

    public IssuanceAdapter(List<IssuanceItem> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_issuance, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        IssuanceItem item = list.get(pos);

        // ===== QR =====
        h.txtQR.setText("QR: " + item.qr);

        // ===== 🔥 DETAILS (NEW) =====
        String variety = item.variety != null ? item.variety : "-";
        String greenhouse = item.greenhouse != null ? item.greenhouse : "-";

        h.txtDetails.setText(
                variety + "\n" +
                        greenhouse + "\nQty: " + item.qty
        );

        // ===== TIMES =====
        String inTime = item.scanIn != null ? item.scanIn : "-";
        String intakeTime = item.intake != null ? item.intake : "-";
        String outTime = item.scanOut != null ? item.scanOut : "-";

        h.txtTimes.setText(
                "IN: " + inTime +
                        "\nINTAKE: " + intakeTime +
                        "\nOUT: " + outTime
        );

        // ===== DURATION =====
        h.txtDuration.setText("Duration: " + item.duration + " mins");

        // ===== STATUS =====
        h.txtStatus.setText(item.status);

        // ===== COLOR LOGIC =====
        int color;
        switch (item.status) {
            case "OK":
                color = h.itemView.getResources().getColor(R.color.green_500);
                break;
            case "NOT VERIFIED":
                color = h.itemView.getResources().getColor(R.color.orange_500);
                break;
            case "NOT RELEASED":
                color = h.itemView.getResources().getColor(R.color.grey_500);
                break;
            case "DELAYED":
                color = h.itemView.getResources().getColor(R.color.red_500);
                break;
            default:
                color = h.itemView.getResources().getColor(R.color.black);
        }

        h.txtStatus.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView txtQR, txtDetails, txtTimes, txtDuration, txtStatus;

        VH(@NonNull View v) {
            super(v);

            txtQR = v.findViewById(R.id.txtQR);
            txtDetails = v.findViewById(R.id.txtDetails); // 🔥 NEW
            txtTimes = v.findViewById(R.id.txtTimes);
            txtDuration = v.findViewById(R.id.txtDuration);
            txtStatus = v.findViewById(R.id.txtStatus);
        }
    }
}