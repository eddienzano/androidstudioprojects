package com.aaagrowers.coldroom.summers;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SummersInventoryFixRepository {

    private static final String API_URL =
            "https://www.aaagrowers.co.ke/inventory/api/upload_inventory_fixes.php";

    public static void sendToServer(Context context) {

        new Thread(() -> {

            try {

                SummersDbHelper db =
                        new SummersDbHelper(context);

                Cursor c =
                        db.getPendingInventoryFixes();

                JSONArray arr =
                        new JSONArray();

                while (c.moveToNext()) {

                    JSONObject obj =
                            new JSONObject();

                    obj.put(
                            "batch_id",
                            c.getString(
                                    c.getColumnIndexOrThrow("batch_id")
                            )
                    );

                    obj.put(
                            "old_variety",
                            c.getString(
                                    c.getColumnIndexOrThrow("old_variety")
                            )
                    );

                    obj.put(
                            "old_qty",
                            c.getInt(
                                    c.getColumnIndexOrThrow("old_qty")
                            )
                    );

                    obj.put(
                            "new_variety",
                            c.getString(
                                    c.getColumnIndexOrThrow("new_variety")
                            )
                    );

                    obj.put(
                            "new_qty",
                            c.getInt(
                                    c.getColumnIndexOrThrow("new_qty")
                            )
                    );

                    arr.put(obj);
                }

                c.close();

                if (arr.length() == 0)
                    return;

                JSONObject payload =
                        new JSONObject();

                payload.put("fixes", arr);

                URL url =
                        new URL(API_URL);

                HttpURLConnection conn =
                        (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                OutputStream os =
                        conn.getOutputStream();

                os.write(
                        payload.toString().getBytes()
                );

                os.flush();
                os.close();

                int code =
                        conn.getResponseCode();

                BufferedReader br =
                        new BufferedReader(
                                new InputStreamReader(
                                        conn.getInputStream()
                                )
                        );

                StringBuilder sb = new StringBuilder();
                String line;

                while((line = br.readLine()) != null){
                    sb.append(line);
                }

                JSONObject response =
                        new JSONObject(sb.toString());

                if(response.optBoolean("success")){

                    db.markInventoryFixesSynced();

                }

                conn.disconnect();

            } catch (Exception ex) {

                Log.e(
                        "FIX_SYNC",
                        ex.toString()
                );
            }

        }).start();
    }
}