package com.hillary.customer;

import android.content.Context;
import android.content.Intent;
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

public class ProductBrowserAdapter extends RecyclerView.Adapter<ProductBrowserAdapter.ViewHolder> {

    Context context;
    ArrayList<ProductModel> productList;

    public ProductBrowserAdapter(Context context, ArrayList<ProductModel> productList) {
        this.context = context;
        this.productList = productList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_marketplace_product, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        ProductModel model = productList.get(position);

        holder.tvName.setText(model.getProduct_name());

        holder.tvSupplier.setText(model.getSupplier_name());

        holder.tvPrice.setText("KES " + model.getPrice());

        if (model.getStock() > 0) {

            holder.tvStock.setText("In Stock");

        } else {

            holder.tvStock.setText("Out Of Stock");
        }

        Glide.with(context)
                .load(model.getThumbnail())
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(holder.imgProduct);

        holder.itemView.setOnClickListener(v -> {

            Intent intent = new Intent(
                    context,
                    ProductDetailsActivity.class
            );

            intent.putExtra("product_id", model.getId());

            context.startActivity(intent);

        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgProduct;

        TextView tvName, tvSupplier, tvPrice, tvStock;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imgProduct = itemView.findViewById(R.id.imgProduct);

            tvName = itemView.findViewById(R.id.tvName);

            tvSupplier = itemView.findViewById(R.id.tvSupplier);

            tvPrice = itemView.findViewById(R.id.tvPrice);

            tvStock = itemView.findViewById(R.id.tvStock);
        }
    }
}