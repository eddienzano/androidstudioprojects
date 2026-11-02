package com.hillary.products;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.hillary.R;

public class ProductDetailsActivity extends AppCompatActivity {

    ImageView imgProduct, btnBack;
    TextView tvName, tvDescription, tvPrice, tvStock;
    MaterialButton btnEdit;

    int productId;
    String name, description, image;
    double price;
    int stock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        initViews();
        loadIntentData();

        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {

            Intent intent = new Intent(this, EditProductActivity.class);

            intent.putExtra("product_id", productId);
            intent.putExtra("name", name);
            intent.putExtra("description", description);
            intent.putExtra("price", price);
            intent.putExtra("stock", stock);
            intent.putExtra("image", image);

            startActivity(intent);
        });
    }

    private void initViews() {

        imgProduct = findViewById(R.id.imgProduct);
        btnBack = findViewById(R.id.btnBack);

        tvName = findViewById(R.id.tvName);
        tvDescription = findViewById(R.id.tvDescription);
        tvPrice = findViewById(R.id.tvPrice);
        tvStock = findViewById(R.id.tvStock);

        btnEdit = findViewById(R.id.btnEdit);
    }

    private void loadIntentData() {

        Intent i = getIntent();

        productId = i.getIntExtra("id", 0);

        name = i.getStringExtra("name");
        description = i.getStringExtra("description");
        image = i.getStringExtra("image");

        price = i.getDoubleExtra("price", 0);
        stock = i.getIntExtra("stock", 0);

        tvName.setText(name);
        tvDescription.setText(description);
        tvPrice.setText("KES " + price);
        tvStock.setText("Stock: " + stock);

        if (image != null && !image.isEmpty()) {

            String url =
                    "https://www.aaagrowers.co.ke/inventory/fillers/"
                            + image;

            Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(imgProduct);
        }
    }
}