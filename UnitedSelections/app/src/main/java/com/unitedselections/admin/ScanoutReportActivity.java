package com.unitedselections.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.unitedselections.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

import okhttp3.*;

public class ScanoutReportActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ProgressBar progressBar;

    List<ScanoutGroup> reportList = new ArrayList<>();
    ScanoutReportAdapter adapter;

    String BASE_URL = "https://unitedselections.supplybridge.co.ke/api/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanout_report);

        recyclerView = findViewById(R.id.recyclerReport);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScanoutReportAdapter(reportList);
        recyclerView.setAdapter(adapter);

        loadScanoutReport();
    }

    private void loadScanoutReport() {

        progressBar.setVisibility(View.VISIBLE);

        String url = BASE_URL + "reports/get_scanout_report.php";

        OkHttpClient client = new OkHttpClient();

        client.newCall(new Request.Builder().url(url).build())
                .enqueue(new Callback() {

                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getApplicationContext(),
                                    "Network error", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                        String body = response.body().string();

                        try {
                            JSONObject obj = new JSONObject(body);

                            if (!obj.getString("status").equals("success")) {
                                throw new Exception("Invalid response");
                            }

                            JSONObject data = obj.getJSONObject("data");

                            List<ScanoutGroup> tempList = new ArrayList<>();

                            Iterator<String> destKeys = data.keys();

                            while (destKeys.hasNext()) {

                                String destination = destKeys.next();
                                JSONObject destObj = data.getJSONObject(destination);

                                Iterator<String> scanKeys = destObj.keys();

                                while (scanKeys.hasNext()) {

                                    String scanId = scanKeys.next();
                                    JSONObject scanObj = destObj.getJSONObject(scanId);

                                    String target = scanObj.getString("target_name");
                                    String outDate = scanObj.getString("out_date");
                                    String harvestDate = scanObj.optString("harvest_date");

                                    JSONArray itemsArr = scanObj.getJSONArray("items");

                                    List<ScanoutItem> items = new ArrayList<>();

                                    for (int i = 0; i < itemsArr.length(); i++) {

                                        JSONObject it = itemsArr.getJSONObject(i);

                                        items.add(new ScanoutItem(
                                                it.getString("variety"),
                                                it.getInt("qty")
                                        ));
                                    }

                                    tempList.add(new ScanoutGroup(
                                            destination,
                                            target,
                                            outDate,
                                            harvestDate,
                                            items
                                    ));
                                }
                            }

                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                reportList.clear();
                                reportList.addAll(tempList);
                                adapter.notifyDataSetChanged();
                            });

                        } catch (Exception e) {
                            Log.e("REPORT", "Parse error", e);
                            runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                        }
                    }
                });
    }
}