package com.yourapp.summers;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.yourapp.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SummersReportsActivity extends AppCompatActivity {

    Spinner spFarm;
    EditText txtFrom, txtTo;
    Button btnDownload, btnOpen;

    ProgressBar progressBar;

    long downloadId;
    String downloadedFilePath = "";

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summers_reports);

        spFarm = findViewById(R.id.spFarm);
        txtFrom = findViewById(R.id.txtFrom);
        txtTo = findViewById(R.id.txtTo);
        btnDownload = findViewById(R.id.btnDownload);

        progressBar = findViewById(R.id.progressBar);
        btnOpen = findViewById(R.id.btnOpen);

        btnOpen.setVisibility(View.GONE);

        String[] farms = {"", "CHUI", "SIMBA"};

        spFarm.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                farms
        ));

        txtFrom.setOnClickListener(v -> showDate(txtFrom));
        txtTo.setOnClickListener(v -> showDate(txtTo));

        btnDownload.setOnClickListener(v -> startDownload());

        btnOpen.setOnClickListener(v -> {

            Intent intent = new Intent(this, PdfViewerActivity.class);
            intent.putExtra("filePath", downloadedFilePath);
            startActivity(intent);
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                    onDownloadComplete,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
            );
        } else {
            registerReceiver(
                    onDownloadComplete,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            unregisterReceiver(onDownloadComplete);
        } catch (Exception ignored) {
        }
    }

    private void startDownload() {

        String farm = spFarm.getSelectedItem().toString();
        String from = txtFrom.getText().toString();
        String to = txtTo.getText().toString();

        if (farm.isEmpty()) {
            Toast.makeText(this, "Select farm", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = farm + "_" + to + ".pdf";

        String url =
                "https://www.aaagrowers.co.ke/inventory/summers/reports/harvest_report_pdf.php?"
                        + "farm=" + farm
                        + "&fromDate=" + from
                        + "&toDate=" + to;

        DownloadManager.Request request =
                new DownloadManager.Request(Uri.parse(url));

        request.setTitle("Downloading Report");
        request.setDescription("Summers Harvest Report");

        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
        );

        request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                fileName
        );

        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadId = dm.enqueue(request);

        progressBar.setVisibility(View.VISIBLE);
    }

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

            if (downloadId == id) {

                DownloadManager dm =
                        (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);

                Cursor cursor = dm.query(query);

                if (cursor.moveToFirst()) {

                    int status = cursor.getInt(
                            cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    );

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {

                        String uri = cursor.getString(
                                cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        );

                        downloadedFilePath = Uri.parse(uri).getPath();

                        progressBar.setVisibility(View.GONE);
                        btnOpen.setVisibility(View.VISIBLE);

                        Toast.makeText(context,
                                "Download complete",
                                Toast.LENGTH_SHORT).show();
                    }
                }

                cursor.close();
            }
        }
    };

    private void showDate(EditText txt) {

        Calendar cal = Calendar.getInstance();

        DatePickerDialog dp =
                new DatePickerDialog(
                        this,
                        (view, y, m, d) -> {

                            Calendar c = Calendar.getInstance();
                            c.set(y, m, d);

                            txt.setText(
                                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            .format(c.getTime())
                            );

                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                );

        dp.show();
    }
}