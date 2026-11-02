package com.aaagrowers.scoutingapp.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aaagrowers.scoutingapp.R;
import com.aaagrowers.scoutingapp.auth.SessionManager;
import com.aaagrowers.scoutingapp.data.database.AppDatabase;
import com.aaagrowers.scoutingapp.data.database.dao.FcmTrapDao;
import com.aaagrowers.scoutingapp.data.database.entity.FcmTrapEntity;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/*
 * ================================================================
 * TRAP MONITORING ACTIVITY
 * ================================================================
 *
 * PURPOSE
 *
 * This activity is the entry point for field trap monitoring.
 *
 * WORKFLOW
 *
 * TRAP MONITORING
 *       |
 *       v
 * SCAN TRAP QR
 *       |
 *       v
 * FIND TRAP LOCALLY
 *       |
 *       v
 * IDENTIFY TRAP TYPE
 *       |
 *       +----------------------+
 *       |                      |
 *       v                      v
 *      FCM              GENERAL MOTH
 *       |                      |
 *       v                      v
 *   FCM COUNT             INSECT TYPE
 *                              |
 *                              v
 *                           COUNT
 *
 * Later:
 *
 * COUNT
 *   |
 *   v
 * TREND
 *   |
 *   v
 * COMMENTS
 *   |
 *   +------> PHOTO
 *   |
 *   +------> GPS
 *   |
 *   v
 * SAVE LOCAL
 *   |
 *   v
 * SYNC SERVER
 *
 * ================================================================
 */

public class TrapMonitoringActivity extends AppCompatActivity {


    /*
     * ================================================================
     * SESSION
     * ================================================================
     */

    private SessionManager sessionManager;


    /*
     * ================================================================
     * DATABASE
     * ================================================================
     */

    private FcmTrapDao fcmTrapDao;

    private ExecutorService trapExecutor;


    /*
     * ================================================================
     * VIEWS
     * ================================================================
     */

    private Button scanTrapButton;

    private ImageButton backButton;

    private TextView trapsMonitoredText;

    private TextView fcmObservationsText;

    private TextView otherObservationsText;

    private TextView lastMonitoringText;


    /*
     * ================================================================
     * QR SCANNER
     * ================================================================
     *
     * Uses the same JourneyApps ZXing scanner implementation as
     * ScoutingActivity.
     *
     * The scanner returns the raw QR contents.
     *
     * We then pass those contents to handleQrResult().
     *
     * ================================================================
     */

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions>
            qrScannerLauncher =
            registerForActivityResult(
                    new ScanContract(),
                    result -> {

                        if (result == null) {
                            return;
                        }


                        String contents =
                                result.getContents();


                        /*
                         * ------------------------------------------------
                         * SCAN CANCELLED / EMPTY RESULT
                         * ------------------------------------------------
                         */

                        if (
                                contents == null
                                        ||
                                        contents.trim().isEmpty()
                        ) {

                            Toast.makeText(
                                    TrapMonitoringActivity.this,
                                    "QR scan cancelled.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }


                        /*
                         * ------------------------------------------------
                         * HANDLE QR
                         * ------------------------------------------------
                         */

                        handleQrResult(
                                contents
                        );
                    }
            );


    /*
     * ================================================================
     * ON CREATE
     * ================================================================
     */

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );


        /*
         * ------------------------------------------------------------
         * LAYOUT
         * ------------------------------------------------------------
         */

        setContentView(
                R.layout.activity_trap_monitoring
        );


        /*
         * ------------------------------------------------------------
         * SESSION
         * ------------------------------------------------------------
         */

        sessionManager =
                new SessionManager(
                        this
                );


        if (
                !sessionManager.isLoggedIn()
        ) {

            finish();

            return;
        }


        /*
         * ------------------------------------------------------------
         * DATABASE
         * ------------------------------------------------------------
         */

        AppDatabase database =
                AppDatabase.getInstance(
                        this
                );


        fcmTrapDao =
                database.fcmTrapDao();


        trapExecutor =
                Executors.newSingleThreadExecutor();


        /*
         * ------------------------------------------------------------
         * VIEWS
         * ------------------------------------------------------------
         */

        scanTrapButton =
                findViewById(
                        R.id.scanTrapButton
                );


        backButton =
                findViewById(
                        R.id.backButton
                );


        trapsMonitoredText =
                findViewById(
                        R.id.trapsMonitoredText
                );


        fcmObservationsText =
                findViewById(
                        R.id.fcmObservationsText
                );


        otherObservationsText =
                findViewById(
                        R.id.otherObservationsText
                );


        lastMonitoringText =
                findViewById(
                        R.id.lastMonitoringText
                );


        /*
         * ------------------------------------------------------------
         * INITIAL STATE
         * ------------------------------------------------------------
         */

        trapsMonitoredText.setText(
                "0"
        );


        fcmObservationsText.setText(
                "0"
        );


        otherObservationsText.setText(
                "0"
        );


        lastMonitoringText.setText(
                "No observations yet"
        );


        /*
         * ------------------------------------------------------------
         * LOAD LOCAL TRAP DATA
         * ------------------------------------------------------------
         */

        loadTrapSummary();


        /*
         * ------------------------------------------------------------
         * SCAN TRAP
         * ------------------------------------------------------------
         */

        scanTrapButton.setOnClickListener(
                view ->
                        startTrapScan()
        );


        /*
         * ------------------------------------------------------------
         * BACK
         * ------------------------------------------------------------
         */

        backButton.setOnClickListener(
                view ->
                        finish()
        );
    }


