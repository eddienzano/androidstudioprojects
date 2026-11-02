package com.unitedselections.admin;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.unitedselections.R;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class EditUserActivity extends AppCompatActivity {

    EditText txtUsername, txtPassword;
    Spinner spinnerRole;
    Button btnUpdate;

    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user);

        txtUsername = findViewById(R.id.txtUsername);
        txtPassword = findViewById(R.id.txtPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnUpdate = findViewById(R.id.btnUpdate);

        // GET DATA
        userId = getIntent().getStringExtra("id");

        String username = getIntent().getStringExtra("username");
        String role = getIntent().getStringExtra("role");

        txtUsername.setText(username);

        // ROLE SPINNER
        String[] roles = {"ADMIN", "FIELD", "INTAKE"};

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        roles
                );

        spinnerRole.setAdapter(adapter);

        // SET CURRENT ROLE
        for (int i = 0; i < roles.length; i++) {

            if (roles[i].equals(role)) {
                spinnerRole.setSelection(i);
                break;
            }
        }

        btnUpdate.setOnClickListener(v -> updateUser());
    }

    private void updateUser() {

        new Thread(() -> {

            HttpURLConnection conn = null;

            try {

                URL url = new URL(
                        "https://unitedselections.supplybridge.co.ke/api/update_user.php"
                );

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                String postData =
                        "id=" + URLEncoder.encode(userId, "UTF-8") +
                                "&username=" + URLEncoder.encode(txtUsername.getText().toString().trim(), "UTF-8") +
                                "&password=" + URLEncoder.encode(txtPassword.getText().toString().trim(), "UTF-8") +
                                "&role=" + URLEncoder.encode(spinnerRole.getSelectedItem().toString(), "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                runOnUiThread(() -> {

                    if (responseCode == 200) {

                        Toast.makeText(this,
                                "User updated successfully",
                                Toast.LENGTH_SHORT).show();

                        finish();

                    } else {

                        Toast.makeText(this,
                                "Update failed. Code: " + responseCode,
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );

            } finally {

                if (conn != null) conn.disconnect();
            }

        }).start();
    }
}