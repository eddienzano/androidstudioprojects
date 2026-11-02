package com.aaagrowers.coldroom.summers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SummersVarietySyncRepository {

    public static void sync(Context ctx) {

        new Thread(() -> {

            try {

                SummersDbHelper db =
                        new SummersDbHelper(ctx);

                URL url = new URL(
                        "https://www.aaagrowers.co.ke/inventory/summers/api/sync_summers_varieties.php"
                );

                HttpURLConnection conn =
                        (HttpURLConnection) url.openConnection();

                BufferedReader br =
                        new BufferedReader(
                                new InputStreamReader(
                                        conn.getInputStream()
                                )
                        );

                StringBuilder sb =
                        new StringBuilder();

                String line;

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                br.close();

                JSONArray arr =
                        new JSONArray(sb.toString());

                db.clearVarieties();

                for (int i = 0; i < arr.length(); i++) {

                    JSONObject o =
                            arr.getJSONObject(i);

                    db.insertVariety(
                            o.getInt("VarietyId"),
                            o.getString("Variety"),
                            o.optString("Code"),
                            o.optString("Type"),
                            o.optString("Colour"),
                            o.optString("FlowerTypeId")
                    );
                }

                new Handler(Looper.getMainLooper())
                        .post(() ->
                                Toast.makeText(
                                        ctx,
                                        "Varieties synced: " + arr.length(),
                                        Toast.LENGTH_LONG
                                ).show()
                        );

            } catch (Exception e) {

                e.printStackTrace();

                new Handler(Looper.getMainLooper())
                        .post(() ->
                                Toast.makeText(
                                        ctx,
                                        "Variety sync failed",
                                        Toast.LENGTH_LONG
                                ).show()
                        );
            }

        }).start();
    }
}