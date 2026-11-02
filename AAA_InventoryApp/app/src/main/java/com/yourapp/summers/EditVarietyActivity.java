package com.yourapp.summers;


import android.annotation.SuppressLint;
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
import com.yourapp.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.*;

public class EditVarietyActivity extends AppCompatActivity {

    TextView serial,bucket,farm,length,statusText;

    EditText qrInput;

    Button scanBtn;
    Button saveBtn;

    RecyclerView recycler;

    List<JSONObject> intakeList = new ArrayList<>();

    EditVarietyAdapter adapter;

    String BASE_URL =
            "https://www.aaagrowers.co.ke/inventory/summers/api/";

    String currentSerial = "";
    String currentBatchId = "";

    private final OkHttpClient client = new OkHttpClient();

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private boolean processing = false;

    private final List<String> varieties =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_varieties);

        serial = findViewById(R.id.serial);
        bucket = findViewById(R.id.bucket);
        farm = findViewById(R.id.farm);
        length = findViewById(R.id.length);
        statusText = findViewById(R.id.statusText);

        qrInput = findViewById(R.id.qr_code);

        scanBtn = findViewById(R.id.scanBtn);
        saveBtn = findViewById(R.id.saveBtn);

        recycler = findViewById(R.id.recycler);

        adapter = new EditVarietyAdapter(
                this,
                intakeList,
                varieties
        );

        recycler.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recycler.setAdapter(adapter);

        scanBtn.setOnClickListener(v -> startScanner());

        saveBtn.setOnClickListener(v -> saveChanges());

        loadVarieties();

        qrInput.addTextChangedListener(
                new android.text.TextWatcher() {

                    Runnable runnable;

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {}

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        handler.removeCallbacksAndMessages(null);

                        runnable = () -> {

                            String qr =
                                    qrInput.getText()
                                            .toString()
                                            .trim();

                            if (!qr.isEmpty()) {

                                parseQR(qr);

                                qrInput.setText("");
                            }
                        };

                        handler.postDelayed(
                                runnable,
                                300
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            android.text.Editable s) {}
                });

    }

    private final ActivityResultLauncher<ScanOptions>
            scanner =
            registerForActivityResult(
                    new ScanContract(),
                    result -> {

                        if (result.getContents() != null) {

                            parseQR(
                                    result.getContents()
                            );
                        }
                    });

    private void startScanner() {

        ScanOptions options =
                new ScanOptions();

        options.setPrompt("Scan Bucket");
        options.setBeepEnabled(true);

        scanner.launch(options);
    }

    private void parseQR(String text) {

        if (processing) return;

        processing = true;

        try {

            String serialVal="";
            String bucketVal="";
            String farmVal="";
            String lengthVal="";

            for(String p:text.split("\\|")){

                p=p.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

                if(p.startsWith("serial")){
                    serialVal=p.split(":")[1];
                }

                if(p.startsWith("bucket")){
                    bucketVal=p.split(":")[1];
                }

                if(p.startsWith("farm")){
                    farmVal=p.split(":")[1];
                }

                if(p.startsWith("length")){
                    lengthVal=p.split(":")[1];
                }
            }

            currentSerial=serialVal;

            serial.setText(
                    "Serial: "+serialVal
            );

            bucket.setText(
                    "Bucket: "+bucketVal
            );

            farm.setText(
                    "Farm: "+farmVal
            );

            length.setText(
                    "Length: "+lengthVal
            );

            fetchData();

        } catch (Exception e){

            processing=false;
        }
    }

    private void loadVarieties() {

        Log.d("VARIETY", "Loading varieties...");

        Request request = new Request.Builder()
                .url(BASE_URL + "get_varieties.php")
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

                Log.e("VARIETY", "Request failed: " + e.getMessage());

                runOnUiThread(() ->
                        Toast.makeText(
                                EditVarietyActivity.this,
                                "Failed to load varieties",
                                Toast.LENGTH_SHORT
                        ).show()
                );
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {

                String body = response.body().string();

                Log.d("VARIETY", "RAW RESPONSE: " + body);

                try {

                    JSONObject root = new JSONObject(body);

                    String status = root.optString("status");

                    Log.d("VARIETY", "STATUS: " + status);

                    JSONArray arr = root.getJSONArray("data");

                    varieties.clear();

                    for (int i = 0; i < arr.length(); i++) {

                        String v = arr.getString(i);

                        Log.d("VARIETY", "Loaded: " + v);

                        varieties.add(v);
                    }

                    Log.d("VARIETY", "TOTAL VARIETIES = " + varieties.size());

                    runOnUiThread(() -> {

                        adapter.notifyDataSetChanged();

                        Toast.makeText(
                                EditVarietyActivity.this,
                                "Loaded " + varieties.size() + " varieties",
                                Toast.LENGTH_SHORT
                        ).show();
                    });

                } catch (Exception e) {

                    Log.e("VARIETY", "PARSE ERROR: " + e.getMessage());
                    Log.e("VARIETY", "RAW WAS: " + body);

                    runOnUiThread(() ->
                            Toast.makeText(
                                    EditVarietyActivity.this,
                                    "Parse error",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );
                }
            }
        });
    }

    private void fetchData(){

        HttpUrl url =
                HttpUrl.parse(
                                BASE_URL+
                                        "get_summer_intake.php"
                        )
                        .newBuilder()
                        .addQueryParameter(
                                "serial",
                                currentSerial
                        )
                        .build();

        Request request =
                new Request.Builder()
                        .url(url)
                        .build();

        client.newCall(request)
                .enqueue(new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e) {

                        runOnUiThread(() -> {

                            statusText.setText(
                                    "Network Error"
                            );

                            processing=false;
                        });
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response)
                            throws IOException {

                        try {

                            JSONObject json =
                                    new JSONObject(
                                            response.body()
                                                    .string()
                                    );

                            currentBatchId =
                                    json.getString(
                                            "batch_id"
                                    );

                            JSONArray arr =
                                    json.getJSONArray(
                                            "data"
                                    );

                            intakeList.clear();

                            for(int i=0;
                                i<arr.length();
                                i++){

                                JSONObject obj =
                                        arr.getJSONObject(i);

                                obj.put(
                                        "new_variety",
                                        obj.optString(
                                                "variety_name"
                                        )
                                );

                                intakeList.add(obj);
                            }

                            runOnUiThread(() -> {

                                adapter.notifyDataSetChanged();

                                statusText.setText(
                                        "Loaded: "
                                                + intakeList.size()
                                );

                                processing=false;
                            });

                        }catch(Exception e){

                            runOnUiThread(() -> {

                                statusText.setText(
                                        "Parse Error"
                                );

                                processing=false;
                            });
                        }
                    }
                });
    }

    private void saveChanges(){

        JSONArray arr =
                new JSONArray(
                        intakeList
                );

        RequestBody body =
                new FormBody.Builder()
                        .add(
                                "batch_id",
                                currentBatchId
                        )
                        .add(
                                "varieties_json",
                                arr.toString()
                        )
                        .build();

        Request request =
                new Request.Builder()
                        .url(
                                BASE_URL+
                                        "edit_varieties.php"
                        )
                        .post(body)
                        .build();

        client.newCall(request)
                .enqueue(new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e) {

                        runOnUiThread(() ->
                                Toast.makeText(
                                        EditVarietyActivity.this,
                                        "Network Error",
                                        Toast.LENGTH_LONG
                                ).show()
                        );
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response)
                            throws IOException {

                        runOnUiThread(() ->
                                Toast.makeText(
                                        EditVarietyActivity.this,
                                        "Varieties Updated",
                                        Toast.LENGTH_LONG
                                ).show()
                        );
                    }
                });
    }
}
