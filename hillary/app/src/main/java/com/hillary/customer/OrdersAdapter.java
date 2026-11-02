package com.hillary.customer;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.hillary.R;

import java.util.ArrayList;

public class OrdersAdapter
        extends RecyclerView.Adapter<OrdersAdapter.ViewHolder> {

    Context context;

    ArrayList<OrderModel> orderList;

    public OrdersAdapter(
            Context context,
            ArrayList<OrderModel> orderList
    ) {

        this.context = context;
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_order, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        OrderModel model = orderList.get(position);

        holder.tvOrderNumber.setText(
                model.getOrderNumber()
        );

        holder.tvAmount.setText(
                "KES " + model.getTotalAmount()
        );

        holder.tvStatus.setText(
                model.getStatus()
        );

        holder.tvDate.setText(
                model.getCreatedAt()
        );

        holder.cardOrder.setOnClickListener(v -> {

            Intent intent = new Intent(
                    context,
                    OrderDetailsActivity.class
            );

            intent.putExtra(
                    "order_id",
                    model.getId()
            );

            context.startActivity(intent);

        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvOrderNumber,
                tvAmount,
                tvStatus,
                tvDate;

        CardView cardOrder;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            cardOrder =
                    itemView.findViewById(R.id.cardOrder);

            tvOrderNumber =
                    itemView.findViewById(R.id.tvOrderNumber);

            tvAmount =
                    itemView.findViewById(R.id.tvAmount);

            tvStatus =
                    itemView.findViewById(R.id.tvStatus);

            tvDate =
                    itemView.findViewById(R.id.tvDate);
        }
    }
}