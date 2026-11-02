package com.aaagrowers.coldroom;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;

import com.aaagrowers.coldroom.stocks.VarietyDbHelper;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class BoxScanActivity extends AppCompatActivity {

    private Button scanBtn;
    private TextView txtResult;
    private EditText inputScan;
    private TextView txtSyncStatus, txtPendingCount;

    private DBHelper coldDb;
    private VarietyDbHelper varietyDb;

    private static final long SCAN_THRESHOLD = 40;
    private static final long DOUBLE_SCAN_LOCK_MS = 2000;

    private long lastCharTime = 0;
    private StringBuilder scanBuffer = new StringBuilder();

    private String lastScannedValue = "";
    private long lastScannedAt = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private List<String> varietyNames = new ArrayList<>();
    private List<Integer> varietyIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_box_scan);

        scanBtn = findViewById(R.id.scanBtn);
        txtResult = findViewById(R.id.txtResult);
        inputScan = findViewById(R.id.inputScan);
        txtSyncStatus = findViewById(R.id.txtSyncStatus);
        txtPendingCount = findViewById(R.id.txtPendingCount);

        Button btnSync = findViewById(R.id.btnSync);

        coldDb = new DBHelper(this);
        varietyDb = new VarietyDbHelper(this);

        btnSync.setOnClickListener(v -> {
            syncPendingUpdates();
            Toast.makeText(this, "Sync started...", Toast.LENGTH_SHORT).show();
        });

        scanBtn.setOnClickListener(v -> startQRScan());

        inputScan.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            public void afterTextChanged(Editable s) {

                long now = System.currentTimeMillis();
                String text = s.toString();

                if (now - lastCharTime < SCAN_THRESHOLD) {
                    scanBuffer.append(text);
                } else {
                    scanBuffer.setLength(0);
                    scanBuffer.append(text);
                }

                lastCharTime = now;

                if (text.endsWith("\n") || text.endsWith("\r")) {

                    String cleaned = text.trim();
                    inputScan.setText("");
                    scanBuffer.setLength(0);

                    if (!cleaned.isEmpty())
                        submitIfAllowed(cleaned);

                    return;
                }

                if (scanBuffer.length() >= 6) {
                    handler.removeCallbacks(bufferFinish);
                    handler.postDelayed(bufferFinish, 80);
                }

                if (text.length() > 128) {
                    inputScan.setText("");
                    scanBuffer.setLength(0);
                }
            }
        });

        updateSyncStatus();
    }

    // =========================
    // LOAD VARIETIES FROM variety.db (FIXED)
    // =========================
    private void loadVarieties() {

        varietyNames.clear();
        varietyIds.clear();

        SQLiteDatabase db = varietyDb.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT variety_id, variety_name FROM variety ORDER BY variety_name ASC",
                null
        );

        while (c.moveToNext()) {
            varietyIds.add(c.getInt(0));
            varietyNames.add(c.getString(1));
        }

        c.close();
        db.close();
    }

    private final Runnable bufferFinish = () -> {

        String s = scanBuffer.toString().trim();
        scanBuffer.setLength(0);
        inputScan.setText("");

        if (!s.isEmpty())
            submitIfAllowed(s);
    };

    private void submitIfAllowed(String qr) {

        long now = System.currentTimeMillis();

        if (qr.equals(lastScannedValue) && (now - lastScannedAt) < DOUBLE_SCAN_LOCK_MS) {
            Toast.makeText(this, "Duplicate scan ignored", Toast.LENGTH_SHORT).show();
            return;
        }

        lastScannedValue = qr;
        lastScannedAt = now;

        processScan(qr);
    }

    private void processScan(String qr) {

        SQLiteDatabase db = coldDb.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT * FROM box_storage_local WHERE qr_code=?",
                new String[]{qr}
        );

        if (c.moveToFirst()) {

            String variety = c.getString(c.getColumnIndexOrThrow("variety_name"));
            int length = c.getInt(c.getColumnIndexOrThrow("length"));
            int stems = c.getInt(c.getColumnIndexOrThrow("stems"));
            String updated = c.getString(c.getColumnIndexOrThrow("updated_at"));

            showPreviewDialog(qr, variety, length, stems, updated);

        } else {
            Toast.makeText(this, "Box not found", Toast.LENGTH_SHORT).show();
        }

        c.close();
        db.close();
    }

    private void showEditDialog(String qr, int currentLength, int currentStems) {

        SQLiteDatabase db = coldDb.getReadableDatabase();

        String currentVarietyName = "";

        Cursor c = db.rawQuery(
                "SELECT variety_name FROM box_storage_local WHERE qr_code=?",
                new String[]{qr}
        );

        if (c.moveToFirst()) {
            currentVarietyName = c.getString(0);
        }

        c.close();
        db.close();

        loadVarieties();

        Spinner spVariety = new Spinner(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                varietyNames
        );

        spVariety.setAdapter(adapter);

        // FIX: match by NAME
        final String finalName = currentVarietyName;

        spVariety.post(() -> {
            int index = 0;

            for (int i = 0; i < varietyNames.size(); i++) {
                if (varietyNames.get(i).equals(finalName)) {
                    index = i;
                    break;
                }
            }

            spVariety.setSelection(index);
        });

        EditText inputLength = new EditText(this);
        inputLength.setText(String.valueOf(currentLength));

        EditText inputStems = new EditText(this);
        inputStems.setText(String.valueOf(currentStems));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        layout.addView(spVariety);
        layout.addView(inputLength);
        layout.addView(inputStems);

        new AlertDialog.Builder(this)
                .setTitle("Edit Box")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {

                    int pos = spVariety.getSelectedItemPosition();
                    String newVarietyName = varietyNames.get(pos);

                    updateBox(
                            qr,
                            newVarietyName,
                            Integer.parseInt(inputLength.getText().toString()),
                            Integer.parseInt(inputStems.getText().toString())
                    );

                    loadBoxDisplay(qr);
                    updateSyncStatus();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateBox(String qr, String varietyName, int length, int stems) {

        SQLiteDatabase db = coldDb.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("variety_name", varietyName);
        cv.put("length", length);
        cv.put("stems", stems);
        cv.put("is_synced", 0);

        db.update("box_storage_local", cv, "qr_code=?", new String[]{qr});
        db.close();
    }

    private void loadBoxDisplay(String qr) {

        SQLiteDatabase db = coldDb.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT * FROM box_storage_local WHERE qr_code=?",
                new String[]{qr}
        );

        if (c.moveToFirst()) {

            txtResult.setText(
                    "QR: " + qr + "\n\n" +
                            "Variety: " + c.getString(c.getColumnIndexOrThrow("variety_name")) + "\n" +
                            "Length: " + c.getInt(c.getColumnIndexOrThrow("length")) + "\n" +
                            "Stems: " + c.getInt(c.getColumnIndexOrThrow("stems")) + "\n" +
                            "Updated: " + c.getString(c.getColumnIndexOrThrow("updated_at"))
            );
        }

        c.close();
        db.close();
    }

    private void syncPendingUpdates() {

        new Thread(() -> {

            SQLiteDatabase db = coldDb.getReadableDatabase();

            Cursor c = db.rawQuery(
                    "SELECT * FROM box_storage_local WHERE is_synced=0",
                    null
            );

            int success = 0;
            int fail = 0;

            while (c.moveToNext()) {

                String qr = c.getString(c.getColumnIndexOrThrow("qr_code"));
                String varietyName = c.getString(c.getColumnIndexOrThrow("variety_name"));
                int length = c.getInt(c.getColumnIndexOrThrow("length"));
                int stems = c.getInt(c.getColumnIndexOrThrow("stems"));

                // 🔥 convert name → ID from variety.db
                int varietyId = getVarietyIdByName(varietyName);

                if (varietyId == 0) {
                    android.util.Log.e("SYNC", "Variety not found: " + varietyName);
                    fail++;
                    continue;
                }

                if (sendToServer(qr, varietyId, length, stems)) {
                    markAsSynced(qr);
                    success++;
                } else {
                    fail++;
                }
            }

            c.close();
            db.close();

            int finalSuccess = success;
            int finalFail = fail;

            runOnUiThread(() -> {
                Toast.makeText(this,
                        "Synced: " + finalSuccess + " | Failed: " + finalFail,
                        Toast.LENGTH_LONG).show();

                updateSyncStatus();
            });

        }).start();
    }

    private boolean sendToServer(String qr, int varietyId, int length, int stems) {

        HttpURLConnection conn = null;

        try {
            URL url = new URL("https://www.aaagrowers.co.ke/inventory/sync/api_update_box.php");

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            String data =
                    "qr_code=" + qr +
                            "&variety_id=" + varietyId +
                            "&length=" + length +
                            "&stems=" + stems;

            conn.getOutputStream().write(data.getBytes("UTF-8"));

            int responseCode = conn.getResponseCode();

            return (responseCode == 200);

        } catch (Exception e) {
            e.printStackTrace();
            return false;

        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void markAsSynced(String qr) {

        SQLiteDatabase db = coldDb.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("is_synced", 1);

        db.update("box_storage_local",
                cv,
                "qr_code=?",
                new String[]{qr});

        db.close();
    }

    private void updateSyncStatus() {
        SQLiteDatabase db = coldDb.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM box_storage_local WHERE is_synced=0",
                null
        );

        int pending = 0;
        if (c.moveToFirst()) pending = c.getInt(0);

        c.close();
        db.close();

        txtPendingCount.setText("Pending: " + pending);
        txtSyncStatus.setText(pending == 0 ? "All synced" : "Pending sync");
    }

    private void startQRScan() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan Box QR");
        options.setBeepEnabled(true);

        barcodeLauncher.launch(options);
    }

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    submitIfAllowed(result.getContents());
                }
            });

    private void showPreviewDialog(String qr, String variety, int length, int stems, String updated) {

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Box Details")
                .setMessage(
                        "QR: " + qr + "\n\n" +
                                "Variety: " + variety + "\n" +
                                "Length: " + length + "\n" +
                                "Stems: " + stems + "\n" +
                                "Updated: " + updated
                )
                .setPositiveButton("✏️ Edit", (d, w) -> {
                    showEditDialog(qr, length, stems);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private int getVarietyIdByName(String name) {

        SQLiteDatabase db = varietyDb.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT variety_id FROM variety WHERE variety_name=? LIMIT 1",
                new String[]{name}
        );

        int id = 0;

        if (c.moveToFirst()) {
            id = c.getInt(0);
        }

        c.close();
        db.close();

        return id;
    }
}