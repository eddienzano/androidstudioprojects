package com.hillary.supplier;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hillary.R;
import com.hillary.network.ApiClient;
import com.hillary.network.Constants;
import com.hillary.utils.SessionManager;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class SupplierProfileActivity extends AppCompatActivity {

    SessionManager session;

    TextView tvCompany, tvContact,
            tvEmail, tvPhone,
            tvAddress, tvStatus;

    ImageView btnEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supplier_profile);

        session = new SessionManager(this);

        initViews();

        btnEdit.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                this,
                                EditSupplierActivity.class
                        )
                )
        );

        loadProfile();
    }

    private void initViews() {

        tvCompany = findViewById(R.id.tvCompany);
        tvContact = findViewById(R.id.tvContact);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvAddress = findViewById(R.id.tvAddress);
        tvStatus = findViewById(R.id.tvStatus);

        btnEdit = findViewById(R.id.btnEdit);
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
                            Call call,
                            IOException e
                    ) {
                        runOnUiThread(() ->
                                Toast.makeText(
                                        SupplierProfileActivity.this,
                                        "Failed to load profile",
                                        Toast.LENGTH_SHORT
                                ).show());
                    }

                    @Override
                    public void onResponse(
                            Call call,
                            Response response
                    ) throws IOException {

                        if (response.body() == null)
                            return;

                        try {

                            String res =
                                    response.body().string();

                            JSONObject obj =
                                    new JSONObject(res);

                            JSONObject data =
                                    obj.getJSONObject("data");

                            runOnUiThread(() -> {

                                tvCompany.setText(
                                        data.optString("company_name"));

                                tvContact.setText(
                                        data.optString("description"));

                                tvEmail.setText(
                                        data.optString("email"));

                                tvPhone.setText(
                                        data.optString("phone"));

                                tvAddress.setText(
                                        data.optString("location"));

                                String status =
                                        data.optString("status")
                                                .equals("approved")
                                                ? "Verified"
                                                : "Pending Verification";

                                tvStatus.setText(status);
                            });

                        } catch (Exception e) {

                            runOnUiThread(() ->
                                    Toast.makeText(
                                            SupplierProfileActivity.this,
                                            "Invalid response",
                                            Toast.LENGTH_SHORT
                                    ).show());
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadProfile();
    }
}