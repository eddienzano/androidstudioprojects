package com.hillary;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.hillary.auth.LoginActivity;
import com.hillary.Dashboard.DashboardActivity;
import com.hillary.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);

        if(session.isLoggedIn()){

            startActivity(new Intent(this, DashboardActivity.class));

        } else {

            startActivity(new Intent(this, LoginActivity.class));
        }

        finish();
    }
}