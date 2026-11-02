package com.unitedselections.admin;

import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.unitedselections.R;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class CreateUserActivity extends AppCompatActivity {

    EditText txtUsername, txtPassword;
    Spinner spinnerRole;
    Button btnCreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_user);

        txtUsername = findViewById(R.id.txtUsername);
        txtPassword = findViewById(R.id.txtPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnCreate = findViewById(R.id.btnCreate);

        String[] roles = {"ADMIN","FIELD","INTAKE"};

        spinnerRole.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        roles
                )
        );

        btnCreate.setOnClickListener(v -> createUser());
    }

    private void createUser(){

        new Thread(() -> {

            try {

                URL url = new URL(
                        "https://unitedselections.supplybridge.co.ke/api/create_user.php"
                );

                HttpURLConnection conn =
                        (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String post =
                        "username=" + URLEncoder.encode(txtUsername.getText().toString(),"UTF-8") +
                                "&password=" + URLEncoder.encode(txtPassword.getText().toString(),"UTF-8") +
                                "&role=" + URLEncoder.encode(spinnerRole.getSelectedItem().toString(),"UTF-8");

                OutputStream os = conn.getOutputStream();

                os.write(post.getBytes());

                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                runOnUiThread(() -> {

                    if(responseCode == 200){

                        Toast.makeText(
                                this,
                                "User Created Successfully",
                                Toast.LENGTH_LONG
                        ).show();

                        txtUsername.setText("");
                        txtPassword.setText("");

                    } else {

                        Toast.makeText(
                                this,
                                "Failed To Create User",
                                Toast.LENGTH_LONG
                        ).show();
                    }

                });

            } catch (Exception e){

                e.printStackTrace();

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show());
            }

        }).start();
    }
}