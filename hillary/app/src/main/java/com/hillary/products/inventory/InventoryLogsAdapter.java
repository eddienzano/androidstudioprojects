package com.hillary.products.inventory;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hillary.R;

import java.util.ArrayList;
import java.util.List;

public class InventoryLogsAdapter
        extends RecyclerView.Adapter<InventoryLogsAdapter.MyHolder> {

    Context context;

    List<InventoryLog> list;
    List<InventoryLog> originalList;

    String BASE_URL;

    public InventoryLogsAdapter(Context context,
                                List<InventoryLog> list,
                                String BASE_URL) {

        this.context = context;
        this.list = list;
        this.originalList = new ArrayList<>(list);
        this.BASE_URL = BASE_URL;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                       int viewType) {

        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_inventory_log,
                        parent,
                        false);

        return new MyHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder h,
                                 int position) {

        InventoryLog log = list.get(position);

        h.txtProduct.setText(log.getProductName());

        h.txtQuantity.setText(
                "Quantity: " + log.getQuantity()
        );

        h.txtStockChange.setText(
                "Stock: "
                        + log.getPreviousStock()
                        + " → "
                        + log.getNewStock()
        );

        h.txtNotes.setText(
                "Notes: " + log.getNotes()
        );

        h.txtDate.setText(log.getCreatedAt());

        String type = log.getTransactionType();

        h.txtType.setText(type);

        if(type.equals("STOCK_IN")){

            h.txtType.setBackgroundColor(
                    Color.parseColor("#4CAF50")
            );

            h.txtType.setTextColor(Color.WHITE);

        }else{

            h.txtType.setBackgroundColor(
                    Color.parseColor("#F44336")
            );

            h.txtType.setTextColor(Color.WHITE);
        }

        Glide.with(context)
                .load(BASE_URL + log.getThumbnail())
                .into(h.imgProduct);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void filter(String text, String type){

        list.clear();

        for(InventoryLog item : originalList){

            boolean matchesSearch =
                    item.getProductName()
                            .toLowerCase()
                            .contains(text.toLowerCase());

            boolean matchesType =
                    type.equals("ALL")
                            || item.getTransactionType()
                            .equals(type);

            if(matchesSearch && matchesType){

                list.add(item);
            }
        }

        notifyDataSetChanged();
    }

    public static class MyHolder
            extends RecyclerView.ViewHolder {

        ImageView imgProduct;

        TextView txtProduct,
                txtType,
                txtQuantity,
                txtStockChange,
                txtNotes,
                txtDate;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            imgProduct =
                    itemView.findViewById(R.id.imgProduct);

            txtProduct =
                    itemView.findViewById(R.id.txtProduct);

            txtType =
                    itemView.findViewById(R.id.txtType);

            txtQuantity =
                    itemView.findViewById(R.id.txtQuantity);

            txtStockChange =
                    itemView.findViewById(R.id.txtStockChange);

            txtNotes =
                    itemView.findViewById(R.id.txtNotes);

            txtDate =
                    itemView.findViewById(R.id.txtDate);
        }
    }
}