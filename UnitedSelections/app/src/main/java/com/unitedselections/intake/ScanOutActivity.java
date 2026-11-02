package com.unitedselections.intake;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.journeyapps.barcodescanner.*;
import com.unitedselections.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

import okhttp3.*;

public class ScanOutActivity extends AppCompatActivity {

    TextView serialText, statusText;
    Button scanBtn, confirmBtn, submitPartialBtn;
    RecyclerView recycler;
    EditText qrInput;

    Spinner spDestination;
    AutoCompleteTextView etTarget;

    String BASE_URL = "https://unitedselections.supplybridge.co.ke/api/";

    String currentQR = "";        // FULL QR
    String serialQR = "";         // ONLY SERIAL (FIX)

    OkHttpClient client = new OkHttpClient();

    List<ScanItem> itemList = new ArrayList<>();
    ScanAdapter adapter;

    int userId = 1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isProcessingQR = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_out);

        serialText = findViewById(R.id.serial);
        statusText = findViewById(R.id.statusText);
        scanBtn = findViewById(R.id.scanBtn);
        confirmBtn = findViewById(R.id.confirmBtn);
        submitPartialBtn = findViewById(R.id.submitPartialBtn);
        recycler = findViewById(R.id.itemsRecycler);
        qrInput = findViewById(R.id.qrInput);

        spDestination = findViewById(R.id.spDestination);
        etTarget = findViewById(R.id.etTarget);

        setupSpinner();

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScanAdapter(itemList);
        recycler.setAdapter(adapter);

        scanBtn.setOnClickListener(v -> startScanner());
        confirmBtn.setOnClickListener(v -> fullScanOut());
        submitPartialBtn.setOnClickListener(v -> submitPartial());

        confirmBtn.setEnabled(false);
        submitPartialBtn.setVisibility(android.view.View.GONE);
    }

    /* ================= SCAN ================= */

    private final ActivityResultLauncher<ScanOptions> scanner =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    handleScan(result.getContents());
                }
            });

    private void startScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan QR");
        options.setBeepEnabled(true);
        scanner.launch(options);
    }

    /* ================= FIX: PARSE QR ================= */

    private void handleScan(String text) {

        if (isProcessingQR) return;

        isProcessingQR = true;

        currentQR = text.trim();

        String[] parts = currentQR.split("\\|");

        if (parts.length < 4) {
            statusText.setText("Invalid QR format");
            isProcessingQR = false;
            return;
        }

        serialQR = parts[0]; // ✅ ONLY SERIAL

        String bucket = parts[1];
        String farm = parts[2];
        String length = parts[3];

        Log.d("QR_DEBUG", "FULL QR: " + currentQR);
        Log.d("QR_DEBUG", "SERIAL: " + serialQR);
        Log.d("QR_DEBUG", "BUCKET: " + bucket);
        Log.d("QR_DEBUG", "FARM: " + farm);
        Log.d("QR_DEBUG", "LENGTH: " + length);

        serialText.setText("Serial: " + serialQR);

        loadItems();
    }

    /* ================= LOAD ITEMS ================= */

    private void loadItems() {

        String url = BASE_URL + "get_qr_items.php?qr_code=" + serialQR;

        Log.d("API_DEBUG", "REQUEST URL: " + url);

        client.newCall(new Request.Builder().url(url).build())
                .enqueue(new Callback() {

                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> statusText.setText("Network error"));
                        isProcessingQR = false;
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                        try {
                            String resBody = response.body().string();

                            Log.d("API_DEBUG", "RESPONSE: " + resBody);

                            JSONObject obj = new JSONObject(resBody);
                            String status = obj.optString("status");

                            if (!"success".equals(status)) {
                                runOnUiThread(() -> statusText.setText(obj.optString("message")));
                                isProcessingQR = false;
                                return;
                            }

                            JSONArray arr = obj.getJSONArray("items");

                            itemList.clear();

                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.getJSONObject(i);

                                itemList.add(new ScanItem(
                                        o.getInt("intake_id"),
                                        o.getString("variety"),
                                        o.getInt("balance")
                                ));
                            }

                            runOnUiThread(() -> {
                                adapter.notifyDataSetChanged();
                                confirmBtn.setEnabled(true);
                                submitPartialBtn.setVisibility(android.view.View.VISIBLE);
                                statusText.setText("Ready");
                                isProcessingQR = false;
                            });

                        } catch (Exception e) {
                            Log.e("API_DEBUG", "PARSE ERROR", e);
                            runOnUiThread(() -> statusText.setText("Parse error"));
                            isProcessingQR = false;
                        }
                    }
                });
    }

    /* ================= PARTIAL ================= */

    private void submitPartial() {

        JSONArray arr = new JSONArray();

        for (ScanItem i : itemList) {
            if (i.enteredQty > 0 && i.enteredQty <= i.maxQty) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("intake_id", i.intakeId);
                    o.put("qty", i.enteredQty);
                    arr.put(o);
                } catch (Exception ignored) {}
            }
        }

        if (arr.length() == 0) {
            Toast.makeText(this, "Enter quantities", Toast.LENGTH_SHORT).show();
            return;
        }

        send(arr);
    }

    /* ================= FULL ================= */

    private void fullScanOut() {

        JSONArray arr = new JSONArray();

        for (ScanItem i : itemList) {
            try {
                JSONObject o = new JSONObject();
                o.put("intake_id", i.intakeId);
                o.put("qty", i.maxQty);
                arr.put(o);
            } catch (Exception ignored) {}
        }

        send(arr);
    }

    /* ================= SEND ================= */

    private void send(JSONArray items) {

        String destination = spDestination.getSelectedItem().toString();
        String target = etTarget.getText().toString();

        RequestBody body = new FormBody.Builder()
                .add("qr_code", serialQR) // ✅ IMPORTANT FIX
                .add("user_id", String.valueOf(userId))
                .add("destination", destination)
                .add("target_name", target)
                .add("items", items.toString())
                .build();

        client.newCall(new Request.Builder()
                        .url(BASE_URL + "scan_out.php")
                        .post(body)
                        .build())
                .enqueue(new Callback() {

                    @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                        String resBody = response.body().string();

                        Log.d("SCAN_OUT_RESPONSE", resBody);

                        runOnUiThread(() -> {

                            try {

                                JSONObject obj = new JSONObject(resBody);

                                String status = obj.optString("status");
                                String message = obj.optString("message");

                                Toast.makeText(
                                        ScanOutActivity.this,
                                        message,
                                        Toast.LENGTH_LONG
                                ).show();

                                if(status.equals("success")){
                                    reset();
                                }

                            } catch (Exception e) {

                                Toast.makeText(
                                        ScanOutActivity.this,
                                        "Invalid server response",
                                        Toast.LENGTH_LONG
                                ).show();

                                Log.e("SCAN_OUT_PARSE", "Parse error", e);
                            }
                        });
                    }
                });
    }

    private void reset() {
        itemList.clear();
        adapter.notifyDataSetChanged();
        confirmBtn.setEnabled(false);
        submitPartialBtn.setVisibility(android.view.View.GONE);
        statusText.setText("Scan next");
        serialText.setText("QR:");
        currentQR = "";
        serialQR = "";
    }

    private void setupSpinner() {
        String[] d = {"SALES","TRIAL","PROPAGATION","DISCARD", "BUNCHES"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, d);

        spDestination.setAdapter(adapter);
    }
}