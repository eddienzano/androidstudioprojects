package com.hillary.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hillary.Dashboard.DashboardActivity;
import com.hillary.R;
import com.hillary.network.ApiClient;
import com.hillary.network.Constants;
import com.hillary.utils.SessionManager;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;

    Button btnLogin;

    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);

        etPassword = findViewById(R.id.etPassword);

        btnLogin = findViewById(R.id.btnLogin);

        session = new SessionManager(this);

        btnLogin.setOnClickListener(v -> login());
    }

    private void login(){

        String email = etEmail.getText().toString().trim();

        String password = etPassword.getText().toString().trim();

        if(email.isEmpty() || password.isEmpty()){

            Toast.makeText(
                    this,
                    "All fields required",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        btnLogin.setEnabled(false);

        RequestBody body = new FormBody.Builder()
                .add("email", email)
                .add("password", password)
                .build();

        Request request = new Request.Builder()
                .url(Constants.LOGIN_URL)
                .post(body)
                .build();

        ApiClient.getPublicClient()
                .newCall(request)
                .enqueue(new Callback() {

                    @Override
                    public void onFailure(Call call, IOException e) {

                        runOnUiThread(() -> {

                            btnLogin.setEnabled(true);

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Connection failed",
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response)
                            throws IOException {

                        runOnUiThread(() ->
                                btnLogin.setEnabled(true)
                        );

                        if(response.body() == null) return;

                        String res = response.body().string();

                        try {

                            JSONObject obj = new JSONObject(res);

                            String status =
                                    obj.getString("status");

                            String message =
                                    obj.getString("message");

                            runOnUiThread(() ->
                                    Toast.makeText(
                                            LoginActivity.this,
                                            message,
                                            Toast.LENGTH_LONG
                                    ).show()
                            );

                            if(status.equals("success")){

                                JSONObject data =
                                        obj.getJSONObject("data");

                                String token =
                                        data.getString("token");

                                JSONObject user =
                                        data.getJSONObject("user");

                                String name =
                                        user.getString("name");

                                int id =
                                        user.getInt("id");

                                session.saveSupplierSession(
                                        token,
                                        name,
                                        id,
                                        email
                                );

                                startActivity(
                                        new Intent(
                                                LoginActivity.this,
                                                DashboardActivity.class
                                        )
                                );

                                finish();
                            }

                        } catch (Exception e){

                            e.printStackTrace();

                            runOnUiThread(() ->
                                    Toast.makeText(
                                            LoginActivity.this,
                                            "Invalid response",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                        }
                    }
                });
    }
}