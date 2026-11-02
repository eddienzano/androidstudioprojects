package com.unitedselections.field;

import android.os.Bundle;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.unitedselections.R;
import com.unitedselections.admin.GreenhouseModel;
import com.unitedselections.db.VarietiesDbHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FieldDiscardActivity extends AppCompatActivity {

    Spinner greenhouse, reasonSpinner;

    AutoCompleteTextView variety;

    EditText qty, remark;

    Button btnAdd, btnSubmit;

    RecyclerView recycler;

    List<JSONObject> selectedVarieties = new ArrayList<>();

    VarietyAdapter adapter;

    ArrayList<GreenhouseModel> greenhouseList = new ArrayList<>();

    ArrayAdapter<GreenhouseModel> ghAdapter;

    OkHttpClient client = new OkHttpClient();

    String[] reasons = {
            "Bent Neck",
            "Short Stems",
            "Disease",
            "Pest Damage",
            "Mechanical Damage",
            "Others"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_discard);

        // =========================
        // INIT
        // =========================

        greenhouse = findViewById(R.id.greenhouse);
        reasonSpinner = findViewById(R.id.reasonSpinner);

        variety = findViewById(R.id.variety);

        qty = findViewById(R.id.qty);

        remark = findViewById(R.id.remark);

        btnAdd = findViewById(R.id.btnAdd);
        btnSubmit = findViewById(R.id.btnSubmit);

        recycler = findViewById(R.id.recycler);

        // =========================
        // RECYCLER
        // =========================

        adapter = new VarietyAdapter(selectedVarieties);

        recycler.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recycler.setAdapter(adapter);

        // =========================
        // REASONS
        // =========================

        ArrayAdapter<String> reasonAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        reasons
                );

        reasonSpinner.setAdapter(reasonAdapter);

        // =========================
        // LOAD DATA
        // =========================

        loadGreenhouses();

        loadVarieties();

        // =========================
        // GREENHOUSE CHANGE
        // =========================

        greenhouse.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            android.view.View view,
                            int position,
                            long id
                    ) {

                        if (position >= 0 &&
                                position < greenhouseList.size()) {

                            GreenhouseModel gh =
                                    greenhouseList.get(position);

                            loadVarieties();
                        }
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {

                    }
                }
        );

        // =========================
        // BUTTONS
        // =========================

        btnAdd.setOnClickListener(v -> addVariety());

        btnSubmit.setOnClickListener(v -> submitDiscard());
    }

    // =====================================================
    // ADD VARIETY
    // =====================================================

    private void addVariety() {

        String selectedVariety =
                variety.getText().toString().trim();

        String quantity =
                qty.getText().toString().trim();

        if (selectedVariety.isEmpty() ||
                quantity.isEmpty()) {

            Toast.makeText(
                    this,
                    "Enter variety and quantity",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        try {

            int newQty =
                    Integer.parseInt(quantity);

            // =========================
            // CHECK DUPLICATES
            // =========================

            for (int i = 0;
                 i < selectedVarieties.size();
                 i++) {

                JSONObject existing =
                        selectedVarieties.get(i);

                String existingVariety =
                        existing.getString("variety_name");

                if (existingVariety.equalsIgnoreCase(
                        selectedVariety
                )) {

                    int oldQty =
                            existing.getInt("quantity");

                    existing.put(
                            "quantity",
                            oldQty + newQty
                    );

                    adapter.notifyItemChanged(i);

                    Toast.makeText(
                            this,
                            "Quantity updated",
                            Toast.LENGTH_SHORT
                    ).show();

                    variety.setText("");
                    qty.setText("");

                    return;
                }
            }

            // =========================
            // ADD NEW
            // =========================

            JSONObject obj = new JSONObject();

            obj.put(
                    "variety_name",
                    selectedVariety
            );

            obj.put(
                    "quantity",
                    newQty
            );

            selectedVarieties.add(obj);

            adapter.notifyItemInserted(
                    selectedVarieties.size() - 1
            );

            variety.setText("");
            qty.setText("");

            Toast.makeText(
                    this,
                    "Variety added",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Invalid quantity",
                    Toast.LENGTH_SHORT
            ).show();

            e.printStackTrace();
        }
    }

    // =====================================================
    // SUBMIT
    // =====================================================

    private void submitDiscard() {

        if (selectedVarieties.isEmpty()) {

            Toast.makeText(
                    this,
                    "Add varieties",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        new Thread(() -> {

            try {

                GreenhouseModel gh =
                        (GreenhouseModel)
                                greenhouse.getSelectedItem();

                if (gh == null) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    this,
                                    "Select greenhouse",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );

                    return;
                }

                JSONArray arr =
                        new JSONArray(selectedVarieties);

                String postData =

                        "greenhouse=" +
                                URLEncoder.encode(
                                        gh.getName(),
                                        "UTF-8"
                                )

                                + "&category=" +

                                URLEncoder.encode(
                                        gh.getCategory(),
                                        "UTF-8"
                                )

                                + "&reason=" +

                                URLEncoder.encode(
                                        reasonSpinner
                                                .getSelectedItem()
                                                .toString(),
                                        "UTF-8"
                                )

                                + "&remark=" +

                                URLEncoder.encode(
                                        remark.getText()
                                                .toString(),
                                        "UTF-8"
                                )

                                + "&user_id=1"

                                + "&items=" +

                                URLEncoder.encode(
                                        arr.toString(),
                                        "UTF-8"
                                );

                URL url = new URL(
                        "https://unitedselections.supplybridge.co.ke/api/save_discard.php"
                );

                HttpURLConnection conn =
                        (HttpURLConnection)
                                url.openConnection();

                conn.setRequestMethod("POST");

                conn.setDoOutput(true);

                OutputStream os =
                        conn.getOutputStream();

                os.write(postData.getBytes());

                os.flush();
                os.close();

                int responseCode =
                        conn.getResponseCode();

                runOnUiThread(() -> {

                    if (responseCode == 200) {

                        Toast.makeText(
                                this,
                                "Discard saved",
                                Toast.LENGTH_SHORT
                        ).show();

                        selectedVarieties.clear();

                        adapter.notifyDataSetChanged();

                        remark.setText("");

                    } else {

                        Toast.makeText(
                                this,
                                "Server error",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });

            } catch (Exception e) {

                e.printStackTrace();

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
            }

        }).start();
    }

    // =====================================================
    // LOAD VARIETIES
    // =====================================================

    private void loadVarieties() {

        Log.d("VARIETY", "Loading varieties");

        VarietiesDbHelper helper =
                new VarietiesDbHelper(this);

        android.database.sqlite.SQLiteDatabase db =
                helper.getReadableDatabase();

        android.database.Cursor cursor =
                db.rawQuery(
                        "SELECT pedigree_item, description " +
                                "FROM varieties_cache " +
                                "ORDER BY pedigree_item ASC",
                        null
                );

        List<String> list =
                new ArrayList<>();

        while (cursor.moveToNext()) {

            String code =
                    cursor.getString(0);

            String desc =
                    cursor.getString(1);

            if (desc != null &&
                    !desc.isEmpty()) {

                list.add(
                        code + " - " + desc
                );

            } else {

                list.add(code);
            }
        }

        cursor.close();

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        list
                );

        variety.setAdapter(adapter);

        variety.setThreshold(1);
    }

    // =====================================================
    // LOAD GREENHOUSES
    // =====================================================

    private void loadGreenhouses() {

        String url =
                "https://unitedselections.supplybridge.co.ke/api/get_greenhouses.php";

        Request request =
                new Request.Builder()
                        .url(url)
                        .build();

        client.newCall(request).enqueue(
                new Callback() {

                    @Override
                    public void onFailure(
                            Call call,
                            java.io.IOException e
                    ) {

                        runOnUiThread(() ->
                                Toast.makeText(
                                        FieldDiscardActivity.this,
                                        "Failed to load greenhouses",
                                        Toast.LENGTH_SHORT
                                ).show()
                        );
                    }

                    @Override
                    public void onResponse(
                            Call call,
                            Response response
                    ) throws java.io.IOException {

                        if (response.body() == null)
                            return;

                        String res =
                                response.body().string();

                        try {

                            JSONObject obj =
                                    new JSONObject(res);

                            if (!obj.getString("status")
                                    .equals("success")) {

                                return;
                            }

                            JSONArray arr =
                                    obj.getJSONArray("data");

                            greenhouseList.clear();

                            for (int i = 0;
                                 i < arr.length();
                                 i++) {

                                JSONObject o =
                                        arr.getJSONObject(i);

                                greenhouseList.add(
                                        new GreenhouseModel(
                                                o.getInt("id"),
                                                o.getString("greenhouse_name"),
                                                o.getString("category_code")
                                        )
                                );
                            }

                            runOnUiThread(() -> {

                                ghAdapter =
                                        new ArrayAdapter<>(

                                                FieldDiscardActivity.this,

                                                android.R.layout
                                                        .simple_spinner_dropdown_item,

                                                greenhouseList
                                        );

                                greenhouse.setAdapter(
                                        ghAdapter
                                );

                                if (!greenhouseList.isEmpty()) {

                                    greenhouse.setSelection(0);
                                }
                            });

                        } catch (Exception e) {

                            e.printStackTrace();

                            runOnUiThread(() ->
                                    Toast.makeText(
                                            FieldDiscardActivity.this,
                                            "Invalid greenhouse data",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                        }
                    }
                }
        );
    }
}