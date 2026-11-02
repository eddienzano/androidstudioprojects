package com.aaagrowers.coldroom;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class BoxSyncActivity extends AppCompatActivity {

    private Button btnSync, btnScan;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_box_sync);

        btnSync = findViewById(R.id.btnSync);
        btnScan = findViewById(R.id.btnScan);

        dbHelper = new DBHelper(this);

        btnScan.setEnabled(false);

        btnSync.setOnClickListener(v -> {
            new SyncBoxesTask().execute();
        });

        btnScan.setOnClickListener(v -> {
            startActivity(new Intent(this, BoxScanActivity.class));
        });
    }

    // =========================
    // SYNC TASK
    // =========================
    private class SyncBoxesTask extends AsyncTask<Void, Void, Boolean> {

        int count = 0;

        @Override
        protected void onPreExecute() {
            Toast.makeText(BoxSyncActivity.this, "Syncing boxes...", Toast.LENGTH_SHORT).show();
            btnSync.setEnabled(false);
        }

        @Override

        protected Boolean doInBackground(Void... voids) {

            String apiUrl = "https://www.aaagrowers.co.ke/inventory/sync/api_get_box_storage.php";

            int attempts = 0;

            while (attempts < 3) {
                try {

                    attempts++;

                    URL url = new URL(apiUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(15000);   // 🔥 IMPORTANT
                    conn.setReadTimeout(15000);      // 🔥 IMPORTANT
                    conn.setUseCaches(false);

                    // 🔥 FORCE DNS RESOLUTION FIRST
                   // java.net.InetAddress.getByName(url.getHost());

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                    );

                    StringBuilder sb = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }

                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());

                    if (!json.getString("status").equals("success")) return false;

                    JSONArray data = json.getJSONArray("data");
                    count = data.length();

                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.beginTransaction();

                    try {

                        db.delete("box_storage_local", null, null);

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);

                            ContentValues cv = new ContentValues();
                            cv.put("qr_code", obj.getString("qr_code"));
                            cv.put("box_id", obj.getInt("box_id"));
                            cv.put("variety_name", obj.getString("VarietyName"));
                            cv.put("length", obj.getInt("length"));
                            cv.put("stems", obj.getInt("number_of_stems"));
                            cv.put("updated_at", obj.getString("updated_at"));

                            db.insert("box_storage_local", null, cv);
                        }

                        db.setTransactionSuccessful();

                    } finally {
                        db.endTransaction();
                        db.close();
                    }

                    return true; // ✅ success → exit loop

                } catch (java.net.UnknownHostException e) {

                    // 🔥 THIS IS YOUR CURRENT ERROR
                    e.printStackTrace();

                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

                } catch (Exception e) {

                    e.printStackTrace();
                    return false; // other errors → fail immediately
                }
            }

            return false; // after retries fail
        }

        @Override
        protected void onPostExecute(Boolean success) {
            btnSync.setEnabled(true);

            if (success) {
                Toast.makeText(BoxSyncActivity.this,
                        "Synced " + count + " boxes",
                        Toast.LENGTH_LONG).show();

                btnScan.setEnabled(true);
            } else {
                Toast.makeText(BoxSyncActivity.this,
                        "Sync failed",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}