package com.aaagrowers.scoutingapp.dashboard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aaagrowers.scoutingapp.R;
import com.aaagrowers.scoutingapp.auth.LoginActivity;
import com.aaagrowers.scoutingapp.auth.SessionManager;
import com.aaagrowers.scoutingapp.data.database.AppDatabase;
import com.aaagrowers.scoutingapp.data.model.MasterDataSyncResult;
import com.aaagrowers.scoutingapp.data.sync.MasterDataSyncManager;
import com.aaagrowers.scoutingapp.data.sync.ScoutingSyncRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.aaagrowers.scoutingapp.data.sync.TrapObservationSyncRepository;




public class DashboardActivity extends AppCompatActivity {

    /*
     * ================================================================
     * LOGGING
     * ================================================================
     */

    private static final String TAG =
            "MASTER_SYNC";


    /*
     * ================================================================
     * SESSION
     * ================================================================
     */

    private SessionManager sessionManager;


    /*
     * ================================================================
     * MASTER DATA SYNC
     * ================================================================
     */

    private MasterDataSyncManager masterDataSyncManager;

    private SharedPreferences preferences;


    private static final String PREFS_NAME =
            "scouting_app_preferences";


    private static final String KEY_MASTER_DATA_SYNCED =
            "master_data_synced";


    private static final String KEY_MASTER_DATA_LAST_SYNC =
            "master_data_last_sync";


    /*
     * ================================================================
     * VIEWS
     * ================================================================
     */

    private TextView welcomeName;

    private TextView roleText;

    private TextView usernameText;

    private TextView masterDataStatus;

    private TextView masterDataLastSync;

    private Button syncMasterDataButton;

    private Button syncToServerButton;

    private Button startScoutingButton;

    private Button trapMonitoringButton;

    private Button syncTrapObservationsButton;

    private Button logoutButton;

    private AppDatabase database;
    private ScoutingSyncRepository syncRepository;

