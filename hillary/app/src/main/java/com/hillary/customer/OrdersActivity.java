package com.hillary.customer;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class OrdersActivity extends AppCompatActivity {

    RecyclerView recyclerOrders;

    ArrayList<OrderModel> orderList;

    OrdersAdapter adapter;

    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_orders);

        session = new SessionManager(this);

        recyclerOrders =
                findViewById(R.id.recyclerOrders);

        recyclerOrders.setLayoutManager(
                new LinearLayoutManager(this)
        );

        orderList = new ArrayList<>();

        adapter = new OrdersAdapter(
                this,
                orderList
        );

        recyclerOrders.setAdapter(adapter);

        loadOrders();
    }

    private void loadOrders() {

        String url =
                Constants.GET_CUSTOMER_ORDERS_URL
                        +
                        "?customer_id="
                        +
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

                Log.e("ORDERS_ERROR", e.toString());
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

                    JSONArray orders =
                            obj.getJSONArray("orders");

                    orderList.clear();

                    for(int i = 0; i < orders.length(); i++){

                        JSONObject item =
                                orders.getJSONObject(i);

                        OrderModel model =
                                new OrderModel(
                                        item.getInt("id"),
                                        item.getString("order_number"),
                                        item.getDouble("total_amount"),
                                        item.getString("status"),
                                        item.getString("created_at")
                                );

                        orderList.add(model);
                    }

                    runOnUiThread(() ->
                            adapter.notifyDataSetChanged()
                    );

                } catch (Exception e){

                    e.printStackTrace();
                }
            }
        });
    }
}