package com.unitedselections;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class LoginActivity extends AppCompatActivity {

    EditText username, password;
    Button loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);

        loginBtn.setOnClickListener(v -> {

            String user = username.getText().toString().trim();
            String pass = password.getText().toString().trim();

            new Thread(() -> {

                try {

                    URL url = new URL("https://unitedselections.supplybridge.co.ke/api/login.php");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);

                    String postData =
                            "username=" + URLEncoder.encode(user, "UTF-8") +
                                    "&password=" + URLEncoder.encode(pass, "UTF-8");

                    OutputStream os = conn.getOutputStream();
                    os.write(postData.getBytes());
                    os.flush();
                    os.close();

                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                    );

                    StringBuilder sb = new StringBuilder();
                    String line;

                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }

                    JSONObject obj = new JSONObject(sb.toString());

                    runOnUiThread(() -> {

                        try {

                            if (obj.getString("status").equals("success")) {

                                Intent i = new Intent(this, DashboardActivity.class);
                                i.putExtra("role", obj.getString("role"));
                                i.putExtra("username", obj.getString("username"));
                                startActivity(i);
                                finish();

                            } else {
                                Toast.makeText(this,
                                        obj.getString("message"),
                                        Toast.LENGTH_SHORT).show();
                            }

                        } catch (Exception e) {
                            Toast.makeText(this, "Parse error", Toast.LENGTH_SHORT).show();
                        }

                    });

                } catch (Exception e) {

                    runOnUiThread(() ->
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }

            }).start();
        });
    }
}