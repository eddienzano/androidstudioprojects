package com.hillary.supplier;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hillary.R;
import com.hillary.network.ApiClient;
import com.hillary.network.Constants;
import com.hillary.utils.SessionManager;

import com.hillary.supplier.SupplierOrderItemsAdapter;
import com.hillary.supplier.SupplierOrderItemModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class SupplierOrderDetailsActivity extends AppCompatActivity {

    RecyclerView recyclerItems;
    TextView tvOrderId, tvTotal;

    int orderId;
    SessionManager session;

    ArrayList<SupplierOrderItemModel> items = new ArrayList<>();
    SupplierOrderItemsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supplier_order_details);

        session = new SessionManager(this);

        orderId = getIntent().getIntExtra("order_id", -1);

        recyclerItems = findViewById(R.id.recyclerItems);
        tvOrderId = findViewById(R.id.tvOrderId);
        tvTotal = findViewById(R.id.tvTotal);

        recyclerItems.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SupplierOrderItemsAdapter(this, items);
        recyclerItems.setAdapter(adapter);

        tvOrderId.setText("Order #" + orderId);

        loadOrderDetails();
    }

    private void loadOrderDetails() {

        String url = Constants.GET_ORDER_DETAILS_URL + "?order_id=" + orderId;

        Request request = new Request.Builder()
                .url(url)
                .build();

        ApiClient.getClient(this).newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.body() == null) return;

                try {

                    String res = response.body().string();

                    JSONObject obj = new JSONObject(res);

                    JSONArray arr = obj.getJSONArray("items");

                    items.clear();

                    for (int i = 0; i < arr.length(); i++) {

                        JSONObject o = arr.getJSONObject(i);

                        items.add(new SupplierOrderItemModel(
                                o.getString("product_name"),
                                o.getInt("qty"),
                                o.getDouble("price"),
                                o.getDouble("subtotal")
                        ));
                    }

                    runOnUiThread(() -> adapter.notifyDataSetChanged());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}