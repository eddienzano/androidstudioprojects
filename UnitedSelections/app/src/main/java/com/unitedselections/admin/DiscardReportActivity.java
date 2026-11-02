package com.unitedselections.admin;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.unitedselections.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

public class DiscardReportActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ArrayList<JSONObject> list = new ArrayList<>();
    ReportAdapter adapter;

    String API = "https://unitedselections.supplybridge.co.ke/api/discards.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_harvest_report);

        recyclerView = findViewById(R.id.recyclerHarvest);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ReportAdapter(list);
        recyclerView.setAdapter(adapter);

        fetchData();
    }

    private void fetchData() {
        new Thread(() -> {
            try {
                URL url = new URL(API);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                InputStream is = conn.getInputStream();
                Scanner scanner = new Scanner(is).useDelimiter("\\A");
                String response = scanner.hasNext() ? scanner.next() : "";

                JSONObject json = new JSONObject(response);
                JSONArray arr = json.getJSONArray("data");

                list.clear();

                for (int i = 0; i < arr.length(); i++) {
                    list.add(arr.getJSONObject(i));
                }

                runOnUiThread(() -> adapter.notifyDataSetChanged());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}