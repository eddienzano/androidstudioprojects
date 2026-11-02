package com.aaagrowers.theumribungoma;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class ClubInfoActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_club_info);
        setupDrawer();
    }
}
