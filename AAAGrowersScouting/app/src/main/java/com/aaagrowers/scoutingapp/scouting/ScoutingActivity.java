package com.aaagrowers.scoutingapp.scouting;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.aaagrowers.scoutingapp.R;
import com.aaagrowers.scoutingapp.auth.SessionManager;
import com.aaagrowers.scoutingapp.data.database.AppDatabase;
import com.aaagrowers.scoutingapp.data.database.dao.BedDao;
import com.aaagrowers.scoutingapp.data.database.dao.GreenhouseDao;
import com.aaagrowers.scoutingapp.data.database.dao.ObservationTypeDao;
import com.aaagrowers.scoutingapp.data.database.dao.ScanningPointBedDao;
import com.aaagrowers.scoutingapp.data.database.dao.ScanningPointDao;
import com.aaagrowers.scoutingapp.data.database.dao.ScoutingGpsTrackDao;
import com.aaagrowers.scoutingapp.data.database.dao.ScoutingObservationDao;
import com.aaagrowers.scoutingapp.data.database.dao.ScoutingRoundDao;
import com.aaagrowers.scoutingapp.data.database.dao.ScoutingSeverePointDao;

import com.aaagrowers.scoutingapp.data.database.entity.BedEntity;
import com.aaagrowers.scoutingapp.data.database.entity.GreenhouseEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ObservationTypeEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ScanningPointBedEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ScanningPointEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ScoutingGpsTrackEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ScoutingObservationEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ScoutingRoundEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ScoutingSeverePointEntity;

import com.aaagrowers.scoutingapp.data.repository.ScoutingRoundRepository;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;


/**
 * ================================================================
 * AAA GROWERS
 * SCOUTING APP
 *
 * FIELD SCOUTING ACTIVITY
 *
 * WORKFLOW
 *
 * ROUND
 *   ↓
 * GPS START
 *   ↓
 * MAKE OBSERVATIONS
 *   ↓
 * SAVE OBSERVATIONS TO PENDING PACKAGE
 *   ↓
 * SCAN SCANNING POINT QR
 *   ↓
 * LOAD GREENHOUSE + BEDS
 *   ↓
 * ASSIGN POINT TO ALL PENDING OBSERVATIONS
 *   ↓
 * COMMIT PACKAGE TO ROOM
 *   ↓
 * CLEAR PENDING PACKAGE
 *   ↓
 * NEXT LOCATION
 *   ↓
 * COMPLETE ROUND
 *
 * Everything is saved locally first.
 * Synchronisation happens later.
 */
public class ScoutingActivity extends AppCompatActivity {


    /*
     * ================================================================
     * CONSTANTS
     * ================================================================
     */

    private static final int LOCATION_PERMISSION_REQUEST = 2001;

    private static final int CAMERA_REQUEST = 2002;


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

    private AppDatabase database;

    private ScoutingRoundDao scoutingRoundDao;

    private ScoutingObservationDao scoutingObservationDao;

    private ScoutingSeverePointDao scoutingSeverePointDao;

    private ScoutingGpsTrackDao scoutingGpsTrackDao;

    private ScanningPointDao scanningPointDao;

    private ScanningPointBedDao scanningPointBedDao;

    private GreenhouseDao greenhouseDao;

    private BedDao bedDao;

    private ObservationTypeDao observationTypeDao;


    /*
     * ================================================================
     * REPOSITORY
     * ================================================================
     */

    private ScoutingRoundRepository scoutingRoundRepository;


    /*
     * ================================================================
     * EXECUTOR
     * ================================================================
     */

    private ExecutorService databaseExecutor;


    /*
     * ================================================================
     * GPS
     * ================================================================
     */

    private LocationManager locationManager;

    private Location currentLocation;

    private boolean gpsTrackingActive = false;


    /*
     * ================================================================
     * ROUND
     * ================================================================
     */

    private long roundId = 0;

    private ScoutingRoundEntity currentRound;


    /*
     * ================================================================
     * CURRENT COMMITTED LOCATION
     * ================================================================
     */

    private ScanningPointEntity currentScanningPoint;

    private GreenhouseEntity currentGreenhouse;

    private List<BedEntity> currentBeds = new ArrayList<>();


    /*
     * ================================================================
     * PENDING OBSERVATION PACKAGE
     *
     * IMPORTANT:
     *
     * These observations are NOT written to Room yet.
     *
     * They remain here until the scanning point QR is scanned.
     * ================================================================
     */

    private final List<PendingObservation> pendingObservations =
            new ArrayList<>();


    /*
     * ================================================================
     * LAST PENDING OBSERVATION
     *
     * Used when marking a severe point or attaching a photo.
     * ================================================================
     */

    private PendingObservation currentPendingObservation;


    /*
     * ================================================================
     * VIEWS
     * ================================================================
     */

    private TextView roundStatus;

    private TextView gpsStatus;

    private TextView gpsCoordinates;

    private TextView gpsAccuracy;

    private TextView greenhouseContext;

    private TextView scanningPointContext;

    private TextView bedContext;

    private TextView observationSaveStatus;

    private TextView severePointStatus;

    private TextView offlineStatus;

    private TextView localSaveStatusText;

    private TextView observationCount;

    private TextView severePointCount;

    private TextView gpsPointCount;

    private TextView gpsTrailPointCount;

    private TextView pendingObservationCount;

    private Spinner observationTypeSpinner;

    private Spinner scoreSpinner;

    private Spinner trendSpinner;

    private EditText commentInput;

    private Button scanScanningPointButton;

    private Button saveObservationButton;

    private Button markSevereButton;

    private Button photoButton;

    private Button completeRoundButton;

    private Button nextPointButton;

    private Button saveAndNextButton;

    private Button pauseButton;

    private boolean scoutingPaused = false;


    /*
     * ================================================================
     * QR SCANNER
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

                        if (
                                contents == null
                                        ||
                                        contents.trim().isEmpty()
                        ) {

                            Toast.makeText(
                                    ScoutingActivity.this,
                                    "QR scan cancelled.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        handleQrResult(
                                contents
                        );
                    }
            );


    /*
     * ================================================================
     * PENDING OBSERVATION CLASS
     * ================================================================
     *
     * This represents an observation that has been made but has not
     * yet been committed to Room because its scanning point has not
     * yet been identified.
     */

    private static class PendingObservation {

        String clientUuid;

        long roundId;

        int observationTypeId;

        String observationName;

        int score;

        String trend;

        String comment;

        String photoPath;

        String observedAt;

        boolean severe;

        double severeLatitude;

        double severeLongitude;

        float severeAccuracy;


        PendingObservation() {

            clientUuid =
                    UUID.randomUUID().toString();

            severe = false;

            severeLatitude = 0;

            severeLongitude = 0;

            severeAccuracy = 0;
        }
    }


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

        setContentView(
                R.layout.activity_scouting
        );


        /*
         * SESSION
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
         * DATABASE
         */

        database =
                AppDatabase.getInstance(
                        this
                );


        scoutingRoundDao =
                database.scoutingRoundDao();

        scoutingObservationDao =
                database.scoutingObservationDao();

        scoutingSeverePointDao =
                database.scoutingSeverePointDao();

        scoutingGpsTrackDao =
                database.scoutingGpsTrackDao();

        scanningPointDao =
                database.scanningPointDao();

        scanningPointBedDao =
                database.scanningPointBedDao();

        greenhouseDao =
                database.greenhouseDao();

        bedDao =
                database.bedDao();

        observationTypeDao =
                database.observationTypeDao();


        /*
         * REPOSITORY
         */

        scoutingRoundRepository =
                new ScoutingRoundRepository(
                        this
                );


        /*
         * EXECUTOR
         */

        databaseExecutor =
                Executors.newSingleThreadExecutor();


