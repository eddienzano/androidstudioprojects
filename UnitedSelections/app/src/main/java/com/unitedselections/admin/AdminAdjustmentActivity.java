package com.unitedselections.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.unitedselections.R;
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

public class AdminAdjustmentActivity extends AppCompatActivity {

    EditText qrInput, qty, reason;

    AutoCompleteTextView variety;

    Button btnFetch, btnAdd, btnScanQR, btnSaveAll;

    TextView txtGreenhouse, txtVerified;

    LinearLayout infoCard;

    RecyclerView recycler;

    AdjustmentAdapter adapter;

    List<JSONObject> selectedVarieties = new ArrayList<>();

    String greenhouse = "";

    int verified = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_adjustment);

        qrInput = findViewById(R.id.qrInput);
        qty = findViewById(R.id.qty);
        reason = findViewById(R.id.reason);

        variety = findViewById(R.id.variety);

        btnFetch = findViewById(R.id.btnFetch);
        btnAdd = findViewById(R.id.btnAdd);
        btnScanQR = findViewById(R.id.btnScanQR);
        btnSaveAll = findViewById(R.id.btnSaveAll);

        txtGreenhouse = findViewById(R.id.txtGreenhouse);
        txtVerified = findViewById(R.id.txtVerified);

        infoCard = findViewById(R.id.infoCard);

        recycler = findViewById(R.id.recycler);

        adapter = new AdjustmentAdapter(this, selectedVarieties);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        loadVarieties();

        btnScanQR.setOnClickListener(v -> scanQR());

        btnFetch.setOnClickListener(v -> fetchQR());

        btnAdd.setOnClickListener(v -> addVariety());

        btnSaveAll.setOnClickListener(v -> saveAdjustments());
    }

    /* =========================================
       QR SCANNER
    ========================================= */

    private void scanQR() {

        IntentIntegrator integrator =
                new IntentIntegrator(this);

        integrator.setPrompt("Scan Harvest QR");

        integrator.setBeepEnabled(true);

        integrator.setOrientationLocked(true);

        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {

        IntentResult result =
                IntentIntegrator.parseActivityResult(
                        requestCode,
                        resultCode,
                        data
                );

        if(result != null){

            if(result.getContents() != null){

                String qrRaw = result.getContents();

                try {

                    String[] parts = qrRaw.split("\\|");

                    String serial = "";

                    for(String part : parts){

                        part = part.trim();

                        if(part.startsWith("UNSE")
                                && part.length() == 9){

                            serial = part;
                        }
                    }

                    if(!serial.isEmpty()){

                        qrInput.setText(serial);

                        fetchQR();

                    }else{

                        Toast.makeText(this,
                                "Invalid QR",
                                Toast.LENGTH_SHORT).show();
                    }

                }catch (Exception e){

                    Toast.makeText(this,
                            "QR Parse Error",
                            Toast.LENGTH_SHORT).show();
                }

            }else{

                Toast.makeText(this,
                        "Scan Cancelled",
                        Toast.LENGTH_SHORT).show();
            }

        }else{
            super.onActivityResult(
                    requestCode,
                    resultCode,
                    data
            );
        }
    }

    /* =========================================
       LOAD VARIETIES
    ========================================= */

    private void loadVarieties() {

        VarietiesDbHelper helper =
                new VarietiesDbHelper(this);

        android.database.sqlite.SQLiteDatabase db =
                helper.getReadableDatabase();

        android.database.Cursor cursor = db.rawQuery(
                "SELECT pedigree_item FROM varieties_cache ORDER BY pedigree_item ASC",
                null
        );

        List<String> list = new ArrayList<>();

        while(cursor.moveToNext()){

            list.add(cursor.getString(0));
        }

        cursor.close();

        ArrayAdapter<String> arr =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        list
                );

        variety.setAdapter(arr);

        variety.setThreshold(1);
    }

    /* =========================================
       FETCH QR DATA
    ========================================= */

    private void fetchQR() {

        new Thread(() -> {

            try {

                String qr =
                        qrInput.getText()
                                .toString()
                                .trim();

                URL url = new URL(
                        "https://unitedselections.supplybridge.co.ke/api/fetch_qr_details.php?qr=" +
                                URLEncoder.encode(qr, "UTF-8")
                );

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        url.openStream()
                                )
                        );

                StringBuilder response =
                        new StringBuilder();

                String line;

                while((line = reader.readLine()) != null){

                    response.append(line);
                }

                JSONObject obj =
                        new JSONObject(response.toString());

                if(obj.getString("status")
                        .equals("success")){

                    greenhouse =
                            obj.getString("greenhouse");

                    verified =
                            obj.getInt("verified");

                    JSONArray arr =
                            obj.getJSONArray("items");

                    selectedVarieties.clear();

                    for(int i = 0; i < arr.length(); i++){

                        selectedVarieties.add(
                                arr.getJSONObject(i)
                        );
                    }

                    runOnUiThread(() -> {

                        infoCard.setVisibility(
                                android.view.View.VISIBLE
                        );

                        txtGreenhouse.setText(
                                "Greenhouse: " + greenhouse
                        );

                        txtVerified.setText(
                                verified == 1
                                        ? "Verified: YES"
                                        : "Verified: NO"
                        );

                        adapter.notifyDataSetChanged();
                    });

                }else{

                    String msg =
                            obj.optString(
                                    "message",
                                    "Failed to load QR"
                            );

                    runOnUiThread(() ->
                            Toast.makeText(
                                    this,
                                    msg,
                                    Toast.LENGTH_LONG
                            ).show()
                    );
                }

            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(this,
                                e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }

        }).start();
    }

    /* =========================================
       ADD NEW VARIETY
    ========================================= */

    private void addVariety() {

        try {

            String v =
                    variety.getText()
                            .toString()
                            .trim();

            int q =
                    Integer.parseInt(
                            qty.getText()
                                    .toString()
                                    .trim()
                    );

            if(v.isEmpty()){

                Toast.makeText(this,
                        "Enter variety",
                        Toast.LENGTH_SHORT).show();

                return;
            }

            JSONObject obj = new JSONObject();

            obj.put("id", 0);

            obj.put("variety", v);

            obj.put("quantity", q);

            selectedVarieties.add(obj);

            adapter.notifyItemInserted(
                    selectedVarieties.size() - 1
            );

            variety.setText("");

            qty.setText("");

        } catch (Exception e) {

            Toast.makeText(this,
                    "Invalid quantity",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /* =========================================
       SAVE
    ========================================= */

    private void saveAdjustments() {

        new Thread(() -> {

            try {

                JSONArray arr =
                        new JSONArray(selectedVarieties);

                String postData =

                        "qr_code=" +
                                URLEncoder.encode(
                                        qrInput.getText().toString(),
                                        "UTF-8"
                                )

                                +

                                "&items=" +
                                URLEncoder.encode(
                                        arr.toString(),
                                        "UTF-8"
                                )

                                +

                                "&reason=" +
                                URLEncoder.encode(
                                        reason.getText().toString(),
                                        "UTF-8"
                                )

                                +

                                "&user_id=1";

                URL url = new URL(
                        "https://unitedselections.supplybridge.co.ke/api/add_harvest_adjustment.php"
                );

                HttpURLConnection conn =
                        (HttpURLConnection)
                                url.openConnection();

                conn.setRequestMethod("POST");

                conn.setDoOutput(true);

                OutputStream os =
                        conn.getOutputStream();

                os.write(postData.getBytes());

                os.flush();

                os.close();

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        conn.getInputStream()
                                )
                        );

                StringBuilder response =
                        new StringBuilder();

                String line;

                while((line = reader.readLine()) != null){

                    response.append(line);
                }

                JSONObject obj =
                        new JSONObject(response.toString());

                runOnUiThread(() -> {

                    if(obj.optString("status")
                            .equals("success")){

                        Toast.makeText(this,
                                "Adjustments saved",
                                Toast.LENGTH_LONG).show();

                    }else{

                        Toast.makeText(this,
                                obj.optString("message"),
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(this,
                                e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }

        }).start();
    }
}