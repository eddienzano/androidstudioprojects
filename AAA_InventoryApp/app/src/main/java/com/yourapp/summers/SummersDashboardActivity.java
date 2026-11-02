package com.yourapp.summers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.yourapp.R;

public class SummersDashboardActivity
        extends AppCompatActivity {

    LinearLayout cardIntake;
    LinearLayout cardEdit;

    LinearLayout cardPackhouseReports;

    LinearLayout cardReports;

    TextView txtWelcome;

    String username;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summers_dashboard);

        cardIntake = findViewById(R.id.cardIntake);
        cardEdit = findViewById(R.id.cardEdit);

        cardReports = findViewById(R.id.cardReports);

        cardPackhouseReports = findViewById(R.id.cardPackhouseReports);

        txtWelcome = findViewById(R.id.txtWelcome);

        username = getIntent()
                .getStringExtra("username");

        userId = getIntent()
                .getStringExtra("user_id");

        txtWelcome.setText(
                "Welcome " + username
        );

        cardIntake.setOnClickListener(v -> {

            Intent i = new Intent(
                    SummersDashboardActivity.this,
                    SummersIntakeActivity.class
            );

            i.putExtra("username", username);
            i.putExtra("user_id", userId);

            startActivity(i);
        });

        cardEdit.setOnClickListener(v -> {

            Intent i = new Intent(
                    SummersDashboardActivity.this,
                    EditVarietyActivity.class
            );

            i.putExtra("username", username);
            i.putExtra("user_id", userId);

            startActivity(i);
        });

        cardReports.setOnClickListener(v -> {

            Intent i = new Intent(
                    SummersDashboardActivity.this,
                    SummersReportsActivity.class
            );

            i.putExtra("username", username);
            i.putExtra("user_id", userId);

            startActivity(i);
        });

        cardPackhouseReports.setOnClickListener(v -> {

            Intent i = new Intent(
                    SummersDashboardActivity.this,
                    PackhouseReportsActivity.class
            );

            i.putExtra("username", username);
            i.putExtra("user_id", userId);

            startActivity(i);
        });



    }
}