package com.unitedselections.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.unitedselections.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;

public class ReportsActivity extends AppCompatActivity {

    private TextView txtHarvest, txtIntake, txtDiscard, txtScanOut;
    private LinearLayout varietyContainer;

    private static final String TAG = "REPORTS";
    private static final String API_URL =
            "https://unitedselections.supplybridge.co.ke/api/reports_summary.php?date=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        txtHarvest = findViewById(R.id.txtHarvestValue);
        txtIntake = findViewById(R.id.txtIntakeValue);
        txtDiscard = findViewById(R.id.txtDiscardValue);
        txtScanOut = findViewById(R.id.txtScanOutValue);
        varietyContainer = findViewById(R.id.varietyContainer);

        // Navigation
        findViewById(R.id.btnHarvestReport).setOnClickListener(v ->
                startActivity(new Intent(this, FieldHarvestReportActivity.class)));

        findViewById(R.id.btnIntakeReport).setOnClickListener(v ->
                startActivity(new Intent(this, IntakeReportActivity.class)));

        findViewById(R.id.btnDiscardReport).setOnClickListener(v ->
                startActivity(new Intent(this, DiscardReportActivity.class)));

        findViewById(R.id.btnIssuanceReport).setOnClickListener(v ->
                startActivity(new Intent(this, ScanoutReportActivity.class)));

        // 🔥 USE TODAY'S DATE (IMPORTANT)
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        loadReport(today);
    }

    private void loadReport(String date) {

        new Thread(() -> {
            try {

                String fullUrl = API_URL + date;
                Log.d(TAG, "Calling API: " + fullUrl);

                URL url = new URL(fullUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                InputStream is = conn.getInputStream();
                Scanner scanner = new Scanner(is).useDelimiter("\\A");
                String response = scanner.hasNext() ? scanner.next() : "";

                Log.d(TAG, "API RESPONSE: " + response);

                JSONObject json = new JSONObject(response);

                if (!json.getString("status").equals("success")) {
                    Log.e(TAG, "API returned error");
                    return;
                }

                JSONObject summary = json.getJSONObject("summary");

                // 🔥 FIX: HANDLE STRING OR INT
                int harvest = Integer.parseInt(summary.optString("harvest", "0"));
                int intake = Integer.parseInt(summary.optString("intake", "0"));
                int discards = Integer.parseInt(summary.optString("discards", "0"));
                int scanOut = Integer.parseInt(summary.optString("scan_out", "0"));

                JSONArray varieties = json.getJSONArray("varieties");

                runOnUiThread(() -> {

                    // 🔥 UPDATE UI
                    txtHarvest.setText(String.valueOf(harvest));
                    txtIntake.setText(String.valueOf(intake));
                    txtDiscard.setText(String.valueOf(discards));
                    txtScanOut.setText(String.valueOf(scanOut));

                    // CLEAR OLD
                    varietyContainer.removeAllViews();

                    // POPULATE VARIETIES
                    for (int i = 0; i < varieties.length(); i++) {
                        try {
                            JSONObject obj = varieties.getJSONObject(i);

                            String variety = obj.getString("variety");
                            int total = Integer.parseInt(obj.optString("total", "0"));

                            LinearLayout row = new LinearLayout(this);
                            row.setOrientation(LinearLayout.HORIZONTAL);
                            row.setPadding(16, 16, 16, 16);
                            row.setBackgroundResource(R.drawable.card_modern);

                            LinearLayout.LayoutParams params =
                                    new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT);
                            params.setMargins(0, 10, 0, 0);
                            row.setLayoutParams(params);

                            TextView name = new TextView(this);
                            name.setText(variety);
                            name.setTextSize(16);
                            name.setLayoutParams(new LinearLayout.LayoutParams(0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                            TextView qty = new TextView(this);
                            qty.setText(String.valueOf(total));
                            qty.setTextSize(16);
                            qty.setTextColor(getResources().getColor(R.color.primary));

                            row.addView(name);
                            row.addView(qty);

                            varietyContainer.addView(row);

                        } catch (Exception e) {
                            Log.e(TAG, "Variety parse error", e);
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "ERROR LOADING REPORT", e);
            }

        }).start();
    }
}