        /*
         * LOCATION
         */

        locationManager =
                (LocationManager)
                        getSystemService(
                                LOCATION_SERVICE
                        );


        /*
         * VIEWS
         */

        bindViews();


        /*
         * SPINNERS
         */

        setupObservationSpinners();


        /*
         * ROUND
         */

        readRoundFromIntent();


        /*
         * BUTTONS
         */

        setupButtons();


        /*
         * GPS
         */

        startGpsTracking();


        /*
         * LOAD ROUND
         */

        loadRoundData();


        /*
         * INITIAL STATUS
         */

        setOfflineStatus(
                "OFFLINE READY"
        );

        setLocalSaveStatus(
                "Make observations. They will be held locally until the QR point is scanned."
        );


        updatePendingPackageUi();




        /*
         * BACK BUTTON
         */

        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(true) {

                            @Override
                            public void handleOnBackPressed() {

                                if (
                                        !pendingObservations.isEmpty()
                                ) {

                                    Toast.makeText(
                                            ScoutingActivity.this,
                                            "You have uncommitted observations. Scan the scanning-point QR before leaving this location.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }


                                Toast.makeText(
                                        ScoutingActivity.this,
                                        "Scouting round is still active. Saved data is safe locally.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }


    /*
     * ================================================================
     * BIND VIEWS
     * ================================================================
     */

    private void bindViews() {

        roundStatus =
                findViewById(
                        R.id.roundStatus
                );

        gpsStatus =
                findViewById(
                        R.id.gpsStatus
                );

        gpsCoordinates =
                findViewById(
                        R.id.gpsCoordinates
                );

        gpsAccuracy =
                findViewById(
                        R.id.gpsAccuracy
                );

        greenhouseContext =
                findViewById(
                        R.id.greenhouseContext
                );

        scanningPointContext =
                findViewById(
                        R.id.scanningPointContext
                );

        bedContext =
                findViewById(
                        R.id.bedContext
                );

        observationSaveStatus =
                findViewById(
                        R.id.observationSaveStatus
                );

        severePointStatus =
                findViewById(
                        R.id.severePointStatus
                );

        offlineStatus =
                findViewById(
                        R.id.offlineStatus
                );

        localSaveStatusText =
                findViewById(
                        R.id.localSaveStatusText
                );

        observationCount =
                findViewById(
                        R.id.observationCount
                );

        severePointCount =
                findViewById(
                        R.id.severePointCount
                );

        gpsPointCount =
                findViewById(
                        R.id.gpsPointCount
                );

        gpsTrailPointCount =
                findViewById(
                        R.id.gpsTrailPointCount
                );

        pendingObservationCount =
                findViewById(
                        R.id.pendingObservationCount
                );

        observationTypeSpinner =
                findViewById(
                        R.id.observationTypeSpinner
                );

        scoreSpinner =
                findViewById(
                        R.id.scoreSpinner
                );

        trendSpinner =
                findViewById(
                        R.id.trendSpinner
                );

        commentInput =
                findViewById(
                        R.id.commentInput
                );

        scanScanningPointButton =
                findViewById(
                        R.id.scanScanningPointButton
                );

        saveObservationButton =
                findViewById(
                        R.id.saveObservationButton
                );

        markSevereButton =
                findViewById(
                        R.id.markSevereButton
                );

        photoButton =
                findViewById(
                        R.id.photoButton
                );

        completeRoundButton =
                findViewById(
                        R.id.completeRoundButton
                );

        nextPointButton =
                findViewById(
                        R.id.nextPointButton
                );

        saveAndNextButton =
                findViewById(
                        R.id.saveAndNextButton
                );

        pauseButton = findViewById(R.id.pauseButton);
    }


    /*
     * ================================================================
     * READ ROUND
     * ================================================================
     */

    private void readRoundFromIntent() {

        Intent intent =
                getIntent();

        if (intent == null) {

            showToast(
                    "Scouting round was not provided."
            );

            finish();

            return;
        }


        roundId =
                intent.getLongExtra(
                        "round_id",
                        0
                );


        if (roundId <= 0) {

            showToast(
                    "Scouting round was not provided."
            );

            finish();
        }
    }


    /*
     * ================================================================
     * LOAD ROUND
     * ================================================================
     */

    private void loadRoundData() {

        if (roundId <= 0) {
            return;
        }


        databaseExecutor.execute(
                () -> {

                    ScoutingRoundEntity round =
                            scoutingRoundDao.getById(
                                    roundId
                            );


                    runOnUiThread(
                            () -> {

                                if (round == null) {

                                    Toast.makeText(
                                            this,
                                            "Scouting round not found.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    finish();

                                    return;
                                }


                                currentRound =
                                        round;


                                updateRoundHeader(
                                        round
                                );


                                updateStatisticsUi(
                                        round.id
                                );
                            }
                    );
                }
        );
    }


    /*
     * ================================================================
     * ROUND HEADER
     * ================================================================
     */

    private void updateRoundHeader(
            ScoutingRoundEntity round
    ) {

        if (roundStatus == null) {
            return;
        }


        if (
                "open".equalsIgnoreCase(
                        round.status
                )
        ) {

            roundStatus.setText(
                    "● ROUND ACTIVE"
            );

        } else {

            roundStatus.setText(
                    round.status != null
                            ? round.status.toUpperCase(
                            Locale.getDefault()
                    )
                            : "ROUND"
            );
        }
    }


    /*
     * ================================================================
     * SPINNERS
     * ================================================================
     */

    private void setupObservationSpinners() {

        /*
         * SCORE
         */

        List<String> scores =
                new ArrayList<>();

        scores.add(
                "Select score"
        );

        scores.add("0");
        scores.add("1");
        scores.add("2");
        scores.add("3");
        scores.add("4");
        scores.add("5");


        ArrayAdapter<String> scoreAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        scores
                );


        scoreAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );


        scoreSpinner.setAdapter(
                scoreAdapter
        );


        /*
         * TREND
         */

        List<String> trends =
                new ArrayList<>();

        trends.add(
                "Select trend"
        );

        trends.add(
                "Improving"
        );

        trends.add(
                "Stable"
        );

        trends.add(
                "Worsening"
        );


        ArrayAdapter<String> trendAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        trends
                );


        trendAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );


        trendSpinner.setAdapter(
                trendAdapter
        );


        /*
         * OBSERVATION TYPES
         */

        databaseExecutor.execute(
                () -> {

                    List<ObservationTypeEntity> types =
                            observationTypeDao.getAll();


                    List<String> names =
                            new ArrayList<>();


                    names.add(
                            "Select observation"
                    );


                    if (types != null) {

                        for (
                                ObservationTypeEntity type :
                                types
                        ) {

                            if (type.active == 1) {

                                String name =
                                        type.observation_name;


                                if (
                                        TextUtils.isEmpty(
                                                name
                                        )
                                ) {

                                    name =
                                            type.observation_code;
                                }


                                if (
                                        !TextUtils.isEmpty(
                                                name
                                        )
                                ) {

                                    names.add(
                                            name
                                    );
                                }
                            }
                        }
                    }


                    runOnUiThread(
                            () -> {

                                ArrayAdapter<String>
                                        adapter =
                                        new ArrayAdapter<>(
                                                this,
                                                android.R.layout.simple_spinner_item,
                                                names
                                        );


                                adapter.setDropDownViewResource(
                                        android.R.layout.simple_spinner_dropdown_item
                                );


                                observationTypeSpinner
                                        .setAdapter(
                                                adapter
                                        );
                            }
                    );
                }
        );
    }


    /*
     * ================================================================
     * BUTTONS
     * ================================================================
     */

    private void setupButtons() {

        /*
         * MAIN SAVE
         *
         * Saves observation into pending package.
         */

        saveObservationButton.setOnClickListener(
                view ->
                        saveObservationToPendingPackage()
        );


        /*
         * SAVE + NEXT
         *
         * Same concept, but explicitly prepares the scout
         * for the next observation.
         */

        saveAndNextButton.setOnClickListener(
                view ->
                        saveObservationToPendingPackage()
        );


        /*
         * SEVERE
         */

        markSevereButton.setOnClickListener(
                view ->
                        markCurrentObservationSevere()
        );


        /*
         * PHOTO
         */

        photoButton.setOnClickListener(
                view ->
                        capturePhoto()
        );


        /*
         * QR
         */

        scanScanningPointButton.setOnClickListener(
                view ->
                        startScanningPointScanner()
        );


        /*
         * NEXT POINT
         */

        nextPointButton.setOnClickListener(
                view ->
                        startScanningPointScanner()
        );


        /*
         * COMPLETE
         */

        completeRoundButton.setOnClickListener(
                view ->
                        completeRound()
        );

        pauseButton.setOnClickListener(view -> {

            if (scoutingPaused) {
                resumeScouting();
            } else {
                pauseScouting();
            }

        });
    }


    /*
     * ================================================================
     * SAVE OBSERVATION TO PENDING PACKAGE
     * ================================================================
     */

    /*
     * ================================================================
     * SAVE OBSERVATION TO PENDING PACKAGE
     * ================================================================
     *
     * IMPORTANT:
     *
     * The observation is NOT added to pendingObservations immediately.
     *
     * First:
     *
     *      Validate
     *          ↓
     *      Show confirmation dialog
     *          ↓
     *      CANCEL → discard
     *          ↓
     *      SAVE → add to pending package
     *
     * ================================================================
     */

    private void saveObservationToPendingPackage() {

        if (currentRound == null) {

            showToast(
                    "No active scouting round."
            );

            return;
        }


        /*
         * ------------------------------------------------------------
         * OBSERVATION TYPE
         * ------------------------------------------------------------
         */

        int observationPosition =
                observationTypeSpinner
                        .getSelectedItemPosition();


        if (observationPosition <= 0) {

            showToast(
                    "Select an observation type."
            );

            return;
        }


        /*
         * ------------------------------------------------------------
         * SCORE
         * ------------------------------------------------------------
         */

        int scorePosition =
                scoreSpinner
                        .getSelectedItemPosition();


        if (scorePosition <= 0) {

            showToast(
                    "Select an observation score."
            );

            return;
        }


        int score =
                scorePosition - 1;


        /*
         * ------------------------------------------------------------
         * TREND
         * ------------------------------------------------------------
         */

        String trend =
                getSelectedTrend();


        if (TextUtils.isEmpty(trend)) {

            showToast(
                    "Select an observation trend."
            );

            return;
        }


        /*
         * ------------------------------------------------------------
         * COMMENT
         * ------------------------------------------------------------
         */

        String comment =
                commentInput
                        .getText()
                        .toString()
                        .trim();


        /*
         * ------------------------------------------------------------
         * LOAD OBSERVATION TYPE
         * ------------------------------------------------------------
         */

        databaseExecutor.execute(
                () -> {

                    try {

                        List<ObservationTypeEntity> types =
                                observationTypeDao.getAll();


                        List<ObservationTypeEntity>
                                activeTypes =
                                new ArrayList<>();


                        if (types != null) {

                            for (
                                    ObservationTypeEntity type :
                                    types
                            ) {

                                if (type.active == 1) {

                                    activeTypes.add(
                                            type
                                    );
                                }
                            }
                        }


                        if (
                                observationPosition >
                                        activeTypes.size()
                        ) {

                            showToast(
                                    "Invalid observation type."
                            );

                            return;
                        }


                        ObservationTypeEntity type =
                                activeTypes.get(
                                        observationPosition - 1
                                );


                        /*
                         * ------------------------------------------------
                         * PREPARE VALUES
                         * ------------------------------------------------
                         */

                        final String observationName =
                                !TextUtils.isEmpty(
                                        type.observation_name
                                )
                                        ? type.observation_name
                                        : type.observation_code;


                        final int observationTypeId =
                                type.id;


                        final int finalScore =
                                score;


                        final String finalTrend =
                                trend;


                        final String finalComment =
                                TextUtils.isEmpty(
                                        comment
                                )
                                        ? null
                                        : comment;


                        /*
                         * ------------------------------------------------
                         * SHOW CONFIRMATION
                         * ------------------------------------------------
                         */

                        runOnUiThread(
                                () -> showObservationConfirmation(
                                        observationTypeId,
                                        observationName,
                                        finalScore,
                                        finalTrend,
                                        finalComment
                                )
                        );


                    } catch (Exception e) {

                        e.printStackTrace();

                        showToast(
                                "Unable to prepare observation."
                        );
                    }
                }
        );
    }


    /*
     * ================================================================
     * OBSERVATION CONFIRMATION DIALOG
     * ================================================================
     */

    private void showObservationConfirmation(
            int observationTypeId,
            String observationName,
            int score,
            String trend,
            String comment
    ) {

        /*
         * ------------------------------------------------------------
         * MAIN CONTAINER
         * ------------------------------------------------------------
         */

        LinearLayout container =
                new LinearLayout(this);

        container.setOrientation(
                LinearLayout.VERTICAL
        );

        container.setPadding(
                48,
                8,
                48,
                8
        );


        /*
         * ------------------------------------------------------------
         * INTRODUCTION
         * ------------------------------------------------------------
         */

        TextView intro =
                new TextView(this);

        intro.setText(
                "Please confirm the observation before adding it to the package."
        );

        intro.setTextSize(
                15
        );

        intro.setTextColor(
                Color.rgb(
                        82,
                        105,
                        90
                )
        );

        intro.setPadding(
                0,
                0,
                0,
                22
        );


        container.addView(
                intro
        );


        /*
         * ------------------------------------------------------------
         * OBSERVATION
         * ------------------------------------------------------------
         */

        addObservationConfirmationRow(
                container,
                "Observation",
                observationName
        );


        /*
         * ------------------------------------------------------------
         * SCORE
         * ------------------------------------------------------------
         */

        addObservationConfirmationRow(
                container,
                "Score",
                String.valueOf(
                        score
                )
        );


        /*
         * ------------------------------------------------------------
         * TREND
         * ------------------------------------------------------------
         */

        addObservationConfirmationRow(
                container,
                "Trend",
                trend
        );


        /*
         * ------------------------------------------------------------
         * GPS
         * ------------------------------------------------------------
         */

        String gpsText;

        if (currentLocation != null) {

            gpsText =
                    String.format(
                            Locale.US,
                            "%.6f, %.6f",
                            currentLocation.getLatitude(),
                            currentLocation.getLongitude()
                    );

        } else {

            gpsText =
                    "GPS not available";
        }


        addObservationConfirmationRow(
                container,
                "GPS Position",
                gpsText
        );


        /*
         * ------------------------------------------------------------
         * GPS ACCURACY
         * ------------------------------------------------------------
         */

        if (currentLocation != null) {

            String accuracy =
                    String.format(
                            Locale.US,
                            "±%.1f m",
                            currentLocation.getAccuracy()
                    );


            addObservationConfirmationRow(
                    container,
                    "GPS Accuracy",
                    accuracy
            );
        }


        /*
         * ------------------------------------------------------------
         * SCANNING POINT
         * ------------------------------------------------------------
         */

        String pointText =
                currentScanningPoint != null
                        ? getScanningPointDisplayName()
                        : "Not yet scanned";


        addObservationConfirmationRow(
                container,
                "Scanning Point",
                pointText
        );


        /*
         * ------------------------------------------------------------
         * COMMENTS
         * ------------------------------------------------------------
         */

        if (
                !TextUtils.isEmpty(
                        comment
                )
        ) {

            addObservationConfirmationRow(
                    container,
                    "Comments",
                    comment
            );
        }


        /*
         * ------------------------------------------------------------
         * OBSERVED AT
         * ------------------------------------------------------------
         */

        addObservationConfirmationRow(
                container,
                "Observed At",
                getCurrentDateTime()
        );


        /*
         * ------------------------------------------------------------
         * SCROLL VIEW
         * ------------------------------------------------------------
         */

        ScrollView scrollView =
                new ScrollView(this);

        scrollView.setFillViewport(
                true
        );

        scrollView.addView(
                container
        );


        /*
         * ------------------------------------------------------------
         * DIALOG
         * ------------------------------------------------------------
         */

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Confirm Observation"
                        )
                        .setView(
                                scrollView
                        )
                        .setNegativeButton(
                                "CANCEL",
                                null
                        )
                        .setPositiveButton(
                                "SAVE OBSERVATION",
                                null
                        )
                        .create();


        /*
         * ------------------------------------------------------------
         * SHOW
         * ------------------------------------------------------------
         */

        dialog.setOnShowListener(
                ignored -> {

                    Button saveButton =
                            dialog.getButton(
                                    AlertDialog.BUTTON_POSITIVE
                            );


                    Button cancelButton =
                            dialog.getButton(
                                    AlertDialog.BUTTON_NEGATIVE
                            );


                    /*
                     * SAVE BUTTON
                     */

                    saveButton.setTextColor(
                            Color.rgb(
                                    46,
                                    125,
                                    50
                            )
                    );


                    saveButton.setTypeface(
                            null,
                            Typeface.BOLD
                    );


                    /*
                     * CANCEL BUTTON
                     */

                    cancelButton.setTextColor(
                            Color.rgb(
                                    100,
                                    100,
                                    100
                            )
                    );


                    /*
                     * ------------------------------------------------
                     * IMPORTANT
                     * ------------------------------------------------
                     *
                     * Override the normal positive-button behaviour.
                     *
                     * The dialog only closes after the observation
                     * has been added to the pending package.
                     * ------------------------------------------------
                     */

                    saveButton.setOnClickListener(
                            view -> {

                                dialog.dismiss();


                                addObservationToPendingPackage(
                                        observationTypeId,
                                        observationName,
                                        score,
                                        trend,
                                        comment
                                );
                            }
                    );
                }
        );


        dialog.show();
    }

    /*
     * ================================================================
     * ADD OBSERVATION TO PENDING PACKAGE
     * ================================================================
     */

    private void addObservationToPendingPackage(
            int observationTypeId,
            String observationName,
            int score,
            String trend,
            String comment
    ) {

        PendingObservation pending =
                new PendingObservation();


        pending.roundId =
                currentRound.id;


        pending.observationTypeId =
                observationTypeId;


        pending.observationName =
                observationName;


        pending.score =
                score;


        pending.trend =
                trend;


        pending.comment =
                comment;


        pending.observedAt =
                getCurrentDateTime();


        /*
         * ------------------------------------------------------------
         * ADD TO PACKAGE
         * ------------------------------------------------------------
         */

        pendingObservations.add(
                pending
        );


        currentPendingObservation =
                pending;


        /*
         * ------------------------------------------------------------
         * UPDATE UI
         * ------------------------------------------------------------
         */

        observationSaveStatus.setText(
                "✓ OBSERVATION HELD IN PACKAGE"
        );


        setOfflineStatus(
                "PENDING PACKAGE • NOT YET COMMITTED"
        );


        setLocalSaveStatus(
                "Observation held locally • scan QR to commit"
        );


        updatePendingPackageUi();


        /*
         * ------------------------------------------------------------
         * CLEAR FORM
         * ------------------------------------------------------------
         */

        clearObservationForm();


        /*
         * ------------------------------------------------------------
         * CONFIRMATION
         * ------------------------------------------------------------
         */

        Toast.makeText(
                this,
                "Observation added. Continue with another observation or scan the QR when finished.",
                Toast.LENGTH_LONG
        ).show();
    }

    /*
     * ================================================================
     * CONFIRMATION ROW
     * ================================================================
     */

    private void addObservationConfirmationRow(
            LinearLayout container,
            String label,
            String value
    ) {

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.VERTICAL
        );

        row.setPadding(
                0,
                7,
                0,
                13
        );


        /*
         * LABEL
         */

        TextView labelView =
                new TextView(this);

        labelView.setText(
                label.toUpperCase(
                        Locale.US
                )
        );

        labelView.setTextSize(
                11
        );

        labelView.setTypeface(
                null,
                Typeface.BOLD
        );

        labelView.setTextColor(
                Color.rgb(
                        120,
                        120,
                        120
                )
        );


        /*
         * VALUE
         */

        TextView valueView =
                new TextView(this);

        valueView.setText(
                TextUtils.isEmpty(
                        value
                )
                        ? "-"
                        : value
        );

        valueView.setTextSize(
                16
        );

        valueView.setTypeface(
                null,
                Typeface.BOLD
        );

        valueView.setTextColor(
                Color.rgb(
                        45,
                        55,
                        50
                )
        );

        valueView.setPadding(
                0,
                3,
                0,
                0
        );


        row.addView(
                labelView
        );

        row.addView(
                valueView
        );


        container.addView(
                row
        );
    }