    /*
     * ================================================================
     * LOAD TRAP SUMMARY
     * ================================================================
     */

    private void loadTrapSummary() {

        trapExecutor.execute(
                () -> {

                    try {

                        int trapCount =
                                fcmTrapDao.getActiveTrapCount();


                        runOnUiThread(
                                () -> {

                                    trapsMonitoredText.setText(
                                            String.valueOf(
                                                    trapCount
                                            )
                                    );


                                    if (trapCount > 0) {

                                        lastMonitoringText.setText(
                                                "Traps available offline"
                                        );

                                    } else {

                                        lastMonitoringText.setText(
                                                "No active traps found"
                                        );
                                    }
                                }
                        );


                    } catch (Exception e) {

                        e.printStackTrace();


                        runOnUiThread(
                                () -> {

                                    trapsMonitoredText.setText(
                                            "0"
                                    );


                                    lastMonitoringText.setText(
                                            "Unable to load trap data"
                                    );
                                }
                        );
                    }
                }
        );
    }


    /*
     * ================================================================
     * START TRAP SCAN
     * ================================================================
     *
     * Opens the JourneyApps ZXing QR scanner.
     *
     * This follows the same implementation used by
     * ScoutingActivity.
     *
     * ================================================================
     */

    private void startTrapScan() {

        ScanOptions options =
                new ScanOptions();


        /*
         * ------------------------------------------------------------
         * SCANNER PROMPT
         * ------------------------------------------------------------
         */

        options.setPrompt(
                "Scan the QR code on the trap"
        );


        /*
         * ------------------------------------------------------------
         * BEEP
         * ------------------------------------------------------------
         */

        options.setBeepEnabled(
                true
        );


        /*
         * ------------------------------------------------------------
         * ORIENTATION
         * ------------------------------------------------------------
         */

        options.setOrientationLocked(
                false
        );


        /*
         * ------------------------------------------------------------
         * QR ONLY
         * ------------------------------------------------------------
         */

        options.setDesiredBarcodeFormats(
                ScanOptions.QR_CODE
        );


        /*
         * ------------------------------------------------------------
         * OPEN CAMERA
         * ------------------------------------------------------------
         */

        qrScannerLauncher.launch(
                options
        );
    }


    /*
     * ================================================================
     * HANDLE QR RESULT
     * ================================================================
     *
     * The scanner returns the raw contents of the QR code.
     *
     * For trap monitoring this should normally be the secure
     * QR token stored in:
     *
     *     fcm_traps.qr_code
     *
     * Example:
     *
     * QR
     *   |
     *   v
     * secure token
     *   |
     *   v
     * fcm_traps.qr_code
     *   |
     *   v
     * FcmTrapEntity
     *
     * ================================================================
     */

