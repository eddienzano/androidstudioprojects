package com.aaagrowers.coldroom.summers;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.database.Cursor;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.aaagrowers.coldroom.R;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONObject;

public class SummersVerifyScanActivity extends AppCompatActivity {

    private EditText inputScan;
    private Button btnCameraScan;

    private TextView txtResult;
    private TextView txtDetails;
    private TextView txtCounter;

    private SummersDbHelper dbHelper;

    private String lastScan = "";
    private long lastScanTime = 0;

    private static final long DUPLICATE_LOCK = 2000;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final StringBuilder scanBuffer =
            new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(
                R.layout.activity_summers_verify_scan
        );

        inputScan =
                findViewById(R.id.inputScan);

        btnCameraScan =
                findViewById(R.id.btnCameraScan);

        txtResult =
                findViewById(R.id.txtResult);

        txtDetails =
                findViewById(R.id.txtDetails);

        txtCounter =
                findViewById(R.id.txtCounter);

        dbHelper =
                new SummersDbHelper(this);

        refreshCounter();

        btnCameraScan.setOnClickListener(v ->
                startCameraScan()
        );

        inputScan.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {}

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {}

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {

                        String value =
                                s.toString().trim();

                        if(value.length() >= 6)
                        {
                            handler.removeCallbacks(
                                    processBuffer
                            );

                            handler.postDelayed(
                                    processBuffer,
                                    100
                            );
                        }
                    }
                }
        );
    }

    private final Runnable processBuffer =
            () -> {

                String qr =
                        inputScan.getText()
                                .toString()
                                .trim();

                inputScan.setText("");

                if(!qr.isEmpty())
                {
                    processScan(qr);
                }
            };

    private void processScan(String qr) {

        String serial;
        String bucket;
        String farmQr;
        int lengthQr;
        String batch;

        try {

            String[] parts = qr.split("\\|");

            serial   = parts[0].split(":",2)[1].trim().toUpperCase();
            bucket   = parts[1].split(":",2)[1].trim();
            farmQr   = parts[2].split(":",2)[1].trim();
            lengthQr = Integer.parseInt(
                    parts[3].split(":",2)[1].trim()
            );

        } catch (Exception e) {
            showInvalid("Invalid QR Format");
            return;
        }

        long now = System.currentTimeMillis();

        if(serial.equals(lastScan)
                && now - lastScanTime < DUPLICATE_LOCK) {
            return;
        }

        lastScan = serial;
        lastScanTime = now;

        // 🔥 Get batch from serial
        batch = dbHelper.getBatchBySerial(serial);

        if(batch == null) {
            showInvalid("Batch not found for: " + serial);
            return;
        }

        // 🔥 Fetch ALL items in batch
        Cursor c = dbHelper.getQrDetailsByBatch(batch);

        if(!c.moveToFirst()) {
            showInvalid("No items found for batch: " + batch);
            c.close();
            return;
        }

        StringBuilder display = new StringBuilder();

        do {

            String dbSerial =
                    c.getString(c.getColumnIndexOrThrow("serial"));

            String variety =
                    c.getString(c.getColumnIndexOrThrow("variety_name"));

            String farm =
                    c.getString(c.getColumnIndexOrThrow("farm"));

            int qty =
                    c.getInt(c.getColumnIndexOrThrow("quantity"));

            int length =
                    c.getInt(c.getColumnIndexOrThrow("length"));

            String dbBatch =
                    c.getString(c.getColumnIndexOrThrow("batch_id"));

            // 🔥 LOG EACH ITEM
            android.util.Log.d("BATCH_SCAN",
                    "Serial=" + dbSerial +
                            " Variety=" + variety +
                            " Farm=" + farm +
                            " Qty=" + qty +
                            " Length=" + length +
                            " Batch=" + dbBatch
            );

            // build UI output
            display.append("Serial: ").append(dbSerial)
                    .append("\nVariety: ").append(variety)
                    .append("\nFarm: ").append(farm)
                    .append("\nQty: ").append(qty)
                    .append("\nLength: ").append(length)
                    .append("\nBatch: ").append(dbBatch)
                    .append("\n-----------------\n");

        } while (c.moveToNext());

        c.close();

        // 🔥 SIMPLE OUTPUT ONLY (NO VERIFY LOGIC YET)
        txtResult.setText("✓ BATCH LOADED");
        txtDetails.setText(display.toString());

        beepSuccess();
    }
    private void showSuccess(
            String serial,
            String farm,
            String variety,
            int qty,
            int length,
            String batch
    ) {

        txtResult.setText(
                "✓ VERIFIED"
        );

        txtDetails.setText(
                "Serial : " + serial +
                        "\nFarm : " + farm +
                        "\nVariety : " + variety +
                        "\nLength : " + length +
                        "\nQty : " + qty +
                        "\nBatch : " + batch
        );

        beepSuccess();
    }

    private void showInvalid(
            String qr
    ) {

        txtResult.setText(
                "✗ QR NOT FOUND"
        );

        txtDetails.setText(
                qr
        );

        beepFail();
    }

    private void showDuplicate(
            String serial,
            String variety
    ) {

        txtResult.setText(
                "⚠ ALREADY VERIFIED"
        );

        txtDetails.setText(
                serial +
                        "\n" +
                        variety
        );

        beepFail();
    }

    private void refreshCounter() {

        txtCounter.setText(
                "Verified Today: "
                        + dbHelper.getPendingCount()
        );
    }

    private void startCameraScan() {

        ScanOptions options =
                new ScanOptions();

        options.setPrompt(
                "Scan Summers QR"
        );

        options.setBeepEnabled(true);

        barcodeLauncher.launch(
                options
        );
    }

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions>
            barcodeLauncher =
            registerForActivityResult(
                    new ScanContract(),
                    result -> {

                        if(result.getContents()
                                != null)
                        {
                            processScan(
                                    result.getContents()
                            );
                        }
                    }
            );

    private void beepSuccess() {

        ToneGenerator tg =
                new ToneGenerator(
                        AudioManager.STREAM_MUSIC,
                        100
                );

        tg.startTone(
                ToneGenerator.TONE_PROP_ACK,
                150
        );
    }

    private void beepFail() {

        ToneGenerator tg =
                new ToneGenerator(
                        AudioManager.STREAM_MUSIC,
                        100
                );

        tg.startTone(
                ToneGenerator.TONE_PROP_NACK,
                300
        );
    }
}