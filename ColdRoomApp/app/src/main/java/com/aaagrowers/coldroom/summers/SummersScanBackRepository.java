package com.aaagrowers.coldroom.summers;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class SummersScanBackRepository {

    private static final Handler handler =
            new Handler(Looper.getMainLooper());

    public static void sendToServer(Context ctx) {

        new Thread(() -> {

            HttpURLConnection conn = null;
            SummersDbHelper db = new SummersDbHelper(ctx);

            try {

                Cursor h = db.getReadableDatabase().rawQuery(
                        "SELECT * FROM qr_fix_header WHERE synced=0",
                        null
                );

                int headersFound = 0;

                while (h.moveToNext()) {

                    headersFound++;

                    String qr = h.getString(h.getColumnIndexOrThrow("qr_code"));

                    String serial = h.getString(h.getColumnIndexOrThrow("serial"));
                    String bucket = h.getString(h.getColumnIndexOrThrow("bucket_name"));
                    String farm = h.getString(h.getColumnIndexOrThrow("farm"));
                    int length = h.getInt(h.getColumnIndexOrThrow("length"));

                    Log.d("SCAN_BACK_DEBUG", "Sending QR: " + qr);
                    Log.d("SCAN_BACK_DEBUG", "Serial: " + serial);
                    Log.d("SCAN_BACK_DEBUG", "Farm: " + farm);

                    // build varieties JSON
                    Cursor i = db.getReadableDatabase().rawQuery(
                            "SELECT variety_id, variety_name, quantity FROM qr_fix_items WHERE qr_code=?",
                            new String[]{qr}
                    );

                    StringBuilder jsonItems = new StringBuilder();
                    jsonItems.append("[");

                    boolean first = true;

                    while (i.moveToNext()) {

                        if (!first) jsonItems.append(",");
                        first = false;

                        jsonItems.append("{")
                                .append("\"variety_id\":").append(i.getInt(0)).append(",")
                                .append("\"variety_name\":\"").append(i.getString(1)).append("\",")
                                .append("\"quantity\":").append(i.getInt(2))
                                .append("}");
                    }

                    i.close();
                    jsonItems.append("]");

                    String urlStr =
                            "https://www.aaagrowers.co.ke/inventory/summers/api/scan_back_summer_process.php";

                    conn = (HttpURLConnection) new URL(urlStr).openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

                    conn.setRequestProperty(
                            "Content-Type",
                            "application/x-www-form-urlencoded"
                    );

                    String postData =
                            "serial=" + URLEncoder.encode(serial, "UTF-8") +
                                    "&bucket_name=" + URLEncoder.encode(bucket, "UTF-8") +
                                    "&farm=" + URLEncoder.encode(farm, "UTF-8") +
                                    "&length=" + length +
                                    "&user_id=99" +
                                    "&source=floor" +
                                    "&varieties_json=" + URLEncoder.encode(jsonItems.toString(), "UTF-8");

                    Log.d("SCAN_BACK_DEBUG", "POST DATA: " + postData);

                    OutputStream os = conn.getOutputStream();
                    os.write(postData.getBytes());
                    os.flush();
                    os.close();

                    int code = conn.getResponseCode();

                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(
                                    code >= 200 && code < 400
                                            ? conn.getInputStream()
                                            : conn.getErrorStream()
                            )
                    );

                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }

                    br.close();

                    Log.d("SCAN_BACK_DEBUG", "HTTP " + code);
                    Log.d("SCAN_BACK_DEBUG", "Response: " + response);

                    if (code == 200 && response.toString().contains("success")) {

                        db.getWritableDatabase().execSQL(
                                "UPDATE qr_fix_header SET synced=1 WHERE synced=0"
                        );

                        db.getWritableDatabase().execSQL(
                                "UPDATE qr_fix_items SET synced=1 WHERE synced=0"
                        );

                        handler.post(() ->
                                Toast.makeText(ctx,
                                        "Scan-back synced successfully",
                                        Toast.LENGTH_LONG).show()
                        );

                    } else {

                        handler.post(() ->
                                Toast.makeText(ctx,
                                        "Server rejected data",
                                        Toast.LENGTH_LONG).show()
                        );
                    }
                }

                h.close();

                if (headersFound == 0) {
                    handler.post(() ->
                            Toast.makeText(ctx,
                                    "No scan-back data",
                                    Toast.LENGTH_SHORT).show()
                    );
                }

            } catch (Exception e) {

                Log.e("SCAN_BACK_DEBUG", "ERROR", e);

                handler.post(() ->
                        Toast.makeText(ctx,
                                "Sync failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );

            } finally {
                if (conn != null) conn.disconnect();
            }

        }).start();
    }
}