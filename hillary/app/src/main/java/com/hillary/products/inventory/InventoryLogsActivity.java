package com.hillary.products.inventory;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hillary.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class InventoryLogsActivity
        extends AppCompatActivity {

    RecyclerView recyclerLogs;

    EditText edtSearch;

    Spinner spinnerFilter;

    List<InventoryLog> list;

    InventoryLogsAdapter adapter;

    String token = "";

    String BASE_URL =
            "https://www.aaagrowers.co.ke/inventory/fillers/api/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_logs);

        recyclerLogs =
                findViewById(R.id.recyclerLogs);

        edtSearch =
                findViewById(R.id.edtSearch);

        spinnerFilter =
                findViewById(R.id.spinnerFilter);

        token = getSharedPreferences(
                "app",
                MODE_PRIVATE
        ).getString("token", "");

        list = new ArrayList<>();

        recyclerLogs.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new InventoryLogsAdapter(
                this,
                list,
                BASE_URL
        );

        recyclerLogs.setAdapter(adapter);

        setupFilter();

        loadLogs();
    }

    private void setupFilter(){

        String[] filters = {
                "ALL",
                "STOCK_IN",
                "STOCK_OUT"
        };

        ArrayAdapter<String> arr =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        filters
                );

        arr.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerFilter.setAdapter(arr);

        edtSearch.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        applyFilter();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                });
    }

    private void applyFilter(){

        String search =
                edtSearch.getText().toString();

        String type =
                spinnerFilter.getSelectedItem().toString();

        adapter.filter(search, type);
    }

    private void loadLogs(){

        new Thread(() -> {

            try{

                OkHttpClient client =
                        new OkHttpClient();

                Request request =
                        new Request.Builder()
                                .url(BASE_URL +
                                        "inventory/get_transactions.php")
                                .addHeader(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .build();

                Response response =
                        client.newCall(request).execute();

                String res =
                        response.body().string();

                JSONObject obj =
                        new JSONObject(res);

                JSONArray arr =
                        obj.getJSONArray("transactions");

                list.clear();

                for(int i=0; i<arr.length(); i++){

                    JSONObject x =
                            arr.getJSONObject(i);

                    list.add(new InventoryLog(
                            x.getString("product_name"),
                            x.optString("thumbnail"),
                            x.getString("transaction_type"),
                            x.getString("quantity"),
                            x.getString("previous_stock"),
                            x.getString("new_stock"),
                            x.optString("notes"),
                            x.getString("created_at")
                    ));
                }

                runOnUiThread(() -> {

                    adapter.notifyDataSetChanged();
                });

            }catch(Exception e){

                runOnUiThread(() -> {

                    Toast.makeText(
                            InventoryLogsActivity.this,
                            e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }

        }).start();
    }
}