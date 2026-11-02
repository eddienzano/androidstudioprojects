package com.unitedselections.intake;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.unitedselections.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import java.net.URLEncoder;
import java.io.OutputStream;

public class IntakeScanActivity extends AppCompatActivity {

    EditText qrInput;
    Button btnScanQR, btnSubmit;
    TextView statusText;

    RecyclerView recycler;
    IntakeAdapter adapter;

    List<JSONObject> intakeList = new ArrayList<>();

    String currentSerial = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intakescan);

        qrInput = findViewById(R.id.qrInput);
        btnScanQR = findViewById(R.id.btnScanQR);
        btnSubmit = findViewById(R.id.btnSubmit);
        statusText = findViewById(R.id.statusText);
        recycler = findViewById(R.id.recycler);

        adapter = new IntakeAdapter(intakeList);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        btnScanQR.setOnClickListener(v -> scanQR());
        btnSubmit.setOnClickListener(v -> submitIntake());
    }

    private void scanQR() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan QR Code");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null && result.getContents() != null) {

            String qrRaw = result.getContents();
            String serial = "";

            try {
                String[] parts = qrRaw.split("\\|");

                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("UNSE") && part.length() == 9) {
                        serial = part;
                    }
                }

                if (!serial.isEmpty()) {
                    currentSerial = serial;
                    qrInput.setText(serial);
                    fetchBatchData(serial);
                } else {
                    Toast.makeText(this, "Invalid QR", Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                Toast.makeText(this, "QR Error", Toast.LENGTH_SHORT).show();
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void showStatus(String status, String message) {

        runOnUiThread(() -> {

            statusText.setText(message);

            switch (status) {

                case "not_found":
                    statusText.setTextColor(0xFFFF0000); // RED
                    break;

                case "verified":
                    statusText.setTextColor(0xFF00C853); // GREEN
                    break;

                case "success":
                    statusText.setTextColor(0xFFFFC107); // YELLOW
                    break;

                default:
                    statusText.setTextColor(0xFF000000); // default
                    break;
            }
        });
    }

    // ================= FETCH BATCH =================
    private void fetchBatchData(String serial) {

        statusText.setText("Loading...");
        intakeList.clear();
        adapter.notifyDataSetChanged();

        new Thread(() -> {
            try {

                String API = "https://unitedselections.supplybridge.co.ke/api/harvest_scans.php?qr=" + serial;

                URL url = new URL(API);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                InputStream is = conn.getInputStream();
                Scanner sc = new Scanner(is).useDelimiter("\\A");
                String response = sc.hasNext() ? sc.next() : "";

                JSONObject json = new JSONObject(response);

                String status = json.getString("status");

                if (!status.equals("success")) {

                    String msg = json.optString("message", "No data");

                    showStatus(status, msg);
                    return;
                }

                JSONArray arr = json.getJSONArray("data");

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);

                    int qty = obj.optInt("quantity", 0);

                    obj.put("original_qty", qty);
                    obj.put("verified_qty", qty);

                    intakeList.add(obj);
                }

                runOnUiThread(() -> {
                    showStatus("success", "Not verified - Loaded: " + intakeList.size());
                    adapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    // ================= SUBMIT =================
    private void submitIntake() {

        if (currentSerial.isEmpty()) {
            Toast.makeText(this, "Scan QR first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (intakeList.isEmpty()) {
            Toast.makeText(this, "No data", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;

            try {

                JSONArray arr = new JSONArray(intakeList);

                String API = "https://unitedselections.supplybridge.co.ke/api/scan_intake.php";
                URL url = new URL(API);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                // ✅ ENCODE DATA PROPERLY
                String postData =
                        "qr=" + URLEncoder.encode(currentSerial, "UTF-8") +
                                "&user_id=" + URLEncoder.encode("1", "UTF-8") +
                                "&items_json=" + URLEncoder.encode(arr.toString(), "UTF-8");

                // ✅ SEND DATA
                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                // ✅ READ RESPONSE
                InputStream is = conn.getInputStream();
                Scanner sc = new Scanner(is).useDelimiter("\\A");
                String response = sc.hasNext() ? sc.next() : "";
                sc.close();
                is.close();

                JSONObject json = new JSONObject(response);

                runOnUiThread(() -> {
                    if ("success".equalsIgnoreCase(json.optString("status"))) {

                        Toast.makeText(this, "✅ Saved", Toast.LENGTH_LONG).show();

                        intakeList.clear();
                        adapter.notifyDataSetChanged();
                        statusText.setText("Done");

                    } else {

                        Toast.makeText(this,
                                json.optString("message", "Error occurred"),
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
                if (conn != null) conn.disconnect();
            }

        }).start();
    }
}