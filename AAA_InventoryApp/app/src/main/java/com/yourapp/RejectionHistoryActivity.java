package com.yourapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

import okhttp3.*;

public class RejectionHistoryActivity extends AppCompatActivity {

    RecyclerView recycler;
    Button btnPickDate;

    Spinner spinnerFarm;
    Spinner spinnerReason;
    AutoCompleteTextView autoVariety;

    OkHttpClient client = new OkHttpClient();

    List<RejectionHistoryItem> items = new ArrayList<>();
    RejectionHistoryAdapter adapter;

    HashMap<String, Integer> farmMap = new HashMap<>();
    HashMap<String, Integer> reasonMap = new HashMap<>();

    int selectedFarmId = 0;
    int selectedReasonId = 0;

    private final String BASE_URL =
            "https://www.aaagrowers.co.ke/inventory/";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        setContentView(R.layout.activity_rejection_history);

        recycler = findViewById(R.id.recyclerRejections);
        btnPickDate = findViewById(R.id.btnPickDate);

        spinnerFarm = findViewById(R.id.spinnerFarm);
        spinnerReason = findViewById(R.id.spinnerReason);
        autoVariety = findViewById(R.id.autoVariety);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RejectionHistoryAdapter(this, items);
        recycler.setAdapter(adapter);

        btnPickDate.setOnClickListener(v -> pickDate());

