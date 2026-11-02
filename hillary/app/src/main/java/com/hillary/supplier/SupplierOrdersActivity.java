package com.hillary.supplier;

import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

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

public class SupplierOrdersActivity extends AppCompatActivity {

    RecyclerView recyclerOrders;

    SupplierOrdersAdapter adapter;

    ArrayList<SupplierOrderModel> orderList;

    SessionManager session;

    TextView tvPending, tvApproved, tvDispatched, tvCompleted;

    String currentStatus = "pending";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_supplier_orders);

        session = new SessionManager(this);

        recyclerOrders = findViewById(R.id.recyclerOrders);

        tvPending = findViewById(R.id.tvPending);
        tvApproved = findViewById(R.id.tvApproved);
        tvDispatched = findViewById(R.id.tvDispatched);
        tvCompleted = findViewById(R.id.tvCompleted);

        recyclerOrders.setLayoutManager(
                new LinearLayoutManager(this)
        );

        orderList = new ArrayList<>();

        adapter = new SupplierOrdersAdapter(
                this,
                orderList
        );

        recyclerOrders.setAdapter(adapter);

        tvPending.setOnClickListener(v -> {
            currentStatus = "pending";
            loadOrders();
        });

        tvApproved.setOnClickListener(v -> {
            currentStatus = "approved";
            loadOrders();
        });

        tvDispatched.setOnClickListener(v -> {
            currentStatus = "dispatched";
            loadOrders();
        });

        tvCompleted.setOnClickListener(v -> {
            currentStatus = "completed";
            loadOrders();
        });

        loadOrders();
    }

    private void loadOrders() {

        String url =
                Constants.GET_SUPPLIER_ORDERS_URL
                        + "?supplier_id="
                        + session.getUserId()
                        + "&status="
                        + currentStatus;

        OkHttpClient client =
                ApiClient.getClient(this);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

                Log.e("SUPPLIER_ORDERS", e.toString());
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

                    JSONArray orders =
                            obj.getJSONArray("orders");

                    orderList.clear();

                    for(int i = 0; i < orders.length(); i++){

                        JSONObject item =
                                orders.getJSONObject(i);

                        SupplierOrderModel model =
                                new SupplierOrderModel(
                                        item.getInt("id"),
                                        item.getString("customer_name"),
                                        item.getString("status"),
                                        item.getString("created_at"),
                                        item.getDouble("total_amount")
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