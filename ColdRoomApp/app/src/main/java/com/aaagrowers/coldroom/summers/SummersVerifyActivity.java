package com.aaagrowers.coldroom.summers;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aaagrowers.coldroom.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SummersVerifyActivity extends AppCompatActivity {

    private TextView txtCache;
    private TextView txtPending;

    private Button btnSync;
    private Button btnScan;

    private SummersDbHelper dbHelper;

    private static final String API_URL =
            "https://www.aaagrowers.co.ke/inventory/summers/api/sync_qr_master.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summers_verify);

        txtCache = findViewById(R.id.txtCache);
        txtPending = findViewById(R.id.txtPending);

        btnSync = findViewById(R.id.btnSync);
        btnScan = findViewById(R.id.btnScan);

        dbHelper = new SummersDbHelper(this);

        updateCounts();

        btnSync.setOnClickListener(v ->
                new SyncTask().execute()
        );

        btnScan.setOnClickListener(v -> {

            Intent i = new Intent(
                    SummersVerifyActivity.this,
                    SummersVerifyScanActivity.class
            );

            startActivity(i);
        });
    }

    private void updateCounts() {

        int cached = dbHelper.getQrMasterCount();
        int pending = dbHelper.getPendingCount();

        txtCache.setText("Cached QRs: " + cached);
        txtPending.setText("Pending Uploads: " + pending);

        btnScan.setEnabled(cached > 0);
    }

    private class SyncTask extends AsyncTask<Void, Void, Boolean> {

        int totalRecords = 0;

        @Override
        protected void onPreExecute() {

            btnSync.setEnabled(false);

            Toast.makeText(
                    SummersVerifyActivity.this,
                    "Syncing...",
                    Toast.LENGTH_SHORT
            ).show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {

            SQLiteDatabase db =
                    dbHelper.getWritableDatabase();

            try {

                JSONObject json = getJson(API_URL);

                if(json == null)
                    return false;

                if(!json.optBoolean("success"))
                    return false;

                JSONArray data =
                        json.getJSONArray("data");

                db.beginTransaction();

                dbHelper.clearQrMaster();

                for (int i = 0; i < data.length(); i++) {

                    JSONObject row = data.getJSONObject(i);

                    dbHelper.insertQrMaster(
                            row.optString("serial"),
                            row.optString("qr_code"),   // 🔥 FROM JOIN
                            row.optString("bucket_name"),
                            row.optString("farm"),
                            row.optInt("length"),
                            row.optString("variety_name"),
                            row.optInt("quantity"),
                            row.optString("batch_id"),
                            row.optString("source_table")
                    );
                }

                db.setTransactionSuccessful();

                totalRecords = data.length();

                return true;

            }
            catch(Exception e)
            {
                Log.e("SUMMERS_SYNC",
                        "ERROR",
                        e);

                return false;
            }
            finally {

                if(db.inTransaction())
                    db.endTransaction();

                db.close();
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {

            btnSync.setEnabled(true);

            if(success)
            {
                Toast.makeText(
                        SummersVerifyActivity.this,
                        "Sync Complete\nRecords: "
                                + totalRecords,
                        Toast.LENGTH_LONG
                ).show();

                updateCounts();
            }
            else
            {
                Toast.makeText(
                        SummersVerifyActivity.this,
                        "Sync Failed",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private JSONObject getJson(String urlStr) {

        HttpURLConnection conn = null;

        try {

            Log.d("SUMMERS_SYNC", "REQUEST URL: " + urlStr);

            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);
            conn.setInstanceFollowRedirects(true);

            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "Android-App");

            int code = conn.getResponseCode();
            Log.d("SUMMERS_SYNC", "HTTP CODE: " + code);

            BufferedReader reader;

            if (code >= 200 && code < 300) {
                reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );
            } else {
                reader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream())
                );
            }

            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            reader.close();

            String response = sb.toString();

            Log.d("SUMMERS_SYNC", "RAW RESPONSE: " + response);

            return new JSONObject(response);

        } catch (Exception e) {

            Log.e("SUMMERS_SYNC", "HTTP ERROR: " + e.getMessage(), e);
            return null;

        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}