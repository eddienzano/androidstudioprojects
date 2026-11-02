package com.hillary.supplier;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hillary.R;

import java.util.ArrayList;

public class SupplierOrderItemsAdapter
        extends RecyclerView.Adapter<SupplierOrderItemsAdapter.ViewHolder> {

    Context context;
    ArrayList<SupplierOrderItemModel> itemList;

    public SupplierOrderItemsAdapter(Context context, ArrayList<SupplierOrderItemModel> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_order_product_simple, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        SupplierOrderItemModel model = itemList.get(position);

        holder.tvName.setText(model.getProductName());
        holder.tvPrice.setText("KES " + model.getPrice());
        holder.tvQty.setText("Qty: " + model.getQty());
        holder.tvSubtotal.setText("Subtotal: KES " + model.getSubtotal());
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvPrice, tvQty, tvSubtotal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvQty = itemView.findViewById(R.id.tvQty);
            tvSubtotal = itemView.findViewById(R.id.tvSubtotal);
        }
    }
}