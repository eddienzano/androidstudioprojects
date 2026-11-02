package com.aaagrowers.coldroom.summers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.aaagrowers.coldroom.R;

import java.util.List;

public class SummersScanAdapter
        extends RecyclerView.Adapter<SummersScanAdapter.Holder>{

    private final List<SummersScanItem> list;

    public SummersScanAdapter(List<SummersScanItem> list){
        this.list = list;
    }

    static class Holder extends RecyclerView.ViewHolder{

        TextView qr,status;

        Holder(View v){
            super(v);

            qr = v.findViewById(R.id.txtQr);
            status = v.findViewById(R.id.txtStatus);
        }
    }

    @Override
    public Holder onCreateViewHolder(
            ViewGroup parent,
            int viewType
    ){

        return new Holder(
                LayoutInflater.from(parent.getContext())
                        .inflate(
                                R.layout.item_summers_scan,
                                parent,
                                false
                        )
        );
    }

    @Override
    public void onBindViewHolder(
            Holder h,
            int position
    ){

        SummersScanItem item = list.get(position);

        h.qr.setText(item.time + " | " + item.qr);
        h.status.setText(item.status);
    }

    @Override
    public int getItemCount(){
        return list.size();
    }
}