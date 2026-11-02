package com.aaagrowers.coldroom;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class ScanActivity extends AppCompatActivity {

    private Button scanBtn;
    private TextView resultText;
    private EditText inputScan;

    private DBHelper dbHelper;
    private String farm;

    private Button btnFix, btnCancel, btnPushFixes;

    private QRInventory currentQR;

    private static final long SCAN_THRESHOLD = 40;
    private static final long DOUBLE_SCAN_LOCK_MS = 2000;

    private long lastCharTime = 0;
    private StringBuilder scanBuffer = new StringBuilder();

    private String lastScannedValue = "";
    private long lastScannedAt = 0;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        scanBtn = findViewById(R.id.scanBtn);
        resultText = findViewById(R.id.resultText);
        inputScan = findViewById(R.id.inputScan);

        btnFix = findViewById(R.id.btnFix);
        btnCancel = findViewById(R.id.btnCancel);

        btnFix.setEnabled(false);
        btnCancel.setEnabled(false);

        dbHelper = new DBHelper(this);

        farm = getIntent().getStringExtra("farm");

        btnPushFixes = findViewById(R.id.btnPushFixes);

        btnPushFixes.setOnClickListener(v -> {
            new PushFixesTask().execute();
        });

        // Camera scan button
        scanBtn.setOnClickListener(v -> startQRScan());

        btnCancel.setOnClickListener(v -> {

            currentQR = null;

            resultText.setText("Waiting for scan...");

            btnFix.setEnabled(false);
            btnCancel.setEnabled(false);

        });

        btnFix.setOnClickListener(v -> {

            if (currentQR != null) {
                showEditDialog();
            }

        });

        // Honeywell scanner input
        inputScan.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

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

                // Scanner sends ENTER
                if (text.endsWith("\n") || text.endsWith("\r")) {

                    String cleaned = text.trim();
                    inputScan.setText("");
                    scanBuffer.setLength(0);

                    if (!cleaned.isEmpty())
                        submitIfAllowed(cleaned);

                    return;
                }

                // Burst detection
                if (scanBuffer.length() >= 6) {
                    mainHandler.removeCallbacks(bufferFinish);
                    mainHandler.postDelayed(bufferFinish, 80);
                }

                if (text.length() > 128) {
                    inputScan.setText("");
                    scanBuffer.setLength(0);
                }
            }
        });
    }

    private class PushFixesTask extends AsyncTask<Void, Void, int[]> {

        @Override
        protected void onPreExecute() {

            btnPushFixes.setEnabled(false);

            Toast.makeText(
                    ScanActivity.this,
                    "Pushing fixes...",
                    Toast.LENGTH_SHORT
            ).show();
        }

        @Override
        protected int[] doInBackground(Void... voids) {

            int pushed = 0;
            int failed = 0;

            SQLiteDatabase db =
                    dbHelper.getWritableDatabase();

            Cursor c = db.rawQuery(
                    "SELECT * FROM inventory_fixes WHERE synced=0",
                    null
            );

            while (c.moveToNext()) {

                try {

                    String qr =
                            c.getString(c.getColumnIndexOrThrow("qr_code"));

                    String variety =
                            c.getString(c.getColumnIndexOrThrow("new_variety"));

                    int qty =
                            c.getInt(c.getColumnIndexOrThrow("new_quantity"));

                    int id =
                            c.getInt(c.getColumnIndexOrThrow("id"));

                    if (sendFix(qr, variety, qty)) {

                        ContentValues cv = new ContentValues();
                        cv.put("synced", 1);

                        db.update(
                                "inventory_fixes",
                                cv,
                                "id=?",
                                new String[]{String.valueOf(id)}
                        );

                        pushed++;

                    } else {

                        failed++;
                    }

                } catch (Exception e) {

                    e.printStackTrace();
                    failed++;
                }
            }

            c.close();

            return new int[]{pushed, failed};
        }

        @Override
        protected void onPostExecute(int[] result) {

            btnPushFixes.setEnabled(true);

            Toast.makeText(
                    ScanActivity.this,
                    "Pushed: " + result[0] +
                            "\nFailed: " + result[1],
                    Toast.LENGTH_LONG
            ).show();
        }


    }


    private boolean sendFix(
            String qrCode,
            String variety,
            int quantity) {

        try {

            URL url = new URL(
                    "https://www.aaagrowers.co.ke/inventory/api/fix_inventory.php"
            );

            HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            String data =
                    "qr_code=" + URLEncoder.encode(qrCode, "UTF-8")
                            + "&variety=" + URLEncoder.encode(variety, "UTF-8")
                            + "&quantity=" + quantity;

            OutputStream os = conn.getOutputStream();
            os.write(data.getBytes("UTF-8"));
            os.flush();
            os.close();

            if (conn.getResponseCode() != 200) {
                return false;
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    conn.getInputStream()
                            )
                    );

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();

            String responseText = response.toString();

            android.util.Log.d("FIX_API", responseText);

            JSONObject json = new JSONObject(responseText);

            return json.optBoolean("success", false);

        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }

    }


    private void showEditDialog() {

        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(this);

        builder.setTitle("Fix Inventory Record");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        // Variety dropdown
        AutoCompleteTextView txtVariety =
                new AutoCompleteTextView(this);

        txtVariety.setHint("Select Variety");

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        dbHelper.getAllVarieties()
                );

        txtVariety.setAdapter(adapter);
        txtVariety.setThreshold(1);

        txtVariety.setOnClickListener(v ->
                txtVariety.showDropDown());

        txtVariety.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                txtVariety.showDropDown();
            }
        });

        // Quantity
        EditText txtQty = new EditText(this);
        txtQty.setHint("Quantity");
        txtQty.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
        );

        // Existing values
        txtVariety.setText(currentQR.varietyName, false);
        txtQty.setText(String.valueOf(currentQR.quantity));

        layout.addView(txtVariety);
        layout.addView(txtQty);

        builder.setView(layout);

        builder.setPositiveButton("Save", (d, w) -> {

            String variety =
                    txtVariety.getText().toString().trim();

            int qty = 0;

            try {
                qty = Integer.parseInt(
                        txtQty.getText().toString().trim()
                );
            } catch (Exception ignored) {
            }

            updateRecord(
                    currentQR.qrCode,
                    variety,
                    qty
            );
        });

        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void updateRecord(String qrCode,
                              String variety,
                              int qty) {

        dbHelper.saveInventoryFix(
                qrCode,
                currentQR.varietyName,
                variety,
                currentQR.quantity,
                qty
        );

        dbHelper.updateQR(
                qrCode,
                variety,
                qty
        );

        currentQR.varietyName = variety;
        currentQR.quantity = qty;

        resultText.setText(
                "QR: " + currentQR.qrCode +
                        "\nVariety: " + currentQR.varietyName +
                        "\nQuantity: " + currentQR.quantity +
                        "\nIntake: " + currentQR.intakeTime
        );

        Toast.makeText(this,
                "Record updated",
                Toast.LENGTH_SHORT).show();
    }

    // burst complete
    private final Runnable bufferFinish = () -> {

        String s = scanBuffer.toString().trim();
        scanBuffer.setLength(0);
        inputScan.setText("");

        if (!s.isEmpty())
            submitIfAllowed(s);
    };

    // duplicate prevention
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

    // process QR
    private void processScan(String code) {

        QRInventory qr = dbHelper.getQR(code, farm);

        if (qr != null) {

            currentQR = qr;

            resultText.setText(
                    "QR: " + qr.qrCode +
                            "\nVariety: " + qr.varietyName +
                            "\nQuantity: " + qr.quantity +
                            "\nIntake: " + qr.intakeTime
            );

            btnFix.setEnabled(true);
            btnCancel.setEnabled(true);

        } else {

            currentQR = null;

            Toast.makeText(this,
                    "QR code not found for farm " + farm,
                    Toast.LENGTH_SHORT).show();

            resultText.setText("No record found");

            btnFix.setEnabled(false);
            btnCancel.setEnabled(false);
        }
    }

    // Camera scan
    private void startQRScan() {

        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);

        barcodeLauncher.launch(options);
    }

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {

                if (result.getContents() != null) {

                    submitIfAllowed(result.getContents());

                }
            });

}
