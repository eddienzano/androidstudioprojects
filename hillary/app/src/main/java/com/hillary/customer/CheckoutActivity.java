package com.hillary.customer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.hillary.R;
import com.hillary.network.ApiClient;
import com.hillary.network.Constants;
import com.hillary.utils.SessionManager;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CheckoutActivity extends AppCompatActivity {

    TextView etFullName,
            etPhone,
            tvSelectedAddress,
            tvCoordinates;

    MaterialButton btnPlaceOrder,
            btnChooseLocation,
            btnOpenMap;

    RadioGroup radioFulfillment;

    RadioButton radioDelivery,
            radioPickup;

    CardView addressCard;

    SessionManager session;

    String selectedAddress = "";

    double latitude = 0.0;
    double longitude = 0.0;

    private static final int MAP_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_checkout);

        session = new SessionManager(this);

        initViews();

        prefillCustomer();

        setupFulfillment();

        setupLocationButtons();

        btnPlaceOrder.setOnClickListener(
                v -> placeOrder()
        );
    }

    private void initViews(){

        etFullName =
                findViewById(R.id.etFullName);

        etPhone =
                findViewById(R.id.etPhone);

        tvSelectedAddress =
                findViewById(R.id.tvSelectedAddress);

        tvCoordinates =
                findViewById(R.id.tvCoordinates);

        btnPlaceOrder =
                findViewById(R.id.btnPlaceOrder);

        btnChooseLocation =
                findViewById(R.id.btnChooseLocation);

        btnOpenMap =
                findViewById(R.id.btnOpenMap);

        radioFulfillment =
                findViewById(R.id.radioFulfillment);

        radioDelivery =
                findViewById(R.id.radioDelivery);

        radioPickup =
                findViewById(R.id.radioPickup);

        addressCard =
                findViewById(R.id.addressCard);
    }

    private void prefillCustomer(){

        etFullName.setText(
                session.getCustomerName()
        );
    }

    private void setupFulfillment(){

        radioFulfillment.setOnCheckedChangeListener(
                (group, checkedId) -> {

                    if(checkedId == R.id.radioPickup){

                        addressCard.setVisibility(View.GONE);

                    } else {

                        addressCard.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void setupLocationButtons(){

        btnChooseLocation.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            CheckoutActivity.this,
                            MapPickerActivity.class
                    );

            startActivityForResult(
                    intent,
                    MAP_REQUEST
            );
        });

        btnOpenMap.setOnClickListener(v -> {

            if(latitude == 0){

                Toast.makeText(
                        this,
                        "Select location first",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            String uri =
                    String.format(
                            Locale.ENGLISH,
                            "geo:%f,%f?q=%f,%f(Label)",
                            latitude,
                            longitude,
                            latitude,
                            longitude
                    );

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(uri)
                    );

            startActivity(intent);
        });
    }

    private void placeOrder(){

        String name =
                etFullName.getText()
                        .toString()
                        .trim();

        String phone =
                etPhone.getText()
                        .toString()
                        .trim();

        String fulfillmentType =
                radioDelivery.isChecked()
                        ? "delivery"
                        : "pickup";

        if(name.isEmpty()){

            Toast.makeText(
                    this,
                    "Full name required",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if(phone.isEmpty()){

            Toast.makeText(
                    this,
                    "Phone number required",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if(
                fulfillmentType.equals("delivery")
                        &&
                        latitude == 0
        ){

            Toast.makeText(
                    this,
                    "Please choose delivery location",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        btnPlaceOrder.setEnabled(false);

        btnPlaceOrder.setText(
                "Processing..."
        );

        try {

            JSONObject json =
                    new JSONObject();

            json.put(
                    "customer_id",
                    session.getCustomerId()
            );

            json.put(
                    "full_name",
                    name
            );

            json.put(
                    "phone",
                    phone
            );

            json.put(
                    "address",
                    selectedAddress
            );

            json.put(
                    "latitude",
                    latitude
            );

            json.put(
                    "longitude",
                    longitude
            );

            json.put(
                    "payment_method",
                    "Cash On Delivery"
            );

            json.put(
                    "fulfillment_type",
                    fulfillmentType
            );

            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            MediaType.parse(
                                    "application/json"
                            )
                    );

            Request request =
                    new Request.Builder()
                            .url(
                                    Constants.PLACE_ORDER_URL
                            )
                            .post(body)
                            .build();

            OkHttpClient client =
                    ApiClient.getPublicClient();

            client.newCall(request)
                    .enqueue(new Callback() {

                        @Override
                        public void onFailure(
                                Call call,
                                IOException e
                        ) {

                            runOnUiThread(() -> {

                                btnPlaceOrder.setEnabled(true);

                                btnPlaceOrder.setText(
                                        "Place Order"
                                );

                                Toast.makeText(
                                        CheckoutActivity.this,
                                        "Order failed",
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                        }

                        @Override
                        public void onResponse(
                                Call call,
                                Response response
                        ) throws IOException {

                            if(response.body() == null)
                                return;

                            try {

                                String res =
                                        response.body()
                                                .string();

                                JSONObject obj =
                                        new JSONObject(res);

                                boolean success =
                                        obj.getBoolean(
                                                "success"
                                        );

                                runOnUiThread(() -> {

                                    btnPlaceOrder.setEnabled(true);

                                    btnPlaceOrder.setText(
                                            "Place Order"
                                    );

                                    Toast.makeText(
                                            CheckoutActivity.this,
                                            obj.optString("message"),
                                            Toast.LENGTH_LONG
                                    ).show();

                                    if(success){

                                        finish();
                                    }
                                });

                            } catch (Exception e){

                                e.printStackTrace();
                            }
                        }
                    });

        } catch (Exception e){

            e.printStackTrace();
        }
    }
    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if(
                requestCode == MAP_REQUEST
                        &&
                        resultCode == RESULT_OK
                        &&
                        data != null
        ){

            latitude =
                    data.getDoubleExtra(
                            "latitude",
                            0
                    );

            longitude =
                    data.getDoubleExtra(
                            "longitude",
                            0
                    );

            selectedAddress =
                    data.getStringExtra(
                            "address"
                    );

            tvSelectedAddress.setText(
                    selectedAddress
            );

            tvCoordinates.setText(
                    "Lat: "
                            + latitude
                            + " | Lng: "
                            + longitude
            );
        }
    }
}