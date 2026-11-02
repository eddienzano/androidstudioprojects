package com.unitedselections.admin;

import android.os.Bundle;
import android.util.Log;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.unitedselections.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class EditGreenhouseActivity extends AppCompatActivity {

    Spinner spGreenhouse;
    EditText etName, etCategory;
    Button btnUpdate;

    ArrayList<GreenhouseModel> list = new ArrayList<>();
    ArrayAdapter<GreenhouseModel> adapter;

    String LOAD_URL =
            "https://unitedselections.supplybridge.co.ke/api/get_greenhouses.php";

    String UPDATE_URL =
            "https://unitedselections.supplybridge.co.ke/api/update_greenhouse.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_greenhouse);

        spGreenhouse = findViewById(R.id.spGreenhouse);
        etName = findViewById(R.id.etName);
        etCategory = findViewById(R.id.etCategory);
        btnUpdate = findViewById(R.id.btnUpdate);

        loadGreenhouses();

        spGreenhouse.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {

                        GreenhouseModel g = list.get(position);

                        etName.setText(g.getName());
                        etCategory.setText(g.getCategory());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                }
        );

        btnUpdate.setOnClickListener(v -> updateGreenhouse());
    }

    private void loadGreenhouses() {

        new Thread(() -> {

            HttpURLConnection conn = null;

            try {

                URL url = new URL("https://unitedselections.supplybridge.co.ke/api/get_greenhouses.php");
                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                int responseCode = conn.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("HTTP Error: " + responseCode);
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                reader.close();

                String response = sb.toString().trim();
                Log.d("GREENHOUSE_API", response);

                JSONObject obj = new JSONObject(response);

// 👇 IMPORTANT FIX HERE
                JSONArray arr = obj.getJSONArray("data");

                list.clear();

                for (int i = 0; i < arr.length(); i++) {

                    JSONObject o = arr.getJSONObject(i);

                    list.add(new GreenhouseModel(
                            o.getInt("id"),
                            o.getString("greenhouse_name"),
                            o.getString("category_code")
                    ));
                }

                runOnUiThread(() -> {

                    adapter = new ArrayAdapter<>(
                            EditGreenhouseActivity.this,
                            android.R.layout.simple_spinner_dropdown_item,
                            list
                    );

                    spGreenhouse.setAdapter(adapter);

                    if (!list.isEmpty()) {
                        spGreenhouse.setSelection(0);
                    }
                });

            } catch (Exception e) {

                Log.e("GREENHOUSE_ERROR", e.getMessage(), e);

                runOnUiThread(() ->
                        Toast.makeText(
                                EditGreenhouseActivity.this,
                                "Failed to load greenhouses: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );

            } finally {
                if (conn != null) conn.disconnect();
            }

        }).start();
    }

    private void updateGreenhouse() {

        GreenhouseModel g =
                (GreenhouseModel) spGreenhouse.getSelectedItem();

        if (g == null) {
            Toast.makeText(this, "No greenhouse selected", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etName.getText().toString().trim();
        String category = etCategory.getText().toString().trim();

        if (name.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Name and category required", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {

            HttpURLConnection conn = null;

            try {

                URL url = new URL(UPDATE_URL);
                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                String postData =
                        "id=" + URLEncoder.encode(String.valueOf(g.getId()), "UTF-8") +
                                "&name=" + URLEncoder.encode(name, "UTF-8") +
                                "&category=" + URLEncoder.encode(category, "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                String res = response.toString().trim();

                runOnUiThread(() -> {

                    try {
                        JSONObject obj = new JSONObject(res);
                        String status = obj.optString("status");
                        String message = obj.optString("message", "Done");

                        if ("success".equalsIgnoreCase(status)) {

                            Toast.makeText(this,
                                    "Updated successfully",
                                    Toast.LENGTH_SHORT).show();

                        } else {

                            Toast.makeText(this,
                                    message,
                                    Toast.LENGTH_LONG).show();
                        }

                    } catch (Exception e) {

                        Toast.makeText(this,
                                "Invalid server response",
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );

            } finally {

                if (conn != null) {
                    conn.disconnect();
                }
            }

        }).start();
    }
}