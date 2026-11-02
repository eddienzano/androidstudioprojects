package com.unitedselections.field;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.unitedselections.R;
import com.unitedselections.admin.GreenhouseModel;
import com.unitedselections.db.VarietiesDbHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.*;

import androidx.appcompat.app.AlertDialog;
import android.widget.EditText;

public class FieldScanActivity extends AppCompatActivity {

    EditText qrInput, length, qty, remark;

    AutoCompleteTextView variety;
    Spinner greenhouse;

    Button btnScanQR, btnIntake, btnDiscard, btnAddVariety;

    RecyclerView varietyList;

    List<JSONObject> selectedVarieties = new ArrayList<>();
    VarietyAdapter adapter;

    ArrayList<GreenhouseModel> greenhouseList = new ArrayList<>();
    ArrayAdapter<GreenhouseModel> ghAdapter;

    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fieldscan);

        qrInput = findViewById(R.id.qrInput);
        length = findViewById(R.id.length);
        qty = findViewById(R.id.qty);
        remark = findViewById(R.id.remark);

        variety = findViewById(R.id.variety);
        greenhouse = findViewById(R.id.greenhouse);

        btnScanQR = findViewById(R.id.btnScanQR);
        btnIntake = findViewById(R.id.btnIntake);
        btnDiscard = findViewById(R.id.btnDiscard);
        btnAddVariety = findViewById(R.id.btnAddVariety);

        varietyList = findViewById(R.id.varietyList);

        adapter = new VarietyAdapter(selectedVarieties);
        varietyList.setLayoutManager(new LinearLayoutManager(this));
        varietyList.setAdapter(adapter);

        btnAddVariety.setOnClickListener(v -> addVariety());

        setupVarietyAutocomplete();
        loadGreenhouses();

        greenhouse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {

                if (position >= 0 && position < greenhouseList.size()) {

                    GreenhouseModel selected = greenhouseList.get(position);

                    loadVarieties(selected.getCategory());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnScanQR.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Scan QR Code");
            integrator.setBeepEnabled(true);
            integrator.setOrientationLocked(true);
            integrator.initiateScan();
        });

        btnIntake.setOnClickListener(v -> sendToServer("INTAKE"));
        btnDiscard.setOnClickListener(v -> sendToServer("DISCARD"));
    }

    // ================= VARIETY =================
    private void addVariety() {

        String selectedVariety = variety.getText().toString().trim();
        String quantity = qty.getText().toString().trim();

        if (selectedVariety.isEmpty() || quantity.isEmpty()) {

            Toast.makeText(
                    this,
                    "Enter variety and quantity",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        try {

            int newQty = Integer.parseInt(quantity);

        /* =========================
           CHECK DUPLICATES
        ========================= */
            for (int i = 0; i < selectedVarieties.size(); i++) {

                JSONObject existing = selectedVarieties.get(i);

                String existingVariety =
                        existing.getString("variety_name");

                if (existingVariety.equalsIgnoreCase(selectedVariety)) {

                    int oldQty =
                            existing.getInt("quantity");

                    int finalI = i;

                /* =========================
                   SHOW EDIT DIALOG
                ========================= */
                    EditText input = new EditText(this);
                    input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                    input.setText(String.valueOf(oldQty));
                    input.setSelection(input.getText().length());

                    new AlertDialog.Builder(this)
                            .setTitle("Variety Already Added")
                            .setMessage(
                                    selectedVariety +
                                            " already exists.\nEdit quantity?"
                            )
                            .setView(input)

                            .setPositiveButton("Update", (dialog, which) -> {

                                try {

                                    int updatedQty =
                                            Integer.parseInt(
                                                    input.getText()
                                                            .toString()
                                                            .trim()
                                            );

                                    existing.put(
                                            "quantity",
                                            updatedQty
                                    );

                                    adapter.notifyItemChanged(finalI);

                                    Toast.makeText(
                                            this,
                                            "Quantity updated",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                } catch (Exception e) {

                                    Toast.makeText(
                                            this,
                                            "Invalid quantity",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            })

                            .setNegativeButton("Cancel", null)
                            .show();

                    return;
                }
            }

        /* =========================
           ADD NEW VARIETY
        ========================= */
            JSONObject obj = new JSONObject();

            obj.put("variety_name", selectedVariety);
            obj.put("quantity", newQty);

            selectedVarieties.add(obj);

            adapter.notifyItemInserted(
                    selectedVarieties.size() - 1
            );

            variety.setText("");
            qty.setText("");

            Toast.makeText(
                    this,
                    "Variety added",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Invalid quantity",
                    Toast.LENGTH_SHORT
            ).show();

            e.printStackTrace();
        }
    }

    private void setupVarietyAutocomplete() {
        variety.setOnItemClickListener((parent, view, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            variety.setText(selected, false);
        });
    }

    // ================= GREENHOUSES FROM API =================
    private void loadGreenhouses() {

        String url = "https://unitedselections.supplybridge.co.ke/api/get_greenhouses.php";

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, java.io.IOException e) {

                runOnUiThread(() ->
                        Toast.makeText(FieldScanActivity.this,
                                "Failed to load greenhouses",
                                Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {

                if (response.body() == null) return;

                String res = response.body().string();

                try {
                    JSONObject obj = new JSONObject(res);

                    if (!obj.getString("status").equals("success")) return;

                    JSONArray arr = obj.getJSONArray("data");

                    greenhouseList.clear();

                    for (int i = 0; i < arr.length(); i++) {

                        JSONObject o = arr.getJSONObject(i);

                        greenhouseList.add(new GreenhouseModel(
                                o.getInt("id"),
                                o.getString("greenhouse_name"),
                                o.getString("category_code")
                        ));
                    }

                    runOnUiThread(() -> {

                        ghAdapter = new ArrayAdapter<>(
                                FieldScanActivity.this,
                                android.R.layout.simple_spinner_dropdown_item,
                                greenhouseList
                        );

                        greenhouse.setAdapter(ghAdapter);

                        if (!greenhouseList.isEmpty()) {
                            greenhouse.setSelection(0);
                        }
                    });

                } catch (Exception e) {

                    runOnUiThread(() ->
                            Toast.makeText(FieldScanActivity.this,
                                    "Invalid greenhouse data",
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    // ================= VARIETIES =================
    private void loadVarieties(String category) {

        Log.d("VARIETY", "Category: " + category);

        VarietiesDbHelper helper = new VarietiesDbHelper(this);
        android.database.sqlite.SQLiteDatabase db = helper.getReadableDatabase();

        android.database.Cursor cursor = db.rawQuery(
                "SELECT pedigree_item, description FROM varieties_cache ORDER BY pedigree_item ASC",
                null
        );

        List<String> list = new ArrayList<>();

        while (cursor.moveToNext()) {

            String code = cursor.getString(0);
            String desc = cursor.getString(1);

            if (desc != null && !desc.isEmpty()) {
                list.add(code + " - " + desc);
            } else {
                list.add(code);
            }
        }

        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                list
        );

        variety.setAdapter(adapter);
        variety.setThreshold(1);
    }

    // ================= QR =================
    // ===== QR RESULT =====
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            if (result.getContents() != null) {

                String qr = result.getContents();

                try {
                    String[] parts = qr.split("\\|");

                    String serial = "";
                    String extractedLength = "";

                    for (String part : parts) {
                        part = part.trim();

                        if (part.startsWith("UNSE") && part.length() == 9) {
                            serial = part;
                        }

                        if (part.matches("\\d+")) {
                            int val = Integer.parseInt(part);
                            if (val >= 40 && val <= 100) {
                                extractedLength = part;
                            }
                        }
                    }

                    if (!serial.isEmpty()) {
                        qrInput.setText(serial);
                    }

                    if (!extractedLength.isEmpty()) {
                        length.setText(extractedLength);
                    } else {
                        Toast.makeText(this, "Length not found", Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    Toast.makeText(this, "Invalid QR format", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(this, "Scan Cancelled", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // ================= SEND =================
    private void sendToServer(String actionType) {

        // ===== LOCK ALL UI VALUES FIRST (IMPORTANT FIX) =====
        final String qr = qrInput.getText().toString().trim();

        GreenhouseModel selectedGH =
                (GreenhouseModel) greenhouse.getSelectedItem();

        final String selectedGreenhouse =
                selectedGH != null ? selectedGH.getName() : "";

        final String selectedCategory =
                selectedGH != null ? selectedGH.getCategory() : "";

        final String userId = "1";
        final String remarkText = remark.getText().toString().trim();
        final JSONArray varietiesArray = new JSONArray(selectedVarieties);

        // ===== VALIDATION =====
        if (qr.isEmpty()) {
            Toast.makeText(this, "Scan QR first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedVarieties.isEmpty()) {
            Toast.makeText(this, "Add at least one variety", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedGreenhouse.isEmpty()) {
            Toast.makeText(this, "Select greenhouse", Toast.LENGTH_SHORT).show();
            return;
        }

        if (remarkText.isEmpty()) {
            Toast.makeText(this, "Enter reason for harvest", Toast.LENGTH_SHORT).show();
            return;
        }

        // ===== NETWORK THREAD =====
        new Thread(() -> {

            try {

                URL url = new URL("https://unitedselections.supplybridge.co.ke/api/scan_field.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData =
                        "qr_code=" + URLEncoder.encode(qr, "UTF-8") +
                                "&user_id=" + URLEncoder.encode(userId, "UTF-8") +
                                "&action=" + URLEncoder.encode(actionType, "UTF-8") +
                                "&greenhouse=" + URLEncoder.encode(selectedGreenhouse, "UTF-8") +
                                "&category=" + URLEncoder.encode(selectedCategory, "UTF-8") +
                                "&remark=" + URLEncoder.encode(remarkText, "UTF-8") +
                                "&varieties_json=" + URLEncoder.encode(varietiesArray.toString(), "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                runOnUiThread(() -> {

                    if (response.toString().contains("success")) {

                        Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show();

                        selectedVarieties.clear();
                        adapter.notifyDataSetChanged();

                        remark.setText("");

                    } else {
                        Toast.makeText(this,
                                "Server error: " + response,
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }

        }).start();
    }
}