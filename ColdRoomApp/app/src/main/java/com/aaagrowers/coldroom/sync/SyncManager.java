package com.aaagrowers.coldroom.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.aaagrowers.coldroom.storage.FlowerDbHelper;
import com.aaagrowers.coldroom.sync.ApiClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SyncManager {

    private static final String TAG = "SyncManager";

    private final FlowerDbHelper db;
    private final ApiClient api;

    public SyncManager(Context context) {
        db = new FlowerDbHelper(context);
        api = new ApiClient();
    }

    // ==========================================================
    // ENTRY POINT
    // ==========================================================
    public void syncAll() {

        syncBoxFills();
        syncWarehouseScans();
        syncAvailableBoxes();   // 🔥 ADD THIS FIRST
    }
    /* ==========================================================
       BOX FILL SYNC (✅ FIXED COLUMN NAME)
       ========================================================== */
    private void syncBoxFills() {

        SQLiteDatabase rdb = db.getReadableDatabase();

        Cursor c = rdb.rawQuery(
                "SELECT " +
                        "c.box_id, " +
                        "b.qr_code, b.farm_id, " +
                        "c.variety_id, c.length, c.number_of_stems, " + // ✅ FIXED HERE
                        "c.updated_at " +
                        "FROM box_contents_local c " +
                        "JOIN boxes_local b ON b.id = c.box_id " +
                        "WHERE c.synced = 0",
                null
        );

        Log.d(TAG, "📦 Storage rows to sync = " + c.getCount());

        JSONArray payload = new JSONArray();
        List<Long> boxIds = new ArrayList<>();

        try {
            while (c.moveToNext()) {

                long boxId = c.getLong(0);

                JSONObject o = new JSONObject();
                o.put("box_id", boxId);
                o.put("qr_code", c.getString(1));
                o.put("farm_id", c.getInt(2));
                o.put("variety_id", c.getInt(3));
                o.put("length", c.getInt(4));
                o.put("number_of_stems", c.getInt(5)); // ✅ FIXED INDEX MATCH
                o.put("updated_at", c.getString(6));

                payload.put(o);
                boxIds.add(boxId);
            }

            if (payload.length() == 0) return;

            boolean ok = api.post("/sync/box_storage.php", payload);

            if (!ok) {
                Log.w(TAG, "❌ Storage sync failed");
                return;
            }

            SQLiteDatabase wdb = db.getWritableDatabase();
            wdb.beginTransaction();

            try {
                for (long boxId : boxIds) {
                    wdb.execSQL(
                            "UPDATE box_contents_local SET synced=1 WHERE box_id=?",
                            new Object[]{boxId}
                    );
                }

                wdb.setTransactionSuccessful();
                Log.d(TAG, "✅ Storage sync success (" + boxIds.size() + ")");

            } finally {
                wdb.endTransaction();
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Storage sync error", e);
        } finally {
            c.close();
        }
    }

    /* ==========================================================
       WAREHOUSE SCAN SYNC
       ========================================================== */
    private void syncWarehouseScans() {

        SQLiteDatabase rdb = db.getReadableDatabase();

        Cursor sessions = rdb.rawQuery(
                "SELECT * FROM warehouse_scan_sessions_local " +
                        "WHERE status='CLOSED'",
                null
        );

        while (sessions.moveToNext()) {

            long localSessionId =
                    sessions.getLong(sessions.getColumnIndexOrThrow("id"));

            try {
                JSONObject payload = new JSONObject();

                payload.put("farm_id",
                        sessions.getInt(sessions.getColumnIndexOrThrow("farm_id")));
                payload.put("scan_date",
                        sessions.getString(sessions.getColumnIndexOrThrow("scan_date")));
                payload.put("started_at",
                        sessions.getString(sessions.getColumnIndexOrThrow("started_at")));
                payload.put("ended_at",
                        sessions.getString(sessions.getColumnIndexOrThrow("ended_at")));

                // =========================
                // Attach scans
                // =========================
                JSONArray scans = new JSONArray();

                Cursor c = rdb.rawQuery(
                        "SELECT qr_code, scanned_at " +
                                "FROM warehouse_scans_local " +
                                "WHERE local_session_id=?",
                        new String[]{String.valueOf(localSessionId)}
                );

                while (c.moveToNext()) {
                    JSONObject s = new JSONObject();
                    s.put("qr_code", c.getString(0));
                    s.put("scanned_at", c.getString(1));
                    scans.put(s);
                }
                c.close();

                if (scans.length() == 0) {
                    Log.w(TAG, "⚠️ No scans for session " + localSessionId);
                    continue;
                }

                payload.put("scans", scans);

                boolean ok = api.postObject(
                        "/sync/warehouse_stock.php",
                        payload
                );

                if (!ok) throw new Exception("Warehouse sync failed");

                // =========================
                // Mark session as SYNCED
                // =========================
                ContentValues v = new ContentValues();
                v.put("status", "SYNCED");

                db.getWritableDatabase().update(
                        "warehouse_scan_sessions_local",
                        v,
                        "id=?",
                        new String[]{String.valueOf(localSessionId)}
                );

                Log.d(TAG, "✅ Warehouse session synced: " + localSessionId);

            } catch (Exception e) {
                Log.e(TAG, "❌ Warehouse sync failed for session " + localSessionId, e);
            }
        }

        sessions.close();
    }

    private void syncAvailableBoxes() {

        try {
            JSONArray response = api.getArray("/sync/get_boxes.php");

            if (response == null || response.length() == 0) {
                Log.d(TAG, "📭 No boxes from server");
                return;
            }

            SQLiteDatabase dbw = db.getWritableDatabase();
            dbw.beginTransaction();

            try {
                for (int i = 0; i < response.length(); i++) {

                    JSONObject o = response.getJSONObject(i);

                    dbw.execSQL(
                            "INSERT OR IGNORE INTO boxes_local (id, qr_code, farm_id, status, synced) VALUES (?,?,?,?,1)",
                            new Object[]{
                                    o.getLong("id"),
                                    o.getString("qr_code"),
                                    o.getInt("farm_id"),
                                    o.getString("status") // should be ""
                            }
                    );
                }

                dbw.setTransactionSuccessful();
                Log.d(TAG, "📦 Boxes synced: " + response.length());

            } finally {
                dbw.endTransaction();
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Box download failed", e);
        }
    }
}