    private void handleQrResult(
            String contents
    ) {

        if (
                TextUtils.isEmpty(
                        contents
                )
        ) {

            return;
        }


        final String qrValue =
                contents.trim();


        /*
         * ------------------------------------------------------------
         * UPDATE UI
         * ------------------------------------------------------------
         */

        lastMonitoringText.setText(
                "QR scanned • identifying trap"
        );


        /*
         * ------------------------------------------------------------
         * LOOK UP TRAP
         * ------------------------------------------------------------
         */

        findTrapByQrCode(
                qrValue
        );
    }


    /*
     * ================================================================
     * FIND TRAP BY QR CODE
     * ================================================================
     *
     * Looks up the scanned QR token in the local Room database.
     *
     * This allows trap monitoring to work completely offline.
     *
     * ================================================================
     */

    private void findTrapByQrCode(
            String qrCode
    ) {

        if (
                qrCode == null
                        ||
                        qrCode.trim().isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "Invalid trap QR code.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        /*
         * ------------------------------------------------------------
         * DISABLE SCAN BUTTON
         * ------------------------------------------------------------
         */

        scanTrapButton.setEnabled(
                false
        );


        scanTrapButton.setText(
                "IDENTIFYING TRAP..."
        );


        /*
         * ------------------------------------------------------------
         * LOCAL DATABASE LOOKUP
         * ------------------------------------------------------------
         */

        trapExecutor.execute(
                () -> {

                    try {

                        FcmTrapEntity trap =
                                fcmTrapDao.findByQrCode(
                                        qrCode.trim()
                                );


                        runOnUiThread(
                                () -> {

                                    /*
                                     * ------------------------------------------------
                                     * RESTORE BUTTON
                                     * ------------------------------------------------
                                     */

                                    scanTrapButton.setEnabled(
                                            true
                                    );


                                    scanTrapButton.setText(
                                            "SCAN TRAP QR     →"
                                    );


                                    /*
                                     * ------------------------------------------------
                                     * TRAP NOT FOUND
                                     * ------------------------------------------------
                                     */

                                    if (trap == null) {

                                        showTrapNotFound();

                                        return;
                                    }


                                    /*
                                     * ------------------------------------------------
                                     * TRAP FOUND
                                     * ------------------------------------------------
                                     */

                                    openTrapObservation(
                                            trap
                                    );
                                }
                        );


                    } catch (Exception e) {

                        e.printStackTrace();


                        runOnUiThread(
                                () -> {

                                    scanTrapButton.setEnabled(
                                            true
                                    );


                                    scanTrapButton.setText(
                                            "SCAN TRAP QR     →"
                                    );


                                    lastMonitoringText.setText(
                                            "Unable to identify trap"
                                    );


                                    Toast.makeText(
                                            this,
                                            "Unable to identify trap.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                        );
                    }
                }
        );
    }


    /*
     * ================================================================
     * TRAP NOT FOUND
     * ================================================================
     */

    private void showTrapNotFound() {

        lastMonitoringText.setText(
                "Trap not recognised"
        );


        Toast.makeText(
                this,
                "Trap not found in offline master data.",
                Toast.LENGTH_LONG
        ).show();
    }


    /*
     * ================================================================
     * OPEN TRAP OBSERVATION
     * ================================================================
     *
     * The actual observation screen will be built next.
     *
     * For now we display the identified trap information.
     *
     * ================================================================
     */

    private void openTrapObservation(
            FcmTrapEntity trap
    ) {

        if (trap == null) {

            return;
        }


        Intent intent =
                new Intent(
                        TrapMonitoringActivity.this,
                        TrapObservationActivity.class
                );


        intent.putExtra(
                "trap_id",
                trap.id
        );


        startActivity(
                intent
        );
    }
    /*
     * ================================================================
     * ON DESTROY
     * ================================================================
     */

    @Override
    protected void onDestroy() {

        if (trapExecutor != null) {

            trapExecutor.shutdown();
        }


        super.onDestroy();
    }
}