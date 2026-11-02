package com.hillary.transporter.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.hillary.utils.SessionManager;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null) return;

        String action = intent.getAction();

        if (action == null) return;

        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            return;
        }

        SessionManager session = new SessionManager(context);

        String transporterId = session.getTransporterId();

        if (transporterId == null || transporterId.trim().isEmpty()) {
            Log.d(TAG, "No logged-in transporter, service not started");
            return;
        }

        Intent serviceIntent =
                new Intent(context, TransporterLocationService.class);

        // 🔥 Important: explicit action for safe restart handling
        serviceIntent.setAction("START_TRACKING");

        serviceIntent.putExtra("from_boot", true);

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            Log.d(TAG, "Location service started from boot");

        } catch (Exception e) {
            Log.e(TAG, "Failed to start service from boot", e);
        }
    }
}