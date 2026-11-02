package com.unitedselections;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.unitedselections.db.VarietiesDbHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_TIME = 2000;

    private static final String API_URL =
            "https://unitedselections.supplybridge.co.ke/api/varieties.php";

    private static final String TAG = "VARIETY_SYNC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        syncVarieties();

        new Handler().postDelayed(() -> {

            startActivity(
                    new Intent(MainActivity.this, LoginActivity.class)
            );

            finish();

        }, SPLASH_TIME);
    }

    private void syncVarieties() {

        new Thread(() -> {

            HttpURLConnection conn = null;
            InputStream is = null;

            try {

                Log.d(TAG, "STARTING SYNC");

                URL url = new URL(API_URL);

                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);

                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();

                Log.d(TAG, "HTTP CODE: " + responseCode);

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("HTTP ERROR: " + responseCode);
                }

                is = new BufferedInputStream(conn.getInputStream());

                ByteArrayOutputStream result = new ByteArrayOutputStream();

                byte[] buffer = new byte[4096];
                int length;

                while ((length = is.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }

                String response = result.toString("UTF-8");

                Log.d(TAG, "RESPONSE LENGTH: " + response.length());

                JSONObject json = new JSONObject(response);

                JSONArray data = json.getJSONArray("data");

                Log.d(TAG, "API RECORDS: " + data.length());

                VarietiesDbHelper dbHelper =
                        new VarietiesDbHelper(MainActivity.this);

                SQLiteDatabase db = dbHelper.getWritableDatabase();

                db.beginTransaction();

                try {

                    db.delete("varieties_cache", null, null);

                    int insertedCount = 0;

                    for (int i = 0; i < data.length(); i++) {

                        JSONObject obj = data.getJSONObject(i);

                        String pedigree =
                                obj.optString("pedigree_item", "").trim();

                        String description =
                                obj.optString("description", "").trim();

                        String category =
                                obj.optString("category", "").trim();

                        // skip empty pedigree
                        if (pedigree.isEmpty()) {
                            Log.e(TAG, "SKIPPED EMPTY PEDIGREE");
                            continue;
                        }

                        ContentValues cv = new ContentValues();

                        cv.put("pedigree_item", pedigree);
                        cv.put("description", description);
                        cv.put("category", category);

                        long insertResult =
                                db.insert("varieties_cache", null, cv);

                        if (insertResult != -1) {

                            insertedCount++;

                            Log.d(TAG,
                                    "INSERTED: "
                                            + pedigree
                                            + " | "
                                            + category);

                        } else {

                            Log.e(TAG,
                                    "FAILED INSERT: "
                                            + pedigree);
                        }
                    }

                    db.setTransactionSuccessful();

                    Log.d(TAG,
                            "TOTAL INSERTED: "
                                    + insertedCount);

                } finally {

                    db.endTransaction();
                }

                Cursor cursor = db.rawQuery(
                        "SELECT COUNT(*) FROM varieties_cache",
                        null
                );

                if (cursor.moveToFirst()) {

                    int count = cursor.getInt(0);

                    Log.d(TAG,
                            "FINAL DB COUNT: "
                                    + count);
                }

                cursor.close();

                db.close();

            } catch (Exception e) {

                Log.e(TAG,
                        "SYNC ERROR",
                        e);

            } finally {

                try {

                    if (is != null) {
                        is.close();
                    }

                } catch (Exception ignored) {}

                if (conn != null) {
                    conn.disconnect();
                }
            }

        }).start();
    }
}