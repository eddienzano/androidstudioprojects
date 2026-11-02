package com.yourapp.summers;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.journeyapps.barcodescanner.*;
import com.yourapp.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

import okhttp3.*;

public class QuarantineManagementActivity extends AppCompatActivity {

    TextView serial, batch, statusText;
    Button scanBtn, btnRelease, btnDiscard, btnReturn;
    EditText qrInput;

    RecyclerView recycler;

    List<JSONObject> list = new ArrayList<>();
    VerifyAdapter adapter;

    String BASE_URL = "https://www.aaagrowers.co.ke/inventory/summers/api/";

    String currentSerial = "";
    String currentBatchId = "";

    OkHttpClient client = new OkHttpClient();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastScan = 0;
    private static final long DELAY = 800;
    private boolean processing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quarantine_management);

        serial = findViewById(R.id.serial);
        batch = findViewById(R.id.batch);
        statusText = findViewById(R.id.statusText);

        scanBtn = findViewById(R.id.scanBtn);
        btnRelease = findViewById(R.id.btnRelease);
        btnDiscard = findViewById(R.id.btnDiscard);
        btnReturn = findViewById(R.id.btnReturn);

        qrInput = findViewById(R.id.qr_code);

        recycler = findViewById(R.id.recycler);
        adapter = new VerifyAdapter(list);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        scanBtn.setOnClickListener(v -> startScanner());

        qrInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {

                handler.removeCallbacksAndMessages(null);

                handler.postDelayed(() -> {
                    String qr = qrInput.getText().toString().trim();
                    if (!qr.isEmpty()) {
                        parseQR(qr);
                        qrInput.setText("");
                    }
                }, 300);
            }

            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        btnRelease.setOnClickListener(v -> sendAction("release"));
        btnDiscard.setOnClickListener(v -> sendAction("discard"));
        btnReturn.setOnClickListener(v -> sendAction("return"));
    }

    private final ActivityResultLauncher<ScanOptions> scanner =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    parseQR(result.getContents());
                }
            });

    private void startScanner() {
        ScanOptions opt = new ScanOptions();
        opt.setPrompt("Scan QR");
        opt.setBeepEnabled(true);
        opt.setOrientationLocked(true);
        opt.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        scanner.launch(opt);
    }

    private void parseQR(String text) {

        long now = System.currentTimeMillis();
        if (processing || now - lastScan < DELAY) return;

        processing = true;
        lastScan = now;

        try {

            String serialVal = "";

            for (String p : text.split("\\|")) {
                if (p.toLowerCase().startsWith("serial")) {
                    String[] sp = p.split(":");
                    if (sp.length > 1) serialVal = sp[1].trim();
                }
            }

            if (serialVal.isEmpty()) {
                processing = false;
                return;
            }

            currentSerial = serialVal;

            serial.setText("Serial: " + serialVal);

            fetchQuarantine();

        } catch (Exception e) {
            processing = false;
        }
    }

    private void fetchQuarantine() {

        HttpUrl url = HttpUrl.parse(BASE_URL + "get_quarantine.php")
                .newBuilder()
                .addQueryParameter("serial", currentSerial)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call,
                                  @NonNull IOException e) {

                runOnUiThread(() -> {
                    statusText.setText("Network Error");
                    processing = false;
                });
            }

            @Override
            public void onResponse(@NonNull Call call,
                                   @NonNull Response response)
                    throws IOException {

                String body = response.body().string();

                try {

                    JSONObject json = new JSONObject(body);

                    if (!json.optString("status")
                            .equalsIgnoreCase("success")) {

                        runOnUiThread(() -> {
                            statusText.setText(
                                    json.optString("message")
                            );

                            list.clear();
                            adapter.notifyDataSetChanged();

                            processing = false;
                        });

                        return;
                    }

                    currentBatchId = json.optString("batch_id");

                    JSONArray arr = json.getJSONArray("data");

                    list.clear();

                    for (int i = 0; i < arr.length(); i++) {

                        JSONObject obj = arr.getJSONObject(i);

                        int qty = obj.optInt("quantity", 0);

                        obj.put("original_qty", qty);
                        obj.put("verified_qty", qty);

                        list.add(obj);
                    }

                    runOnUiThread(() -> {

                        adapter.notifyDataSetChanged();

                        statusText.setText(
                                "Loaded: " + list.size() + " varieties"
                        );

                        batch.setText(
                                "Batch: " + currentBatchId
                        );

                        processing = false;
                    });

                } catch (Exception e) {

                    runOnUiThread(() -> {
                        statusText.setText("Parse Error");
                        processing = false;
                    });
                }
            }
        });
    }

    private void sendAction(String action) {

        RequestBody body = new FormBody.Builder()
                .add("serial", currentSerial)
                .add("batch_id", currentBatchId)
                .add("action", action)
                .add("user_id", "1")
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "quarantine_action.php")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call,
                                  @NonNull IOException e) {

                runOnUiThread(() ->
                        Toast.makeText(
                                QuarantineManagementActivity.this,
                                "Network Error",
                                Toast.LENGTH_LONG
                        ).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call,
                                   @NonNull Response response)
                    throws IOException {

                String res = response.body().string();

                runOnUiThread(() -> {

                    Toast.makeText(
                            QuarantineManagementActivity.this,
                            res,
                            Toast.LENGTH_LONG
                    ).show();

                    statusText.setText(res);
                });
            }
        });
    }
}