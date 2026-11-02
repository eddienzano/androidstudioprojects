package com.hillary.products.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.hillary.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class InventoryActivity extends AppCompatActivity {

    RecyclerView recyclerInventory;
    SwipeRefreshLayout swipeRefresh;
    EditText edtSearch;
    TextView txtLowStockCount;
    LinearLayout layoutEmpty;

    List<ProductInventory> list;
    InventoryAdapter adapter;

    String BASE_URL =
            "https://www.aaagrowers.co.ke/inventory/fillers/api/inventory/";

    String token = "";

    private static final String TAG = "INVENTORY_DEBUG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        initViews();
        setupRecycler();
        setupSearch();
        setupRefresh();
        loadProducts();
    }

    private void initViews() {

        recyclerInventory = findViewById(R.id.recyclerInventory);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        edtSearch = findViewById(R.id.edtSearch);
        txtLowStockCount = findViewById(R.id.txtLowStockCount);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        Button btnLogs = findViewById(R.id.btnLogs);
        Button btnDashboard = findViewById(R.id.btnDashboard);

        token = getSharedPreferences("app", MODE_PRIVATE)
                .getString("token", "");

        btnLogs.setOnClickListener(v ->
                startActivity(new Intent(this, InventoryLogsActivity.class))
        );

        btnDashboard.setOnClickListener(v ->
                startActivity(new Intent(this, InventoryDashboardActivity.class))
        );
    }

    private void setupRecycler() {

        list = new ArrayList<>();

        recyclerInventory.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new InventoryAdapter(
                this,
                list,
                token,
                BASE_URL
        );

        recyclerInventory.setAdapter(adapter);
    }

    private void setupSearch() {

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                adapter.getFilter().filter(s);
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadProducts);
    }

    private void loadProducts() {

        swipeRefresh.setRefreshing(true);

        new Thread(() -> {

            try {

                OkHttpClient client = new OkHttpClient();

                String url = BASE_URL + "get_products.php";

                Log.d("INVENTORY_DEBUG", "REQUEST URL: " + url);

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + token)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.body() == null) {
                    throw new Exception("Empty response body");
                }

                String res = response.body().string();

                Log.d("INVENTORY_DEBUG", "RAW RESPONSE: " + res);

                // 🔴 HARD GUARD BEFORE JSON PARSE
                if (!res.trim().startsWith("{")) {
                    throw new Exception("Server did not return JSON. Got: " + res);
                }

                JSONObject obj = new JSONObject(res);

                Log.d("INVENTORY_DEBUG", "JSON PARSED SUCCESSFULLY");

                if (!obj.optString("status").equals("success")) {
                    throw new Exception("API Error: " + obj.toString());
                }

                JSONArray arr = obj.optJSONArray("products");

                if (arr == null) {
                    throw new Exception("Products array missing in response");
                }

                List<ProductInventory> tempList = new ArrayList<>();

                int lowStockCount = 0;

                for (int i = 0; i < arr.length(); i++) {

                    JSONObject p = arr.getJSONObject(i);

                    String stockStr = p.optString("stock", "0");

                    double stock = 0;

                    try {

                        stock = Double.parseDouble(stockStr);

                    } catch (Exception e) {

                        Log.e(
                                "INVENTORY_DEBUG",
                                "Stock parse error: " + stockStr
                        );
                    }

                    tempList.add(

                            new ProductInventory(

                                    p.optString("id"),

                                    p.optString("product_name"),

                                    p.optString("supplier_name"),

                                    p.optString("thumbnail"),

                                    stockStr,

                                    p.optString("category"),

                                    p.optString("sku"),

                                    p.optString("price")
                            )
                    );

                    if(stock < 10){

                        lowStockCount++;
                    }
                }
                    double stock = 0;


                int finalLowStockCount = lowStockCount;

                runOnUiThread(() -> {

                    swipeRefresh.setRefreshing(false);
                    adapter.updateList(tempList);

                    txtLowStockCount.setText(finalLowStockCount + " Low Stock Items");

                    if (list.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        recyclerInventory.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        recyclerInventory.setVisibility(View.VISIBLE);
                    }
                });

            } catch (Exception e) {

                Log.e("INVENTORY_DEBUG", "ERROR", e);

                runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

        }).start();
    }
}