package com.hillary.products;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hillary.R;
import com.hillary.helpers.ImagePickerHelper;
import com.hillary.network.ApiClient;
import com.hillary.network.Constants;
import com.hillary.network.MultipartUploader;
import com.hillary.utils.SessionManager;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CreateProductActivity extends AppCompatActivity {

    private static final String TAG = "CREATE_PRODUCT";

    EditText etName, etDescription, etCategory, etUnit, etPrice, etStock, etSku;
    Button btnCreate, btnPickImage;

    Uri selectedImageUri = null;
    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_product);

        session = new SessionManager(this);

        initViews();

        btnPickImage.setOnClickListener(v -> {
            Log.d(TAG, "Image picker opened");
            ImagePickerHelper.pickImage(this);
        });

        btnCreate.setOnClickListener(v -> {
            Log.d(TAG, "Create product clicked");
            createProduct();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        selectedImageUri = ImagePickerHelper.getImageResult(requestCode, resultCode, data);

        if (selectedImageUri != null) {
            Log.d(TAG, "Image selected: " + selectedImageUri.toString());
            Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
        } else {
            Log.w(TAG, "No image selected");
        }
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        etCategory = findViewById(R.id.etCategory);
        etUnit = findViewById(R.id.etUnit);
        etPrice = findViewById(R.id.etPrice);
        etStock = findViewById(R.id.etStock);
        etSku = findViewById(R.id.etSku);

        btnCreate = findViewById(R.id.btnCreate);
        btnPickImage = findViewById(R.id.btnPickImage);
    }

    private void createProduct() {

        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String unit = etUnit.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String stock = etStock.getText().toString().trim();
        String sku = etSku.getText().toString().trim();

        Log.d(TAG, "Payload => " +
                "name=" + name +
                ", price=" + price +
                ", stock=" + stock);

        btnCreate.setEnabled(false);

        RequestBody body = new FormBody.Builder()
                .add("product_name", name)
                .add("description", description)
                .add("category", category)
                .add("unit", unit)
                .add("price", price)
                .add("stock", stock)
                .add("sku", sku)
                .build();

        Request request = new Request.Builder()
                .url(Constants.CREATE_PRODUCT_URL)
                .addHeader("Authorization", "Bearer " + session.getToken())
                .post(body)
                .build();

        ApiClient.getClient(this).newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "CREATE PRODUCT FAILED", e);

                runOnUiThread(() -> {
                    btnCreate.setEnabled(true);
                    Toast.makeText(CreateProductActivity.this,
                            "Connection failed",
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String res = response.body() != null ? response.body().string() : "null";

                Log.d(TAG, "CREATE RESPONSE => " + res);

                try {
                    JSONObject obj = new JSONObject(res);

                    runOnUiThread(() -> {

                        btnCreate.setEnabled(true);

                        String status = obj.optString("status");
                        String message = obj.optString("message");

                        Toast.makeText(CreateProductActivity.this,
                                message,
                                Toast.LENGTH_SHORT).show();

                        if ("success".equals(status)) {

                            JSONObject data = obj.optJSONObject("data");
                            String productId = (data != null) ? data.optString("product_id") : "";

                            Log.d("CREATE_PRODUCT", "Product created ID => " + productId);

                            if (selectedImageUri != null) {

                                Log.d(TAG, "Starting image upload...");

                                MultipartUploader.uploadProductImage(
                                        CreateProductActivity.this,
                                        session.getToken(),
                                        productId,
                                        selectedImageUri,
                                        new Callback() {

                                            @Override
                                            public void onFailure(Call call, IOException e) {
                                                Log.e(TAG, "IMAGE UPLOAD FAILED", e);
                                            }

                                            @Override
                                            public void onResponse(Call call, Response response) throws IOException {
                                                String imgRes = response.body() != null ? response.body().string() : "null";
                                                Log.d(TAG, "IMAGE UPLOAD RESPONSE => " + imgRes);
                                            }
                                        }
                                );

                            } else {
                                Log.w(TAG, "No image selected");
                            }

                            finish();
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "PARSE ERROR", e);
                }
            }
        });
    }
}