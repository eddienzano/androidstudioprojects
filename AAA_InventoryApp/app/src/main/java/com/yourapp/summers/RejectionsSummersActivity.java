package com.yourapp.summers;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.yourapp.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

import okhttp3.*;

public class RejectionsSummersActivity extends AppCompatActivity {

    // ============================================================
    // UI
    // ============================================================

    private ImageView btnBack;

    private LinearLayout selectionSection;
    private LinearLayout rejectionSection;
    private LinearLayout commentsSection;
    private LinearLayout summarySection;

    private Spinner spinnerFarm;
    private Spinner spinnerFlowerType;
    private AutoCompleteTextView autoVariety;

    private CardView cardFlowerType;
    private CardView cardVariety;
    private CardView cardSelectionStatus;

    private TextView tvSelectionStatus;
    private TextView tvSelectionDetails;

    private Button btnStartRejections;
    private Button btnRejectionsNext;
    private Button btnCommentsNext;
    private Button btnSubmitAll;

    private TextView tvSelectedVariety;
    private TextView tvSelectedFarmType;
    private TextView tvTotalRejected;

    private EditText edtComments;

    private TextView tvCommentsTotal;

    private TextView tvSummaryVariety;
    private TextView tvSummaryFarmType;
    private TextView tvSummaryTotal;
    private TextView tvSummaryComments;

    private LinearLayout containerDiseases;
    private LinearLayout containerPest;
    private LinearLayout containerQuality;
    private LinearLayout containerSummary;

    private TextView tvBottomStatus;
    private TextView tvBottomTotal;


    // ============================================================
    // NETWORK
    // ============================================================

    private final OkHttpClient client = new OkHttpClient();

    private final String BASE_URL =
            "https://www.aaagrowers.co.ke/inventory/";


    // ============================================================
    // SELECTION DATA
    // ============================================================

    private final HashMap<String, Integer> farmMap =
            new HashMap<>();

    private final ArrayList<String> flowerTypes =
            new ArrayList<>();

    private final ArrayList<SummerVariety> varieties =
            new ArrayList<>();

    private int selectedFarmId = -1;
    private String selectedFarmName = "";

    private String selectedFlowerType = "";

    private int selectedVarietyId = -1;
    private String selectedVarietyName = "";
    private String selectedVarietyCode = "";
    private String selectedVarietyColour = "";


    // ============================================================
    // REJECTION DATA
    // ============================================================

    private final ArrayList<SummerRejectionReason> rejectionReasons =
            new ArrayList<>();

    private final LinkedHashMap<Integer, EditText> reasonInputs =
            new LinkedHashMap<>();

    private final LinkedHashMap<Integer, SummerRejectionReason> reasonObjects =
            new LinkedHashMap<>();


    // ============================================================
    // COMMENTS
    // ============================================================

    private String rejectionComments = "";


    // ============================================================
    // QUEUE
    // ============================================================

    private volatile boolean processingQueue = false;

    private final Set<String> inflight =
            Collections.synchronizedSet(new HashSet<>());

    private ProgressDialog progressDialog;


