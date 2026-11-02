package com.aaagrowers.theumribungoma;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class HomeActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    ImageView menuBtn;
    NavigationView navigationView;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        drawerLayout = findViewById(R.id.drawerLayout);
        menuBtn = findViewById(R.id.menuBtn);
        navigationView = findViewById(R.id.navigationView);

        // Open drawer
        menuBtn.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // Handle menu clicks
        navigationView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // stay here
            }

            else if (id == R.id.nav_fixtures) {
                startActivity(new Intent(this, FixturesActivity.class));
            }

            else if (id == R.id.nav_squad) {
                startActivity(new Intent(this, SquadActivity.class));
            }

            else if (id == R.id.nav_league) {
                startActivity(new Intent(this, LeagueActivity.class));
            }

            else if (id == R.id.nav_news) {
                startActivity(new Intent(this, NewsActivity.class));
            }

            else if (id == R.id.nav_media) {
                startActivity(new Intent(this, MediaActivity.class));
            }

            else if (id == R.id.nav_fanzone) {
                startActivity(new Intent(this, FanZoneActivity.class));
            }

            else if (id == R.id.nav_club) {
                startActivity(new Intent(this, ClubInfoActivity.class));
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }
}