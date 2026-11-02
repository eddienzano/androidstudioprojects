package com.hillary.Dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.hillary.R;
import com.hillary.auth.LoginActivity;
import com.hillary.network.ApiClient;
import com.hillary.network.Constants;
import com.hillary.products.MyProductsActivity;
import com.hillary.products.inventory.InventoryDashboardActivity;
import com.hillary.supplier.SupplierOrdersActivity;
import com.hillary.supplier.SupplierProfileActivity;
import com.hillary.utils.SessionManager;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class DashboardActivity extends AppCompatActivity {

    SessionManager session;

    TextView tvName, tvEmail;
    TextView tvOrders, tvPending, tvCompleted, tvNotifications;

    CardView cardProfile,
            cardOrders,
            cardAnalytics,
            cardInventoryDashboard,
            cardLogout,
            ordersButton;   // ✅ FIX: was LinearLayout before

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        session = new SessionManager(this);

        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        loadDashboardData();
        setupClicks();
        runAnimations();
    }

    private void initViews() {

        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);

        tvOrders = findViewById(R.id.tvOrders);
        tvPending = findViewById(R.id.tvPending);
        tvCompleted = findViewById(R.id.tvCompleted);
        tvNotifications = findViewById(R.id.tvNotifications);

        ordersButton = findViewById(R.id.ordersButton);

        cardProfile = findViewById(R.id.cardProfile);
        cardOrders = findViewById(R.id.cardOrders);
        cardAnalytics = findViewById(R.id.cardAnalytics);
        cardLogout = findViewById(R.id.cardLogout);
        cardInventoryDashboard = findViewById(R.id.cardInventoryDashboard);

        tvName.setText(session.getName());
        tvEmail.setText(session.getEmail());
    }

    private void loadDashboardData() {

        Request request = new Request.Builder()
                .url(Constants.ME_URL)
                .addHeader("Authorization", "Bearer " + session.getToken())
                .build();

        ApiClient.getClient(this).newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(DashboardActivity.this,
                                "Failed to load dashboard",
                                Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.body() == null) return;

                String res = response.body().string();

                try {
                    JSONObject obj = new JSONObject(res);

                    if (obj.getString("status").equals("success")) {

                        JSONObject stats = obj.getJSONObject("data").getJSONObject("stats");

                        runOnUiThread(() -> {

                            tvOrders.setText(String.valueOf(stats.optInt("total_orders")));
                            tvPending.setText(String.valueOf(stats.optInt("pending_orders")));
                            tvCompleted.setText(String.valueOf(stats.optInt("completed_orders")));
                            tvNotifications.setText(String.valueOf(stats.optInt("notifications")));
                        });
                    }

                } catch (Exception ignored) {}
            }
        });
    }

    private void setupClicks() {

        cardLogout.setOnClickListener(v -> {
            session.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        cardProfile.setOnClickListener(v ->
                startActivity(new Intent(this, SupplierProfileActivity.class)));

        cardOrders.setOnClickListener(v ->
                startActivity(new Intent(this, MyProductsActivity.class)));

        cardAnalytics.setOnClickListener(v ->
                startActivity(new Intent(this, InventoryDashboardActivity.class)));

        cardInventoryDashboard.setOnClickListener(v ->
                startActivity(new Intent(this, InventoryDashboardActivity.class)));

        ordersButton.setOnClickListener(v ->
                startActivity(new Intent(this, SupplierOrdersActivity.class)));
    }

    private void runAnimations() {

        animateView(tvName, 0);
        animateView(tvEmail, 100);
        animateView(cardProfile, 300);
        animateView(cardOrders, 400);
        animateView(cardAnalytics, 500);
        animateView(cardInventoryDashboard, 600);
        animateView(cardLogout, 700);
    }

    private void animateView(View view, long delay) {

        view.setAlpha(0f);
        view.setTranslationY(40f);

        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(500)
                .start();
    }
}