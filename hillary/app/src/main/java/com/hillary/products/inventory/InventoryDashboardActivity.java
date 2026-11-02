package com.hillary.products.inventory;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.hillary.R;
import com.hillary.network.ApiClient;
import com.hillary.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

import android.content.Intent;

public class InventoryDashboardActivity
        extends AppCompatActivity {

    TextView txtProducts,
            txtStock,
            txtLowStock,
            txtOutStock,
            txtInventoryValue;

    Button btnManageInventory,
            btnViewLogs;

    RecyclerView recyclerSuppliers;

    LineChart lineChart;

    SessionManager session;

    List<DashboardSupplier> supplierList;

    DashboardSupplierAdapter supplierAdapter;

    String BASE_URL =
            "https://www.aaagrowers.co.ke/inventory/fillers/api/inventory/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_inventory_dashboard
        );

        session = new SessionManager(this);

        initViews();

        loadSummary();

        loadMovements();

        loadSuppliers();
    }

    private void initViews() {

        txtProducts = findViewById(R.id.txtProducts);
        txtStock = findViewById(R.id.txtStock);
        txtLowStock = findViewById(R.id.txtLowStock);
        txtOutStock = findViewById(R.id.txtOutStock);
        txtInventoryValue =
                findViewById(R.id.txtInventoryValue);

        recyclerSuppliers =
                findViewById(R.id.recyclerSuppliers);

        lineChart = findViewById(R.id.lineChart);

        supplierList = new ArrayList<>();

        recyclerSuppliers.setLayoutManager(
                new LinearLayoutManager(this)
        );

        supplierAdapter =
                new DashboardSupplierAdapter(
                        this,
                        supplierList
                );

        recyclerSuppliers.setAdapter(
                supplierAdapter
        );

        btnManageInventory =
                findViewById(R.id.btnManageInventory);

        btnViewLogs =
                findViewById(R.id.btnViewLogs);

        btnManageInventory.setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            this,
                            InventoryActivity.class
                    )
            );
        });

        btnViewLogs.setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            this,
                            InventoryLogsActivity.class
                    )
            );
        });
    }

    /* =========================
       SUMMARY
    ========================= */
    private void loadSummary() {

        Request request = new Request.Builder()
                .url(BASE_URL +
                        "dashboard_summary.php")
                .addHeader(
                        "Authorization",
                        "Bearer " + session.getToken()
                )
                .build();

        ApiClient.getClient(this)
                .newCall(request)
                .enqueue(new Callback() {

                    @Override
                    public void onFailure(okhttp3.Call call,
                                          IOException e) {

                        runOnUiThread(() ->
                                Toast.makeText(
                                        InventoryDashboardActivity.this,
                                        "Failed to load dashboard",
                                        Toast.LENGTH_SHORT
                                ).show()
                        );
                    }

                    @Override
                    public void onResponse(okhttp3.Call call,
                                           Response response)
                            throws IOException {

                        if(response.body() == null)
                            return;

                        String res =
                                response.body().string();

                        try {

                            JSONObject obj =
                                    new JSONObject(res);

                            JSONObject s =
                                    obj.getJSONObject("message")
                                            .getJSONObject("summary");

                            runOnUiThread(() -> {

                                txtProducts.setText(
                                        s.optString(
                                                "total_products"
                                        )
                                );

                                txtStock.setText(
                                        s.optString(
                                                "total_stock"
                                        )
                                );

                                txtLowStock.setText(
                                        s.optString(
                                                "low_stock"
                                        )
                                );

                                txtOutStock.setText(
                                        s.optString(
                                                "out_of_stock"
                                        )
                                );

                                txtInventoryValue.setText(
                                        "KES "
                                                + s.optString(
                                                "inventory_value"
                                        )
                                );
                            });

                        } catch (Exception ignored) {}
                    }
                });
    }

    /* =========================
       MOVEMENTS CHART
    ========================= */
    private void loadMovements() {

        Request request = new Request.Builder()
                .url(BASE_URL +
                        "dashboard_movements.php")
                .addHeader(
                        "Authorization",
                        "Bearer " + session.getToken()
                )
                .build();

        ApiClient.getClient(this)
                .newCall(request)
                .enqueue(new Callback() {

                    @Override
                    public void onFailure(okhttp3.Call call,
                                          IOException e) {

                    }

                    @Override
                    public void onResponse(okhttp3.Call call,
                                           Response response)
                            throws IOException {

                        if(response.body() == null)
                            return;

                        String res =
                                response.body().string();

                        try {

                            JSONObject obj =
                                    new JSONObject(res);

                            JSONArray arr =
                                    obj.getJSONArray("message");

                            List<Entry> entries =
                                    new ArrayList<>();

                            for(int i=0;
                                i<arr.length();
                                i++) {

                                JSONObject x =
                                        arr.getJSONObject(i);

                                entries.add(
                                        new Entry(
                                                i,
                                                (float)
                                                        x.getDouble(
                                                                "stock_in"
                                                        )
                                        )
                                );
                            }

                            runOnUiThread(() -> {

                                LineDataSet dataSet =
                                        new LineDataSet(
                                                entries,
                                                "Stock In"
                                        );

                                LineData lineData =
                                        new LineData(dataSet);

                                lineChart.setData(
                                        lineData
                                );

                                lineChart.invalidate();
                            });

                        } catch (Exception ignored) {}
                    }
                });
    }

    /* =========================
       SUPPLIERS
    ========================= */
    private void loadSuppliers() {

        Request request = new Request.Builder()
                .url(BASE_URL +
                        "dashboard_suppliers.php")
                .addHeader(
                        "Authorization",
                        "Bearer " + session.getToken()
                )
                .build();

        ApiClient.getClient(this)
                .newCall(request)
                .enqueue(new Callback() {

                    @Override
                    public void onFailure(okhttp3.Call call,
                                          IOException e) {

                    }

                    @Override
                    public void onResponse(okhttp3.Call call,
                                           Response response)
                            throws IOException {

                        if(response.body() == null)
                            return;

                        String res =
                                response.body().string();

                        try {

                            JSONObject obj =
                                    new JSONObject(res);

                            JSONArray arr =
                                    obj.getJSONArray("message");

                            supplierList.clear();

                            for(int i=0;
                                i<arr.length();
                                i++) {

                                JSONObject x =
                                        arr.getJSONObject(i);

                                supplierList.add(

                                        new DashboardSupplier(

                                                x.getString(
                                                        "supplier_name"
                                                ),

                                                x.getString(
                                                        "total_products"
                                                ),

                                                x.getString(
                                                        "total_stock"
                                                )
                                        )
                                );
                            }

                            runOnUiThread(() ->
                                    supplierAdapter
                                            .notifyDataSetChanged()
                            );

                        } catch (Exception ignored) {}
                    }
                });
    }
}