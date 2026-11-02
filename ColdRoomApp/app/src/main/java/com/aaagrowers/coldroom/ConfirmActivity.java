package com.aaagrowers.coldroom;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ConfirmActivity extends AppCompatActivity {

    private Spinner farmSpinner;
    private Button syncButton, scanButton;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);

        farmSpinner = findViewById(R.id.farmSpinner);
        syncButton = findViewById(R.id.syncButton);
        scanButton = findViewById(R.id.scanButton);

        dbHelper = new DBHelper(this);

        String[] farms = {"Chui", "Simba", "Chestnut"};

        farmSpinner.setAdapter(
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item,
                        farms)
        );

        syncButton.setOnClickListener(v -> {
            String farm = farmSpinner.getSelectedItem().toString();
            new SyncTask(farm).execute();
        });

        scanButton.setOnClickListener(v -> {
            String farm = farmSpinner.getSelectedItem().toString();
            Intent intent = new Intent(this, ScanActivity.class);
            intent.putExtra("farm", farm);
            startActivity(intent);
        });

        scanButton.setEnabled(false);
    }

    // =========================
    // SYNC TASK
    // =========================
    private class SyncTask extends AsyncTask<Void, Void, Boolean> {

        private final String farm;
        private int qrCount = 0;
        private int varietyCount = 0;

        SyncTask(String farm) {
            this.farm = farm;
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(ConfirmActivity.this,
                    "Syncing data...",
                    Toast.LENGTH_SHORT).show();

            syncButton.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            try {

                // =========================
                // QR INVENTORY
                // =========================
                JSONObject qrJson = getJson(
                        "https://www.aaagrowers.co.ke/inventory/api/api_get_qr_inventory.php?farm=" + farm
                );

                if (qrJson == null || !qrJson.optString("status").equals("success")) {
                    Log.e("SYNC", "QR API failed");
                    return false;
                }

                JSONArray qrArray = qrJson.optJSONArray("data");

                if (qrArray != null) {

                    db.beginTransaction();

                    db.delete("qr_inventory", "farm=?", new String[]{farm});

                    for (int i = 0; i < qrArray.length(); i++) {

                        JSONObject item = qrArray.getJSONObject(i);

                        ContentValues cv = new ContentValues();
                        cv.put("qr_code", item.optString("qr_code"));
                        cv.put("variety_name", item.optString("variety_name"));
                        cv.put("quantity", item.optInt("quantity", 0));
                        cv.put("intake_time", item.optString("intake_time"));
                        cv.put("farm", farm);

                        long res = db.insertWithOnConflict(
                                "qr_inventory",
                                null,
                                cv,
                                SQLiteDatabase.CONFLICT_REPLACE
                        );

                        if (res == -1) {
                            Log.e("SYNC", "QR insert failed: " + item.toString());
                        }
                    }

                    db.setTransactionSuccessful();
                    db.endTransaction();

                    qrCount = qrArray.length();
                }

                // =========================
                // VARIETIES
                // =========================
                JSONObject vJson = getJson(
                        "https://www.aaagrowers.co.ke/inventory/api/api_get_varieties.php?farm=" + farm
                );

                if (vJson == null || !vJson.optString("status").equals("success")) {
                    Log.e("SYNC", "Variety API failed");
                    return false;
                }

                JSONArray vArray = vJson.optJSONArray("data");

                if (vArray != null && vArray.length() > 0) {

                    db.beginTransaction();

                    db.delete("varieties_local", null, null);

                    for (int i = 0; i < vArray.length(); i++) {

                        JSONObject item = vArray.getJSONObject(i);

                        ContentValues cv = new ContentValues();
                        cv.put("varietyid", item.optInt("id"));
                        cv.put("varietyname", item.optString("name"));

                        long res = db.insert("varieties_local", null, cv);

                        if (res == -1) {
                            Log.e("SYNC", "Variety insert failed: " + item.toString());
                        }
                    }

                    db.setTransactionSuccessful();
                    db.endTransaction();

                    varietyCount = vArray.length();
                }

                return true;

            } catch (Exception e) {
                Log.e("SYNC", "ERROR", e);
                return false;

            } finally {
                if (db.inTransaction()) {
                    db.endTransaction();
                }
                db.close();
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {

            syncButton.setEnabled(true);

            if (success) {
                Toast.makeText(
                        ConfirmActivity.this,
                        "Sync complete\nQR: " + qrCount + "\nVarieties: " + varietyCount,
                        Toast.LENGTH_LONG
                ).show();

                scanButton.setEnabled(true);
            } else {
                Toast.makeText(
                        ConfirmActivity.this,
                        "Sync failed (check logs)",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    // =========================
    // HTTP JSON helper
    // =========================
    private JSONObject getJson(String urlStr) {
        try {

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(conn.getInputStream()));

            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            return new JSONObject(sb.toString());

        } catch (Exception e) {
            Log.e("SYNC", "HTTP ERROR", e);
            return null;
        }
    }
}