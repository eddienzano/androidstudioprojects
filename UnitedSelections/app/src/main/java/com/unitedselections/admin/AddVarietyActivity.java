package com.unitedselections.admin;

import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.unitedselections.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.*;

public class AddVarietyActivity extends AppCompatActivity {

    EditText etPedigree, etDescription, etSeq;

    Spinner spGreenhouse, spVarietyType;

    Button btnSave;

    String BASE_URL =
            "https://unitedselections.supplybridge.co.ke/api/create_variety.php";

    String GREENHOUSE_URL =
            "https://unitedselections.supplybridge.co.ke/api/get_greenhouses.php";

    String[] varietyTypes = {
            "ROSE",
            "CHRYSANTHEMUM",
            "ALSTROEMERIA"
    };

    ArrayList<GreenhouseModel> greenhouseList = new ArrayList<>();

    ArrayAdapter<GreenhouseModel> greenhouseAdapter;

    OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_variety);

        initViews();

        setupVarietyTypes();

        loadGreenhouses();

        btnSave.setOnClickListener(v -> saveVariety());
    }

    private void initViews() {

        etPedigree = findViewById(R.id.etPedigree);
        etDescription = findViewById(R.id.etDescription);
        etSeq = findViewById(R.id.etSeq);

        spGreenhouse = findViewById(R.id.spGreenhouse);
        spVarietyType = findViewById(R.id.spVarietyType);

        btnSave = findViewById(R.id.btnSave);
    }

    private void setupVarietyTypes() {

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
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
                                AddVarietyActivity.this,
                                "Failed to load greenhouses",
                                Toast.LENGTH_SHORT
                        ).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.body() == null) return;

                String res = response.body().string();

                try {

                    JSONObject obj = new JSONObject(res);

                    String status = obj.optString("status");

                    if (!status.equals("success")) {
                        return;
                    }

                    JSONArray arr = obj.getJSONArray("data");

                    greenhouseList.clear();

                    for (int i = 0; i < arr.length(); i++) {

                        JSONObject o = arr.getJSONObject(i);

                        greenhouseList.add(
                                new GreenhouseModel(
                                        o.getInt("id"),
                                        o.getString("greenhouse_name"),
                                        o.getString("category_code")
                                )
                        );
                    }

                    runOnUiThread(() -> {

                        greenhouseAdapter = new ArrayAdapter<>(
                                AddVarietyActivity.this,
                                android.R.layout.simple_spinner_dropdown_item,
                                greenhouseList
                        );

                        spGreenhouse.setAdapter(greenhouseAdapter);
                    });

                } catch (Exception e) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    AddVarietyActivity.this,
                                    "Invalid greenhouse response",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );
                }
            }
        });
    }

    private void saveVariety() {

        String pedigree =
                etPedigree.getText().toString().trim();

        String desc =
                etDescription.getText().toString().trim();

        String seqStr =
                etSeq.getText().toString().trim();

        if (pedigree.isEmpty()) {

            Toast.makeText(
                    this,
                    "Pedigree is required",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        GreenhouseModel selectedGreenhouse =
                (GreenhouseModel) spGreenhouse.getSelectedItem();

        if (selectedGreenhouse == null) {

            Toast.makeText(
                    this,
                    "Please select greenhouse",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        btnSave.setEnabled(false);

        String category =
                selectedGreenhouse.getCategory();

        String varietyType =
                spVarietyType.getSelectedItem().toString();

        RequestBody formBody = new FormBody.Builder()

                .add("pedigree_item", pedigree)

                .add("description", desc)

                .add("category", category)

                .add("variety_type", varietyType)

                .add("seq", seqStr.isEmpty() ? "0" : seqStr)

                .build();

        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

                runOnUiThread(() -> {

                    btnSave.setEnabled(true);

                    Toast.makeText(
                            AddVarietyActivity.this,
                            "Connection failed",
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                runOnUiThread(() ->
                        btnSave.setEnabled(true)
                );

                if (response.body() == null) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    AddVarietyActivity.this,
                                    "Empty server response",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );

                    return;
                }

                String res = response.body().string();

                try {

                    JSONObject obj = new JSONObject(res);

                    String status =
                            obj.getString("status");

                    String message =
                            obj.getString("message");

                    runOnUiThread(() -> {

                        Toast.makeText(
                                AddVarietyActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();

                        if (status.equals("success")) {

                            etPedigree.setText("");
                            etDescription.setText("");
                            etSeq.setText("");

                            spGreenhouse.setSelection(0);
                            spVarietyType.setSelection(0);
                        }
                    });

                } catch (Exception e) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    AddVarietyActivity.this,
                                    "Invalid server response",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );
                }
            }
        });
    }
}