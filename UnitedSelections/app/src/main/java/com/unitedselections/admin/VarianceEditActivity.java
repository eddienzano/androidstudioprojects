package com.unitedselections.admin;

import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.unitedselections.R;

import java.io.IOException;

import okhttp3.*;

public class VarianceEditActivity extends AppCompatActivity {

    EditText etFieldQty, etIntakeQty;
    TextView txtQr, txtFieldVar, txtIntakeVar;
    Button btnSave;

    String id;

    OkHttpClient client = new OkHttpClient();

    String API = "https://unitedselections.supplybridge.co.ke/api/update_variance.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_variance_edit);

        txtQr = findViewById(R.id.txtQr);
        txtFieldVar = findViewById(R.id.txtFieldVar);
        txtIntakeVar = findViewById(R.id.txtIntakeVar);

        etFieldQty = findViewById(R.id.etFieldQty);
        etIntakeQty = findViewById(R.id.etIntakeQty);

        btnSave = findViewById(R.id.btnSave);

        // DATA FROM INTENT
        id = getIntent().getStringExtra("id");

        txtQr.setText(getIntent().getStringExtra("qr"));
        txtFieldVar.setText(getIntent().getStringExtra("field_variety"));
        txtIntakeVar.setText(getIntent().getStringExtra("intake_variety"));

        etFieldQty.setText(String.valueOf(getIntent().getIntExtra("field_qty", 0)));
        etIntakeQty.setText(String.valueOf(getIntent().getIntExtra("intake_qty", 0)));

        btnSave.setOnClickListener(v -> save());
    }

    private void save() {

        String fieldQty = etFieldQty.getText().toString().trim();
        String intakeQty = etIntakeQty.getText().toString().trim();

        RequestBody body = new FormBody.Builder()
                .add("id", id)
                .add("field_qty", fieldQty)
                .add("intake_qty", intakeQty)
                .build();

        Request request = new Request.Builder()
                .url(API)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(VarianceEditActivity.this,
                                "Network error", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String res = response.body().string();

                runOnUiThread(() ->
                        Toast.makeText(VarianceEditActivity.this,
                                res, Toast.LENGTH_LONG).show()
                );
            }
        });
    }
}