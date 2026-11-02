package com.aaagrowers.coldroom.stocks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GradedStockRepository {

    private static final String TAG = "GradedStockRepo";
    private static final String INVENTORY_URL =
            "https://www.aaagrowers.co.ke/inventory/api/api_get_graded_inventory.php?farm=";
    private static final String UPLOAD_URL =
            "https://www.aaagrowers.co.ke/inventory/api/api_upload_graded_scans2.php";
    private static final String FIX_UPLOAD_URL =
            "https://www.aaagrowers.co.ke/inventory/api/api_upload_graded_fixes.php";

    private GradedStockRepository() { }

    public static void syncGradedStock(Context ctx, int farmId) {
        HttpURLConnection conn = null;
        GradedStockDbHelper helper = null;
        SQLiteDatabase db = null;
        try {
            conn = (HttpURLConnection) new URL(INVENTORY_URL + farmId).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Inventory sync HTTP error: " + conn.getResponseCode());
                return;
            }

            String response = readStream(conn);
            JSONObject json = new JSONObject(response);
            if (!"success".equalsIgnoreCase(json.optString("status"))) {
                Log.e(TAG, "Inventory API did not return success: " + response);
                return;
            }

            JSONArray data = json.getJSONArray("data");
            helper = new GradedStockDbHelper(ctx);
            db = helper.getWritableDatabase();
            db.beginTransaction();
            try {
                db.delete("graded_stock", "farm_id = ?", new String[]{String.valueOf(farmId)});

                for (int i = 0; i < data.length(); i++) {
                    JSONObject item = data.getJSONObject(i);
                    ContentValues cv = new ContentValues();
                    cv.put("qr", item.optString("qr", ""));
                    cv.put("serial", item.optString("serial", ""));
                    cv.put("bucket_name", item.optString("bucket_name", ""));
                    cv.put("farm_id", item.optInt("farm_id", farmId));
                    cv.put("variety_id", item.optInt("variety_id", 0));
                    cv.put("variety_name", item.optString("variety_name", ""));
                    cv.put("length", item.optInt("length", 0));
                    cv.put("remaining_bunches", item.optInt("remaining_bunches", 0));
                    cv.put("stems_per_bunch", item.optInt("stems_per_bunch", 0));
                    cv.put("graded_at", item.optString("graded_at", ""));

                    long result = db.insertWithOnConflict(
                            "graded_stock", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                    if (result == -1) {
                        throw new Exception("Failed to insert QR " + item.optString("qr"));
                    }
                }

                // Pending fixes take precedence over downloaded inventory until server confirms them.
                reapplyPendingFixes(db, farmId);
                db.setTransactionSuccessful();
                Log.d(TAG, "Synced " + data.length() + " inventory rows for farm " + farmId);
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing graded stock", e);
        } finally {
            if (db != null && db.isOpen()) db.close();
            if (helper != null) helper.close();
            if (conn != null) conn.disconnect();
        }
    }

    private static void reapplyPendingFixes(SQLiteDatabase db, int farmId) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT qr, variety_id, variety_name, length, " +
                    "remaining_bunches, stems_per_bunch FROM graded_stock_fixes " +
                    "WHERE synced = 0 AND farm_id = ?", new String[]{String.valueOf(farmId)});
            while (c.moveToNext()) {
                ContentValues values = new ContentValues();
                values.put("variety_id", c.getInt(1));
                values.put("variety_name", c.getString(2));
                values.put("length", c.getInt(3));
                values.put("remaining_bunches", c.getInt(4));
                values.put("stems_per_bunch", c.getInt(5));
                db.update("graded_stock", values, "qr = ? AND farm_id = ?",
                        new String[]{c.getString(0), String.valueOf(farmId)});
            }
        } finally {
            if (c != null) c.close();
        }
    }

    public static JSONObject getGradedStock(Context ctx, String qr) {
        GradedStockDbHelper helper = null;
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            helper = new GradedStockDbHelper(ctx);
            db = helper.getReadableDatabase();
            c = db.rawQuery("SELECT * FROM graded_stock WHERE qr = ?", new String[]{qr});
            if (!c.moveToFirst()) return null;

            JSONObject obj = new JSONObject();
            obj.put("qr", c.getString(c.getColumnIndexOrThrow("qr")));
            obj.put("variety_id", c.getInt(c.getColumnIndexOrThrow("variety_id")));
            obj.put("variety", c.getString(c.getColumnIndexOrThrow("variety_name")));
            obj.put("length", c.getInt(c.getColumnIndexOrThrow("length")));
            obj.put("remaining_bunches", c.getInt(c.getColumnIndexOrThrow("remaining_bunches")));
            obj.put("stems_per_bunch", c.getInt(c.getColumnIndexOrThrow("stems_per_bunch")));
            obj.put("graded_at", c.getString(c.getColumnIndexOrThrow("graded_at")));
            obj.put("farm_id", c.getInt(c.getColumnIndexOrThrow("farm_id")));
            obj.put("serial", c.getString(c.getColumnIndexOrThrow("serial")));
            obj.put("bucket_name", c.getString(c.getColumnIndexOrThrow("bucket_name")));
            return obj;
        } catch (Exception e) {
            Log.e(TAG, "Error getting graded stock for QR=" + qr, e);
            return null;
        } finally {
            if (c != null) c.close();
            if (db != null && db.isOpen()) db.close();
            if (helper != null) helper.close();
        }
    }

    public static int getGradedStockCount(Context ctx) {
        GradedStockDbHelper helper = null;
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            helper = new GradedStockDbHelper(ctx);
            db = helper.getReadableDatabase();
            c = db.rawQuery("SELECT COUNT(*) FROM graded_stock", null);
            return c.moveToFirst() ? c.getInt(0) : 0;
        } catch (Exception e) {
            Log.e(TAG, "Error counting graded stock", e);
            return 0;
        } finally {
            if (c != null) c.close();
            if (db != null && db.isOpen()) db.close();
            if (helper != null) helper.close();
        }
    }

    public static boolean saveScan(Context ctx, String qr, int farmId) {
        GradedStockDbHelper helper = null;
        SQLiteDatabase db = null;
        try {
            helper = new GradedStockDbHelper(ctx);
            db = helper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("qr", qr);
            cv.put("farm_id", farmId);
            cv.put("scanned_at", String.valueOf(System.currentTimeMillis()));
            cv.put("synced", 0);
            long id = db.insert("graded_scans", null, cv);
            if (id == -1) Log.e(TAG, "Failed to save scan: " + qr);
            return id != -1;
        } catch (Exception e) {
            Log.e(TAG, "Error saving scan", e);
            return false;
        } finally {
            if (db != null && db.isOpen()) db.close();
            if (helper != null) helper.close();
        }
    }

    /** Upload ordinary scans. Returns true only if all queued rows were accepted/recognized. */
    public static boolean uploadScans(Context ctx, int farmId) {
        GradedStockDbHelper helper = null;
        SQLiteDatabase db = null;
        HttpURLConnection conn = null;
        Cursor c = null;
        try {
            helper = new GradedStockDbHelper(ctx);
            db = helper.getWritableDatabase();
            c = db.rawQuery("SELECT id, qr, farm_id, scanned_at FROM graded_scans " +
                            "WHERE synced = 0 AND farm_id = ? ORDER BY id ASC",
                    new String[]{String.valueOf(farmId)});

            JSONArray data = new JSONArray();
            List<Integer> ids = new ArrayList<>();
            List<String> qrs = new ArrayList<>();
            while (c.moveToNext()) {
                JSONObject row = new JSONObject();
                row.put("qr", c.getString(1));
                row.put("farm_id", c.getInt(2));
                row.put("scanned_at", c.getString(3));
                data.put(row);
                ids.add(c.getInt(0));
                qrs.add(c.getString(1));
            }
            c.close(); c = null;
            if (data.length() == 0) return true;

            JSONObject payload = new JSONObject();
            payload.put("data", data);
            conn = openPost(UPLOAD_URL, payload);
            int code = conn.getResponseCode();
            String response = readStream(conn);
            if (code != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Scan upload HTTP " + code + ": " + response);
                return false;
            }

            JSONObject result = new JSONObject(response);
            JSONArray results = result.optJSONArray("results");
            if (results == null) {
                Log.e(TAG, "Scan API did not return per-record results: " + response);
                return false;
            }

            Set<String> accepted = new HashSet<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject r = results.optJSONObject(i);
                if (r == null) continue;
                String qr = r.optString("qr", "");
                String status = r.optString("status", "");
                if ("INSERTED".equalsIgnoreCase(status)
                        || "ALREADY_SCANNED".equalsIgnoreCase(status)) {
                    accepted.add(qr);
                }
            }

            db.beginTransaction();
            try {
                for (int i = 0; i < ids.size(); i++) {
                    if (accepted.contains(qrs.get(i))) {
                        ContentValues cv = new ContentValues();
                        cv.put("synced", 1);
                        db.update("graded_scans", cv, "id = ? AND farm_id = ?",
                                new String[]{String.valueOf(ids.get(i)), String.valueOf(farmId)});
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            return accepted.size() >= new HashSet<>(qrs).size();
        } catch (Exception e) {
            Log.e(TAG, "Error uploading scans", e);
            return false;
        } finally {
            if (c != null) c.close();
            if (db != null && db.isOpen()) db.close();
            if (helper != null) helper.close();
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Update local inventory and persist the exact correction for later upload.
     * This queue is not cleared until the server confirms the correction.
     */
    public static boolean updateGradedStock(Context ctx, String qr, int farmId,
                                            int varietyId, String varietyName, int length,
                                            int remainingBunches, int stemsPerBunch) {
        GradedStockDbHelper helper = null;
        SQLiteDatabase db = null;
        try {
            helper = new GradedStockDbHelper(ctx);
            db = helper.getWritableDatabase();
            db.beginTransaction();
            try {
                ContentValues inventory = new ContentValues();
                inventory.put("variety_id", varietyId);
                inventory.put("variety_name", varietyName);
                inventory.put("length", length);
                inventory.put("remaining_bunches", remainingBunches);
                inventory.put("stems_per_bunch", stemsPerBunch);
                int changed = db.update("graded_stock", inventory,
                        "qr = ? AND farm_id = ?",
                        new String[]{qr, String.valueOf(farmId)});
                if (changed == 0) return false;

                ContentValues fix = new ContentValues();
                fix.put("qr", qr);
                fix.put("farm_id", farmId);
                fix.put("variety_id", varietyId);
                fix.put("variety_name", varietyName);
                fix.put("length", length);
                fix.put("remaining_bunches", remainingBunches);
                fix.put("stems_per_bunch", stemsPerBunch);
                fix.put("queued_at", String.valueOf(System.currentTimeMillis()));
                fix.put("synced", 0);
                fix.put("last_error", "");
                long queued = db.insertWithOnConflict("graded_stock_fixes", null, fix,
                        SQLiteDatabase.CONFLICT_REPLACE);
                if (queued == -1) return false;
                db.setTransactionSuccessful();
                Log.d(TAG, "Local correction saved and queued: " + qr);
                return true;
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error correcting graded stock QR=" + qr, e);
            return false;
        } finally {
            if (db != null && db.isOpen()) db.close();
            if (helper != null) helper.close();
        }
    }

    /** Upload corrections and mark only server-confirmed corrections as synced. */
    public static boolean uploadInventoryFixes(Context ctx, int farmId) {
        GradedStockDbHelper helper = null;
        SQLiteDatabase db = null;
        HttpURLConnection conn = null;
        Cursor c = null;
        try {
            helper = new GradedStockDbHelper(ctx);
            db = helper.getWritableDatabase();
            c = db.rawQuery("SELECT id, qr, farm_id, variety_id, variety_name, length, " +
                            "remaining_bunches, stems_per_bunch FROM graded_stock_fixes " +
                            "WHERE synced = 0 AND farm_id = ? ORDER BY id ASC",
                    new String[]{String.valueOf(farmId)});

            JSONArray data = new JSONArray();
            List<Integer> ids = new ArrayList<>();
            List<String> qrs = new ArrayList<>();
            while (c.moveToNext()) {
                JSONObject row = new JSONObject();
                row.put("qr", c.getString(1));
                row.put("farm_id", c.getInt(2));
                row.put("variety_id", c.getInt(3));
                row.put("variety_name", c.getString(4));
                row.put("length", c.getInt(5));
                row.put("remaining_bunches", c.getInt(6));
                row.put("stems_per_bunch", c.getInt(7));
                data.put(row);
                ids.add(c.getInt(0));
                qrs.add(c.getString(1));
            }
            c.close(); c = null;
            if (data.length() == 0) return true;

            JSONObject payload = new JSONObject();
            payload.put("data", data);
            conn = openPost(FIX_UPLOAD_URL, payload);
            int code = conn.getResponseCode();
            String response = readStream(conn);
            if (code != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Fix upload HTTP " + code + ": " + response);
                return false;
            }

            JSONObject result = new JSONObject(response);
            JSONArray results = result.optJSONArray("results");
            if (results == null) {
                Log.e(TAG, "Fix API did not return per-record results: " + response);
                return false;
            }

            Set<String> accepted = new HashSet<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject r = results.optJSONObject(i);
                if (r == null) continue;
                String qr = r.optString("qr", "");
                String status = r.optString("status", "");
                if ("UPDATED".equalsIgnoreCase(status)
                        || "UNCHANGED".equalsIgnoreCase(status)) {
                    accepted.add(qr);
                } else {
                    ContentValues error = new ContentValues();
                    error.put("last_error", r.optString("message", status));
                    db.update("graded_stock_fixes", error,
                            "qr = ? AND farm_id = ? AND synced = 0",
                            new String[]{qr, String.valueOf(farmId)});
                }
            }

            db.beginTransaction();
            try {
                for (int i = 0; i < ids.size(); i++) {
                    if (accepted.contains(qrs.get(i))) {
                        ContentValues cv = new ContentValues();
                        cv.put("synced", 1);
                        cv.put("last_error", "");
                        db.update("graded_stock_fixes", cv,
                                "id = ? AND farm_id = ?",
                                new String[]{String.valueOf(ids.get(i)), String.valueOf(farmId)});
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            return accepted.size() >= new HashSet<>(qrs).size();
        } catch (Exception e) {
            Log.e(TAG, "Error uploading inventory fixes", e);
            return false;
        } finally {
            if (c != null) c.close();
            if (db != null && db.isOpen()) db.close();
            if (helper != null) helper.close();
            if (conn != null) conn.disconnect();
        }
    }

    private static HttpURLConnection openPost(String urlString, JSONObject payload) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(20000);
        conn.setDoOutput(true);
        byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream out = conn.getOutputStream()) {
            out.write(bytes);
            out.flush();
        }
        return conn;
    }

    private static String readStream(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        java.io.InputStream stream = (code >= 200 && code < 400)
                ? conn.getInputStream() : conn.getErrorStream();
        if (stream == null) return "";
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) out.append(line);
        }
        return out.toString();
    }
}
