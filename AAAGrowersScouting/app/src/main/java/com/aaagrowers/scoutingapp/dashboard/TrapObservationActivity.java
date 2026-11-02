package com.aaagrowers.scoutingapp.dashboard;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.aaagrowers.scoutingapp.R;
import com.aaagrowers.scoutingapp.auth.SessionManager;
import com.aaagrowers.scoutingapp.data.database.AppDatabase;
import com.aaagrowers.scoutingapp.data.database.dao.FcmTrapDao;
import com.aaagrowers.scoutingapp.data.database.dao.InsectTypeDao;
import com.aaagrowers.scoutingapp.data.database.dao.TrapObservationDao;
import com.aaagrowers.scoutingapp.data.database.entity.FcmTrapEntity;
import com.aaagrowers.scoutingapp.data.database.entity.InsectTypeEntity;
import com.aaagrowers.scoutingapp.data.database.entity.TrapObservationEntity;

import java.io.File;
import java.io.IOException;
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
import android.view.Gravity;
import android.widget.ScrollView;


/**
 * ================================================================
 * SCOUTING APP
 * TRAP OBSERVATION
 * ================================================================
 *
 * Handles:
 *
 * - FCM trap observations
 * - General moth trap observations
 * - Insect type selection
 * - Moth / insect count
 * - Observation trend
 * - Comments
 * - GPS capture
 * - Camera photo capture
 * - Offline Room persistence
 * - Pending sync status
 *
 * ================================================================
 */
