package com.aaagrowers.coldroom.stocks;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class VarietyRepository {

    public static void syncVarieties(Context ctx, String farm){
        try{
            // Pass farm as query parameter
            String api = "https://www.aaagrowers.co.ke/inventory/fetch_varieties.php?farm=" + farm;

            URL url = new URL(api);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while((line=reader.readLine())!=null) sb.append(line);
            reader.close();

            JSONArray arr = new JSONArray(sb.toString());
            VarietyDbHelper helper = new VarietyDbHelper(ctx);
            SQLiteDatabase db = helper.getWritableDatabase();

            db.beginTransaction();
            try{
                db.delete("variety",null,null);

                for(int i=0;i<arr.length();i++){
                    JSONObject v = arr.getJSONObject(i);
                    ContentValues cv = new ContentValues();
                    cv.put("variety_id",v.getInt("VarietyId"));
                    cv.put("variety_name",v.getString("VarietyName"));
                    db.insert("variety",null,cv);
                }

                db.setTransactionSuccessful();
            }finally{
                db.endTransaction();
                db.close();
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}