    /*
     * ================================================================
     * SCANNING POINT DISPLAY NAME
     * ================================================================
     */

    private String getScanningPointDisplayName() {

        if (currentScanningPoint == null) {

            return "Not yet scanned";
        }


        String name =
                currentScanningPoint.point_name;


        if (
                TextUtils.isEmpty(
                        name
                )
        ) {

            name =
                    currentScanningPoint.point_code;
        }


        if (
                TextUtils.isEmpty(
                        name
                )
        ) {

            name =
                    "Scanning Point #" +
                            currentScanningPoint.id;
        }


        return name;
    }


    /*
     * ================================================================
     * CLEAR OBSERVATION FORM
     * ================================================================
     */

    private void clearObservationForm() {

        observationTypeSpinner.setSelection(
                0
        );

        scoreSpinner.setSelection(
                0
        );

        trendSpinner.setSelection(
                0
        );

        commentInput.setText("");

        currentPendingObservation =
                null;

        severePointStatus.setText(
                "No severe point marked"
        );
    }


    /*
     * ================================================================
     * MARK CURRENT OBSERVATION SEVERE
     * ================================================================
     */

    private void markCurrentObservationSevere() {

        if (scoutingPaused) {
            showToast("Scouting is paused. Resume scouting first.");
            return;
        }

        if (
                pendingObservations.isEmpty()
        ) {

            showToast(
                    "Save the observation first, then mark it as severe."
            );

            return;
        }


        PendingObservation pending =
                pendingObservations.get(
                        pendingObservations.size() - 1
                );


        if (pending.severe) {

            showToast(
                    "This observation is already marked severe."
            );

            return;
        }


        if (currentLocation == null) {

            showToast(
                    "Waiting for GPS position."
            );

            return;
        }


        pending.severe =
                true;


        pending.severeLatitude =
                currentLocation.getLatitude();


        pending.severeLongitude =
                currentLocation.getLongitude();


        pending.severeAccuracy =
                currentLocation.getAccuracy();


        currentPendingObservation =
                pending;


        severePointStatus.setText(
                "🔴 SEVERE POINT HELD WITH CURRENT OBSERVATION"
        );


        setOfflineStatus(
                "SEVERE POINT HELD • WILL COMMIT WITH PACKAGE"
        );


        setLocalSaveStatus(
                "Severe point linked to pending observation"
        );


        Toast.makeText(
                this,
                "Severe point marked. It will be saved with this observation when the QR is scanned.",
                Toast.LENGTH_LONG
        ).show();
    }


