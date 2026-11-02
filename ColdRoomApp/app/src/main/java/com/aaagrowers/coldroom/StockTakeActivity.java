package com.aaagrowers.coldroom;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import com.aaagrowers.coldroom.stocks.VarietyRepository;

import com.aaagrowers.coldroom.stocks.StockInventoryRepository;
import com.aaagrowers.coldroom.stocks.StockScanActivity;

public class StockTakeActivity extends AppCompatActivity {

    private Spinner farmSpinner;
    private Button syncButton;
    private Button startStockTake;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stocktake);

        farmSpinner = findViewById(R.id.farmSpinner);
        syncButton = findViewById(R.id.syncButton);
        startStockTake = findViewById(R.id.startStockTake);

        String[] farms = {"Chui", "Simba", "Chestnut"};

        farmSpinner.setAdapter(
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item,
                        farms)
        );

        startStockTake.setEnabled(false);

        syncButton.setOnClickListener(v -> {

            String farm = farmSpinner.getSelectedItem().toString();

            new SyncInventoryTask(farm).execute();
        });

        startStockTake.setOnClickListener(v -> {

            String farm = farmSpinner.getSelectedItem().toString();

            Intent i = new Intent(this, StockScanActivity.class);
            i.putExtra("farm", farm);
            startActivity(i);

        });
    }

    class SyncInventoryTask extends AsyncTask<Void,Void,Integer>{

        String farm;

        SyncInventoryTask(String farm){
            this.farm = farm;
        }

        @Override
        protected void onPreExecute() {

            Toast.makeText(
                    StockTakeActivity.this,
                    "Syncing inventory...",
                    Toast.LENGTH_SHORT
            ).show();

            syncButton.setEnabled(false);
        }


        @Override
        protected Integer doInBackground(Void... voids) {

            // Sync expected inventory
            StockInventoryRepository.syncInventory(
                    StockTakeActivity.this,
                    farm
            );

            // Sync varieties for fix dialog — pass selected farm
            VarietyRepository.syncVarieties(
                    StockTakeActivity.this,
                    farm
            );

            return StockInventoryRepository.getInventoryCount(
                    StockTakeActivity.this
            );
        }

        @Override
        protected void onPostExecute(Integer count) {

            syncButton.setEnabled(true);

            Toast.makeText(
                    StockTakeActivity.this,
                    "Inventory synced: " + count + " boxes",
                    Toast.LENGTH_LONG
            ).show();

            startStockTake.setEnabled(true);
        }
    }
}