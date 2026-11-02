package com.aaagrowers.coldroom;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.aaagrowers.coldroom.storage.BoxFillActivity;
import com.aaagrowers.coldroom.storage.FlowerDbHelper;
import com.aaagrowers.coldroom.summers.SummersStockTakeActivity;
import com.aaagrowers.coldroom.summers.SummersVerifyActivity;
import com.aaagrowers.coldroom.sync.SyncScheduler;
import com.aaagrowers.coldroom.sync.SyncManager;
import com.aaagrowers.coldroom.warehouse.WarehouseStockScanActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.*;

import android.content.Intent;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String BASE_URL = "https://www.aaagrowers.co.ke/inventory/";

    private MaterialCardView btnSummers,
            btnSummersVerify,
            btndipScan,
            btnstorageScanout,
            btnInventoryScan,
            btnStockIntake,
            btnDashboard,
            btnstorage,
            btnGradedStockTake,
            btnstorageconfirm,
            btnstoragescan;
    private ApiClient apiClient;
    private FlowerDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialCardView btnDatabaseBackup =
                findViewById(R.id.btnDatabaseBackup);

        MaterialCardView btnDatabaseRestore =
                findViewById(R.id.btnDatabaseRestore);

        btnDatabaseBackup.setOnClickListener(v ->
                startActivity(new Intent(
                        MainActivity.this,
                        DatabaseBackupActivity.class
                ))
        );

        btnDatabaseRestore.setOnClickListener(v ->
                startActivity(new Intent(
                        MainActivity.this,
                        DatabaseRestoreActivity.class
                ))
        );

        apiClient = ApiClient.getInstance();
        dbHelper = new FlowerDbHelper(this);

        btnInventoryScan = findViewById(R.id.btnInventoryScan);
        btnStockIntake = findViewById(R.id.btnStockIntake);
        btnDashboard = findViewById(R.id.btnDashboard);
        btnGradedStockTake = findViewById(R.id.btnGradedStockTake);
        btnstorage = findViewById(R.id.btnstorage);
        btnstoragescan = findViewById(R.id.btnstoragescan);
        btnstorageconfirm = findViewById(R.id.btnstorageconfirm);
        btnstorageScanout = findViewById(R.id.btnstorageScanout);
        btndipScan = findViewById(R.id.btndipScan);
        btnSummers = findViewById(R.id.btnSummers);
        btnSummersVerify = findViewById(R.id.btnSummersVerify);

        btnSummers.setOnClickListener(v ->
                startActivity(new Intent(this, SummersStockTakeActivity.class)));

        btnSummersVerify.setOnClickListener(v ->
                startActivity(new Intent(this, SummersVerifyActivity.class)));

        btnStockIntake.setOnClickListener(v ->
                startActivity(new Intent(this, StockTakeActivity.class)));

        btnInventoryScan.setOnClickListener(v ->
                startActivity(new Intent(this, ConfirmActivity.class)));

        btnDashboard.setOnClickListener(v ->
                startActivity(new Intent(this, DashboardActivity.class)));

        btnGradedStockTake.setOnClickListener(v ->
                startActivity(new Intent(this, GradedStockTakeActivity.class)));

        btnstorage.setOnClickListener(v ->
                startActivity(new Intent(this, BoxFillActivity.class)));

        btnstoragescan.setOnClickListener(v ->
                startActivity(new Intent(this, WarehouseStockScanActivity.class)));

        btnstorageconfirm.setOnClickListener(v ->
                startActivity(new Intent(this, BoxSyncActivity.class)));

        btnstorageScanout.setOnClickListener(v ->
                startActivity(new Intent(this, ScanOutActivity.class)));

        btndipScan.setOnClickListener(v ->
                startActivity(new Intent(this, DipScanActivity.class)));

        // MASTER DATA SYNC
        syncFarmsAndVarieties();

        // BACKGROUND SYNC
        SyncScheduler.schedule(this);

        // IMMEDIATE PUSH SYNC
        new Thread(() -> new SyncManager(this).syncAll()).start();
    }

    // =============================
    // FARMS
    // =============================
    private void syncFarmsAndVarieties() {

        Toast.makeText(this, "Syncing master data...", Toast.LENGTH_SHORT).show();

        Request request = new Request.Builder()
                .url(BASE_URL + "get_farms.php")
                .get()
                .build();

        apiClient.getClient().newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Farm sync failed", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    JSONArray arr = new JSONArray(response.body().string());

                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.beginTransaction();

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject f = arr.getJSONObject(i);

                        db.execSQL(
                                "INSERT OR REPLACE INTO farms_local (id,name) VALUES (?,?)",
                                new Object[]{f.getInt("id"), f.getString("name")}
                        );
                    }

                    db.setTransactionSuccessful();
                    db.endTransaction();

                } catch (Exception e) {
                    Log.e(TAG, "Farm parse error", e);
                } finally {
                    response.close();
                    syncVarieties();
                }
            }
        });
    }

    // =============================
    // VARIETIES
    // =============================
    private void syncVarieties() {

        Request request = new Request.Builder()
                .url(BASE_URL + "fetch_varieties.php")
                .get()
                .build();

        apiClient.getClient().newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Variety sync failed", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    JSONArray arr = new JSONArray(response.body().string());

                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.beginTransaction();

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject v = arr.getJSONObject(i);

                        db.execSQL(
                                "INSERT OR REPLACE INTO varieties_local (id,farm_id,name) VALUES (?,?,?)",
                                new Object[]{
                                        v.getInt("VarietyId"),
                                        v.getInt("FarmId"),
                                        v.getString("VarietyName")
                                }
                        );
                    }

                    db.setTransactionSuccessful();
                    db.endTransaction();

                } catch (Exception e) {
                    Log.e(TAG, "Variety parse error", e);
                } finally {
                    response.close();
                    syncBoxes(); // 🔥 CRITICAL
                }
            }
        });
    }

    // =============================
    // BOXES (EMPTY BOXES SOURCE)
    // =============================
    private void syncBoxes() {

        Request request = new Request.Builder()
                .url(BASE_URL + "sync/get_boxes.php")
                .get()
                .build();

        apiClient.getClient().newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Box sync failed", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    JSONArray arr = new JSONArray(response.body().string());

                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.beginTransaction();

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject b = arr.getJSONObject(i);

                        db.execSQL(
                                "INSERT OR REPLACE INTO boxes_local (id, qr_code, farm_id, status, synced) VALUES (?,?,?,?,1)",
                                new Object[]{
                                        b.getLong("id"),
                                        b.getString("qr_code"),
                                        b.getInt("farm_id"),
                                        b.getString("status")
                                }
                        );
                    }

                    db.setTransactionSuccessful();
                    db.endTransaction();

                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "Master sync complete ✔",
                                    Toast.LENGTH_SHORT).show()
                    );

                    Log.d(TAG, "✅ Boxes synced: " + arr.length());

                } catch (Exception e) {
                    Log.e(TAG, "Box parse error", e);
                } finally {
                    response.close();
                }
            }
        });
    }
}