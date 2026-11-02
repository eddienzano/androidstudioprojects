package com.hillary;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.hillary.auth.LoginActivity;
import com.hillary.customer.MarketplaceActivity;
import com.hillary.transporter.TransporterLoginActivity;

public class LandingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        findViewById(R.id.btnMarketplace).setOnClickListener(v -> {
            startActivity(new Intent(this, MarketplaceActivity.class));
        });

        findViewById(R.id.btnSupplier).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });

        findViewById(R.id.btnTransporter).setOnClickListener(v -> {
            startActivity(new Intent(this, TransporterLoginActivity.class));
        });
    }
}