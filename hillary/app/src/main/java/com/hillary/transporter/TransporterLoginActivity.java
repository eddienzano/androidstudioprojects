package com.hillary.transporter;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hillary.R;
import com.hillary.network.ApiClient;
import com.hillary.network.Constants;
import com.hillary.utils.SessionManager;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TransporterLoginActivity extends AppCompatActivity {

    EditText phone, password;
    Button btnLogin;

    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transporter_login);

        phone = findViewById(R.id.phone);
        password = findViewById(R.id.password);
        btnLogin = findViewById(R.id.btnLogin);

        session = new SessionManager(this);

        btnLogin.setOnClickListener(v -> login());
    }



    private void login() {

        OkHttpClient client = ApiClient.getPublicClient();

        RequestBody body = new FormBody.Builder()
                .add("phone", phone.getText().toString().trim())
                .add("password", password.getText().toString().trim())
                .build();

        Request request = new Request.Builder()
                .url(Constants.TRANSPORTER_LOGIN_URL)
                .post(body)
                .build();

        new Thread(() -> {

            try {

                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "Server error: " + response.code(),
                                    Toast.LENGTH_LONG).show()
                    );
                    return;
                }

                if (response.body() == null) {
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "Empty server response",
                                    Toast.LENGTH_LONG).show()
                    );
                    return;
                }

                String res = response.body().string();

                JSONObject obj;

                try {
                    obj = new JSONObject(res);
                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "Invalid response: " + res,
                                    Toast.LENGTH_LONG).show()
                    );
                    return;
                }

                runOnUiThread(() -> {

                    try {

                        boolean success = obj.optBoolean("success", false);

                        if (success) {

                            JSONObject t = obj.getJSONObject("transporter");

                            session.saveTransporter(
                                    t.optString("id"),
                                    t.optString("full_name"),
                                    t.optString("phone")
                            );

                            Toast.makeText(this,
                                    "Login successful",
                                    Toast.LENGTH_SHORT).show();



                            startActivity(
                                    new Intent(this, TransporterDashboardActivity.class)
                            );

                            finish();

                        } else {

                            String msg = obj.optString("message", "Login failed");

                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        Toast.makeText(this,
                                "Processing error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }

                });

            } catch (IOException e) {

                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Network error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }

        }).start();
    }
}