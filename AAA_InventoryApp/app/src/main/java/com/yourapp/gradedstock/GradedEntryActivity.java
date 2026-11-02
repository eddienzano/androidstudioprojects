package com.yourapp.gradedstock;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.yourapp.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradedEntryActivity extends AppCompatActivity
        implements GradedQrAdapter.OnBucketRemovedListener {

    private AutoCompleteTextView etVariety;
    private EditText etQrInput;

    private Button btnCameraScan;
    private Button btnSaveOffline;
    private Button btnSync;

    private TextView tvScanCount;
    private RecyclerView rvList;

    private ArrayList<QrItem> scanned =
            new ArrayList<>();

    private GradedQrAdapter adapter;

    private BroadcastReceiver honeywellScanReceiver;

    private int farmId = 1;

    private int selectedVarietyId = 0;
    private String selectedVarietyName = "";

    private final HashMap<String, Integer> varietyMap =
            new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_graded_entry
        );

        if (getIntent().hasExtra("farm_id")) {

            farmId =
                    getIntent().getIntExtra(
                            "farm_id",
                            1
                    );
        }

        bindViews();

        setupRecycler();

        loadVarieties();

        setupScannerInput();

        setupHoneywellScanner();

        setupButtons();

        updateCounter();
    }

    // ============================================================
    // VIEW BINDING
    // ============================================================

    private void bindViews() {

        etVariety =
                findViewById(R.id.etVariety);

        etQrInput =
                findViewById(R.id.etQrInput);

        btnCameraScan =
                findViewById(R.id.btnCameraScan);

        btnSaveOffline =
                findViewById(R.id.btnSubmit);

        btnSync =
                findViewById(R.id.btnSync);

        tvScanCount =
                findViewById(R.id.tvScanCount);

        rvList =
                findViewById(R.id.rvList);

        etQrInput.setShowSoftInputOnFocus(false);
    }

    // ============================================================
    // RECYCLER
    // ============================================================

    private void setupRecycler() {

        adapter =
                new GradedQrAdapter(
                        scanned,
                        this
                );

        rvList.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvList.setAdapter(adapter);
    }

    // ============================================================
    // BUTTONS
    // ============================================================

    private void setupButtons() {

        btnCameraScan.setOnClickListener(v -> {

            IntentIntegrator integrator =
                    new IntentIntegrator(this);

            integrator.setPrompt(
                    "Scan Bucket QR"
            );

            integrator.setBeepEnabled(true);

            integrator.setOrientationLocked(false);

            integrator.initiateScan();
        });

        btnSaveOffline.setOnClickListener(v ->
                saveOffline()
        );

        btnSync.setOnClickListener(v ->
                syncServer()
        );
    }

    // ============================================================
    // TEXT / HARDWARE SCANNER
    // ============================================================

    private void setupScannerInput() {

        etQrInput.requestFocus();

        etQrInput.addTextChangedListener(
                new SimpleTextWatcher() {

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        String qr =
                                s.toString().trim();

                        if (qr.length() < 10) {
                            return;
                        }

                        etQrInput.setText("");

                        handleScan(qr);
                    }
                }
        );
    }

    // ============================================================
    // VARIETIES
    // ============================================================

    private void loadVarieties() {

        SQLiteDatabase db =
                new StockDbHelper(this)
                        .getReadableDatabase();

        ArrayList<String> names =
                new ArrayList<>();

        Cursor c =
                db.rawQuery(
                        "SELECT id,name " +
                                "FROM varieties " +
                                "WHERE farm_id=? " +
                                "ORDER BY name",
                        new String[]{
                                String.valueOf(farmId)
                        }
                );

        while (c.moveToNext()) {

            int id =
                    c.getInt(0);

            String name =
                    c.getString(1);

            names.add(name);

            varietyMap.put(
                    name,
                    id
            );
        }

        c.close();

        ArrayAdapter<String> varietyAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout
                                .simple_dropdown_item_1line,
                        names
                );

        etVariety.setAdapter(
                varietyAdapter
        );

        etVariety.setOnItemClickListener(
                (parent, view, position, id) -> {

                    String selected =
                            varietyAdapter
                                    .getItem(position);

                    if (selected == null) {
                        return;
                    }

                    selectedVarietyName =
                            selected;

                    Integer selectedId =
                            varietyMap.get(
                                    selected
                            );

                    selectedVarietyId =
                            selectedId != null
                                    ? selectedId
                                    : 0;
                }
        );
    }

    // ============================================================
    // HONEYWELL SCANNER
    // ============================================================

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void setupHoneywellScanner() {

        honeywellScanReceiver =
                new BroadcastReceiver() {

                    @Override
                    public void onReceive(
                            Context context,
                            Intent intent
                    ) {

                        try {

                            String barcode = null;

                            if (intent.hasExtra("data")) {

                                barcode =
                                        intent.getStringExtra(
                                                "data"
                                        );

                            } else if (
                                    intent.hasExtra(
                                            "barcode"
                                    )
                            ) {

                                barcode =
                                        intent.getStringExtra(
                                                "barcode"
                                        );

                            } else if (
                                    intent.hasExtra(
                                            "decoded_string"
                                    )
                            ) {

                                barcode =
                                        intent.getStringExtra(
                                                "decoded_string"
                                        );
                            }

                            if (TextUtils.isEmpty(
                                    barcode
                            )) {
                                return;
                            }

                            final String qr =
                                    barcode.trim();

                            runOnUiThread(() -> {

                                handleScan(qr);

                                etQrInput.setText("");

                                etQrInput.requestFocus();
                            });

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
                };

        IntentFilter filter =
                new IntentFilter();

        filter.addAction(
                "com.honeywell.scan.RESULT"
        );

        filter.addAction(
                "com.honeywell.aidc.action.SCAN"
        );

        if (
                Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.TIRAMISU
        ) {

            registerReceiver(
                    honeywellScanReceiver,
                    filter,
                    Context.RECEIVER_EXPORTED
            );

        } else {

            registerReceiver(
                    honeywellScanReceiver,
                    filter
            );
        }
    }

    // ============================================================
    // HANDLE SCAN
    // ============================================================

    private void handleScan(String qr) {

        if (selectedVarietyId == 0) {

            toast(
                    "Select variety first"
            );

            return;
        }

        if (existsInMemory(qr)) {

            toast(
                    "Already scanned"
            );

            return;
        }

        if (isPendingDuplicate(qr)) {

            toast(
                    "Already saved"
            );

            return;
        }

        /*
         * ========================================================
         * LOCAL RELEASE HAS PRIORITY
         * ========================================================
         *
         * A COMPLETE scan-out may have happened while offline.
         *
         * The server may still show the old bucket as In-Use,
         * but locally that bucket has already been released.
         *
         * Therefore a locally released QR is allowed to proceed.
         */
        boolean locallyReleased =
                isLocallyReleased(qr);

        if (
                !locallyReleased &&
                        isActiveBucket(qr)
        ) {

            toast(
                    "Bucket already active"
            );

            return;
        }

        QrItem item =
                parseQR(qr);

        if (item == null) {

            toast(
                    "Invalid QR"
            );

            return;
        }

        item.variety_id =
                selectedVarietyId;

        item.variety_name =
                selectedVarietyName;

        item.bunches = 10;

        item.stems = 10;

        scanned.add(item);

        adapter.notifyItemInserted(
                scanned.size() - 1
        );

        rvList.smoothScrollToPosition(
                scanned.size() - 1
        );

        etQrInput.requestFocus();

        updateCounter();
    }

    // ============================================================
    // DUPLICATE CHECKS
    // ============================================================

    private boolean existsInMemory(
            String qr
    ) {

        for (QrItem item : scanned) {

            if (item.qr.equals(qr)) {
                return true;
            }
        }

        return false;
    }

    private boolean isPendingDuplicate(
            String qr
    ) {

        SQLiteDatabase db =
                new StockDbHelper(this)
                        .getReadableDatabase();

        Cursor c =
                db.rawQuery(
                        "SELECT id " +
                                "FROM graded_pending " +
                                "WHERE qr=? " +
                                "AND synced=0 " +
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

    private boolean isActiveBucket(
            String qr
    ) {

        SQLiteDatabase db =
                new StockDbHelper(this)
                        .getReadableDatabase();

        Cursor c =
                db.rawQuery(
                        "SELECT qr " +
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

    private boolean isLocallyReleased(
            String qr
    ) {

        SQLiteDatabase db =
                new StockDbHelper(this)
                        .getReadableDatabase();

        Cursor c =
                db.rawQuery(
                        "SELECT qr " +
                                "FROM graded_released_qrs " +
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
    // QR PARSER
    // ============================================================

    private QrItem parseQR(
            String qr
    ) {

        try {

            Pattern pattern =
                    Pattern.compile(
                            "Serial:\\s*([^|]+)\\|" +
                                    "\\s*Bucket:\\s*([^|]+)\\|" +
                                    "\\s*Farm:\\s*([^|]+)\\|" +
                                    "\\s*Length:\\s*(\\d+)",
                            Pattern.CASE_INSENSITIVE
                    );

            Matcher matcher =
                    pattern.matcher(qr);

            if (!matcher.find()) {
                return null;
            }

            QrItem item =
                    new QrItem();

            item.qr =
                    qr;

            item.serial =
                    matcher.group(1).trim();

            item.bucket_name =
                    matcher.group(2).trim();

            item.length =
                    Integer.parseInt(
                            matcher.group(4).trim()
                    );

            return item;

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }

    // ============================================================
    // COUNTER
    // ============================================================

    private void updateCounter() {

        tvScanCount.setText(
                "Scanned: " +
                        scanned.size() +
                        " buckets"
        );
    }

    @Override
    public void onBucketRemoved(
            int newCount
    ) {

        updateCounter();
    }

    // ============================================================
    // SAVE OFFLINE
    // ============================================================

    private void saveOffline() {

        if (scanned.isEmpty()) {

            toast(
                    "Nothing to save"
            );

            return;
        }

        SQLiteDatabase db =
                new StockDbHelper(this)
                        .getWritableDatabase();

        int saved = 0;

        db.beginTransaction();

        try {

            for (QrItem item : scanned) {

                ContentValues cv =
                        new ContentValues();

                cv.put(
                        "qr",
                        item.qr
                );

                cv.put(
                        "serial",
                        item.serial
                );

                cv.put(
                        "bucket_name",
                        item.bucket_name
                );

                cv.put(
                        "farm_id",
                        farmId
                );

                cv.put(
                        "length",
                        item.length
                );

                cv.put(
                        "variety_id",
                        item.variety_id
                );

                cv.put(
                        "bunches",
                        item.bunches
                );

                cv.put(
                        "stems_per_bunch",
                        item.stems
                );

                cv.put(
                        "synced",
                        0
                );

                long result =
                        db.insert(
                                "graded_pending",
                                null,
                                cv
                        );

                if (result <= 0) {
                    continue;
                }

                saved++;

                /*
                 * ==================================================
                 * LOCALLY RELEASED BUCKET
                 * ==================================================
                 *
                 * This QR has now been reused locally.
                 *
                 * Remove the release marker so the same physical
                 * bucket cannot be used for another new intake
                 * before this intake is synced.
                 */
                db.delete(
                        "graded_released_qrs",
                        "qr=?",
                        new String[]{
                                item.qr
                        }
                );

                /*
                 * Remove stale old active stock from the local cache.
                 *
                 * The old stock was already completed locally.
                 */
                db.delete(
                        "graded_active_qrs",
                        "qr=?",
                        new String[]{
                                item.qr
                        }
                );
            }

            db.setTransactionSuccessful();

        } finally {

            db.endTransaction();
        }

        scanned.clear();

        adapter.notifyDataSetChanged();

        updateCounter();

        toast(
                saved +
                        " buckets saved"
        );
    }

    // ============================================================
    // SYNC
    // ============================================================

    private void syncServer() {

        new Thread(() -> {

            /*
             * ====================================================
             * STEP 1
             * ====================================================
             *
             * Scan-outs MUST reach the server first.
             *
             * This is what changes the old server stock from
             * active -> dispatched and releases the bucket.
             */
            boolean scanoutsReady =
                    syncScanOutsBeforeIntake();

            if (!scanoutsReady) {

                runOnUiThread(() ->
                        toast(
                                "Scan-outs must sync first"
                        )
                );

                return;
            }

            /*
             * ====================================================
             * STEP 2
             * ====================================================
             *
             * Now upload new graded intake.
             */
            syncPendingGradedStock();

        }).start();
    }

    // ============================================================
    // SYNC SCAN-OUTS BEFORE GRADED INTAKE
    // ============================================================

    private boolean syncScanOutsBeforeIntake() {

        try {

            StockDbHelper helper =
                    new StockDbHelper(this);

            SQLiteDatabase db =
                    helper.getWritableDatabase();

            Cursor c =
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

            JSONArray records =
                    new JSONArray();

            while (c.moveToNext()) {

                JSONObject row =
                        new JSONObject();

                row.put(
                        "id",
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "id"
                                )
                        )
                );

                row.put(
                        "qr",
                        c.getString(
                                c.getColumnIndexOrThrow(
                                        "qr"
                                )
                        )
                );

                row.put(
                        "graded_stock_id",
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "graded_stock_id"
                                )
                        )
                );

                row.put(
                        "farm_id",
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "farm_id"
                                )
                        )
                );

                row.put(
                        "action_type",
                        c.getString(
                                c.getColumnIndexOrThrow(
                                        "action_type"
                                )
                        )
                );

                row.put(
                        "bunches_removed",
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "bunches_removed"
                                )
                        )
                );

                row.put(
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

                    row.put(
                            "device_id",
                            deviceId
                    );
                }

                records.put(row);
            }

            c.close();

            /*
             * No pending scan-outs means intake can proceed.
             */
            if (records.length() == 0) {
                return true;
            }

            String response =
                    HttpHelper.postJson(
                            "https://www.aaagrowers.co.ke/inventory/graded/sync_scanouts.php",
                            records.toString()
                    );

            if (
                    response == null ||
                            response.trim().isEmpty()
            ) {

                return false;
            }

            JSONObject result =
                    new JSONObject(response);

            JSONArray successfulIds =
                    result.optJSONArray(
                            "successful_ids"
                    );

            if (successfulIds != null) {

                for (
                        int i = 0;
                        i < successfulIds.length();
                        i++
                ) {

                    int localId =
                            successfulIds.getInt(i);

                    db.execSQL(
                            "UPDATE graded_out_local " +
                                    "SET synced=1 " +
                                    "WHERE id=?",
                            new Object[]{
                                    localId
                            }
                    );

                    /*
                     * Mark the corresponding local release
                     * as successfully synchronized.
                     */
                    db.execSQL(
                            "UPDATE graded_released_qrs " +
                                    "SET synced=1 " +
                                    "WHERE scanout_local_id=?",
                            new Object[]{
                                    localId
                            }
                    );
                }
            }

            /*
             * ====================================================
             * IMPORTANT
             * ====================================================
             *
             * Do not proceed to graded intake if any scan-out
             * remains unsynced.
             */
            Cursor remaining =
                    db.rawQuery(
                            "SELECT COUNT(*) " +
                                    "FROM graded_out_local " +
                                    "WHERE synced=0",
                            null
                    );

            int count = 0;

            if (remaining.moveToFirst()) {
                count =
                        remaining.getInt(0);
            }

            remaining.close();

            return count == 0;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    // ============================================================
    // SYNC GRADED PENDING
    // ============================================================

    private void syncPendingGradedStock() {

        try {

            SQLiteDatabase db =
                    new StockDbHelper(this)
                            .getReadableDatabase();

            Cursor c =
                    db.rawQuery(
                            "SELECT " +
                                    "id, " +
                                    "qr, " +
                                    "serial, " +
                                    "bucket_name, " +
                                    "farm_id, " +
                                    "length, " +
                                    "variety_id, " +
                                    "bunches, " +
                                    "stems_per_bunch " +
                                    "FROM graded_pending " +
                                    "WHERE synced=0 " +
                                    "ORDER BY id ASC",
                            null
                    );

            JSONArray records =
                    new JSONArray();

            while (c.moveToNext()) {

                JSONObject row =
                        new JSONObject();

                row.put(
                        "qr",
                        c.getString(
                                c.getColumnIndexOrThrow(
                                        "qr"
                                )
                        )
                );

                row.put(
                        "serial",
                        c.getString(
                                c.getColumnIndexOrThrow(
                                        "serial"
                                )
                        )
                );

                row.put(
                        "bucket_name",
                        c.getString(
                                c.getColumnIndexOrThrow(
                                        "bucket_name"
                                )
                        )
                );

                row.put(
                        "farm_id",
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "farm_id"
                                )
                        )
                );

                row.put(
                        "length",
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "length"
                                )
                        )
                );

                row.put(
                        "variety_id",
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "variety_id"
                                )
                        )
                );

                row.put(
                        "bunches",
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "bunches"
                                )
                        )
                );

                row.put(
                        "stems_per_bunch",
                        c.getInt(
                                c.getColumnIndexOrThrow(
                                        "stems_per_bunch"
                                )
                        )
                );

                records.put(row);
            }

            c.close();

            if (records.length() == 0) {

                runOnUiThread(() ->
                        toast(
                                "Nothing to sync"
                        )
                );

                return;
            }

            JSONObject payload =
                    new JSONObject();

            payload.put(
                    "records",
                    records
            );

            String response =
                    HttpHelper.postJson(
                            "https://www.aaagrowers.co.ke/inventory/graded/sync_graded_upload.php",
                            payload.toString()
                    );

            if (
                    response == null ||
                            response.trim().isEmpty()
            ) {

                runOnUiThread(() ->
                        toast(
                                "Sync failed: empty server response"
                        )
                );

                return;
            }

            JSONObject result =
                    new JSONObject(response);

            JSONArray successful =
                    result.optJSONArray(
                            "successful_qrs"
                    );

            JSONArray failed =
                    result.optJSONArray(
                            "failed_qrs"
                    );

            int successCount =
                    successful != null
                            ? successful.length()
                            : 0;

            int failedCount =
                    failed != null
                            ? failed.length()
                            : 0;

            /*
             * ====================================================
             * MARK ONLY SUCCESSFUL QRS
             * ====================================================
             */
            if (successful != null) {

                SQLiteDatabase writeDb =
                        new StockDbHelper(this)
                                .getWritableDatabase();

                for (
                        int i = 0;
                        i < successful.length();
                        i++
                ) {

                    JSONObject success =
                            successful.getJSONObject(i);

                    String qr =
                            success.optString(
                                    "qr",
                                    ""
                            );

                    if (qr.isEmpty()) {
                        continue;
                    }

                    writeDb.execSQL(
                            "UPDATE graded_pending " +
                                    "SET synced=1, " +
                                    "graded_stock_id=? " +
                                    "WHERE qr=? " +
                                    "AND synced=0",
                            new Object[]{
                                    success.optInt(
                                            "graded_stock_id",
                                            0
                                    ),
                                    qr
                            }
                    );
                }
            }

            /*
             * Refresh local active stock after successful upload.
             *
             * This is only a cache refresh. It is NOT what makes
             * the upload successful.
             */
            SyncManager.syncActiveStock(
                    GradedEntryActivity.this,
                    farmId,
                    new SyncManager.SyncCallback() {

                        @Override
                        public void onSuccess() {

                            runOnUiThread(() -> {

                                if (failedCount == 0) {

                                    toast(
                                            "Sync successful: " +
                                                    successCount
                                    );

                                } else {

                                    toast(
                                            "Synced: " +
                                                    successCount +
                                                    " | Failed: " +
                                                    failedCount
                                    );
                                }
                            });
                        }

                        @Override
                        public void onError(
                                String msg
                        ) {

                            runOnUiThread(() -> {

                                if (failedCount == 0) {

                                    toast(
                                            "Synced: " +
                                                    successCount +
                                                    " | Refresh failed"
                                    );

                                } else {

                                    toast(
                                            "Synced: " +
                                                    successCount +
                                                    " | Failed: " +
                                                    failedCount
                                    );
                                }
                            });
                        }
                    }
            );

        } catch (Exception e) {

            e.printStackTrace();

            runOnUiThread(() ->
                    toast(
                            "Sync error"
                    )
            );
        }
    }

    // ============================================================
    // CAMERA RESULT
    // ============================================================

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    ) {

        IntentResult result =
                IntentIntegrator.parseActivityResult(
                        requestCode,
                        resultCode,
                        data
                );

        if (result != null) {

            if (result.getContents() != null) {

                handleScan(
                        result.getContents()
                );

            } else {

                toast(
                        "Scan cancelled"
                );
            }

        } else {

            super.onActivityResult(
                    requestCode,
                    resultCode,
                    data
            );
        }
    }

    // ============================================================
    // CLEANUP
    // ============================================================

    @Override
    protected void onDestroy() {

        super.onDestroy();

        try {

            if (honeywellScanReceiver != null) {

                unregisterReceiver(
                        honeywellScanReceiver
                );
            }

        } catch (Exception ignored) {
        }
    }

    // ============================================================
    // TOAST
    // ============================================================

    private void toast(
            String msg
    ) {

        Toast.makeText(
                this,
                msg,
                Toast.LENGTH_SHORT
        ).show();
    }
}