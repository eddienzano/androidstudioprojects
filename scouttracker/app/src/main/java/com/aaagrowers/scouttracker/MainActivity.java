package com.aaagrowers.scouttracker;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.*;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DBHelper dbHelper;

    private boolean isTracking = false;
    private Location currentLocation;

    private static final int LOCATION_PERMISSION_REQUEST = 100;

    private String sessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);
        Button btnObservation = findViewById(R.id.btnObservation);

        btnStart.setOnClickListener(v -> checkPermissionsAndStart());
        btnStop.setOnClickListener(v -> stopTracking());
        btnObservation.setOnClickListener(v -> openObservationDialog());

        Button btnSync = findViewById(R.id.btnSync);
        btnSync.setOnClickListener(v -> syncData());
    }

    // =========================
    // LOCATION PERMISSION
    // =========================
    private void checkPermissionsAndStart() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            startTracking();
        }
    }

    // =========================
    // START TRACKING
    // =========================
    private void startTracking() {

        sessionId = "SESSION_" + System.currentTimeMillis();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        isTracking = true;

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {

                if (locationResult == null || !isTracking) return;

                for (Location location : locationResult.getLocations()) {

                    currentLocation = location;

                    double lat = location.getLatitude();
                    double lng = location.getLongitude();
                    float accuracy = location.getAccuracy();

                    dbHelper.insertLocation(lat, lng, accuracy, formatNow(), sessionId);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
        );

        Toast.makeText(this, "Tracking Started", Toast.LENGTH_SHORT).show();
    }

    // =========================
    // STOP TRACKING
    // =========================
    private void stopTracking() {
        isTracking = false;

        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        Toast.makeText(this, "Tracking Stopped", Toast.LENGTH_SHORT).show();
    }

    // =========================
    // OBSERVATION DIALOG
    // =========================
    private void openObservationDialog() {

        if (currentLocation == null) {
            Toast.makeText(this, "Waiting for GPS...", Toast.LENGTH_SHORT).show();
            return;
        }

        long startTime = System.currentTimeMillis();

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_observation, null);

        EditText etGreenhouse = view.findViewById(R.id.etGreenhouse);
        EditText etBay = view.findViewById(R.id.etBay);

        CheckBox chkThrips = view.findViewById(R.id.chkThrips);
        EditText etThrips = view.findViewById(R.id.etThrips);

        CheckBox chkMites = view.findViewById(R.id.chkMites);
        EditText etMites = view.findViewById(R.id.etMites);

        CheckBox chkDowny = view.findViewById(R.id.chkDowny);
        EditText etDowny = view.findViewById(R.id.etDowny);

        CheckBox chkPowdery = view.findViewById(R.id.chkPowdery);
        EditText etPowdery = view.findViewById(R.id.etPowdery);

        CheckBox chkOther = view.findViewById(R.id.chkOther);
        EditText etOther = view.findViewById(R.id.etOther);

        // Toggle note fields
        chkThrips.setOnCheckedChangeListener((b, c) -> etThrips.setVisibility(c ? View.VISIBLE : View.GONE));
        chkMites.setOnCheckedChangeListener((b, c) -> etMites.setVisibility(c ? View.VISIBLE : View.GONE));
        chkDowny.setOnCheckedChangeListener((b, c) -> etDowny.setVisibility(c ? View.VISIBLE : View.GONE));
        chkPowdery.setOnCheckedChangeListener((b, c) -> etPowdery.setVisibility(c ? View.VISIBLE : View.GONE));
        chkOther.setOnCheckedChangeListener((b, c) -> etOther.setVisibility(c ? View.VISIBLE : View.GONE));

        new AlertDialog.Builder(this)
                .setTitle("Record Observation")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {

                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;

                    try {
                        JSONObject issues = new JSONObject();

                        if (chkThrips.isChecked()) issues.put("Thrips", etThrips.getText().toString());
                        if (chkMites.isChecked()) issues.put("Mites", etMites.getText().toString());
                        if (chkDowny.isChecked()) issues.put("Downy", etDowny.getText().toString());
                        if (chkPowdery.isChecked()) issues.put("Powdery", etPowdery.getText().toString());
                        if (chkOther.isChecked()) issues.put("Other", etOther.getText().toString());

                        dbHelper.insertObservation(
                                currentLocation.getLatitude(),
                                currentLocation.getLongitude(),
                                formatTime(startTime),
                                formatTime(endTime),
                                duration,
                                etGreenhouse.getText().toString(),
                                etBay.getText().toString(),
                                issues.toString(),sessionId
                        );

                        Toast.makeText(this, "Saved (" + duration / 1000 + " sec)", Toast.LENGTH_SHORT).show();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =========================
    // TIME FORMATTERS
    // =========================
    private String formatNow() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
    }

    private String formatTime(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date(millis));
    }

    // =========================
    // PERMISSION RESULT
    // =========================
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTracking();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    private void syncData() {

        syncObservations();
        syncLocations();
    }

    private void syncObservations() {

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM observations", null);

        RequestQueue queue = Volley.newRequestQueue(this);

        while (cursor.moveToNext()) {

            try {
                JSONObject body = new JSONObject();

                body.put("user_id", 1); // change if multi-user
                body.put("latitude", cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")));
                body.put("longitude", cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")));
                body.put("start_time", cursor.getString(cursor.getColumnIndexOrThrow("start_time")));
                body.put("end_time", cursor.getString(cursor.getColumnIndexOrThrow("end_time")));
                body.put("duration", cursor.getLong(cursor.getColumnIndexOrThrow("duration")));
                body.put("greenhouse", cursor.getString(cursor.getColumnIndexOrThrow("greenhouse")));
                body.put("bay", cursor.getString(cursor.getColumnIndexOrThrow("bay")));
                body.put("issues", cursor.getString(cursor.getColumnIndexOrThrow("issues")));
                body.put("session_id", cursor.getString(cursor.getColumnIndexOrThrow("session_id")));

                String url = "https://www.aaagrowers.co.ke/scout/api/insert_observation.php";

                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        body,
                        response -> Toast.makeText(this, "Observations synced", Toast.LENGTH_SHORT).show(),
                        error -> Toast.makeText(this, "Sync error", Toast.LENGTH_SHORT).show()
                );

                queue.add(request);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        cursor.close();
    }

    private void syncLocations() {

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM location_trace", null);

        RequestQueue queue = Volley.newRequestQueue(this);

        while (cursor.moveToNext()) {

            try {
                JSONObject body = new JSONObject();

                body.put("user_id", 1);
                body.put("latitude", cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")));
                body.put("longitude", cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")));
                body.put("accuracy", cursor.getFloat(cursor.getColumnIndexOrThrow("accuracy")));
                body.put("timestamp", cursor.getString(cursor.getColumnIndexOrThrow("timestamp")));
                body.put("session_id", cursor.getString(cursor.getColumnIndexOrThrow("session_id")));

                String url = "https://www.aaagrowers.co.ke/scout/api/insert_location.php";

                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        body,
                        response -> {},
                        error -> {}
                );

                queue.add(request);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        cursor.close();

        Toast.makeText(this, "Sync completed", Toast.LENGTH_SHORT).show();
    }
}