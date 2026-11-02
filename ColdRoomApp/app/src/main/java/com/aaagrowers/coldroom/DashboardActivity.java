package com.aaagrowers.coldroom;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TextView;

import com.aaagrowers.coldroom.stocks.StockDbHelper;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvBoxesToday;
    private TextView tvTotalBoxes;
    private TextView tvRecent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        tvBoxesToday = findViewById(R.id.tvBoxesToday);
        tvTotalBoxes = findViewById(R.id.tvTotalBoxes);
        tvRecent = findViewById(R.id.tvRecent);

        loadStats();
    }

    private void loadStats() {

        StockDbHelper helper = new StockDbHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();

        // Total boxes
        Cursor c1 = db.rawQuery(
                "SELECT COUNT(*) FROM pending_scans",
                null
        );

        if(c1.moveToFirst())
            tvTotalBoxes.setText("Total Boxes: " + c1.getInt(0));

        c1.close();

        // Boxes today
        Cursor c2 = db.rawQuery(
                "SELECT COUNT(*) FROM pending_scans WHERE date(scan_time)=date('now')",
                null
        );

        if(c2.moveToFirst())
            tvBoxesToday.setText("Boxes Today: " + c2.getInt(0));

        c2.close();

        // Last 20 scans
        Cursor c3 = db.rawQuery(
                "SELECT qr_code,scan_time FROM pending_scans ORDER BY id DESC LIMIT 20",
                null
        );

        StringBuilder sb = new StringBuilder();

        while(c3.moveToNext()){

            sb.append(c3.getString(0))
                    .append("  ")
                    .append(c3.getString(1))
                    .append("\n");
        }

        tvRecent.setText(sb.toString());

        c3.close();
        db.close();
    }
}