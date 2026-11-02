package com.yourapp;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

import okhttp3.*;

public class RejectActivity extends AppCompatActivity {

    private Spinner spinnerFarm;
    private AutoCompleteTextView autoVariety;
    private Button btnStartRejections, btnNextReason, btnSubmitAll, btnRefresh;
    private EditText edtCurrentStems;
    private CardView cardCurrentReason;

    private final ArrayList<RejectionEntry2> tempRejections = new ArrayList<>();
    private int currentReasonIndex = 0;
    private final List<String> rejectionReasons = new ArrayList<>();
    private final HashMap<String, Integer> farmMap = new HashMap<>();
    private final HashMap<String, Integer> reasonMap = new HashMap<>();

    private int selectedVarietyId = -1;
    private String selectedVarietyName = "";

    private final OkHttpClient client = new OkHttpClient();
    private final String BASE_URL = "https://www.aaagrowers.co.ke/inventory/";

    private DatabaseHelper dbHelper;
    private VarietyAdapter3 varietyAdapter;

    // handler for debouncing + main thread
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    // keep reference to the TextWatcher so we can remove it safely
    private TextWatcher varietyWatcher = null;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reject);

        dbHelper = new DatabaseHelper(this);

        spinnerFarm = findViewById(R.id.spinnerFarm);
        autoVariety = findViewById(R.id.autoVariety);
        btnStartRejections = findViewById(R.id.btnStartRejections);
        btnNextReason = findViewById(R.id.btnNextReason);
        btnSubmitAll = findViewById(R.id.btnSubmitAll);
        btnRefresh = findViewById(R.id.btnRefresh);
        cardCurrentReason = findViewById(R.id.cardCurrentReason);
        edtCurrentStems = findViewById(R.id.edtCurrentStems);

        // initialize adapter once (empty)
        varietyAdapter = new VarietyAdapter3(this, new ArrayList<>(), new ArrayList<>());
        autoVariety.setAdapter(varietyAdapter);
        autoVariety.setThreshold(1);

        // load whatever is locally available
        loadFarmsOffline();
        loadReasonsOffline();

        // when a farm is selected, preload that farm's varieties
        spinnerFarm.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadAllVarietiesForFarm();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        setupVarietyAutoCompleteOffline();

        btnRefresh.setOnClickListener(v -> fetchAllDataFromServer());
        btnStartRejections.setOnClickListener(v -> startRejectionWizard());
        btnNextReason.setOnClickListener(v -> goToNextReason());
        btnSubmitAll.setOnClickListener(v -> submitAllRejections());
    }

    // ---------- SYNC ----------
    private void fetchAllDataFromServer() {
        Toast.makeText(this, "Fetching latest data...", Toast.LENGTH_SHORT).show();
        // fetch varieties first (it will insert into DB), then farms and reasons
        fetchVarietiesAndGreenhousesFromServer();
        fetchFarmsFromServer();
        fetchReasonsFromServer();
    }

    private void fetchFarmsFromServer() {
        Request request = new Request.Builder().url(BASE_URL + "get_farms.php").build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { e.printStackTrace(); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                try {
                    JSONArray arr = new JSONArray(response.body().string());
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.beginTransaction();
                    try {
                        db.delete("farm", null, null);
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.optJSONObject(i);
                            if (obj == null) continue;
                            int id = obj.optInt("id", obj.optInt("farmid", 0));
                            String name = obj.optString("name", obj.optString("farmname", ""));
                            ContentValues cv = new ContentValues();
                            cv.put("farmid", id);
                            cv.put("farmname", name);
                            db.insertWithOnConflict("farm", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                        }
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }
                    // load farms on main thread
                    runOnUiThread(() -> loadFarmsOffline());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void fetchReasonsFromServer() {
        Request request = new Request.Builder().url(BASE_URL + "get_reasons.php").build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { e.printStackTrace(); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                try {
                    JSONArray arr = new JSONArray(response.body().string());
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.beginTransaction();
                    try {
                        db.delete("rejection_reason", null, null);
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.optJSONObject(i);
                            if (obj == null) continue;
                            int id = obj.optInt("id", obj.optInt("rejection_id", 0));
                            String cat = obj.optString("category", "");
                            String abb = obj.optString("abbreviation", "");
                            int pr = obj.optInt("priority", 0);
                            ContentValues cv = new ContentValues();
                            cv.put("rejection_id", id);
                            cv.put("category", cat);
                            cv.put("abbreviation", abb);
                            cv.put("priority", pr);
                            db.insertWithOnConflict("rejection_reason", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                        }
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }
                    runOnUiThread(() -> loadReasonsOffline());
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    /**
     * Fetches from your PHP endpoint (search_variety.php) which returns objects:
     * { GreenhouseId, DisplayName, GreenhouseName, VarietyId, VarietyName, FarmId }
     *
     * We insert into local 'variety' (VarietyId, VarietyName, FarmId)
     * and 'greenhouses' (GreenhouseId, GreenhouseName, display_name, FarmId, VarietyId)
     */
    private void fetchVarietiesAndGreenhousesFromServer() {

        Request request = new Request.Builder()
                .url(BASE_URL + "search_variety.php")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { e.printStackTrace(); }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;

                try {
                    JSONArray arr = new JSONArray(response.body().string());

                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.beginTransaction();

                    try {
                        // keep old data and upsert new entries (INSERT OR REPLACE style)
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);

                            int varietyId = obj.optInt("VarietyId", obj.optInt("varietyId", 0));
                            String varietyName = obj.optString("VarietyName", obj.optString("VarietyName", ""));
                            int farmId = obj.optInt("FarmId", obj.optInt("farmId", 0));

                            // ---- insert / replace variety ----
                            ContentValues cvVar = new ContentValues();
                            cvVar.put("VarietyId", varietyId);
                            cvVar.put("VarietyName", varietyName);
                            cvVar.put("FarmId", farmId);
                            db.insertWithOnConflict("variety", null, cvVar, SQLiteDatabase.CONFLICT_REPLACE);

                            // ---- insert / replace greenhouse ----
                            ContentValues cvGH = new ContentValues();
                            cvGH.put("GreenhouseId", obj.optInt("GreenhouseId", obj.optInt("greenhouseId", 0)));
                            cvGH.put("GreenhouseName", obj.optString("GreenhouseName", obj.optString("greenhouse_name", "")));
                            // IMPORTANT: server's field is DisplayName (or Display_Name mapped) — we store as display_name
                            String display = obj.optString("DisplayName", obj.optString("Display_Name", ""));
                            cvGH.put("display_name", display);
                            cvGH.put("FarmId", farmId);
                            cvGH.put("VarietyId", varietyId);
                            db.insertWithOnConflict("greenhouses", null, cvGH, SQLiteDatabase.CONFLICT_REPLACE);
                        }

                        db.setTransactionSuccessful();

                    } finally {
                        db.endTransaction();
                    }

                    runOnUiThread(() -> {
                        Toast.makeText(RejectActivity.this, "Varieties updated", Toast.LENGTH_SHORT).show();
                        debugPrintVarieties();
                        // Preload varieties for currently selected farm (if any)
                        loadAllVarietiesForFarm();
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    // ---------- OFFLINE LOADERS ----------
    private void loadFarmsOffline() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = null;
        List<String> names = new ArrayList<>();
        farmMap.clear();
        try {
            c = db.query("farm", null, null, null, null, null, null);
            int colName = c.getColumnIndexOrThrow("farmname");
            int colId = c.getColumnIndexOrThrow("farmid");
            while (c.moveToNext()) {
                String name = c.getString(colName);
                int id = c.getInt(colId);
                names.add(name);
                farmMap.put(name, id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_dropdown_item, names);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerFarm.setAdapter(adapter);
    }

    private void loadReasonsOffline() {
        SQLiteDatabase db = db_helper_db_get();
        Cursor c = null;
        rejectionReasons.clear();
        reasonMap.clear();
        try {
            c = db.query("rejection_reason", null, null, null, null, null, "priority ASC");
            int colCategory = c.getColumnIndexOrThrow("category");
            int colId = c.getColumnIndexOrThrow("rejection_id");
            while (c.moveToNext()) {
                String cat = c.getString(colCategory);
                int id = c.getInt(colId);
                rejectionReasons.add(cat);
                reasonMap.put(cat, id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
        }
    }

    // small helper to get readable DB with minimal code duplication
    private SQLiteDatabase db_helper_db_get() {
        return dbHelper.getReadableDatabase();
    }

    // ---------- PRELOAD & AUTOCOMPLETE ----------
    /**
     * Preload all varieties for the selected farm into the adapter so typing is fast.
     * This reads from table `variety` where FarmId = ?
     */
    private void loadAllVarietiesForFarm() {
        if (spinnerFarm.getSelectedItem() == null) return;

        String farmName = spinnerFarm.getSelectedItem().toString();
        Integer farmId = farmMap.get(farmName);
        if (farmId == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            List<String> names = new ArrayList<>();
            List<Integer> ids = new ArrayList<>();

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = null;
            try {
                c = db.rawQuery(
                        "SELECT VarietyId, VarietyName FROM variety WHERE FarmId = ? ORDER BY VarietyName ASC",
                        new String[]{String.valueOf(farmId)}
                );

                int idxVarId = c.getColumnIndexOrThrow("VarietyId");
                int idxVarName = c.getColumnIndexOrThrow("VarietyName");
                while (c.moveToNext()) {
                    ids.add(c.getInt(idxVarId));
                    names.add(c.getString(idxVarName));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (c != null) c.close();
            }

            mainHandler.post(() -> {
                varietyAdapter.updateData(names, ids);
            });
        });
    }

    private void setupVarietyAutoCompleteOffline() {

        // Remove previous watcher
        try {
            if (varietyWatcher != null) {
                autoVariety.removeTextChangedListener(varietyWatcher);
                varietyWatcher = null;
            }
        } catch (Exception ignored) {}

        varietyWatcher = new SimpleTextWatcher() {

            private Runnable pending;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final String term = (s == null ? "" : s.toString().trim());

                if (pending != null) mainHandler.removeCallbacks(pending);

                // if less than 1 char, keep adapter preloaded or clear suggestions
                if (term.length() < 1) {
                    // we keep preloaded list (from loadAllVarietiesForFarm)
                    return;
                }

                String farmName = (spinnerFarm.getSelectedItem() != null)
                        ? spinnerFarm.getSelectedItem().toString()
                        : "";

                Integer farmId = farmMap.get(farmName);
                if (farmId == null || farmId == -1) return;

                // DEBOUNCE
                pending = () -> Executors.newSingleThreadExecutor().execute(() -> {

                    // Search local DB for matching display_name (greenhouse display) OR variety name
                    List<VarietyEntry> results = searchVarietiesOffline(term, farmId);

                    if (results.isEmpty()) {
                        mainHandler.post(() -> {
                            varietyAdapter.updateData(new ArrayList<>(), new ArrayList<>());
                            autoVariety.dismissDropDown();
                        });
                        return;
                    }

                    List<String> names = new ArrayList<>();
                    List<Integer> ids = new ArrayList<>();

                    for (VarietyEntry v : results) {
                        // use greenhouse display_name when available, else varietyName
                        String display = (v.displayName != null && !v.displayName.isEmpty())
                                ? v.displayName
                                : (v.varietyName != null ? v.varietyName : "");
                        names.add(display);
                        ids.add(v.varietyId);
                    }

                    mainHandler.post(() -> {
                        varietyAdapter.updateData(names, ids);
                        autoVariety.showDropDown();
                    });

                });

                mainHandler.postDelayed(pending, 120);
            }
        };

        autoVariety.addTextChangedListener(varietyWatcher);
        autoVariety.setAdapter(varietyAdapter);

        // IMPORTANT: CAPTURE SELECTION CORRECTLY
        autoVariety.setOnItemClickListener((parent, view, position, id) -> {
            // adapter returns the displayed name
            selectedVarietyId = varietyAdapter.getIdAt(position);
            selectedVarietyName = varietyAdapter.getItem(position);
            autoVariety.setText(selectedVarietyName);
        });
    }

    /**
     * Searches the local DB for varieties matching the typed term within the selected farm.
     * It uses greenhouse.display_name (your confirmed column) and variety.VarietyName.
     */
    private List<VarietyEntry> searchVarietiesOffline(String term, Integer farmId) {
        List<VarietyEntry> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = null;

        try {
            // filter by farmId and search both display_name and variety name
            c = db.rawQuery(
                    "SELECT g.GreenhouseId, g.display_name AS GHDisplay, g.GreenhouseName, g.VarietyId, " +
                            "v.VarietyName AS VarietyName, g.FarmId " +
                            "FROM greenhouses g " +
                            "INNER JOIN variety v ON g.VarietyId = v.VarietyId " +
                            "WHERE g.FarmId = ? AND (g.display_name LIKE ? COLLATE NOCASE OR v.VarietyName LIKE ? COLLATE NOCASE) " +
                            "ORDER BY g.display_name COLLATE NOCASE ASC " +
                            "LIMIT 300",
                    new String[]{String.valueOf(farmId), "%" + term + "%", "%" + term + "%"}
            );

            int idxGHId = c.getColumnIndexOrThrow("GreenhouseId");
            int idxGHName = c.getColumnIndexOrThrow("GreenhouseName");
            int idxGHDisplay = c.getColumnIndexOrThrow("GHDisplay");
            int idxVarId = c.getColumnIndexOrThrow("VarietyId");
            int idxVarName = c.getColumnIndexOrThrow("VarietyName");
            int idxFarm = c.getColumnIndexOrThrow("FarmId");

            while (c.moveToNext()) {
                list.add(new VarietyEntry(
                        c.getInt(idxGHId),
                        c.getString(idxGHName),
                        c.getString(idxGHDisplay),
                        c.getInt(idxVarId),
                        c.getString(idxVarName),
                        c.getInt(idxFarm)
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
        }

        return list;
    }

    // ---------- DB DEBUG ----------
    private void debugPrintVarieties() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM greenhouses LIMIT 200", null);
            while (c.moveToNext()) {
                int ghId = c.getInt(c.getColumnIndexOrThrow("GreenhouseId"));
                String ghName = c.getString(c.getColumnIndexOrThrow("GreenhouseName"));
                String display = c.getString(c.getColumnIndexOrThrow("display_name"));
                int varId = c.getInt(c.getColumnIndexOrThrow("VarietyId"));
                int farmId = c.getInt(c.getColumnIndexOrThrow("FarmId"));

                Log.d("DB_DEBUG", "GHId=" + ghId + ", GHName=" + ghName +
                        ", Display=" + display + ", VarId=" + varId + ", FarmId=" + farmId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
        }
    }

    // ---------- REJECTION WIZARD ----------
    private void startRejectionWizard() {
        if (spinnerFarm.getSelectedItem() == null || autoVariety.getText().toString().isEmpty()) {
            Toast.makeText(this, "Select farm and variety first", Toast.LENGTH_SHORT).show();
            return;
        }
        currentReasonIndex = 0;
        cardCurrentReason.setVisibility(View.VISIBLE);
        btnStartRejections.setVisibility(View.GONE);
        showCurrentReason();
    }

    private void showCurrentReason() {
        if (currentReasonIndex >= rejectionReasons.size()) {
            cardCurrentReason.setVisibility(View.GONE);
            btnSubmitAll.setVisibility(View.VISIBLE);
            return;
        }
        String reason = rejectionReasons.get(currentReasonIndex);
        ((TextView) findViewById(R.id.tvCurrentReason)).setText(reason);
        edtCurrentStems.setText("");
    }

    private void goToNextReason() {
        String stemsStr = edtCurrentStems.getText().toString();
        int stems = stemsStr.isEmpty() ? 0 : Integer.parseInt(stemsStr);

        String farmName = spinnerFarm.getSelectedItem().toString();
        int farmId = farmMap.get(farmName);
        int reasonId = reasonMap.get(rejectionReasons.get(currentReasonIndex));

        RejectionEntry2 entry = new RejectionEntry2(
                farmName, farmId,
                selectedVarietyName, selectedVarietyId,
                stems,
                rejectionReasons.get(currentReasonIndex), reasonId
        );
        tempRejections.add(entry);
        currentReasonIndex++;
        showCurrentReason();
    }

    private void submitAllRejections() {
        if (tempRejections.isEmpty()) {
            Toast.makeText(this, "No rejections to submit", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (RejectionEntry2 r : tempRejections) {
                ContentValues cv = new ContentValues();
                cv.put("farm", r.farmId);
                cv.put("variety_id", r.varietyId);
                cv.put("variety_name", r.varietyName);
                cv.put("stems", r.stems);
                cv.put("rejection_reason", r.rejectionReasonId);
                db.insert("rejections", null, cv);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        tempRejections.clear();
        btnSubmitAll.setVisibility(View.GONE);
        btnStartRejections.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Rejections saved offline successfully", Toast.LENGTH_SHORT).show();
    }

    // ---------- Helper classes ----------
    public static class VarietyEntry {
        public final int greenhouseId;
        public final String greenhouseName;
        public final String displayName;
        public final int varietyId;
        public final String varietyName;
        public final int farmId;

        public VarietyEntry(int greenhouseId, String greenhouseName, String displayName,
                            int varietyId, String varietyName, int farmId) {
            this.greenhouseId = greenhouseId;
            this.greenhouseName = greenhouseName;
            this.displayName = displayName;
            this.varietyId = varietyId;
            this.varietyName = varietyName;
            this.farmId = farmId;
        }
    }
}
