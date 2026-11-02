package com.aaagrowers.coldroom.summers;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SummersSyncRepository {

    private static final Handler handler =
            new Handler(Looper.getMainLooper());

    public static void sendToServer(Context ctx) {

        new Thread(() -> {

            HttpURLConnection conn = null;
            SummersDbHelper db = new SummersDbHelper(ctx);

            try {

                // =====================================================
                // 1. SCANS (EXISTING)
                // =====================================================
                Cursor c = db.getPendingScans();
                JSONArray scansArr = new JSONArray();

                while (c.moveToNext()) {
                    JSONObject o = new JSONObject();
                    o.put("qr", c.getString(0));
                    o.put("farm", c.getString(1));
                    scansArr.put(o);
                }
                c.close();

                // =====================================================
                // 2. FIX HEADERS
                // =====================================================
                Cursor h = db.getReadableDatabase().rawQuery(
                        "SELECT * FROM qr_fix_header WHERE synced=0",
                        null
                );

                JSONArray fixesArr = new JSONArray();

                while (h.moveToNext()) {

                    String qr = h.getString(h.getColumnIndexOrThrow("qr_code"));

                    JSONObject fixObj = new JSONObject();
                    fixObj.put("qr_code", qr);
                    fixObj.put("farm", h.getString(h.getColumnIndexOrThrow("farm")));
                    fixObj.put("serial", h.getString(h.getColumnIndexOrThrow("serial")));
                    fixObj.put("length", h.getInt(h.getColumnIndexOrThrow("length")));
                    fixObj.put("bucket_name", h.getString(h.getColumnIndexOrThrow("bucket_name")));

                    // =====================================================
                    // ITEMS FOR THIS QR
                    // =====================================================
                    Cursor i = db.getReadableDatabase().rawQuery(
                            "SELECT variety_id, variety_name, quantity FROM qr_fix_items WHERE qr_code=?",
                            new String[]{qr}
                    );

                    JSONArray itemsArr = new JSONArray();

                    while (i.moveToNext()) {

                        JSONObject item = new JSONObject();
                        item.put("variety_id", i.getInt(0));
                        item.put("variety_name", i.getString(1));
                        item.put("quantity", i.getInt(2));

                        itemsArr.put(item);
                    }

                    i.close();

                    fixObj.put("items", itemsArr);

                    fixesArr.put(fixObj);
                }

                h.close();

                // =====================================================
                // 3. BUILD FINAL PAYLOAD
                // =====================================================
                JSONObject payload = new JSONObject();
                payload.put("records", scansArr);
                payload.put("fixes", fixesArr);

                if (scansArr.length() == 0 && fixesArr.length() == 0) {

                    handler.post(() ->
                            Toast.makeText(ctx, "Nothing to sync", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                // =====================================================
                // 4. SEND
                // =====================================================
                URL url = new URL(
                        "https://www.aaagrowers.co.ke/inventory/summers/api/sync_summers_scan.php"
                );

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Content-Type", "application/json");

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes());
                os.flush();
                os.close();

                int code = conn.getResponseCode();

                if (code == 200) {

                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                    );

                    StringBuilder sb = new StringBuilder();
                    String line;

                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    JSONObject response = new JSONObject(sb.toString());

                    int inserted = response.optInt("inserted", 0);
                    int updated = response.optInt("updated", 0);
                    int failed = response.optInt("failed", 0);

                    handler.post(() -> {

                        Toast.makeText(
                                ctx,
                                "Synced: " + (inserted + updated) + " | Failed: " + failed,
                                Toast.LENGTH_LONG
                        ).show();

                        // =====================================================
                        // MARK SCANS SYNCED
                        // =====================================================
                        db.markSyncedNonDuplicates();

                        // =====================================================
                        // MARK FIXES SYNCED
                        // =====================================================
                        db.getWritableDatabase().execSQL(
                                "UPDATE qr_fix_header SET synced=1 WHERE synced=0"
                        );

                        db.getWritableDatabase().execSQL(
                                "UPDATE qr_fix_items SET synced=1 WHERE synced=0"
                        );
                    });

                } else {

                    handler.post(() ->
                            Toast.makeText(ctx,
                                    "Sync failed: HTTP " + code,
                                    Toast.LENGTH_LONG).show()
                    );
                }

            } catch (Exception e) {

                Log.e("SUMMERS_SYNC", "Sync Error", e);

                handler.post(() ->
                        Toast.makeText(ctx,
                                "Sync error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );

            } finally {
                if (conn != null) conn.disconnect();
            }

        }).start();
    }
}