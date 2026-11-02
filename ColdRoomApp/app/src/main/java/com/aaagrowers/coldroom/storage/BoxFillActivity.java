package com.aaagrowers.coldroom.storage;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.text.*;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.*;

import com.aaagrowers.coldroom.R;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.aaagrowers.coldroom.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

public class BoxFillActivity extends AppCompatActivity {

    private static final String HONEYWELL_ACTION = "com.honeywell.scan.RESULT";

    private Spinner spinnerFarm;
    private AutoCompleteTextView varietyInput;
    private EditText etLength, etStems, etScanSink;
    private Button btnSave;
    private RecyclerView rv;

    private TextView tvPending, tvSynced;
    private Button btnQuickSync;

    private EditText etPackDate;
    private String selectedDate = "";

    private int selectedFarmId = 0;
    private int selectedVarietyId = 0;

    private final List<BoxFillItem> scannedBoxes = new ArrayList<>();
    private final Set<Long> scannedBoxIds = new HashSet<>();

    private BoxFillAdapter adapter;
    private FlowerDbHelper dbHelper;

    private BroadcastReceiver honeywellReceiver;
    private long lastScanTs = 0;

    // =====================================================
    // LIFECYCLE
    // =====================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_box_fill);

        dbHelper = new FlowerDbHelper(this);

        spinnerFarm = findViewById(R.id.spinnerFarm);
        varietyInput = findViewById(R.id.varietyInput);
        etLength = findViewById(R.id.etBunches); // now LENGTH
        etStems = findViewById(R.id.etStems);
        etScanSink = findViewById(R.id.etScanSink);
        btnSave = findViewById(R.id.btnSaveBox);
        rv = findViewById(R.id.rvScannedBoxes);

        etPackDate = findViewById(R.id.etPackDate);

        setupDatePicker();

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BoxFillAdapter(scannedBoxes);
        rv.setAdapter(adapter);

        findViewById(R.id.btnSyncNow).setOnClickListener(v -> triggerManualSync());

        setupVarietyAutocomplete();
        loadFarmsOffline();
        setupHoneywellScanner();
        setupScanSink();

        tvPending = findViewById(R.id.tvPending);
        tvSynced = findViewById(R.id.tvSynced);
        btnQuickSync = findViewById(R.id.btnQuickSync);

        btnQuickSync.setOnClickListener(v -> triggerManualSync());

// ✅ NOW safe
        loadSyncCounts();

        btnQuickSync.setOnClickListener(v -> triggerManualSync());

        varietyInput.setEnabled(false);

        btnSave.setOnClickListener(v -> saveBoxesOffline());

