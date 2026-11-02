package com.hillary.products.inventory;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.hillary.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class InventoryDetailsActivity
        extends AppCompatActivity {

    ImageView imgProduct;

    TextView txtName,
            txtSupplier,
            txtStock,
            txtPrice,
            txtValue,
            txtCategory,
            txtSku,
            txtLowStock;

    Button btnStockIn,
            btnStockOut;

    RecyclerView recyclerLogs;

    LineChart lineChart;

    InventoryLogsAdapter logsAdapter;

    List<InventoryLog> logsList;

    String BASE_URL =
            "https://www.aaagrowers.co.ke/inventory/fillers/api/";

    String token = "";

    String productId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_inventory_details
        );

        token = getSharedPreferences(
                "app",
                MODE_PRIVATE
        ).getString("token", "");

        productId = getIntent()
                .getStringExtra("product_id");

        initViews();

        loadDetails();

        loadChart();

        loadLogs();
    }

    private void initViews() {

        imgProduct = findViewById(R.id.imgProduct);

        txtName = findViewById(R.id.txtName);
        txtSupplier = findViewById(R.id.txtSupplier);
        txtStock = findViewById(R.id.txtStock);
        txtPrice = findViewById(R.id.txtPrice);
        txtValue = findViewById(R.id.txtValue);
        txtCategory = findViewById(R.id.txtCategory);
        txtSku = findViewById(R.id.txtSku);
        txtLowStock = findViewById(R.id.txtLowStock);

        btnStockIn = findViewById(R.id.btnStockIn);
        btnStockOut = findViewById(R.id.btnStockOut);

        recyclerLogs =
                findViewById(R.id.recyclerLogs);

        lineChart =
                findViewById(R.id.lineChart);

        logsList = new ArrayList<>();

        recyclerLogs.setLayoutManager(
                new LinearLayoutManager(this)
        );

        logsAdapter =
                new InventoryLogsAdapter(
                        this,
                        logsList,
                        BASE_URL
                );

        recyclerLogs.setAdapter(logsAdapter);

        btnStockIn.setOnClickListener(v -> {
            showStockDialog(true);
        });

        btnStockOut.setOnClickListener(v -> {
            showStockDialog(false);
        });
    }

    /* =========================================
       LOAD DETAILS
    ========================================= */

    private void loadDetails() {

        new Thread(() -> {

            try {

                OkHttpClient client =
                        new OkHttpClient();

                Request request =
                        new Request.Builder()
                                .url(
                                        BASE_URL
                                                + "inventory/product_details.php?id="
                                                + productId
                                )
                                .addHeader(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .build();

                Response response =
                        client.newCall(request)
                                .execute();

                String res =
                        response.body().string();

                JSONObject obj =
                        new JSONObject(res);

                JSONObject p =
                        obj.getJSONObject("product");

                runOnUiThread(() -> {

                    txtName.setText(
                            p.optString("product_name")
                    );

                    txtSupplier.setText(
                            "Supplier: "
                                    + p.optString(
                                    "supplier_name"
                            )
                    );

                    txtStock.setText(
                            "Stock: "
                                    + p.optString("stock")
                    );

                    txtPrice.setText(
                            "Price: KES "
                                    + p.optString("price")
                    );

                    double stock =
                            p.optDouble("stock");

                    double price =
                            p.optDouble("price");

                    double value =
                            stock * price;

                    txtValue.setText(
                            "Inventory Value: KES "
                                    + value
                    );

                    txtCategory.setText(
                            "Category: "
                                    + p.optString(
                                    "category"
                            )
                    );

                    txtSku.setText(
                            "SKU: "
                                    + p.optString("sku")
                    );

                    if(stock < 10){

                        txtLowStock.setText(
                                "LOW STOCK ALERT"
                        );

                        txtLowStock.setTextColor(
                                Color.RED
                        );

                    }else{

                        txtLowStock.setText(
                                "Stock Healthy"
                        );

                        txtLowStock.setTextColor(
                                Color.parseColor(
                                        "#4CAF50"
                                )
                        );
                    }

                    Glide.with(
                                    InventoryDetailsActivity.this
                            )
                            .load(
                                    BASE_URL
                                            + p.optString(
                                            "thumbnail"
                                    )
                            )
                            .into(imgProduct);
                });

            } catch (Exception e) {

                runOnUiThread(() ->

                        Toast.makeText(
                                this,
                                e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
            }

        }).start();
    }

    /* =========================================
       LOAD CHART
    ========================================= */

    private void loadChart() {

        new Thread(() -> {

            try {

                OkHttpClient client =
                        new OkHttpClient();

                Request request =
                        new Request.Builder()
                                .url(
                                        BASE_URL
                                                + "inventory/product_chart.php?id="
                                                + productId
                                )
                                .addHeader(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .build();

                Response response =
                        client.newCall(request)
                                .execute();

                String res =
                        response.body().string();

                JSONObject obj =
                        new JSONObject(res);

                JSONArray arr =
                        obj.getJSONArray("data");

                List<Entry> entries =
                        new ArrayList<>();

                for(int i=0; i<arr.length(); i++){

                    JSONObject x =
                            arr.getJSONObject(i);

                    entries.add(
                            new Entry(
                                    i,
                                    (float)x.getDouble(
                                            "quantity"
                                    )
                            )
                    );
                }

                runOnUiThread(() -> {

                    LineDataSet dataSet =
                            new LineDataSet(
                                    entries,
                                    "Stock Movement"
                            );

                    LineData lineData =
                            new LineData(dataSet);

                    lineChart.setData(lineData);

                    lineChart.invalidate();
                });

            } catch (Exception ignored) {}
        }).start();
    }

    /* =========================================
       LOAD LOGS
    ========================================= */

    private void loadLogs() {

        new Thread(() -> {

            try {

                OkHttpClient client =
                        new OkHttpClient();

                Request request =
                        new Request.Builder()
                                .url(
                                        BASE_URL
                                                + "inventory/product_logs.php?id="
                                                + productId
                                )
                                .addHeader(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .build();

                Response response =
                        client.newCall(request)
                                .execute();

                String res =
                        response.body().string();

                JSONObject obj =
                        new JSONObject(res);

                JSONArray arr =
                        obj.getJSONArray("logs");

                logsList.clear();

                for(int i=0; i<arr.length(); i++){

                    JSONObject x =
                            arr.getJSONObject(i);

                    logsList.add(

                            new InventoryLog(

                                    x.optString("product_name"),

                                    x.optString("thumbnail"),

                                    x.optString("type"),

                                    x.optString("quantity"),

                                    x.optString("previous_stock"),

                                    x.optString("new_stock"),

                                    x.optString("notes"),

                                    x.optString("created_at")
                            )
                    );
                }

                runOnUiThread(() ->
                        logsAdapter.notifyDataSetChanged()
                );

            } catch (Exception ignored) {}
        }).start();
    }

    /* =========================================
       STOCK DIALOG
    ========================================= */

    private void showStockDialog(
            boolean isStockIn
    ){

        AlertDialog.Builder builder =
                new AlertDialog.Builder(this);

        builder.setTitle(
                isStockIn ?
                        "Stock In"
                        :
                        "Stock Out"
        );

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(40,20,40,20);

        EditText qty = new EditText(this);

        qty.setHint("Quantity");

        qty.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        EditText notes =
                new EditText(this);

        notes.setHint("Notes");

        layout.addView(qty);
        layout.addView(notes);

        builder.setView(layout);

        builder.setPositiveButton(
                "SAVE",
                (d,w)->{

                    sendStock(
                            qty.getText().toString(),
                            notes.getText().toString(),
                            isStockIn
                    );
                });

        builder.setNegativeButton(
                "CANCEL",
                null
        );

        builder.show();
    }

    /* =========================================
       SEND STOCK
    ========================================= */

    private void sendStock(
            String quantity,
            String notes,
            boolean isStockIn
    ){

        new Thread(() -> {

            try {

                String endpoint;

                if(isStockIn){

                    endpoint =
                            "inventory/stock_in.php";

                }else{

                    endpoint =
                            "inventory/stock_out.php";
                }

                OkHttpClient client =
                        new OkHttpClient();

                RequestBody body =
                        new FormBody.Builder()

                                .add(
                                        "product_id",
                                        productId
                                )

                                .add(
                                        "quantity",
                                        quantity
                                )

                                .add(
                                        "notes",
                                        notes
                                )

                                .build();

                Request request =
                        new Request.Builder()

                                .url(
                                        BASE_URL
                                                + endpoint
                                )

                                .addHeader(
                                        "Authorization",
                                        "Bearer " + token
                                )

                                .post(body)

                                .build();

                Response response =
                        client.newCall(request)
                                .execute();

                String res =
                        response.body().string();

                JSONObject obj =
                        new JSONObject(res);

                runOnUiThread(() -> {

                    Toast.makeText(
                            this,
                            obj.optString(
                                    "message"
                            ),
                            Toast.LENGTH_LONG
                    ).show();

                    loadDetails();
                    loadLogs();
                    loadChart();
                });

            } catch (Exception e) {

                runOnUiThread(() ->

                        Toast.makeText(
                                this,
                                e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
            }

        }).start();
    }
}