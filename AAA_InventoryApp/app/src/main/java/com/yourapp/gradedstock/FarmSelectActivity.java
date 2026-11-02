package com.yourapp.gradedstock;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.yourapp.R;

import org.json.JSONArray;
import org.json.JSONObject;

public class FarmSelectActivity extends AppCompatActivity {

    private Spinner spFarm;

    private Button btnSync;
    private Button btnSyncScanOuts;

    private Button btnGraded;
    private Button btnScanOut;
    private Button btnScanOutRework;

    private LinearLayout operationsLayout;

    private int selectedFarmId = 0;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_farm_select
        );

        spFarm =
                findViewById(R.id.spFarm);

        btnSync =
                findViewById(R.id.btnSync);

        btnSyncScanOuts =
                findViewById(
                        R.id.btnSyncScanOuts
                );

        btnGraded =
                findViewById(R.id.btnGraded);

        btnScanOut =
                findViewById(R.id.btnScanOut);

        btnScanOutRework =
                findViewById(
                        R.id.btnScanOutRework
                );

        operationsLayout =
                findViewById(
                        R.id.operationsLayout
                );

        setupFarmSpinner();

        setupButtons();
    }

    // ============================================================
    // FARM SPINNER
    // ============================================================

    private void setupFarmSpinner() {

        String[] farms = {
                "Select Farm",
                "Simba",
                "Chui",
                "Chestnut"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout
                                .simple_spinner_item,
                        farms
                );

        adapter.setDropDownViewResource(
                android.R.layout
                        .simple_spinner_dropdown_item
        );

        spFarm.setAdapter(adapter);

        spFarm.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        selectedFarmId =
                                position;
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {

                        selectedFarmId = 0;
                    }
                }
        );
    }

    // ============================================================
    // BUTTONS
    // ============================================================

    private void setupButtons() {

        // --------------------------------------------------------
        // SYNC ACTIVE STOCK
        // --------------------------------------------------------

        btnSync.setOnClickListener(v -> {

            if (!requireFarm()) {
                return;
            }

            syncFromServer(
                    selectedFarmId
            );
        });

        // --------------------------------------------------------
        // MANUAL SCAN-OUT SYNC
        // --------------------------------------------------------

        btnSyncScanOuts.setOnClickListener(
                v -> syncScanOuts()
        );

        // --------------------------------------------------------
        // GRADED INTAKE
        // --------------------------------------------------------

        btnGraded.setOnClickListener(v -> {

            if (!requireFarm()) {
                return;
            }

            Intent intent =
                    new Intent(
                            this,
                            GradedEntryActivity.class
                    );

            intent.putExtra(
                    "farm_id",
                    selectedFarmId
            );

            startActivity(intent);
        });

        // --------------------------------------------------------
        // SCAN OUT
        // --------------------------------------------------------

        btnScanOut.setOnClickListener(v -> {

            if (!requireFarm()) {
                return;
            }

            Intent intent =
                    new Intent(
                            this,
                            ScanOutWipActivity.class
                    );

            intent.putExtra(
                    "farm_id",
                    selectedFarmId
            );

            startActivity(intent);
        });

        // --------------------------------------------------------
        // SCAN OUT REWORK
        // --------------------------------------------------------

        btnScanOutRework.setOnClickListener(v -> {

            if (!requireFarm()) {
                return;
            }

            Intent intent =
                    new Intent(
                            this,
                            ScanOutReworkActivity.class
                    );

            intent.putExtra(
                    "farm_id",
                    selectedFarmId
            );

            startActivity(intent);
        });
    }

    // ============================================================
    // FARM VALIDATION
    // ============================================================

    private boolean requireFarm() {

        if (selectedFarmId == 0) {

            toast(
                    "Select farm first"
            );

            return false;
        }

        return true;
    }

    // ============================================================
    // SYNC ACTIVE STOCK
    // ============================================================

    private void syncFromServer(
            int farmId
    ) {

        SyncManager.syncActiveStock(
                this,
                farmId,
                new SyncManager.SyncCallback() {

                    @Override
                    public void onSuccess() {

                        runOnUiThread(() -> {

                            toast(
                                    "Sync completed"
                            );

                            operationsLayout
                                    .setVisibility(
                                            View.VISIBLE
                                    );
                        });
                    }

                    @Override
                    public void onError(
                            String msg
                    ) {

                        runOnUiThread(() ->
                                toast(msg)
                        );
                    }
                }
        );
    }

    // ============================================================
    // MANUAL SCAN-OUT SYNC
    // ============================================================

    private void syncScanOuts() {

        new Thread(() -> {

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

                JSONArray arr =
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

                    arr.put(row);
                }

                c.close();

                if (arr.length() == 0) {

                    runOnUiThread(() ->
                            toast(
                                    "Nothing to sync"
                            )
                    );

                    return;
                }

                String response =
                        HttpHelper.postJson(
                                "https://www.aaagrowers.co.ke/inventory/graded/sync_scanouts.php",
                                arr.toString()
                        );

                Log.d(
                        "SCANOUT_SYNC",
                        "Server response: " +
                                response
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

                JSONArray successfulIds =
                        result.optJSONArray(
                                "successful_ids"
                        );

                JSONArray failedIds =
                        result.optJSONArray(
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

                // ------------------------------------------------
                // MARK ONLY SUCCESSFUL SCAN-OUTS
                // ------------------------------------------------

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
                         * The corresponding local release is
                         * now confirmed by the server.
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

                final int successFinal =
                        successfulCount;

                final int failedFinal =
                        failedCount;

                /*
                 * ------------------------------------------------
                 * Refresh server active stock only after the
                 * scan-out request has been processed.
                 * ------------------------------------------------
                 */
                if (selectedFarmId > 0) {

                    runOnUiThread(() -> {

                        SyncManager.syncActiveStock(
                                FarmSelectActivity.this,
                                selectedFarmId,
                                new SyncManager.SyncCallback() {

                                    @Override
                                    public void onSuccess() {

                                        if (
                                                failedFinal == 0
                                        ) {

                                            toast(
                                                    "Scan outs synced: " +
                                                            successFinal
                                            );

                                        } else {

                                            toast(
                                                    "Synced: " +
                                                            successFinal +
                                                            " | Failed: " +
                                                            failedFinal
                                            );
                                        }
                                    }

                                    @Override
                                    public void onError(
                                            String msg
                                    ) {

                                        if (
                                                failedFinal == 0
                                        ) {

                                            toast(
                                                    "Scan outs synced: " +
                                                            successFinal +
                                                            " | Refresh failed"
                                            );

                                        } else {

                                            toast(
                                                    "Synced: " +
                                                            successFinal +
                                                            " | Failed: " +
                                                            failedFinal
                                            );
                                        }
                                    }
                                }
                        );
                    });

                } else {

                    runOnUiThread(() -> {

                        if (
                                failedFinal == 0
                        ) {

                            toast(
                                    "Scan outs synced: " +
                                            successFinal
                            );

                        } else {

                            toast(
                                    "Synced: " +
                                            successFinal +
                                            " | Failed: " +
                                            failedFinal
                            );
                        }
                    });
                }

            } catch (Exception e) {

                e.printStackTrace();

                runOnUiThread(() ->
                        toast(
                                "Sync failed"
                        )
                );
            }

        }).start();
    }

    // ============================================================
    // TOAST
    // ============================================================

    private void toast(
            String message
    ) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }
}