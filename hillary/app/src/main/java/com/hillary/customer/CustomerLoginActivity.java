package com.hillary.customer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.hillary.R;
import com.hillary.network.ApiClient;
import com.hillary.network.Constants;
import com.hillary.utils.SessionManager;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CustomerLoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;

    MaterialButton btnLogin;

    TextView btnRegister, btnContinue;

    SessionManager session;

    int pendingProductId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_customer_login);

        session = new SessionManager(this);

        pendingProductId =
                getIntent().getIntExtra(
                        "product_id",
                        0
                );

        etEmail = findViewById(R.id.etEmail);

        etPassword = findViewById(R.id.etPassword);

        btnLogin = findViewById(R.id.btnLogin);

        btnRegister = findViewById(R.id.btnRegister);

        btnContinue = findViewById(R.id.btnContinue);

        btnRegister.setOnClickListener(v -> {

            Intent intent = new Intent(
                    CustomerLoginActivity.this,
                    CustomerRegisterActivity.class
            );

            intent.putExtra(
                    "product_id",
                    pendingProductId
            );

            startActivity(intent);
        });

        btnContinue.setOnClickListener(v -> {

            startActivity(new Intent(
                    CustomerLoginActivity.this,
                    MarketplaceActivity.class
            ));

            finish();
        });

        btnLogin.setOnClickListener(v -> login());
    }

    private void login() {

        String email =
                etEmail.getText().toString().trim();

        String password =
                etPassword.getText().toString().trim();

        if(email.isEmpty() || password.isEmpty()){

            Toast.makeText(
                    this,
                    "All fields required",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        try {

            JSONObject json = new JSONObject();

            json.put("email", email);

            json.put("password", password);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(Constants.CUSTOMER_LOGIN_URL)
                    .post(body)
                    .build();

            OkHttpClient client =
                    ApiClient.getPublicClient();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    CustomerLoginActivity.this,
                                    "Login failed",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );
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

                        boolean success =
                                obj.getBoolean("success");

                        runOnUiThread(() -> {

                            try {

                                if(success){

                                    JSONObject customer =
                                            obj.getJSONObject("customer");

                                    session.saveCustomerSession(
                                            customer.getInt("id"),
                                            customer.getString("full_name"),
                                            customer.getString("email")
                                    );

                                    Intent intent;

                                    if(pendingProductId != 0){

                                        intent = new Intent(
                                                CustomerLoginActivity.this,
                                                ProductDetailsActivity.class
                                        );

                                        intent.putExtra(
                                                "product_id",
                                                pendingProductId
                                        );

                                        intent.putExtra(
                                                "auto_add_to_cart",
                                                true
                                        );

                                    } else {

                                        intent = new Intent(
                                                CustomerLoginActivity.this,
                                                MarketplaceActivity.class
                                        );
                                    }

                                    startActivity(intent);

                                    finish();

                                } else {

                                    Toast.makeText(
                                            CustomerLoginActivity.this,
                                            obj.getString("message"),
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }

                            } catch (Exception e){

                                e.printStackTrace();
                            }

                        });

                    } catch (Exception e){

                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e){

            e.printStackTrace();
        }
    }
}