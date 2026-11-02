package com.aaagrowers.coldroom.warehouse;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.*;
import android.text.*;
import android.view.Gravity;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import com.google.zxing.integration.android.*;
import com.aaagrowers.coldroom.R;
import com.aaagrowers.coldroom.storage.FlowerDbHelper;

import org.json.*;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class WarehouseStockScanActivity extends AppCompatActivity {

    private EditText etScanQr;
    private Spinner spinnerFarm;
    private TextView txtCount, tvRecent;
    private Button btnSync, btnCamera, btnPullBoxes;
    private RecyclerView rvScans;

    private FlowerDbHelper db;
    private final List<String> scannedQrs = new ArrayList<>();
    private WarehouseScanAdapter adapter;

    private int selectedFarmId = 0;

    private final Set<Long> scannedBoxIds = new HashSet<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80);
    private Vibrator vibrator;

    private static final String PULL_URL = "https://www.aaagrowers.co.ke/inventory/sync/get_filled_boxes.php";
    private static final String SYNC_URL = "https://www.aaagrowers.co.ke/inventory/sync/storage_stockscan.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warehouse_stock_scan);

        etScanQr = findViewById(R.id.etScanQr);
        spinnerFarm = findViewById(R.id.spinnerFarm);
        txtCount = findViewById(R.id.txtCount);
        tvRecent = findViewById(R.id.tvRecent);
        btnSync = findViewById(R.id.btnSync);
        btnCamera = findViewById(R.id.btnCameraScan);
        btnPullBoxes = findViewById(R.id.btnPullBoxes);
        rvScans = findViewById(R.id.rvScans);

        db = new FlowerDbHelper(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        setupRecycler();
        loadFarmsOffline();
        setupScanner();

        btnCamera.setOnClickListener(v -> startCameraScan());
        btnSync.setOnClickListener(v -> syncToServer());
        btnPullBoxes.setOnClickListener(v -> pullFilledBoxes());
    }

    // =====================================
    private void setupRecycler() {
        adapter = new WarehouseScanAdapter(scannedQrs);
        rvScans.setLayoutManager(new LinearLayoutManager(this));
        rvScans.setAdapter(adapter);
    }

    // =====================================
    private void loadFarmsOffline() {
        List<String> names = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();

        names.add("-- Select Farm --");
        ids.add(0);

        Cursor c = db.getReadableDatabase()
                .rawQuery("SELECT id,name FROM farms_local", null);

        while (c.moveToNext()) {
            ids.add(c.getInt(0));
            names.add(c.getString(1));
        }
        c.close();

        spinnerFarm.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, names));

        spinnerFarm.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                selectedFarmId = ids.get(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // =====================================
    // 🔥 STEP 1: PULL FILLED BOXES
    private void pullFilledBoxes() {
        new Thread(() -> {
            try {
                URL url = new URL(PULL_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                Scanner sc = new Scanner(conn.getInputStream());
                StringBuilder sb = new StringBuilder();
                while (sc.hasNext()) sb.append(sc.nextLine());

                JSONArray arr = new JSONArray(sb.toString());

                int insertedCount = 0; // <-- Count the boxes inserted

                SQLiteDatabase wdb = db.getWritableDatabase();
                wdb.beginTransaction();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);

                    wdb.execSQL(
                            "INSERT OR REPLACE INTO filled_boxes_local (id, qr_code, farm_id) VALUES (?,?,?)",
                            new Object[]{
                                    o.getLong("id"),
                                    o.getString("qr_code"),
                                    o.getInt("farm_id")
                            }
                    );

                    insertedCount++; // increment counter
                }

                wdb.setTransactionSuccessful();
                wdb.endTransaction();

                int finalCount = insertedCount;
                runOnUiThread(() -> showToast(finalCount + " boxes synced ✔"));

            } catch (Exception e) {
                runOnUiThread(() -> showToast("Pull failed: " + e.getMessage()));
            }
        }).start();
    }

    // =====================================
    private void setupScanner() {
        etScanQr.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            public void onTextChanged(CharSequence s,int a,int b,int c){}

            public void afterTextChanged(Editable s) {
                String qr = s.toString().trim();
                if (!qr.isEmpty()) {
                    etScanQr.setText("");
                    handleScan(qr);
                }
            }
        });
    }

    private void handleScan(String raw) {

        try {
            JSONObject obj = new JSONObject(raw);
            long boxId = obj.getLong("b");

            if (scannedBoxIds.contains(boxId)) {
                showToast("Already scanned");
                return;
            }

            Cursor c = db.getReadableDatabase().rawQuery(
                    "SELECT id FROM filled_boxes_local WHERE id=?",
                    new String[]{String.valueOf(boxId)}
            );

            if (!c.moveToFirst()) {
                showToast("Box not synced");
                return;
            }
            c.close();

            saveOffline(boxId, raw);

        } catch (Exception e) {
            showToast("Invalid QR");
        }
    }

    // =====================================
    private void saveOffline(long boxId, String qr) {

        try {
            SQLiteDatabase wdb = db.getWritableDatabase();

            wdb.execSQL(
                    "INSERT INTO warehouse_scans_local (farm_id, box_id, qr_code, scanned_at, synced) VALUES (?,?,?,?,0)",
                    new Object[]{selectedFarmId, boxId, qr, now()}
            );

            scannedBoxIds.add(boxId);
            scannedQrs.add(0, qr);

            adapter.notifyItemInserted(0);
            txtCount.setText("Scanned: " + scannedQrs.size());

            tone.startTone(ToneGenerator.TONE_PROP_BEEP);

        } catch (Exception e) {
            showToast("Duplicate blocked");
        }
    }

    // =====================================
    // 🔥 STEP 3: SYNC
    private void syncToServer() {

        new Thread(() -> {
            HttpURLConnection conn = null;

            try {
                Cursor c = db.getReadableDatabase().rawQuery(
                        "SELECT farm_id, box_id, scanned_at FROM warehouse_scans_local WHERE synced=0",
                        null
                );

                JSONArray arr = new JSONArray();

                while (c.moveToNext()) {
                    JSONObject o = new JSONObject();
                    o.put("farm_id", c.getInt(0));
                    o.put("box_id", c.getLong(1));
                    o.put("scanned_at", c.getString(2));
                    arr.put(o);
                }
                c.close();

                JSONObject payload = new JSONObject();
                payload.put("scans", arr);

                URL url = new URL(SYNC_URL);
                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                // Send payload
                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int code = conn.getResponseCode();

                // 🔥 Read response (important)
                Scanner scanner;
                if (code >= 200 && code < 300) {
                    scanner = new Scanner(conn.getInputStream());
                } else {
                    scanner = new Scanner(conn.getErrorStream());
                }

                StringBuilder response = new StringBuilder();
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
                scanner.close();

                String responseText = response.toString();

                // Log response for debugging
                android.util.Log.d("SYNC_RESPONSE", responseText);

                if (code == 200) {

                    db.getWritableDatabase().execSQL(
                            "UPDATE warehouse_scans_local SET synced=1 WHERE synced=0"
                    );

                    runOnUiThread(() -> showToast("Sync success ✔"));

                } else {
                    runOnUiThread(() -> showToast("Server error: " + code));
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("Sync failed: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }

    private void startCameraScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.initiateScan();
    }

    private void showToast(String msg) {
        mainHandler.post(() -> {
            Toast t = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
            t.setGravity(Gravity.CENTER,0,0);
            t.show();
        });
    }
}