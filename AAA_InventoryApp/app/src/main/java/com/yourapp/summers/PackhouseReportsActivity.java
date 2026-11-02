package com.yourapp.summers;

import android.app.DatePickerDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.yourapp.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PackhouseReportsActivity extends AppCompatActivity {

    Spinner spFarm;
    EditText txtFrom, txtTo;
    Button btnDownload, btnOpen;
    ProgressBar progressBar;

    String downloadedFilePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_packhouse_reports);

        spFarm = findViewById(R.id.spFarm);
        txtFrom = findViewById(R.id.txtFrom);
        txtTo = findViewById(R.id.txtTo);
        btnDownload = findViewById(R.id.btnDownload);
        btnOpen = findViewById(R.id.btnOpen);
        progressBar = findViewById(R.id.progressBar);

        String[] farms = {"", "CHUI", "SIMBA"};

        spFarm.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                farms
        ));

        txtFrom.setOnClickListener(v -> showDate(txtFrom));
        txtTo.setOnClickListener(v -> showDate(txtTo));

        btnDownload.setOnClickListener(v -> downloadReport());

        btnOpen.setOnClickListener(v -> openPdf());
    }

    private void downloadReport() {

        String farm = spFarm.getSelectedItem().toString();
        String from = txtFrom.getText().toString();
        String to = txtTo.getText().toString();

        if (from.isEmpty() || to.isEmpty()) {
            Toast.makeText(this, "Select dates first", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnOpen.setVisibility(View.GONE);

        String url = "https://www.aaagrowers.co.ke/inventory/summers/reports/packhouse_report_pdf.php"
                + "?farm=" + farm
                + "&fromDate=" + from
                + "&toDate=" + to;

        String fileName = "PACKHOUSE_" + farm + "_" + from + "_TO_" + to + ".pdf";

        DownloadManager.Request request =
                new DownloadManager.Request(Uri.parse(url));

        request.setTitle("Packhouse Report");
        request.setDescription("Downloading report...");
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
        );

        File file = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
        );

        downloadedFilePath = file.getAbsolutePath();

        request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                fileName
        );

        DownloadManager dm =
                (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        long id = dm.enqueue(request);

        Toast.makeText(this, "Downloading...", Toast.LENGTH_SHORT).show();

        // simple delay to show UX (DownloadManager is async)
        progressBar.postDelayed(() -> {
            progressBar.setVisibility(View.GONE);
            btnOpen.setVisibility(View.VISIBLE);
        }, 2500);
    }

    private void openPdf() {
        try {
            File file = new File(downloadedFilePath);

            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    file
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(intent);

        } catch (ActivityNotFoundException e) {
            Toast.makeText(this,
                    "No PDF app installed",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showDate(EditText txt) {

        Calendar cal = Calendar.getInstance();

        DatePickerDialog dp = new DatePickerDialog(
                this,
                (view, y, m, d) -> {

                    Calendar c = Calendar.getInstance();
                    c.set(y, m, d);

                    txt.setText(new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                    ).format(c.getTime()));
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        dp.show();
    }
}