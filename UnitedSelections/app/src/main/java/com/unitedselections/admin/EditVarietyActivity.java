package com.unitedselections.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.unitedselections.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditVarietyActivity extends AppCompatActivity {

    AutoCompleteTextView autoSearch;

    EditText etPedigree, etDescription, etSeq;

    Spinner spGreenhouse, spVarietyType;

    Button btnUpdate;

    LinearLayout layoutFields;

    OkHttpClient client;

    ArrayAdapter<String> autoAdapter;

    ArrayAdapter<GreenhouseModel> greenhouseAdapter;

    ArrayList<String> varietyNames = new ArrayList<>();

    ArrayList<Integer> varietyIds = new ArrayList<>();

    ArrayList<GreenhouseModel> greenhouseList =
            new ArrayList<>();

    int selectedId = 0;

    String SEARCH_URL =
            "https://unitedselections.supplybridge.co.ke/api/search_varieties.php";

    String GET_URL =
            "https://unitedselections.supplybridge.co.ke/api/get_variety.php";

    String UPDATE_URL =
            "https://unitedselections.supplybridge.co.ke/api/update_variety.php";

    String GREENHOUSE_URL =
            "https://unitedselections.supplybridge.co.ke/api/get_greenhouses.php";

    String[] varietyTypes = {
            "ROSE",
            "CHRYSANTHEMUM",
            "ALSTROEMERIA"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_variety);

        client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();

        initViews();

        setupVarietyTypes();

        loadGreenhouses();

        setupSearch();

        btnUpdate.setOnClickListener(v -> updateVariety());
    }

    private void initViews() {

        autoSearch = findViewById(R.id.autoSearch);

        etPedigree = findViewById(R.id.etPedigree);

        etDescription = findViewById(R.id.etDescription);

        etSeq = findViewById(R.id.etSeq);

        spGreenhouse = findViewById(R.id.spGreenhouse);

        spVarietyType = findViewById(R.id.spVarietyType);

        btnUpdate = findViewById(R.id.btnUpdate);

        layoutFields = findViewById(R.id.layoutFields);
    }

    private void setupVarietyTypes() {

        ArrayAdapter<String> typeAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        varietyTypes
                );

        spVarietyType.setAdapter(typeAdapter);
    }

    private void loadGreenhouses() {

        Request request = new Request.Builder()
                .url(GREENHOUSE_URL)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

                runOnUiThread(() ->
                        Toast.makeText(
                                EditVarietyActivity.this,
                                "Failed to load greenhouses",
                                Toast.LENGTH_SHORT
                        ).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {

                if (response.body() == null) return;

                String res = response.body().string();

                try {

                    JSONObject obj = new JSONObject(res);

                    JSONArray arr =
                            obj.getJSONArray("data");

                    greenhouseList.clear();

                    for (int i = 0; i < arr.length(); i++) {

                        JSONObject o =
                                arr.getJSONObject(i);

                        greenhouseList.add(
                                new GreenhouseModel(
                                        o.getInt("id"),
                                        o.getString("greenhouse_name"),
                                        o.getString("category_code")
                                )
                        );
                    }

                    runOnUiThread(() -> {

                        greenhouseAdapter =
                                new ArrayAdapter<>(
                                        EditVarietyActivity.this,
                                        android.R.layout.simple_spinner_dropdown_item,
                                        greenhouseList
                                );

                        spGreenhouse.setAdapter(
                                greenhouseAdapter
                        );
                    });

                } catch (Exception e) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    EditVarietyActivity.this,
                                    "Invalid greenhouse response",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );
                }
            }
        });
    }

    private void setupSearch() {

        autoAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                varietyNames
        );

        autoSearch.setAdapter(autoAdapter);

        autoSearch.setThreshold(1);

        autoSearch.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        String term =
                                s.toString().trim();

                        if (term.length() >= 1) {

                            searchVarieties(term);
                        }
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                });

        autoSearch.setOnItemClickListener(
                (parent, view, position, id) -> {

                    if (position < varietyIds.size()) {

                        selectedId =
                                varietyIds.get(position);

                        loadVariety(selectedId);
                    }
                });
    }

    private void searchVarieties(String term) {

        try {

            String encodedTerm =
                    URLEncoder.encode(term, "UTF-8");

            Request request = new Request.Builder()
                    .url(SEARCH_URL + "?term=" + encodedTerm)
                    .build();

            client.newCall(request).enqueue(
                    new Callback() {

                        @Override
                        public void onFailure(
                                Call call,
                                IOException e
                        ) {

                            runOnUiThread(() ->
                                    Toast.makeText(
                                            EditVarietyActivity.this,
                                            "Search failed",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                        }

                        @Override
                        public void onResponse(
                                Call call,
                                Response response
                        ) throws IOException {

                            if (response.body() == null)
                                return;

                            String res =
                                    response.body().string();

                            try {

                                JSONObject root =
                                        new JSONObject(res);

                                JSONArray arr =
                                        root.getJSONArray(
                                                "search_results"
                                        );

                                ArrayList<String> tempNames =
                                        new ArrayList<>();

                                ArrayList<Integer> tempIds =
                                        new ArrayList<>();

                                for (int i = 0;
                                     i < arr.length();
                                     i++) {

                                    JSONObject obj =
                                            arr.getJSONObject(i);

                                    String pedigree =
                                            obj.optString(
                                                    "pedigree_item"
                                            );

                                    String desc =
                                            obj.optString(
                                                    "description"
                                            );

                                    tempNames.add(
                                            pedigree + " - " + desc
                                    );

                                    tempIds.add(
                                            obj.optInt("id")
                                    );
                                }

                                runOnUiThread(() -> {

                                    varietyNames.clear();

                                    varietyIds.clear();

                                    varietyNames.addAll(
                                            tempNames
                                    );

                                    varietyIds.addAll(
                                            tempIds
                                    );

                                    autoAdapter =
                                            new ArrayAdapter<>(
                                                    EditVarietyActivity.this,
                                                    android.R.layout.simple_dropdown_item_1line,
                                                    varietyNames
                                            );

                                    autoSearch.setAdapter(
                                            autoAdapter
                                    );

                                    autoAdapter.notifyDataSetChanged();

                                    if (!varietyNames.isEmpty()) {

                                        autoSearch.showDropDown();
                                    }
                                });

                            } catch (Exception e) {

                                runOnUiThread(() ->
                                        Toast.makeText(
                                                EditVarietyActivity.this,
                                                "Invalid search response",
                                                Toast.LENGTH_SHORT
                                        ).show()
                                );
                            }
                        }
                    });

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Search error",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void loadVariety(int id) {

        Request request = new Request.Builder()
                .url(GET_URL + "?id=" + id)
                .build();

        client.newCall(request).enqueue(
                new Callback() {

                    @Override
                    public void onFailure(
                            Call call,
                            IOException e
                    ) {

                        runOnUiThread(() ->
                                Toast.makeText(
                                        EditVarietyActivity.this,
                                        "Failed to load variety",
                                        Toast.LENGTH_SHORT
                                ).show()
                        );
                    }

                    @Override
                    public void onResponse(
                            Call call,
                            Response response
                    ) throws IOException {

                        if (response.body() == null)
                            return;

                        String res =
                                response.body().string();

                        try {

                            JSONObject obj =
                                    new JSONObject(res);

                            String status =
                                    obj.optString("status");

                            if (!status.equals("success"))
                                return;

                            JSONObject data =
                                    obj.getJSONObject("data");

                            runOnUiThread(() -> {

                                layoutFields.setVisibility(
                                        View.VISIBLE
                                );

                                etPedigree.setText(
                                        data.optString(
                                                "pedigree_item"
                                        )
                                );

                                etDescription.setText(
                                        data.optString(
                                                "description"
                                        )
                                );

                                etSeq.setText(
                                        data.optString("seq")
                                );

                                setGreenhouseSpinner(
                                        data.optString(
                                                "category"
                                        )
                                );

                                setSpinnerValue(
                                        spVarietyType,
                                        data.optString(
                                                "variety_type"
                                        )
                                );
                            });

                        } catch (Exception e) {

                            runOnUiThread(() ->
                                    Toast.makeText(
                                            EditVarietyActivity.this,
                                            "Invalid variety response",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                        }
                    }
                });
    }

    private void updateVariety() {

        GreenhouseModel selectedGreenhouse =
                (GreenhouseModel)
                        spGreenhouse.getSelectedItem();

        if (selectedGreenhouse == null) {

            Toast.makeText(
                    this,
                    "Please select greenhouse",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        btnUpdate.setEnabled(false);

        RequestBody formBody =
                new FormBody.Builder()

                        .add(
                                "id",
                                String.valueOf(selectedId)
                        )

                        .add(
                                "pedigree_item",
                                etPedigree.getText()
                                        .toString()
                                        .trim()
                        )

                        .add(
                                "description",
                                etDescription.getText()
                                        .toString()
                                        .trim()
                        )

                        .add(
                                "seq",
                                etSeq.getText()
                                        .toString()
                                        .trim()
                        )

                        .add(
                                "category",
                                selectedGreenhouse.getCategory()
                        )

                        .add(
                                "variety_type",
                                spVarietyType.getSelectedItem()
                                        .toString()
                        )

                        .build();

        Request request = new Request.Builder()
                .url(UPDATE_URL)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(
                new Callback() {

                    @Override
                    public void onFailure(
                            Call call,
                            IOException e
                    ) {

                        runOnUiThread(() -> {

                            btnUpdate.setEnabled(true);

                            Toast.makeText(
                                    EditVarietyActivity.this,
                                    "Update failed",
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    }

                    @Override
                    public void onResponse(
                            Call call,
                            Response response
                    ) throws IOException {

                        runOnUiThread(() ->
                                btnUpdate.setEnabled(true)
                        );

                        if (response.body() == null)
                            return;

                        String res =
                                response.body().string();

                        try {

                            JSONObject obj =
                                    new JSONObject(res);

                            String message =
                                    obj.optString(
                                            "message",
                                            "Update completed"
                                    );

                            runOnUiThread(() -> {

                                Toast.makeText(
                                        EditVarietyActivity.this,
                                        message,
                                        Toast.LENGTH_LONG
                                ).show();

                                if (message.toLowerCase()
                                        .contains("success")) {

                                    selectedId = 0;

                                    autoSearch.setText("");

                                    etPedigree.setText("");

                                    etDescription.setText("");

                                    etSeq.setText("");

                                    spGreenhouse.setSelection(0);

                                    spVarietyType.setSelection(0);

                                    layoutFields.setVisibility(
                                            View.GONE
                                    );

                                    varietyNames.clear();

                                    varietyIds.clear();

                                    autoAdapter.notifyDataSetChanged();
                                }
                            });

                        } catch (Exception e) {

                            runOnUiThread(() ->
                                    Toast.makeText(
                                            EditVarietyActivity.this,
                                            "Invalid server response",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                        }
                    }
                });
    }

    private void setSpinnerValue(
            Spinner spinner,
            String value
    ) {

        ArrayAdapter adapter =
                (ArrayAdapter) spinner.getAdapter();

        for (int i = 0;
             i < adapter.getCount();
             i++) {

            if (adapter.getItem(i)
                    .toString()
                    .equals(value)) {

                spinner.setSelection(i);

                return;
            }
        }
    }

    private void setGreenhouseSpinner(
            String category
    ) {

        for (int i = 0;
             i < greenhouseList.size();
             i++) {

            if (greenhouseList.get(i)
                    .getCategory()
                    .equals(category)) {

                spGreenhouse.setSelection(i);

                return;
            }
        }
    }
}