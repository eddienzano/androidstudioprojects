package com.aaagrowers.theumribungoma;

import android.content.Intent;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class BaseActivity extends AppCompatActivity {

    protected void setupDrawer() {

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);
        ImageView menuBtn = findViewById(R.id.menuBtn);

        if (menuBtn != null) {
            menuBtn.setOnClickListener(v -> {
                drawerLayout.openDrawer(GravityCompat.START);
            });
        }

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {

                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class));
                }

                else if (id == R.id.nav_fixtures) {
                    startActivity(new Intent(this, FixturesActivity.class));
                }

                else if (id == R.id.nav_squad) {
                    // already here
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
}