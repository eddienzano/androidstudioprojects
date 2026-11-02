package com.unitedselections.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.unitedselections.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class IntakeReportActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ArrayList<JSONObject> list = new ArrayList<>();
    ReportAdapter adapter;

    TextView txtTotal;
    Button btnDate;

    String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intake_report);

        recyclerView = findViewById(R.id.recyclerIntake);
        txtTotal = findViewById(R.id.txtTotal);
        btnDate = findViewById(R.id.btnDate);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(list);
        recyclerView.setAdapter(adapter);

        // Default date = today
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        btnDate.setText(selectedDate);

        btnDate.setOnClickListener(v -> showDatePicker());

        fetchData();
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();

        new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDate = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day);
            btnDate.setText(selectedDate);
            fetchData();
        },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void fetchData() {
        new Thread(() -> {
            try {

                String API = "https://unitedselections.supplybridge.co.ke/api/intake.php?date=" + selectedDate;

                URL url = new URL(API);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                InputStream is = conn.getInputStream();
                Scanner scanner = new Scanner(is).useDelimiter("\\A");
                String response = scanner.hasNext() ? scanner.next() : "";

                JSONObject json = new JSONObject(response);
                JSONArray arr = json.getJSONArray("data");

                int total = json.optInt("total", 0);

                list.clear();

                for (int i = 0; i < arr.length(); i++) {
                    list.add(arr.getJSONObject(i));
                }

                runOnUiThread(() -> {
                    txtTotal.setText("Total: " + total);
                    adapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}