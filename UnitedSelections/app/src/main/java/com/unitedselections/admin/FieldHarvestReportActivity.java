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

public class FieldHarvestReportActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    TextView txtTotal;
    Spinner spinnerGroup;
    Button btnDate;

    List<Object> harvestList = new ArrayList<>();
    HarvestAdapter adapter;

    String selectedDate;
    String selectedGroup = "variety";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_harvest_report);

        recyclerView = findViewById(R.id.recyclerHarvest);
        txtTotal = findViewById(R.id.txtTotal);
        spinnerGroup = findViewById(R.id.spinnerGroup);
        btnDate = findViewById(R.id.btnDate);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HarvestAdapter(harvestList);
        recyclerView.setAdapter(adapter);

        selectedDate = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        btnDate.setText(selectedDate);

        String[] groups = {"variety", "greenhouse", "both"};
        spinnerGroup.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, groups));

        spinnerGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int pos, long id) {
                selectedGroup = groups[pos];
                fetchData();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });

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
                String API = "https://unitedselections.supplybridge.co.ke/api/harvest.php?date="
                        + selectedDate + "&group=" + selectedGroup;

                URL url = new URL(API);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                InputStream is = conn.getInputStream();
                Scanner scanner = new Scanner(is).useDelimiter("\\A");
                String response = scanner.hasNext() ? scanner.next() : "";

                JSONObject json = new JSONObject(response);
                JSONArray arr = json.getJSONArray("data");
                int total = json.getInt("total");

                harvestList.clear();

                if(selectedGroup.equals("variety")) {
                    for(int i=0; i<arr.length(); i++){
                        JSONObject obj = arr.getJSONObject(i);
                        String name = obj.optString("variety", "N/A");
                        int qty = Integer.parseInt(obj.optString("qty","0"));
                        harvestList.add(new VarietyItem(name, qty));
                    }

                } else if(selectedGroup.equals("greenhouse")) {
                    for(int i=0; i<arr.length(); i++){
                        JSONObject obj = arr.getJSONObject(i);
                        String name = obj.optString("greenhouse", "N/A");
                        int qty = Integer.parseInt(obj.optString("qty","0"));
                        harvestList.add(new GreenhouseItem(name, qty, null));
                    }

                } else if(selectedGroup.equals("both")) {
                    // Group by greenhouse, then list varieties
                    // First, we need a map to accumulate greenhouse totals
                    for(int i=0; i<arr.length(); i++){
                        JSONObject obj = arr.getJSONObject(i);
                        String ghName = obj.optString("greenhouse","N/A");
                        int qty = Integer.parseInt(obj.optString("qty","0"));
                        String varietyName = obj.optString("variety","N/A");

                        // Add greenhouse item if not already added
                        boolean ghExists = false;
                        for(Object o : harvestList){
                            if(o instanceof GreenhouseItem && ((GreenhouseItem)o).greenhouseName.equals(ghName)){
                                ((GreenhouseItem)o).totalStems += qty;
                                ghExists = true;
                                break;
                            }
                        }
                        if(!ghExists){
                            harvestList.add(new GreenhouseItem(ghName, qty, null));
                        }

                        // Add variety under greenhouse
                        harvestList.add(new VarietyItem(varietyName, qty));
                    }
                }

                runOnUiThread(() -> {
                    txtTotal.setText("Total: " + total);
                    adapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    // Data models
    public static class GreenhouseItem {
        public String greenhouseName;
        public int totalStems;
        public List<VarietyItem> varieties;
        public GreenhouseItem(String name, int total, List<VarietyItem> list){
            greenhouseName = name;
            totalStems = total;
            varieties = list;
        }
    }

    public static class VarietyItem {
        public String varietyName;
        public int stems;
        public VarietyItem(String name, int stems){
            varietyName=name;
            this.stems=stems;
        }
    }
}