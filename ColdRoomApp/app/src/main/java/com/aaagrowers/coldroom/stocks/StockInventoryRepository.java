package com.aaagrowers.coldroom.stocks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class StockInventoryRepository {

    public static void syncInventory(Context ctx, String farm) {
        try {

            String apiUrl =
                    "https://www.aaagrowers.co.ke/inventory/api/api_get_qr_inventory.php?farm=" + farm;

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            int code = conn.getResponseCode();
            if (code != 200) return;

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(conn.getInputStream()));

            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null)
                sb.append(line);

            reader.close();
            conn.disconnect();

            JSONObject json = new JSONObject(sb.toString());

            if (!json.getString("status").equalsIgnoreCase("success"))
                return;

            JSONArray data = json.getJSONArray("data");

            StockInventoryDbHelper helper =
                    new StockInventoryDbHelper(ctx);

            SQLiteDatabase db = helper.getWritableDatabase();

            db.beginTransaction();

            try {

                db.delete("active_inventory", "farm=?", new String[]{farm});

                for (int i = 0; i < data.length(); i++) {

                    JSONObject item = data.getJSONObject(i);

                    ContentValues cv = new ContentValues();

                    cv.put("qr_code", item.getString("qr_code"));
                    cv.put("variety_name", item.getString("variety_name"));
                    cv.put("quantity", item.getInt("quantity"));
                    cv.put("intake_time", item.getString("intake_time"));
                    cv.put("farm", farm);

                    db.insertWithOnConflict(
                            "active_inventory",
                            null,
                            cv,
                            SQLiteDatabase.CONFLICT_REPLACE
                    );
                }

                db.setTransactionSuccessful();

            } finally {

                db.endTransaction();
                db.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ===============================
       GET INVENTORY RECORD
    =============================== */

    public static JSONObject getInventory(Context ctx, String qr) {

        try {

            StockInventoryDbHelper helper =
                    new StockInventoryDbHelper(ctx);

            SQLiteDatabase db = helper.getReadableDatabase();

            Cursor c = db.rawQuery(
                    "SELECT variety_name, quantity, farm, intake_time FROM active_inventory WHERE qr_code=?",
                    new String[]{qr}
            );

            if (c.moveToFirst()) {

                JSONObject obj = new JSONObject();

                obj.put("variety", c.getString(0));
                obj.put("quantity", c.getInt(1));
                obj.put("farm", c.getString(2));
                obj.put("intake_time", c.getString(3));

                c.close();
                db.close();

                return obj;
            }

            c.close();
            db.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static int getInventoryCount(Context ctx){

        int count = 0;

        try{

            StockInventoryDbHelper helper =
                    new StockInventoryDbHelper(ctx);

            SQLiteDatabase db = helper.getReadableDatabase();

            Cursor c = db.rawQuery(
                    "SELECT COUNT(*) FROM active_inventory",
                    null
            );

            if(c.moveToFirst())
                count = c.getInt(0);

            c.close();
            db.close();

        }catch(Exception e){
            e.printStackTrace();
        }

        return count;
    }
}