package com.unitedselections.admin;

import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.unitedselections.R;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class AddGreenhouseActivity extends AppCompatActivity {

    EditText etName;
    EditText etCategory;
    Button btnSave;

    String API_URL =
            "https://unitedselections.supplybridge.co.ke/api/create_greenhouse.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_greenhouse);

        etName = findViewById(R.id.etName);
        etCategory = findViewById(R.id.etCategory);
        btnSave = findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> saveGreenhouse());
    }

    private void saveGreenhouse() {

        String name = etName.getText().toString().trim();
        String category = etCategory.getText().toString().trim();

        if (name.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {

                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData =
                        "greenhouse_name=" + URLEncoder.encode(name, "UTF-8") +
                                "&category_code=" + URLEncoder.encode(category, "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                runOnUiThread(() -> {

                    if (responseCode == 200) {

                        Toast.makeText(this,
                                "Greenhouse added",
                                Toast.LENGTH_SHORT).show();

                        etName.setText("");
                        etCategory.setText("");

                    } else {

                        Toast.makeText(this,
                                "Failed to save",
                                Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
}