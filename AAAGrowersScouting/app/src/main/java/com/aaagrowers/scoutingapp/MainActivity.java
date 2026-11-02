package com.aaagrowers.scoutingapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.aaagrowers.scoutingapp.auth.LoginActivity;
import com.aaagrowers.scoutingapp.data.database.AppDatabase;

public class MainActivity extends AppCompatActivity {

    private AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        /*
         * ----------------------------------------------------------
         * INITIALIZE LOCAL DATABASE
         * ----------------------------------------------------------
         *
         * This ensures Room opens/creates the local database
         * before the application proceeds.
         */

        database =
                AppDatabase.getInstance(
                        getApplicationContext()
                );


        /*
         * ----------------------------------------------------------
         * TEMPORARY AUTHENTICATION ROUTING
         * ----------------------------------------------------------
         *
         * Authentication/session checking will be added here
         * in Milestone 2.2.
         *
         * For now every application launch goes to LoginActivity.
         */

        Intent intent =
                new Intent(
                        MainActivity.this,
                        LoginActivity.class
                );

        startActivity(intent);


        /*
         * Remove MainActivity from the back stack so that pressing
         * Back from Login does not return to this temporary screen.
         */

        finish();
    }
}