package com.aaagrowers.theumribungoma.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aaagrowers.theumribungoma.R;

public class AdminLoginActivity extends AppCompatActivity {

    EditText email, password;
    Button loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);

        loginBtn.setOnClickListener(v -> {

            String user = email.getText().toString();
            String pass = password.getText().toString();

            if (user.equals("admin") && pass.equals("1234")) {
                startActivity(new Intent(this, AdminDashboardActivity.class));
            } else {
                Toast.makeText(this, "Invalid login", Toast.LENGTH_SHORT).show();
            }
        });
    }
}