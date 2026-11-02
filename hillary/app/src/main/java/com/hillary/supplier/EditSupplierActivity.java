package com.hillary.supplier;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.hillary.R;
import com.hillary.network.ApiClient;
import com.hillary.network.Constants;
import com.hillary.utils.SessionManager;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditSupplierActivity extends AppCompatActivity {

    EditText etCompany, etDescription,
            etPhone, etEmail, etLocation;

    MaterialButton btnUpdate;

    ImageView btnBack;

    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_supplier);

        session = new SessionManager(this);

        initViews();

        loadProfile();

        btnBack.setOnClickListener(v -> finish());

        btnUpdate.setOnClickListener(v -> updateProfile());
    }

    private void initViews() {

        btnBack = findViewById(R.id.btnBack);

        etCompany = findViewById(R.id.etCompany);
        etDescription = findViewById(R.id.etDescription);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etLocation = findViewById(R.id.etLocation);

        btnUpdate = findViewById(R.id.btnUpdate);
    }

    private void loadProfile() {

        Request request = new Request.Builder()
                .url(Constants.SUPPLIER_ME_URL)
                .addHeader("Authorization",
                        "Bearer " + session.getToken())
                .build();

        ApiClient.getClient(this)
                .newCall(request)
                .enqueue(new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() ->
                                Toast.makeText(
                                        EditSupplierActivity.this,
                                        "Failed to load profile",
                                        Toast.LENGTH_SHORT
                                ).show());
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {

                        if (response.body() == null) return;

                        try {

                            String res = response.body().string();

                            JSONObject obj =
                                    new JSONObject(res);

                            JSONObject data =
                                    obj.getJSONObject("data");

                            runOnUiThread(() -> {

                                etCompany.setText(
                                        data.optString("company_name"));

                                etDescription.setText(
                                        data.optString("description"));

                                etPhone.setText(
                                        data.optString("phone"));

                                etEmail.setText(
                                        data.optString("email"));

                                etLocation.setText(
                                        data.optString("location"));
                            });

                        } catch (Exception e) {

                            runOnUiThread(() ->
                                    Toast.makeText(
                                            EditSupplierActivity.this,
                                            "Invalid response",
                                            Toast.LENGTH_SHORT
                                    ).show());
                        }
                    }
                });
    }

    private void updateProfile() {

        RequestBody body = new FormBody.Builder()

                .add("company_name",
                        etCompany.getText().toString().trim())

                .add("description",
                        etDescription.getText().toString().trim())

                .add("phone",
                        etPhone.getText().toString().trim())

                .add("email",
                        etEmail.getText().toString().trim())

                .add("location",
                        etLocation.getText().toString().trim())

                .build();

        Request request = new Request.Builder()
                .url(Constants.UPDATE_SUPPLIER_URL)
                .addHeader("Authorization",
                        "Bearer " + session.getToken())
                .post(body)
                .build();

        ApiClient.getClient(this)
                .newCall(request)
                .enqueue(new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() ->
                                Toast.makeText(
                                        EditSupplierActivity.this,
                                        "Update failed",
                                        Toast.LENGTH_SHORT
                                ).show());
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {

                        if (response.body() == null) return;

                        String res = response.body().string();

                        runOnUiThread(() -> {

                            try {

                                JSONObject obj = new JSONObject(res);

                                String status = obj.optString("status");
                                String message = obj.optString("message");

                                Toast.makeText(
                                        EditSupplierActivity.this,
                                        message,
                                        Toast.LENGTH_SHORT
                                ).show();

                                if (status.equalsIgnoreCase("success")) {

                                    finish();
                                }

                            } catch (Exception e) {

                                Toast.makeText(
                                        EditSupplierActivity.this,
                                        "Invalid server response",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        });
                    }
                });
    }
}