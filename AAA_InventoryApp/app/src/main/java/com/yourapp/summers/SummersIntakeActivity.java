package com.yourapp.summers;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
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

public class SummersIntakeActivity extends AppCompatActivity {

    private String currentSerial = "";
    private String currentBucket = "";
    private String currentFarm = "";
    private String currentLength = "";

    TextView serial, bucket, farm, length, statusText;

    EditText qrInput;
    AutoCompleteTextView varietyInput;
    EditText quantityInput;

    Button scanBtn, submitBtn, addVarietyBtn;

    RecyclerView varietyList;

    boolean isBucketBlocked = false;

    List<JSONObject> selectedVarieties = new ArrayList<>();
    VarietyAdapter adapter;

    String BASE_URL = "https://www.aaagrowers.co.ke/inventory/summers/api/";

    private final OkHttpClient client = new OkHttpClient();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final long REQUEST_DELAY = 800;

    private long lastRequestTime = 0;

    private boolean isProcessingQR = false;

    private Call activeCall;

    // ================= VARIETY CONTROL =================

    private boolean varietySelected = false;

    private int selectedVarietyId = 0;

    private final List<String> varietyNames = new ArrayList<>();
    private final List<Integer> varietyIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summers_intake);

        qrInput = findViewById(R.id.qr_code);

        serial = findViewById(R.id.serial);
        bucket = findViewById(R.id.bucket);
        farm = findViewById(R.id.farm);
        length = findViewById(R.id.length);

        statusText = findViewById(R.id.statusText);

        varietyInput = findViewById(R.id.varietyInput);
        quantityInput = findViewById(R.id.quantityInput);

        scanBtn = findViewById(R.id.scanBtn);
        submitBtn = findViewById(R.id.submitBtn);
        addVarietyBtn = findViewById(R.id.addVarietyBtn);

        varietyList = findViewById(R.id.varietyList);

        adapter = new VarietyAdapter(selectedVarieties);

        varietyList.setLayoutManager(new LinearLayoutManager(this));
        varietyList.setAdapter(adapter);

        scanBtn.setOnClickListener(v -> startScanner());

        addVarietyBtn.setOnClickListener(v -> addVariety());

        submitBtn.setOnClickListener(v -> submitData());

        // ================= HONEYWELL LISTENER =================

        qrInput.addTextChangedListener(new android.text.TextWatcher() {

            private Runnable runnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                handler.removeCallbacks(runnable);

                runnable = () -> {

                    String scanned = qrInput.getText().toString().trim();

                    if (!scanned.isEmpty()) {

                        parseQR(scanned);

                        qrInput.setText("");
                    }
                };

                handler.postDelayed(runnable, 250);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        qrInput.setOnEditorActionListener((v, actionId, event) -> {

            String input = qrInput.getText().toString().trim();

            if (!input.isEmpty()) {

                parseQR(input);

                qrInput.setText("");
            }

            return true;
        });

        setupVarietyAutocomplete();

        qrInput.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        qrInput.requestFocus();
    }

    // ================= CAMERA SCANNER =================

    private final ActivityResultLauncher<ScanOptions> scanner =
            registerForActivityResult(new ScanContract(), result -> {

                if (result.getContents() != null) {

                    parseQR(result.getContents());
                }
            });

    private void startScanner() {

        ScanOptions options = new ScanOptions();

        options.setPrompt("Scan Summer QR");

        scanner.launch(options);
    }

    // ================= QR PARSER =================

    private void parseQR(String text) {

        if (isProcessingQR) return;

        long now = System.currentTimeMillis();

        if (now - lastRequestTime < REQUEST_DELAY) return;

        isProcessingQR = true;

        lastRequestTime = now;

        try {

            clearBucketUI();

            text = text.replace("\u0000", "");
            text = text.replaceAll("[^\\x20-\\x7E|:]", "");
            text = text.replaceAll("[\\r\\n]", "").trim();

            String serialVal = "";
            String bucketVal = "";
            String farmVal = "";
            String lengthVal = "";

            for (String p : text.split("\\|")) {

                p = p.trim().toLowerCase(Locale.ROOT);

                if (p.startsWith("serial")) {

                    String[] sp = p.split(":");

                    if (sp.length > 1) serialVal = sp[1].trim();
                }

                if (p.startsWith("bucket")) {

                    String[] sp = p.split(":");

                    if (sp.length > 1) bucketVal = sp[1].trim();
                }

                if (p.startsWith("farm")) {

                    String[] sp = p.split(":");

                    if (sp.length > 1) farmVal = sp[1].trim();
                }

                if (p.startsWith("length")) {

                    String[] sp = p.split(":");

                    if (sp.length > 1) lengthVal = sp[1].trim();
                }
            }

            if (serialVal.isEmpty()) {

                Toast.makeText(this,
                        "Invalid QR",
                        Toast.LENGTH_SHORT).show();

                isProcessingQR = false;

                return;
            }

            currentSerial = serialVal;
            currentBucket = bucketVal;
            currentFarm = farmVal;
            currentLength = lengthVal;

            serial.setText("Serial: " + serialVal);
            bucket.setText("Bucket: " + bucketVal);
            farm.setText("Farm: " + farmVal);
            length.setText("Length: " + lengthVal + " CM");

            checkBucketStatus(serialVal);

        } catch (Exception e) {

            isProcessingQR = false;

            Toast.makeText(this,
                    "QR Parse Error",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ================= VARIETY AUTOCOMPLETE =================

    private long lastSearchTime = 0;

    private String lastQuery = "";

    private void setupVarietyAutocomplete() {

        // Search immediately from first character
        varietyInput.setThreshold(1);

        varietyInput.setOnItemClickListener((parent, view, position, id) -> {

            selectedVarietyId = varietyIds.get(position);

            varietySelected = true;
        });

        varietyInput.addTextChangedListener(new android.text.TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {

                varietySelected = false;

                String term = s.toString().trim();

                if (term.isEmpty()) {
                    return;
                }

                long now = System.currentTimeMillis();

                // small delay to avoid hammering server
                if (now - lastSearchTime < 150) {
                    return;
                }

                if (term.equalsIgnoreCase(lastQuery)) {
                    return;
                }

                lastQuery = term;
                lastSearchTime = now;

                try {

                    String url = BASE_URL +
                            "search_summer_variety.php?term=" +
                            java.net.URLEncoder.encode(term, "UTF-8");

                    // cancel previous search request
                    if (activeCall != null && !activeCall.isCanceled()) {
                        activeCall.cancel();
                    }

                    makeGetRequest(url, new SimpleCallback() {

                        @Override
                        public void onSuccess(String response) {

                            try {

                                JSONArray arr = new JSONArray(response);

                                varietyNames.clear();
                                varietyIds.clear();

                                for (int i = 0; i < arr.length(); i++) {

                                    JSONObject obj = arr.getJSONObject(i);

                                    varietyNames.add(
                                            obj.optString("Variety")
                                    );

                                    varietyIds.add(
                                            obj.optInt("VarietyId")
                                    );
                                }

                                runOnUiThread(() -> {

                                    ArrayAdapter<String> adapter =
                                            new ArrayAdapter<>(
                                                    SummersIntakeActivity.this,
                                                    android.R.layout.simple_dropdown_item_1line,
                                                    varietyNames
                                            );

                                    varietyInput.setAdapter(adapter);

                                    if (!varietyNames.isEmpty()) {
                                        varietyInput.showDropDown();
                                    }
                                });

                            } catch (Exception e) {

                                Log.e("VARIETY_SEARCH",
                                        "Parse Error: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onFail(String error) {

                            Log.e("VARIETY_SEARCH",
                                    "Search Error: " + error);
                        }
                    });

                } catch (Exception e) {

                    Log.e("VARIETY_SEARCH",
                            "URL Error: " + e.getMessage());
                }
            }
        });
    }

    // ================= HTTP =================

    private void makeGetRequest(String url, SimpleCallback callback) {

        Request request = new Request.Builder()
                .url(url)
                .build();

        activeCall = client.newCall(request);

        activeCall.enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call,
                                   @NonNull Response response) throws IOException {

                callback.onSuccess(response.body().string());
            }
        });
    }

    interface SimpleCallback {
        void onSuccess(String response);
        void onFail(String error);
    }

    // ================= BUCKET STATUS =================

    private void checkBucketStatus(String serialVal) {

        String url = BASE_URL + "check_bucket_status.php?serial=" + serialVal;

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call,
                                  @NonNull IOException e) {

                runOnUiThread(() -> {

                    isProcessingQR = false;

                    statusText.setText("❌ Network Error");

                    statusText.setTextColor(Color.RED);
                });
            }

            @Override
            public void onResponse(@NonNull Call call,
                                   @NonNull Response response) throws IOException {

                String body = response.body().string();

                try {

                    JSONObject json = new JSONObject(body);

                    String status = json.optString("status");

                    runOnUiThread(() -> {

                        isProcessingQR = false;

                        if ("blocked".equals(status)) {

                            isBucketBlocked = true;

                            statusText.setText("❌ Bucket In Use");

                            statusText.setTextColor(Color.RED);

                            submitBtn.setEnabled(false);

                            Toast.makeText(SummersIntakeActivity.this,
                                    "Bucket already in use",
                                    Toast.LENGTH_LONG).show();

                        } else {

                            isBucketBlocked = false;

                            statusText.setText("✅ Ready");

                            statusText.setTextColor(Color.GREEN);

                            submitBtn.setEnabled(true);
                        }
                    });

                } catch (Exception e) {

                    runOnUiThread(() -> {

                        isProcessingQR = false;

                        statusText.setText("❌ Error");

                        statusText.setTextColor(Color.RED);
                    });
                }
            }
        });
    }

    // ================= CLEAR =================

    private void clearBucketUI() {

        serial.setText("Serial:");
        bucket.setText("Bucket:");
        farm.setText("Farm:");
        length.setText("Length:");

        currentSerial = "";
        currentBucket = "";
        currentFarm = "";
        currentLength = "";

        selectedVarieties.clear();

        adapter.notifyDataSetChanged();
    }

    // ================= ADD VARIETY =================

    private void addVariety() {

        if (isBucketBlocked) {

            Toast.makeText(this,
                    "Bucket blocked",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        if (!varietySelected || selectedVarietyId <= 0) {

            Toast.makeText(this,
                    "Select variety from dropdown",
                    Toast.LENGTH_LONG).show();

            return;
        }

        String variety = varietyInput.getText().toString().trim();

        String qty = quantityInput.getText().toString().trim();

        if (TextUtils.isEmpty(qty)) {

            Toast.makeText(this,
                    "Enter quantity",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        try {

            JSONObject obj = new JSONObject();

            obj.put("variety_id", selectedVarietyId);

            obj.put("variety_name", variety);

            obj.put("quantity", qty);

            selectedVarieties.add(obj);

            adapter.notifyDataSetChanged();

            varietyInput.setText("");

            quantityInput.setText("");

            selectedVarietyId = 0;

            varietySelected = false;

        } catch (Exception ignored) {}
    }

    // ================= SUBMIT =================

    private void submitData() {

        if (selectedVarieties.isEmpty()) {

            Toast.makeText(this,
                    "Add varieties first",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        String cleanSerial = serial.getText()
                .toString()
                .replace("Serial:", "")
                .trim();

        JSONArray arr = new JSONArray(selectedVarieties);

//        RequestBody body = new FormBody.Builder()
//                .add("serial", cleanSerial)
//                .add("user_id", "1")
//                .add("varieties_json", arr.toString())
//                .build();

        RequestBody body = new FormBody.Builder()
                .add("serial", currentSerial)
                .add("bucket_name", currentBucket)
                .add("farm", currentFarm)
                .add("length", currentLength)
                .add("user_id", "1")
                .add("varieties_json", arr.toString())
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "summers_intake_process.php")
                .post(body)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(@NonNull Call call,
                                  @NonNull IOException e) {

                runOnUiThread(() ->
                        Toast.makeText(SummersIntakeActivity.this,
                                "Network Failed",
                                Toast.LENGTH_LONG).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call,
                                   @NonNull Response response) throws IOException {

                String res = response.body().string();

                runOnUiThread(() -> {

                    try {

                        JSONObject json = new JSONObject(res);

                        String status = json.optString("status");

                        String message = json.optString("message");

                        Toast.makeText(SummersIntakeActivity.this,
                                message,
                                Toast.LENGTH_LONG).show();

                        if ("success".equals(status)) {

                            selectedVarieties.clear();

                            adapter.notifyDataSetChanged();

                            clearBucketUI();
                        }

                    } catch (Exception e) {

                        Toast.makeText(SummersIntakeActivity.this,
                                "Invalid Response",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}