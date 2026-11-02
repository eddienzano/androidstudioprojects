package com.aaagrowers.coldroom.summers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aaagrowers.coldroom.R;
import com.aaagrowers.coldroom.stocks.VarietyDb;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SummersScanActivity extends Activity {

    private static final long SCAN_THRESHOLD = 40;
    private static final long SAME_QR_LOCK_MS = 1000;

    private EditText input;
    private Button btnCamera, btnSend;

    private TextView tvTotal, tvValid, tvDuplicate, tvSyncStats;

    private RecyclerView recyclerScans;

    private SummersDbHelper dbHelper;

    private String farm;

    private int total = 0;
    private int valid = 0;
    private int duplicates = 0;
    private int invalid = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private long lastCharTime = 0;
    private final StringBuilder scanBuffer = new StringBuilder();

    private String lastQr = "";
    private long lastScanTime = 0;

    private final ArrayList<SummersScanItem> scanList = new ArrayList<>();
    private SummersScanAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summers_scan);

        farm = getIntent().getStringExtra("farm");
        dbHelper = new SummersDbHelper(this);

        input = findViewById(R.id.inputScan);
        btnCamera = findViewById(R.id.btnCameraScan);
        btnSend = findViewById(R.id.btnSendDb);

        tvValid = findViewById(R.id.tvValid);
        tvDuplicate = findViewById(R.id.tvDuplicate);
        tvTotal = findViewById(R.id.tvTotal);
        tvSyncStats = findViewById(R.id.tvSyncStats);

        recyclerScans = findViewById(R.id.recyclerScans);

        adapter = new SummersScanAdapter(scanList);
        recyclerScans.setLayoutManager(new LinearLayoutManager(this));
        recyclerScans.setAdapter(adapter);

        loadExistingScans();
        refreshSyncStats();
        updateUI();

        btnCamera.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Scan Summers QR");
            integrator.setBeepEnabled(true);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });

        btnSend.setOnClickListener(v -> {

            SummersSyncRepository.sendToServer(this);

            SummersScanBackRepository.sendToServer(this);

            SummersInventoryFixRepository.sendToServer(this);

            handler.postDelayed(() -> {

                refreshSyncStats();
                loadExistingScans();

            }, 2000);
        });

        dbHelper = new SummersDbHelper(this);

        if (dbHelper.getVarietyCount() == 0) {
            SummersVarietySyncRepository.sync(this);
        }

        input.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}

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

                    String qr = text.trim();
                    input.setText("");
                    scanBuffer.setLength(0);

                    if (!qr.isEmpty()) process(qr);
                    return;
                }

                handler.removeCallbacks(finishRunnable);
                handler.postDelayed(finishRunnable, 80);
            }
        });
    }

    private final Runnable finishRunnable = () -> {
        String qr = scanBuffer.toString().trim();
        scanBuffer.setLength(0);
        input.setText("");

        if (!qr.isEmpty()) process(qr);
    };

    private void process(String qr) {

        qr = qr == null ? "" : qr.trim();

        if (qr.isEmpty()) {
            Toast.makeText(this, "Empty QR ignored", Toast.LENGTH_SHORT).show();
            return;
        }

        long now = System.currentTimeMillis();

        if (qr.equals(lastQr) && (now - lastScanTime) < SAME_QR_LOCK_MS) {
            return;
        }

        lastQr = qr;
        lastScanTime = now;

        total++;

        Cursor c = dbHelper.getQrDetailsByQr(qr);

        if (c == null || !c.moveToFirst()) {

            invalid++;

            vibrateError();
            addRecentScan(qr, "UNKNOWN QR");

            showFixDialog(qr);

            updateUI();
            return;
        }

        ArrayList<QrContentItem> items = new ArrayList<>();

        do {

            String variety =
                    c.getString(
                            c.getColumnIndexOrThrow("variety_name")
                    );

            int qty =
                    c.getInt(
                            c.getColumnIndexOrThrow("quantity")
                    );

            items.add(
                    new QrContentItem(
                            variety,
                            qty
                    )
            );

        } while (c.moveToNext());

        c.close();

        showQrDialog(qr, items);
    }

    private Map<String, String> parseQr(String qr) {

        Map<String, String> map = new HashMap<>();

        if (qr == null || qr.trim().isEmpty()) return map;

        String[] parts = qr.split("\\|");

        for (String part : parts) {

            String[] kv = part.split(":", 2); // safer split (only first colon)

            if (kv.length == 2) {
                map.put(kv[0].trim().toUpperCase(), kv[1].trim());
            }
        }

        return map;
    }

    private void showFixDialog(String qr) {

        Map<String, String> qrData = parseQr(qr);

        String serial = qrData.getOrDefault("SERIAL", "");
        String bucket = qrData.getOrDefault("BUCKET", "");
        String farmFromQr = qrData.getOrDefault("FARM", "");
        String lengthStr = qrData.getOrDefault("LENGTH", "0");

        runOnUiThread(() -> {

            View view = getLayoutInflater().inflate(R.layout.dialog_fix_qr, null);

            EditText inputFarm = view.findViewById(R.id.inputFarm);
            AutoCompleteTextView inputVariety = view.findViewById(R.id.inputVariety);
            EditText inputQty = view.findViewById(R.id.inputQty);
            Button btnAdd = view.findViewById(R.id.btnAddVariety);
            TextView tvList = view.findViewById(R.id.tvFixList);

            ArrayList<QrFixItem> fixItems = new ArrayList<>();

            final int[] selectedVarietyId = {0};
            final String[] selectedVarietyName = {""};

            // ================================
            // AUTO FILL FARM
            // ================================
            if (!farmFromQr.isEmpty()) {
                inputFarm.setText(farmFromQr);
            }

            // ================================
            // VARIETY SEARCH (SAFE VERSION)
            // ================================
            inputVariety.addTextChangedListener(new TextWatcher() {

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {

                    String term = s.toString().trim();
                    if (term.length() < 2) return;

                    new Thread(() -> {

                        ArrayList<SummersDbHelper.VarietyItem> list =
                                dbHelper.searchVarieties(term);

                        runOnUiThread(() -> {

                            ArrayAdapter<SummersDbHelper.VarietyItem> adapter =
                                    new ArrayAdapter<>(
                                            SummersScanActivity.this,
                                            android.R.layout.simple_dropdown_item_1line,
                                            list
                                    );

                            inputVariety.setAdapter(adapter);
                            inputVariety.showDropDown();

                            inputVariety.setOnItemClickListener((parent, v, pos, id) -> {

                                SummersDbHelper.VarietyItem item = adapter.getItem(pos);

                                if (item != null) {
                                    selectedVarietyId[0] = item.id;
                                    selectedVarietyName[0] = item.name;
                                    inputVariety.setText(item.name, false);
                                }
                            });
                        });

                    }).start();
                }
            });

            // ================================
            // ADD ITEM
            // ================================
            btnAdd.setOnClickListener(v -> {

                String qtyStr = inputQty.getText().toString().trim();

                if (selectedVarietyId[0] == 0 || qtyStr.isEmpty()) {
                    Toast.makeText(this, "Select variety + qty", Toast.LENGTH_SHORT).show();
                    return;
                }

                int qty = Integer.parseInt(qtyStr);

                fixItems.add(new QrFixItem(
                        selectedVarietyId[0],
                        selectedVarietyName[0],
                        qty
                ));

                inputVariety.setText("");
                inputQty.setText("");
                selectedVarietyId[0] = 0;
                selectedVarietyName[0] = "";

                StringBuilder sb = new StringBuilder();
                for (QrFixItem i : fixItems) {
                    sb.append(i.name).append(" x ").append(i.qty).append("\n");
                }

                tvList.setText(sb.toString());
            });

            // ================================
            // BUILD DIALOG (IMPORTANT FIX)
            // ================================
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Fix Missing QR (Multi-Variety)")
                    .setView(view)
                    .setPositiveButton("Save", null)
                    .setNegativeButton("Cancel", (d, w) -> {
                        addRecentScan(qr, "IGNORED FIX");
                    })
                    .create();

            dialog.setOnShowListener(d -> {

                Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

                btnSave.setOnClickListener(v -> {

                    String farmInput = inputFarm.getText().toString().trim();
                    String farm = !farmFromQr.isEmpty() ? farmFromQr : farmInput;

                    int length = 0;
                    try {
                        length = Integer.parseInt(lengthStr);
                    } catch (Exception ignored) {}

                    // ================================
                    // VALIDATION
                    // ================================
                    if (serial.isEmpty()) {
                        Toast.makeText(this, "Invalid QR (missing serial)", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (farm.isEmpty()) {
                        Toast.makeText(this, "Farm required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (fixItems.isEmpty()) {
                        Toast.makeText(this, "Add at least one variety", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // ================================
                    // HEADER INSERT
                    // ================================
                    dbHelper.insertQrFixHeader(
                            qr,
                            serial,
                            farm,
                            length,
                            bucket
                    );

                    // ================================
                    // ITEMS INSERT
                    // ================================
                    for (QrFixItem item : fixItems) {

                        dbHelper.insertQrFixItem(
                                qr,
                                item.varietyId,
                                item.name,
                                item.qty
                        );
                    }


// SCAN INSERT (SAME STYLE AS showQrDialog)
// ================================

                    boolean isDup = dbHelper.isDuplicateScan(qr, farm);

// IMPORTANT: single scan entry ONLY
                    dbHelper.insertScan(qr, farm);

                    if (isDup) duplicates++;
                    else valid++;

                    playSuccessSound();

// You can still show more detailed label if you want
                    addRecentScan(
                            qr,
                            isDup ? "FIXED DUPLICATE" : "FIXED VALID"
                    );

                    updateUI();
                    refreshSyncStats();

                    dialog.dismiss();
                });
            });

            dialog.show();
        });
    }
    private void markScan(String qr, String label) {
        valid++;
        addRecentScan(qr, label);
        updateUI();
        refreshSyncStats();
    }

    private void showQrDialog(String qr, ArrayList<QrContentItem> items) {

        View view = getLayoutInflater().inflate(R.layout.dialog_qr_confirm, null);

        TextView tvQrCode = view.findViewById(R.id.tvQrCode);
        TextView tvTotalItems = view.findViewById(R.id.tvTotal);
        RecyclerView recycler = view.findViewById(R.id.recyclerQrItems);

        tvQrCode.setText("QR: " + qr);

        int sum = 0;
        for (QrContentItem i : items) sum += i.quantity;

        tvTotalItems.setText("Total: " + sum);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(new QrContentAdapter(items));

        new AlertDialog.Builder(this)
                .setView(view)

                .setPositiveButton("YES", (d, w) -> {

                    boolean isDup =
                            dbHelper.isDuplicateScan(qr, farm);

                    dbHelper.insertScan(qr, farm);

                    if (isDup)
                        duplicates++;
                    else
                        valid++;

                    playSuccessSound();

                    addRecentScan(
                            qr,
                            isDup ? "DUPLICATE" : "VALID"
                    );

                    updateUI();
                    refreshSyncStats();
                })

                .setNeutralButton("FIX", (d, w) -> {

                    showFixExistingQrDialog(
                            qr,
                            items
                    );

                })

                .setNegativeButton("CANCEL", (d, w) -> {

                    addRecentScan(
                            qr,
                            "CANCELLED"
                    );

                    updateUI();
                })

                .show();
    }

    private void showFixExistingQrDialog(
            String qr,
            ArrayList<QrContentItem> items
    ) {

        String[] options =
                new String[items.size()];

        for (int i = 0; i < items.size(); i++) {

            options[i] =
                    items.get(i).varietyName
                            + " x "
                            + items.get(i).quantity;
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Variety To Fix")
                .setItems(options, (d, which) -> {

                    showEditVarietyDialog(
                            qr,
                            items.get(which)
                    );

                })
                .show();
    }

    private void showEditVarietyDialog(
            String qr,
            QrContentItem item
    ) {

        View view =
                getLayoutInflater()
                        .inflate(
                                R.layout.dialog_fix_qr,
                                null
                        );

        AutoCompleteTextView inputVariety =
                view.findViewById(R.id.inputVariety);

        EditText inputQty =
                view.findViewById(R.id.inputQty);

        EditText inputFarm =
                view.findViewById(R.id.inputFarm);

        Button btnAdd =
                view.findViewById(R.id.btnAddVariety);

        TextView tvList =
                view.findViewById(R.id.tvFixList);

        inputFarm.setVisibility(View.GONE);
        btnAdd.setVisibility(View.GONE);
        tvList.setVisibility(View.GONE);

        inputVariety.setText(
                item.varietyName,
                false
        );

        inputQty.setText(
                String.valueOf(item.quantity)
        );

        final int[] selectedVarietyId = {0};
        final String[] selectedVarietyName =
                {item.varietyName};

        inputVariety.addTextChangedListener(
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
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {

                        String term =
                                s.toString().trim();

                        if (term.length() < 2)
                            return;

                        new Thread(() -> {

                            ArrayList<SummersDbHelper.VarietyItem> list =
                                    dbHelper.searchVarieties(term);

                            runOnUiThread(() -> {

                                ArrayAdapter<SummersDbHelper.VarietyItem> adapter =
                                        new ArrayAdapter<>(
                                                SummersScanActivity.this,
                                                android.R.layout.simple_dropdown_item_1line,
                                                list
                                        );

                                inputVariety.setAdapter(adapter);
                                inputVariety.showDropDown();

                                inputVariety.setOnItemClickListener(
                                        (parent, v, pos, id) -> {

                                            SummersDbHelper.VarietyItem selected =
                                                    adapter.getItem(pos);

                                            if (selected != null) {

                                                selectedVarietyId[0] =
                                                        selected.id;

                                                selectedVarietyName[0] =
                                                        selected.name;
                                            }
                                        });
                            });

                        }).start();
                    }
                });

        new AlertDialog.Builder(this)
                .setTitle("Fix Variety")
                .setView(view)

                .setPositiveButton("Save", (d, w) -> {

                    int qty;

                    try {

                        qty = Integer.parseInt(
                                inputQty.getText()
                                        .toString()
                                        .trim()
                        );

                    } catch (Exception e) {

                        Toast.makeText(
                                this,
                                "Invalid quantity",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    String variety =
                            inputVariety.getText()
                                    .toString()
                                    .trim();

                    String batchId =
                            dbHelper.getBatchByQr(qr);

                    // Queue fix first
                    dbHelper.insertInventoryFix(
                            batchId,
                            item.varietyName,
                            item.quantity,
                            variety,
                            qty
                    );

// Then perform the stocktake
                    boolean isDup = dbHelper.isDuplicateScan(qr, farm);

                    dbHelper.insertScan(qr, farm);

                    if (isDup)
                        duplicates++;
                    else
                        valid++;

                    playSuccessSound();

                    addRecentScan(
                            qr,
                            isDup
                                    ? "FIXED & DUPLICATE"
                                    : "FIXED & VALID"
                    );

                    updateUI();
                    refreshSyncStats();

                    Toast.makeText(
                            this,
                            "Fix saved and stocktake completed",
                            Toast.LENGTH_LONG
                    ).show();

                    d.dismiss();

                })

                .setNegativeButton(
                        "Cancel",
                        null
                )

                .show();
    }

    private void addRecentScan(String qr, String status) {

        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new Date());

        scanList.add(0, new SummersScanItem(qr, status, time));
        adapter.notifyItemInserted(0);
        recyclerScans.scrollToPosition(0);
    }

    private void loadExistingScans() {

        scanList.clear();

        Cursor c = dbHelper.getRecentScans();

        while (c.moveToNext()) {

            String qr = c.getString(0);
            int synced = c.getInt(1);
            String time = c.getString(2);

            String status = synced == 1 ? "PENDING" : "SYNCED";

            scanList.add(new SummersScanItem(qr, status, time));
        }

        c.close();
        adapter.notifyDataSetChanged();
    }

    private void refreshSyncStats() {

        int pending = dbHelper.getPendingCount();
        int synced = dbHelper.getSyncedCount();

        tvSyncStats.setText("Synced: " + pending + " | Pending: " + synced);
    }

    private void updateUI() {

        tvTotal.setText(String.valueOf(total));
        tvValid.setText(String.valueOf(valid));
        tvDuplicate.setText(String.valueOf(duplicates));
    }

    private void playSuccessSound() {
        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
    }

    private void vibrateError() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (v == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(300);
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(req, res, data);

        if (result != null && result.getContents() != null) {
            process(result.getContents().trim());
            return;
        }

        super.onActivityResult(req, res, data);
    }

    class QrFixItem {
        int varietyId;
        String name;
        int qty;

        QrFixItem(int id, String name, int qty) {
            this.varietyId = id;
            this.name = name;
            this.qty = qty;
        }
    }
}