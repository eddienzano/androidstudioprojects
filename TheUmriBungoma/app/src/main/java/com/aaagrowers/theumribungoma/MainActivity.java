package com.aaagrowers.theumribungoma;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    ImageView logo;
    TextView title, tagline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logo = findViewById(R.id.logo);
        title = findViewById(R.id.title);
        tagline = findViewById(R.id.tagline);

        // 🎬 Load animations
        Animation zoom = AnimationUtils.loadAnimation(this, R.anim.logo_zoom);
        Animation fade = AnimationUtils.loadAnimation(this, R.anim.fade_in_text);
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.glow_pulse);

        // 🔥 Start animations
        logo.startAnimation(zoom);
        logo.startAnimation(pulse);

        title.startAnimation(fade);
        tagline.startAnimation(fade);

        // ⏱️ Delay then go to Home
        new Handler().postDelayed(() -> {
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
            finish();
        }, 3000);
    }
}