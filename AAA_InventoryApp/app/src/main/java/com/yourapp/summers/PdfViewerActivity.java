package com.yourapp.summers;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.yourapp.R;

import java.io.File;

public class PdfViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        String path = getIntent().getStringExtra("filePath");

        if (path == null) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        openPdf(path);
    }

    private void openPdf(String path) {

        File file = new File(path);

        if (!file.exists()) {
            Toast.makeText(this, "PDF missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".provider",
                    file
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            startActivity(intent);

            finish();

        } catch (Exception e) {
            Toast.makeText(this,
                    "No PDF viewer installed",
                    Toast.LENGTH_LONG).show();
        }
    }
}