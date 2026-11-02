package com.hillary.customer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.hillary.R;
import com.hillary.network.ApiClient;
import com.hillary.network.Constants;
import com.hillary.utils.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProductDetailsActivity extends AppCompatActivity {

    ImageView imgProduct, btnBack;

    TextView tvName, tvSupplier, tvPrice, tvStock, tvDescription;

    MaterialButton btnAddToCart, btnBuyNow;

    int productId;

    SessionManager session;
    ImageView btnCart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_customer_product_details);

        session = new SessionManager(this);

        imgProduct = findViewById(R.id.imgProduct);
        btnBack = findViewById(R.id.btnBack);

        tvName = findViewById(R.id.tvName);
        tvSupplier = findViewById(R.id.tvSupplier);
        tvPrice = findViewById(R.id.tvPrice);
        tvStock = findViewById(R.id.tvStock);
        tvDescription = findViewById(R.id.tvDescription);

        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnBuyNow = findViewById(R.id.btnBuyNow);

        btnCart = findViewById(R.id.btnCart);

        productId = getIntent().getIntExtra("product_id", 0);

        btnBack.setOnClickListener(v -> finish());

        btnAddToCart.setOnClickListener(v -> {

            if(!session.isCustomerLoggedIn()){

                Intent intent = new Intent(
                        ProductDetailsActivity.this,
                        CustomerLoginActivity.class
                );

                intent.putExtra("product_id", productId);

                startActivity(intent);

                return;
            }

            addToCart();
        });

        btnCart.setOnClickListener(v -> {

            if(!session.isCustomerLoggedIn()){

                Intent intent = new Intent(
                        ProductDetailsActivity.this,
                        CustomerLoginActivity.class
                );

                intent.putExtra("product_id", productId);

                startActivity(intent);

                return;
            }

            startActivity(new Intent(
                    ProductDetailsActivity.this,
                    CartActivity.class
            ));
        });

        loadProduct();

        boolean autoAdd =
                getIntent().getBooleanExtra(
                        "auto_add_to_cart",
                        false
                );

        if(autoAdd){

            addToCart();
        }
    }

    private void addToCart() {

        try {

            JSONObject json = new JSONObject();

            json.put(
                    "customer_id",
                    session.getCustomerId()
            );

            json.put("product_id", productId);

            json.put("qty", 1);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(Constants.ADD_TO_CART_URL)
                    .post(body)
                    .build();

            OkHttpClient client =
                    ApiClient.getPublicClient();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    ProductDetailsActivity.this,
                                    "Failed to add to cart",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );
                }

                @Override
                public void onResponse(Call call, Response response)
                        throws IOException {

                    if(response.body() == null) return;

                    String res = response.body().string();

                    runOnUiThread(() ->
                            Toast.makeText(
                                    ProductDetailsActivity.this,
                                    "Added to cart",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );
                }
            });

        } catch (JSONException e) {

            e.printStackTrace();
        }
    }

    private void loadProduct() {

        OkHttpClient client = ApiClient.getPublicClient();

        String url =
                Constants.PRODUCT_DETAILS_URL +
                        "?product_id=" + productId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

                Log.e("PRODUCT_DETAILS", e.toString());
            }

            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {

                if(response.body() == null) return;

                try {

                    String res = response.body().string();

                    JSONObject jsonObject =
                            new JSONObject(res);

                    JSONObject product =
                            jsonObject.getJSONObject("product");

                    runOnUiThread(() -> {

                        try {

                            tvName.setText(
                                    product.getString("product_name")
                            );

                            tvSupplier.setText(
                                    product.getString("supplier_name")
                            );

                            tvPrice.setText(
                                    "KES " +
                                            product.getString("price")
                            );

                            int stock = Integer.parseInt(
                                    product.getString("stock")
                            );

                            if(stock > 0){

                                tvStock.setText("In Stock");

                            } else {

                                tvStock.setText("Out Of Stock");
                            }

                            tvDescription.setText(
                                    product.getString("description")
                            );

                            Glide.with(ProductDetailsActivity.this)
                                    .load(product.getString("thumbnail"))
                                    .placeholder(
                                            R.drawable.ic_launcher_background
                                    )
                                    .into(imgProduct);

                        } catch (Exception e){

                            e.printStackTrace();
                        }

                    });

                } catch (Exception e){

                    Log.e("PRODUCT_PARSE", e.toString());
                }
            }
        });
    }
}