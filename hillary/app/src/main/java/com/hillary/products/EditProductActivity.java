package com.hillary.products;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.hillary.R;
import com.hillary.network.ApiClient;
import com.hillary.network.Constants;
import com.hillary.utils.SessionManager;

import java.io.*;

import okhttp3.*;

public class EditProductActivity extends AppCompatActivity {

    ImageView imgProduct, btnBack;
    LinearLayout btnChangeImage;

    EditText etName, etDescription, etPrice, etStock, etSku;
    Spinner spCategory, spStatus;
    MaterialButton btnUpdate;

    Uri imageUri;
    int productId;

    SessionManager session;

    private static final String BASE_IMAGE_URL =
            "https://www.aaagrowers.co.ke/inventory/fillers/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_product);

        session = new SessionManager(this);

        initViews();
        setupSpinners();
        loadData();

        btnBack.setOnClickListener(v -> finish());
        btnChangeImage.setOnClickListener(v -> pickImage());
        btnUpdate.setOnClickListener(v -> updateProduct());
    }

    private void initViews() {
        imgProduct = findViewById(R.id.imgProduct);
        btnChangeImage = findViewById(R.id.btnChangeImage);
        btnBack = findViewById(R.id.btnBack);

        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        etStock = findViewById(R.id.etStock);
        etSku = findViewById(R.id.etSku);

        spCategory = findViewById(R.id.spCategory);
        spStatus = findViewById(R.id.spStatus);

        btnUpdate = findViewById(R.id.btnUpdate);
    }

    private void setupSpinners() {
        String[] categories = {"Electronics", "Clothes", "Phones", "Food", "Accessories"};
        spCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, categories));

        String[] statuses = {"active", "inactive", "out_of_stock"};
        spStatus.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, statuses));
    }

    private void loadData() {

        Intent i = getIntent();

        productId = i.getIntExtra("product_id", 0);

        etName.setText(i.getStringExtra("name"));
        etDescription.setText(i.getStringExtra("description"));
        etPrice.setText(String.valueOf(i.getDoubleExtra("price", 0)));
        etStock.setText(String.valueOf(i.getIntExtra("stock", 0)));
        etSku.setText(i.getStringExtra("sku"));

        String image = i.getStringExtra("image");

        if (image == null || image.equals("null") || image.isEmpty()) {
            imgProduct.setImageResource(R.drawable.ic_launcher_background);
        } else {

            String url = image.startsWith("http")
                    ? image
                    : BASE_IMAGE_URL + image;

            Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(imgProduct);
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.getData();
            imgProduct.setImageURI(imageUri);
        }
    }

    // SAFE STREAM READER
    private byte[] getBytes(Uri uri) throws IOException {
        InputStream is = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        byte[] data = new byte[4096];
        int n;

        while ((n = is.read(data)) != -1) {
            buffer.write(data, 0, n);
        }

        return buffer.toByteArray();
    }

    private void updateProduct() {

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        builder.addFormDataPart("product_id", String.valueOf(productId));
        builder.addFormDataPart("product_name", etName.getText().toString());
        builder.addFormDataPart("description", etDescription.getText().toString());
        builder.addFormDataPart("price", etPrice.getText().toString());
        builder.addFormDataPart("stock", etStock.getText().toString());
        builder.addFormDataPart("sku", etSku.getText().toString());
        builder.addFormDataPart("category", spCategory.getSelectedItem().toString());
        builder.addFormDataPart("status", spStatus.getSelectedItem().toString());

        if (imageUri != null) {
            try {

                byte[] imageBytes = getBytes(imageUri);

                RequestBody fileBody =
                        RequestBody.create(imageBytes, MediaType.parse("image/*"));

                builder.addFormDataPart(
                        "thumbnail",
                        "product.jpg",
                        fileBody
                );

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Request request = new Request.Builder()
                .url(Constants.UPDATE_PRODUCT_URL)
                .addHeader("Authorization", "Bearer " + session.getToken())
                .post(builder.build())
                .build();

        ApiClient.getClient(this)
                .newCall(request)
                .enqueue(new Callback() {

                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() ->
                                Toast.makeText(EditProductActivity.this,
                                        "Update failed", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        runOnUiThread(() -> {
                            Toast.makeText(EditProductActivity.this,
                                    "Updated successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                });
    }
}