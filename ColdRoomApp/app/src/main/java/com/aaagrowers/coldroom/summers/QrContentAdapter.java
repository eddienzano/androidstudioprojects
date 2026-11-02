package com.aaagrowers.coldroom.summers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.aaagrowers.coldroom.R;

import java.util.ArrayList;

public class QrContentAdapter extends RecyclerView.Adapter<QrContentAdapter.VH> {

    private final ArrayList<QrContentItem> list;

    public QrContentAdapter(ArrayList<QrContentItem> list) {
        this.list = list;
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView tvVariety, tvQty;

        VH(View v) {
            super(v);
            tvVariety = v.findViewById(R.id.tvVariety);
            tvQty = v.findViewById(R.id.tvQty);
        }
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_qr_content, parent, false);

        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH h, int pos) {

        QrContentItem item = list.get(pos);

        h.tvVariety.setText(item.varietyName);
        h.tvQty.setText(String.valueOf(item.quantity));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}