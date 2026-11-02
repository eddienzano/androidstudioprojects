package com.aaagrowers.coldroom.summers;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aaagrowers.coldroom.R;

public class SummersStockTakeActivity extends AppCompatActivity {

    private Spinner farmSpinner;

    private Button syncBtn;
    private Button startBtn;

    private final String[] farms = {
            "Chui",
            "Simba",
            "Chestnut"
    };

    @Override
    protected void onCreate(Bundle b) {

        super.onCreate(b);

        setContentView(R.layout.activity_stocktake);

        farmSpinner = findViewById(R.id.farmSpinner);

        syncBtn = findViewById(R.id.syncButton);

        startBtn = findViewById(R.id.startStockTake);

        farmSpinner.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        farms
                )
        );

        startBtn.setEnabled(false);

        // ==========================================
        // SYNC INVENTORY
        // ==========================================

        syncBtn.setOnClickListener(v -> {

            String farm =
                    farmSpinner.getSelectedItem().toString();

            syncBtn.setEnabled(false);

            Toast.makeText(
                    this,
                    "Syncing inventory...",
                    Toast.LENGTH_SHORT
            ).show();

            new SyncTask(farm).execute();
        });

        // ==========================================
        // START STOCKTAKE
        // ==========================================

        startBtn.setOnClickListener(v -> {

            String farm =
                    farmSpinner.getSelectedItem().toString();

            Intent i = new Intent(
                    this,
                    SummersScanActivity.class
            );

            i.putExtra("farm", farm);

            startActivity(i);
        });
    }

    // ==================================================
    // BACKGROUND SYNC
    // ==================================================

    class SyncTask extends AsyncTask<Void, Void, Boolean> {

        String farm;

        SyncTask(String f) {
            farm = f;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {

            try {

                SummersInventoryRepository.syncInventory(
                        SummersStockTakeActivity.this,
                        farm
                );

                return true;

            } catch (Exception e) {

                e.printStackTrace();

                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {

            syncBtn.setEnabled(true);

            if (success) {

                Toast.makeText(
                        SummersStockTakeActivity.this,
                        "Inventory synced successfully",
                        Toast.LENGTH_LONG
                ).show();

                startBtn.setEnabled(true);

            } else {

                Toast.makeText(
                        SummersStockTakeActivity.this,
                        "Inventory sync failed",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }
}