package com.aaagrowers.theumribungoma;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MediaActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);
        setupDrawer();
    }
}
