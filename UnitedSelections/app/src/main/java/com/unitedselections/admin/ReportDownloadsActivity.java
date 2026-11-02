package com.unitedselections.admin;

import android.app.DatePickerDialog;
import android.app.DownloadManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.unitedselections.R;

import java.util.Calendar;

public class ReportDownloadsActivity extends AppCompatActivity {

    EditText fromDate, toDate;

    String BASE_URL = "https://unitedselections.supplybridge.co.ke/api/reports/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_downloads);

        fromDate = findViewById(R.id.fromDate);
        toDate = findViewById(R.id.toDate);

        fromDate.setOnClickListener(v -> showDatePicker(fromDate));
        toDate.setOnClickListener(v -> showDatePicker(toDate));

        /* 🔥 CONNECT CARDS */
        setupCard(R.id.cardField, "Field Report", "field.php");
        setupCard(R.id.cardIntake, "Intake Report", "intake.php");
        setupCard(R.id.cardVariance, "Variance Report", "variance.php");
        setupCard(R.id.cardIssuance, "Issuance Report", "issuance.php");
        setupCard(R.id.cardGreenhouse, "Greenhouse Report", "greenhouse.php");
        setupCard(R.id.cardStockFlow, "Stock Flow Report", "stock_flow.php");
        setupCard(R.id.cardScanoutSummary, "Scanout Summary", "scanout_summary.php");
        setupCard(R.id.cardDiscard, "Discard Report", "discard.php");
    }

    /* 🔥 CARD HANDLER */
    private void setupCard(int cardId, String title, String file){

        LinearLayout card = findViewById(cardId);

        if(card == null) return;

        TextView txt = card.findViewById(R.id.title);

        if(txt != null){
            txt.setText(title);
        }

        card.setOnClickListener(v -> downloadReport(file));
    }

    private void showDatePicker(EditText field){
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            field.setText(year + "-" + (month+1) + "-" + day);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void downloadReport(String file){
        String from = fromDate.getText().toString();
        String to = toDate.getText().toString();

        if(from.isEmpty() || to.isEmpty()){
            fromDate.setError("Required");
            toDate.setError("Required");
            return;
        }

        String url = BASE_URL + file + "?from=" + from + "&to=" + to;

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Downloading " + file);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        manager.enqueue(request);
    }
}