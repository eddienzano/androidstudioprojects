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

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CustomerRegisterActivity extends AppCompatActivity {

    EditText etName, etEmail, etPhone, etPassword;

    MaterialButton btnRegister;

    TextView btnLogin;

    int pendingProductId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_customer_register);

        pendingProductId =
                getIntent().getIntExtra("product_id", 0);

        etName = findViewById(R.id.etName);

        etEmail = findViewById(R.id.etEmail);

        etPhone = findViewById(R.id.etPhone);

        etPassword = findViewById(R.id.etPassword);

        btnRegister = findViewById(R.id.btnRegister);

        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> registerCustomer());
    }

    private void registerCustomer(){

        try {

            JSONObject json = new JSONObject();

            json.put(
                    "full_name",
                    etName.getText().toString().trim()
            );

            json.put(
                    "email",
                    etEmail.getText().toString().trim()
            );

            json.put(
                    "phone",
                    etPhone.getText().toString().trim()
            );

            json.put(
                    "password",
                    etPassword.getText().toString().trim()
            );

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(Constants.CUSTOMER_REGISTER_URL)
                    .post(body)
                    .build();

            OkHttpClient client =
                    ApiClient.getPublicClient();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    CustomerRegisterActivity.this,
                                    "Registration failed",
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

                        runOnUiThread(() -> {

                            try {

                                Toast.makeText(
                                        CustomerRegisterActivity.this,
                                        obj.getString("message"),
                                        Toast.LENGTH_SHORT
                                ).show();

                                if(obj.getBoolean("success")){

                                    Intent intent = new Intent(
                                            CustomerRegisterActivity.this,
                                            CustomerLoginActivity.class
                                    );

                                    intent.putExtra(
                                            "product_id",
                                            pendingProductId
                                    );

                                    startActivity(intent);

                                    finish();
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