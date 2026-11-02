package com.yourapp.gradedstock;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.yourapp.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScanOutWipActivity extends AppCompatActivity {

    EditText qrInput;
    Button scanBtn;
    Button btnSyncNow;
    LinearLayout historyList;
    TextView resultBox;

    int farmId;

    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_scan_out_wip
        );

        farmId =
                getIntent().getIntExtra(
                        "farm_id",
                        0
                );

        qrInput =
                findViewById(
                        R.id.qrInput
                );

        scanBtn =
                findViewById(
                        R.id.scanBtn
                );

        historyList =
                findViewById(
                        R.id.historyList
                );

        resultBox =
                findViewById(
                        R.id.resultBox
                );

        btnSyncNow =
                findViewById(
                        R.id.btnSyncNow
                );

        addScannerWatcher();

        /*
         * ==========================================================
         * MANUAL / BARCODE INPUT
         * ==========================================================
         */

        qrInput.setOnEditorActionListener(
                (v, actionId, event) -> {

                    String qr =
                            qrInput
                                    .getText()
                                    .toString()
                                    .trim();

                    if (!qr.isEmpty()) {

                        processScan(qr);

                        qrInput.setText("");
                    }

                    return true;
                }
        );

        /*
         * ==========================================================
         * CAMERA SCANNER
         * ==========================================================
         */

        scanBtn.setOnClickListener(v -> {

            IntentIntegrator integrator =
                    new IntentIntegrator(
                            ScanOutWipActivity.this
                    );

            integrator.setPrompt(
                    "Scan Bucket QR"
            );

            integrator.setBeepEnabled(
                    true
            );

            integrator.setOrientationLocked(
                    false
            );

            integrator.initiateScan();
        });

        /*
         * ==========================================================
         * MANUAL SYNC
         * ==========================================================
         */

        btnSyncNow.setOnClickListener(
                v -> syncAll()
        );
    }

    @Override
    protected void onResume() {

        super.onResume();

        /*
         * Try to synchronize whenever this screen
         * becomes active.
         */
        syncAll();
    }

    // ============================================================
    // FULL SYNC
    // ============================================================

    private void syncAll() {

        new Thread(() -> {

            try {

                runOnUiThread(() ->
                        showResult(
                                "Syncing...",
                                true
                        )
                );

                /*
                 * --------------------------------------------------
                 * STEP 1
                 * --------------------------------------------------
                 *
                 * Push old scan-outs FIRST.
                 *
                 * This is critical for bucket reuse.
                 *
                 * If a bucket was completed locally and then reused,
                 * the old scan-out must reach the server before the
                 * new graded intake.
                 */
                boolean pushOk =
                        syncScanOutsInternal();

                if (!pushOk) {

                    runOnUiThread(() ->
                            showResult(
                                    "Scan-out sync failed. Try again.",
                                    false
                            )
                    );

                    return;
                }

                /*
                 * --------------------------------------------------
                 * STEP 2
                 * --------------------------------------------------
                 *
                 * Pull latest active stock.
                 */
                boolean pullOk =
                        syncActiveBuckets();

                if (pullOk) {

                    runOnUiThread(() ->
                            showResult(
                                    "Sync complete ✓",
                                    true
                            )
                    );

                } else {

                    runOnUiThread(() ->
                            showResult(
                                    "Active stock sync failed.",
                                    false
                            )
                    );
                }

            } catch (Exception e) {

                e.printStackTrace();

                runOnUiThread(() ->
                        showResult(
                                "Sync error.",
                                false
                        )
                );
            }

        }).start();
    }

    // ============================================================
    // PUSH SCAN-OUTS
    // ============================================================

    private boolean syncScanOutsInternal() {

        StockDbHelper helper = null;
        SQLiteDatabase db = null;
        Cursor c = null;

        try {

            helper =
                    new StockDbHelper(this);

            db =
                    helper.getWritableDatabase();

            c =
                    db.rawQuery(
                            "SELECT " +
                                    "id, " +
                                    "qr, " +
                                    "graded_stock_id, " +
                                    "farm_id, " +
                                    "action_type, " +
                                    "bunches_removed, " +
                                    "scanned_at, " +
                                    "device_id " +
                                    "FROM graded_out_local " +
                                    "WHERE synced=0 " +
                                    "ORDER BY id ASC",
                            null
                    );

            JSONArray arr =
                    new JSONArray();

            while (c.moveToNext()) {

                JSONObject o =
                        new JSONObject();

                o.put(
                        "id",
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "id"
                                )
                        )
                );

                o.put(
                        "qr",
                        c.getString(
                                c.getColumnIndexOrThrow(
                                        "qr"
                                )
                        )
                );

                o.put(
                        "graded_stock_id",
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "graded_stock_id"
                                )
                        )
                );

                o.put(
                        "farm_id",
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "farm_id"
                                )
                        )
                );

                o.put(
                        "action_type",
                        c.getString(
                                c.getColumnIndexOrThrow(
                                        "action_type"
                                )
                        )
                );

                o.put(
                        "bunches_removed",
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "bunches_removed"
                                )
                        )
                );

                o.put(
                        "scanned_at",
                        c.getString(
                                c.getColumnIndexOrThrow(
                                        "scanned_at"
                                )
                        )
                );

                String deviceId =
                        c.getString(
                                c.getColumnIndexOrThrow(
                                        "device_id"
                                )
                        );

                if (deviceId != null) {

                    o.put(
                            "device_id",
                            deviceId
                    );
                }

                arr.put(o);
            }

            if (c != null) {

                c.close();

                c = null;
            }

            /*
             * Nothing waiting for upload.
             */
            if (arr.length() == 0) {

                return true;
            }

            String url =
                    "https://www.aaagrowers.co.ke/inventory/graded/sync_scanouts.php";

            String response =
                    HttpHelper.postJson(
                            url,
                            arr.toString()
                    );

            if (
                    response == null ||
                            response.trim().isEmpty()
            ) {

                return false;
            }

            JSONObject respObj =
                    new JSONObject(
                            response
                    );

            /*
             * ======================================================
             * SUCCESSFUL RECORDS
             * ======================================================
             *
             * The server tells us exactly which local IDs
             * succeeded.
             */
            JSONArray successfulIds =
                    respObj.optJSONArray(
                            "successful_ids"
                    );

            /*
             * Failed records remain synced=0.
             */
            JSONArray failedIds =
                    respObj.optJSONArray(
                            "failed_ids"
                    );

            int successfulCount =
                    successfulIds != null
                            ? successfulIds.length()
                            : 0;

            int failedCount =
                    failedIds != null
                            ? failedIds.length()
                            : 0;

            /*
             * ======================================================
             * MARK SUCCESSFUL SCAN-OUTS
             * ======================================================
             */

            if (successfulIds != null) {

                for (
                        int i = 0;
                        i < successfulIds.length();
                        i++
                ) {

                    int localId =
                            successfulIds.getInt(i);

                    ContentValues values =
                            new ContentValues();

                    values.put(
                            "synced",
                            1
                    );

                    db.update(
                            "graded_out_local",
                            values,
                            "id=?",
                            new String[]{
                                    String.valueOf(
                                            localId
                                    )
                            }
                    );

                    /*
                     * Mark the corresponding locally released
                     * bucket as synchronized.
                     *
                     * This does NOT remove the release record.
                     * It remains useful for local history/state.
                     */
                    ContentValues releaseValues =
                            new ContentValues();

                    releaseValues.put(
                            "synced",
                            1
                    );

                    db.update(
                            "graded_released_qrs",
                            releaseValues,
                            "scanout_local_id=?",
                            new String[]{
                                    String.valueOf(
                                            localId
                                    )
                            }
                    );
                }
            }

            final int successFinal =
                    successfulCount;

            final int failedFinal =
                    failedCount;

            runOnUiThread(() -> {

                if (failedFinal == 0) {

                    showResult(
                            "Scan-outs synced: " +
                                    successFinal +
                                    " ✓",
                            true
                    );

                } else {

                    showResult(
                            "Synced: " +
                                    successFinal +
                                    " | Failed: " +
                                    failedFinal,
                            false
                    );
                }
            });

            /*
             * A server response was received, even if some
             * individual records failed.
             *
             * The failed records remain pending.
             */
            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;

        } finally {

            if (c != null) {

                try {

                    c.close();

                } catch (Exception ignored) {
                }
            }
        }
    }

    // ============================================================
    // PULL ACTIVE GRADED STOCK
    // ============================================================

    private boolean syncActiveBuckets() {

        SQLiteDatabase db = null;

        try {

            String url =
                    "https://www.aaagrowers.co.ke/inventory/graded/sync_active_graded.php?farm_id="
                            + farmId;

            String json =
                    HttpHelper.get(url);

            if (
                    json == null ||
                            json.trim().isEmpty()
            ) {

                return false;
            }

            JSONObject obj =
                    new JSONObject(json);

            if (
                    !obj.optBoolean(
                            "success",
                            false
                    )
            ) {

                return false;
            }

            JSONArray buckets =
                    obj.optJSONArray(
                            "buckets"
                    );

            if (buckets == null) {

                buckets =
                        new JSONArray();
            }

            StockDbHelper helper =
                    new StockDbHelper(this);

            db =
                    helper.getWritableDatabase();

            /*
             * The server response is authoritative for ACTIVE
             * stock.
             *
             * This is safe because scan-outs have already been
             * pushed above.
             */
            db.delete(
                    "graded_active_qrs",
                    null,
                    null
            );

            for (
                    int i = 0;
                    i < buckets.length();
                    i++
            ) {

                JSONObject b =
                        buckets.getJSONObject(i);

                int gradedStockId =
                        b.optInt(
                                "graded_stock_id",
                                0
                        );

                if (gradedStockId <= 0) {

                    continue;
                }

                ContentValues cv =
                        new ContentValues();

                cv.put(
                        "graded_stock_id",
                        gradedStockId
                );

                cv.put(
                        "qr",
                        b.optString(
                                "qr",
                                ""
                        )
                );

                cv.put(
                        "serial",
                        b.optString(
                                "serial",
                                ""
                        )
                );

                cv.put(
                        "bucket_name",
                        b.optString(
                                "bucket_name",
                                ""
                        )
                );

                cv.put(
                        "farm_id",
                        b.optInt(
                                "farm_id",
                                0
                        )
                );

                cv.put(
                        "length",
                        b.optInt(
                                "length",
                                0
                        )
                );

                cv.put(
                        "variety_id",
                        b.optInt(
                                "variety_id",
                                0
                        )
                );

                cv.put(
                        "bunches",
                        b.optInt(
                                "bunches",
                                0
                        )
                );

                cv.put(
                        "stems_per_bunch",
                        b.optInt(
                                "stems_per_bunch",
                                0
                        )
                );

                db.insert(
                        "graded_active_qrs",
                        null,
                        cv
                );
            }

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    // ============================================================
    // SCANNER WATCHER
    // ============================================================

    private void addScannerWatcher() {

        Runnable r = () -> {

            String qr =
                    qrInput
                            .getText()
                            .toString()
                            .trim();

            if (!qr.isEmpty()) {

                processScan(qr);

                qrInput.setText("");
            }
        };

        qrInput.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        handler.removeCallbacks(
                                r
                        );

                        if (s.length() > 6) {

                            handler.postDelayed(
                                    r,
                                    300
                            );
                        }
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );
    }

    // ============================================================
    // PROCESS SCAN
    // ============================================================

    private void processScan(
            String qr
    ) {

        qr =
                qr.trim();

        if (qr.isEmpty()) {

            return;
        }

        /*
         * Scan-out only operates on active stock.
         */
        if (!bucketExists(qr)) {

            showResult(
                    "Bucket not found in active stock",
                    false
            );

            return;
        }

        showDispatchDialog(qr);
    }

    // ============================================================
    // CHECK LOCAL ACTIVE BUCKET
    // ============================================================

    private boolean bucketExists(
            String qr
    ) {

        StockDbHelper helper =
                new StockDbHelper(this);

        SQLiteDatabase db =
                helper.getReadableDatabase();

        Cursor c =
                db.rawQuery(
                        "SELECT graded_stock_id " +
                                "FROM graded_active_qrs " +
                                "WHERE qr=? " +
                                "LIMIT 1",
                        new String[]{
                                qr
                        }
                );

        boolean exists =
                c.moveToFirst();

        c.close();

        return exists;
    }

    // ============================================================
    // DISPATCH DIALOG
    // ============================================================

    private void showDispatchDialog(
            String qr
    ) {

        int remaining =
                getBunchesForQr(qr);

        if (remaining <= 0) {

            showResult(
                    "No remaining bunches to dispatch",
                    false
            );

            return;
        }

        String[] options = {
                "Partial Dispatch (" +
                        remaining +
                        " remaining)",

                "Complete Dispatch"
        };

        new AlertDialog.Builder(this)
                .setTitle(
                        "Dispatch Type"
                )
                .setItems(
                        options,
                        (dialog, which) -> {

                            if (which == 0) {

                                askPartial(
                                        qr,
                                        remaining
                                );

                            } else {

                                saveOffline(
                                        qr,
                                        "COMPLETE",
                                        remaining
                                );
                            }
                        }
                )
                .show();
    }

    // ============================================================
    // PARTIAL DISPATCH
    // ============================================================

    private void askPartial(
            String qr,
            int maxQty
    ) {

        EditText input =
                new EditText(this);

        input.setInputType(
                android.text.InputType
                        .TYPE_CLASS_NUMBER
        );

        input.setHint(
                "Enter bunches removed (max " +
                        maxQty +
                        ")"
        );

        new AlertDialog.Builder(this)
                .setTitle(
                        "Partial Dispatch"
                )
                .setView(input)
                .setPositiveButton(
                        "Save",
                        (dialog, which) -> {

                            String value =
                                    input
                                            .getText()
                                            .toString()
                                            .trim();

                            if (value.isEmpty()) {

                                Toast.makeText(
                                        this,
                                        "Enter quantity",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            int qty;

                            try {

                                qty =
                                        Integer.parseInt(
                                                value
                                        );

                            } catch (Exception e) {

                                Toast.makeText(
                                        this,
                                        "Invalid quantity",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (qty <= 0) {

                                Toast.makeText(
                                        this,
                                        "Quantity must be greater than zero",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (qty > maxQty) {

                                Toast.makeText(
                                        this,
                                        "Cannot remove more than remaining",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            saveOffline(
                                    qr,
                                    "PARTIAL",
                                    qty
                            );
                        }
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .show();
    }

    // ============================================================
    // SAVE OFFLINE
    // ============================================================

    private void saveOffline(
            String qr,
            String action,
            int qty
    ) {

        StockDbHelper helper =
                new StockDbHelper(this);

        SQLiteDatabase db =
                helper.getWritableDatabase();

        /*
         * Get the exact server graded_stock_id.
         */
        int stockId =
                getGradedStockIdForQr(qr);

        if (stockId <= 0) {

            showResult(
                    "Graded stock record not found",
                    false
            );

            return;
        }

        int remaining =
                getBunchesForQr(qr);

        if (remaining <= 0) {

            showResult(
                    "No remaining bunches",
                    false
            );

            return;
        }

        if (
                !action.equals("COMPLETE") &&
                        qty > remaining
        ) {

            showResult(
                    "Cannot remove more than remaining",
                    false
            );

            return;
        }

        /*
         * ==========================================================
         * SAVE SCAN-OUT
         * ==========================================================
         */

        ContentValues cv =
                new ContentValues();

        cv.put(
                "qr",
                qr
        );

        cv.put(
                "graded_stock_id",
                stockId
        );

        cv.put(
                "farm_id",
                farmId
        );

        cv.put(
                "action_type",
                action
        );

        cv.put(
                "bunches_removed",
                qty
        );

        cv.put(
                "scanned_at",
                getTime()
        );

        /*
         * Build.SERIAL can return UNKNOWN on modern Android.
         *
         * Keep the same field but use a stable Android identifier
         * where available.
         */
        String deviceId =
                android.provider.Settings.Secure.getString(
                        getContentResolver(),
                        android.provider.Settings.Secure.ANDROID_ID
                );

        cv.put(
                "device_id",
                deviceId
        );

        cv.put(
                "synced",
                0
        );

        long localId =
                db.insert(
                        "graded_out_local",
                        null,
                        cv
                );

        if (localId == -1) {

            showResult(
                    "Failed to save scan-out",
                    false
            );

            return;
        }

        /*
         * ==========================================================
         * UPDATE LOCAL ACTIVE STOCK
         * ==========================================================
         */

        int newRemaining;

        if (
                action.equals("COMPLETE")
        ) {

            newRemaining = 0;

        } else {

            newRemaining =
                    Math.max(
                            0,
                            remaining - qty
                    );
        }

        ContentValues updateCv =
                new ContentValues();

        updateCv.put(
                "bunches",
                newRemaining
        );

        db.update(
                "graded_active_qrs",
                updateCv,
                "qr=?",
                new String[]{
                        qr
                }
        );

        /*
         * ==========================================================
         * LOCAL BUCKET RELEASE
         * ==========================================================
         *
         * COMPLETE means the physical bucket is now available
         * for reuse immediately.
         *
         * We record this locally even though the server has not
         * necessarily received the scan-out yet.
         */
        if (
                action.equals("COMPLETE")
        ) {

            ContentValues releaseCv =
                    new ContentValues();

            releaseCv.put(
                    "qr",
                    qr
            );

            releaseCv.put(
                    "graded_stock_id",
                    stockId
            );

            releaseCv.put(
                    "farm_id",
                    farmId
            );

            releaseCv.put(
                    "released_at",
                    getTime()
            );

            releaseCv.put(
                    "scanout_local_id",
                    localId
            );

            releaseCv.put(
                    "synced",
                    0
            );

            /*
             * REPLACE allows the same QR to have a current
             * locally released state.
             */
            db.insertWithOnConflict(
                    "graded_released_qrs",
                    null,
                    releaseCv,
                    SQLiteDatabase.CONFLICT_REPLACE
            );

            /*
             * Remove it from the active local cache.
             *
             * The old stock still exists in graded_out_local,
             * so nothing is lost.
             */
            db.delete(
                    "graded_active_qrs",
                    "qr=?",
                    new String[]{
                            qr
                    }
            );

            addHistoryItem(
                    qr
            );

            showResult(
                    "Bucket released locally ✓\n" +
                            "Bucket can be reused now.",
                    true
            );

            return;
        }

        /*
         * ==========================================================
         * PARTIAL DISPATCH
         * ==========================================================
         *
         * Partial stock remains active.
         */
        addHistoryItem(
                qr
        );

        showResult(
                "Saved offline ✓\n" +
                        newRemaining +
                        " bunches remaining.",
                true
        );
    }

    // ============================================================
    // GET GRADED STOCK ID
    // ============================================================

    private int getGradedStockIdForQr(
            String qr
    ) {

        int stockId = 0;

        StockDbHelper helper =
                new StockDbHelper(this);

        SQLiteDatabase db =
                helper.getReadableDatabase();

        Cursor c =
                db.rawQuery(
                        "SELECT graded_stock_id " +
                                "FROM graded_active_qrs " +
                                "WHERE qr=? " +
                                "LIMIT 1",
                        new String[]{
                                qr
                        }
                );

        if (c.moveToFirst()) {

            stockId =
                    c.getInt(
                            c.getColumnIndexOrThrow(
                                    "graded_stock_id"
                            )
                    );
        }

        c.close();

        return stockId;
    }

    // ============================================================
    // GET REMAINING BUNCHES
    // ============================================================

    private int getBunchesForQr(
            String qr
    ) {

        int bunches = 0;

        StockDbHelper helper =
                new StockDbHelper(this);

        SQLiteDatabase db =
                helper.getReadableDatabase();

        Cursor c =
                db.rawQuery(
                        "SELECT bunches " +
                                "FROM graded_active_qrs " +
                                "WHERE qr=? " +
                                "LIMIT 1",
                        new String[]{
                                qr
                        }
                );

        if (c.moveToFirst()) {

            bunches =
                    c.getInt(
                            c.getColumnIndexOrThrow(
                                    "bunches"
                            )
                    );
        }

        c.close();

        return bunches;
    }

    // ============================================================
    // HISTORY
    // ============================================================

    private void addHistoryItem(
            String qr
    ) {

        TextView t =
                new TextView(this);

        t.setText(
                qr +
                        " • " +
                        getTime()
        );

        historyList.addView(
                t,
                0
        );
    }

    // ============================================================
    // RESULT
    // ============================================================

    private void showResult(
            String msg,
            boolean ok
    ) {

        resultBox.setText(
                msg
        );
    }

    // ============================================================
    // TIME
    // ============================================================

    private String getTime() {

        return new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
        ).format(
                new Date()
        );
    }

    // ============================================================
    // QR SCANNER RESULT
    // ============================================================

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        IntentResult result =
                IntentIntegrator.parseActivityResult(
                        requestCode,
                        resultCode,
                        data
                );

        if (result != null) {

            if (
                    result.getContents() != null
            ) {

                String qr =
                        result.getContents()
                                .trim();

                processScan(qr);

            } else {

                Toast.makeText(
                        this,
                        "Scan cancelled",
                        Toast.LENGTH_SHORT
                ).show();
            }

        } else {

            super.onActivityResult(
                    requestCode,
                    resultCode,
                    data
            );
        }
    }
}