package com.hillary.transporter.service;

import android.Manifest;
import android.app.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.*;
import com.hillary.network.ApiClient;
import com.hillary.utils.SessionManager;

import java.io.IOException;

import okhttp3.*;

public class TransporterLocationService extends Service {

    private static final String TAG = "TRANSPORTER_GPS";
    private static final String CHANNEL_ID = "transport_location_channel";

    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;

    private SessionManager session;
    private OkHttpClient client;

    private boolean isTracking = false;

    @Override
    public void onCreate() {
        super.onCreate();

        session = new SessionManager(this);
        client = ApiClient.getPublicClient();

        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        startForegroundServiceNotification();
        startLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null || intent.getAction() == null) {
            startLocationUpdates();
            return START_STICKY;
        }

        if ("START_TRACKING".equals(intent.getAction())) {
            startLocationUpdates();
        }

        return START_STICKY;
    }

    private void startForegroundServiceNotification() {

        createChannel();

        Notification notification =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Location Tracking Active")
                        .setContentText("Updating transporter location...")
                        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                        .setOngoing(true)
                        .build();

        startForeground(1001, notification);
    }

    private void createChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Transport Location Service",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void startLocationUpdates() {

        if (isTracking) return;

        LocationRequest request =
                new LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        10000 // 10 sec updates
                ).setMinUpdateIntervalMillis(5000)
                        .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {

                if (result == null) return;

                Location location = result.getLastLocation();
                if (location == null) return;

                sendToServer(location);
            }
        };

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {

            Log.e(TAG, "No location permission");
            return;
        }

        fusedClient.requestLocationUpdates(
                request,
                locationCallback,
                getMainLooper()
        );

        isTracking = true;

        Log.d(TAG, "Location tracking started");
    }

    private void sendToServer(Location location) {

        String transporterId = session.getTransporterId();
        if (transporterId == null) return;

        RequestBody body = new FormBody.Builder()
                .add("transporter_id", transporterId)
                .add("latitude", String.valueOf(location.getLatitude()))
                .add("longitude", String.valueOf(location.getLongitude()))
                .add("speed", String.valueOf(location.getSpeed()))
                .add("heading", String.valueOf(location.getBearing()))
                .build();

        Request request = new Request.Builder()
                .url("https://www.supplybridge.co.ke/fillers/api/transporter/update_location.php")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Upload failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response.close();
                Log.d(TAG, "Location updated");
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (fusedClient != null && locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {

        Intent restart = new Intent(getApplicationContext(), TransporterLocationService.class);
        restart.setAction("START_TRACKING");
        restart.setPackage(getPackageName());

        startService(restart);

        super.onTaskRemoved(rootIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}