package com.aaagrowers.coldroom.stocks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StockRepository {

    private static final String API_URL =
            "https://www.aaagrowers.co.ke/inventory/api/scan_receive.php";
    private static final String FIXED_API_URL =
            "https://www.aaagrowers.co.ke/inventory/scan_back_coldroom_process.php";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    /* ==============================
       CHECK DUPLICATE IN PENDING SCANS
    ============================== */
    public static boolean alreadyScannedPending(Context ctx, String qr) {

        StockDbHelper helper = new StockDbHelper(ctx);
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT qr_code FROM pending_scans WHERE qr_code=?",
                new String[]{qr}
        );

        boolean exists = c.moveToFirst();

        c.close();
        db.close();

        return exists;
    }

    /* ==============================
       SAVE TO PENDING SCANS (ALWAYS)
    ============================== */
    public static void saveToPending(Context ctx, String qr, boolean isDuplicate) {

        StockDbHelper helper = new StockDbHelper(ctx);
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("qr_code", qr);
        cv.put("is_duplicate", isDuplicate ? 1 : 0);
        cv.put("synced", 0);

        // Insert or ignore if it already exists
        db.insertWithOnConflict(
                "pending_scans",
                null,
                cv,
                SQLiteDatabase.CONFLICT_IGNORE
        );

        db.close();

        // Feedback
        if (isDuplicate)
            toast(ctx, "Duplicate detected ⚠");
        else
            toast(ctx, "Saved ✓");
    }

    /* ==============================
       SAVE OFFLINE (calls saveToPending)
    ============================== */
    public static void saveOffline(Context ctx, String qr, boolean isDuplicate) {
        saveToPending(ctx, qr, isDuplicate);
    }

    /* ==============================
       SAVE FIXED SCAN (UNKNOWN QR)
    ============================== */
    public static void saveFixedScan(
            Context ctx,
            String qr,
            int varietyId,
            String varietyName,
            int qty,
            String farm
    ) {

        StockDbHelper helper = new StockDbHelper(ctx);
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("qr_code", qr);
        cv.put("variety_id", varietyId);
        cv.put("variety_name", varietyName);
        cv.put("quantity", qty);
        cv.put("farm", farm);
        cv.put("synced", 0);

        db.insert("fixed_scans", null, cv);

        // Also add to pending_scans for duplicate detection next session
        saveToPending(ctx, qr, false);

        db.close();
        toast(ctx, "Fixed item saved ✓");
    }

    /* ==============================
       MARK AS SCANNED (for stock_take)
    ============================== */
    public static void markAsScanned(Context ctx, String qr) {

        StockDbHelper helper = new StockDbHelper(ctx);
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("qr_code", qr);

        db.insertWithOnConflict(
                "stock_take",
                null,
                cv,
                SQLiteDatabase.CONFLICT_IGNORE
        );

        db.close();
    }

    /* ==============================
       SEND ALL TO SERVER
    ============================== */
    public static void sendAll(Context ctx) {
        executor.execute(() -> {

            StockDbHelper helper = new StockDbHelper(ctx);
            SQLiteDatabase db = helper.getWritableDatabase();

            int successFixed = 0;
            int successPending = 0;

            // 1️⃣ Send fixed scans
            Cursor cFixed = db.rawQuery(
                    "SELECT id, qr_code, variety_id, variety_name, quantity, farm FROM fixed_scans WHERE synced=0",
                    null
            );

            while (cFixed.moveToNext()) {
                int id = cFixed.getInt(0);
                String qr = cFixed.getString(1);
                int varietyId = cFixed.getInt(2);
                String varietyName = cFixed.getString(3);
                int qty = cFixed.getInt(4);
                String farmStored = cFixed.getString(5);

                if (sendFixedToServer(ctx, qr, varietyId, varietyName, qty, farmStored)) {
                    // Mark fixed scan as synced
                    ContentValues cv = new ContentValues();
                    cv.put("synced", 1);
                    db.update("fixed_scans", cv, "id=?", new String[]{String.valueOf(id)});

                    // Insert into stock_take
                    ContentValues stockCv = new ContentValues();
                    stockCv.put("qr_code", qr);
                    db.insertWithOnConflict("stock_take", null, stockCv, SQLiteDatabase.CONFLICT_IGNORE);

                    // Queue in pending_scans if missing
                    saveToPending(ctx, qr, false);

                    successFixed++;
                }
            }

            cFixed.close();

            // 2️⃣ Send pending scans
            Cursor cPending = db.rawQuery(
                    "SELECT id, qr_code FROM pending_scans WHERE synced=0",
                    null
            );

            while (cPending.moveToNext()) {
                int id = cPending.getInt(0);
                String qr = cPending.getString(1);

                if (sendToServer(qr)) {
                    ContentValues cv = new ContentValues();
                    cv.put("synced", 1);
                    db.update("pending_scans", cv, "id=?", new String[]{String.valueOf(id)});
                    successPending++;
                }
            }

            cPending.close();
            db.close();

            toast(ctx, "Uploaded " + successFixed + " fixed scans + " + successPending + " pending scans");
        });
    }

    /* ==============================
       API CALLS
    ============================== */
    private static boolean sendToServer(String qrCode) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            String post =
                    "qr_code=" + URLEncoder.encode(qrCode, "UTF-8") +
                            "&user_id=android_user";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(post.getBytes());
            }

            int code = conn.getResponseCode();
            InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();

            String response;
            try (java.util.Scanner sc = new java.util.Scanner(is).useDelimiter("\\A")) {
                response = sc.hasNext() ? sc.next() : "";
            }

            if (code == 200 && response.trim().startsWith("{")) {
                JSONObject obj = new JSONObject(response);
                return obj.optBoolean("success", false);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private static boolean sendFixedToServer(
            Context ctx,
            String qr,
            int varietyId,
            String varietyName,
            int qty,
            String farmStored
    ) {
        try {
            java.util.Map<String, String> qrData = parseQr(qr);
            String serial = qrData.getOrDefault("serial", "");
            String bucketName = qrData.getOrDefault("bucket_name", "");
            String farm = qrData.getOrDefault("farm", farmStored);
            String length = qrData.getOrDefault("length", "");
            String coldroom = "1";

            URL url = new URL(FIXED_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String postData =
                    "qr_code=" + URLEncoder.encode(qr, "UTF-8") +
                            "&serial=" + URLEncoder.encode(serial, "UTF-8") +
                            "&bucket_name=" + URLEncoder.encode(bucketName, "UTF-8") +
                            "&farm=" + URLEncoder.encode(farm, "UTF-8") +
                            "&length=" + URLEncoder.encode(length, "UTF-8") +
                            "&quantity=" + URLEncoder.encode(String.valueOf(qty), "UTF-8") +
                            "&coldroom=" + URLEncoder.encode(coldroom, "UTF-8") +
                            "&variety_id=" + URLEncoder.encode(String.valueOf(varietyId), "UTF-8") +
                            "&variety_name=" + URLEncoder.encode(varietyName, "UTF-8") +
                            "&user_id=android_user" +
                            "&format=json";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes());
            }

            int code = conn.getResponseCode();
            InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();

            String response;
            try (java.util.Scanner sc = new java.util.Scanner(is).useDelimiter("\\A")) {
                response = sc.hasNext() ? sc.next() : "";
            }

            if (response.trim().startsWith("{")) {
                JSONObject obj = new JSONObject(response);
                return obj.optBoolean("success", false);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private static java.util.Map<String, String> parseQr(String qrText) {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "Serial: *([^|]+)\\| *Bucket: *([^|]+)\\| *Farm: *([^|]+)\\| *Length: *([^|]+)",
                java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(qrText);
        if (matcher.find()) {
            map.put("serial", matcher.group(1).trim());
            map.put("bucket_name", matcher.group(2).trim());
            map.put("farm", matcher.group(3).trim());
            map.put("length", matcher.group(4).trim());
        }
        return map;
    }

    private static void toast(Context ctx, String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show());
    }

    public static void removeFromPending(Context ctx, String qr){

        StockDbHelper helper = new StockDbHelper(ctx);
        SQLiteDatabase db = helper.getWritableDatabase();

        db.delete("pending_scans", "qr_code=?", new String[]{qr});

        db.close();
    }
}