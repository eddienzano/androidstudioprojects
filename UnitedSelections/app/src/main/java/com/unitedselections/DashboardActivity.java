package com.unitedselections;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.unitedselections.admin.*;
import com.unitedselections.field.FieldDiscardActivity;
import com.unitedselections.field.FieldScanActivity;
import com.unitedselections.intake.IntakeScanActivity;
import com.unitedselections.intake.ScanOutActivity;

public class DashboardActivity extends AppCompatActivity {

    LinearLayout cardEditQR, cardField, cardIntake, cardReports, cardVariance, cardScanOut, cardUtilities, cardDownloads, cardfieldDiscard;

    String role, username;

    TextView txtUser, txtGreeting, txtRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        role = getIntent().getStringExtra("role");
        username = getIntent().getStringExtra("username");

        if (role == null) role = "FIELD";

        txtUser = findViewById(R.id.txtUser);
        txtGreeting = findViewById(R.id.txtGreeting);
        txtRole = findViewById(R.id.txtRole);

        txtUser.setText(username != null ? username : "User");
        txtRole.setText(role);

        txtGreeting.setText(getGreeting());

        cardField = findViewById(R.id.cardField);
        cardIntake = findViewById(R.id.cardIntake);
        cardReports = findViewById(R.id.cardReports);
        cardVariance = findViewById(R.id.cardVariance);
        cardScanOut = findViewById(R.id.cardScanOut);
        cardUtilities = findViewById(R.id.cardUtilities);
        cardDownloads = findViewById(R.id.cardDownloads);
        cardfieldDiscard =findViewById(R.id.cardfieldDiscard);
        cardEditQR =findViewById(R.id.cardEditQR);

        // animations
        animate(cardField);
        animate(cardIntake);
        animate(cardReports);
        animate(cardVariance);
        animate(cardScanOut);
        animate(cardUtilities);
        animate(cardDownloads);
        animate(cardfieldDiscard);
        animate(cardEditQR);

        applyRolePermissions();

        bindClicks();
    }

    private String getGreeting() {
        return "Welcome back 👋";
    }

    private void applyRolePermissions() {

        if ("FIELD".equals(role)) {

            cardIntake.setVisibility(View.GONE);
            cardVariance.setVisibility(View.GONE);
            cardReports.setVisibility(View.GONE);
            cardScanOut.setVisibility(View.GONE);
            cardUtilities.setVisibility(View.GONE);
            cardEditQR.setVisibility(View.GONE);

        }

        if ("INTAKE".equals(role)) {

            cardField.setVisibility(View.GONE);
            cardVariance.setVisibility(View.GONE);
            cardReports.setVisibility(View.GONE);
            cardUtilities.setVisibility(View.GONE);
            cardUtilities.setVisibility(View.GONE);
            cardEditQR.setVisibility(View.GONE);
        }
    }

    private void bindClicks() {

        cardField.setOnClickListener(v ->
                startActivity(new Intent(this, FieldScanActivity.class)));

        cardIntake.setOnClickListener(v ->
                startActivity(new Intent(this, IntakeScanActivity.class)));

        cardReports.setOnClickListener(v ->
                startActivity(new Intent(this, ReportsActivity.class)));

        cardVariance.setOnClickListener(v ->
                startActivity(new Intent(this, VarianceActivity.class)));

        cardScanOut.setOnClickListener(v ->
                startActivity(new Intent(this, ScanOutActivity.class)));

        cardUtilities.setOnClickListener(v ->
                startActivity(new Intent(this, UtilitiesActivity.class)));

        cardDownloads.setOnClickListener(v ->
                startActivity(new Intent(this, ReportDownloadsActivity.class)));
        cardfieldDiscard.setOnClickListener(v ->
                startActivity((new Intent(this, FieldDiscardActivity.class))));
        cardEditQR.setOnClickListener(v ->
                startActivity((new Intent(this, AdminAdjustmentActivity.class))));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void animate(View v) {

        v.setOnTouchListener((view, event) -> {

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                view.animate().scaleX(0.97f).scaleY(0.97f)
                        .setDuration(120).start();
            } else {
                view.animate().scaleX(1f).scaleY(1f)
                        .setDuration(120).start();
            }
            return false;
        });
    }
}