    private TrapObservationSyncRepository trapObservationSyncRepository;

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
                R.layout.activity_dashboard
        );

        database = AppDatabase.getInstance(this);

        syncRepository = new ScoutingSyncRepository(
                database.scoutingRoundDao(),
                database.scoutingObservationDao(),
                database.scoutingSeverePointDao(),
                database.scoutingGpsTrackDao()
        );

        trapObservationSyncRepository =
                new TrapObservationSyncRepository(
                        database.trapObservationDao()
                );

        syncToServerButton =
                findViewById(
                        R.id.syncToServerButton
                );

        syncToServerButton.setOnClickListener(
                view -> syncToServer()
        );

        /*
         * ------------------------------------------------------------
         * SESSION MANAGER
         * ------------------------------------------------------------
         */

        sessionManager =
                new SessionManager(
                        this
                );


        /*
         * ------------------------------------------------------------
         * CHECK LOGIN
         * ------------------------------------------------------------
         */

        if (
                !sessionManager.isLoggedIn()
        ) {

            openLogin();

            return;
        }


        /*
         * ------------------------------------------------------------
         * PREFERENCES
         * ------------------------------------------------------------
         */

        preferences =
                getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                );


        /*
         * ------------------------------------------------------------
         * MASTER DATA SYNC MANAGER
         * ------------------------------------------------------------
         */

        masterDataSyncManager =
                new MasterDataSyncManager(
                        this
                );


        /*
         * ------------------------------------------------------------
         * FIND VIEWS
         * ------------------------------------------------------------
         */

        welcomeName =
                findViewById(
                        R.id.welcomeName
                );


        roleText =
                findViewById(
                        R.id.roleText
                );


        usernameText =
                findViewById(
                        R.id.usernameText
                );


        masterDataStatus =
                findViewById(
                        R.id.masterDataStatus
                );


        masterDataLastSync =
                findViewById(
                        R.id.masterDataLastSync
                );


        syncMasterDataButton =
                findViewById(
                        R.id.syncMasterDataButton
                );


        startScoutingButton =
                findViewById(
                        R.id.startScoutingButton
                );


        logoutButton =
                findViewById(
                        R.id.logoutButton
                );

        trapMonitoringButton =
                findViewById(
                        R.id.trapMonitoringButton1
                );

        syncTrapObservationsButton =
                findViewById(
                        R.id.syncTrapObservationsButton
                );


        /*
         * ------------------------------------------------------------
         * LOAD USER INFORMATION
         * ------------------------------------------------------------
         */

        loadUser();


        /*
         * ------------------------------------------------------------
         * LOAD PREVIOUS SYNC STATE
         * ------------------------------------------------------------
         */

        loadMasterDataState();


        /*
         * ------------------------------------------------------------
         * SYNC BUTTON
         * ------------------------------------------------------------
         */

        syncMasterDataButton.setOnClickListener(
                view -> {

                    Log.d(
                            TAG,
                            "========================================"
                    );

                    Log.d(
                            TAG,
                            "SYNC BUTTON CLICKED"
                    );

                    Log.d(
                            TAG,
                            "========================================"
                    );

                    syncMasterData();
                }
        );


        /*
         * ------------------------------------------------------------
         * START SCOUTING
         * ------------------------------------------------------------
         */

        startScoutingButton.setOnClickListener(
                view -> {

                    Intent intent =
                            new Intent(
                                    DashboardActivity.this,
                                    StartScoutingActivity.class
                            );

                    startActivity(intent);
                }
        );

        /*
         * ------------------------------------------------------------
         * TRAP MONITORING
         * ------------------------------------------------------------
         */

        trapMonitoringButton.setOnClickListener(
                view -> {

                    Intent intent =
                            new Intent(
                                    DashboardActivity.this,
                                    TrapMonitoringActivity.class
                            );

                    startActivity(intent);
                }
        );

        syncTrapObservationsButton.setOnClickListener(
                view -> syncTrapObservations()
        );


        /*
         * ------------------------------------------------------------
         * LOGOUT
         * ------------------------------------------------------------
         */

        logoutButton.setOnClickListener(
                view -> logout()
        );
    }

    private void syncTrapObservations() {

        syncTrapObservationsButton.setEnabled(false);

        syncTrapObservationsButton.setText(
                "SYNCING TRAP OBSERVATIONS..."
        );


        new Thread(() -> {

            try {

                /*
                 * --------------------------------------------------------
                 * SYNC TRAP OBSERVATIONS
                 * --------------------------------------------------------
                 */

                trapObservationSyncRepository.syncAll();


                /*
                 * --------------------------------------------------------
                 * SUCCESS
                 * --------------------------------------------------------
                 */

                runOnUiThread(() -> {

                    syncTrapObservationsButton.setEnabled(
                            true
                    );

                    syncTrapObservationsButton.setText(
                            "SYNC TRAP OBSERVATIONS     ↻"
                    );


                    Toast.makeText(
                            DashboardActivity.this,
                            "Trap observations sent successfully.",
                            Toast.LENGTH_LONG
                    ).show();
                });


            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Trap observation synchronization failed",
                        e
                );


                /*
                 * --------------------------------------------------------
                 * FAILURE
                 * --------------------------------------------------------
                 */

                runOnUiThread(() -> {

                    syncTrapObservationsButton.setEnabled(
                            true
                    );

                    syncTrapObservationsButton.setText(
                            "SYNC TRAP OBSERVATIONS     ↻"
                    );


                    Toast.makeText(
                            DashboardActivity.this,
                            "Trap sync failed: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }

        }).start();
    }


    /*
     * ================================================================
     * LOAD USER
     * ================================================================
     */

    private void loadUser() {

        String fullName =
                sessionManager.getFullName();


        String username =
                sessionManager.getUsername();


        String role =
                sessionManager.getRole();


        /*
         * ------------------------------------------------------------
         * FULL NAME
         * ------------------------------------------------------------
         */

        if (
                fullName == null
                        ||
                        fullName.trim().isEmpty()
        ) {

            welcomeName.setText(
                    "Welcome back"
            );

        } else {

            welcomeName.setText(
                    fullName
            );
        }


        /*
         * ------------------------------------------------------------
         * USERNAME
         * ------------------------------------------------------------
         */

        if (
                username != null
                        &&
                        !username.trim().isEmpty()
        ) {

            usernameText.setText(
                    "@" + username
            );

        } else {

            usernameText.setText(
                    "@scout"
            );
        }


        /*
         * ------------------------------------------------------------
         * ROLE
         * ------------------------------------------------------------
         */

        roleText.setText(
                formatRole(
                        role
                )
        );
    }



    private void syncToServer() {

        syncToServerButton.setEnabled(false);
        syncToServerButton.setText("SENDING...");

        new Thread(() -> {

            try {

                syncRepository.syncAll();

                runOnUiThread(() -> {

                    syncToServerButton.setEnabled(true);
                    syncToServerButton.setText("SEND TO SERVER");

                    Toast.makeText(
                            DashboardActivity.this,
                            "Scouting data sent successfully",
                            Toast.LENGTH_LONG
                    ).show();
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    syncToServerButton.setEnabled(true);
                    syncToServerButton.setText("SEND TO SERVER");

                    Toast.makeText(
                            DashboardActivity.this,
                            "Sync failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }

        }).start();
    }


    /*
     * ================================================================
     * LOAD MASTER DATA STATE
     * ================================================================
     */

    private void loadMasterDataState() {

        boolean synced =
                preferences.getBoolean(
                        KEY_MASTER_DATA_SYNCED,
                        false
                );


        long lastSync =
                preferences.getLong(
                        KEY_MASTER_DATA_LAST_SYNC,
                        0
                );


        /*
         * ------------------------------------------------------------
         * PREVIOUS SYNC EXISTS
         * ------------------------------------------------------------
         */

        if (synced) {

            if (lastSync > 0) {

                masterDataStatus.setText(
                        "✓ MASTER DATA READY"
                );


                masterDataLastSync.setText(
                        "Last synchronized "
                                + formatSyncTime(
                                lastSync
                        )
                                + ".\n"
                                + "Master data is available offline."
                );

            } else {

                showMasterDataReady();
            }


            /*
             * Enable scouting because we already
             * have a successful sync.
             */

            startScoutingButton.setEnabled(
                    true
            );


            startScoutingButton.setAlpha(
                    1.0f
            );

            trapMonitoringButton.setEnabled(
                    true
            );

            trapMonitoringButton.setAlpha(
                    1.0f
            );


        } else {

            showMasterDataNotReady();
        }
    }


    /*
     * ================================================================
     * SYNCHRONIZE MASTER DATA
     * ================================================================
     */

    private void syncMasterData() {

        /*
         * ------------------------------------------------------------
         * PREVENT DOUBLE CLICK
         * ------------------------------------------------------------
         */

        syncMasterDataButton.setEnabled(
                false
        );


        syncMasterDataButton.setText(
                "SYNCING..."
        );


        /*
         * ------------------------------------------------------------
         * SHOW CURRENT STATUS
         * ------------------------------------------------------------
         */

        masterDataStatus.setText(
                "SYNCHRONIZING MASTER DATA..."
        );


        masterDataLastSync.setText(
                "Downloading field configuration..."
        );


        /*
         * ------------------------------------------------------------
         * DISABLE SCOUTING WHILE SYNCING
         * ------------------------------------------------------------
         */

        startScoutingButton.setEnabled(
                false
        );


        startScoutingButton.setAlpha(
                0.45f
        );

        trapMonitoringButton.setEnabled(
                false
        );

        trapMonitoringButton.setAlpha(
                0.45f
        );


        /*
         * ------------------------------------------------------------
         * START SYNCHRONIZATION
         * ------------------------------------------------------------
         */

        Log.d(
                TAG,
                "Starting MasterDataSyncManager..."
        );


        masterDataSyncManager.sync(
                result -> {

                    /*
                     * ==================================================
                     * RESULT NULL
                     * ==================================================
                     */

                    if (result == null) {

                        Log.e(
                                TAG,
                                "SYNC RESULT IS NULL"
                        );


                        showSyncFailure(
                                "No synchronization result was received."
                        );


                        return;
                    }


                    /*
                     * ==================================================
                     * LOG RESULT
                     * ==================================================
                     */

                    Log.d(
                            TAG,
                            "SYNC CALLBACK RECEIVED"
                    );


                    Log.d(
                            TAG,
                            "SUCCESS = "
                                    + result.isSuccess()
                    );


                    Log.d(
                            TAG,
                            "MESSAGE = "
                                    + result.getMessage()
                    );


                    /*
                     * ==================================================
                     * SUCCESS
                     * ==================================================
                     */

                    if (
                            result.isSuccess()
                    ) {

                        Log.d(
                                TAG,
                                "DASHBOARD: MASTER DATA SYNC SUCCESS"
                        );


                        /*
                         * ------------------------------------------------
                         * SAVE SYNC STATE
                         * ------------------------------------------------
                         */

                        long syncTime =
                                System.currentTimeMillis();


                        preferences.edit()
                                .putBoolean(
                                        KEY_MASTER_DATA_SYNCED,
                                        true
                                )
                                .putLong(
                                        KEY_MASTER_DATA_LAST_SYNC,
                                        syncTime
                                )
                                .apply();


                        /*
                         * ------------------------------------------------
                         * DISPLAY COMPLETE SYNC RESULT
                         * ------------------------------------------------
                         */

                        showMasterDataSyncResult(
                                result,
                                syncTime
                        );


                        /*
                         * ------------------------------------------------
                         * ENABLE SCOUTING
                         * ------------------------------------------------
                         */

                        startScoutingButton.setEnabled(
                                true
                        );


                        startScoutingButton.setAlpha(
                                1.0f
                        );


                        /*
                         * ------------------------------------------------
                         * ENABLE TRAP MONITORING
                         * ------------------------------------------------
                         */

                        trapMonitoringButton.setEnabled(
                                true
                        );

                        trapMonitoringButton.setAlpha(
                                1.0f
                        );



                        /*
                         * ------------------------------------------------
                         * ENABLE SYNC AGAIN
                         * ------------------------------------------------
                         */

                        syncMasterDataButton.setEnabled(
                                true
                        );


                        syncMasterDataButton.setText(
                                "SYNC MASTER DATA"
                        );


                    } else {

                        /*
                         * =================================================
                         * FAILURE
                         * =================================================
                         */

                        Log.e(
                                TAG,
                                "DASHBOARD: MASTER DATA SYNC FAILED"
                        );


                        String message =
                                result.getMessage();


                        if (
                                message == null
                                        ||
                                        message.trim().isEmpty()
                        ) {

                            message =
                                    "Unable to synchronize master data.";
                        }


                        showSyncFailure(
                                message
                        );
                    }
                }
        );
    }


    /*
     * ================================================================
     * SHOW SUCCESSFUL SYNC RESULT
     * ================================================================
     */

    private void showMasterDataSyncResult(
            MasterDataSyncResult result,
            long syncTime
    ) {

        /*
         * ------------------------------------------------------------
         * STATUS
         * ------------------------------------------------------------
         */

        masterDataStatus.setText(
                "✓ MASTER DATA READY"
        );


        /*
         * ------------------------------------------------------------
         * BUILD SCOUT-FRIENDLY SUMMARY
         * ------------------------------------------------------------
         */

        StringBuilder summary =
                new StringBuilder();


        summary.append(
                "Master data synchronized successfully.\n\n"
        );


        /*
         * ------------------------------------------------------------
         * FARM
         * ------------------------------------------------------------
         */

        summary.append(
                "Farms: "
        );

        summary.append(
                result.getFarms()
        );


        summary.append(
                "\n"
        );


        /*
         * ------------------------------------------------------------
         * SECTIONS
         * ------------------------------------------------------------
         */

        summary.append(
                "Sections: "
        );

        summary.append(
                result.getSections()
        );


        summary.append(
                "\n"
        );


        /*
         * ------------------------------------------------------------
         * GREENHOUSES
         * ------------------------------------------------------------
         */

        summary.append(
                "Greenhouses: "
        );

        summary.append(
                result.getGreenhouses()
        );


        summary.append(
                "\n"
        );


        /*
         * ------------------------------------------------------------
         * BAYS
         * ------------------------------------------------------------
         */

        summary.append(
                "Bays: "
        );

        summary.append(
                result.getBays()
        );


        summary.append(
                "\n"
        );


        /*
         * ------------------------------------------------------------
         * BEDS
         * ------------------------------------------------------------
         */

        summary.append(
                "Beds: "
        );

        summary.append(
                result.getBeds()
        );


        summary.append(
                "\n"
        );


        /*
         * ------------------------------------------------------------
         * SCANNING POINTS
         * ------------------------------------------------------------
         */

        summary.append(
                "Scanning Points: "
        );

        summary.append(
                result.getScanningPoints()
        );


        summary.append(
                "\n"
        );


        /*
         * ------------------------------------------------------------
         * SCANNING POINT BEDS
         * ------------------------------------------------------------
         */

        summary.append(
                "Scanning Point Beds: "
        );

        summary.append(
                result.getScanningPointBeds()
        );


        summary.append(
                "\n"
        );


        /*
         * ------------------------------------------------------------
         * FCM TRAPS
         * ------------------------------------------------------------
         */

        summary.append(
                "FCM Traps: "
        );

        summary.append(
                result.getFcmTraps()
        );

        summary.append(
                "\n"
        );


        /*
         * ------------------------------------------------------------
         * INSECT TYPES
         * ------------------------------------------------------------
         */

        summary.append(
                "Insect Types: "
        );

        summary.append(
                result.getInsectTypes()
        );

        summary.append(
                "\n"
        );




        /*
         * ------------------------------------------------------------
         * OBSERVATION TYPES
         * ------------------------------------------------------------
         */

        summary.append(
                "Observation Types: "
        );

        summary.append(
                result.getObservationTypes()
        );


        summary.append(
                "\n"
        );


        /*
         * ------------------------------------------------------------
         * SCORE DEFINITIONS
         * ------------------------------------------------------------
         */

        summary.append(
                "Score Definitions: "
        );

        summary.append(
                result.getScoreDefinitions()
        );


        summary.append(
                "\n\n"
        );


        /*
         * ------------------------------------------------------------
         * SYNC TIME
         * ------------------------------------------------------------
         */

        summary.append(
                "Last synchronized: "
        );


        summary.append(
                formatSyncTime(
                        syncTime
                )
        );


        summary.append(
                "\n"
        );


        /*
         * ------------------------------------------------------------
         * OFFLINE MESSAGE
         * ------------------------------------------------------------
         */

        summary.append(
                "✓ Data is available offline."
        );


        /*
         * ------------------------------------------------------------
         * DISPLAY
         * ------------------------------------------------------------
         */

        masterDataLastSync.setText(
                summary.toString()
        );
    }


    /*
     * ================================================================
     * SHOW SYNC FAILURE
     * ================================================================
     */

    private void showSyncFailure(
            String message
    ) {

        /*
         * ------------------------------------------------------------
         * STATUS
         * ------------------------------------------------------------
         */

        masterDataStatus.setText(
                "✕ SYNCHRONIZATION FAILED"
        );


        /*
         * ------------------------------------------------------------
         * MESSAGE
         * ------------------------------------------------------------
         */

        masterDataLastSync.setText(
                message
        );


        /*
         * ------------------------------------------------------------
         * ENABLE SYNC AGAIN
         * ------------------------------------------------------------
         */

        syncMasterDataButton.setEnabled(
                true
        );


        syncMasterDataButton.setText(
                "SYNC MASTER DATA"
        );


        /*
         * ------------------------------------------------------------
         * DO NOT ENABLE SCOUTING
         * ------------------------------------------------------------
         */

        boolean previouslySynced =
                preferences.getBoolean(
                        KEY_MASTER_DATA_SYNCED,
                        false
                );


        if (previouslySynced) {

            /*
             * If the scout had a previous successful sync,
             * retain offline scouting capability.
             */

            startScoutingButton.setEnabled(
                    true
            );


            startScoutingButton.setAlpha(
                    1.0f
            );

        } else {

            startScoutingButton.setEnabled(
                    false
            );


            startScoutingButton.setAlpha(
                    0.45f
            );
        }
    }


    /*
     * ================================================================
     * MASTER DATA READY
     * ================================================================
     */

    private void showMasterDataReady() {

        masterDataStatus.setText(
                "✓ MASTER DATA READY"
        );


        masterDataLastSync.setText(
                "Master data is available offline."
        );


        syncMasterDataButton.setEnabled(
                true
        );


        syncMasterDataButton.setText(
                "SYNC MASTER DATA"
        );


        startScoutingButton.setEnabled(
                true
        );


        startScoutingButton.setAlpha(
                1.0f
        );

        trapMonitoringButton.setEnabled(
                true
        );

        trapMonitoringButton.setAlpha(
                1.0f
        );
    }


    /*
     * ================================================================
     * MASTER DATA NOT READY
     * ================================================================
     */

    private void showMasterDataNotReady() {

        masterDataStatus.setText(
                "MASTER DATA NOT SYNCHRONIZED"
        );


        masterDataLastSync.setText(
                "Sync before starting a scouting round."
        );


        syncMasterDataButton.setEnabled(
                true
        );


        syncMasterDataButton.setText(
                "SYNC MASTER DATA"
        );


        startScoutingButton.setEnabled(
                false
        );


        startScoutingButton.setAlpha(
                0.45f
        );

        trapMonitoringButton.setEnabled(
                false
        );

        trapMonitoringButton.setAlpha(
                0.45f
        );
    }


    /*
     * ================================================================
     * FORMAT SYNC TIME
     * ================================================================
     */

    private String formatSyncTime(
            long timestamp
    ) {

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "dd MMM yyyy, HH:mm:ss",
                        Locale.getDefault()
                );


        return formatter.format(
                new Date(timestamp)
        );
    }


    /*
     * ================================================================
     * FORMAT ROLE
     * ================================================================
     */

    private String formatRole(
            String role
    ) {

        if (
                role == null
                        ||
                        role.trim().isEmpty()
        ) {

            return "User";
        }


        if (
                role.equalsIgnoreCase(
                        "scout"
                )
        ) {

            return "FIELD SCOUT";
        }


        if (
                role.equalsIgnoreCase(
                        "supervisor"
                )
        ) {

            return "SUPERVISOR";
        }


        if (
                role.equalsIgnoreCase(
                        "admin"
                )
        ) {

            return "ADMINISTRATOR";
        }


        return role.toUpperCase();
    }


    /*
     * ================================================================
     * LOGOUT
     * ================================================================
     */

    private void logout() {

        Log.d(
                TAG,
                "USER LOGGING OUT"
        );


        sessionManager.logout();


        if (
                masterDataSyncManager != null
        ) {

            masterDataSyncManager.shutdown();

            masterDataSyncManager =
                    null;
        }


        openLogin();
    }


    /*
     * ================================================================
     * OPEN LOGIN
     * ================================================================
     */

    private void openLogin() {

        Intent intent =
                new Intent(
                        DashboardActivity.this,
                        LoginActivity.class
                );


        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );


        startActivity(
                intent
        );


        finish();
    }


    /*
     * ================================================================
     * ON DESTROY
     * ================================================================
     */

    @Override
    protected void onDestroy() {

        if (
                masterDataSyncManager != null
        ) {

            masterDataSyncManager.shutdown();

            masterDataSyncManager =
                    null;
        }


        super.onDestroy();
    }
}