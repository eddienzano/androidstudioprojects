package com.hillary.customer;

import android.content.Context;
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

public class OrderItemsAdapter
        extends RecyclerView.Adapter<OrderItemsAdapter.ViewHolder> {

    Context context;

    ArrayList<OrderItemModel> itemList;

    public OrderItemsAdapter(
            Context context,
            ArrayList<OrderItemModel> itemList
    ) {

        this.context = context;
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(context)
                .inflate(
                        R.layout.item_order_product,
                        parent,
                        false
                );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        OrderItemModel model = itemList.get(position);

        holder.tvName.setText(
                model.getProductName()
        );

        holder.tvPrice.setText(
                "KES " + model.getPrice()
        );

        holder.tvQty.setText(
                "Qty: " + model.getQty()
        );

        holder.tvSubtotal.setText(
                "Subtotal: KES " + model.getSubtotal()
        );

        Glide.with(context)
                .load(model.getThumbnail())
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imgProduct);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        ImageView imgProduct;

        TextView tvName,
                tvPrice,
                tvQty,
                tvSubtotal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imgProduct =
                    itemView.findViewById(R.id.imgProduct);

            tvName =
                    itemView.findViewById(R.id.tvName);

            tvPrice =
                    itemView.findViewById(R.id.tvPrice);

            tvQty =
                    itemView.findViewById(R.id.tvQty);

            tvSubtotal =
                    itemView.findViewById(R.id.tvSubtotal);
        }
    }
}