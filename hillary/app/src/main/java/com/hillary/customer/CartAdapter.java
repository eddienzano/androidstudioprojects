package com.hillary.customer;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.hillary.R;
import com.hillary.network.ApiClient;
import com.hillary.network.Constants;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CartAdapter
        extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    Context context;

    ArrayList<CartModel> cartList;

    public CartAdapter(
            Context context,
            ArrayList<CartModel> cartList
    ) {

        this.context = context;
        this.cartList = cartList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_cart, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        CartModel model = cartList.get(position);

        holder.tvName.setText(model.getProductName());

        holder.tvPrice.setText(
                "KES " + model.getPrice()
        );

        holder.tvQty.setText(
                String.valueOf(model.getQty())
        );

        Glide.with(context)
                .load(model.getThumbnail())
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imgProduct);

        /* ===============================
           PLUS
        =============================== */

        holder.btnPlus.setOnClickListener(v -> {

            int newQty = model.getQty() + 1;

            updateCartQty(
                    model.getCartId(),
                    newQty,
                    model,
                    holder
            );
        });

        /* ===============================
           MINUS
        =============================== */

        holder.btnMinus.setOnClickListener(v -> {

            int newQty = model.getQty() - 1;

            if(newQty < 1){
                return;
            }

            updateCartQty(
                    model.getCartId(),
                    newQty,
                    model,
                    holder
            );
        });

        /* ===============================
           DELETE
        =============================== */

        holder.btnDelete.setOnClickListener(v -> {

            removeCartItem(
                    model.getCartId(),
                    position
            );
        });
    }

    @Override
    public int getItemCount() {
        return cartList.size();
    }

    /* ==========================================
       UPDATE CART QTY
    ========================================== */

    private void updateCartQty(
            int cartId,
            int qty,
            CartModel model,
            ViewHolder holder
    ) {

        try {

            JSONObject json = new JSONObject();

            json.put("cart_id", cartId);

            json.put("qty", qty);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(Constants.UPDATE_CART_QTY_URL)
                    .post(body)
                    .build();

            OkHttpClient client =
                    ApiClient.getPublicClient();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {

                    Log.e("CART_QTY", e.toString());
                }

                @Override
                public void onResponse(Call call, Response response)
                        throws IOException {

                    model.setQty(qty);

                    ((android.app.Activity) context)
                            .runOnUiThread(() -> {

                                holder.tvQty.setText(
                                        String.valueOf(qty)
                                );

                                notifyDataSetChanged();
                            });
                }
            });

        } catch (Exception e){

            e.printStackTrace();
        }
    }

    /* ==========================================
       REMOVE ITEM
    ========================================== */

    private void removeCartItem(
            int cartId,
            int position
    ) {

        try {

            JSONObject json = new JSONObject();

            json.put("cart_id", cartId);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(Constants.REMOVE_CART_ITEM_URL)
                    .post(body)
                    .build();

            OkHttpClient client =
                    ApiClient.getPublicClient();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {

                    Log.e("REMOVE_CART", e.toString());
                }

                @Override
                public void onResponse(Call call, Response response)
                        throws IOException {

                    ((android.app.Activity) context)
                            .runOnUiThread(() -> {

                                cartList.remove(position);

                                notifyItemRemoved(position);

                                Toast.makeText(
                                        context,
                                        "Item removed",
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                }
            });

        } catch (Exception e){

            e.printStackTrace();
        }
    }

    /* ==========================================
       VIEW HOLDER
    ========================================== */

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        ImageView imgProduct, btnDelete;

        TextView tvName, tvPrice, tvQty;

        TextView btnMinus, btnPlus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imgProduct =
                    itemView.findViewById(R.id.imgProduct);

            btnDelete =
                    itemView.findViewById(R.id.btnDelete);

            tvName =
                    itemView.findViewById(R.id.tvName);

            tvPrice =
                    itemView.findViewById(R.id.tvPrice);

            tvQty =
                    itemView.findViewById(R.id.tvQty);

            btnMinus =
                    itemView.findViewById(R.id.btnMinus);

            btnPlus =
                    itemView.findViewById(R.id.btnPlus);
        }
    }
}