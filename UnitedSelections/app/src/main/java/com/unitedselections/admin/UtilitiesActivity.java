package com.unitedselections.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.unitedselections.R;

public class UtilitiesActivity extends AppCompatActivity {

    LinearLayout cardAddVariety, cardEditVariety;
    LinearLayout cardAddGreenhouse, cardEditGreenhouse;

    // 🔥 NEW USER MANAGEMENT
    LinearLayout cardAddUser, cardManageUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utilities);

        // EXISTING
        cardAddVariety = findViewById(R.id.cardAddVariety);
        cardEditVariety = findViewById(R.id.cardEditVariety);

        cardAddGreenhouse = findViewById(R.id.cardAddGreenhouse);
        cardEditGreenhouse = findViewById(R.id.cardEditGreenhouse);

        // 🔥 NEW
        cardAddUser = findViewById(R.id.cardAddUser);
        cardManageUsers = findViewById(R.id.cardManageUsers);

        // =========================================
        // VARIETY
        // =========================================

        cardAddVariety.setOnClickListener(v ->
                startActivity(new Intent(this, AddVarietyActivity.class))
        );

        cardEditVariety.setOnClickListener(v ->
                startActivity(new Intent(this, EditVarietyActivity.class))
        );

        // =========================================
        // GREENHOUSE
        // =========================================

        cardAddGreenhouse.setOnClickListener(v ->
                startActivity(new Intent(this, AddGreenhouseActivity.class))
        );

        cardEditGreenhouse.setOnClickListener(v ->
                startActivity(new Intent(this, EditGreenhouseActivity.class))
        );

        // =========================================
        // USER MANAGEMENT
        // =========================================

        cardAddUser.setOnClickListener(v ->
                startActivity(new Intent(this, CreateUserActivity.class))
        );

        cardManageUsers.setOnClickListener(v ->
                startActivity(new Intent(this, UsersActivity.class))
        );
    }
}