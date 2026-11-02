package com.hillary.products;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hillary.R;
import com.hillary.network.Constants;

import java.util.ArrayList;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    Context context;
    ArrayList<Product> productList;

    OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public ProductAdapter(
            Context context,
            ArrayList<Product> productList,
            OnProductClickListener listener
    ) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_product, parent, false);

        return new ProductViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {

        Product product = productList.get(position);

        holder.tvName.setText(product.getName());

        holder.tvDescription.setText(product.getDescription());

        holder.tvPrice.setText("KES " + product.getPrice());

        holder.tvStock.setText("Stock: " + product.getStock());

        String imagePath = product.getImage();

        if (imagePath != null && !imagePath.isEmpty()) {

            String fullUrl =
                    "https://www.aaagrowers.co.ke/inventory/fillers/"
                            + imagePath;

            Glide.with(context)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(holder.imgProduct);

        } else {

            holder.imgProduct.setImageResource(
                    R.drawable.ic_launcher_background
            );
        }

        holder.cardProduct.setOnClickListener(v -> {

            Intent intent = new Intent(context, ProductDetailsActivity.class);

            intent.putExtra("id", product.getId());
            intent.putExtra("name", product.getName());
            intent.putExtra("description", product.getDescription());
            intent.putExtra("price", product.getPrice());
            intent.putExtra("stock", product.getStock());
            intent.putExtra("image", product.getImage());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvDescription, tvPrice, tvStock;
        CardView cardProduct;
        ImageView imgProduct;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);

            cardProduct = itemView.findViewById(R.id.cardProduct);

            tvName = itemView.findViewById(R.id.tvName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStock = itemView.findViewById(R.id.tvStock);

            imgProduct = itemView.findViewById(R.id.imgProduct);
        }
    }




}