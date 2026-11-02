package com.aaagrowers.coldroom;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.*;

import org.json.*;

import java.io.IOException;

import okhttp3.*;

public class ScanOutActivity extends AppCompatActivity {

    private EditText inputScan;
    private TextView tvValid, tvDuplicate, tvTotal;
    private Button btnSync, btnReset, btnCamera;

    private int validCount = 0, duplicateCount = 0;

    private DBHelper dbHelper;
    private int userId;

    private static final String BASE_URL = "https://www.aaagrowers.co.ke/inventory/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_out);

        inputScan = findViewById(R.id.inputScan);
        tvValid = findViewById(R.id.tvValid);
        tvDuplicate = findViewById(R.id.tvDuplicate);
        tvTotal = findViewById(R.id.tvTotal);
        btnSync = findViewById(R.id.btnSync);
        btnReset = findViewById(R.id.btnReset);
        btnCamera = findViewById(R.id.btnCameraScan);

        dbHelper = new DBHelper(this);

        userId = getIntent().getIntExtra("user_id", -1);

        addTextWatcher();

        btnCamera.setOnClickListener(v -> startQRScanner());

        btnSync.setOnClickListener(v -> syncData());

        btnReset.setOnClickListener(v -> resetSession());

        updateSyncCount();
    }

    private void addTextWatcher() {
        Handler handler = new Handler();

        inputScan.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacksAndMessages(null);

                if (s.length() > 6) {
                    handler.postDelayed(() -> {
                        String code = inputScan.getText().toString().trim();
                        inputScan.setText("");
                        processScan(code);
                    }, 200);
                }
            }

            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void processScan(String qrCode) {
        if (qrCode.isEmpty()) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT id FROM offline_scanout WHERE qr_code = ?",
                new String[]{qrCode}
        );

        if (cursor.getCount() > 0) {
            duplicateCount++;
            tvDuplicate.setText(String.valueOf(duplicateCount));
            Toast.makeText(this, "Duplicate", Toast.LENGTH_SHORT).show();
        } else {
            ContentValues values = new ContentValues();
            values.put("qr_code", qrCode);
            values.put("user_id", userId);
            values.put("synced", 0);

            db.insert("offline_scanout", null, values);

            validCount++;
            tvValid.setText(String.valueOf(validCount));
        }

        cursor.close();

        tvTotal.setText(String.valueOf(validCount + duplicateCount));

        updateSyncCount();
    }

    private void updateSyncCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM offline_scanout WHERE synced = 0", null
        );

        if (cursor.moveToFirst()) {
            int count = cursor.getInt(0);
            btnSync.setText("SYNC (" + count + ")");
        }

        cursor.close();
    }

    private void resetSession() {
        validCount = 0;
        duplicateCount = 0;

        tvValid.setText("0");
        tvDuplicate.setText("0");
        tvTotal.setText("0");
    }

    private void syncData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM offline_scanout WHERE synced = 0", null
        );

        JSONArray arr = new JSONArray();

        while (cursor.moveToNext()) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("qr_code", cursor.getString(cursor.getColumnIndexOrThrow("qr_code")));
                obj.put("user_id", cursor.getInt(cursor.getColumnIndexOrThrow("user_id")));
                arr.put(obj);
            } catch (JSONException e) {}
        }

        cursor.close();

        if (arr.length() == 0) {
            Toast.makeText(this, "Nothing to sync", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();



        JSONObject wrapper = new JSONObject();
        try {
            wrapper.put("scans", arr);
        } catch (JSONException e) {}

        RequestBody body = RequestBody.create(
                wrapper.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "sync/box_scanout_sync.php")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(ScanOutActivity.this, "Sync failed", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                String resp = response.body().string();

                try {
                    JSONObject json = new JSONObject(resp);
                    String status = json.optString("status");

                    runOnUiThread(() -> {
                        if ("success".equals(status)) {
                            markSynced();
                            Toast.makeText(ScanOutActivity.this, "Synced!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ScanOutActivity.this, "Server error", Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(ScanOutActivity.this, "Invalid response", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void markSynced() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("UPDATE offline_scanout SET synced = 1 WHERE synced = 0");
        updateSyncCount();
    }

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    processScan(result.getContents());
                }
            });

    private void startQRScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan QR Code");
        options.setBeepEnabled(true);
        barcodeLauncher.launch(options);
    }
}