    // ============================================================
    // ACTIVITY
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_rejections_summers);

        initialiseViews();

        setupBackButton();

        setupFarmSelection();

        setupFlowerTypeSelection();

        setupVarietySelection();

        setupButtons();

        setupProgressDialog();

        loadFarms();

        loadRejectionReasons();

        retryPendingBatches();

        registerNetworkListener();
    }


    // ============================================================
    // INITIALISE VIEWS
    // ============================================================

    private void initialiseViews() {

        btnBack = findViewById(R.id.btnBack);

        selectionSection =
                findViewById(R.id.selectionSection);

        rejectionSection =
                findViewById(R.id.rejectionSection);

        commentsSection =
                findViewById(R.id.commentsSection);

        summarySection =
                findViewById(R.id.summarySection);


        spinnerFarm =
                findViewById(R.id.spinnerFarm);

        spinnerFlowerType =
                findViewById(R.id.spinnerFlowerType);

        autoVariety =
                findViewById(R.id.autoVariety);


        cardFlowerType =
                findViewById(R.id.cardFlowerType);

        cardVariety =
                findViewById(R.id.cardVariety);

        cardSelectionStatus =
                findViewById(R.id.cardSelectionStatus);


        tvSelectionStatus =
                findViewById(R.id.tvSelectionStatus);

        tvSelectionDetails =
                findViewById(R.id.tvSelectionDetails);


        btnStartRejections =
                findViewById(R.id.btnStartRejections);

        btnRejectionsNext =
                findViewById(R.id.btnRejectionsNext);

        btnCommentsNext =
                findViewById(R.id.btnCommentsNext);

        btnSubmitAll =
                findViewById(R.id.btnSubmitAll);


        tvSelectedVariety =
                findViewById(R.id.tvSelectedVariety);

        tvSelectedFarmType =
                findViewById(R.id.tvSelectedFarmType);

        tvTotalRejected =
                findViewById(R.id.tvTotalRejected);


        edtComments =
                findViewById(R.id.edtComments);


        tvCommentsTotal =
                findViewById(R.id.tvCommentsTotal);


        tvSummaryVariety =
                findViewById(R.id.tvSummaryVariety);

        tvSummaryFarmType =
                findViewById(R.id.tvSummaryFarmType);

        tvSummaryTotal =
                findViewById(R.id.tvSummaryTotal);

        tvSummaryComments =
                findViewById(R.id.tvSummaryComments);


        containerDiseases =
                findViewById(R.id.containerDiseases);

        containerPest =
                findViewById(R.id.containerPest);

        containerQuality =
                findViewById(R.id.containerQuality);

        containerSummary =
                findViewById(R.id.containerSummary);


        tvBottomStatus =
                findViewById(R.id.tvBottomStatus);

        tvBottomTotal =
                findViewById(R.id.tvBottomTotal);


        // Initial state

        cardFlowerType.setAlpha(0.55f);
        cardVariety.setAlpha(0.55f);

        btnStartRejections.setEnabled(false);

        updateSelectionStatus(
                "Select a farm",
                "Farm is required before continuing."
        );
    }


    // ============================================================
    // BACK BUTTON
    // ============================================================

    private void setupBackButton() {

        btnBack.setOnClickListener(v -> {

            if (summarySection.getVisibility() == View.VISIBLE) {

                showCommentsSection();

                return;
            }

            if (commentsSection.getVisibility() == View.VISIBLE) {

                showRejectionSection();

                return;
            }

            if (rejectionSection.getVisibility() == View.VISIBLE) {

                showSelectionSection();

                return;
            }

            finish();
        });
    }


    // ============================================================
    // FARM
    // ============================================================

    private void setupFarmSelection() {

        spinnerFarm.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        Object selected =
                                parent.getItemAtPosition(position);

                        if (selected == null) {
                            return;
                        }

                        selectedFarmName =
                                selected.toString();

                        Integer farmId =
                                farmMap.get(selectedFarmName);

                        if (farmId == null) {

                            selectedFarmId = -1;

                            return;
                        }

                        selectedFarmId = farmId;

                        resetFlowerType();

                        loadSummerTypes();
                    }


                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {

                        selectedFarmId = -1;

                        selectedFarmName = "";

                        resetFlowerType();
                    }
                }
        );
    }


    // ============================================================
    // LOAD FARMS
    // ============================================================

    private void loadFarms() {

        Request request =
                new Request.Builder()
                        .url(BASE_URL + "get_farms.php")
                        .get()
                        .build();

        client.newCall(request).enqueue(
                new Callback() {

                    @Override
                    public void onFailure(
                            Call call,
                            IOException e) {

                        runOnUiThread(() ->
                                updateBottomStatus(
                                        "Unable to load farms"
                                )
                        );
                    }


                    @Override
                    public void onResponse(
                            Call call,
                            Response response)
                            throws IOException {

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            return;
                        }

                        try {

                            String responseText =
                                    response.body().string();

                            JSONArray array =
                                    new JSONArray(responseText);

                            ArrayList<String> names =
                                    new ArrayList<>();

                            farmMap.clear();

                            for (int i = 0;
                                 i < array.length();
                                 i++) {

                                JSONObject object =
                                        array.getJSONObject(i);

                                String name =
                                        object.optString(
                                                "name",
                                                ""
                                        );

                                int id =
                                        object.optInt(
                                                "id",
                                                -1
                                        );

                                if (!name.isEmpty()
                                        && id != -1) {

                                    names.add(name);

                                    farmMap.put(
                                            name,
                                            id
                                    );
                                }
                            }

                            runOnUiThread(() -> {

                                ArrayAdapter<String>
                                        adapter =
                                        new ArrayAdapter<>(
                                                RejectionsSummersActivity.this,
                                                R.layout.spinner_dropdown_item,
                                                names
                                        );

                                adapter.setDropDownViewResource(
                                        R.layout.spinner_dropdown_item
                                );

                                spinnerFarm.setAdapter(
                                        adapter
                                );

                                updateBottomStatus(
                                        "Select farm"
                                );
                            });

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
                }
        );
    }


    // ============================================================
    // FLOWER TYPE
    // ============================================================

    private void setupFlowerTypeSelection() {

        spinnerFlowerType.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        Object selected =
                                parent.getItemAtPosition(position);

                        if (selected == null) {
                            return;
                        }

                        selectedFlowerType =
                                selected.toString();

                        selectedVarietyId = -1;
                        selectedVarietyName = "";
                        selectedVarietyCode = "";
                        selectedVarietyColour = "";

                        autoVariety.setText("");

                        autoVariety.setEnabled(true);

                        cardVariety.setAlpha(1f);

                        updateSelectionStatus(
                                "Select a variety",
                                selectedFlowerType
                                        + " varieties are ready."
                        );
                    }


                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );
    }


    // ============================================================
    // RESET FLOWER TYPE
    // ============================================================

    private void resetFlowerType() {

        selectedFlowerType = "";

        selectedVarietyId = -1;
        selectedVarietyName = "";

        spinnerFlowerType.setAdapter(null);

        spinnerFlowerType.setEnabled(false);

        autoVariety.setText("");

        autoVariety.setEnabled(false);

        cardFlowerType.setAlpha(0.55f);
        cardVariety.setAlpha(0.55f);

        btnStartRejections.setEnabled(false);

        updateSelectionStatus(
                "Loading flower types",
                selectedFarmName
        );
    }


    // ============================================================
    // LOAD SUMMER TYPES
    // ============================================================

    private void loadSummerTypes() {

        if (selectedFarmId == -1) {
            return;
        }

        HttpUrl url =
                HttpUrl.parse(
                                BASE_URL + "summers/api/get_summer_types.php"
                        )
                        .newBuilder()
                        .addQueryParameter(
                                "farm_id",
                                String.valueOf(selectedFarmId)
                        )
                        .build();

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request).enqueue(
                new Callback() {

                    @Override
                    public void onFailure(
                            Call call,
                            IOException e) {

                        runOnUiThread(() ->
                                updateSelectionStatus(
                                        "Unable to load flower types",
                                        "Check your connection and try again."
                                )
                        );
                    }


                    @Override
                    public void onResponse(
                            Call call,
                            Response response)
                            throws IOException {

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            return;
                        }

                        try {

                            JSONArray array =
                                    new JSONArray(
                                            response.body().string()
                                    );

                            ArrayList<String>
                                    types =
                                    new ArrayList<>();

                            for (int i = 0;
                                 i < array.length();
                                 i++) {

                                JSONObject object =
                                        array.getJSONObject(i);

                                String type =
                                        object.optString(
                                                "type",
                                                ""
                                        );

                                if (!type.isEmpty()) {

                                    types.add(type);
                                }
                            }

                            runOnUiThread(() -> {

                                flowerTypes.clear();

                                flowerTypes.addAll(types);

                                ArrayAdapter<String>
                                        adapter =
                                        new ArrayAdapter<>(
                                                RejectionsSummersActivity.this,
                                                R.layout.spinner_dropdown_item,
                                                flowerTypes
                                        );

                                adapter.setDropDownViewResource(
                                        R.layout.spinner_dropdown_item
                                );

                                spinnerFlowerType.setAdapter(
                                        adapter
                                );

                                spinnerFlowerType.setEnabled(
                                        true
                                );

                                cardFlowerType.setAlpha(
                                        1f
                                );

                                updateSelectionStatus(
                                        "Select a flower type",
                                        selectedFarmName
                                );
                            });

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
                }
        );
    }


    // ============================================================
    // VARIETY
    // ============================================================

    private void setupVarietySelection() {

        autoVariety.setEnabled(false);

        autoVariety.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }


                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        String term =
                                s.toString().trim();

                        if (selectedFlowerType.isEmpty()) {
                            return;
                        }

                        if (term.length() < 1) {
                            return;
                        }

                        loadSummerVarieties(term);
                    }


                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );
    }


    // ============================================================
    // LOAD VARIETIES
    // ============================================================

    private void loadSummerVarieties(
            String term) {

        HttpUrl url =
                HttpUrl.parse(
                                BASE_URL
                                        + "summers/api/search_summer_varieties.php"
                        )
                        .newBuilder()
                        .addQueryParameter(
                                "type",
                                selectedFlowerType
                        )
                        .addQueryParameter(
                                "term",
                                term
                        )
                        .build();

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        client.newCall(request).enqueue(
                new Callback() {

                    @Override
                    public void onFailure(
                            Call call,
                            IOException e) {
                    }


                    @Override
                    public void onResponse(
                            Call call,
                            Response response)
                            throws IOException {

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            return;
                        }

                        try {

                            JSONArray array =
                                    new JSONArray(
                                            response.body().string()
                                    );

                            ArrayList<String>
                                    displayNames =
                                    new ArrayList<>();

                            ArrayList<SummerVariety>
                                    loaded =
                                    new ArrayList<>();

                            for (int i = 0;
                                 i < array.length();
                                 i++) {

                                JSONObject object =
                                        array.getJSONObject(i);

                                SummerVariety variety =
                                        new SummerVariety();

                                variety.id =
                                        object.optInt(
                                                "VarietyId",
                                                -1
                                        );

                                variety.name =
                                        object.optString(
                                                "Variety",
                                                ""
                                        );

                                variety.code =
                                        object.optString(
                                                "Code",
                                                ""
                                        );

                                variety.type =
                                        object.optString(
                                                "Type",
                                                ""
                                        );

                                variety.colour =
                                        object.optString(
                                                "Colour",
                                                ""
                                        );

                                if (variety.id != -1
                                        && !variety.name.isEmpty()) {

                                    loaded.add(variety);

                                    displayNames.add(
                                            variety.name
                                    );
                                }
                            }

                            runOnUiThread(() -> {

                                varieties.clear();

                                varieties.addAll(
                                        loaded
                                );

                                ArrayAdapter<String>
                                        adapter =
                                        new ArrayAdapter<>(
                                                RejectionsSummersActivity.this,
                                                R.layout.autocomplete_item,
                                                displayNames
                                        );

                                autoVariety.setAdapter(
                                        adapter
                                );

                                autoVariety.showDropDown();

                                autoVariety.setOnItemClickListener(
                                        (parent,
                                         view,
                                         position,
                                         id) -> {

                                            if (position
                                                    >= varieties.size()) {

                                                return;
                                            }

                                            SummerVariety
                                                    selected =
                                                    varieties.get(
                                                            position
                                                    );

                                            selectedVarietyId =
                                                    selected.id;

                                            selectedVarietyName =
                                                    selected.name;

                                            selectedVarietyCode =
                                                    selected.code;

                                            selectedVarietyColour =
                                                    selected.colour;

                                            autoVariety.setText(
                                                    selected.name
                                            );

                                            autoVariety.dismissDropDown();

                                            btnStartRejections
                                                    .setEnabled(
                                                            true
                                                    );

                                            updateSelectionStatus(
                                                    "Selection complete",
                                                    selectedFarmName
                                                            + " • "
                                                            + selectedFlowerType
                                                            + " • "
                                                            + selectedVarietyName
                                            );
                                        }
                                );
                            });

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
                }
        );
    }


    // ============================================================
    // BUTTONS
    // ============================================================

    private void setupButtons() {

        btnStartRejections.setOnClickListener(
                v -> startRejection()
        );

        btnRejectionsNext.setOnClickListener(
                v -> proceedToComments()
        );

        btnCommentsNext.setOnClickListener(
                v -> showSummary()
        );

        btnSubmitAll.setOnClickListener(
                v -> submitAllRejections()
        );
    }


    // ============================================================
    // START REJECTION
    // ============================================================

    private void startRejection() {

        if (selectedFarmId == -1) {

            Toast.makeText(
                    this,
                    "Please select a farm.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (selectedFlowerType.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please select a flower type.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (selectedVarietyId == -1) {

            Toast.makeText(
                    this,
                    "Please select a variety.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        tvSelectedVariety.setText(
                selectedVarietyName
        );

        tvSelectedFarmType.setText(
                selectedFarmName
                        + "  •  "
                        + selectedFlowerType
        );

        selectionSection.setVisibility(
                View.GONE
        );

        rejectionSection.setVisibility(
                View.VISIBLE
        );

        commentsSection.setVisibility(
                View.GONE
        );

        summarySection.setVisibility(
                View.GONE
        );

        updateBottomStatus(
                "Recording rejection"
        );

        mainScrollToTop();

        updateTotalRejected();
    }


    // ============================================================
    // LOAD REJECTION REASONS
    // ============================================================

    private void loadRejectionReasons() {

        Request request =
                new Request.Builder()
                        .url(
                                BASE_URL
                                        + "summers/api/summers_rejection_reasons.php"
                        )
                        .get()
                        .build();

        client.newCall(request).enqueue(
                new Callback() {

                    @Override
                    public void onFailure(
                            Call call,
                            IOException e) {

                        runOnUiThread(() ->
                                updateBottomStatus(
                                        "Unable to load rejection reasons"
                                )
                        );
                    }


                    @Override
                    public void onResponse(
                            Call call,
                            Response response)
                            throws IOException {

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            return;
                        }

                        try {

                            JSONArray array =
                                    new JSONArray(
                                            response.body().string()
                                    );

                            rejectionReasons.clear();

                            for (int i = 0;
                                 i < array.length();
                                 i++) {

                                JSONObject object =
                                        array.getJSONObject(i);

                                SummerRejectionReason
                                        reason =
                                        new SummerRejectionReason();

                                reason.id =
                                        object.optInt(
                                                "id",
                                                -1
                                        );

                                reason.category =
                                        object.optString(
                                                "category",
                                                ""
                                        );

                                reason.reason =
                                        object.optString(
                                                "reason",
                                                ""
                                        );

                                reason.sortOrder =
                                        object.optInt(
                                                "sort_order",
                                                0
                                        );

                                if (reason.id != -1
                                        && !reason.reason.isEmpty()) {

                                    rejectionReasons.add(
                                            reason
                                    );
                                }
                            }

                            runOnUiThread(
                                    () ->
                                            buildReasonUI()
                            );

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
                }
        );
    }


    // ============================================================
    // BUILD REJECTION UI
    // ============================================================

    private void buildReasonUI() {

        containerDiseases.removeAllViews();
        containerPest.removeAllViews();
        containerQuality.removeAllViews();

        reasonInputs.clear();
        reasonObjects.clear();

        for (SummerRejectionReason reason
                : rejectionReasons) {

            LinearLayout target;

            if (reason.category.equalsIgnoreCase(
                    "Diseases")) {

                target = containerDiseases;

            } else if (reason.category.equalsIgnoreCase(
                    "Pest")) {

                target = containerPest;

            } else {

                target = containerQuality;
            }

            addReasonRow(
                    target,
                    reason
            );
        }
    }


    // ============================================================
    // CREATE REASON ROW
    // ============================================================

    private void addReasonRow(
            LinearLayout parent,
            SummerRejectionReason reason) {

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        row.setPadding(
                0,
                10,
                0,
                10
        );


        // Reason name

        TextView label =
                new TextView(this);

        label.setText(
                reason.reason
        );

        label.setTextColor(
                android.graphics.Color.rgb(
                        55,
                        71,
                        79
                )
        );

        label.setTextSize(
                14
        );

        label.setTypeface(
                null,
                Typeface.NORMAL
        );

        LinearLayout.LayoutParams
                labelParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        row.addView(
                label,
                labelParams
        );


        // Quantity field

        EditText input =
                new EditText(this);

        input.setText("0");

        input.setTextSize(14);

        input.setTextColor(
                android.graphics.Color.rgb(
                        38,
                        50,
                        56
                )
        );

        input.setGravity(
                Gravity.CENTER
        );

        input.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
        );

        input.setSingleLine(true);

        input.setSelectAllOnFocus(true);

        input.setPadding(
                8,
                0,
                8,
                0
        );

        input.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.rgb(
                                46,
                                125,
                                50
                        )
                )
        );

        LinearLayout.LayoutParams
                inputParams =
                new LinearLayout.LayoutParams(
                        dp(65),
                        dp(48)
                );

        row.addView(
                input,
                inputParams
        );


        // Store

        reasonInputs.put(
                reason.id,
                input
        );

        reasonObjects.put(
                reason.id,
                reason
        );


        // Live total

        input.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        updateTotalRejected();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );


        parent.addView(
                row
        );
    }


    // ============================================================
    // TOTAL
    // ============================================================

    private int calculateTotalRejected() {

        int total = 0;

        for (EditText input
                : reasonInputs.values()) {

            String value =
                    input.getText()
                            .toString()
                            .trim();

            if (value.isEmpty()) {
                continue;
            }

            try {

                total +=
                        Integer.parseInt(value);

            } catch (NumberFormatException ignored) {
            }
        }

        return total;
    }


    private void updateTotalRejected() {

        int total =
                calculateTotalRejected();

        String text =
                total + " "
                        + (total == 1
                        ? "stem"
                        : "stems");

        tvTotalRejected.setText(
                text
        );

        tvCommentsTotal.setText(
                text
        );

        tvSummaryTotal.setText(
                text
        );

        tvBottomTotal.setText(
                text
        );
    }


    // ============================================================
    // REJECTION → COMMENTS
    // ============================================================

    private void proceedToComments() {

        int total =
                calculateTotalRejected();

        if (total <= 0) {

            new AlertDialog.Builder(this)
                    .setTitle("No rejection recorded")
                    .setMessage(
                            "Please enter at least one rejected stem before continuing."
                    )
                    .setPositiveButton(
                            "OK",
                            null
                    )
                    .show();

            return;
        }

        rejectionSection.setVisibility(
                View.GONE
        );

        commentsSection.setVisibility(
                View.VISIBLE
        );

        updateBottomStatus(
                "Add comments"
        );

        mainScrollToTop();
    }


    // ============================================================
    // COMMENTS → SUMMARY
    // ============================================================

    private void showSummary() {

        rejectionComments =
                edtComments.getText()
                        .toString()
                        .trim();

        buildSummary();

        commentsSection.setVisibility(
                View.GONE
        );

        summarySection.setVisibility(
                View.VISIBLE
        );

        updateBottomStatus(
                "Review before submitting"
        );

        mainScrollToTop();
    }


    // ============================================================
    // BUILD SUMMARY
    // ============================================================

    private void buildSummary() {

        tvSummaryVariety.setText(
                selectedVarietyName
        );

        tvSummaryFarmType.setText(
                selectedFarmName
                        + "  •  "
                        + selectedFlowerType
        );

        tvSummaryComments.setText(
                rejectionComments.isEmpty()
                        ? "No comments added."
                        : rejectionComments
        );

        containerSummary.removeAllViews();

        for (SummerRejectionReason reason
                : rejectionReasons) {

            EditText input =
                    reasonInputs.get(
                            reason.id
                    );

            if (input == null) {
                continue;
            }

            int stems =
                    parseInteger(
                            input.getText()
                                    .toString()
                    );

            if (stems <= 0) {
                continue;
            }

            LinearLayout row =
                    new LinearLayout(this);

            row.setOrientation(
                    LinearLayout.HORIZONTAL
            );

            row.setGravity(
                    Gravity.CENTER_VERTICAL
            );

            row.setPadding(
                    0,
                    7,
                    0,
                    7
            );


            TextView reasonText =
                    new TextView(this);

            reasonText.setText(
                    reason.reason
            );

            reasonText.setTextColor(
                    android.graphics.Color.rgb(
                            69,
                            90,
                            100
                    )
            );

            reasonText.setTextSize(
                    14
            );

            LinearLayout.LayoutParams
                    reasonParams =
                    new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1
                    );

            row.addView(
                    reasonText,
                    reasonParams
            );


            TextView stemsText =
                    new TextView(this);

            stemsText.setText(
                    stems + " stems"
            );

            stemsText.setTextColor(
                    android.graphics.Color.rgb(
                            46,
                            125,
                            50
                    )
            );

            stemsText.setTextSize(
                    14
            );

            stemsText.setTypeface(
                    null,
                    Typeface.BOLD
            );

            row.addView(
                    stemsText
            );


            containerSummary.addView(
                    row
            );
        }
    }


    // ============================================================
    // SUBMIT
    // ============================================================

    private void submitAllRejections() {

        int total =
                calculateTotalRejected();

        if (total <= 0) {

            Toast.makeText(
                    this,
                    "No rejection quantities entered.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Submit rejection?")
                .setMessage(
                        "You are about to submit "
                                + total
                                + " rejected stems for "
                                + selectedVarietyName
                                + "."
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Submit",
                        (dialog, which) ->
                                createAndQueueBatch()
                )
                .show();
    }


    // ============================================================
    // CREATE BATCH
    // ============================================================

    private void createAndQueueBatch() {

        String batchId =
                UUID.randomUUID().toString();

        JSONObject batch =
                new JSONObject();

        try {

            batch.put(
                    "batch_id",
                    batchId
            );

            batch.put(
                    "farm_id",
                    selectedFarmId
            );

            batch.put(
                    "farm_name",
                    selectedFarmName
            );

            batch.put(
                    "flower_type",
                    selectedFlowerType
            );

            batch.put(
                    "variety_id",
                    selectedVarietyId
            );

            batch.put(
                    "variety",
                    selectedVarietyName
            );

            batch.put(
                    "variety_code",
                    selectedVarietyCode
            );

            batch.put(
                    "comments",
                    rejectionComments
            );


            JSONArray entries =
                    new JSONArray();


            for (SummerRejectionReason reason
                    : rejectionReasons) {

                EditText input =
                        reasonInputs.get(
                                reason.id
                        );

                if (input == null) {
                    continue;
                }

                int stems =
                        parseInteger(
                                input.getText()
                                        .toString()
                        );

                if (stems <= 0) {
                    continue;
                }


                JSONObject entry =
                        new JSONObject();

                entry.put(
                        "reason_id",
                        reason.id
                );

                entry.put(
                        "reason",
                        reason.reason
                );

                entry.put(
                        "category",
                        reason.category
                );

                entry.put(
                        "stems",
                        stems
                );

                entries.put(
                        entry
                );
            }

            batch.put(
                    "entries",
                    entries
            );

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    "Unable to prepare rejection.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        addBatchToQueue(
                batch
        );

        Toast.makeText(
                this,
                "Rejection added to submission queue.",
                Toast.LENGTH_SHORT
        ).show();


        clearCurrentForm();

        retryPendingBatches();
    }


    // ============================================================
    // QUEUE
    // ============================================================

    private void addBatchToQueue(
            JSONObject batch) {

        try {

            SharedPreferences prefs =
                    getSharedPreferences(
                            "summer_rejection_queue",
                            MODE_PRIVATE
                    );

            JSONArray queue =
                    new JSONArray(
                            prefs.getString(
                                    "queue",
                                    "[]"
                            )
                    );

            queue.put(
                    batch
            );

            prefs.edit()
                    .putString(
                            "queue",
                            queue.toString()
                    )
                    .apply();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    // ============================================================
    // RETRY QUEUE
    // ============================================================

    private void retryPendingBatches() {

        if (processingQueue) {
            return;
        }

        processingQueue = true;

        new Thread(() -> {

            try {

                SharedPreferences prefs =
                        getSharedPreferences(
                                "summer_rejection_queue",
                                MODE_PRIVATE
                        );

                JSONArray queue =
                        new JSONArray(
                                prefs.getString(
                                        "queue",
                                        "[]"
                                )
                        );

                int total =
                        queue.length();

                if (total == 0) {

                    runOnUiThread(() -> {

                        if (progressDialog != null
                                && progressDialog.isShowing()) {

                            progressDialog.dismiss();
                        }

                        updateBottomStatus(
                                "Ready"
                        );
                    });

                    processingQueue = false;

                    return;
                }


                runOnUiThread(() -> {

                    progressDialog.setMax(
                            total
                    );

                    progressDialog.setProgress(
                            0
                    );

                    progressDialog.show();
                });


                int processed = 0;


                for (int i = 0;
                     i < queue.length();
                     i++) {

                    JSONObject batch =
                            queue.getJSONObject(i);

                    String batchId =
                            batch.optString(
                                    "batch_id",
                                    ""
                            );

                    if (batchId.isEmpty()) {

                        processed++;

                        final int progress =
                                processed;

                        runOnUiThread(() ->
                                progressDialog.setProgress(
                                        progress
                                )
                        );

                        continue;
                    }


                    if (inflight.contains(
                            batchId
                    )) {

                        processed++;

                        final int progress =
                                processed;

                        runOnUiThread(() ->
                                progressDialog.setProgress(
                                        progress
                                )
                        );

                        continue;
                    }


                    inflight.add(
                            batchId
                    );


                    final Object lock =
                            new Object();

                    final boolean[] finished =
                            {false};


                    submitBatchFromQueue(
                            batch,
                            new SubmitCallback() {

                                @Override
                                public void onSuccess() {

                                    removeBatchLocally(
                                            batchId
                                    );

                                    inflight.remove(
                                            batchId
                                    );

                                    synchronized (lock) {

                                        finished[0] =
                                                true;

                                        lock.notify();
                                    }
                                }


                                @Override
                                public void onFailure() {

                                    inflight.remove(
                                            batchId
                                    );

                                    synchronized (lock) {

                                        finished[0] =
                                                true;

                                        lock.notify();
                                    }
                                }
                            }
                    );


                    synchronized (lock) {

                        while (!finished[0]) {

                            try {

                                lock.wait();

                            } catch (InterruptedException e) {

                                Thread.currentThread()
                                        .interrupt();

                                break;
                            }
                        }
                    }


                    processed++;

                    final int progress =
                            processed;

                    runOnUiThread(() ->
                            progressDialog.setProgress(
                                    progress
                            )
                    );
                }


                runOnUiThread(() -> {

                    if (progressDialog.isShowing()) {

                        progressDialog.dismiss();
                    }

                    updateBottomStatus(
                            "Ready"
                    );
                });


            } catch (Exception e) {

                e.printStackTrace();

                runOnUiThread(() -> {

                    if (progressDialog != null
                            && progressDialog.isShowing()) {

                        progressDialog.dismiss();
                    }
                });

            } finally {

                processingQueue = false;
            }

        }).start();
    }


    // ============================================================
    // SUBMIT QUEUED BATCH
    // ============================================================

    private void submitBatchFromQueue(
            JSONObject batch,
            SubmitCallback callback) {

        try {

            String batchId =
                    batch.optString(
                            "batch_id"
                    );

            int farmId =
                    batch.optInt(
                            "farm_id",
                            0
                    );

            int varietyId =
                    batch.optInt(
                            "variety_id",
                            0
                    );

            String farmName =
                    batch.optString(
                            "farm_name",
                            ""
                    );

            String flowerType =
                    batch.optString(
                            "flower_type",
                            ""
                    );

            String variety =
                    batch.optString(
                            "variety",
                            ""
                    );

            String varietyCode =
                    batch.optString(
                            "variety_code",
                            ""
                    );

            String comments =
                    batch.optString(
                            "comments",
                            ""
                    );

            JSONArray entries =
                    batch.optJSONArray(
                            "entries"
                    );


            MultipartBody.Builder builder =
                    new MultipartBody.Builder()
                            .setType(
                                    MultipartBody.FORM
                            );


            builder.addFormDataPart(
                    "batch_id",
                    batchId
            );

            builder.addFormDataPart(
                    "farm_id",
                    String.valueOf(
                            farmId
                    )
            );

            builder.addFormDataPart(
                    "farm_name",
                    farmName
            );

            builder.addFormDataPart(
                    "flower_type",
                    flowerType
            );

            builder.addFormDataPart(
                    "variety_id",
                    String.valueOf(
                            varietyId
                    )
            );

            builder.addFormDataPart(
                    "variety",
                    variety
            );

            builder.addFormDataPart(
                    "variety_code",
                    varietyCode
            );

            builder.addFormDataPart(
                    "comments",
                    comments
            );


            if (entries != null) {

                for (int i = 0;
                     i < entries.length();
                     i++) {

                    JSONObject entry =
                            entries.getJSONObject(
                                    i
                            );

                    builder.addFormDataPart(
                            "reason_id[]",
                            String.valueOf(
                                    entry.optInt(
                                            "reason_id",
                                            0
                                    )
                            )
                    );

                    builder.addFormDataPart(
                            "reason[]",
                            entry.optString(
                                    "reason",
                                    ""
                            )
                    );

                    builder.addFormDataPart(
                            "category[]",
                            entry.optString(
                                    "category",
                                    ""
                            )
                    );

                    builder.addFormDataPart(
                            "stems[]",
                            String.valueOf(
                                    entry.optInt(
                                            "stems",
                                            0
                                    )
                            )
                    );
                }
            }


            Request request =
                    new Request.Builder()
                            .url(
                                    BASE_URL
                                            + "summers/api/summer_rejection_form.php"
                            )
                            .post(
                                    builder.build()
                            )
                            .build();


            client.newCall(request).enqueue(
                    new Callback() {

                        @Override
                        public void onFailure(
                                Call call,
                                IOException e) {

                            e.printStackTrace();

                            runOnUiThread(() ->
                                    updateBottomStatus(
                                            "Pending submission"
                                    )
                            );

                            callback.onFailure();
                        }


                        @Override
                        public void onResponse(
                                Call call,
                                Response response)
                                throws IOException {

                            String responseBody =
                                    response.body() != null
                                            ? response.body()
                                            .string()
                                            : "";

                            if (!response.isSuccessful()) {

                                callback.onFailure();

                                return;
                            }


                            try {

                                JSONObject result =
                                        new JSONObject(
                                                responseBody
                                        );

                                boolean success =
                                        result.optBoolean(
                                                "success",
                                                false
                                        );

                                if (success) {

                                    callback.onSuccess();

                                } else {

                                    callback.onFailure();
                                }

                            } catch (Exception e) {

                                /*
                                 * If the endpoint returns a plain
                                 * successful response instead of JSON,
                                 * treat HTTP 2xx as successful.
                                 */

                                callback.onSuccess();
                            }
                        }
                    }
            );

        } catch (Exception e) {

            e.printStackTrace();

            callback.onFailure();
        }
    }


    // ============================================================
    // REMOVE QUEUED BATCH
    // ============================================================

    private void removeBatchLocally(
            String batchId) {

        try {

            SharedPreferences prefs =
                    getSharedPreferences(
                            "summer_rejection_queue",
                            MODE_PRIVATE
                    );

            JSONArray queue =
                    new JSONArray(
                            prefs.getString(
                                    "queue",
                                    "[]"
                            )
                    );

            JSONArray newQueue =
                    new JSONArray();


            for (int i = 0;
                 i < queue.length();
                 i++) {

                JSONObject object =
                        queue.getJSONObject(
                                i
                        );

                if (!object.optString(
                        "batch_id",
                        ""
                ).equals(batchId)) {

                    newQueue.put(
                            object
                    );
                }
            }


            prefs.edit()
                    .putString(
                            "queue",
                            newQueue.toString()
                    )
                    .apply();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    // ============================================================
    // CLEAR FORM
    // ============================================================

    private void clearCurrentForm() {

        for (EditText input
                : reasonInputs.values()) {

            input.setText("0");
        }

        edtComments.setText("");

        rejectionComments = "";

        selectedVarietyId = -1;
        selectedVarietyName = "";
        selectedVarietyCode = "";
        selectedVarietyColour = "";

        autoVariety.setText("");

        btnStartRejections.setEnabled(
                false
        );

        showSelectionSection();

        updateSelectionStatus(
                "Select a variety",
                "Ready for the next rejection."
        );
    }


    // ============================================================
    // SCREEN NAVIGATION
    // ============================================================

    private void showSelectionSection() {

        selectionSection.setVisibility(
                View.VISIBLE
        );

        rejectionSection.setVisibility(
                View.GONE
        );

        commentsSection.setVisibility(
                View.GONE
        );

        summarySection.setVisibility(
                View.GONE
        );

        updateBottomStatus(
                "Select farm, type and variety"
        );

        mainScrollToTop();
    }


    private void showRejectionSection() {

        selectionSection.setVisibility(
                View.GONE
        );

        rejectionSection.setVisibility(
                View.VISIBLE
        );

        commentsSection.setVisibility(
                View.GONE
        );

        summarySection.setVisibility(
                View.GONE
        );

        updateBottomStatus(
                "Recording rejection"
        );

        mainScrollToTop();
    }


    private void showCommentsSection() {

        selectionSection.setVisibility(
                View.GONE
        );

        rejectionSection.setVisibility(
                View.GONE
        );

        commentsSection.setVisibility(
                View.VISIBLE
        );

        summarySection.setVisibility(
                View.GONE
        );

        updateBottomStatus(
                "Add comments"
        );

        mainScrollToTop();
    }


    // ============================================================
    // SELECTION STATUS
    // ============================================================

    private void updateSelectionStatus(
            String title,
            String details) {

        tvSelectionStatus.setText(
                title
        );

        tvSelectionDetails.setText(
                details
        );
    }


    // ============================================================
    // BOTTOM STATUS
    // ============================================================

    private void updateBottomStatus(
            String status) {

        if (tvBottomStatus != null) {

            tvBottomStatus.setText(
                    status
            );
        }
    }


    // ============================================================
    // PROGRESS
    // ============================================================

    private void setupProgressDialog() {

        progressDialog =
                new ProgressDialog(this);

        progressDialog.setTitle(
                "Submitting Pending Rejections"
        );

        progressDialog.setMessage(
                "Please wait..."
        );

        progressDialog.setCancelable(
                false
        );

        progressDialog.setProgressStyle(
                ProgressDialog.STYLE_HORIZONTAL
        );
    }


    // ============================================================
    // NETWORK LISTENER
    // ============================================================

    private void registerNetworkListener() {

        ConnectivityManager cm =
                (ConnectivityManager)
                        getSystemService(
                                CONNECTIVITY_SERVICE
                        );

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.N) {

            cm.registerDefaultNetworkCallback(
                    new ConnectivityManager.NetworkCallback() {

                        @Override
                        public void onAvailable(
                                Network network) {

                            runOnUiThread(() ->
                                    retryPendingBatches()
                            );
                        }
                    }
            );
        }
    }


    // ============================================================
    // SCROLL
    // ============================================================

    private void mainScrollToTop() {

        NestedScrollViewHelper.scrollToTop(
                findViewById(
                        R.id.mainScrollView
                )
        );
    }


    // ============================================================
    // INTEGER
    // ============================================================

    private int parseInteger(
            String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return 0;
        }

        try {

            return Integer.parseInt(
                    value.trim()
            );

        } catch (NumberFormatException e) {

            return 0;
        }
    }


    // ============================================================
    // DP
    // ============================================================

    private int dp(int value) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(
                value * density
        );
    }


    // ============================================================
    // CALLBACK
    // ============================================================

    private interface SubmitCallback {

        void onSuccess();

        void onFailure();
    }


    // ============================================================
    // SUMMER VARIETY MODEL
    // ============================================================

    private static class SummerVariety {

        int id;

        String name = "";

        String code = "";

        String type = "";

        String colour = "";
    }


    // ============================================================
    // REJECTION REASON MODEL
    // ============================================================

    private static class SummerRejectionReason {

        int id;

        String category = "";

        String reason = "";

        int sortOrder = 0;
    }


    // ============================================================
    // SCROLL HELPER
    // ============================================================

    private static class NestedScrollViewHelper {

        static void scrollToTop(
                androidx.core.widget.NestedScrollView scrollView) {

            if (scrollView == null) {
                return;
            }

            scrollView.post(
                    () -> scrollView.smoothScrollTo(
                            0,
                            0
                    )
            );
        }
    }
}