        findViewById(R.id.btnScanBox).setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("Scan Box QR");
            integrator.setBeepEnabled(true);
            integrator.initiateScan();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerHoneywell();
        etScanSink.requestFocus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterHoneywell();
    }

    // =====================================================
    // ZXING RESULT
    // =====================================================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            handleScan(result.getContents().trim());
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // =====================================================
    // SYNC
    // =====================================================
    private void triggerManualSync() {

        Toast.makeText(this, "Syncing...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {

            boolean success = syncBoxFills();

            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Sync complete ✔", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Sync failed ❌", Toast.LENGTH_SHORT).show();
                }
                loadSyncCounts();
            });

        }).start();
    }

    private boolean syncBoxFills() {

        SQLiteDatabase rdb = dbHelper.getReadableDatabase();

        Cursor c = rdb.rawQuery(
                "SELECT " +
                        "c.box_id, " +
                        "b.qr_code, b.farm_id, " +
                        "c.variety_id, c.length, c.number_of_stems, " +
                        "c.updated_at " +
                        "FROM box_contents_local c " +
                        "JOIN boxes_local b ON b.id = c.box_id " +
                        "WHERE c.synced = 0",
                null
        );

        if (c.getCount() == 0) {
            c.close();
            return true; // nothing to sync
        }

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
                o.put("number_of_stems", c.getInt(5));
                o.put("updated_at", c.getString(6));

                payload.put(o);
                boxIds.add(boxId);
            }

            c.close();

            // 🔥 CALL API DIRECTLY
            ApiClient api = new ApiClient();
            boolean ok = api.post("/sync/box_storage.php", payload);

            if (!ok) return false;

            // ✅ MARK AS SYNCED
            SQLiteDatabase wdb = dbHelper.getWritableDatabase();
            wdb.beginTransaction();

            try {
                for (long boxId : boxIds) {
                    wdb.execSQL(
                            "UPDATE box_contents_local SET synced=1 WHERE box_id=?",
                            new Object[]{boxId}
                    );
                }

                wdb.setTransactionSuccessful();

            } finally {
                wdb.endTransaction();
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void loadSyncCounts() {

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c1 = db.rawQuery(
                "SELECT COUNT(*) FROM box_contents_local WHERE synced=0",
                null
        );

        if (c1.moveToFirst()) {
            tvPending.setText(String.valueOf(c1.getInt(0)));
        }
        c1.close();

        Cursor c2 = db.rawQuery(
                "SELECT COUNT(*) FROM box_contents_local WHERE synced=1",
                null
        );

        if (c2.moveToFirst()) {
            tvSynced.setText(String.valueOf(c2.getInt(0)));
        }
        c2.close();
    }

    // =====================================================
    // SCAN SINK
    // =====================================================
    private void setupScanSink() {

        etScanSink.setShowSoftInputOnFocus(false);

        etScanSink.setOnEditorActionListener((v, actionId, event) -> {
            boolean isEnter = actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER);

            if (!isEnter) return false;

            String scan = etScanSink.getText().toString().trim();
            etScanSink.setText("");

            if (!scan.isEmpty()) handleScan(scan);
            return true;
        });

        etScanSink.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                String scan = s.toString().trim();
                if (!scan.isEmpty()) {
                    etScanSink.setText("");
                    handleScan(scan);
                }
            }
        });
    }

    // =====================================================
    // HANDLE SCAN (FINAL FIXED)
    // =====================================================
    private void handleScan(String raw) {

        if (raw == null || raw.isEmpty()) {
            feedback(false);
            return;
        }

        // ✅ BLOCK scanning before required inputs
        if (selectedFarmId == 0 || selectedVarietyId == 0) {
            Toast.makeText(this, "Select farm & variety first", Toast.LENGTH_SHORT).show();
            feedback(false);
            return;
        }

        if (etLength.getText().toString().isEmpty() ||
                etStems.getText().toString().isEmpty()) {
            Toast.makeText(this, "Enter length & stems first", Toast.LENGTH_SHORT).show();
            feedback(false);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastScanTs < 300) return;
        lastScanTs = now;

        try {
            JSONObject obj = new JSONObject(raw);

            if (!obj.has("b")) {
                feedback(false);
                Toast.makeText(this, "Invalid QR", Toast.LENGTH_SHORT).show();
                return;
            }

            long boxId = obj.getLong("b");

            if (scannedBoxIds.contains(boxId)) {
                Toast.makeText(this, "Already scanned", Toast.LENGTH_SHORT).show();
                feedback(false);
                return;
            }

            // ✅ DB validation
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT status FROM boxes_local WHERE id=? LIMIT 1",
                    new String[]{String.valueOf(boxId)}
            );

            if (!c.moveToFirst()) {
                c.close();
                feedback(false);
                Toast.makeText(this, "Box not synced", Toast.LENGTH_SHORT).show();
                return;
            }

            String status = c.getString(0);
            c.close();

            if ("FILLED".equalsIgnoreCase(status)) {
                feedback(false);
                Toast.makeText(this, "Already filled", Toast.LENGTH_SHORT).show();
                return;
            }

            int length = Integer.parseInt(etLength.getText().toString());
            int stems = Integer.parseInt(etStems.getText().toString());

            scannedBoxIds.add(boxId);

            // ✅ store per scan
            scannedBoxes.add(new BoxFillItem(boxId, raw, length, stems));


            adapter.notifyItemInserted(scannedBoxes.size() - 1);
            rv.scrollToPosition(scannedBoxes.size() - 1);

            feedback(true);

        } catch (Exception e) {
            feedback(false);
            Toast.makeText(this, "Invalid QR", Toast.LENGTH_SHORT).show();
        }
    }

    // =====================================================
    // SAVE (FINAL FIXED)
    // =====================================================
    private void saveBoxesOffline() {

        // ✅ CHECK DATE FIRST
        if (selectedDate == null || selectedDate.isEmpty()) {
            Toast.makeText(this, "Select pack date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (scannedBoxes.isEmpty()) {
            Toast.makeText(this, "No boxes scanned", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedFarmId == 0 || selectedVarietyId == 0) {
            Toast.makeText(this, "Select farm & variety", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
        timeFormat.setTimeZone(TimeZone.getTimeZone("Africa/Nairobi"));

        String now = selectedDate + " " + timeFormat.format(new Date());

        db.beginTransaction();
        try {
            for (BoxFillItem item : scannedBoxes) {

                db.execSQL(
                        "INSERT OR REPLACE INTO boxes_local (id, qr_code, farm_id, status, synced) VALUES (?,?,?,?,0)",
                        new Object[]{item.boxId, item.qrRaw, selectedFarmId, "FILLED"}
                );

                db.execSQL(
                        "INSERT OR REPLACE INTO box_contents_local (box_id, variety_id, length, number_of_stems, updated_at, synced) VALUES (?,?,?,?,?,0)",
                        new Object[]{
                                item.boxId,
                                selectedVarietyId,
                                item.length,
                                item.stems,
                                now
                        }
                );
            }

            db.setTransactionSuccessful();

            scannedBoxes.clear();
            scannedBoxIds.clear();
            adapter.notifyDataSetChanged();

            etLength.setText("");
            etStems.setText("");
            etScanSink.requestFocus();

            Toast.makeText(this, "Saved ✔", Toast.LENGTH_LONG).show();
            loadSyncCounts();

        } finally {
            db.endTransaction();
        }
    }

    // =====================================================
    // FARMS + VARIETY (UNCHANGED)
    // =====================================================
    private void loadFarmsOffline() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id,name FROM farms_local ORDER BY name", null);

        List<String> names = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();

        names.add("-- Select Farm --");
        ids.add(0);

        while (c.moveToNext()) {
            ids.add(c.getInt(0));
            names.add(c.getString(1));
        }
        c.close();

        spinnerFarm.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, names));

        spinnerFarm.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedFarmId = ids.get(pos);
                selectedVarietyId = 0;
                varietyInput.setText("");
                varietyInput.setEnabled(pos > 0);
            }

            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupVarietyAutocomplete() {

        varietyInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                if (s.length() >= 1) fetchVarietiesOffline(s.toString());
            }
        });

        varietyInput.setOnItemClickListener((parent, view, position, id) -> {
            String name = (String) parent.getItemAtPosition(position);
            selectedVarietyId = resolveVarietyIdByName(name);
        });
    }

    private void fetchVarietiesOffline(String term) {
        if (selectedFarmId == 0) return;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT name FROM varieties_local WHERE farm_id=? AND name LIKE ?",
                new String[]{String.valueOf(selectedFarmId), "%" + term + "%"}
        );

        List<String> names = new ArrayList<>();
        while (c.moveToNext()) names.add(c.getString(0));
        c.close();

        if (names.isEmpty()) return;

        varietyInput.setAdapter(new VarietyAdapter(this, names));
        if (!varietyInput.isPopupShowing()) varietyInput.showDropDown();
    }

    private int resolveVarietyIdByName(String name) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id FROM varieties_local WHERE name=? AND farm_id=? LIMIT 1",
                new String[]{name, String.valueOf(selectedFarmId)}
        );

        int id = 0;
        if (c.moveToFirst()) id = c.getInt(0);
        c.close();
        return id;
    }

    // =====================================================
    // FEEDBACK
    // =====================================================
    private void feedback(boolean ok) {
        ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        tg.startTone(ok ? ToneGenerator.TONE_PROP_BEEP : ToneGenerator.TONE_PROP_NACK, 120);
    }

    // =====================================================
    // HONEYWELL
    // =====================================================
    private void setupHoneywellScanner() {
        honeywellReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String data = intent.getStringExtra("data");
                if (data != null) handleScan(data.trim());
            }
        };
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerHoneywell() {
        IntentFilter f = new IntentFilter(HONEYWELL_ACTION);
        if (Build.VERSION.SDK_INT >= 33)
            registerReceiver(honeywellReceiver, f, Context.RECEIVER_EXPORTED);
        else
            registerReceiver(honeywellReceiver, f);
    }

    private void unregisterHoneywell() {
        try { unregisterReceiver(honeywellReceiver); }
        catch (Exception ignored) {}
    }

    private void setupDatePicker() {

        Calendar calendar = Calendar.getInstance();

        // Default = today
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        selectedDate = sdf.format(calendar.getTime());
        etPackDate.setText(selectedDate);

        etPackDate.setOnClickListener(v -> {

            DatePickerDialog dp = new DatePickerDialog(
                    this,
                    (view, year, month, day) -> {
                        Calendar c = Calendar.getInstance();
                        c.set(year, month, day);

                        selectedDate = sdf.format(c.getTime());
                        etPackDate.setText(selectedDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            dp.show();
        });
    }
}