public class TrapObservationActivity
        extends AppCompatActivity {


    // ================================================================
    // DATABASE
    // ================================================================

    private AppDatabase database;

    private FcmTrapDao fcmTrapDao;

    private InsectTypeDao insectTypeDao;

    private TrapObservationDao trapObservationDao;

    private ExecutorService executorService;


    // ================================================================
    // SESSION
    // ================================================================

    private SessionManager sessionManager;


    // ================================================================
    // TRAP
    // ================================================================

    private int trapId;

    private FcmTrapEntity trap;


    // ================================================================
    // HEADER / IDENTITY
    // ================================================================

    private ImageButton backButton;

    private TextView trapCodeText;

    private TextView trapTypeText;

    private TextView trapLocationText;

    private TextView observationModeText;


    // ================================================================
    // OBSERVATION
    // ================================================================

    private LinearLayout insectTypeSection;

    private LinearLayout mothCountSection;

    private TextView countLabelText;

    private TextView countHelperText;

    private ImageButton decreaseCountButton;

    private ImageButton increaseCountButton;

    private TextView mothCountText;


    // ================================================================
    // GENERAL MOTH — INSECT TYPE
    // ================================================================

    private AutoCompleteTextView insectTypeInput;

    private InsectTypeEntity selectedInsectType;

    private final List<InsectTypeEntity> insectTypes =
            new ArrayList<>();


    // ================================================================
    // TREND
    // ================================================================

    private TextView trendDecreasingButton;

    private TextView trendStableButton;

    private TextView trendIncreasingButton;


    // ================================================================
    // COMMENTS
    // ================================================================

    private EditText commentsInput;


    // ================================================================
    // FIELD CAPTURE
    // ================================================================

    private LinearLayout photoCard;

    private LinearLayout gpsCard;

    private TextView photoStatusText;

    private TextView gpsStatusText;


    // ================================================================
    // PHOTO
    // ================================================================

    /**
     * Absolute path to the locally captured photograph.
     *
     * This path is stored in TrapObservationEntity.photo_path.
     */
    private String photoPath = null;

    /**
     * URI supplied to the camera application.
     */
    private Uri photoUri = null;

    /**
     * Temporary file created before opening the camera.
     */
    private File photoFile = null;

    /**
     * Camera permission.
     */
    private static final int CAMERA_PERMISSION_REQUEST_CODE =
            2002;

    /**
     * Camera activity result.
     */
    private static final int CAMERA_CAPTURE_REQUEST_CODE =
            3001;


    // ================================================================
    // GPS
    // ================================================================

    private LocationManager locationManager;

    private LocationListener locationListener;

    private Double latitude = null;

    private Double longitude = null;

    private boolean gpsCaptured = false;

    private static final int LOCATION_PERMISSION_REQUEST_CODE =
            2001;


    // ================================================================
    // OBSERVATION TIME
    // ================================================================

    private TextView observationTimeText;

    private String observationTime;


    // ================================================================
    // SAVE
    // ================================================================

    private Button saveObservationButton;

    private boolean savingObservation = false;


    // ================================================================
    // OBSERVATION STATE
    // ================================================================

    private int mothCount = 0;

    private String selectedTrend = "";


    // ================================================================
    // LIFECYCLE
    // ================================================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_trap_observation
        );


        // ------------------------------------------------------------
        // SESSION
        // ------------------------------------------------------------

        sessionManager =
                new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {

            finish();

            return;
        }


        // ------------------------------------------------------------
        // DATABASE
        // ------------------------------------------------------------

        database =
                AppDatabase.getInstance(this);

        fcmTrapDao =
                database.fcmTrapDao();

        insectTypeDao =
                database.insectTypeDao();

        trapObservationDao =
                database.trapObservationDao();

        executorService =
                Executors.newSingleThreadExecutor();


        // ------------------------------------------------------------
        // LOCATION
        // ------------------------------------------------------------

        locationManager =
                (LocationManager)
                        getSystemService(
                                Context.LOCATION_SERVICE
                        );


        // ------------------------------------------------------------
        // OBSERVATION TIME
        // ------------------------------------------------------------

        observationTime =
                getCurrentTimestamp();


        // ------------------------------------------------------------
        // GET TRAP ID
        // ------------------------------------------------------------

        trapId =
                getIntent().getIntExtra(
                        "trap_id",
                        -1
                );


        if (trapId <= 0) {

            Toast.makeText(
                    this,
                    "Invalid trap.",
                    Toast.LENGTH_LONG
            ).show();

            finish();

            return;
        }


        // ------------------------------------------------------------
        // BIND VIEWS
        // ------------------------------------------------------------

        bindViews();


        // ------------------------------------------------------------
        // DISPLAY TIME
        // ------------------------------------------------------------

        observationTimeText.setText(
                formatObservationTime(
                        observationTime
                )
        );


        // ------------------------------------------------------------
        // INITIAL PHOTO STATE
        // ------------------------------------------------------------

        photoStatusText.setText(
                "Tap to capture a photo"
        );


        // ------------------------------------------------------------
        // INITIAL GPS STATE
        // ------------------------------------------------------------

        gpsStatusText.setText(
                "Tap to capture GPS location"
        );


        // ------------------------------------------------------------
        // SETUP EVENTS
        // ------------------------------------------------------------

        setupEvents();


        // ------------------------------------------------------------
        // LOAD TRAP
        // ------------------------------------------------------------

        loadTrap();
    }


    // ================================================================
    // BIND VIEWS
    // ================================================================

    private void bindViews() {

        // ------------------------------------------------------------
        // HEADER
        // ------------------------------------------------------------

        backButton =
                findViewById(
                        R.id.backButton
                );

        trapCodeText =
                findViewById(
                        R.id.trapCodeText
                );

        trapTypeText =
                findViewById(
                        R.id.trapTypeText
                );

        trapLocationText =
                findViewById(
                        R.id.trapLocationText
                );

        observationModeText =
                findViewById(
                        R.id.observationModeText
                );


        // ------------------------------------------------------------
        // OBSERVATION
        // ------------------------------------------------------------

        insectTypeSection =
                findViewById(
                        R.id.insectTypeSection
                );

        insectTypeInput =
                findViewById(
                        R.id.insectTypeInput
                );

        mothCountSection =
                findViewById(
                        R.id.mothCountSection
                );

        countLabelText =
                findViewById(
                        R.id.countLabelText
                );

        countHelperText =
                findViewById(
                        R.id.countHelperText
                );

        decreaseCountButton =
                findViewById(
                        R.id.decreaseCountButton
                );

        increaseCountButton =
                findViewById(
                        R.id.increaseCountButton
                );

        mothCountText =
                findViewById(
                        R.id.mothCountText
                );


        // ------------------------------------------------------------
        // TREND
        // ------------------------------------------------------------

        trendDecreasingButton =
                findViewById(
                        R.id.trendDecreasingButton
                );

        trendStableButton =
                findViewById(
                        R.id.trendStableButton
                );

        trendIncreasingButton =
                findViewById(
                        R.id.trendIncreasingButton
                );


        // ------------------------------------------------------------
        // COMMENTS
        // ------------------------------------------------------------

        commentsInput =
                findViewById(
                        R.id.commentsInput
                );


        // ------------------------------------------------------------
        // FIELD CAPTURE
        // ------------------------------------------------------------

        photoCard =
                findViewById(
                        R.id.photoCard
                );

        gpsCard =
                findViewById(
                        R.id.gpsCard
                );

        photoStatusText =
                findViewById(
                        R.id.photoStatusText
                );

        gpsStatusText =
                findViewById(
                        R.id.gpsStatusText
                );


        // ------------------------------------------------------------
        // TIME
        // ------------------------------------------------------------

        observationTimeText =
                findViewById(
                        R.id.observationTimeText
                );


        // ------------------------------------------------------------
        // SAVE
        // ------------------------------------------------------------

        saveObservationButton =
                findViewById(
                        R.id.saveObservationButton
                );
    }


    // ================================================================
    // SETUP EVENTS
    // ================================================================

    private void setupEvents() {

        // ------------------------------------------------------------
        // BACK
        // ------------------------------------------------------------

        backButton.setOnClickListener(
                v -> finish()
        );


        // ------------------------------------------------------------
        // INSECT TYPE
        // ------------------------------------------------------------

        setupInsectTypeSelector();


        // ------------------------------------------------------------
        // INCREASE COUNT
        // ------------------------------------------------------------

        increaseCountButton.setOnClickListener(
                v -> {

                    mothCount++;

                    updateMothCount();
                }
        );


        // ------------------------------------------------------------
        // DECREASE COUNT
        // ------------------------------------------------------------

        decreaseCountButton.setOnClickListener(
                v -> {

                    if (mothCount > 0) {

                        mothCount--;

                        updateMothCount();
                    }
                }
        );


        // ------------------------------------------------------------
        // TREND
        // ------------------------------------------------------------

        trendDecreasingButton.setOnClickListener(
                v -> selectTrend(
                        "decreasing"
                )
        );

        trendStableButton.setOnClickListener(
                v -> selectTrend(
                        "stable"
                )
        );

        trendIncreasingButton.setOnClickListener(
                v -> selectTrend(
                        "increasing"
                )
        );


        // ------------------------------------------------------------
        // PHOTO
        // ------------------------------------------------------------

        photoCard.setOnClickListener(
                v -> capturePhoto()
        );


        // ------------------------------------------------------------
        // GPS
        // ------------------------------------------------------------

        gpsCard.setOnClickListener(
                v -> captureGpsLocation()
        );


        // ------------------------------------------------------------
        // SAVE
        // ------------------------------------------------------------

        saveObservationButton.setOnClickListener(
                v -> validateAndSaveObservation()
        );
    }


    // ================================================================
    // LOAD TRAP
    // ================================================================

    private void loadTrap() {

        executorService.execute(
                () -> {

                    FcmTrapEntity result =
                            fcmTrapDao.findByTrapId(
                                    trapId
                            );


                    runOnUiThread(
                            () -> {

                                if (result == null) {

                                    Toast.makeText(
                                            this,
                                            "Trap could not be found.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    finish();

                                    return;
                                }


                                trap = result;

                                displayTrap();
                            }
                    );
                }
        );
    }


    // ================================================================
    // DISPLAY TRAP
    // ================================================================

    private void displayTrap() {

        if (trap == null) {

            return;
        }


        trapCodeText.setText(
                trap.trap_code != null
                        ? trap.trap_code
                        : "UNKNOWN"
        );


        String trapType =
                trap.trap_type != null
                        ? trap.trap_type.trim()
                        : "";


        if (
                trapType.equalsIgnoreCase(
                        "FCM"
                )
        ) {

            setupFcmForm();

        } else {

            setupGeneralMothForm();
        }


        trapLocationText.setText(
                "Bay " + trap.bay_id
        );
    }


    // ================================================================
    // FCM FORM
    // ================================================================

    private void setupFcmForm() {

        trapTypeText.setText(
                "FCM TRAP"
        );

        observationModeText.setText(
                "FCM MOTH COUNT"
        );


        insectTypeSection.setVisibility(
                View.GONE
        );

        selectedInsectType = null;

        insectTypeInput.setText(
                "",
                false
        );


        mothCountSection.setVisibility(
                View.VISIBLE
        );

        countLabelText.setText(
                "MOTH COUNT"
        );

        countHelperText.setText(
                "Enter the number of moths observed in the FCM trap."
        );


        mothCount = 0;

        updateMothCount();


        clearTrendSelection();
    }


    // ================================================================
    // GENERAL MOTH FORM
    // ================================================================

    private void setupGeneralMothForm() {

        trapTypeText.setText(
                "GENERAL MOTH TRAP"
        );

        observationModeText.setText(
                "GENERAL MOTH OBSERVATION"
        );


        insectTypeSection.setVisibility(
                View.VISIBLE
        );


        mothCountSection.setVisibility(
                View.VISIBLE
        );

        countLabelText.setText(
                "INSECT COUNT"
        );

        countHelperText.setText(
                "Enter the number of insects observed."
        );


        mothCount = 0;

        updateMothCount();

        selectedInsectType = null;

        insectTypeInput.setText(
                "",
                false
        );


        loadInsectTypes();
    }


    // ================================================================
    // LOAD ACTIVE INSECT TYPES
    // ================================================================

    private void loadInsectTypes() {

        executorService.execute(
                () -> {

                    List<InsectTypeEntity> results =
                            insectTypeDao
                                    .getActiveInsectTypes();


                    runOnUiThread(
                            () -> {

                                insectTypes.clear();


                                if (
                                        results == null ||
                                                results.isEmpty()
                                ) {

                                    insectTypeInput.setText(
                                            "",
                                            false
                                    );

                                    insectTypeInput.setHint(
                                            "No insect types available"
                                    );

                                    insectTypeInput.setEnabled(
                                            false
                                    );

                                    return;
                                }


                                insectTypes.addAll(
                                        results
                                );


                                List<String> insectNames =
                                        new ArrayList<>();


                                for (
                                        InsectTypeEntity insect :
                                        insectTypes
                                ) {

                                    if (
                                            insect != null &&
                                                    insect.name != null &&
                                                    !insect.name
                                                            .trim()
                                                            .isEmpty()
                                    ) {

                                        insectNames.add(
                                                insect.name
                                        );
                                    }
                                }


                                ArrayAdapter<String> adapter =
                                        new ArrayAdapter<>(
                                                this,
                                                android.R.layout
                                                        .simple_dropdown_item_1line,
                                                insectNames
                                        );


                                insectTypeInput.setAdapter(
                                        adapter
                                );


                                insectTypeInput.setEnabled(
                                        true
                                );

                                insectTypeInput.setHint(
                                        "Select insect type"
                                );
                            }
                    );
                }
        );
    }


    // ================================================================
    // INSECT TYPE SELECTOR
    // ================================================================

    private void setupInsectTypeSelector() {

        insectTypeInput.setOnItemClickListener(
                (parent, view, position, id) -> {

                    if (
                            position < 0 ||
                                    position >= insectTypes.size()
                    ) {

                        selectedInsectType = null;

                        return;
                    }


                    selectedInsectType =
                            insectTypes.get(
                                    position
                            );


                    if (
                            selectedInsectType != null
                    ) {

                        insectTypeInput.setText(
                                selectedInsectType.name,
                                false
                        );
                    }
                }
        );


        insectTypeInput.setOnFocusChangeListener(
                (v, hasFocus) -> {

                    if (
                            hasFocus &&
                                    !insectTypes.isEmpty()
                    ) {

                        insectTypeInput.showDropDown();
                    }
                }
        );
    }


    // ================================================================
    // UPDATE COUNT
    // ================================================================

    private void updateMothCount() {

        mothCountText.setText(
                String.valueOf(
                        mothCount
                )
        );
    }


    // ================================================================
    // TREND
    // ================================================================

    private void selectTrend(
            String trend
    ) {

        selectedTrend = trend;


        trendDecreasingButton.setSelected(
                false
        );

        trendStableButton.setSelected(
                false
        );

        trendIncreasingButton.setSelected(
                false
        );


        if (
                "decreasing".equals(
                        trend
                )
        ) {

            trendDecreasingButton.setSelected(
                    true
            );

        } else if (
                "stable".equals(
                        trend
                )
        ) {

            trendStableButton.setSelected(
                    true
            );

        } else if (
                "increasing".equals(
                        trend
                )
        ) {

            trendIncreasingButton.setSelected(
                    true
            );
        }


        updateTrendTextColours();
    }


    // ================================================================
    // CLEAR TREND
    // ================================================================

    private void clearTrendSelection() {

        selectedTrend = "";


        trendDecreasingButton.setSelected(
                false
        );

        trendStableButton.setSelected(
                false
        );

        trendIncreasingButton.setSelected(
                false
        );


        updateTrendTextColours();
    }


    // ================================================================
    // TREND TEXT COLOURS
    // ================================================================

    private void updateTrendTextColours() {

        trendDecreasingButton.setTextColor(
                trendDecreasingButton.isSelected()
                        ? 0xFFFFFFFF
                        : 0xFF52695A
        );

        trendStableButton.setTextColor(
                trendStableButton.isSelected()
                        ? 0xFFFFFFFF
                        : 0xFF52695A
        );

        trendIncreasingButton.setTextColor(
                trendIncreasingButton.isSelected()
                        ? 0xFFFFFFFF
                        : 0xFF52695A
        );
    }


    // ================================================================
    // PHOTO
    // ================================================================

    private void capturePhoto() {

        // ------------------------------------------------------------
        // CAMERA PERMISSION
        // ------------------------------------------------------------

        if (
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.CAMERA
                    },
                    CAMERA_PERMISSION_REQUEST_CODE
            );

            return;
        }


        // ------------------------------------------------------------
        // CHECK CAMERA
        // ------------------------------------------------------------

        Intent cameraIntent =
                new Intent(
                        MediaStore.ACTION_IMAGE_CAPTURE
                );


        if (
                cameraIntent.resolveActivity(
                        getPackageManager()
                ) == null
        ) {

            Toast.makeText(
                    this,
                    "No camera application is available.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        // ------------------------------------------------------------
        // CREATE PHOTO FILE
        // ------------------------------------------------------------

        try {

            photoFile =
                    createPhotoFile();

        } catch (IOException e) {

            Toast.makeText(
                    this,
                    "Unable to prepare photo storage.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        // ------------------------------------------------------------
        // FILE URI
        // ------------------------------------------------------------

        photoUri =
                FileProvider.getUriForFile(
                        this,
                        getPackageName()
                                + ".fileprovider",
                        photoFile
                );


        // ------------------------------------------------------------
        // CAMERA OUTPUT
        // ------------------------------------------------------------

        cameraIntent.putExtra(
                MediaStore.EXTRA_OUTPUT,
                photoUri
        );


        cameraIntent.addFlags(
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        |
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
        );


        // ------------------------------------------------------------
        // OPEN CAMERA
        // ------------------------------------------------------------

        photoStatusText.setText(
                "Opening camera..."
        );


        startActivityForResult(
                cameraIntent,
                CAMERA_CAPTURE_REQUEST_CODE
        );
    }


    // ================================================================
    // CREATE PHOTO FILE
    // ================================================================

    private File createPhotoFile()
            throws IOException {

        String timestamp =
                new SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.US
                ).format(
                        new Date()
                );


        String trapCode =
                trap != null &&
                        trap.trap_code != null
                        ? trap.trap_code
                        : "TRAP";


        /*
         * Remove characters that should not appear in filenames.
         */

        trapCode =
                trapCode.replaceAll(
                        "[^a-zA-Z0-9_-]",
                        "_"
                );


        String fileName =
                "TRAP_"
                        + trapCode
                        + "_"
                        + timestamp
                        + "_";


        File storageDirectory =
                new File(
                        getExternalFilesDir(
                                Environment.DIRECTORY_PICTURES
                        ),
                        "TrapObservations"
                );


        if (
                !storageDirectory.exists()
        ) {

            if (
                    !storageDirectory.mkdirs()
            ) {

                throw new IOException(
                        "Could not create photo directory."
                );
            }
        }


        return File.createTempFile(
                fileName,
                ".jpg",
                storageDirectory
        );
    }


    // ================================================================
    // CAMERA RESULT
    // ================================================================

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
                requestCode !=
                        CAMERA_CAPTURE_REQUEST_CODE
        ) {

            return;
        }


        // ------------------------------------------------------------
        // PHOTO CAPTURED
        // ------------------------------------------------------------

        if (
                resultCode ==
                        RESULT_OK
        ) {

            if (
                    photoFile != null &&
                            photoFile.exists() &&
                            photoFile.length() > 0
            ) {

                photoPath =
                        photoFile.getAbsolutePath();


                photoStatusText.setText(
                        "PHOTO CAPTURED\n"
                                + photoFile.getName()
                );


                Toast.makeText(
                        this,
                        "Photo captured successfully.",
                        Toast.LENGTH_SHORT
                ).show();


            } else {

                photoPath = null;

                photoStatusText.setText(
                        "Photo capture failed"
                );


                Toast.makeText(
                        this,
                        "The photo could not be saved.",
                        Toast.LENGTH_LONG
                ).show();
            }


        } else {

            /*
             * User cancelled the camera.
             *
             * Delete the unused temporary file so that we do not
             * leave empty files inside the application storage.
             */

            deleteTemporaryPhoto();


            photoPath = null;

            photoStatusText.setText(
                    "Tap to capture a photo"
            );
        }
    }


    // ================================================================
    // DELETE TEMPORARY PHOTO
    // ================================================================

    private void deleteTemporaryPhoto() {

        if (
                photoFile == null
        ) {

            return;
        }


        try {

            if (
                    photoFile.exists()
            ) {

                photoFile.delete();
            }

        } catch (Exception ignored) {
        }


        photoFile = null;

        photoUri = null;
    }


    // ================================================================
    // CAMERA PERMISSION RESULT
    // ================================================================

    private void handleCameraPermissionResult(
            int[] grantResults
    ) {

        if (
                grantResults.length > 0 &&
                        grantResults[0]
                                == PackageManager.PERMISSION_GRANTED
        ) {

            capturePhoto();

        } else {

            photoStatusText.setText(
                    "Camera permission required"
            );


            Toast.makeText(
                    this,
                    "Camera permission was denied.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    // ================================================================
    // GPS
    // ================================================================

    private void captureGpsLocation() {

        // ------------------------------------------------------------
        // PERMISSION
        // ------------------------------------------------------------

        if (
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                        &&
                        ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE
            );

            return;
        }


        // ------------------------------------------------------------
        // LOCATION SERVICE
        // ------------------------------------------------------------

        if (locationManager == null) {

            Toast.makeText(
                    this,
                    "Location service is not available.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        // ------------------------------------------------------------
        // PROVIDERS
        // ------------------------------------------------------------

        boolean gpsEnabled =
                locationManager.isProviderEnabled(
                        LocationManager.GPS_PROVIDER
                );

        boolean networkEnabled =
                locationManager.isProviderEnabled(
                        LocationManager.NETWORK_PROVIDER
                );


        if (
                !gpsEnabled &&
                        !networkEnabled
        ) {

            Toast.makeText(
                    this,
                    "Please turn on Location services.",
                    Toast.LENGTH_LONG
            ).show();

            gpsStatusText.setText(
                    "Location services are off"
            );

            return;
        }


        // ------------------------------------------------------------
        // UI
        // ------------------------------------------------------------

        gpsStatusText.setText(
                "CAPTURING LOCATION..."
        );


        // ------------------------------------------------------------
        // LISTENER
        // ------------------------------------------------------------

        locationListener =
                new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            @NonNull Location location
                    ) {

                        if (location == null) {

                            return;
                        }


                        updateGpsStatus(
                                location
                        );


                        stopGpsUpdates();
                    }


                    @Override
                    public void onProviderEnabled(
                            @NonNull String provider
                    ) {
                    }


                    @Override
                    public void onProviderDisabled(
                            @NonNull String provider
                    ) {
                    }
                };


        // ------------------------------------------------------------
        // REQUEST UPDATES
        // ------------------------------------------------------------

        try {

            if (gpsEnabled) {

                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000,
                        1,
                        locationListener
                );
            }


            if (networkEnabled) {

                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        1000,
                        1,
                        locationListener
                );
            }


            // --------------------------------------------------------
            // LAST KNOWN LOCATION
            // --------------------------------------------------------

            Location lastKnownLocation = null;


            if (gpsEnabled) {

                lastKnownLocation =
                        locationManager.getLastKnownLocation(
                                LocationManager.GPS_PROVIDER
                        );
            }


            if (
                    lastKnownLocation == null &&
                            networkEnabled
            ) {

                lastKnownLocation =
                        locationManager.getLastKnownLocation(
                                LocationManager.NETWORK_PROVIDER
                        );
            }


            if (lastKnownLocation != null) {

                updateGpsStatus(
                        lastKnownLocation
                );

                stopGpsUpdates();
            }


        } catch (SecurityException e) {

            gpsStatusText.setText(
                    "Location permission required"
            );

            Toast.makeText(
                    this,
                    "Location permission is required.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    // ================================================================
    // UPDATE GPS
    // ================================================================

    private void updateGpsStatus(
            Location location
    ) {

        if (location == null) {

            gpsCaptured = false;

            gpsStatusText.setText(
                    "Location unavailable"
            );

            return;
        }


        latitude =
                location.getLatitude();

        longitude =
                location.getLongitude();

        gpsCaptured = true;


        String latitudeText =
                String.format(
                        Locale.US,
                        "%.6f",
                        latitude
                );

        String longitudeText =
                String.format(
                        Locale.US,
                        "%.6f",
                        longitude
                );


        gpsStatusText.setText(
                "GPS CAPTURED\n"
                        + latitudeText
                        + ", "
                        + longitudeText
        );


        Toast.makeText(
                this,
                "GPS location captured.",
                Toast.LENGTH_SHORT
        ).show();
    }


    // ================================================================
    // STOP GPS
    // ================================================================

    private void stopGpsUpdates() {

        if (
                locationManager == null ||
                        locationListener == null
        ) {

            return;
        }


        try {

            locationManager.removeUpdates(
                    locationListener
            );

        } catch (SecurityException ignored) {
        }
    }


    // ================================================================
    // PERMISSION RESULT
    // ================================================================

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


        // ------------------------------------------------------------
        // CAMERA
        // ------------------------------------------------------------

        if (
                requestCode ==
                        CAMERA_PERMISSION_REQUEST_CODE
        ) {

            handleCameraPermissionResult(
                    grantResults
            );

            return;
        }


        // ------------------------------------------------------------
        // LOCATION
        // ------------------------------------------------------------

        if (
                requestCode !=
                        LOCATION_PERMISSION_REQUEST_CODE
        ) {

            return;
        }


        boolean granted = false;


        for (
                int result :
                grantResults
        ) {

            if (
                    result ==
                            PackageManager.PERMISSION_GRANTED
            ) {

                granted = true;

                break;
            }
        }


        if (granted) {

            captureGpsLocation();

        } else {

            gpsCaptured = false;

            gpsStatusText.setText(
                    "Location permission required"
            );


            Toast.makeText(
                    this,
                    "Location permission was denied.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    // ================================================================
    // VALIDATE + SAVE
    // ================================================================

    private void validateAndSaveObservation() {

        if (savingObservation) {

            return;
        }


        // ------------------------------------------------------------
        // TRAP
        // ------------------------------------------------------------

        if (trap == null) {

            Toast.makeText(
                    this,
                    "Trap information is not ready.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        String trapType =
                trap.trap_type != null
                        ? trap.trap_type.trim()
                        : "";


        // ------------------------------------------------------------
        // GENERAL MOTH — INSECT TYPE
        // ------------------------------------------------------------

        if (
                !trapType.equalsIgnoreCase(
                        "FCM"
                )
        ) {

            if (
                    selectedInsectType == null
            ) {

                Toast.makeText(
                        this,
                        "Please select an insect type.",
                        Toast.LENGTH_SHORT
                ).show();

                insectTypeInput.requestFocus();

                insectTypeInput.showDropDown();

                return;
            }
        }


        // ------------------------------------------------------------
        // COUNT
        // ------------------------------------------------------------

        if (mothCount < 0) {

            Toast.makeText(
                    this,
                    "Count cannot be negative.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // ------------------------------------------------------------
        // TREND
        // ------------------------------------------------------------

        if (
                selectedTrend == null ||
                        selectedTrend.trim().isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "Please select the observation trend.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // ------------------------------------------------------------
        // GPS
        // ------------------------------------------------------------

//        if (!gpsCaptured) {
//
//            Toast.makeText(
//                    this,
//                    "Please capture the GPS location.",
//                    Toast.LENGTH_SHORT
//            ).show();
//
//            return;
//        }


        // ------------------------------------------------------------
        // SAVE
        // ------------------------------------------------------------

       // saveObservation();

        // ------------------------------------------------------------
// GPS
// ------------------------------------------------------------

        if (!gpsCaptured) {

            Toast.makeText(
                    this,
                    "Please capture the GPS location.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


// ------------------------------------------------------------
// CONFIRM OBSERVATION
// ------------------------------------------------------------

        showObservationConfirmation();
    }


    // ================================================================
// CONFIRM OBSERVATION
// ================================================================

    private void showObservationConfirmation() {

        if (trap == null) {
            return;
        }

        String trapType =
                trap.trap_type != null
                        ? trap.trap_type.trim()
                        : "";

        boolean isFcm =
                trapType.equalsIgnoreCase("FCM");


        // ------------------------------------------------------------
        // BUILD CONFIRMATION CONTENT
        // ------------------------------------------------------------

        LinearLayout container =
                new LinearLayout(this);

        container.setOrientation(
                LinearLayout.VERTICAL
        );

        container.setPadding(
                48,
                10,
                48,
                10
        );


        // ------------------------------------------------------------
        // INTRO
        // ------------------------------------------------------------

        TextView intro =
                new TextView(this);

        intro.setText(
                "Please confirm that the following observation is correct."
        );

        intro.setTextSize(15);

        intro.setTextColor(
                Color.rgb(82, 105, 90)
        );

        intro.setPadding(
                0,
                0,
                0,
                25
        );

        container.addView(
                intro
        );


        // ------------------------------------------------------------
        // TRAP
        // ------------------------------------------------------------

        addConfirmationRow(
                container,
                "Trap",
                trap.trap_code != null
                        ? trap.trap_code
                        : "Unknown"
        );


        // ------------------------------------------------------------
        // TRAP TYPE
        // ------------------------------------------------------------

        addConfirmationRow(
                container,
                "Observation",
                isFcm
                        ? "FCM Moth Count"
                        : "General Moth"
        );


        // ------------------------------------------------------------
        // INSECT TYPE
        // ------------------------------------------------------------

        if (!isFcm) {

            addConfirmationRow(
                    container,
                    "Insect Type",
                    selectedInsectType != null
                            ? selectedInsectType.name
                            : "Not selected"
            );
        }


        // ------------------------------------------------------------
        // COUNT
        // ------------------------------------------------------------

        addConfirmationRow(
                container,
                isFcm
                        ? "Moth Count"
                        : "Insect Count",
                String.valueOf(
                        mothCount
                )
        );


        // ------------------------------------------------------------
        // TREND
        // ------------------------------------------------------------

        addConfirmationRow(
                container,
                "Trend",
                formatTrend(
                        selectedTrend
                )
        );


        // ------------------------------------------------------------
        // GPS
        // ------------------------------------------------------------

        String gpsText =
                latitude != null &&
                        longitude != null
                        ? String.format(
                        Locale.US,
                        "%.6f, %.6f",
                        latitude,
                        longitude
                )
                        : "Not captured";

        addConfirmationRow(
                container,
                "GPS Location",
                gpsText
        );


        // ------------------------------------------------------------
        // PHOTO
        // ------------------------------------------------------------

        String photoText =
                photoPath != null &&
                        !photoPath.trim().isEmpty()
                        ? "Photo captured"
                        : "No photo";

        addConfirmationRow(
                container,
                "Photo",
                photoText
        );


        // ------------------------------------------------------------
        // OBSERVATION TIME
        // ------------------------------------------------------------

        addConfirmationRow(
                container,
                "Observed At",
                formatObservationTime(
                        observationTime
                )
        );


        // ------------------------------------------------------------
        // COMMENTS
        // ------------------------------------------------------------

        String comments =
                commentsInput.getText()
                        .toString()
                        .trim();

        if (!comments.isEmpty()) {

            addConfirmationRow(
                    container,
                    "Comments",
                    comments
            );
        }


        // ------------------------------------------------------------
        // SCROLL VIEW
        // ------------------------------------------------------------

        ScrollView scrollView =
                new ScrollView(this);

        scrollView.setFillViewport(true);

        scrollView.addView(
                container
        );


        // ------------------------------------------------------------
        // DIALOG
        // ------------------------------------------------------------

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


        // ------------------------------------------------------------
        // SHOW
        // ------------------------------------------------------------

        dialog.setOnShowListener(
                d -> {

                    Button saveButton =
                            dialog.getButton(
                                    AlertDialog.BUTTON_POSITIVE
                            );

                    Button cancelButton =
                            dialog.getButton(
                                    AlertDialog.BUTTON_NEGATIVE
                            );


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


                    cancelButton.setTextColor(
                            Color.rgb(
                                    100,
                                    100,
                                    100
                            )
                    );


                    // ------------------------------------------------
                    // IMPORTANT:
                    // Do not let the dialog automatically dismiss
                    // until the save has actually been requested.
                    // ------------------------------------------------

                    saveButton.setOnClickListener(
                            v -> {

                                dialog.dismiss();

                                saveObservation();
                            }
                    );
                }
        );


        dialog.show();
    }


// ================================================================
// CONFIRMATION ROW
// ================================================================

    private void addConfirmationRow(
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
                8,
                0,
                14
        );


        // ------------------------------------------------------------
        // LABEL
        // ------------------------------------------------------------

        TextView labelText =
                new TextView(this);

        labelText.setText(
                label.toUpperCase(
                        Locale.US
                )
        );

        labelText.setTextSize(
                11
        );

        labelText.setTypeface(
                null,
                Typeface.BOLD
        );

        labelText.setTextColor(
                Color.rgb(
                        120,
                        120,
                        120
                )
        );


        // ------------------------------------------------------------
        // VALUE
        // ------------------------------------------------------------

        TextView valueText =
                new TextView(this);

        valueText.setText(
                value != null
                        ? value
                        : "-"
        );

        valueText.setTextSize(
                16
        );

        valueText.setTypeface(
                null,
                Typeface.BOLD
        );

        valueText.setTextColor(
                Color.rgb(
                        45,
                        55,
                        50
                )
        );

        valueText.setPadding(
                0,
                3,
                0,
                0
        );


        row.addView(
                labelText
        );

        row.addView(
                valueText
        );


        container.addView(
                row
        );
    }


// ================================================================
// FORMAT TREND
// ================================================================

    private String formatTrend(
            String trend
    ) {

        if (trend == null) {

            return "-";
        }

        switch (
                trend.toLowerCase(
                        Locale.US
                )
        ) {

            case "decreasing":

                return "Decreasing";

            case "stable":

                return "Stable";

            case "increasing":

                return "Increasing";

            default:

                return trend;
        }
    }


    // ================================================================
    // SAVE OBSERVATION TO ROOM
    // ================================================================

    private void saveObservation() {

        if (savingObservation) {

            return;
        }


        savingObservation = true;


        saveObservationButton.setEnabled(
                false
        );

        saveObservationButton.setText(
                "SAVING..."
        );


        // ------------------------------------------------------------
        // COPY VALUES BEFORE BACKGROUND THREAD
        // ------------------------------------------------------------

        final int finalTrapId =
                trap.id;

        final String finalTrapCode =
                trap.trap_code;

        final String finalTrapType =
                trap.trap_type;

        final Integer finalInsectTypeId =
                selectedInsectType != null
                        ? selectedInsectType.id
                        : null;

        final String finalInsectTypeName =
                selectedInsectType != null
                        ? selectedInsectType.name
                        : null;

        final int finalMothCount =
                mothCount;

        final String finalTrend =
                selectedTrend;

        final String finalComments =
                commentsInput.getText()
                        .toString()
                        .trim();

        final String finalPhotoPath =
                photoPath;

        final Double finalLatitude =
                latitude;

        final Double finalLongitude =
                longitude;

        final String finalObservedAt =
                observationTime;


        // ------------------------------------------------------------
        // USER ID
        // ------------------------------------------------------------

        final int finalUserId =
                getCurrentUserId();


        // ------------------------------------------------------------
        // CREATE ENTITY
        // ------------------------------------------------------------

        executorService.execute(
                () -> {

                    try {

                        TrapObservationEntity observation =
                                new TrapObservationEntity();

                        observation.client_uuid = UUID.randomUUID().toString();


                        observation.trap_id =
                                finalTrapId;

                        observation.trap_code =
                                finalTrapCode;

                        observation.trap_type =
                                finalTrapType;

                        observation.insect_type_id =
                                finalInsectTypeId;

                        observation.insect_type_name =
                                finalInsectTypeName;

                        observation.moth_count =
                                finalMothCount;

                        observation.trend =
                                finalTrend;

                        observation.comments =
                                finalComments.isEmpty()
                                        ? null
                                        : finalComments;

                        observation.photo_path =
                                finalPhotoPath;

                        observation.latitude =
                                finalLatitude;

                        observation.longitude =
                                finalLongitude;

                        observation.observed_at =
                                finalObservedAt;

                        observation.user_id =
                                finalUserId;

                        observation.sync_status =
                                "pending";

                        observation.sync_error =
                                null;

                        observation.created_at =
                                getCurrentTimestamp();


                        // ------------------------------------------------
                        // INSERT
                        // ------------------------------------------------

                        long observationId =
                                trapObservationDao.insert(
                                        observation
                                );


                        runOnUiThread(
                                () -> {

                                    savingObservation =
                                            false;

                                    saveObservationButton
                                            .setEnabled(true);

                                    saveObservationButton
                                            .setText(
                                                    "SAVE OBSERVATION"
                                            );


                                    if (
                                            observationId > 0
                                    ) {

                                        showSaveSuccess(
                                                observationId
                                        );

                                    } else {

                                        Toast.makeText(
                                                this,
                                                "Observation could not be saved.",
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                }
                        );


                    } catch (Exception e) {

                        runOnUiThread(
                                () -> {

                                    savingObservation =
                                            false;

                                    saveObservationButton
                                            .setEnabled(true);

                                    saveObservationButton
                                            .setText(
                                                    "SAVE OBSERVATION"
                                            );


                                    Toast.makeText(
                                            this,
                                            "Failed to save observation: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                        );
                    }
                }
        );
    }


    // ================================================================
    // GET CURRENT USER ID
    // ================================================================

    private int getCurrentUserId() {

        /*
         * Use the existing SessionManager user ID.
         */

        try {

            return sessionManager.getUserId();

        } catch (Exception ignored) {

            return 0;
        }
    }


    // ================================================================
    // SAVE SUCCESS
    // ================================================================

    private void showSaveSuccess(
            long observationId
    ) {

        Toast.makeText(
                this,
                "Observation saved offline.",
                Toast.LENGTH_SHORT
        ).show();


        /*
         * The observation is now safely stored locally.
         *
         * photo_path contains the local photograph path when a
         * photograph was captured.
         *
         * sync_status = pending
         *
         * Step 11 will pick this observation up when the user
         * presses Send to Database.
         */

        finish();
    }


    // ================================================================
    // CURRENT TIMESTAMP
    // ================================================================

    private String getCurrentTimestamp() {

        SimpleDateFormat format =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.US
                );

        return format.format(
                new Date()
        );
    }


    // ================================================================
    // DISPLAY OBSERVATION TIME
    // ================================================================

    private String formatObservationTime(
            String timestamp
    ) {

        try {

            SimpleDateFormat input =
                    new SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.US
                    );

            Date date =
                    input.parse(
                            timestamp
                    );


            if (date == null) {

                return timestamp;
            }


            SimpleDateFormat output =
                    new SimpleDateFormat(
                            "dd MMM yyyy • HH:mm",
                            Locale.US
                    );

            return output.format(
                    date
            );


        } catch (Exception e) {

            return timestamp;
        }
    }


    // ================================================================
    // DESTROY
    // ================================================================

    @Override
    protected void onDestroy() {

        // ------------------------------------------------------------
        // STOP GPS
        // ------------------------------------------------------------

        stopGpsUpdates();


        // ------------------------------------------------------------
        // SHUTDOWN EXECUTOR
        // ------------------------------------------------------------

        if (
                executorService != null
        ) {

            executorService.shutdown();
        }


        super.onDestroy();
    }
}