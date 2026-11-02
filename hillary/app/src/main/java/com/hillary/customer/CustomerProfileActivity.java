package com.hillary.customer;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hillary.R;
import com.hillary.utils.SessionManager;

public class CustomerProfileActivity
        extends AppCompatActivity {

    TextView tvName,
            tvEmail,
            tvPhone;

    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_customer_profile);

        session = new SessionManager(this);

        tvName =
                findViewById(R.id.tvName);

        tvEmail =
                findViewById(R.id.tvEmail);

        tvPhone =
                findViewById(R.id.tvPhone);

        tvName.setText(
                session.getCustomerName()
        );

        tvEmail.setText(
                session.getCustomerEmail()
        );

        tvPhone.setText(
                "Phone not available"
        );
    }
}