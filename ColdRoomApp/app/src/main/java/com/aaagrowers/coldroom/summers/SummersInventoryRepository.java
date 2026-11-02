package com.aaagrowers.coldroom.summers;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class SummersInventoryRepository {

    public static void syncInventory(Context ctx, String farm) {

        HttpURLConnection conn = null;

        try {

            String encodedFarm =
                    URLEncoder.encode(farm, "UTF-8");

            URL url = new URL(
                    "https://www.aaagrowers.co.ke/" +
                            "inventory/summers/api/" +
                            "get_summers_inventory.php?farm="
                            + encodedFarm
            );

            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            StringBuilder sb = new StringBuilder();

            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            br.close();

            String response = sb.toString();

            Log.d("SUMMERS_API", response);

            JSONObject obj = new JSONObject(response);

            JSONArray arr = obj.getJSONArray("data");

            SummersDbHelper db =
                    new SummersDbHelper(ctx);

            // 🔥 CLEAR ONLY THIS FARM
            db.clearInventory(farm);

            for (int i = 0; i < arr.length(); i++) {

                JSONObject o = arr.getJSONObject(i);

                db.insertInventory(
                        o.getString("qr_code"),
                        o.getString("variety_name"),
                        o.getInt("quantity"),
                        o.getString("created_at"),
                        o.getString("farm")
                );
            }

            Log.d("SUMMERS_SYNC",
                    "Inventory synced: " + arr.length());

        } catch (Exception e) {

            Log.e("SUMMERS_SYNC",
                    "Sync Error", e);

        } finally {

            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}