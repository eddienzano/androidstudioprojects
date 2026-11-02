package com.hillary.customer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.hillary.R;
import com.hillary.network.ApiClient;
import com.hillary.network.Constants;
import com.hillary.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CartActivity extends AppCompatActivity {

    RecyclerView recyclerCart;

    TextView tvTotal;

    ArrayList<CartModel> cartList;

    CartAdapter adapter;

    SessionManager session;

    MaterialButton btnCheckout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_cart);

        session = new SessionManager(this);

        recyclerCart = findViewById(R.id.recyclerCart);

        tvTotal = findViewById(R.id.tvTotal);

        recyclerCart.setLayoutManager(
                new LinearLayoutManager(this)
        );

        cartList = new ArrayList<>();

        adapter = new CartAdapter(this, cartList);

        recyclerCart.setAdapter(adapter);

        btnCheckout = findViewById(R.id.btnCheckout);

        btnCheckout.setOnClickListener(v -> {

            startActivity(new Intent(
                    CartActivity.this,
                    CheckoutActivity.class
            ));

        });

        loadCart();
    }



    private void loadCart() {

        String url =
                Constants.GET_CART_URL +
                        "?customer_id=" +
                        session.getCustomerId();

        OkHttpClient client =
                ApiClient.getPublicClient();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

                Log.e("CART_ERROR", e.toString());
            }

            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {

                if(response.body() == null) return;

                try {

                    String res =
                            response.body().string();

                    JSONObject obj =
                            new JSONObject(res);

                    JSONArray items =
                            obj.getJSONArray("cart");

                    double total = 0;

                    cartList.clear();

                    for(int i = 0; i < items.length(); i++){

                        JSONObject item =
                                items.getJSONObject(i);

                        CartModel model =
                                new CartModel(
                                        item.getInt("cart_id"),
                                        item.getInt("product_id"),
                                        item.getString("product_name"),
                                        item.getString("thumbnail"),
                                        item.getDouble("price"),
                                        item.getInt("qty"),
                                        item.getInt("stock")
                                );

                        cartList.add(model);

                        total +=
                                item.getDouble("price")
                                        *
                                        item.getInt("qty");
                    }

                    double finalTotal = total;

                    runOnUiThread(() -> {

                        adapter.notifyDataSetChanged();

                        tvTotal.setText(
                                "KES " + finalTotal
                        );

                    });

                } catch (Exception e){

                    e.printStackTrace();
                }
            }
        });
    }
}