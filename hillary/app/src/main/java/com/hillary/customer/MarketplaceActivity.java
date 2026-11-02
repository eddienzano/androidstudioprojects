package com.hillary.customer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hillary.R;
import com.hillary.auth.LoginActivity;
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

public class MarketplaceActivity extends AppCompatActivity {

    RecyclerView recyclerProducts;

    SearchView searchView;

    TextView btnSupplierLogin;

    ImageView btnProfile;

    ArrayList<ProductModel> productList;

    ProductBrowserAdapter adapter;

    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_marketplace);

        session = new SessionManager(this);

        recyclerProducts =
                findViewById(R.id.recyclerProducts);

        searchView =
                findViewById(R.id.searchView);

        btnSupplierLogin =
                findViewById(R.id.btnSupplierLogin);

        btnProfile =
                findViewById(R.id.btnProfile);

        recyclerProducts.setLayoutManager(
                new GridLayoutManager(this, 2)
        );

        productList = new ArrayList<>();

        adapter = new ProductBrowserAdapter(
                this,
                productList
        );

        recyclerProducts.setAdapter(adapter);

        btnSupplierLogin.setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            MarketplaceActivity.this,
                            LoginActivity.class
                    )
            );

        });

        btnProfile.setOnClickListener(v -> {

            PopupMenu popupMenu =
                    new PopupMenu(
                            MarketplaceActivity.this,
                            btnProfile
                    );

            if(session.isLoggedIn()){

                popupMenu.getMenu().add("My Profile");

                popupMenu.getMenu().add("My Orders");

                popupMenu.getMenu().add("Logout");

            } else {

                popupMenu.getMenu().add("Customer Login");
            }

            popupMenu.setOnMenuItemClickListener(item -> {

                String title =
                        item.getTitle().toString();

                if(title.equals("Customer Login")){

                    startActivity(
                            new Intent(
                                    MarketplaceActivity.this,
                                    LoginActivity.class
                            )
                    );
                }

                else if(title.equals("My Orders")){

                    startActivity(
                            new Intent(
                                    MarketplaceActivity.this,
                                    OrdersActivity.class
                            )
                    );
                }

                else if(title.equals("My Profile")){

                    startActivity(
                            new Intent(
                                    MarketplaceActivity.this,
                                    CustomerProfileActivity.class
                            )
                    );
                }

                else if(title.equals("Logout")){

                    session.logout();

                    recreate();
                }

                return true;
            });

            popupMenu.show();

        });

        loadProducts();
    }

    private void loadProducts() {

        OkHttpClient client =
                ApiClient.getPublicClient();

        Request request =
                new Request.Builder()
                        .url(Constants.MARKETPLACE_PRODUCTS_URL)
                        .get()
                        .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

                Log.e(
                        "MARKETPLACE_ERROR",
                        e.toString()
                );
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

                    Log.d(
                            "MARKETPLACE_RESPONSE",
                            res
                    );

                    JSONObject jsonObject =
                            new JSONObject(res);

                    JSONArray products =
                            jsonObject.getJSONArray("products");

                    productList.clear();

                    for(int i = 0; i < products.length(); i++){

                        JSONObject obj =
                                products.getJSONObject(i);

                        ProductModel model =
                                new ProductModel(
                                        Integer.parseInt(
                                                obj.optString("id", "0")
                                        ),
                                        obj.optString(
                                                "product_name",
                                                ""
                                        ),
                                        obj.optString(
                                                "description",
                                                ""
                                        ),
                                        obj.optString(
                                                "category",
                                                ""
                                        ),
                                        obj.optString(
                                                "unit",
                                                ""
                                        ),
                                        Double.parseDouble(
                                                obj.optString(
                                                        "price",
                                                        "0"
                                                )
                                        ),
                                        Integer.parseInt(
                                                obj.optString(
                                                        "stock",
                                                        "0"
                                                )
                                        ),
                                        obj.optString(
                                                "thumbnail",
                                                ""
                                        ),
                                        obj.optString(
                                                "supplier_name",
                                                ""
                                        )
                                );

                        productList.add(model);
                    }

                    runOnUiThread(() ->
                            adapter.notifyDataSetChanged()
                    );

                } catch (Exception e){

                    Log.e(
                            "MARKETPLACE_PARSE",
                            e.toString()
                    );
                }
            }
        });
    }
}