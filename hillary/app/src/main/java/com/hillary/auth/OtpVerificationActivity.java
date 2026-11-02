package com.hillary.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hillary.R;
import com.hillary.network.ApiClient;
import com.hillary.network.Constants;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OtpVerificationActivity extends AppCompatActivity {

    EditText etEmail, etOtp;
    Button btnVerify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        etEmail = findViewById(R.id.etEmail);
        etOtp = findViewById(R.id.etOtp);
        btnVerify = findViewById(R.id.btnVerify);

        btnVerify.setOnClickListener(v -> verifyOtp());
    }

    private void verifyOtp(){

        String email =
                etEmail.getText().toString().trim();

        String otp =
                etOtp.getText().toString().trim();

        if(email.isEmpty() || otp.isEmpty()){

            Toast.makeText(
                    this,
                    "All fields required",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        btnVerify.setEnabled(false);

        RequestBody body = new FormBody.Builder()
                .add("email", email)
                .add("otp", otp)
                .build();

        Request request = new Request.Builder()
                .url(Constants.VERIFY_OTP_URL)
                .post(body)
                .build();

        ApiClient.getClient(this)
                .newCall(request)
                .enqueue(new Callback() {

                    @Override
                    public void onFailure(Call call, IOException e) {

                        runOnUiThread(() -> {

                            btnVerify.setEnabled(true);

                            Toast.makeText(
                                    OtpVerificationActivity.this,
                                    "Connection failed",
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response)
                            throws IOException {

                        runOnUiThread(() ->
                                btnVerify.setEnabled(true));

                        String res =
                                response.body().string();

                        try {

                            JSONObject obj =
                                    new JSONObject(res);

                            String status =
                                    obj.getString("status");

                            String message =
                                    obj.getString("message");

                            runOnUiThread(() -> {

                                Toast.makeText(
                                        OtpVerificationActivity.this,
                                        message,
                                        Toast.LENGTH_LONG
                                ).show();

                                if(status.equals("success")){

                                    startActivity(
                                            new Intent(
                                                    OtpVerificationActivity.this,
                                                    LoginActivity.class
                                            )
                                    );

                                    finish();
                                }
                            });

                        } catch (Exception e){

                            runOnUiThread(() ->
                                    Toast.makeText(
                                            OtpVerificationActivity.this,
                                            "Invalid response",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                        }
                    }
                });
    }
}