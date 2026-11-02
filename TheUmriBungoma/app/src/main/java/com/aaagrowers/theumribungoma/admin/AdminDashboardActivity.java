package com.aaagrowers.theumribungoma.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.aaagrowers.theumribungoma.R;

public class AdminDashboardActivity extends AppCompatActivity {

    Button addFixture, addPlayer, addNews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // 🔥 LINK UI
        addFixture = findViewById(R.id.addFixture);
        addPlayer = findViewById(R.id.addPlayer);
        addNews = findViewById(R.id.addNews);

        // ⚽ ADD FIXTURE
        addFixture.setOnClickListener(v -> {
            startActivity(new Intent(this, AddFixtureActivity.class));
        });

        // 🧑 ADD PLAYER
        addPlayer.setOnClickListener(v -> {
            startActivity(new Intent(this, AddPlayerActivity.class));
        });

        // 📰 ADD NEWS
        addNews.setOnClickListener(v -> {
            startActivity(new Intent(this, AddNewsActivity.class));
        });
    }
}