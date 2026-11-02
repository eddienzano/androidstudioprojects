package com.unitedselections.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.unitedselections.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class VarianceActivity extends AppCompatActivity {

    RecyclerView recycler;
    List<VarianceItem> list = new ArrayList<>();
    VarianceAdapter adapter;

    Button btnFrom, btnTo, btnFilter;

    String fromDate = "";
    String toDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_variance);

        recycler = findViewById(R.id.recyclerVariance);
        btnFrom = findViewById(R.id.btnFrom);
        btnTo = findViewById(R.id.btnTo);
        btnFilter = findViewById(R.id.btnFilter);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VarianceAdapter(this, list);
        recycler.setAdapter(adapter);

        // Default = today
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        fromDate = today;
        toDate = today;

        btnFrom.setText(fromDate);
        btnTo.setText(toDate);

        btnFrom.setOnClickListener(v -> pickDate(true));
        btnTo.setOnClickListener(v -> pickDate(false));

        btnFilter.setOnClickListener(v -> loadData());

        loadData();
    }

    private void pickDate(boolean isFrom) {
        Calendar cal = Calendar.getInstance();

        new DatePickerDialog(this, (view, year, month, day) -> {

            String date = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day);

            if (isFrom) {
                fromDate = date;
                btnFrom.setText(date);
            } else {
                toDate = date;
                btnTo.setText(date);
            }

        },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void loadData() {

        new Thread(() -> {
            try {

                String API = "https://unitedselections.supplybridge.co.ke/api/variance.php"
                        + "?from=" + fromDate + "&to=" + toDate;

                URL url = new URL(API);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                InputStream is = conn.getInputStream();
                Scanner sc = new Scanner(is).useDelimiter("\\A");
                String res = sc.hasNext() ? sc.next() : "";

                JSONObject json = new JSONObject(res);
                JSONArray arr = json.getJSONArray("data");

                list.clear();

                for(int i=0;i<arr.length();i++){
                    JSONObject o = arr.getJSONObject(i);

                    VarianceItem item = new VarianceItem();
                    item.id = o.getString("id"); // 🔥 ADD THIS
                    item.qr = o.getString("qr_code");
                    item.harvestVariety = o.getString("harvest_variety");
                    item.intakeVariety = o.optString("intake_variety","-");
                    item.harvestQty = o.getInt("harvest_qty");
                    item.intakeQty = o.optInt("intake_qty",0);
                    item.status = o.getString("status");

                    list.add(item);
                }

                runOnUiThread(() -> adapter.notifyDataSetChanged());

            } catch (Exception e){
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
}