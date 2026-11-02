package com.hillary.auth;

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

public class RegisterActivity extends AppCompatActivity {

    EditText etName, etEmail, etPhone, etPassword;
    Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);

        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> register());
    }

    private void register(){

        String name =
                etName.getText().toString().trim();

        String email =
                etEmail.getText().toString().trim();

        String phone =
                etPhone.getText().toString().trim();

        String password =
                etPassword.getText().toString().trim();

        if(name.isEmpty() ||
                email.isEmpty() ||
                phone.isEmpty() ||
                password.isEmpty()){

            Toast.makeText(
                    this,
                    "All fields are required",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        btnRegister.setEnabled(false);

        RequestBody body = new FormBody.Builder()
                .add("full_name", name)
                .add("email", email)
                .add("phone", phone)
                .add("password", password)
                .build();

        Request request = new Request.Builder()
                .url(Constants.REGISTER_URL)
                .post(body)
                .build();

        ApiClient.getClient(this)
                .newCall(request)
                .enqueue(new Callback() {

                    @Override
                    public void onFailure(Call call, IOException e) {

                        runOnUiThread(() -> {

                            btnRegister.setEnabled(true);

                            Toast.makeText(
                                    RegisterActivity.this,
                                    "Connection failed",
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response)
                            throws IOException {

                        runOnUiThread(() ->
                                btnRegister.setEnabled(true));

                        if(response.body() == null){

                            runOnUiThread(() ->
                                    Toast.makeText(
                                            RegisterActivity.this,
                                            "Empty response",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );

                            return;
                        }

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
                                        RegisterActivity.this,
                                        message,
                                        Toast.LENGTH_LONG
                                ).show();

                                if(status.equals("success")){

                                    etName.setText("");
                                    etEmail.setText("");
                                    etPhone.setText("");
                                    etPassword.setText("");
                                }
                            });

                        } catch (Exception e){

                            runOnUiThread(() ->
                                    Toast.makeText(
                                            RegisterActivity.this,
                                            "Invalid server response",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                        }
                    }
                });
    }
}