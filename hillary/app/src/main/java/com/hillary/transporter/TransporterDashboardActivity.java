package com.hillary.transporter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.*;
import com.hillary.R;
import com.hillary.network.ApiClient;
import com.hillary.network.Constants;
import com.hillary.transporter.map.TransporterMapManager;
import com.hillary.transporter.model.DriverState;
import com.hillary.transporter.service.TransporterLocationService;
import com.hillary.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.*;

public class TransporterDashboardActivity extends AppCompatActivity {

    private TextView tvName, tvPhone, tvStatus;
    private TextView tvActiveOrderNo, tvActiveAddress, tvActiveStatus;

    private Button btnStart, btnArrived, btnDelivered;

    private SessionManager session;
    private OkHttpClient client;

    private TransporterMapManager mapManager;

    private String transporterId;
    private String activeOrderId;

    private DriverState currentState = DriverState.IDLE;

    private static final int LOCATION_PERMISSION_CODE = 1001;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transporter_dashboard);

        session = new SessionManager(this);
        client = ApiClient.getClient(this);
        transporterId = session.getTransporterId();

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        initViews();
        initMap();

        updateDriverState(DriverState.IDLE);

        checkLocationPermission();
        loadJobs();
    }

    @SuppressLint("MissingPermission")
    private void startMapLocationUpdates() {

        LocationRequest request =
                new LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        5000
                ).build();

        locationCallback =
                new LocationCallback() {

                    @Override
                    public void onLocationResult(LocationResult result) {

                        if (result == null) return;

                        android.location.Location location =
                                result.getLastLocation();

                        if (location == null) return;

                        mapManager.updateDriverLocation(
                                location.getLatitude(),
                                location.getLongitude()
                        );
                    }
                };

        fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                getMainLooper()
        );
    }


    // ===================== LOCATION =====================

    private void checkLocationPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE
            );

        } else {
            startTrackingService();
            startMapLocationUpdates();
        }
    }

    private void startTrackingService() {

        Intent intent =
                new Intent(this,
                        TransporterLocationService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_CODE) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startTrackingService();
                startMapLocationUpdates();

            } else {
                Toast.makeText(this,
                        "Location permission required",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }



    // ===================== UI =====================

    private void initViews() {

        tvName = findViewById(R.id.tvName);
        tvPhone = findViewById(R.id.tvPhone);
        tvStatus = findViewById(R.id.tvStatus);

        tvActiveOrderNo = findViewById(R.id.tvActiveOrderNo);
        tvActiveAddress = findViewById(R.id.tvActiveAddress);
        tvActiveStatus = findViewById(R.id.tvActiveStatus);

        btnStart = findViewById(R.id.btnStart);
        btnArrived = findViewById(R.id.btnArrived);
        btnDelivered = findViewById(R.id.btnDelivered);

        btnStart.setOnClickListener(v ->
                startTrip());

        btnArrived.setOnClickListener(v ->
                updateStatus("arrived_at_pickup"));

        btnDelivered.setOnClickListener(v ->
                updateStatus("delivered"));

    }

    // ===================== MAP =====================

    private void initMap() {
        mapManager = new TransporterMapManager(
                this,
                findViewById(R.id.mapContainer)
        );

        mapManager.initMap();
    }

    // ===================== LOAD JOBS =====================

    private void loadJobs() {

        String url =
                Constants.TRANSPORTER_DASHBOARD_URL
                        + "?transporter_id="
                        + transporterId;

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

                runOnUiThread(() ->
                        Toast.makeText(
                                TransporterDashboardActivity.this,
                                "Network Error",
                                Toast.LENGTH_SHORT
                        ).show());
            }

            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {

                try {

                    String json = response.body().string();

                    JSONObject obj = new JSONObject(json);

                    if (!obj.getBoolean("success")) return;

                    JSONObject transporter =
                            obj.getJSONObject("transporter");

                    JSONArray deliveries =
                            obj.getJSONArray("deliveries");

                    JSONObject activeOrder = null;

                    for (int i = 0; i < deliveries.length(); i++) {

                        JSONObject d = deliveries.getJSONObject(i);

                        String status =
                                d.optString("order_status");

                        if (status.equals("processing")
                                || status.equals("ready")
                                || status.equals("out_for_delivery")) {

                            activeOrder = d;
                            break;
                        }
                    }

                    JSONObject finalActiveOrder = activeOrder;

                    runOnUiThread(() -> {

                        tvName.setText(
                                transporter.optString("full_name")
                        );

                        tvPhone.setText(
                                transporter.optString("phone")
                        );

                        if (finalActiveOrder != null) {

                            activeOrderId =
                                    finalActiveOrder.optString("id");

                            tvActiveOrderNo.setText(
                                    "ORDER #"
                                            + finalActiveOrder.optString("order_number")
                            );

                            tvActiveAddress.setText(
                                    finalActiveOrder.optString("delivery_address")
                            );

                            tvActiveStatus.setText(
                                    finalActiveOrder.optString("order_status")
                            );

                            String orderStatus =
                                    finalActiveOrder.optString("order_status");

                            String tripStatus =
                                    finalActiveOrder.optString(
                                            "trip_status",
                                            "assigned"
                                    );



                            if (orderStatus.equals("processing")) {

                                updateDriverState(
                                        DriverState.JOB_ASSIGNED
                                );

                            } else if (orderStatus.equals("ready")) {

                                updateDriverState(
                                        DriverState.AT_PICKUP
                                );

                            } else if (orderStatus.equals("out_for_delivery")) {

                                updateDriverState(
                                        DriverState.OUT_FOR_DELIVERY
                                );
                            }

                            updateButtons(tripStatus);

                            mapManager.showRoute(
                                    finalActiveOrder.optString("latitude"),
                                    finalActiveOrder.optString("longitude")
                            );

                        } else {

                            activeOrderId = null;



                            tvActiveOrderNo.setText(
                                    "No Active Delivery"
                            );

                            tvActiveAddress.setText(
                                    "Waiting for jobs..."
                            );

                            tvActiveStatus.setText(
                                    "IDLE"
                            );

                            updateDriverState(
                                    DriverState.IDLE
                            );

                            updateButtons("assigned");

                            mapManager.clearRoute();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // ===================== UPDATE STATUS =====================

    private void updateStatus(String status) {

        if (!canUpdate(status)) {
            Toast.makeText(this,
                    "Action not allowed in current state",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (activeOrderId == null) {
            Toast.makeText(this,
                    "No Active Delivery",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body =
                new FormBody.Builder()
                        .add("order_id", activeOrderId)
                        .add("transporter_id", transporterId)
                        .add("status", status)
                        .build();

        Request request =
                new Request.Builder()
                        .url(Constants.UPDATE_ORDER_STATUS_URL)
                        .post(body)
                        .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {

                runOnUiThread(() ->
                        Toast.makeText(
                                TransporterDashboardActivity.this,
                                "Status Updated",
                                Toast.LENGTH_SHORT
                        ).show());

                loadJobs();
            }
        });
    }

    // ===================== DRIVER STATE =====================

    private void updateDriverState(DriverState state) {

        currentState = state;

        switch (state) {

            case OFFLINE:
                tvStatus.setText("● OFFLINE");
                break;

            case ONLINE:
                tvStatus.setText("● ONLINE");
                break;

            case IDLE:
                tvStatus.setText("● WAITING FOR JOBS");
                break;

            case JOB_ASSIGNED:
                tvStatus.setText("● JOB ASSIGNED");
                break;

            case ENROUTE_PICKUP:
                tvStatus.setText("● GOING TO PICKUP");
                break;

            case AT_PICKUP:
                tvStatus.setText("● AT PICKUP");
                break;

            case OUT_FOR_DELIVERY:
                tvStatus.setText("● OUT FOR DELIVERY");
                break;

            case DELIVERED:
                tvStatus.setText("● DELIVERED");
                break;

            case COMPLETED:
                tvStatus.setText("● COMPLETED");
                break;
        }
    }
    private boolean canUpdate(String newStatus) {

        switch (currentState) {

            case ENROUTE_PICKUP:
                return newStatus.equals("arrived_at_pickup");

            case AT_PICKUP:
                return newStatus.equals("delivered");

            default:
                return false;
        }
    }

    private void updateButtons(String tripStatus) {

        if (activeOrderId == null) {

            btnStart.setEnabled(false);
            btnArrived.setEnabled(false);
            btnDelivered.setEnabled(false);
            return;
        }

        if ("started".equalsIgnoreCase(tripStatus)) {

            btnStart.setEnabled(false);

            btnArrived.setEnabled(true);

            btnDelivered.setEnabled(true);

        } else {

            btnStart.setEnabled(true);

            btnArrived.setEnabled(false);

            btnDelivered.setEnabled(false);
        }
    }

    private void startTrip() {

        if (activeOrderId == null) {

            Toast.makeText(
                    this,
                    "No active order",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        RequestBody body =
                new FormBody.Builder()
                        .add("order_id", activeOrderId)
                        .add("transporter_id", transporterId)
                        .build();

        Request request =
                new Request.Builder()
                        .url(Constants.START_TRIP_URL)
                        .post(body)
                        .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

                runOnUiThread(() ->
                        Toast.makeText(
                                TransporterDashboardActivity.this,
                                "Failed to start trip",
                                Toast.LENGTH_SHORT
                        ).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {

                try {

                    String json =
                            response.body().string();

                    JSONObject obj =
                            new JSONObject(json);

                    runOnUiThread(() -> {

                        if (obj.optBoolean("success")) {

                            Toast.makeText(
                                    TransporterDashboardActivity.this,
                                    "Trip Started",
                                    Toast.LENGTH_SHORT
                            ).show();

                            loadJobs();

                        } else {

                            Toast.makeText(
                                    TransporterDashboardActivity.this,
                                    obj.optString(
                                            "message",
                                            "Unable to start trip"
                                    ),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });

                } catch (Exception e) {

                    e.printStackTrace();
                }
            }
        });
    }


}