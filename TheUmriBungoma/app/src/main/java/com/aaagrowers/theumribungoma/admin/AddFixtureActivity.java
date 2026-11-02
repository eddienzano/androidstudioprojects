package com.aaagrowers.theumribungoma.admin;



import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aaagrowers.theumribungoma.R;
import com.android.volley.Request;
import com.android.volley.toolbox.Volley;

import java.io.InputStream;

public class AddFixtureActivity extends AppCompatActivity {

    ImageView previewImage;
    Button selectImageBtn, uploadBtn;

    Uri imageUri;

    private static final int PICK_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_fixture);

        previewImage = findViewById(R.id.previewImage);
        selectImageBtn = findViewById(R.id.selectImageBtn);
        uploadBtn = findViewById(R.id.uploadBtn);

        // 📸 SELECT IMAGE
        selectImageBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE);
        });

        // 🚀 UPLOAD IMAGE
        uploadBtn.setOnClickListener(v -> {
            if (imageUri != null) {
                uploadImage();
            } else {
                Toast.makeText(this, "Select image first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            previewImage.setImageURI(imageUri);
        }
    }

    // 📡 UPLOAD TO SERVER
    private void uploadImage() {

        String url = "https://www.aaagrowers.co.ke/inventory/img/images/upload_fixture.php";

        try {
            InputStream iStream = getContentResolver().openInputStream(imageUri);
            byte[] imageBytes = new byte[iStream.available()];
            iStream.read(imageBytes);
            iStream.close();

            String encodedImage = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);

            com.android.volley.toolbox.StringRequest request =
                    new com.android.volley.toolbox.StringRequest(Request.Method.POST, url,
                            response -> {
                                Toast.makeText(this, "Uploaded Successfully", Toast.LENGTH_SHORT).show();
                            },
                            error -> {
                                Toast.makeText(this, "Upload Failed", Toast.LENGTH_SHORT).show();
                            }) {

                        @Override
                        protected java.util.Map<String, String> getParams() {
                            java.util.Map<String, String> params = new java.util.HashMap<>();
                            params.put("image", encodedImage);
                            return params;
                        }
                    };

            Volley.newRequestQueue(this).add(request);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}