    /*
     * ================================================================
     * PHOTO
     * ================================================================
     */

    private void capturePhoto() {

        if (scoutingPaused) {
            showToast("Scouting is paused. Resume scouting first.");
            return;
        }

        if (pendingObservations.isEmpty()) {

            showToast(
                    "Save an observation first before attaching a photo."
            );

            return;
        }


        currentPendingObservation =
                pendingObservations.get(
                        pendingObservations.size() - 1
                );


        Intent intent =
                new Intent(
                        MediaStore.ACTION_IMAGE_CAPTURE
                );


        if (
                intent.resolveActivity(
                        getPackageManager()
                ) != null
        ) {

            startActivityForResult(
                    intent,
                    CAMERA_REQUEST
            );

        } else {

            Toast.makeText(
                    this,
                    "Camera is not available.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    /*
     * ================================================================
     * PHOTO RESULT
     * ================================================================
     */

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );


        if (
                requestCode != CAMERA_REQUEST
                        ||
                        resultCode != RESULT_OK
                        ||
                        data == null
        ) {

            return;
        }


        Bitmap bitmap =
                (Bitmap)
                        data.getExtras()
                                .get("data");


        if (bitmap == null) {

            showToast(
                    "Unable to capture photo."
            );

            return;
        }


        if (
                currentPendingObservation == null
        ) {

            showToast(
                    "No pending observation to attach the photo to."
            );

            return;
        }


        databaseExecutor.execute(
                () -> {

                    try {

                        File directory =
                                new File(
                                        getFilesDir(),
                                        "scouting_photos"
                                );


                        if (!directory.exists()) {

                            directory.mkdirs();
                        }


                        String fileName =
                                "scout_" +
                                        UUID.randomUUID() +
                                        ".jpg";


                        File photoFile =
                                new File(
                                        directory,
                                        fileName
                                );


                        FileOutputStream output =
                                new FileOutputStream(
                                        photoFile
                                );


                        bitmap.compress(
                                Bitmap.CompressFormat.JPEG,
                                90,
                                output
                        );


                        output.flush();

                        output.close();


                        currentPendingObservation
                                .photoPath =
                                photoFile.getAbsolutePath();


                        runOnUiThread(
                                () -> {

                                    setOfflineStatus(
                                            "PHOTO ATTACHED TO PENDING OBSERVATION"
                                    );


                                    setLocalSaveStatus(
                                            "Photo held locally • will commit with package"
                                    );


                                    Toast.makeText(
                                            this,
                                            "Photo attached to current observation.",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                        );


                    } catch (Exception e) {

                        e.printStackTrace();

                        showToast(
                                "Unable to save photo locally."
                        );
                    }
                }
        );
    }


    /*
     * ================================================================
     * GET TREND
     * ================================================================
     */

    private String getSelectedTrend() {

        if (
                trendSpinner == null
                        ||
                        trendSpinner.getSelectedItem() == null
        ) {

            return null;
        }


        String trend =
                trendSpinner
                        .getSelectedItem()
                        .toString();


        if (
                TextUtils.isEmpty(
                        trend
                )
                        ||
                        "Select trend".equalsIgnoreCase(
                                trend
                        )
        ) {

            return null;
        }


        return trend;
    }


    /*
     * ================================================================
     * PENDING PACKAGE UI
     * ================================================================
     */

    private void updatePendingPackageUi() {

        int count = pendingObservations.size();

        if (pendingObservationCount != null) {

            pendingObservationCount.setText(
                    String.valueOf(count)
            );
        }


        /*
         * If scouting is paused, keep the package controls disabled.
         */
        if (scoutingPaused) {

            scanScanningPointButton.setText(
                    count > 0
                            ? "SCOUTING PAUSED"
                            : "SCANNING PAUSED"
            );

            scanScanningPointButton.setEnabled(false);

            nextPointButton.setEnabled(false);

            return;
        }


        /*
         * Normal active scouting state.
         */
        if (count == 0) {

            scanScanningPointButton.setText(
                    "SCAN SCANNING POINT"
            );

            scanScanningPointButton.setEnabled(false);

            nextPointButton.setEnabled(false);

        } else {

            scanScanningPointButton.setText(
                    "SCAN QR • COMMIT "
                            + count
                            + " OBSERVATION"
                            + (count == 1 ? "" : "S")
            );

            scanScanningPointButton.setEnabled(true);

            nextPointButton.setEnabled(true);
        }
    }


    /*
     * ================================================================
     * START QR SCANNER
     * ================================================================
     */

    private void startScanningPointScanner() {

        if (scoutingPaused) {
            showToast("Scouting is paused. Resume scouting first.");
            return;
        }

        if (currentRound == null) {

            showToast(
                    "No active scouting round."
            );

            return;
        }


        if (pendingObservations.isEmpty()) {

            showToast(
                    "Make at least one observation before scanning the location QR."
            );

            return;
        }


        ScanOptions options =
                new ScanOptions();


        options.setPrompt(
                "Scan the QR code on the scanning point"
        );


        options.setBeepEnabled(
                true
        );


        options.setOrientationLocked(
                false
        );


        options.setDesiredBarcodeFormats(
                ScanOptions.QR_CODE
        );


        qrScannerLauncher.launch(
                options
        );
    }

    /*
     * ================================================================
     * HANDLE QR RESULT
     * ================================================================
     */

    private void handleQrResult(
            String contents
    ) {

        if (
                TextUtils.isEmpty(contents)
        ) {

            return;
        }


        final String qrValue =
                contents.trim();


        setOfflineStatus(
                "QR SCANNED • IDENTIFYING LOCATION"
        );


        setLocalSaveStatus(
                "Looking up scanning point..."
        );


        databaseExecutor.execute(
                () -> {

                    try {

                        /*
                         * ------------------------------------------------
                         * DIRECT QR TOKEN LOOKUP
                         * ------------------------------------------------
                         */

                        ScanningPointEntity matchedPoint =
                                scanningPointDao.getByQrToken(
                                        qrValue
                                );


                        /*
                         * ------------------------------------------------
                         * NOT FOUND
                         * ------------------------------------------------
                         */

                        if (matchedPoint == null) {

                            runOnUiThread(
                                    () -> {

                                        setOfflineStatus(
                                                "QR NOT RECOGNISED"
                                        );


                                        setLocalSaveStatus(
                                                "Scanning point was not found in local master data"
                                        );


                                        Toast.makeText(
                                                this,
                                                "This QR code is not a recognised scanning point.",
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                            );

                            return;
                        }


                        /*
                         * ------------------------------------------------
                         * CHECK ACTIVE
                         * ------------------------------------------------
                         */

                        if (matchedPoint.active != 1) {

                            runOnUiThread(
                                    () -> {

                                        setOfflineStatus(
                                                "SCANNING POINT INACTIVE"
                                        );


                                        setLocalSaveStatus(
                                                "This scanning point is currently inactive"
                                        );


                                        Toast.makeText(
                                                this,
                                                "This scanning point is inactive.",
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                            );

                            return;
                        }


                        /*
                         * ------------------------------------------------
                         * LOAD LOCATION + COMMIT
                         * ------------------------------------------------
                         */

                        loadScanningPointAndCommit(
                                matchedPoint
                        );


                    } catch (Exception e) {

                        e.printStackTrace();


                        showToast(
                                "Unable to process scanning point QR."
                        );
                    }
                }
        );
    }
    /*
     * ================================================================
     * FIND SCANNING POINT
     * ================================================================
     *
     * The QR code contains the scanning_point.qr_token.
     *
     * Example QR content:
     *
     * a25f57b15cb3b134849bb2fc9df7e014774d6df4ac298c10bb12de970886fbb3
     *
     * We also retain support for:
     *
     * SCANNING_POINT:<qr_token>
     *
     * ================================================================
     */

    private ScanningPointEntity findScanningPoint(
            List<ScanningPointEntity> points,
            String qrValue
    ) {

        if (
                points == null
                        ||
                        points.isEmpty()
                        ||
                        TextUtils.isEmpty(qrValue)
        ) {

            return null;
        }


        String value =
                qrValue.trim();


        /*
         * ------------------------------------------------
         * SUPPORT OPTIONAL PREFIX
         * ------------------------------------------------
         *
         * This allows the QR to contain either:
         *
         * TOKEN
         *
         * or
         *
         * SCANNING_POINT:TOKEN
         *
         * ------------------------------------------------
         */

        final String prefix =
                "SCANNING_POINT:";


        if (
                value.regionMatches(
                        true,
                        0,
                        prefix,
                        0,
                        prefix.length()
                )
        ) {

            value =
                    value.substring(
                            prefix.length()
                    ).trim();
        }


        /*
         * ------------------------------------------------
         * MATCH QR TOKEN
         * ------------------------------------------------
         */

        for (
                ScanningPointEntity point :
                points
        ) {

            /*
             * Only active scanning points can be used.
             */

            if (point.active != 1) {
                continue;
            }


            /*
             * PRIMARY MATCH
             *
             * This is what the QR scanner should use.
             */

            if (
                    !TextUtils.isEmpty(
                            point.qr_token
                    )
                            &&
                            point.qr_token.equalsIgnoreCase(
                                    value
                            )
            ) {

                return point;
            }
        }


        /*
         * ------------------------------------------------
         * OPTIONAL FALLBACKS
         * ------------------------------------------------
         *
         * These are useful during testing and do not hurt
         * the QR-token workflow.
         * ------------------------------------------------
         */

        for (
                ScanningPointEntity point :
                points
        ) {

            if (point.active != 1) {
                continue;
            }


            /*
             * Point code fallback
             */

            if (
                    !TextUtils.isEmpty(
                            point.point_code
                    )
                            &&
                            point.point_code.equalsIgnoreCase(
                                    value
                            )
            ) {

                return point;
            }


            /*
             * Point name fallback
             */

            if (
                    !TextUtils.isEmpty(
                            point.point_name
                    )
                            &&
                            point.point_name.equalsIgnoreCase(
                                    value
                            )
            ) {

                return point;
            }


            /*
             * Numeric ID fallback
             */

            try {

                int scannedId =
                        Integer.parseInt(
                                value
                        );


                if (
                        point.id ==
                                scannedId
                ) {

                    return point;
                }

            } catch (NumberFormatException ignored) {

                // Not a numeric scanning point ID.
            }
        }


        return null;
    }


    /*
     * ================================================================
     * LOAD POINT + GREENHOUSE + BEDS
     * ================================================================
     */

    private void loadScanningPointAndCommit(
            ScanningPointEntity point
    ) {

        databaseExecutor.execute(
                () -> {

                    try {

                        GreenhouseEntity greenhouse =
                                greenhouseDao.getById(
                                        point.greenhouse_id
                                );


                        List<ScanningPointBedEntity>
                                mappings =
                                scanningPointBedDao
                                        .getByScanningPoint(
                                                point.id
                                        );


                        List<BedEntity> beds =
                                new ArrayList<>();


                        if (mappings != null) {

                            for (
                                    ScanningPointBedEntity mapping :
                                    mappings
                            ) {

                                BedEntity bed =
                                        bedDao.getById(
                                                mapping.bed_id
                                        );


                                if (bed != null) {

                                    beds.add(
                                            bed
                                    );
                                }
                            }
                        }


                        currentScanningPoint =
                                point;


                        currentGreenhouse =
                                greenhouse;


                        currentBeds =
                                beds;


                        runOnUiThread(
                                () -> {

                                    updateScanningPointContext();

                                    commitPendingPackage();
                                }
                        );


                    } catch (Exception e) {

                        e.printStackTrace();

                        showToast(
                                "Unable to load scanning point context."
                        );
                    }
                }
        );
    }


    /*
     * ================================================================
     * UPDATE POINT CONTEXT
     * ================================================================
     */

    private void updateScanningPointContext() {

        if (currentScanningPoint == null) {
            return;
        }


        String pointName =
                currentScanningPoint.point_name;


        if (
                TextUtils.isEmpty(
                        pointName
                )
        ) {

            pointName =
                    currentScanningPoint.point_code;
        }


        if (
                TextUtils.isEmpty(
                        pointName
                )
        ) {

            pointName =
                    "Scanning Point #" +
                            currentScanningPoint.id;
        }


        scanningPointContext.setText(
                pointName
        );


        updateGreenhouseContext();

        updateBedContext();
    }


    /*
     * ================================================================
     * GREENHOUSE
     * ================================================================
     */

    private void updateGreenhouseContext() {

        if (currentGreenhouse == null) {

            greenhouseContext.setText(
                    "Greenhouse unavailable"
            );

            return;
        }


        String name =
                currentGreenhouse.greenhouse_name;


        if (
                TextUtils.isEmpty(
                        name
                )
        ) {

            name =
                    currentGreenhouse.greenhouse_code;
        }


        greenhouseContext.setText(
                "🌱 " +
                        (
                                TextUtils.isEmpty(
                                        name
                                )
                                        ? "Unknown greenhouse"
                                        : name
                        )
        );
    }


    /*
     * ================================================================
     * BEDS
     * ================================================================
     */

    private void updateBedContext() {

        int count =
                currentBeds != null
                        ? currentBeds.size()
                        : 0;


        bedContext.setText(
                count +
                        " mapped bed" +
                        (count == 1 ? "" : "s")
        );
    }


    /*
     * ================================================================
     * COMMIT PENDING PACKAGE
     * ================================================================
     */

    private void commitPendingPackage() {

        if (currentScanningPoint == null) {

            showToast(
                    "Scanning point is not available."
            );

            return;
        }


        if (pendingObservations.isEmpty()) {

            showToast(
                    "There are no pending observations."
            );

            return;
        }


        final long activeRoundId =
                currentRound.id;


        final int scanningPointId =
                currentScanningPoint.id;


        final int bedId =
                currentBeds != null
                        &&
                        !currentBeds.isEmpty()
                        ? currentBeds.get(0).id
                        : 0;


        /*
         * Copy package so the UI list can safely be cleared after
         * the database transaction.
         */

        final List<PendingObservation> packageToCommit =
                new ArrayList<>(
                        pendingObservations
                );


        scanScanningPointButton.setEnabled(
                false
        );


        nextPointButton.setEnabled(
                false
        );


        setOfflineStatus(
                "COMMITTING OBSERVATION PACKAGE"
        );


        setLocalSaveStatus(
                "Assigning scanning point to " +
                        packageToCommit.size() +
                        " observation(s)..."
        );


        databaseExecutor.execute(
                () -> {

                    try {

                        database.runInTransaction(
                                () -> {

                                    for (
                                            PendingObservation pending :
                                            packageToCommit
                                    ) {

                                        /*
                                         * ------------------------------------------------
                                         * OBSERVATION
                                         * ------------------------------------------------
                                         */

                                        ScoutingObservationEntity observation =
                                                new ScoutingObservationEntity();


                                        observation.client_uuid =
                                                pending.clientUuid;


                                        observation.round_id =
                                                activeRoundId;


                                        observation.scanning_point_id =
                                                scanningPointId;


                                        observation.greenhouse_variety_id =
                                                0;


                                        observation.observation_type_id =
                                                pending.observationTypeId;


                                        observation.score =
                                                pending.score;


                                        observation.trend =
                                                pending.trend;


                                        observation.comment =
                                                pending.comment;


                                        observation.photo_path =
                                                pending.photoPath;


                                        observation.observed_at =
                                                pending.observedAt;


                                        observation.created_at =
                                                getCurrentDateTime();


                                        observation.updated_at =
                                                getCurrentDateTime();


                                        scoutingObservationDao.insert(
                                                observation
                                        );


                                        /*
                                         * ------------------------------------------------
                                         * SEVERE POINT
                                         *
                                         * Severe status belongs to this observation.
                                         * ------------------------------------------------
                                         */

                                        if (pending.severe) {

                                            ScoutingSeverePointEntity severePoint =
                                                    new ScoutingSeverePointEntity();


                                            severePoint.client_uuid =
                                                    UUID.randomUUID()
                                                            .toString();


                                            severePoint.round_id =
                                                    activeRoundId;


                                            severePoint.scanning_point_id =
                                                    scanningPointId;


                                            severePoint.bed_id =
                                                    bedId;


                                            severePoint.observation_type_id =
                                                    pending.observationTypeId;


                                            severePoint.score =
                                                    pending.score;


                                            severePoint.trend =
                                                    pending.trend;


                                            severePoint.latitude =
                                                    pending.severeLatitude;


                                            severePoint.longitude =
                                                    pending.severeLongitude;


                                            severePoint.gps_accuracy =
                                                    pending.severeAccuracy;


                                            severePoint.comment =
                                                    pending.comment;


                                            severePoint.photo_path =
                                                    pending.photoPath;


                                            severePoint.observed_at =
                                                    pending.observedAt;


                                            severePoint.created_at =
                                                    getCurrentDateTime();


                                            severePoint.updated_at =
                                                    getCurrentDateTime();


                                            scoutingSeverePointDao.insert(
                                                    severePoint
                                            );
                                        }
                                    }
                                }
                        );


                        runOnUiThread(
                                () -> {

                                    /*
                                     * ------------------------------------------------
                                     * PACKAGE COMMITTED
                                     * ------------------------------------------------
                                     */

                                    pendingObservations.clear();

                                    currentPendingObservation =
                                            null;


                                    observationSaveStatus.setText(
                                            "✓ PACKAGE COMMITTED"
                                    );


                                    severePointStatus.setText(
                                            "No severe point pending"
                                    );


                                    setOfflineStatus(
                                            "PACKAGE SAVED LOCALLY"
                                    );


                                    setLocalSaveStatus(
                                            packageToCommit.size() +
                                                    " observation(s) committed to Room"
                                    );


                                    Toast.makeText(
                                            this,
                                            "Observation package committed successfully.",
                                            Toast.LENGTH_LONG
                                    ).show();


                                    updatePendingPackageUi();


                                    updateStatisticsUi(
                                            activeRoundId
                                    );


                                    /*
                                     * ------------------------------------------------
                                     * RESET LOCATION
                                     *
                                     * We deliberately clear the current point.
                                     * The next observations belong to the next
                                     * physical scouting location.
                                     * ------------------------------------------------
                                     */

                                    currentScanningPoint =
                                            null;

                                    currentGreenhouse =
                                            null;

                                    currentBeds =
                                            new ArrayList<>();


                                    scanningPointContext.setText(
                                            "No scanning point selected"
                                    );


                                    greenhouseContext.setText(
                                            "--"
                                    );


                                    bedContext.setText(
                                            "0 mapped beds"
                                    );


                                    /*
                                     * Reset scanner action.
                                     */

                                    scanScanningPointButton.setText(
                                            "SCAN NEXT SCANNING POINT"
                                    );


                                    setOfflineStatus(
                                            "READY FOR NEXT LOCATION"
                                    );


                                    setLocalSaveStatus(
                                            "Move to next location and make observations"
                                    );
                                }
                        );


                    } catch (Exception e) {

                        e.printStackTrace();


                        runOnUiThread(
                                () -> {

                                    scanScanningPointButton
                                            .setEnabled(
                                                    true
                                            );


                                    nextPointButton
                                            .setEnabled(
                                                    true
                                            );


                                    setOfflineStatus(
                                            "PACKAGE COMMIT FAILED"
                                    );


                                    setLocalSaveStatus(
                                            "Pending observations were not cleared"
                                    );


                                    showToast(
                                            "Unable to commit observation package."
                                    );
                                }
                        );
                    }
                }
        );
    }


    /*
     * ================================================================
     * GPS
     * ================================================================
     */

    private void startGpsTracking() {

        if (locationManager == null) {
            return;
        }


        if (
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
                        != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST
            );

            return;
        }


        gpsTrackingActive =
                true;


        gpsStatus.setText(
                "● GPS ACTIVE"
        );


        try {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000,
                    3,
                    locationListener
            );


            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000,
                    5,
                    locationListener
            );


        } catch (SecurityException e) {

            e.printStackTrace();

            gpsStatus.setText(
                    "GPS PERMISSION REQUIRED"
            );
        }
    }


    /*
     * ================================================================
     * LOCATION LISTENER
     * ================================================================
     */

    private final LocationListener locationListener =
            new LocationListener() {

                @Override
                public void onLocationChanged(
                        @NonNull Location location
                ) {

                    currentLocation =
                            location;


                    updateGpsUi(
                            location
                    );


                    saveGpsTrack(
                            location
                    );
                }


                @Override
                public void onProviderEnabled(
                        @NonNull String provider
                ) {

                    runOnUiThread(
                            () ->
                                    gpsStatus.setText(
                                            "● GPS ACTIVE"
                                    )
                    );
                }


                @Override
                public void onProviderDisabled(
                        @NonNull String provider
                ) {

                    runOnUiThread(
                            () ->
                                    gpsStatus.setText(
                                            "GPS UNAVAILABLE"
                                    )
                    );
                }
            };


    /*
     * ================================================================
     * GPS UI
     * ================================================================
     */

    private void updateGpsUi(
            Location location
    ) {

        runOnUiThread(
                () -> {

                    gpsStatus.setText(
                            "● GPS LOCKED"
                    );


                    gpsCoordinates.setText(
                            String.format(
                                    Locale.getDefault(),
                                    "%.6f, %.6f",
                                    location.getLatitude(),
                                    location.getLongitude()
                            )
                    );


                    gpsAccuracy.setText(
                            String.format(
                                    Locale.getDefault(),
                                    "Accuracy ±%.1f m",
                                    location.getAccuracy()
                            )
                    );
                }
        );
    }


    /*
     * ================================================================
     * GPS TRACK
     * ================================================================
     */

    private void saveGpsTrack(
            Location location
    ) {

        if (currentRound == null) {
            return;
        }


        final long activeRoundId =
                currentRound.id;


        databaseExecutor.execute(
                () -> {

                    try {

                        ScoutingGpsTrackEntity track =
                                new ScoutingGpsTrackEntity();


                        track.client_uuid =
                                UUID.randomUUID()
                                        .toString();


                        track.round_id =
                                activeRoundId;


                        track.latitude =
                                location.getLatitude();


                        track.longitude =
                                location.getLongitude();


                        track.accuracy =
                                location.getAccuracy();


                        track.altitude =
                                location.hasAltitude()
                                        ? location.getAltitude()
                                        : 0;


                        track.speed =
                                location.hasSpeed()
                                        ? location.getSpeed()
                                        : 0;


                        track.bearing =
                                location.hasBearing()
                                        ? location.getBearing()
                                        : 0;


                        track.recorded_at =
                                getCurrentDateTime();


                        track.created_at =
                                getCurrentDateTime();


                        scoutingGpsTrackDao.insert(
                                track
                        );


                        runOnUiThread(
                                () ->
                                        updateStatisticsUi(
                                                activeRoundId
                                        )
                        );


                    } catch (Exception e) {

                        e.printStackTrace();
                    }
                }
        );
    }


    /*
     * ================================================================
     * COMPLETE ROUND
     * ================================================================
     */

    private void completeRound() {

        if (scoutingPaused) {
            showToast("Resume scouting before completing the round.");
            return;
        }

        if (currentRound == null) {

            showToast(
                    "No active scouting round."
            );

            return;
        }


        /*
         * CRITICAL:
         *
         * Never allow a round to finish while there are observations
         * waiting for a scanning-point QR.
         */

        if (!pendingObservations.isEmpty()) {

            Toast.makeText(
                    this,
                    "You have " +
                            pendingObservations.size() +
                            " uncommitted observation(s). Scan the scanning-point QR before completing the round.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        completeRoundButton.setEnabled(
                false
        );


        scoutingRoundRepository.completeRound(
                currentRound.id,
                null,
                (
                        success,
                        message
                ) -> {

                    runOnUiThread(
                            () -> {

                                completeRoundButton
                                        .setEnabled(
                                                true
                                        );


                                if (success) {

                                    stopGpsTracking();


                                    roundStatus.setText(
                                            "✓ ROUND COMPLETED"
                                    );


                                    setOfflineStatus(
                                            "ROUND SAVED LOCALLY"
                                    );


                                    setLocalSaveStatus(
                                            "Round completed • ready for synchronization"
                                    );


                                    Toast.makeText(
                                            this,
                                            message,
                                            Toast.LENGTH_LONG
                                    ).show();


                                    finish();

                                } else {

                                    Toast.makeText(
                                            this,
                                            message,
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }
                    );
                }
        );
    }


    /*
     * ================================================================
     * STATISTICS
     * ================================================================
     */

    private void updateStatisticsUi(
            long activeRoundId
    ) {

        if (databaseExecutor == null) {
            return;
        }


        databaseExecutor.execute(
                () -> {

                    try {

                        int observations =
                                scoutingObservationDao
                                        .countByRound(
                                                activeRoundId
                                        );


                        int severePoints =
                                scoutingSeverePointDao
                                        .countByRound(
                                                activeRoundId
                                        );


                        int gpsPoints =
                                scoutingGpsTrackDao
                                        .countByRound(
                                                activeRoundId
                                        );


                        runOnUiThread(
                                () -> {

                                    observationCount.setText(
                                            String.valueOf(
                                                    observations
                                            )
                                    );


                                    severePointCount.setText(
                                            String.valueOf(
                                                    severePoints
                                            )
                                    );


                                    gpsPointCount.setText(
                                            String.valueOf(
                                                    gpsPoints
                                            )
                                    );


                                    gpsTrailPointCount.setText(
                                            String.valueOf(
                                                    gpsPoints
                                            )
                                    );
                                }
                        );


                    } catch (Exception e) {

                        e.printStackTrace();
                    }
                }
        );
    }


    /*
     * ================================================================
     * STATUS
     * ================================================================
     */

    private void setOfflineStatus(
            String message
    ) {

        if (offlineStatus != null) {

            offlineStatus.setText(
                    message
            );
        }
    }


    private void setLocalSaveStatus(
            String message
    ) {

        if (localSaveStatusText != null) {

            localSaveStatusText.setText(
                    message
            );
        }
    }


    /*
     * ================================================================
     * STOP GPS
     * ================================================================
     */

    private void stopGpsTracking() {

        if (locationManager == null) {
            return;
        }


        if (
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
                        != PackageManager.PERMISSION_GRANTED
        ) {

            return;
        }


        try {

            locationManager.removeUpdates(
                    locationListener
            );

        } catch (Exception e) {

            e.printStackTrace();
        }


        gpsTrackingActive =
                false;


        if (gpsStatus != null) {

            gpsStatus.setText(
                    "GPS TRACKING STOPPED"
            );
        }
    }


    /*
     * ================================================================
     * PERMISSION RESULT
     * ================================================================
     */

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );


        if (
                requestCode ==
                        LOCATION_PERMISSION_REQUEST
        ) {

            if (
                    grantResults.length > 0
                            &&
                            grantResults[0]
                                    ==
                                    PackageManager.PERMISSION_GRANTED
            ) {

                startGpsTracking();

            } else {

                gpsStatus.setText(
                        "GPS PERMISSION REQUIRED"
                );


                Toast.makeText(
                        this,
                        "Location permission is required for field scouting.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }


    /*
     * ================================================================
     * DATE / TIME
     * ================================================================
     */

    private String getCurrentDateTime() {

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                );


        return formatter.format(
                new Date()
        );
    }


    /*
     * ================================================================
     * TOAST
     * ================================================================
     */

    private void showToast(
            String message
    ) {

        runOnUiThread(
                () ->
                        Toast.makeText(
                                ScoutingActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show()
        );
    }


    /*
     * ================================================================
     * DESTROY
     * ================================================================
     */

    @Override
    protected void onDestroy() {

        stopGpsTracking();


        if (scoutingRoundRepository != null) {

            scoutingRoundRepository.shutdown();
        }


        if (databaseExecutor != null) {

            databaseExecutor.shutdown();
        }


        super.onDestroy();
    }

    private void pauseScouting() {

        if (currentRound == null) {
            showToast("No active scouting round.");
            return;
        }

        /*
         * Pending observations currently exist only in memory.
         * Do not allow the scout to pause with unsaved pending observations.
         */
        if (!pendingObservations.isEmpty()) {

            showToast(
                    "Please scan the scanning point QR before pausing."
            );

            return;
        }

        scoutingPaused = true;

        // Stop GPS trail recording
        stopGpsTracking();

        // Update button
        pauseButton.setText("RESUME");

        // Update round status
        roundStatus.setText("● SCOUTING PAUSED");
        roundStatus.setTextColor(
                getColor(android.R.color.holo_orange_dark)
        );

        // Update offline/status information
        setOfflineStatus("SCOUTING PAUSED");

        setLocalSaveStatus(
                "Scouting is paused. GPS tracking and observations are stopped."
        );

        // Disable scouting controls
        setScoutingControlsEnabled(false);

        showToast("Scouting paused.");
    }

    private void resumeScouting() {

        if (currentRound == null) {
            showToast("No active scouting round.");
            return;
        }

        scoutingPaused = false;

        // Resume GPS tracking
        startGpsTracking();

        // Update button
        pauseButton.setText("PAUSE");

        // Update round status
        roundStatus.setText("● ROUND ACTIVE");
        roundStatus.setTextColor(
                getColor(android.R.color.holo_green_dark)
        );

        setOfflineStatus("OFFLINE READY");

        setLocalSaveStatus(
                "Scouting resumed. GPS tracking is active."
        );

        // Enable scouting controls
        setScoutingControlsEnabled(true);

        updatePendingPackageUi();

        showToast("Scouting resumed.");
    }
    private void setScoutingControlsEnabled(boolean enabled) {

        observationTypeSpinner.setEnabled(enabled);
        scoreSpinner.setEnabled(enabled);
        trendSpinner.setEnabled(enabled);

        commentInput.setEnabled(enabled);

        saveObservationButton.setEnabled(enabled);
        saveAndNextButton.setEnabled(enabled);

        markSevereButton.setEnabled(enabled);
        photoButton.setEnabled(enabled);

        scanScanningPointButton.setEnabled(
                enabled && !pendingObservations.isEmpty()
        );

        nextPointButton.setEnabled(
                enabled && !pendingObservations.isEmpty()
        );

        completeRoundButton.setEnabled(enabled);

        /*
         * Pause/resume must always remain available.
         */
        pauseButton.setEnabled(true);
    }
}