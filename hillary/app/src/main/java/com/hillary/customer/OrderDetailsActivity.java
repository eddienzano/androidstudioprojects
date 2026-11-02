package com.hillary.customer;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hillary.R;
import com.hillary.network.ApiClient;
import com.hillary.network.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OrderDetailsActivity extends AppCompatActivity {

    TextView tvOrderNumber,
            tvStatus,
            tvTotal,
            tvDate,
            tvPhone,
            tvAddress;

    RecyclerView recyclerItems;

    ArrayList<OrderItemModel> itemList;

    OrderItemsAdapter adapter;

    int orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_order_details);

        orderId =
                getIntent().getIntExtra("order_id", 0);

        tvOrderNumber =
                findViewById(R.id.tvOrderNumber);

        tvStatus =
                findViewById(R.id.tvStatus);

        tvTotal =
                findViewById(R.id.tvTotal);

        tvDate =
                findViewById(R.id.tvDate);

        tvPhone =
                findViewById(R.id.tvPhone);

        tvAddress =
                findViewById(R.id.tvAddress);

        recyclerItems =
                findViewById(R.id.recyclerItems);

        recyclerItems.setLayoutManager(
                new LinearLayoutManager(this)
        );

        itemList = new ArrayList<>();

        adapter = new OrderItemsAdapter(
                this,
                itemList
        );

        recyclerItems.setAdapter(adapter);

        loadOrderDetails();
    }

    private void loadOrderDetails() {

        String url =
                Constants.GET_ORDER_DETAILS_URL
                        +
                        "?order_id="
                        +
                        orderId;

        OkHttpClient client =
                ApiClient.getPublicClient();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

                Log.e("ORDER_DETAILS", e.toString());
            }

            @Override
            public void onResponse(
                    Call call,
                    Response response
            ) throws IOException {

                if(response.body() == null) return;

                try {

                    String res =
                            response.body().string();

                    JSONObject obj =
                            new JSONObject(res);

                    JSONObject order =
                            obj.getJSONObject("order");

                    JSONArray items =
                            obj.getJSONArray("items");

                    itemList.clear();

                    for(int i = 0; i < items.length(); i++){

                        JSONObject item =
                                items.getJSONObject(i);

                        OrderItemModel model =
                                new OrderItemModel(
                                        item.getInt("product_id"),
                                        item.getString("product_name"),
                                        item.getString("thumbnail"),
                                        item.getDouble("price"),
                                        item.getInt("qty"),
                                        item.getDouble("subtotal")
                                );

                        itemList.add(model);
                    }

                    runOnUiThread(() -> {

                        try {

                            tvOrderNumber.setText(
                                    order.getString("order_number")
                            );

                            tvStatus.setText(
                                    order.getString("status")
                            );

                            tvTotal.setText(
                                    "KES "
                                            +
                                            order.getString("total_amount")
                            );

                            tvDate.setText(
                                    order.getString("created_at")
                            );

                            tvPhone.setText(
                                    order.getString("phone")
                            );

                            tvAddress.setText(
                                    order.getString("delivery_address")
                            );

                            adapter.notifyDataSetChanged();

                        } catch (Exception e){

                            e.printStackTrace();
                        }

                    });

                } catch (Exception e){

                    e.printStackTrace();
                }
            }
        });
    }
}