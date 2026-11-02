package com.hillary.products;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import okhttp3.Request;
import okhttp3.Response;

public class MyProductsActivity extends AppCompatActivity {

    private RecyclerView recyclerProducts;
    private FloatingActionButton fabAdd;

    private ArrayList<Product> productList;
    private ProductAdapter adapter;

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_products);

        initializeViews();

        setupRecyclerView();

        setupListeners();

        loadProducts();
    }

    /* =========================================================
       INITIALIZE VIEWS
    ========================================================= */

    private void initializeViews() {

        session = new SessionManager(this);

        recyclerProducts = findViewById(R.id.recyclerProducts);

        fabAdd = findViewById(R.id.fabAdd);

        productList = new ArrayList<>();

        adapter = new ProductAdapter(
                this,
                productList,
                product -> {

                    Intent intent =
                            new Intent(
                                    MyProductsActivity.this,
                                    ProductDetailsActivity.class
                            );

                    intent.putExtra("id", product.getId());
                    intent.putExtra("name", product.getName());
                    intent.putExtra("description", product.getDescription());
                    intent.putExtra("price", product.getPrice());
                    intent.putExtra("stock", product.getStock());
                    intent.putExtra("image", product.getImage());

                    startActivity(intent);
                }
        );
    }

    /* =========================================================
       SETUP RECYCLER VIEW
    ========================================================= */

    private void setupRecyclerView() {

        GridLayoutManager gridLayoutManager =
                new GridLayoutManager(this, 2);

        recyclerProducts.setLayoutManager(gridLayoutManager);

        recyclerProducts.setHasFixedSize(true);

        recyclerProducts.setItemAnimator(new DefaultItemAnimator());

        recyclerProducts.setAdapter(adapter);

        recyclerProducts.setClipToPadding(false);
    }

    /* =========================================================
       BUTTON LISTENERS
    ========================================================= */

    private void setupListeners() {

        fabAdd.setOnClickListener(v -> {

            Intent intent =
                    new Intent(MyProductsActivity.this,
                            CreateProductActivity.class);

            startActivity(intent);
        });
    }

    /* =========================================================
       LOAD PRODUCTS
    ========================================================= */

    private void loadProducts() {

        Request request = new Request.Builder()
                .url(Constants.MY_PRODUCTS_URL)
                .addHeader(
                        "Authorization",
                        "Bearer " + session.getToken()
                )
                .build();

        ApiClient.getClient(this)
                .newCall(request)
                .enqueue(new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        Log.e("PRODUCTS_API",
                                "API Failure: " + e.getMessage());

                        runOnUiThread(() ->

                                Toast.makeText(
                                        MyProductsActivity.this,
                                        "Failed to load products",
                                        Toast.LENGTH_SHORT
                                ).show()
                        );
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {

                        if (response.body() == null) {

                            runOnUiThread(() ->

                                    Toast.makeText(
                                            MyProductsActivity.this,
                                            "Empty server response",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );

                            return;
                        }

                        String res = response.body().string();

                        Log.d("PRODUCTS_API", res);

                        try {

                            JSONObject obj =
                                    new JSONObject(res);

                            String status = obj.optString("status");

                            if (!status.equalsIgnoreCase("success")) {

                                runOnUiThread(() ->

                                        Toast.makeText(
                                                MyProductsActivity.this,
                                                "Server returned error",
                                                Toast.LENGTH_SHORT
                                        ).show()
                                );

                                return;
                            }
                            JSONArray array =
                                    obj.getJSONArray("data");

                            productList.clear();

                            for (int i = 0; i < array.length(); i++) {

                                JSONObject item =
                                        array.getJSONObject(i);

                                Product product =
                                        new Product(

                                                item.optInt("id"),

                                                item.optString("name"),

                                                item.optString("description"),

                                                item.optDouble("price"),

                                                item.optInt("stock"),

                                                item.optString("thumbnail")
                                        );

                                productList.add(product);
                            }

                            runOnUiThread(() -> {

                                adapter.notifyDataSetChanged();

                                if (productList.isEmpty()) {

                                    Toast.makeText(
                                            MyProductsActivity.this,
                                            "No products found",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            });

                        } catch (Exception e) {

                            Log.e(
                                    "PRODUCTS_API",
                                    "JSON Parsing Error",
                                    e
                            );

                            runOnUiThread(() ->

                                    Toast.makeText(
                                            MyProductsActivity.this,
                                            "Invalid server response",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                        }
                    }
                });
    }

    /* =========================================================
       AUTO REFRESH WHEN RETURNING
    ========================================================= */

    @Override
    protected void onResume() {
        super.onResume();

        loadProducts();
    }
}