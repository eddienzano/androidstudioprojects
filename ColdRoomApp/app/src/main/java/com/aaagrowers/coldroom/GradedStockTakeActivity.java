package com.aaagrowers.coldroom;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.aaagrowers.coldroom.stocks.GradedStockRepository;
import com.aaagrowers.coldroom.stocks.GradedStockScanActivity;
import com.aaagrowers.coldroom.stocks.VarietyRepository;

import java.util.HashMap;
import java.util.Map;

public class GradedStockTakeActivity extends AppCompatActivity {

    private Spinner farmSpinner;
    private Button syncButton;
    private Button startStockTake;

    // Map farm names to numeric IDs
    private final Map<String, Integer> farmMap = new HashMap<String, Integer>() {{
        put("Chui", 2);
        put("Simba", 1);
        put("Chestnut", 3);
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graded_stock_take);

        farmSpinner = findViewById(R.id.farmSpinner);
        syncButton = findViewById(R.id.syncButton);
        startStockTake = findViewById(R.id.startStockTake);

        // Populate spinner
        String[] farms = farmMap.keySet().toArray(new String[0]);
        farmSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, farms));

        startStockTake.setEnabled(false);

        syncButton.setOnClickListener(v -> {
            String farmName = farmSpinner.getSelectedItem().toString();
            int farmId = farmMap.get(farmName);
            new SyncGradedStockTask(farmId).execute();
        });

        startStockTake.setOnClickListener(v -> {
            String farmName = farmSpinner.getSelectedItem().toString();
            int farmId = farmMap.get(farmName);

            Intent i = new Intent(this, GradedStockScanActivity.class);
            i.putExtra("farm", farmId);
            startActivity(i);
        });
    }

    private class SyncGradedStockTask extends AsyncTask<Void, Void, Integer> {

        private final int farmId;

        SyncGradedStockTask(int farmId) {
            this.farmId = farmId;
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(GradedStockTakeActivity.this,
                    "Syncing graded stock...", Toast.LENGTH_SHORT).show();
            syncButton.setEnabled(false);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            // Sync graded stock with int farmId
            GradedStockRepository.syncGradedStock(GradedStockTakeActivity.this, farmId);

            // Convert int farmId to String for VarietyRepository
            VarietyRepository.syncVarieties(GradedStockTakeActivity.this, String.valueOf(farmId));

            // Return total synced graded stock count
            return GradedStockRepository.getGradedStockCount(GradedStockTakeActivity.this);
        }

        @Override
        protected void onPostExecute(Integer count) {
            syncButton.setEnabled(true);
            Toast.makeText(GradedStockTakeActivity.this,
                    "Graded stock synced: " + count + " boxes", Toast.LENGTH_LONG).show();
            startStockTake.setEnabled(true);
        }
    }
}