        loadFarms();
        loadRejectionReasons();
    }

    // =========================
    // REASONS
    // =========================
    private void loadRejectionReasons() {

        Request request = new Request.Builder()
                .url(BASE_URL + "rejections/api/get_rejection_reasons.php")
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("REASON", "Failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                try {

                    JSONArray arr = new JSONArray(response.body().string());

                    List<String> reasons = new ArrayList<>();
                    reasonMap.clear();

                    for (int i = 0; i < arr.length(); i++) {

                        JSONObject o = arr.getJSONObject(i);

                        String category = o.getString("category");
                        int id = o.getInt("rejection_id");

                        reasons.add(category);
                        reasonMap.put(category, id);
                    }

                    runOnUiThread(() -> {

                        ArrayAdapter<String> ad =
                                new ArrayAdapter<>(
                                        RejectionHistoryActivity.this,
                                        android.R.layout.simple_spinner_item,
                                        reasons
                                );

                        ad.setDropDownViewResource(
                                android.R.layout.simple_spinner_dropdown_item
                        );

                        spinnerReason.setAdapter(ad);

                        spinnerReason.setOnItemSelectedListener(
                                new AdapterView.OnItemSelectedListener() {

                                    @Override
                                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                                        String selected = parent.getItemAtPosition(position).toString();
                                        selectedReasonId = reasonMap.get(selected);

                                        Log.d("REASON", "Selected ID = " + selectedReasonId);
                                    }

                                    @Override
                                    public void onNothingSelected(AdapterView<?> parent) {}
                                }
                        );
                    });

                } catch (Exception e) {
                    Log.e("REASON_PARSE", "Error", e);
                }
            }
        });
    }

    // =========================
    // FARMS
    // =========================
    private void loadFarms() {

        Request request =
                new Request.Builder()
                        .url(BASE_URL + "get_farms.php")
                        .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(
                    Call call,
                    Response response
            ) throws IOException {

                if(!response.isSuccessful())
                    return;

                try {

                    JSONArray arr =
                            new JSONArray(
                                    response.body().string()
                            );

                    List<String> farms =
                            new ArrayList<>();

                    farmMap.clear();

                    for(int i=0;i<arr.length();i++){

                        JSONObject o =
                                arr.getJSONObject(i);

                        farmMap.put(
                                o.getString("name"),
                                o.getInt("id")
                        );

                        farms.add(
                                o.getString("name")
                        );
                    }

                    runOnUiThread(() -> {

                        ArrayAdapter<String> ad =
                                new ArrayAdapter<>(
                                        RejectionHistoryActivity.this,
                                        android.R.layout.simple_spinner_item,
                                        farms
                                );

                        ad.setDropDownViewResource(
                                android.R.layout.simple_spinner_dropdown_item
                        );

                        spinnerFarm.setAdapter(ad);

                        spinnerFarm.setOnItemSelectedListener(
                                new AdapterView.OnItemSelectedListener() {

                                    @Override
                                    public void onItemSelected(
                                            AdapterView<?> parent,
                                            View view,
                                            int position,
                                            long id
                                    ) {

                                        selectedFarmId =
                                                farmMap.get(
                                                        spinnerFarm
                                                                .getSelectedItem()
                                                                .toString()
                                                );

                                        loadVarieties(
                                                selectedFarmId
                                        );
                                    }

                                    @Override
                                    public void onNothingSelected(
                                            AdapterView<?> parent
                                    ) {

                                    }
                                });

                    });

                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    // =========================
    // VARIETIES (FIXED)
    // =========================
    private void loadVarieties(int farmId) {

        Request request = new Request.Builder()
                .url(BASE_URL + "rejections/api/get_varieties.php?farm=" + farmId)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("VARIETY", "Failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                try {

                    String json = response.body().string();

                    if (!json.trim().startsWith("[")) {
                        Log.e("VARIETY", "Invalid response: " + json);
                        return;
                    }

                    JSONArray arr = new JSONArray(json);

                    List<String> list = new ArrayList<>();

                    for (int i = 0; i < arr.length(); i++) {
                        list.add(arr.getString(i));
                    }

                    Collections.sort(list, String.CASE_INSENSITIVE_ORDER);

                    runOnUiThread(() -> {

                        ArrayAdapter<String> ad =
                                new ArrayAdapter<>(
                                        RejectionHistoryActivity.this,
                                        android.R.layout.simple_dropdown_item_1line,
                                        list
                                );

                        autoVariety.setAdapter(ad);
                        autoVariety.setThreshold(1);
                        autoVariety.setText("");
                    });

                } catch (Exception e) {
                    Log.e("VARIETY_PARSE", "Error", e);
                }
            }
        });
    }

    // =========================
    // FILTER REJECTIONS
    // =========================
    private void pickDate() {

        if (selectedFarmId == 0) {
            Toast.makeText(this, "Select farm first", Toast.LENGTH_SHORT).show();
            return;
        }

        String variety = autoVariety.getText().toString().trim();

        if (variety.isEmpty()) {
            Toast.makeText(this, "Select variety first", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar c = Calendar.getInstance();

        new DatePickerDialog(this,
                (view, y, m, d) -> {

                    String date =
                            y + "-" +
                                    String.format("%02d", m + 1) + "-" +
                                    String.format("%02d", d);

                    loadRejections(date, selectedFarmId);

                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void loadRejections(String date, int farmId) {

        try {

            String variety = autoVariety.getText().toString().trim();

            String encodedVariety = URLEncoder.encode(variety, "UTF-8");

            Request request = new Request.Builder()
                    .url(BASE_URL +
                            "api/get_rejections_by_date.php" +
                            "?date=" + date +
                            "&farm=" + farmId +
                            "&variety=" + encodedVariety +
                            "&reason=" + selectedReasonId)
                    .build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("REJECT", "Failed", e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                    try {

                        JSONArray arr = new JSONArray(response.body().string());

                        items.clear();

                        for (int i = 0; i < arr.length(); i++) {

                            JSONObject o = arr.getJSONObject(i);

                            RejectionHistoryItem r = new RejectionHistoryItem();

                            r.id = o.optInt("id");
                            r.variety = o.optString("variety");
                            r.reason = o.optString("rejection_reason", "N/A");
                            r.stems = o.optInt("stems");
                            r.length = o.optString("length");
                            r.greenhouse = o.optString("GreenhouseName", "");
                            r.time = o.optString("submitted_at");

                            items.add(r);
                        }

                        runOnUiThread(() -> adapter.notifyDataSetChanged());

                    } catch (Exception e) {
                        Log.e("REJECT_PARSE", "Error", e);
                    }
                }
            });

        } catch (Exception e) {
            Log.e("REJECT", "Exception", e);
        }
    }
}